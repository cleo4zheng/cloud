package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.crm.CrmInfo;

public interface CrmInfoMapper extends SuperMapper<CrmInfo,String>{

	public List<CrmInfo> selectAllCrmInfoByDisableStatus(int isdisabled);

}
