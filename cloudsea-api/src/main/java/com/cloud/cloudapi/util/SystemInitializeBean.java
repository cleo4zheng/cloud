package com.cloud.cloudapi.util;

import java.util.Locale;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.cloud.cloudapi.dao.businessapi.WorkflowFlowMapperDao;
import com.cloud.cloudapi.dao.common.CloudRoleMapper;
import com.cloud.cloudapi.dao.common.CloudUserMapper;
import com.cloud.cloudapi.dao.common.DomainTenantUserMapper;
import com.cloud.cloudapi.dao.common.TenantEndpointMapper;
import com.cloud.cloudapi.dao.common.TenantMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.CloudRole;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.service.businessapi.zabbix.ZabbixService;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.CloudUserService;
import com.cloud.cloudapi.service.openstackapi.CloudServiceService;
import com.cloud.cloudapi.service.openstackapi.FlavorService;
import com.cloud.cloudapi.service.openstackapi.HostService;
import com.cloud.cloudapi.service.openstackapi.ImageService;
import com.cloud.cloudapi.service.openstackapi.NetworkService;
import com.cloud.cloudapi.service.openstackapi.PortService;
import com.cloud.cloudapi.service.openstackapi.QuotaService;
import com.cloud.cloudapi.service.openstackapi.RouterService;
import com.cloud.cloudapi.service.openstackapi.SubnetService;
import com.cloud.cloudapi.service.openstackapi.VolumeTypeService;
import com.cloud.cloudapi.service.rating.RatingTemplateService;
import com.cloud.cloudapi.workflow.WorkFlowConstant;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 用于系统初始化,当系统第一次启动时开始初始化,主要步骤如下: 1.建立系统admin权限(超级管理员) 2.建立系统管理员账号
 * 3.绑定openstack管理员权限 4.绑定workflow系统的超级管理员权限 5.初始化计费模块数据 6.初始化工作流模块中流程对应关系
 * 
 * @author wangw
 * @create 2016年9月8日 上午11:18:46
 * 
 */
public class SystemInitializeBean {
	private static Logger log = LogManager.getLogger(SystemInitializeBean.class);

	@Resource
	private CloudRoleMapper cloudRoleMapper;

	@Resource
	private CloudUserMapper cloudUserMapper;

	@Resource
	private DomainTenantUserMapper domainTenantUserMapper;

	@Resource
	private TenantMapper tenantMapper;

	@Resource
	private CloudConfig cloudconfig;

	@Resource
	private TenantEndpointMapper tenantEndpointMapper;

	@Resource
	private CloudUserService cloudUserServiceImpl;

	@Resource
	private ZabbixService zabbixService;

	@Resource
	private RatingTemplateService ratingTemplateService;

	@Resource
	private HostService hostService;

	@Resource
	private NetworkService networkService;

	@Resource
	private SubnetService subnetService;

	@Resource
	private VolumeTypeService volumeTypeService;

	@Resource
	private FlavorService flavorService;

	@Resource
	private ImageService imageService;

	@Resource
	private RouterService routerService;

	@Resource
	private PortService portService;

	@Resource
	private AuthService authService;

	@Resource
	private QuotaService quotaService;
	
	@Resource
	private CloudServiceService serviceService;

	@Resource
	private WorkflowFlowMapperDao workflowFlowMapperDao;


	public void initialize() throws BusinessException {
		log.info("系统初始化开始: 创建 admin 用户开始!");
		// 判断是否已经初始化完成
		if (checkIsInited()) {
			log.info("系统已经执行过初始化,初始化取消!");
			return;
		}

		try {
			// 插入用户
			log.info("系统初始化: 初始化中间层模块开始!");
			TokenOs adminToken = null;
			try {
				adminToken = authService.createDefaultAdminOsToken();
			} catch (Exception e) {
				throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,
						ParamConstant.SERVICE_ERROR_RESPONSE_CODE, new Locale(cloudconfig.getSystemDefaultLocale()));
			}
			authService.createDefaultUserRole();
			authService.createAdminUser();
			serviceService.initServices(adminToken);
			volumeTypeService.getVolumeTypeList(null, adminToken);
			hostService.getHostList(null, adminToken);
			hostService.getHostAggregates(null, adminToken);
			networkService.getNetworkList(null, adminToken);
			imageService.getImageList(null, adminToken);
			
			
			ratingTemplateService.initRatingTemplate(null, adminToken);
			quotaService.createSystemTemplate(adminToken);
			ratingTemplateService.createSystemTemplate(adminToken);
			quotaService.createDefaultQuota(adminToken);
			flavorService.getFlavorList(null, adminToken);
			routerService.getRouterList(null, adminToken);
			portService.getPortList(null, adminToken, false);
			subnetService.getSubnetList(null, adminToken);
			
			log.info("系统初始化: 初始化中间层模块结束!");

			/*log.info("系统初始化: 初始化workflow映射表开始!");
			URL filePath = SystemInitializeBean.class.getClassLoader().getResource(WorkFlowConstant.WF_CONFIG_FILE_PaaS);
	        File configFile;
	        if(filePath != null){
	        	configFile = new File(filePath.getPath());
	        }else{
	        	filePath = SystemInitializeBean.class.getClassLoader().getResource(WorkFlowConstant.WF_CONFIG_FILE_IaaS);
	        	configFile = new File(filePath.getPath());
	        }
	        ObjectMapper mapper = new ObjectMapper();
	        ArrayList<HashMap> list = mapper.readValue(configFile, ArrayList.class);
	        for (HashMap map : list) {  
	        	WorkflowFlowMapper wfMapper = new WorkflowFlowMapper();
	        	wfMapper.setId(Util.makeUUID());
	        	wfMapper.setHttpUrl(StringHelper.objectToString(map.get("http_url")));
	        	wfMapper.setHttpMethod(StringHelper.objectToString(map.get("http_method")));
	        	wfMapper.setFlowName(StringHelper.objectToString(map.get("flow_name")));
	        	wfMapper.setJbpmProcessDefId(StringHelper.objectToString(map.get("jbpm_processDefId")));
	        	workflowFlowMapperDao.insertSelective(wfMapper);
	          
	        } 
	        log.info("系统初始化: 初始化workflow映射表结束!");
	        */
		     }catch(Exception e){
			log.error("系统初始化失败!" + e.getMessage(),e);
		     }
}

	/**
	 * 检查系统是否已经被初始化了 根据系统是否有管理员权限来判断
	 * 
	 * @return
	 */
	private boolean checkIsInited() {

		CloudRole cloudRole = cloudRoleMapper.selectByRoleName(Util.ADMIN_ROLE_NAME);
		if (null != cloudRole)
			return true;

		int num = this.cloudUserMapper.countNumByUserAccount(cloudconfig.getSystemAdminDefaultAccount());
		if (num != 0)
			return true;

		return false;

	}
}
