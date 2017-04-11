package com.cloud.cloudapi.pojo.openstackapi.foros;

public class Links {
	private String href;

	private String rel;
	
	private String self;
	
	private String previous;
	
	private String next;

	public String getPrevious() {
		return previous;
	}

	public void setPrevious(String previous) {
		this.previous = previous;
	}

	public String getNext() {
		return next;
	}

	public void setNext(String next) {
		this.next = next;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public String getHref() {
		return this.href;
	}

	public void setRel(String rel) {
		this.rel = rel;
	}

	public String getRel() {
		return this.rel;
	}

	public String getSelf() {
		return self;
	}

	public void setSelf(String self) {
		this.self = self;
	}

	
}