package com.cloud.cloudapi.service.openstackapi.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.KeypairMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Keypair;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.KeypairService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("keypairService")
public class KeypairServiceImpl implements KeypairService {

	@Autowired
	private KeypairMapper keypairMapper;

	@Autowired
	private CloudConfig cloudconfig;

	@Resource
	private AuthService authService;
	
	public KeypairMapper getKeypairMapper() {
		return keypairMapper;
	}

	public void setKeypairMapper(KeypairMapper keypairMapper) {
		this.keypairMapper = keypairMapper;
	}

	public CloudConfig getCloudconfig() {
		return cloudconfig;
	}

	public void setCloudconfig(CloudConfig cloudconfig) {
		this.cloudconfig = cloudconfig;
	}

	@Resource
	private OSHttpClientUtil httpClient;

	private Logger log = LogManager.getLogger(KeypairServiceImpl.class);
	
	@Override
	public List<Keypair> getKeypairList(Map<String, String> paramMap, TokenOs ostoken)
			throws BusinessException {
        
		int limitItems = Util.getLimit(paramMap);
		List<Keypair> keypairsFromDB = null;
		if(-1 == limitItems){
			keypairsFromDB = keypairMapper.selectAllByTenantId(ostoken.getTenantid());
		}else{
			keypairsFromDB = keypairMapper.selectAllByTenantIdWithLimit(ostoken.getTenantid(),limitItems);
		}
		if (!Util.isNullOrEmptyList(keypairsFromDB)){
			return keypairsFromDB;
		}
			
		// todo 1: 通过guitokenid 取得实际，用户信息
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion();// we should get the regioninfo by the
									// guiTokenId

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/os-keypairs", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
        Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = httpClient.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		List<Keypair> keypairs = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				keypairs = getKeypairs(rs);
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
			if (httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				keypairs = getKeypairs(rs);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_KEYPAIR_GET_FAILED,httpCode,locale);
		}
		if(!Util.isNullOrEmptyList(keypairs)){
			for (Keypair keypair : keypairs) {
				keypair.setTenantId(ostoken.getTenantid());
			}
		}
			
