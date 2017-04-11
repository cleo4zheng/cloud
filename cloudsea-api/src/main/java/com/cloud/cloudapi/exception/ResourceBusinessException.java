package com.cloud.cloudapi.exception;

import java.util.Locale;

import com.cloud.cloudapi.util.Message;

public class ResourceBusinessException extends BusinessException {

	private static final long serialVersionUID = -5597846931484161460L;

	private String message = null;
    private int statusCode;

	public ResourceBusinessException(String messageID,String... params) {
		super(messageID,params);
		message = ResourceBusinessException.createMessage(messageID, params, true);
	}
	
	public ResourceBusinessException(String messageID,Locale locale,String... params) {
		super(messageID,params);
		message = ResourceBusinessException.createMessage(messageID,locale,params, true);
	}
	
	public ResourceBusinessException(String messageID,int code,String... params) {
		super(messageID,params);
		statusCode = code;
		message = ResourceBusinessException.createMessage(messageID, params, true);
	}

	public ResourceBusinessException(String messageID,int code,Locale locale,String... params) {
		super(messageID,params);
		statusCode = code;
		message = ResourceBusinessException.createMessage(messageID,locale,params, true);
	}
	
	public ResourceBusinessException(String messageID,int code,Throwable throwable,
			String... params) {
		super(messageID,params, throwable);
		statusCode = code;
		message = ResourceBusinessException.createMessage(messageID, params, true);
	}

	
	@Override
	public String getMessage() {
		return message;
	}

    public String getResponseMessage(){
    	return this.message;
    }
    
	public String getMessage(boolean withMsgId) {
		return ResourceBusinessException.createMessage(getMessageID(),getParams(), withMsgId);
	}

	
	@Override
	public void setMessageID(String messageID) {
		super.setMessageID(messageID);
		message = ResourceBusinessException.createMessage(messageID, getParams(),true);
	}

	
	@Override
	public void setParams(String[] params) {
		super.setParams(params);
		message = ResourceBusinessException.createMessage(getMessageID(), params,true);
	}

	private static String createMessage(String messageID, String[] params,
			boolean withMsgId) {
		return Message.getMessage(messageID, Locale.getDefault(),params, withMsgId);
	}
	
	private static String createMessage(String messageID, Locale locale,String[] params,
			boolean withMsgId) {
		return Message.getMessage(messageID,locale,params, withMsgId);
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}


}
