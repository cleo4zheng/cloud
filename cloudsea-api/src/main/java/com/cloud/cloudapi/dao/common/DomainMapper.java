package com.cloud.cloudapi.dao.common;
import java.util.List;

import com.cloud.cloudapi.pojo.common.Domain;

public interface DomainMapper extends SuperMapper<Domain,String>{

	public int countNum();
	public Domain selectByName(String name);
	public List<Domain> selectList();
}