		storeKeypairs2DB(keypairs);
		return getLimitItems(keypairs,ostoken.getTenantid(),limitItems);
	}

	private List<Keypair> getLimitItems(List<Keypair> keypairs,String tenantId,int limit){
		if(Util.isNullOrEmptyList(keypairs))
			return null;
		List<Keypair> tenantKeypairs = new ArrayList<Keypair>();
		for(Keypair keypair : keypairs){
			if(!tenantId.equals(keypair.getTenantId()))
				continue;
			tenantKeypairs.add(keypair);
		}
		if(-1 != limit){
			if(limit <= tenantKeypairs.size())
				return tenantKeypairs.subList(0, limit);
		}
		return tenantKeypairs;
	}
	
	private void storeKeypairs2DB(List<Keypair> keypairs) {
		if (Util.isNullOrEmptyList(keypairs))
			return;
		for (Keypair keypair : keypairs) {
			if (null != keypairMapper.selectByName(keypair.getName()))
				keypairMapper.updateByPrimaryKeySelective(keypair);
			else
				keypairMapper.insertSelective(keypair);
		}
	}

	private Keypair makeKeypair(String createBody,TokenOs ostoken) throws BusinessException{
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/os-keypairs", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		// String body = generateBody(paraMap);
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = httpClient.httpDoPost(url, headers, createBody);
		Util.checkResponseBody(rs,locale);

		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));

		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		
		Keypair keypair = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				keypair = genPrivateKey(rs);
			} catch (Exception e) {
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
			rs = httpClient.httpDoPost(url, headers, createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				keypair = genPrivateKey(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_COMPUTE_KEYPAIR_CREATE_FAILED,httpCode,locale);
		}
		
		return keypair;
	}
	
	@Override
	public Keypair createKeypair(String createBody, TokenOs ostoken)
			throws BusinessException, JsonProcessingException, IOException {
		// todo 1: 通过guitokenid 取得实际，用户信息
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		String keypairCreateBody = makeCreateBody(createBody,ostoken,false);
		Keypair keypair = makeKeypair(keypairCreateBody,ostoken);
		if(null == keypair)
			throw new ResourceBusinessException(Message.CS_COMPUTE_KEYPAIR_CREATE_FAILED,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		keypair.setId(Util.makeUUID());
		keypair.setMillionSeconds(Util.getCurrentMillionsecond());
		keypair.setTenantId(ostoken.getTenantid());
		
		String privateKey = keypair.getPrivate_key();
		if(!Util.isNullOrEmptyValue(privateKey)){
			privateKey = privateKey.replace("\n", "\\n");
			keypair.setPrivate_key(privateKey);	
		}
		keypairMapper.insertSelective(keypair);
		return keypair;
	}

	@Override
	public Keypair uploadKeypair(String createBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException{

		String keypairCreateBody = makeCreateBody(createBody,ostoken,true);
		Keypair keypair = makeKeypair(keypairCreateBody,ostoken);
		if(null == keypair)
			throw new ResourceBusinessException(Message.CS_COMPUTE_KEYPAIR_CREATE_FAILED,new Locale(ostoken.getLocale()));
		keypair.setId(Util.makeUUID());
	//	keypair.setCreatedAt(Util.getCurrentDate());
		keypair.setMillionSeconds(Util.getCurrentMillionsecond());
		keypair.setTenantId(ostoken.getTenantid());
		
		String privateKey = keypair.getPrivate_key();
		if(!Util.isNullOrEmptyValue(privateKey)){
			privateKey = privateKey.replace("\n", "\\n");
			keypair.setPrivate_key(privateKey);
		}
	     
		keypairMapper.insertSelective(keypair);
		return keypair;
	}
	
	@Override
	public Keypair getKeypair(String keypairName, TokenOs ostoken)
			throws BusinessException {
		Keypair keypair = keypairMapper.selectByName(keypairName);
	    if (null != keypair){
	    	return keypair;
	    }
	    
	    keypair = refreshKeypair(keypairName,ostoken);
		storeKeypair2DB(keypair);
		return keypair;
	}

	private Keypair refreshKeypair(String keypairName, TokenOs ostoken) throws BusinessException {
		
		Keypair keypair = null;
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/os-keypairs/");
		sb.append(keypairName);
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
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				JsonNode keypairNode = rootNode.path(ResponseConstant.KEYPAIR);
				keypair = getKeypairInfo(keypairNode);
			} catch (Exception e) {
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
			if (httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				JsonNode keypairNode = rootNode.path(ResponseConstant.KEYPAIR);
				keypair = getKeypairInfo(keypairNode);
			}  catch (Exception e) {
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_KEYPAIR_DETAIL_GET_FAILED,httpCode,locale);
		}
		return keypair;
	}
	
	@Override
	public Keypair downloadKeypair(String keypairName,TokenOs ostoken) throws BusinessException, IOException{
		Keypair keypair =  keypairMapper.selectByName(keypairName);
	    if (null == keypair)
	    	throw new ResourceBusinessException(Message.CS_COMPUTE_KEYPAIR_NOT_EXISTING,new Locale(ostoken.getLocale()));
	    return keypair;
	}
	
	@Override
	public String deleteKeypair(String keypairName, TokenOs ostoken)
			throws BusinessException {
		
		Locale locale = new Locale(ostoken.getLocale());
		checkRelatedResource(keypairName,locale);
		
		// token should have Regioninfo
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/os-keypairs/");
		sb.append(keypairName);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = httpClient.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_KEYPAIR_DELETE_FAILED,httpCode,locale);
		}
		
		Keypair deletedKeyPair = keypairMapper.selectByName(keypairName);
		String keypairId = deletedKeyPair.getId();
		keypairMapper.deleteByPrimaryKey(keypairId);
		return keypairId;
	}

//	private String generateBody(Map<String, String> paraMap) {
//		if (null == paraMap || 0 == paraMap.size())
//			return "";
//
//		ObjectMapper mapper = new ObjectMapper();
//		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
//		mapper.setSerializationInclusion(Include.NON_NULL);
//		mapper.setSerializationInclusion(Include.NON_EMPTY);
//		KeypairJSON keypair = null;
//		if (1 == paraMap.size())
//			keypair = new KeypairJSON(new Keypair(paraMap.get(ParamConstant.NAME)));
//		else
//			keypair = new KeypairJSON(
//					new Keypair(paraMap.get(ParamConstant.NAME), paraMap.get(ParamConstant.PUBLIC_KEY)));
//		String jsonStr = "";
//		try {
//			jsonStr = mapper.writeValueAsString(keypair);
//		} catch (JsonProcessingException e) {
//			// TODO Auto-generated catch block
//			log.error(e);
//		}
//		return jsonStr;
//	}

	private Keypair genPrivateKey(Map<String, String> rs)
			throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode keypairNode = rootNode.path(ResponseConstant.KEYPAIR);
//		int keypairProCount = keypairNode.size();
//		if (5 != keypairProCount)
//			return null;
		Keypair keypair = new Keypair();
		keypair.setId(keypairNode.path(ResponseConstant.ID).textValue());
		keypair.setName(keypairNode.path(ResponseConstant.NAME).textValue());
		keypair.setPublic_key(keypairNode.path(ResponseConstant.PUBLIC_KEY).textValue());
		keypair.setFingerprint(keypairNode.path(ResponseConstant.FINGERPRINT).textValue());
		keypair.setUser_id(keypairNode.path(ResponseConstant.USER_ID).textValue());
