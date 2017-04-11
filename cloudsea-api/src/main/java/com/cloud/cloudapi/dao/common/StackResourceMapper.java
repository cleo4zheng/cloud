package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.StackResource;

public interface StackResourceMapper {

	int deleteByPrimaryKey(String id);

	int deleteByStackId(String stackId);

	int insert(StackResource record);

	StackResource selectByPrimaryKey(String id);

	int updateByPrimaryKey(StackResource record);

	List<StackResource> selectByStackId(String stackId);
}