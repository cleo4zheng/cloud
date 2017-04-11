package com.cloud.cloudapi.service.openstackapi.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.ImageMapper;
import com.cloud.cloudapi.dao.common.InstanceMapper;
import com.cloud.cloudapi.dao.common.ResourceCreateProcessMapper;
import com.cloud.cloudapi.dao.common.SyncResourceMapper;
import com.cloud.cloudapi.dao.common.VolumeMapper;
import com.cloud.cloudapi.dao.common.VolumeSnapshotMapper;
import com.cloud.cloudapi.dao.common.VolumeTypeMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Image;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceCreateProcess;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SyncResource;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Volume;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeSnapshot;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeType;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.ImageService;
import com.cloud.cloudapi.service.openstackapi.QuotaService;
import com.cloud.cloudapi.service.openstackapi.ResourceSpecService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("imageService")
public class ImageServiceImpl  implements ImageService {
	@Resource
	private OSHttpClientUtil httpClient;

	@Resource
	private AuthService authService;
	
	@Resource
	private QuotaService quotaService;
	
	@Resource
	private ResourceSpecService resourceSpecService;
	
	@Autowired
	private ImageMapper imageMapper;
	
	@Autowired
	private VolumeMapper volumeMapper;
	
	@Autowired
	private VolumeTypeMapper volumeTypeMapper;
	
	@Autowired
	private VolumeSnapshotMapper volumeSnapshotMapper;
	
	@Autowired
	private InstanceMapper instanceMapper;
	
	@Autowired
	private SyncResourceMapper syncResourceMapper;
	
	@Autowired
	private ResourceCreateProcessMapper resourceCreateProcessMapper;

	@Autowired
	private CloudConfig cloudconfig;
	
	private Logger log = LogManager.getLogger(ImageServiceImpl.class);
	
	public ImageMapper getImageMapper() {
		return imageMapper;
	}

	public void setImageMapper(ImageMapper imageMapper) {
		this.imageMapper = imageMapper;
	}

	public CloudConfig getCloudconfig() {
		return cloudconfig;
	}

	public void setCloudconfig(CloudConfig cloudconfig) {
		this.cloudconfig = cloudconfig;
	}


	private List<Image> getAllImages(Map<String, String> paramMap,TokenOs ostoken) throws BusinessException{
		
		Locale locale = new Locale(ostoken.getLocale());
		TokenOs adminToken = null;
		try{
			adminToken = authService.createDefaultAdminOsToken();	
		}catch(Exception e){
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,locale);
		}
		// todo 1: 通过guitokenid 取得实际，用户信息
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		String region = ostoken.getCurrentRegion();

		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_IMAGE, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2/images", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());

