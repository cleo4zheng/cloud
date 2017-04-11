package com.cloud.cloudapi.service.openstackapi;

import java.util.List;
import java.util.Locale;

import com.cloud.cloudapi.pojo.common.Region;
import com.cloud.cloudapi.pojo.common.TokenGui;
import com.cloud.cloudapi.pojo.common.TokenOs;

public interface RegionService {
	public List<Region> getRegionListFromOs(TokenOs adminToken) throws Exception;
	public List<Region> getRegionListFromDb(Locale locale) throws Exception;
	public Region getRegionByIdFromDb(String regionId,Locale locale) throws Exception;
 //   public Map updateRegionListToDb(TokenOs adminToken) throws Exception;
    public boolean insertRegionListToDb(List<Region> regionlist) throws Exception;
	public boolean updateCurrentRegionToDb(TokenGui guiToken,Region currentRegion) throws Exception;
	public String updateCurrentRegionToDb(TokenGui guiToken,String currentRegionId);
}
