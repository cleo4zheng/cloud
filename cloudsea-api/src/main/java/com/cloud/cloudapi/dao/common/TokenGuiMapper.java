package com.cloud.cloudapi.dao.common;
import java.util.List;

import com.cloud.cloudapi.pojo.common.TokenGui;

public interface TokenGuiMapper extends SuperMapper<TokenGui,String>{
	
	public int countNum(String guitokenid);
	
	public  List<TokenGui> selectListByUser(String userid);
	
	public  List<TokenGui> selectListByUserId(String tenantuserid);
	
	public Integer insertOrUpdateBatch(List<TokenGui> tokens);
	
	//根据设定的有效时间，清除所有超出有效期的token
	public int deleteBytime(long nowtime);
	
}
