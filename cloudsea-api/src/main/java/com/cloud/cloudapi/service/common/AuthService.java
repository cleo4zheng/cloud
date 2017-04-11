package com.cloud.cloudapi.service.common;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.pojo.common.TokenGui;
import com.cloud.cloudapi.pojo.common.TokenOs;
/**
 * 
 * @author cuibl@cn.fujitsu.com
 *
 */
public interface AuthService {
	/**
	 * 用户登录验证
	 * @param user 用户信息：主要是用户名和密码
	 * @return TokenGui 用户名密码验证登陆成功的话，返回新生成tokengui对象
	 * @throws Exception 登陆失败发生异常的的话抛出异常信息
	 */
	public TokenGui insertLogin(CloudUser user)throws ResourceBusinessException,BusinessException,Exception;
	
	/**
	 * 用户注销登陆
	 * @param guitokenid 
	 * @return 删除guitokenid: true,失败：flase
	 * @throws Exception 注销失败发生异常的的话抛出异常信息
	 */
	public  boolean deleteLogout(String guitokenid)throws ResourceBusinessException,BusinessException,Exception;	
	
	
	/**
	 * 检查所给guiTokenid是否有效,并返回guiToken.
	 * 主要给不涉及openstack交互的操作使用
	 *  -如果输入guitokenid 不存在，或已过期，则抛出异常
	 *  -其它返回从数据库中取到的guitoken对象
	 * @param guitokenid
	 * @return TokenGui
	 */
	public TokenGui  selectCheckGui(String guitokenid)throws ResourceBusinessException,BusinessException,Exception;
	
	/**
	 * 根据从GUI传过来的加密token，检查所给guiTokenid是否有效,并返回guiToken.
	 * 主要给不涉及openstack交互的操作使用
	 *  -如果输入guitokenid 不存在，或已过期，则抛出异常
	 *  -其它返回从数据库中取到的guitoken对象
	 * @param guitokenid
	 * @return TokenGui
	 */
	public TokenGui  selectCheckGuiByEncrypt(String encryptToken)throws ResourceBusinessException,BusinessException,Exception;
	

	/**
	 * 检查所给guiTokenid是否有效，并返回osToken
	 * 主要给涉及openstack交互的操作使用
	 *  -如果输入guitokenid 不存在，或已过期，则抛出异常
	 *  -其它返回从数据库中取到的OpenstackToken对象
	 *   #如果openstackToken不存在或对象已过期，则重新生成，并把新的token保存到数据库
	 * @param guitokenid
	 * @return TokenOs
	 */
	public TokenOs  insertCheckGuiAndOsToken(String guitokenid)throws ResourceBusinessException,Exception;
	

	/**
	 * 根据从GUI传过来的加密token，检查所给guiTokenid是否有效，并返回osToken
	 * 主要给涉及openstack交互的操作使用
	 *  -如果输入guitokenid 不存在，或已过期，则抛出异常
	 *  -其它返回从数据库中取到的OpenstackToken对象
	 *   #如果openstackToken不存在或对象已过期，则重新生成，并把新的token保存到数据库
	 * @param guitokenid
	 * @return TokenOs
	 */
	public TokenOs  insertCheckGuiAndOsTokenByEncrypt(String encryptToken)throws ResourceBusinessException,BusinessException,Exception;
	
	public TokenOs createNewToken(String userId,String region,String locale) throws Exception;
	/**
	 * 根据guitokenid 获取ostoken
	 *  -如果输入guitokenid 不存在，或已过期，则抛出异常
	 *  -其它返回从数据库中取到的OpenstackToken对象
	 *   #如果openstackToken对象已过期，则重新生成
	 * @param guitokenid
	 * @return TokenOs
	 */
	public TokenOs  insertOsTokenByGuiId(String guitokenid)throws ResourceBusinessException,BusinessException,Exception;
	
	
	/**
	 * 根据ostokenid 获取ostoken
	 *  -如果输入ostokenid 存在且不过期，返回从数据库中取到的OpenstackToken对象
	 *  -如果输入ostokenid 存在且者过期，则重新发行token，并更新数据库 
	 *  -如果输入ostokenid 不存在，则抛出异常
	 * @param ostokenid
	 * @return TokenOs
	 */
	public TokenOs  insertOsTokenById(String ostokenid)throws ResourceBusinessException,BusinessException,Exception;
	

