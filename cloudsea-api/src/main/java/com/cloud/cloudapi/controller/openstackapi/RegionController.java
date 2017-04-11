package com.cloud.cloudapi.controller.openstackapi;

import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.cloud.cloudapi.controller.common.BaseController;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.Region;
import com.cloud.cloudapi.pojo.common.TokenGui;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.service.openstackapi.RegionService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;

@RestController
public class RegionController  extends BaseController {

	@Resource
	private RegionService regionService;
	
	private Logger log = LogManager.getLogger(RegionController.class);
	
	@RequestMapping(value = "/regions", method = RequestMethod.GET)
	public String getRegionList(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,HttpServletResponse response) {
		//get ostoken by cuibl
		List<Region> list=null;
		String rsMessage = "success";
		boolean hasException = false;
		TokenOs authToken = null;
		try{
		  authToken = this.getAuthService().insertCheckGuiAndOsTokenByEncrypt(guiToken);
		  list=regionService.getRegionListFromDb(new Locale(authToken.getLocale()));
		  log.debug("get region list size:jsonboy-" +list.size());
		  if(list==null||list.size()==0){
		//	TokenOs authToken = this.getAuthService().insertCheckGuiAndOsTokenByEncrypt(guiToken);
			list=regionService.getRegionListFromOs(authToken);
			regionService.insertRegionListToDb(list);
		  }
		}catch (Exception e){
			hasException = true;
			// TODO Auto-generated catch block
			rsMessage= new ResourceBusinessException("CS_REGION_GET_0001",new Locale(authToken.getLocale())).getMessage();
			log.error(rsMessage,e);
		}
		
		if(hasException){
            response.setStatus(500);
        }else{
            rsMessage= new JsonHelper<List<Region>,String>().generateJsonBodySimple(list,ResponseConstant.REGIONS);        	
        }
  		return rsMessage;
	}
	
	/**
	 *更新当前Region 
	 */
	@RequestMapping(value = "/regions/{id}", method = RequestMethod.PUT)
	public String updateRegion(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		
		String rsMessage = "success";		
		TokenGui authToken=null;
		TokenOs osToken=null;

		boolean hasException = false;
		boolean isSuccess =false;
		try{
			authToken = this.getUserGuiToken(guiToken);	
		    osToken = this.getUserOsToken(guiToken);
		    if(null == osToken || null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
		    this.checkUserPermission(osToken, ParamConstant.COMMON_UPDATE_REGION);
			String token = regionService.updateCurrentRegionToDb(authToken, id);
			if(null != token){
				return new JsonHelper<String,String>().generateJsonBodySimple(token,"token");
			}
		}catch (Exception e){
			hasException = true;
			log.error("update current  region failed",e);		
		}
		
		if(hasException||!isSuccess){
            response.setStatus(500);
            rsMessage= new JsonHelper<String,String>().generateJsonBodySimple(new ResourceBusinessException("CS_REGION_UPDATE_NOW_0001",new Locale(authToken.getLocale())).getMessage(),"status");        	
        }
  		return rsMessage;
	}
	
}
