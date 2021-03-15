package com.easipass.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.easipass.commoncore.exception.EasiServiceException;
import com.easipass.commoncore.model.ApiResult;
import com.easipass.commoncore.util.JsonUtils;
import com.easipass.gateway.config.BlockChainRecordConf;
import com.easipass.gateway.constant.ExceptionConstant;
import com.easipass.gateway.entity.RecordEntity;
import com.easipass.gateway.model.GatewayContext;
import com.easipass.gateway.util.HandleExceptionUtils;
import com.easipass.gateway.util.HttpUtils;
import com.google.common.base.Joiner;
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
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class BlockChainRecordFilter implements GlobalFilter, Ordered {

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Value("${easipass.logappender.config.kafkaTopic}")
    private String kafkaTopic;

    @Value("${easipass.gateway.blockChainRecordSwitch:false}")
    private Boolean blockChainRecordSwitch;

    @Value("${easipass.gateway.blockChainTopicProject:BlockChainRecord}")
    private String blockChainTopicProject;

    @Value("${easipass.gateway.blockChain.appName:ep-fabric-demo}")
    private String blockChainAppName;

    private static Joiner joiner = Joiner.on("");

    public BlockChainRecordFilter() {
        super();
    }

    @Override
    public int getOrder() {
        return -2;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        log.info("-------- BlockChainRecordFilter --------");

        //判断是否进入此filter  只有区块链项目进此filter  （由于order必须<-1 故使用全局filter）
        if (!verifyPassPath(exchange)) {
            return chain.filter(exchange);
        }

        //开关
        if (!blockChainRecordSwitch) {
            return chain.filter(exchange);
        }

        // 判断是否需要进行录制   例如：回放的时候不需要网关进行录制
        if (verifyStopRecord(exchange)) {
            return chain.filter(exchange);
        }

        // 判断是否需要录制  例如：在apollo中配置 path和是否录制的关联关系
        if (!verifyUriPath(exchange)) {
            return chain.filter(exchange);
        }

        //录制请求数据
        RecordEntity recordEntity = null;
        try {
            recordEntity = recordRequest(exchange);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return HandleExceptionUtils.handleErrJsonResponse(ExceptionConstant.ERROR_CODE601, exchange);
        }

        final RecordEntity finalRecordEntity = recordEntity;

        return chain.filter(exchange.mutate().response(printResponse(exchange, recordEntity)).build()).then(
                Mono.fromRunnable(() -> {
                    //将录制的数据发送的数据中心
                    sendRecord2DataCenter(finalRecordEntity);
                })
        );
    }

    private Boolean verifyPassPath(ServerWebExchange exchange) {
        String part1 = exchange.getRequest().getURI().getPath().split("/")[1];
        return blockChainAppName.equals(part1);
    }

    private Boolean verifyUriPath(ServerWebExchange exchange) {
        String recordPath = BlockChainRecordConf.getRelations().getJSONObject("ep-fabric-demo").getString("recordPath");
        List<String> recordPathList = Arrays.asList(recordPath.split(","));
        String part3 = exchange.getRequest().getURI().getPath().split("/")[3];
        return recordPathList.contains(part3);
    }

    private Boolean verifyStopRecord(ServerWebExchange exchange) {
        String paramValue = HttpUtils.getParamByExchange(exchange, new String[]{"isPlayback"});
        return "1".equals(paramValue);
    }

    private void sendRecord2DataCenter(RecordEntity recordEntity) {

        kafkaTemplate.send(kafkaTopic , recordEntity.toString());
    }

    private void recordResponse(String response, RecordEntity recordEntity) {
        recordEntity.setResponse(response);
        ApiResult apiResult = JsonUtils.jsonToBean(response, ApiResult.class);
        boolean isSuccess = "T".equals(apiResult.getFlag());
        recordEntity.setResponseFlag(isSuccess);
        if (isSuccess) {
            Long createTime = ((JSONObject) apiResult.getData()).getLong("createTime");
            recordEntity.setTimeOrder(createTime);
            recordEntity.setTimestamp(new Date(createTime));
        }
    }

    private RecordEntity recordRequest(ServerWebExchange exchange) {

        RecordEntity recordEntity = new RecordEntity();

        recordEntity.setTopicProject(blockChainTopicProject);
//        recordEntity.setRecordId();
        GatewayContext gatewaycontext = exchange.getAttribute(GatewayContext.CACHE_GATEWAY_CONTEXT);
        recordEntity.setRequestMethod(gatewaycontext.getRequestMethod());
        recordEntity.setRequestHeaders(gatewaycontext.getHeaders());
        recordEntity.setFullUrl(gatewaycontext.getFullUrl());
        recordEntity.setPath(gatewaycontext.getPath());
        recordEntity.setRequestParams(gatewaycontext.getRequestJsonParams());
        recordEntity.setRequestBody(gatewaycontext.getCacheBody());

        return recordEntity;
    }

    public ServerHttpResponseDecorator printResponse(ServerWebExchange exchange, RecordEntity recordEntity) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                    return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
                        List<String> list = new ArrayList<>();
                        dataBuffers.forEach(dataBuffer -> {
                            try {
                                byte[] content = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(content);
                                DataBufferUtils.release(dataBuffer);

                                list.add(new String(content, "utf-8"));
                            } catch (Exception e) {
                                log.error(e.getMessage() , e);
                            }
                        });
                        String responseData = joiner.join(list);
                        log.info("recordResponse:" + responseData);
                        // 二次处理（加密/过滤等）如果不需要做二次处理可直接跳过下行
                        try {
                            recordResponse(responseData, recordEntity);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            throw new EasiServiceException(ExceptionConstant.ERROR_CODE601.getErrorCode(),ExceptionConstant.ERROR_CODE601.getErrorInfo());
                        }
                        byte[] uppedContent = new String(responseData.getBytes(), Charset.forName("UTF-8")).getBytes();
                        originalResponse.getHeaders().setContentLength(uppedContent.length);
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