	/**
	 * 根据ostokenid 获取新发行的osToken
	 * 	-如果输入ostokenid 不存在，则抛出异常
	 *  -如果输入ostokenid 存在，不坚持是否过期直接新建并更新数据库
	 * @param ostokenid
	 * @return TokenOs
	 */
	public TokenOs  insertNewOsTokenById(String ostokenid)throws ResourceBusinessException,BusinessException,Exception;	
	
	/**
	 *开发用：无条件返回 admin用户的osToken
	 * @return TokenOs
	 */
	public TokenOs  createDefaultAdminOsToken()throws BusinessException;
	
	/**
	 * 根据GUI直接传过来的未解密Token获取用户对象
	 * 前提：此方法不检查tokenId有效性。默认为已经被拦截器认证过，为有效token。
	 * @param encryptGuiTokenId
	 * @return
	 * @throws ResourceBusinessException
	 * @throws BusinessException
	 * @throws Exception
	 */
	public CloudUser getCloudUserByEncryptGuiToken(String encryptGuiTokenId)throws ResourceBusinessException,BusinessException,Exception;
	
	/**
	 * 根据GUI直接传过来的未解密Token获取用户账户名
	 * 前提：此方法不检查tokenId有效性。默认为已经被拦截器认证过，为有效token。
	 * @param encryptGuiTokenId
	 * @return username or null
	 * @throws ResourceBusinessException
	 * @throws BusinessException
	 */
	public String getCloudUserNameByEncryptGuiToken(String encryptGuiTokenId);
	
	/**
	 * 根据已解密的guiTokenId获取用户对象
	 * 前提：此方法不检查tokenId有效性。默认为已经被拦截器认证过，为有效token。
	 * @param encryptGuiTokenId
	 * @return
	 * @throws ResourceBusinessException
	 * @throws BusinessException
	 * @throws Exception
	 */
	public CloudUser getCloudUserByGuiTokenId(String guiTokenId)throws ResourceBusinessException,BusinessException,Exception;
	
	/**
	 * 根据osTokenId获取用户对象
	 * @param encryptGuiTokenId
	 * @return
	 * @throws ResourceBusinessException
	 * @throws BusinessException
	 * @throws Exception
	 */
	public CloudUser getCloudUserByOsTokenId(String osTokenId)throws ResourceBusinessException,BusinessException,Exception;
	
	/**
	 * 根据Ostoken获取用户账户名，主要为了兼容老马的操作日志写法
	 * 前提：此方法不检查tokenId有效性。默认为已经被拦截器认证过，为有效token。
	 * @param encryptGuiTokenId
	 * @return username or null
	 * @throws ResourceBusinessException
	 * @throws BusinessException
	 */	
	public String getCloudUserNameByOsToken(TokenOs token);
	
	public CloudUser getUserByGuiToken(String guiToken) throws Exception;
	
	public void checkIsAdmin(TokenOs token) throws ResourceBusinessException;
	/**
	 * 检查旧的密码
	 * @param user
	 * @return
	 */
	public boolean checkOldPassword(CloudUser user)throws ResourceBusinessException;
	
	/**
	 * 修改密码
	 * @param user
	 * @return
	 */
	public boolean modifyPassword(String account ,String newPassword)throws ResourceBusinessException;
	
	public CloudUser createAdminUser();
	public void createDefaultUserRole();
	public String getUserPassword(TokenOs token) throws ResourceBusinessException;
}
