package com.easipass.sys.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author mlzhang
 */
@ControllerAdvice(annotations = {RestController.class , Controller.class})
@ResponseBody
@Slf4j
public class GlobalRestExceptionHandler {

    @ExceptionHandler
    @ResponseBody
    public String restExceptionHandler(Exception e) {
        log.error(e.getMessage() , e);
        return e.getMessage();
    }
}
