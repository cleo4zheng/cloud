package com.cloud.cloudapi.mail;

import org.apache.commons.mail.EmailException;

public interface MailService {
	  public void sendSimpleMessage(Mail mail) throws EmailException;
	  public void sendHtmlMessage(Mail mail) throws EmailException;
}
