package com.cloud.cloudapi.mail;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.springframework.stereotype.Service;

@Service("mailService")
public class MailServiceImpl implements MailService {

	@Override
	public void sendSimpleMessage(Mail mail) throws EmailException {
		SimpleEmail email = new SimpleEmail();
		email.setHostName(mail.getHost());
		email.setAuthentication(mail.getUsername(),mail.getPassword());
		email.addTo(mail.getReceiver());
		email.setFrom(mail.getSender());
		email.setSubject(mail.getSubject());
		email.setMsg(mail.getMessage());
		email.send();
	}
	
	@Override
	public void sendHtmlMessage(Mail mail) throws EmailException {
		HtmlEmail email = new HtmlEmail();
		email.setHostName(mail.getHost());
		email.setAuthentication(mail.getUsername(),mail.getPassword());
		email.addTo(mail.getReceiver());
		email.setFrom(mail.getSender());
		email.setSubject(mail.getSubject());
		email.setTextMsg(mail.getMessage());
		email.send();
	}
}
