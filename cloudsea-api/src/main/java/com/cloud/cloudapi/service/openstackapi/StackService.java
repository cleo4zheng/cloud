package com.cloud.cloudapi.service.openstackapi;

import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Stack;
import com.cloud.cloudapi.pojo.openstackapi.forgui.StackResource;

public interface StackService {
	public List<Stack> getStackList(Map<String, String> paraMap, TokenOs ostoken);

	public Stack getStack(String stackId, TokenOs ostoken);

	public Stack createStack(String stackName, Map<String, String> paramMap, String template, Map<String, String> files,
			String environment, TokenOs ostoken) throws BusinessException;

	public Stack updateStack(String stackName, Map<String, String> paramMap, String template, Map<String, String> files,
			String environment, TokenOs ostoken) throws BusinessException;

	public List<StackResource> getStackResourceList(String stackName, String stackId, TokenOs ostoken);

	public String getStackNameById(String stackId, TokenOs ostoken);

	public StackResource getStackResource(String stackName, String stackId, String resourceName, TokenOs ostoken);
	
	public boolean deleteStack(String stackName, String stackId, TokenOs ostoken);
}
