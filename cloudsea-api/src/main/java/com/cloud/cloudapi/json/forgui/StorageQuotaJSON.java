package com.cloud.cloudapi.json.forgui;

import com.cloud.cloudapi.pojo.openstackapi.forgui.StorageQuota;

public class StorageQuotaJSON {

	private StorageQuota quota_set;
	
	public StorageQuotaJSON(StorageQuota quota_set){
		this.quota_set = quota_set;
	}
}
