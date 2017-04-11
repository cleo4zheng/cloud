package com.cloud.cloudapi.json.forgui;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Quota;

public class TenantQuotaJSON {

	private List<Quota> panelInfo;
	private List<Quota> progressInfo;
	
	public TenantQuotaJSON(List<Quota> panelInfo,List<Quota> progressInfo){
		this.panelInfo = panelInfo;
		this.progressInfo = progressInfo;
	}
}
