package com.easipass.gateway.filter;

import com.easipass.commoncore.model.KafkaMessageEntity;
import com.easipass.gateway.controller.CounterController;
import com.easipass.gateway.model.GatewayContext;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;

@Component
@Slf4j
public class ElapsedFilter implements GlobalFilter, Ordered {
    private static final String ELAPSED_TIME_BEGIN = "elapsedTimeBegin";

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Value("${easipass.logappender.config.kafkaTopic}")
    private String kafkaTopic;

    @Value("${easipass.logappender.config.autoAppendSwitch:true}")
    private Boolean autoAppendSwitch;

    @Value("${spring.application.name}")
    private String topicProject;

    @Value("${settings.saveSessionSecond:3600}")
    private Integer saveSessionSecond;

    @Override
    public int getOrder() {
        return -2;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("-------- GatewayFilter ---------");
        log.info("---remoteAddress---" + exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
        log.info("---Host---" + exchange.getRequest().getHeaders().get("Host"));
        KafkaMessageEntity kafkaMessageEntity = new KafkaMessageEntity();
        kafkaMessageEntity.setRequestUri(exchange.getRequest().getURI().getPath());
        kafkaMessageEntity.setTopicProject(topicProject);

        GatewayContext gatewayContext = exchange.getAttribute(GatewayContext.CACHE_GATEWAY_CONTEXT);
        kafkaMessageEntity.setRequestParames(gatewayContext.getRequestParames());
        kafkaMessageEntity.setRequestHeaders(gatewayContext.getHeaders());
        kafkaMessageEntity.setRequestBody(gatewayContext.getCacheBody());
        kafkaMessageEntity.setUserIp(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());

        exchange.getAttributes().put(ELAPSED_TIME_BEGIN, System.currentTimeMillis());
//        return chain.filter(exchange.mutate().response(printResponse(exchange , kafkaMessageEntity)).build()).then(
        return chain.filter(exchange).then(
                Mono.fromRunnable(() -> {
                    Long startTime = exchange.getAttribute(ELAPSED_TIME_BEGIN);
                    if (startTime != null) {
                        Long proccedMills = System.currentTimeMillis() - startTime;
                        log.info(exchange.getRequest().getURI().getRawPath() + ": " + proccedMills + "ms");
                        kafkaMessageEntity.setProceedMills(proccedMills);
                    }

                    if(autoAppendSwitch){
                        kafkaTemplate.send(kafkaTopic , kafkaMessageEntity.toString());
                    }

                    //对断言中的自定义配置做出处理
                    String predicateName = exchange.getAttribute("PredicateName");
                    if(!StringUtils.isEmpty(predicateName)){
                        if ("WhiteCounter".equals(predicateName)){
                            String countGroup = exchange.getAttribute("countGroup");
                            String count = exchange.getAttribute("count");
                            CounterController.oauthRedisSet(predicateName + ":" + countGroup , count);
                        }
                    }

                    //对过滤器中的自定义配置做出处理
                    String filterName = exchange.getAttribute("FilterName");
                    if(!StringUtils.isEmpty(filterName)){
                        if ("SaveSessionToRequestUri".equals(filterName)){
                            Integer saveSessionIndex = exchange.getAttribute("saveSessionIndex");
                            HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
                            String setCookieStr = responseHeaders.getFirst("Set-Cookie");
                            if(!StringUtils.isEmpty(setCookieStr)){
                                String jessionid = null;
                                String[] setCookies = setCookieStr.split(";");
                                for(String setCookie : setCookies){
                                    String[] setCookiees = setCookie.split("=");
                                    if("JSESSIONID".equals(setCookiees[0].trim())){
                                        jessionid = setCookiees[1].trim();
                                    }
                                }
                                log.info("jessionid: " + jessionid + " , saveSessionIndex: " + saveSessionIndex);
                                CounterController.oauthRedisSetEx(filterName + ":" + jessionid , String.valueOf(saveSessionIndex), saveSessionSecond);
                            }
                        }
                    }
                })
        );
    }

    public ServerHttpResponseDecorator printResponse(ServerWebExchange exchange , KafkaMessageEntity kafkaMessageEntity){
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                    return super.writeWith(fluxBody.map(dataBuffer -> {
                        // probably should reuse buffers
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(content);
                        //释放掉内存
                        DataBufferUtils.release(dataBuffer);
                        String s = new String(content, Charset.forName("UTF-8"));
                        kafkaMessageEntity.setAfterReturningResult(s);
                        byte[] uppedContent = new String(content, Charset.forName("UTF-8")).getBytes();
                        return bufferFactory.wrap(uppedContent);
                    }));
                }
                // if body is not a flux. never got there.
                return super.writeWith(body);
            }
        };
        return decoratedResponse;
    }
}