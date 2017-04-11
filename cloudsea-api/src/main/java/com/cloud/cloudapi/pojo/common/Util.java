package com.cloud.cloudapi.pojo.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.UUID;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Util {
	
    public static final String DEFAULT_DISPLAY_PERMISSION = "admin-dashboard,dashboard,resource_map,pools,bare-metals,zones,volumeTypes,resource_setting,instances,bare-metals-instances,security-groups,keypairs,volumes,backups,images,networks,subnets,ports,floating-ips,routers,resource_management,alarms,notifications,notification-lists,operations,notification,load-balancers,firewalls,vpns,containers,relational-databases,non-relational-databases,database,service,vdiInstances,VMWares,vdi,process-instances,pricings,bills,billAccounts,billing,users,roles,tenants,color,management,market,thirdparts,serviceCatalog";
    public static final String DEFAULT_OPERATION_PERMISSION = "pool_new,pool_apply,pool_newStack,pool_deleteStack,bareMetal_new,bareMetal_start,bareMetal_stop,bareMetal_delete,bareMetal_restart,zone_new,zone_delete,zone_update,zone_addHost,zone_removeHost,volumeType_new,volumeType_delete,volumeType_update,instance_new,instance_start,instance_stop,instance_delete,instance_restart,instance_forceRestart,instance_suspend,instance_pause,instance_recover,instance_console,instance_resize,instance_update,instance_addVolume,instance_removeVolume,instance_addPort,instance_removePort,instance_addFloatingIP,instance_removeFloatingIP,instance_addSecurityGroup,instance_removeSecurityGroup,image_new,image_delete,bareMetalInstance_new,bareMetalInstance_start,bareMetalInstance_stop,bareMetalInstance_delete,bareMetalInstance_restart,securityGroup_new,securityGroup_delete,securityGroup_update,securityGroup_addRule,securityGroup_removeRule,securityGroup_addPort,securityGroup_removePort,keypair_new,keypair_delete,keypair_upload,volume_new,volume_delete,volume_update,volume_restore,backup_new,backup_delete,network_new,network_delete,network_update,subnet_new,subnet_delete,subnet_update,port_new,port_delete,port_update,floatingIP_new,floatingIP_delete,floatingIP_update,router_new,router_delete,router_openFloatingIP,router_closeFloatingIP,router_update,router_addSubnet,router_removeSubnet,router_addPort,router_removePort,alarm_new,alarm_delete,alarm_update,alarm_disable,alarm_enable,alarm_addResource,alarm_removeResource,alarm_addNotificationList,alarm_removeNotificationList,alarm_addRule,alarm_removeRule,notification_read,notification_unread,notificationList_new,notificationList_delete,notificationList_update,notificationList_addTerminal,notificationList_deleteTerminal,operation_delete,loadBalancer_new,loadBalancer_delete,loadBalancer_update,loadBalancer_disable,loadBalancer_enable,loadBalancer_addFIP,loadBalancer_removeFIP,firewall_new,firewall_delete,firewall_update,firewall_disable,firewall_enable,firewall_addRouter,firewall_removeRouter,vpn_new,vpn_delete,vpn_update,vpn_disable,vpn_enable,workflow_delete,workflow_approve,workflow_disapprove,container_new,container_delete,container_update,relationalDatabase_new,relationalDatabase_delete,relationalDatabase_start,relationalDatabase_stop,relationalDatabase_addUser,relationalDatabase_deleteUser,relationalDatabase_addDatabase,relationalDatabase_deleteDatabase,nonrelationalDatabase_new,nonrelationalDatabase_delete,nonrelationalDatabase_start,nonrelationalDatabase_stop,nonrelationalDatabase_addUser,nonrelationalDatabase_deleteUser,nonrelationalDatabase_addDatabase,nonrelationalDatabase_deleteDatabase,vdiInstance_new,vdiInstance_start,vdiInstance_stop,vdiInstance_delete,vdiInstance_restart,vdiInstance_forceRestart,vdiInstance_suspend,vdiInstance_pause,vdiInstance_recover,vdiInstance_console,vdiInstance_resize,vdiInstance_update,vdiInstance_addVolume,vdiInstance_removeVolume,vdiInstance_addPort,vdiInstance_removePort,vdiInstance_addFloatingIP,vdiInstance_removeFloatingIP,vdiInstance_addSecurityGroup,vdiInstance_removeSecurityGroup,pricing_new,pricing_delete,pricing_update,pricing_newVersion,pricing_execute,bill_updateAccount,billAccount_new,billAccount_delete,billAccount_setDefaultAccount,user_new,user_enable,user_disable,user_reset,user_modifyPassword,user_bindRole,user_removeRole,role_new,role_delete,role_update,role_bindUser,tenant_new,tenant_delete,tenant_update,tenant_addUser,tenant_removeUser,tenant_updateUserRole,common_updateLanguage,common_updateRegion,common_updateColor,common_updateTenant";
    public static final String DEFAULT_USER_DISPLAY_PERMISSION = "dashboard,instances,images,bare-metals-instances,security-groups,networks,subnets,ports,floating-ips,routers,network,keypairs,security,volumes,backups,storage,load-balancers,firewalls,vpns,alarms,process-instances,containers,relational-databases,non-relational-databases,database,vdiInstances,VMWares,vdi,service,pools,deploy,notifications,operations,notification-lists,notification,pricings,bills,billAccounts,billing,market,thirdparts,serviceCatalog,color";
    public static final String DEFAULT_USER_OPERATION_PERMISSION = "instance_new,instance_start,instance_stop,instance_delete,instance_restart,instance_forceRestart,instance_suspend,instance_pause,instance_recover,instance_console,instance_resize,instance_update,instance_addVolume,instance_removeVolume,instance_addPort,instance_removePort,instance_addFloatingIP,instance_removeFloatingIP,instance_addSecurityGroup,instance_removeSecurityGroup,image_new,image_delete,bareMetalInstance_new,bareMetalInstance_start,bareMetalInstance_stop,bareMetalInstance_delete,bareMetalInstance_restart,securityGroup_new,securityGroup_delete,securityGroup_update,securityGroup_addRule,securityGroup_removeRule,network_new,network_delete,network_update,subnet_new,subnet_delete,subnet_update,port_new,port_delete,port_update,floatingIP_new,floatingIP_delete,floatingIP_update,router_new,router_delete,router_openFloatingIP,router_closeFloatingIP,router_update,router_addSubnet,router_removeSubnet,keypair_new,keypair_delete,keypair_upload,volume_new,volume_delete,volume_update,volume_restore,backup_new,backup_delete,loadBalancer_new,loadBalancer_delete,loadBalancer_update,loadBalancer_disable,loadBalancer_enable,firewall_new,firewall_delete,firewall_update,firewall_disable,firewall_enable,firewall_addRouter,firewall_removeRouter,vpn_new,vpn_delete,vpn_update,vpn_disable,vpn_enable,alarm_new,alarm_delete,alarm_update,alarm_disable,alarm_enable,alarm_addResource,alarm_removeResource,alarm_addNotificationList,alarm_removeNotificationList,alarm_addRule,alarm_removeRule,workflow_delete,workflow_approve,workflow_disapprove,container_new,container_delete,container_update,relationalDatabase_new,relationalDatabase_delete,relationalDatabase_start,relationalDatabase_stop,relationalDatabase_addUser,relationalDatabase_deleteUser,relationalDatabase_addDatabase,relationalDatabase_deleteDatabase,nonrelationalDatabase_new,nonrelationalDatabase_delete,nonrelationalDatabase_start,nonrelationalDatabase_stop,nonrelationalDatabase_addUser,nonrelationalDatabase_deleteUser,nonrelationalDatabase_addDatabase,nonrelationalDatabase_deleteDatabase,vdiInstance_new,vdiInstance_start,vdiInstance_stop,vdiInstance_delete,vdiInstance_restart,vdiInstance_forceRestart,vdiInstance_suspend,vdiInstance_pause,vdiInstance_recover,vdiInstance_console,vdiInstance_resize,vdiInstance_update,vdiInstance_addVolume,vdiInstance_removeVolume,vdiInstance_addPort,vdiInstance_removePort,vdiInstance_addFloatingIP,vdiInstance_removeFloatingIP,vdiInstance_addSecurityGroup,vdiInstance_removeSecurityGroup,pool_new,pool_apply,pool_newStack,pool_deleteStack,notification_read,notification_unread,operation_delete,notificationList_new,notificationList_delete,notificationList_update,notificationList_addTerminal,notificationList_deleteTerminal,bill_updateAccount,billAccount_new,billAccount_delete,billAccount_setDefaultAccount,common_updateLanguage,common_updateRegion,common_updateColor";
    public static final String ADMIN_ROLE_NAME = "admin";
    public static final String USER_ROLE_NAME = "user";
	public static final String ADMIN_ROLE_SIGN = "999100000000";
	
	public static String listToString(List list, char separator) {    
		if(null == list || 0 == list.size())
			return "";
		StringBuilder sb = new StringBuilder();    
		int length = list.size();
		for (int i = 0; i < length; i++) {        
			sb.append(list.get(i)); 
			if (i < length - 1)   
				sb.append(separator); 
		}   
		return sb.toString();
	}
	
	public static List<String> stringToList(String value,String separator) {    
		if(null == value || value.isEmpty())
			return null;
		String[] values = value.split(separator);
		List<String> valueList = Arrays.asList(values);
		return valueList;
	}
	
	public static boolean isSystemVolume(String name){
		if(null == name)
			return false;
		int index = name.indexOf("/dev/");
		if(-1 == index)
			return false;
		String deviceName = name.substring(index+"/dev/".length());
		if(deviceName.equals("vda") || deviceName.equals("hda") || deviceName.equals("sda"))
			return true;
		return false;
	}
	
	public static String getImageIdFromLocation(String location){
		if(null == location)
			return null;
		int imagePos = location.indexOf(ParamConstant.IMAGES);
		if(-1 == imagePos)
			return null;
		int length = (ParamConstant.IMAGES+"/").length();
		return location.substring(imagePos+length);
	}
	
	public static String getCurrentDate(){
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return df.format(new Date());
	}
	
	public static long getCurrentMillionsecond(){
		return new Date().getTime();
	}
	
	public static String millionSecond2Date(long millionSeconds){
		Date d = new Date(millionSeconds);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return df.format(d);
	}
	
	public static long utc2Millionsecond(String utcTime){
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z");
		String time = utcTime.replace("Z", " UTC");
		try {
			Date d = df.parse(time);
			return d.getTime();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			Logger log = LogManager.getLogger(Util.class);
			log.error(e);;
		}
		return 0;
	}
	
	public static long time2Millionsecond(String time,String format){
		SimpleDateFormat df = new SimpleDateFormat(format);
		try {
			Date d = df.parse(time);
			return d.getTime();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			Logger log = LogManager.getLogger(Util.class);
			log.error(e);;
		}
		return 0;
	}
	
	public static String getIdWithAppendId(String appendId,String orgId){
		if(Util.isNullOrEmptyValue(orgId))
			return appendId;
		if(Util.isNullOrEmptyValue(appendId))
			return orgId;
		StringBuilder sb = new StringBuilder();
		sb.append(orgId);
		sb.append(',');
		sb.append(appendId);
		return sb.toString();
	}


	public static String getCreateBody(String bodyName,Map<String,String> paramMap){
		if(null == paramMap || 0 == paramMap.size())
			return null;
		StringBuilder sb = new StringBuilder();
		sb.append(bodyName);
		sb.append(":{");
		int i = 0;
		for (Entry<String, String> entry : paramMap.entrySet()) {
			sb.append(entry.getKey());
			sb.append(":");
			sb.append( entry.getValue());
			if (i < paramMap.size() - 1)   
				sb.append(','); 
			++i;
		}
		sb.append("}");
		return sb.toString();
	}
	
	public static boolean isNullOrEmptyValue(String value){
		if(null == value || value.isEmpty())
			return true;
		return false;
	}
	
	public static String getFailedReason(Map<String, String> rs){
		String message = "";
		if (null == rs || 0 == rs.size())
			return message;
		
		ObjectMapper mapper = new ObjectMapper();
		try{
			JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
			JsonNode errorNode = rootNode.get(ResponseConstant.NETWORK_FAILED);
			if(errorNode != null && !errorNode.isMissingNode()){
				message = errorNode.path(ResponseConstant.MESSAGE).textValue();
				return message;
			}
			
			errorNode = rootNode.path(ResponseConstant.BADREQUEST);
			if(errorNode != null && !errorNode.isMissingNode()){
				message = errorNode.path(ResponseConstant.MESSAGE).textValue();
				return message;
			}
			
			errorNode = rootNode.path(ResponseConstant.ITEM_NOT_FOUND);
			if(errorNode != null && !errorNode.isMissingNode()){
				message = errorNode.path(ResponseConstant.MESSAGE).textValue();
				return message;
			}
			
			errorNode = rootNode.path(ResponseConstant.COMPUTE_FAULT);
			if(errorNode != null && !errorNode.isMissingNode()){
				message = errorNode.path(ResponseConstant.MESSAGE).textValue();
				return message;
			}
			
			errorNode = rootNode.path(ResponseConstant.FAULT);
			if(errorNode != null && !errorNode.isMissingNode()){
				return errorNode.asText();
			}
			
			errorNode = rootNode.path(ResponseConstant.ERROR);
			if(errorNode != null && !errorNode.isMissingNode()){
				return errorNode.asText();
			}
			
		}catch(Exception e){
			return "";
		}
		return message;
	}
	
	public static List<String> getCorrectedIdInfo(String orgIds, String cmpId) {
		if (Util.isNullOrEmptyValue(orgIds))
			return null;
		List<String> orgIdList = Util.stringToList(orgIds, ",");
		List<String> correctedIdList = new ArrayList<String>();
		for (String id : orgIdList) {
			if (id.equals(cmpId))
				continue;
			correctedIdList.add(id);
		}
		return correctedIdList;
	}
	
	public static String getAppendedIds(String ids,List<String> appendedIds){
		if(Util.isNullOrEmptyList(appendedIds))
			return ids;
		String newIds = ids;
		if (!Util.isNullOrEmptyValue(ids)) {
			for(String appendedId : appendedIds){
				if (!ids.contains(appendedId)) {
					newIds += ",";
					newIds += appendedId;
				}	
			}
		} else {
			return Util.listToString(appendedIds, ',');
		}
		return newIds;
	}
	
	public static String getAppendedIds(String ids,String appendedId){
		if(Util.isNullOrEmptyValue(appendedId))
			return ids;
		String newIds = ids;
		if (!Util.isNullOrEmptyValue(ids)) {
				if (!ids.contains(appendedId)) {
					newIds += ",";
					newIds += appendedId;
				}	
		} else {
			return appendedId;
		}
		return newIds;
	}
	
	public static boolean isSame(String newId,String orgId){
		if(Util.isNullOrEmptyValue(newId) && Util.isNullOrEmptyValue(orgId))
			return true;
		if(Util.isNullOrEmptyValue(newId) && !Util.isNullOrEmptyValue(orgId))
			return false;
		if(!Util.isNullOrEmptyValue(newId) && Util.isNullOrEmptyValue(orgId))
			return false;
		return newId.equals(orgId);
	}
	
	public static boolean isNullOrEmptyList(List list){
		if(null == list || 0 == list.size())
			return true;
		return false;
	}
	
	public static boolean string2Boolean(String value){
		if(Util.isNullOrEmptyValue(value))
			return false;
		if(value.equalsIgnoreCase(ParamConstant.TRUE))
			return true;
		return false;
	}
	
	
	public static Long byte2Mega(Long size){
		if(0 == size)
			return size;
		return size/ParamConstant.BYTE2MEGA;
	}
	
	public static Map<String, String> makeRequestParamInfo(String limit,String name,String status,String type,String id){
		Map<String, String> paramMap = null;
		if (!"".equals(limit)) {
			paramMap = new HashMap<String, String>();
			paramMap.put(ParamConstant.LIMIT, limit);
		}
		if (!"".equals(name)) {
			if (paramMap == null)
				paramMap = new HashMap<String, String>();
			paramMap.put(ParamConstant.NAME, name);
		}
		if (!"".equals(status)) {
			if (paramMap == null)
				paramMap = new HashMap<String, String>();
			paramMap.put(ParamConstant.STATUS, status);
		}
		if (!"".equals(id)) {
			if (paramMap == null)
				paramMap = new HashMap<String, String>();
			paramMap.put(type, id);
		}
		return paramMap;
	}
	
	public static int getLimit(Map<String, String> paramMap){
		if(null == paramMap || 0 == paramMap.size())
			return -1;
		String limit = paramMap.get(ParamConstant.LIMIT);
		if(Util.isNullOrEmptyValue(limit))
			return -1;
		return Integer.parseInt(limit);
	}
	
	public static String makeUUID(){
		return UUID.randomUUID().toString();
	}
	
	public static void checkResponseBody(Map<String, String> rs,Locale locale) throws BusinessException{
		if (null == rs || 0 == rs.size()){
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.NOT_FOUND_RESPONSE_CODE,locale);
		}
	}
	