//		String keyName = keypairNode.path(ResponseConstant.NAME).textValue();
		JsonNode privateKeyNode = keypairNode.path(ResponseConstant.PRIVATE_KEY);
		if(privateKeyNode.isMissingNode())
			return keypair;
		String privateKey = privateKeyNode.textValue();
	//	keyFilePath = keyFilePath.concat(keyName).concat(".pem");
	//	if (false == saveKeyFile(keyFilePath, privateKey))
	//		return null;
		keypair.setPrivate_key(privateKey);
	
		return keypair;
	}

	/*
	private boolean saveKeyFile(String keyPath, String privateKey) {
		if ("".equals(privateKey))
			return false;
		String filePath = keyPath;//keyPath.concat("/key.pem");
		try {
			// 获得文件对象
			File Keyfile = new File(filePath);
			if (!Keyfile.exists()) {
				if (!Keyfile.getParentFile().exists())
					Keyfile.getParentFile().mkdirs();
				Keyfile.createNewFile();
			}
			FileWriter resultFile = new FileWriter(filePath);
			PrintWriter myFile = new PrintWriter(resultFile);
			myFile.println(privateKey);
			resultFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
			return false;
		}
		return true;
	}*/

	/*
	private String downloadFile(String keyPath) throws IOException {
		if (Util.isNullOrEmptyValue(keyPath))
			return null;
	//	String fileName = keyPath.substring(keyPath.lastIndexOf("/") + 1);
	//	response.setHeader("content-disposition", "attachment;filename=" + fileName);
		try {
			FileInputStream in = new FileInputStream(keyPath);
			int size = in.available();
			byte[] buffer = new byte[size];
			in.read(buffer);
			in.close();
			return new String(buffer);

		} catch (IOException e) {
			log.error(e);
			return null;
		}
	}*/
	
	private List<Keypair> getKeypairs(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode keypairsNode = rootNode.path(ResponseConstant.KEYPAIRS);
		int keypairsCount = keypairsNode.size();
		if (0 == keypairsCount)
			return null;
		List<Keypair> keypairs = new ArrayList<Keypair>();
		for (int index = 0; index < keypairsCount; ++index) {
			JsonNode keypairNode = keypairsNode.get(index).path(ResponseConstant.KEYPAIR);
			Keypair keypairInfo = getKeypairInfo(keypairNode);
			if(null == keypairInfo)
				continue;
			keypairs.add(keypairInfo);
		}
		return keypairs;
	}

	private Keypair getKeypairInfo(JsonNode keypairNode) {
		if (null == keypairNode)
			return null;
		Keypair keypairInfo = new Keypair();
		keypairInfo.setId(Integer.toString(keypairNode.path(ResponseConstant.ID).intValue()));
		keypairInfo.setName(keypairNode.path(ResponseConstant.NAME).textValue());
		keypairInfo.setFingerprint(keypairNode.path(ResponseConstant.FINGERPRINT).textValue());
		keypairInfo.setPublic_key(keypairNode.path(ResponseConstant.PUBLIC_KEY).textValue());
		keypairInfo.setUser_id(keypairNode.path(ResponseConstant.USER_ID).textValue());
	   //	keypairInfo.setCreatedAt(keypairNode.path(ResponseConstant.CREATED_AT).textValue());
		
		return keypairInfo;
	}

	private void storeKeypair2DB(Keypair keypair) {
		if (null != keypairMapper.selectByName(keypair.getName()))
			keypairMapper.updateByPrimaryKeySelective(keypair);
		else
			keypairMapper.insertSelective(keypair);
	}
	
	private String makeCreateBody(String createBody,TokenOs ostoken,Boolean upload) throws JsonProcessingException, IOException, BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(createBody);
		String name = rootNode.path(ResponseConstant.NAME).textValue();
		checkKeypairName(name,ostoken);
		String publicKey = null;
		if(true == upload)
			publicKey = rootNode.path(ResponseConstant.PUBLIC_KEY).textValue();
		StringBuilder sb = new StringBuilder();
		sb.append("{\"keypair\":{");
		sb.append("\"name\":\"");
		sb.append(name);
		sb.append("\"");
		if(true == upload){
			sb.append(",");
			sb.append("\"public_key\":\"");
			sb.append(publicKey);
			sb.append("\"");
		}
		sb.append("}}");
		return sb.toString();
	}
	
	private void checkRelatedResource(String name,Locale locale) throws BusinessException{
		Keypair keypair = keypairMapper.selectByName(name);
		if(null == keypair)
			return;
		if(!Util.isNullOrEmptyValue(keypair.getInstanceId()))
			throw new ResourceBusinessException(Message.CS_HAVE_RELATED_INSTANCE_RESOURCE,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
        return;
	}
	
	private void checkKeypairName(String name,TokenOs ostoken) throws BusinessException{
		List<Keypair> keypairs = keypairMapper.selectList();
		if(Util.isNullOrEmptyList(keypairs))
			return;
		for(Keypair keypair : keypairs){
			if(keypair.getName().equals(name))
				throw new ResourceBusinessException(Message.CS_COMPUTE_KEYPAIR_CREATE_FAILED_WITH_NAME,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,new Locale(ostoken.getLocale()));	
		}
		return;
	}
	
}
