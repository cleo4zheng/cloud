package com.cloud.cloudapi.service.crm.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.CrmInfoMapper;
import com.cloud.cloudapi.pojo.crm.CrmInfo;
import com.cloud.cloudapi.service.crm.CrmInfoService;

@Service
public class CrmInfoServiceImpl implements CrmInfoService {

	@Resource
	private CrmInfoMapper crmInfoMapper;
	
	@Override
	public List<CrmInfo> getAllavaialeCrmInfo() {
		// TODO Auto-generated method stub
		return crmInfoMapper.selectAllCrmInfoByDisableStatus(0);
	}

}
