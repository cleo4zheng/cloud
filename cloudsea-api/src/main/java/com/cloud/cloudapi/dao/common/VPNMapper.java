package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.VPN;

public interface VPNMapper extends SuperMapper<VPN, String> {
	public Integer countNum();

	public Integer insertOrUpdate(VPN vpn);
	
	public VPN selectVPNBySubetId(String subnet_id);
	
	public VPN selectVPNByRouterId(String router_id);
	
	public List<VPN> selectByIds(String[] ids);
	
	public List<VPN> selectAll();

	public List<VPN> selectAllByTenantId(String tenant_id);

	public List<VPN> selectListByTenantIds(List<String> ids);

	public List<VPN> selectAllForPage(int start, int end);
}
