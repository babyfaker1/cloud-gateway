package com.easipass.gateway.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.datetime.standard.DateTimeContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

@RestController
@Slf4j
public class TestController {

    private final RestTemplate restTemplate;

    @Value("${sentinel.flowRules:[]}")
    private String flowRulesStr;

    @Autowired
    public TestController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    @SentinelResource(value = "echo" , blockHandler = "blockHandlerForEcho")
    @RequestMapping(value = "echo/{str}", method = RequestMethod.GET)
    public String echo(@PathVariable String str) {
        LocalTime localTime = LocalTime.now();
        Locale locale = LocaleContextHolder.getLocale();
        DateTimeFormatter formatterToUse = DateTimeContextHolder.getFormatter(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT), locale);
        log.info(localTime.format(formatterToUse));
        return restTemplate.getForObject("http://ep-server/ep-server/echo/" + str, String.class);
    }

    // blockHandler 函数，原方法调用被限流/降级/系统保护的时候调用
    public String blockHandlerForEcho(String str, BlockException ex) {
        log.error(ex.getMessage());
        return "blockHandlerForEcho:" + str;
    }


}
