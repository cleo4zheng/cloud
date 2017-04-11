package com.cloud.cloudapi.service.openstackapi;


import com.cloud.cloudapi.service.common.MonitorService;
import com.cloud.cloudapi.service.common.NotificationListService;
import com.cloud.cloudapi.service.common.NotificationService;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.impl.MonitorServiceImpl;
import com.cloud.cloudapi.service.common.impl.NotificationListServiceImpl;
import com.cloud.cloudapi.service.common.impl.NotificationServiceImpl;
import com.cloud.cloudapi.service.common.impl.OperationLogServiceImpl;
import com.cloud.cloudapi.service.openstackapi.impl.BackupServiceImpl;
import com.cloud.cloudapi.service.openstackapi.impl.FlavorServiceImpl;
import com.cloud.cloudapi.service.openstackapi.impl.FloatingIPServiceImpl;
import com.cloud.cloudapi.service.openstackapi.impl.HostServiceImpl;
import com.cloud.cloudapi.service.openstackapi.impl.ImageServiceImpl;
import com.cloud.cloudapi.service.openstackapi.impl.InstanceServiceImpl;
import com.cloud.cloudapi.service.openstackapi.impl.KeypairServiceImpl;
import com.cloud.cloudapi.service.openstackapi.impl.NetworkServiceImpl;
import com.cloud.cloudapi.service.openstackapi.impl.PoolServiceImpl;
import com.cloud.cloudapi.service.openstackapi.impl.QuotaServiceImpl;
import com.cloud.cloudapi.service.openstackapi.impl.RoleServiceImpl;
import com.cloud.cloudapi.service.openstackapi.impl.SubnetServiceImpl;
import com.cloud.cloudapi.service.openstackapi.impl.TenantServiceImpl;
import com.cloud.cloudapi.service.openstackapi.impl.UserServiceImpl;
import com.cloud.cloudapi.service.openstackapi.impl.VolumeServiceImpl;

public class OsApiServiceFactory {
//    @Resource
//    private static TenantService tenantService;
//    
//    @Resource  
//    private static UserService userService;
//    
//    @Resource  
//    private static RoleService roleService; 
    
	public static InstanceService getInstanceService(){
		
		return new InstanceServiceImpl();
	}
	
    public static NetworkService getNetworkService(){
		
		return new NetworkServiceImpl();
	}
	
	public static VolumeService getVolumeService() {
		
		return new VolumeServiceImpl();
	}

	public static ImageService getImageService(){
		
		return new ImageServiceImpl();
	}
	
	public static KeypairService getkeypairService(){
		
		return new KeypairServiceImpl();
	}
	
	public static BackupService getBackupService() {
		
		return new BackupServiceImpl();
	}
	
	public static HostService getHostService(){
		return new HostServiceImpl();
	}
	
	public static QuotaService getQuotaService(){
		return new QuotaServiceImpl();
	}
	
	public static TenantService getTenantService(){
		
		return new TenantServiceImpl();			
//		return tenantService;
	}
	
	public static FloatingIPService getFloatingIPService(){
		return new FloatingIPServiceImpl();
	}
	
	public static SubnetService getSubnetService(){
		return new SubnetServiceImpl();
	}
	
	public static FlavorService getFlavorService(){
		return new FlavorServiceImpl();
	}
	
	public static PoolService getPoolService(){
		return new PoolServiceImpl();
	}

	public static NotificationService getNotificationService(){
		return new NotificationServiceImpl();
	}
	
	public static NotificationListService getNotificationListService() {
		return new NotificationListServiceImpl();
	}
	
	public static UserService getUserService(){
	
		return new UserServiceImpl();	
		
//		return userService;
		
	}
	
	public static RoleService getRoleService(){
		
		return new RoleServiceImpl();
		
//		return roleService;		
	}
	
	public static MonitorService getMonitorService(){
		return new MonitorServiceImpl();
	}
	
	public static OperationLogService getOperationService(){
		return new OperationLogServiceImpl();
	}
	
}