		Map<String, String> rs = httpClient.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		List<Image> images = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				images = getImages(rs);
			}catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = httpClient.httpDoGet(url, headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				images = getImages(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_COMPUTE_IMAGE_GET_FAILED,httpCode,locale);
		}
		
		return images;
	}
	
	@Override 
	public List<Image> getRatingImageList(Map<String, String> paramMap,TokenOs ostoken) throws BusinessException {
		List<Image> imagesFromDB = imageMapper.selectAll();
		
		if(!Util.isNullOrEmptyList(imagesFromDB)){
			return filterRatingImages(imagesFromDB);
		}
		
		imagesFromDB = getAllImages(paramMap,ostoken);
		List<Image> images = storeImages2DB(imagesFromDB);
		return filterRatingImages(images);
	}
	
	@Override
	public Map<String,List<Image>> getImageList(Map<String, String> paramMap,TokenOs ostoken) throws BusinessException {

		int limitItems = Util.getLimit(paramMap);
		List<Image> imagesFromDB = null;
		if(-1 == limitItems){
			imagesFromDB = imageMapper.selectAll();//selectAllInstanceImages();
		}else{
			imagesFromDB = imageMapper.selectListWithLimit(limitItems);//selectInstanceImagesWithLimit(limitItems);
		}
		Locale locale = new Locale(ostoken.getLocale());
		if(!Util.isNullOrEmptyList(imagesFromDB)){
			return classifyImages(imagesFromDB,locale);
		//	return imagesFromDB;
		}
		
		imagesFromDB = getAllImages(paramMap,ostoken);
		List<Image> images = storeImages2DB(imagesFromDB);
		images = getLimitItems(images,ostoken.getTenantid(),limitItems);
		sortImages(images);
		return classifyImages(images,locale);
	}

	@Override
	public Map<String,List<Image>> getPrivateImages(Map<String,String> paramMap,TokenOs ostoken){
		int limitItems = Util.getLimit(paramMap);
		List<Image> imagesFromDB = null;
		if(-1 == limitItems){
			imagesFromDB = imageMapper.selectAllPrivateImages(ostoken.getTenantid());
		}else{
			imagesFromDB = imageMapper.selectPrivateImagesWithLimit(ostoken.getTenantid(),limitItems);
		}
		
		sortImages(imagesFromDB);
		return classifyImages(imagesFromDB,new Locale(ostoken.getLocale()));
	}
	
	@Override
	public List<Image> getImages(String type,TokenOs ostoken){
		List<Image>  imagesFromDB = imageMapper.selectImagesByType(type);
		return imagesFromDB;
	}
	
	@Override
	public Image getImage(String imageId, TokenOs ostoken)
			throws BusinessException {
		Image image = imageMapper.selectByPrimaryKey(imageId);
		if(null != image){
			 return image;
		}
		
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_IMAGE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2/images/");
		sb.append(imageId);
		url = RequestUrlHelper.createFullUrl(sb.toString(), null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = httpClient.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode imageNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				image = getImageInfo(imageNode);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = httpClient.httpDoGet(url, headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode imageNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				image = getImageInfo(imageNode);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_COMPUTE_IMAGE_DETAIL_GET_FAILED,httpCode,locale);
		}

		return image;
	}
	
	@Override
	public VolumeType getImageVolumeType(String imageId,TokenOs ostoken) throws BusinessException{
		Image image = imageMapper.selectByPrimaryKey(imageId);
		if(null == image)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		List<VolumeSnapshot> volumeSnpashots = volumeSnapshotMapper.selectByTenantIdAndName(ostoken.getTenantid(),"snapshot for "+image.getName());
        if(Util.isNullOrEmptyList(volumeSnpashots))
			throw new ResourceBusinessException(Message.CS_PRIVATE_IMAGE_IS_BUILDING,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
        String volumeId = volumeSnpashots.get(0).getVolumeId();
        Volume volume = volumeMapper.selectByPrimaryKey(volumeId);
        if(null == volume)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
        VolumeType volumeType = volumeTypeMapper.selectByName(volume.getVolume_type());
        if(null == volumeType)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
        if(null == volumeType.getDisplayName()){
        	volumeType.setDisplayName(Message.getMessage(volumeType.getName().toUpperCase(), new Locale(ostoken.getLocale()),false));
        }
        volumeType.setName(volumeType.getDisplayName());
        return volumeType;
	}
	
	@Override
	public List<Image> getInstanceImages(String instanceId){
	    List<Image> snapshots = imageMapper.selectInstanceImages(instanceId,true);
	    if(!Util.isNullOrEmptyList(snapshots)){
	    	for(Image image : snapshots){
	    		image.setCreatedAt(Util.millionSecond2Date(image.getMillionSeconds()));
	    	}
	    }
	    return snapshots;
	}

	private void checkResource(String id,Locale locale) throws BusinessException{
		//check instance sync resource
		SyncResource syncResource = syncResourceMapper.selectByPrimaryKey(id);
		if(null != syncResource)
			throw new ResourceBusinessException(Message.CS_RESOURCE_IS_DOING,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
	}
	
	@Override
	public void deleteImage(String imageId,TokenOs ostoken) throws BusinessException{
		// token should have Regioninfo
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(imageId,locale);
		
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_IMAGE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2/images/");
		sb.append(imageId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		Map<String, String> rs = httpClient.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		switch (httpCode) {
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE: 
		case ParamConstant.NORMAL_DELETE_RESPONSE_CODE:{
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = httpClient.httpDoDelete(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		    failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE || httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_COMPUTE_IMAGE_DELETE_FAILED,httpCode,locale);
		}
		
		Image image = imageMapper.selectByPrimaryKey(imageId);
        if(true == image.getPrivateFlag()){
        	updatePrivateImageQuota(ostoken,ParamConstant.IMAGE,false);
        }
		imageMapper.deleteByPrimaryKey(imageId);
		updateRelatedInstanceInfo(imageId);
		
		updateSyncResourceInfo(ostoken.getTenantid(),imageId,null,image.getStatus(),ParamConstant.DELETED_STATUS,ParamConstant.IMAGE,ostoken.getCurrentRegion(),image.getName());

	}
	
	private List<Image> getImages(Map<String, String> rs) throws JsonProcessingException, IOException{
		List<Image> images = new ArrayList<Image>();
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode imagesNode = rootNode.path(ResponseConstant.IMAGES);
		for (int index = 0; index < imagesNode.size(); ++index) {
			Image imageInfo = getImageInfo(imagesNode.get(index));
			if(null == imageInfo)
				continue;
			images.add(imageInfo);
		}
		return images;
	}
	
	private Image getImageInfo(JsonNode imageNode) {
		if (null == imageNode)
			return null;
		Image imageInfo = new Image();
		imageInfo.setId(imageNode.path(ResponseConstant.ID).textValue());
		imageInfo.setName(imageNode.path(ResponseConstant.NAME).textValue());
//		Map<String,Boolean> typeFlag = Util.getImageType(imageInfo.getName());
//		for (Map.Entry<String, Boolean> entry : typeFlag.entrySet()) {
//		   imageInfo.setType(entry.getKey());
//		   imageInfo.setForInstance(entry.getValue());
//		}
		// imageInfo.setTags(imageNode.path(ResponseConstant.TAGS).textValue());
		if(Util.isNullOrEmptyValue(imageNode.path(ResponseConstant.BASE_IMAGE_REF).textValue())){
			imageInfo.setPrivateFlag(false);
		}else{
			imageInfo.setBaseImageId(imageNode.path(ResponseConstant.BASE_IMAGE_REF).textValue());
			imageInfo.setPrivateFlag(true);
		}
		imageInfo.setVisibility(imageNode.path(ResponseConstant.VISIBILITY).textValue());
		imageInfo.setDiskFormat(imageNode.path(ResponseConstant.DISK_FORMAT).textValue());
		imageInfo.setMinDisk(imageNode.path(ResponseConstant.MIN_DISK).intValue());
		imageInfo.setMinRam(imageNode.path(ResponseConstant.MIN_RAM).intValue());
		imageInfo.setSize(imageNode.path(ResponseConstant.SIZE).longValue());
		imageInfo.setFile(imageNode.path(ResponseConstant.FILE).textValue());
		imageInfo.setOwner(imageNode.path(ResponseConstant.OWNER).textValue());
		if(!Util.isNullOrEmptyValue(imageNode.path(ResponseConstant.STATUS).textValue()))
			imageInfo.setStatus(imageNode.path(ResponseConstant.STATUS).textValue().toUpperCase());
		imageInfo.setSystemType(imageNode.path(ResponseConstant.SYSTEM_TYPE).textValue());
		imageInfo.setObjectType(imageNode.path(ResponseConstant.OBJECT_TYPE).textValue());
		if(!imageNode.path(ResponseConstant.RATING).isMissingNode())
			imageInfo.setRating(imageNode.path(ResponseConstant.RATING).booleanValue());
		else
			imageInfo.setRating(false);
		// // SimpleDateFormat dateFormat = new
		// SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		imageInfo.setMillionSeconds(Util.utc2Millionsecond(imageNode.path(ResponseConstant.CREATED_AT).textValue()));
		//imageInfo.setCreatedAt(imageNode.path(ResponseConstant.CREATED_AT).textValue());
		imageInfo.setUpdatedAt(imageNode.path(ResponseConstant.UPDATED_AT).textValue());
		return imageInfo;
	}
	
	private List<Image> storeImages2DB(List<Image> images){
		if(Util.isNullOrEmptyList(images))
			return null;
		
		//List<Image> imagesToResponse = new ArrayList<Image>();
		for(Image image : images){
			if(null != imageMapper.selectByPrimaryKey(image.getId()))
			    imageMapper.updateByPrimaryKeySelective(image);
			else
				imageMapper.insertSelective(image);
			//image.setSize(Util.byte2Mega(image.getSize()));
			//imagesToResponse.add(image);
		}	
		return images;
	}
	
	private List<Image> getLimitItems(List<Image> images,String tenantId,int limit){
		if(Util.isNullOrEmptyList(images))
			return null;
		List<Image> instanceImages = new ArrayList<Image>();
		for(Image image : images){
			if(ParamConstant.POSTGRE.equalsIgnoreCase(image.getSystemType()) || ParamConstant.MYSQL.equalsIgnoreCase(image.getSystemType())
				|| ParamConstant.MONOGODB.equalsIgnoreCase(image.getSystemType()) || ParamConstant.CONTAINER.equalsIgnoreCase(image.getSystemType())
				|| ParamConstant.BAREMETAL.equalsIgnoreCase(image.getSystemType()))
				continue;
			instanceImages.add(image);
		}
		if(-1 != limit){
			if(limit <= instanceImages.size())
				return instanceImages.subList(0, limit);
		}
		return instanceImages;
	}
	
	private void sortImages(List<Image> images){
		if(Util.isNullOrEmptyList(images))
			return;
		Collections.sort(images, new Comparator<Image>() {
			@Override
			public int compare(Image image1, Image image2) {
				return image1.getMillionSeconds().compareTo(image2.getMillionSeconds());
			}
		});
	}
	
	private Map<String,List<Image>> classifyImages(List<Image> images,Locale locale){
		Map<String,List<Image>> normalImages = new HashMap<String,List<Image>>();
	    List<Image> vmwareZoneImages = new ArrayList<Image>();
	    List<Image> kvmZoneImages = new ArrayList<Image>();
	    List<Image> vdiZoneImages = new ArrayList<Image>();
	    List<Image> dbZoneImages = new ArrayList<Image>();
	    List<Image> baremetalZoneImages = new ArrayList<Image>();
	    List<Image> containerZoneImages = new ArrayList<Image>();
		for(Image image : images){
			image.setCreatedAt(Util.millionSecond2Date(image.getMillionSeconds()));
			if(ParamConstant.VMDK.equalsIgnoreCase(image.getDiskFormat())){
				vmwareZoneImages.add(image);
			}
			else{
				if(ParamConstant.MYSQL.equalsIgnoreCase(image.getSystemType())){
					dbZoneImages.add(image);
				}else if(ParamConstant.POSTGRE.equalsIgnoreCase(image.getSystemType())){
					dbZoneImages.add(image);
				}else if(ParamConstant.MONOGODB.equalsIgnoreCase(image.getSystemType())){
					dbZoneImages.add(image);
				}else if(ParamConstant.BAREMETAL.equalsIgnoreCase(image.getSystemType())){
					baremetalZoneImages.add(image);
				}else if(ParamConstant.CONTAINER.equalsIgnoreCase(image.getSystemType())){
					containerZoneImages.add(image);
				}else{
					kvmZoneImages.add(image);
					vdiZoneImages.add(image);
				}
			}
		}
		normalImages.put("allImages", images);
		normalImages.put(Message.getMessage("VMDK-IMAGES-ZONE",locale,false), vmwareZoneImages);
		normalImages.put(Message.getMessage("KVM-IMAGES-ZONE",locale,false), kvmZoneImages);
		normalImages.put(Message.getMessage("VDI-IMAGES-ZONE",locale,false), vdiZoneImages);
		normalImages.put(Message.getMessage("DB-IMAGES-ZONE",locale,false), dbZoneImages);
		normalImages.put(Message.getMessage("BAREMETAL-IMAGES-ZONE",locale,false), baremetalZoneImages);
		normalImages.put(Message.getMessage("CONTAINER-IMAGES-ZONE",locale,false), containerZoneImages);
		return normalImages;
	}
	
	private List<Image> filterRatingImages(List<Image> images){
		if(Util.isNullOrEmptyList(images))
			return null;
		List<Image> filterImages = new ArrayList<Image>();
		for(Image image : images){
			if(null == image.getRating())
				continue;
			if(true == image.getRating())
				filterImages.add(image);
//			if(image.getName().matches("(?i)"+ParamConstant.WINDOWS+".*"))
//				filterImages.add(image);
		}
		return filterImages;
	}

	private void updateRelatedInstanceInfo(String id){
		Instance instance = instanceMapper.selectInstanceByImageId(id);
		if(null == instance)
			return;
		String orgImagesId = instance.getImageIds();
		instance.setImageIds(Util.listToString(Util.getCorrectedIdInfo(orgImagesId, id), ','));
		instanceMapper.insertOrUpdate(instance);
	}
	
	private void updateSyncResourceInfo(String tenantId,String id,String relatedResource,String orgStatus,String expectedStatus,String type,String region,String name){
		SyncResource resource = new SyncResource();
		resource.setId(id);
		resource.setType(type);
		resource.setOrgStatus(orgStatus);
		resource.setExpectedStatus(expectedStatus);
		resource.setRelatedResource(relatedResource);
		resource.setRegion(region);
		syncResourceMapper.insertSelective(resource);
		
		updateResourceCreateProcessInfo(tenantId,id,type,name);
	}
	
	private void updateResourceCreateProcessInfo(String tenantId,String id,String type,String name){
		ResourceCreateProcess createProcess = new ResourceCreateProcess();
		createProcess.setId(id);
		createProcess.setTenantId(tenantId);
		createProcess.setType(type);
		createProcess.setName(name);
		createProcess.setBegineSeconds(Util.time2Millionsecond(Util.getCurrentDate(),ParamConstant.TIME_FORMAT_01));
		resourceCreateProcessMapper.insertOrUpdate(createProcess);
	}
	
	private void updatePrivateImageQuota(TokenOs ostoken,String type,boolean bAdd){
		quotaService.updateQuota(type,ostoken,bAdd,1);
		resourceSpecService.updateResourceSpecQuota(type,ParamConstant.IMAGE,1,bAdd);
	}
	
}
