package com.cloud.cloudapi.service.openstackapi.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.CloudUserMapper;
import com.cloud.cloudapi.dao.common.DomainTenantUserMapper;
import com.cloud.cloudapi.dao.common.RegionMapper;
import com.cloud.cloudapi.dao.common.TokenGuiMapper;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.pojo.common.DomainTenantUser;
import com.cloud.cloudapi.pojo.common.Region;
import com.cloud.cloudapi.pojo.common.TokenGui;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.service.common.CloudUserService;
import com.cloud.cloudapi.service.openstackapi.RegionService;
import com.cloud.cloudapi.util.JWTTokenHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RegionServiceImpl implements RegionService{
	@Resource
	private CloudConfig cloudconfig;
	
	@Resource
	private RegionMapper regionMapper;
	
	@Resource
	private TokenGuiMapper tokenGuiMapper;
	
	@Resource
	private CloudUserMapper cloudUserMapper;	
	
	@Resource
	private DomainTenantUserMapper domainTenantUserMapper;

    @Resource
    private CloudUserService  cloudUserService;
    
	@Resource
	private OSHttpClientUtil httpClient;
	
	private Logger log = LogManager.getLogger(RegionServiceImpl.class);	
	
    private final int ERROR_HTTP_CODE=400;	
	
    @Override
	public List<Region> getRegionListFromOs(TokenOs adminToken) throws Exception {
		String url_teant=RequestUrlHelper.urlPlus(cloudconfig.getOs_authurl(),"regions");
		Map<String, String>  rs =httpClient.httpDoGet(url_teant,adminToken.getTokenid());
		
		log.debug("httpcode:"+rs.get("httpcode")); 
		log.debug("jsonbody:"+rs.get("jsonbody")); 
			
		if(Integer.parseInt(rs.get("httpcode")) >= ERROR_HTTP_CODE){
			log.debug("wo cha:get region list request failed"); 
			throw new Exception("create project request failed");
		}		
		List<Region> ll=null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			Locale locale = new Locale(adminToken.getLocale());
			JsonNode rootNode = mapper.readTree(rs.get("jsonbody"));
			JsonNode rolesNode = rootNode.path(ResponseConstant.REGIONS);
			ll = mapper.readValue(rolesNode.toString(),new TypeReference<List<Region>>(){});
			setRegionsName(ll,locale);
		}catch(Exception e){
			// TODO Auto-generated catch block
			log.error(e);
			throw e;
		}
		return ll;
	}
	
	@Override
	public List<Region> getRegionListFromDb(Locale locale) throws Exception {
		List<Region> regions =  regionMapper.selectList();
		setRegionsName(regions,locale);
		return regions;
	}
	
//	@Override
//	public Map updateRegionListToDb(TokenOs adminToken) throws Exception {
//		List<Region> listos=this.getRegionListFromOs(adminToken);
//		List<Region> listdb=getRegionListFromDb(new Locale(adminToken.getLocale()));
//		
//		if(listos==null||listdb==null){
//		 throw new Exception("get region failed");	
//		}
//		List<Region> listAdd=null;
//		List<Region> listDel=null;
//		for(Region oneos:listos){
//			boolean same=false;
//			for(Region onedb:listdb){		 
//		     if(oneos.getId().equals(onedb.getId())){
//		    	same=true;
//				break; 
//			 }     
//			}
//			//如现有数据库没有此Region，则插入此Region到数据库
//			if(!same){
//				if("".equals(oneos.getDescription())){
//					oneos.setDescription(oneos.getId());
//				}
//				regionMapper.insertSelective(oneos);
//				if(listAdd==null){
//					listAdd=new ArrayList<Region>();
//				}
//				listAdd.add(oneos);
//			}
//		}
//		
//		for(Region onedb:listdb){
//			boolean same=false;
//			for(Region oneos:listos){			 
//		     if(oneos.getId().equals(onedb.getId())){
//		    	same=true;
//				break; 
//			 }
//		     
//			}	
//			//如果从Openstack中获取的regionList已经没有此Region则删除
//			if(!same){
//				regionMapper.deleteByPrimaryKey(onedb.getId());
//				if(listDel==null){
//					listDel=new ArrayList<Region>();
//				}
//				listDel.add(onedb);				
//			}			
//		}
//		
//		HashMap<String,List<Region>> map=new  HashMap<String,List<Region>>();
//		map.put("add", listAdd);
//		map.put("delete", listDel);
//		return map;
//	}
		
	@Override
	public boolean updateCurrentRegionToDb(TokenGui guiToken, Region currentRegion) throws Exception {
		// TODO Auto-generated method stub
		DomainTenantUser tenantUser = domainTenantUserMapper.selectByPrimaryKey(guiToken.getTenantuserid());
		if(null == tenantUser)
			return false;
		CloudUser user = cloudUserMapper.selectByPrimaryKey(tenantUser.getClouduserid());
		if(null == user)
			return false;
		user.setCurrentregion(currentRegion.getId());
		cloudUserMapper.updateByPrimaryKeySelective(user);
	//	guiToken.setCurrentRegion(currentRegion.getId());
		return true;
	}
	
	@Override
	public String updateCurrentRegionToDb(TokenGui guiToken, String currentRegionId) {
		// TODO Auto-generated method stub
		DomainTenantUser tenantUser = domainTenantUserMapper.selectByPrimaryKey(guiToken.getTenantuserid());
		if(null == tenantUser)
			return null;
		CloudUser user = cloudUserMapper.selectByPrimaryKey(tenantUser.getClouduserid());
		if(null == user)
			return null;
		user.setCurrentregion(currentRegionId);
		cloudUserMapper.updateByPrimaryKeySelective(user);
	//	guiToken.setCurrentRegion(currentRegionId);
		guiToken.setCurrentRegion(currentRegionId);
		return JWTTokenHelper.createEncryptToken(guiToken,user,cloudUserService.checkIsAdmin(tenantUser.getClouduserid()));
	}

	@Override
	public Region getRegionByIdFromDb(String regionId,Locale locale) throws Exception {
		// TODO Auto-generated method stub
		Region region = regionMapper.selectByPrimaryKey(regionId);
		setRegionName(region,locale);
		return region;
		
	}

	@Override
	public boolean insertRegionListToDb(List<Region> regionlist) throws Exception {
		// TODO Auto-generated method stub
		if(regionlist==null) return false;
		for(Region oneRegion:regionlist){
			if("".equals(oneRegion.getDescription())){
				oneRegion.setDescription(oneRegion.getId());
			}			
			regionMapper.insertSelective(oneRegion);
		}
		return true;
	}
	
	private void setRegionName(Region region,Locale locale){
		if(null == region)
			return;
		String regionName = region.getDescription();
		try{
			regionName = Message.getMessage(region.getId().toUpperCase(),locale,false);	
		}catch (Exception e){
			regionName = Message.getMessage("DEFAULTREGION",locale,false);	
		}
		region.setName(regionName);	
	}
	
	private void setRegionsName(List<Region> regions,Locale locale){
		if(Util.isNullOrEmptyList(regions))
			return;
		for(Region region : regions){
			setRegionName(region,locale);
		}
	}
}
