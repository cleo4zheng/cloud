package com.cloud.cloudapi.util.interceptor;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.cloud.cloudapi.dao.common.TokenGuiMapper;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenGui;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.util.JWTTokenHelper;
import com.cloud.cloudapi.util.ParamConstant;

/** 
* @author  wangw
* @create  2016年6月3日 上午10:10:27 
* 拦截器：对请求进行预处理
*      1. 检查从GUI传入的Token是否有效
*/
public class TokenBasedAccessInterceptor extends HandlerInterceptorAdapter {
	
	@Resource
	private AuthService authServiceImpl;
	
	@Resource
	private TokenGuiMapper tokenGuiMapper;
	
	@Resource
	private CloudConfig cloudconfig;
	
	private Logger log = LogManager.getLogger(TokenBasedAccessInterceptor.class);
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
	            Object handler) throws Exception {
		 
		    String guiToken = request.getHeader(ParamConstant.AUTH_TOKEN);
	        if(Util.isNullOrEmptyValue(guiToken)){
		       response.setStatus(ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE);
	           return false;
	        }
	        
	        //检查token是否有效
	        try{
	        	String guitokenId = JWTTokenHelper.getGuiTokenIdFromEncryptToken(guiToken);
	            authServiceImpl.selectCheckGui(guitokenId);
	            //update token created time and expire time
	            TokenGui tokenGui = tokenGuiMapper.selectByPrimaryKey(guitokenId);
			    long nowtime=System.currentTimeMillis();
			    tokenGui.setCreateTime(nowtime);
			    long expiretime=nowtime+cloudconfig.getTimeout_token_cloudapi()*3600*1000;
			    tokenGui.setExpiresTime(expiretime);
			    tokenGuiMapper.updateByPrimaryKeySelective(tokenGui);
	           //TokenGui tokenObj = authServiceImpl.selectCheckGui(guiToken);
	        } catch (ResourceBusinessException e) {
	        	log.error("token don't exist or timeout error:"+e.getMessage());
			    response.setStatus(ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE);
			    writeJsonToResponseBody(response,e.getMessage());
		        return false;	        	
				//throw  e;
			} catch (Exception e) {
				log.error("token don't exist or timeout error:"+e.getMessage());
				//throw new ResourceBusinessException(Message.CS_AUTH_ERROR);
			    response.setStatus(ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE);
			    writeJsonToResponseBody(response,new ResourceBusinessException("CS_AUTH_ERROR_0006").getMessage());
		        return false;					
			}	

	        return true;
	}
	
	private void writeJsonToResponseBody(HttpServletResponse response,String jsonBody){
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json;charset=UTF-8");
		try {
			response.getWriter().write(jsonBody);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error("token don't exist or timeout error:"+e);
		}
	}

}
