package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.ArrayList;
import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;

public class Quota {

	private String id;
	private String userId;
	private String tenantId;
	private String quotaType;
	private String quotaTypeName;
	private String quotaDetailsId;
	private Boolean shared;
	private List<QuotaDetail> data;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getQuotaType() {
		return quotaType;
	}

	public void setQuotaType(String quotaType) {
		this.quotaType = quotaType;
	}

	public List<QuotaDetail> getData() {
		return data;
	}

	public void setData(List<QuotaDetail> data) {
		this.data = data;
	}

	public void addQuotaDetail(QuotaDetail quotaDetail){
		if(null == this.data)
			this.data = new ArrayList<QuotaDetail>();
		this.data.add(quotaDetail);
	}
	
	public void addQuotaDetail(List<QuotaDetail> quotaDetails){
		if(null == this.data)
			this.data = new ArrayList<QuotaDetail>();
		this.data.addAll(quotaDetails);
	}
	
	public void setUserId(String userId){
		this.userId = userId;
	}
	
	public String getUserId(){
		return this.userId;
	}
	
	public void setTenantId(String tenantId){
		this.tenantId = tenantId;
	}
	
	public String getTenantId(){
		return this.tenantId;
	}

	public String getQuotaDetailsId() {
		return quotaDetailsId;
	}

	public void setQuotaDetailsId(String quotaDetailsId) {
		this.quotaDetailsId = quotaDetailsId;
	}
	
	public void makeQuotaDetailsId(){
		if(Util.isNullOrEmptyList(this.data))
			return;
		List<String> detailsId = new ArrayList<String>();
		for(QuotaDetail detail : this.data){
			detailsId.add(detail.getId());
		}
		this.quotaDetailsId = Util.listToString(detailsId, ',');
	}

	public String getQuotaTypeName() {
		return quotaTypeName;
	}

	public void setQuotaTypeName(String quotaTypeName) {
		this.quotaTypeName = quotaTypeName;
	}

	public Boolean getShared() {
		return shared;
	}

	public void setShared(Boolean shared) {
		this.shared = shared;
	}
	
	public void normalInfo(){
		this.setTenantId(null);
		this.setQuotaDetailsId(null);
		this.setShared(null);
		List<QuotaDetail> datas = this.getData();
		if(Util.isNullOrEmptyList(datas))
			return;
		for(QuotaDetail data : datas){
			data.setTenantId(null);
		}
	}
}