//	public static Map<String,Boolean> getImageType(String imageType){
//		Map<String,Boolean> typeFlag = new HashMap<String,Boolean>();
//		if(Util.isNullOrEmptyValue(imageType)){
//			typeFlag.put(null, true);
//		    return  typeFlag;	
//		}
//		if(ParamConstant.WINDOWS.equalsIgnoreCase(imageType)){
//			typeFlag.put(ParamConstant.WINDOWS, true);
//			return typeFlag;
//		}else if(ParamConstant.CENTOS.equalsIgnoreCase(imageType)){
//			typeFlag.put(ParamConstant.CENTOS, true);
//			return typeFlag;
//		}else if(ParamConstant.UBUNTU.equalsIgnoreCase(imageType)){
//			typeFlag.put(ParamConstant.UBUNTU, true);
//			return typeFlag;
//		}else if(ParamConstant.FEDORA.equalsIgnoreCase(imageType)){
//			typeFlag.put(ParamConstant.FEDORA, true);
//			return typeFlag;
//		}else if(ParamConstant.MYSQL.equalsIgnoreCase(imageType)){
//			typeFlag.put(ParamConstant.MYSQL, true);
//			return typeFlag;
//		}else if(ParamConstant.POSTGRE.equalsIgnoreCase(imageType)){
//			typeFlag.put(ParamConstant.POSTGRE, true);
//			return typeFlag;
//		}else if(ParamConstant.MONOGODB.equalsIgnoreCase(imageType)){
//			typeFlag.put(ParamConstant.MONOGODB, true);
//			return typeFlag;
//		}else if(ParamConstant.PHYSICAL.equalsIgnoreCase(imageType)){
//			typeFlag.put(ParamConstant.PHYSICAL, true);
//			return typeFlag;
//		}else if(ParamConstant.DOCKER.equalsIgnoreCase(imageType)){
//			typeFlag.put(ParamConstant.DOCKER, true);
//			return typeFlag;
//		}else{
//			typeFlag.put(null, true);			
//		}			
////		if(name.matches("(?i)"+ParamConstant.CENTOS+".*")){
////			typeFlag.put(ParamConstant.CENTOS, true);
////		}else if(name.matches("(?i)"+ParamConstant.UBUNTU+".*")){
////			typeFlag.put(ParamConstant.UBUNTU, true);
////		}else if(name.matches("(?i)"+ParamConstant.FEDORA+".*")){
////			typeFlag.put(ParamConstant.FEDORA, true);
////		}else if(name.matches("(?i)"+ParamConstant.WINDOWS+".*")){
////			typeFlag.put(ParamConstant.WINDOWS, true);
////		}else if(name.matches("(?i)"+ParamConstant.MYSQL+".*")){
////			typeFlag.put(ParamConstant.MYSQL, false);
////		}else if(name.matches("(?i)"+ParamConstant.POSTGRE+".*")){
////			typeFlag.put(ParamConstant.POSTGRE, false);
////		}else if(name.matches("(?i)"+ParamConstant.MONOGODB+".*")){
////			typeFlag.put(ParamConstant.MONOGODB, false);
////		}else if(name.matches("(?i)"+ParamConstant.PHYSICAL+".*")){
////			typeFlag.put(ParamConstant.PHYSICAL, false);
////		}else if(name.matches("(?i)"+ParamConstant.DOCKER+".*")){
////			typeFlag.put(ParamConstant.DOCKER, false);
////		}else{
////			typeFlag.put(null, true);			
////		}
//		return typeFlag;	
//	}
	
	public static String getAvailabilityZone(String availabilityZone,String[] instanceTypes){
		if(null == instanceTypes)
			return null;
		for (int index = 0; index < instanceTypes.length; ++index) {
			if(Message.getMessage(instanceTypes[index].toUpperCase(),false).equals(availabilityZone))
				return instanceTypes[index];
		}
		return null;
	}
	
	public static String getVolumeType(String volumeType,String[] volumeTypes){
		if(null == volumeTypes)
			return null;
		for (int index = 0; index < volumeTypes.length; ++index) {
			if(Message.getMessage(volumeTypes[index].toUpperCase(),false).equals(volumeType))
				return volumeTypes[index];
		}
		return null;
	}
}
