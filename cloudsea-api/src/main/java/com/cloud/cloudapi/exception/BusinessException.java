package com.cloud.cloudapi.exception;

public class BusinessException extends Exception {

    private static final long serialVersionUID = 1L;
    private String messageID;
    private String params[];

    
    public BusinessException(String messageID) {
        this(messageID,(String[])null, null);
    }

    public BusinessException(String messageID,String param) {
        this(messageID, param, null);
    }

    public BusinessException(String messageID,String param[]) {
        this(messageID,param, null);
    }

    public BusinessException(String messageID,String param, Throwable t) {
        super("", t);
        this.messageID = messageID;
        String tmp[] = {param};
        this.params = tmp;
    }

    public BusinessException(String messageID,String params[], Throwable t) {
        super("", t);
        this.messageID = messageID;
        this.params = params;
    }

    public String getMessageID() {
        return messageID;
    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    public String[] getParams() {
        return params;
    }

    public void setParams(String[] params) {
        this.params = params;
    }

}
