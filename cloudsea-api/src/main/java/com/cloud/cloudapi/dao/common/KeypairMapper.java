package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Keypair;

public interface KeypairMapper extends SuperMapper<Keypair, String>{
	public Integer countNum();

	public Integer countNumByInstanceStatus(String status);
	
	public Keypair selectByInstanceId(String id); 
	
	public Keypair selectByName(String name);
	
	public Integer insertOrUpdate(Keypair keypair);
	
	public int insertOrUpdateBatch(List<Keypair> keypairs);
	
	public List<Keypair> selectKeypairsById(String[] keypairIds);
	
	public List<Keypair> selectKeypairsByName(String[] names);
	
	public List<Keypair> selectListByInstanceId(String id);
	
	public List<Keypair> selectList();
	
	public List<Keypair> selectAllByTenantId(String tenantId);
	
	public List<Keypair> selectListByTenantIds(List<String> ids);

	public List<Keypair> selectListWithLimit(int limit);
	
	public List<Keypair> selectAllByTenantIdWithLimit(String tenantId,int limit);
	
	public List<Keypair> selectListForPage(int start, int end);
}
