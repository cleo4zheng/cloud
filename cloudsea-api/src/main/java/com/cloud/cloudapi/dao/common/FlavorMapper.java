package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Flavor;


/** 
* @author  wangw
* @create  2016年5月31日 上午11:52:51 
* 
*/
public interface FlavorMapper extends SuperMapper<Flavor,String> {
	
	public Flavor selectByName(String name);
	
	public List<Flavor> selectAll();

	public List<Flavor> selectByType(String type);
	
	public List<Flavor> selectListWithLimit(int limit);
	
	public List<Flavor> selectAllForPage(int start, int end);
	
	public List<Flavor> selectByIds(String[] ids);
	
}
