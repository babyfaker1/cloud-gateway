package com.easipass.gateway.constant;

public enum ExceptionConstant {

    ERROR_CODE601("601", "区块链录制参数失败");

    private String errorCode;
    private String errorInfo;

    private ExceptionConstant(String errorCode, String errorInfo) {
        this.errorCode = errorCode;
        this.errorInfo = errorInfo;
    }

    public String getErrorCode() {
        return this.errorCode;
    }

    public String getErrorInfo() {
        return this.errorInfo;
    }
    }
