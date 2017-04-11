package com.cloud.cloudapi.service.common.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.CloudServiceMapper;
import com.cloud.cloudapi.dao.common.HostAggregateMapper;
import com.cloud.cloudapi.dao.common.TemplateFieldMapper;
import com.cloud.cloudapi.dao.common.TemplateFieldRatingMapper;
import com.cloud.cloudapi.dao.common.TemplateServiceMapper;
import com.cloud.cloudapi.dao.common.TemplateTenantMappingMapper;
import com.cloud.cloudapi.dao.common.VolumeTypeMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.StackConfig;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.CloudService;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Datastore;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FloatingIPConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.HostAggregate;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ImageConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.InstanceConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.InstanceType;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Network;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PoolConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Range;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceSpec;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeType;
import com.cloud.cloudapi.pojo.rating.TemplateField;
import com.cloud.cloudapi.pojo.rating.TemplateFieldRating;
import com.cloud.cloudapi.pojo.rating.TemplateService;
import com.cloud.cloudapi.pojo.rating.TemplateTenantMapping;
import com.cloud.cloudapi.service.common.ConfigService;
import com.cloud.cloudapi.service.openstackapi.DBService;
import com.cloud.cloudapi.service.openstackapi.FloatingIPService;
import com.cloud.cloudapi.service.openstackapi.NetworkService;
import com.cloud.cloudapi.service.openstackapi.VolumeTypeService;
import com.cloud.cloudapi.service.pool.PoolResource;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.StringHelper;

@Service("configService")
public class ConfigServiceImpl implements ConfigService{
	
	@Autowired
	private CloudConfig cloudconfig;
	
	@Autowired
	private TemplateServiceMapper templateServiceMapper;
	
	@Autowired
	private TemplateFieldMapper templateFieldMapper;
	
	@Autowired
	private TemplateTenantMappingMapper templateTenantMappingMapper;
	
	@Autowired
	private TemplateFieldRatingMapper templateFieldRatingMapper;

	@Autowired
	private HostAggregateMapper hostAggregateMapper;

	@Resource
	private VolumeTypeMapper volumeTypeMapper;

	@Resource
	private CloudServiceMapper serviceMapper;
	
	@Resource
	private VolumeTypeService volumeTypeService;

	@Resource
	private NetworkService networkService;
	
	@Resource
	private FloatingIPService floatingIPService;
	
	@Resource
	private DBService dbService;
	
	private Logger log = LogManager.getLogger(PoolResource.class);
	
	private void makeVolumeConfig(PoolConfig poolConfig,TemplateTenantMapping tenantReating,TokenOs authToken) throws BusinessException{
		VolumeConfig volumeConfig = getVolumeConfig(tenantReating,true,authToken);
		poolConfig.setVolume(volumeConfig);
	}
	
	private void makeFloatingIPConfig(PoolConfig poolConfig,TemplateTenantMapping tenantReating,TokenOs authToken) throws BusinessException{
		Locale locale = new Locale(authToken.getLocale());
		
		String[] floatingSpecs = cloudconfig.getSystemFloatingSpec().split(",");
		String floatingPrice = cloudconfig.getFloatingPrice();
		String[] fipRangeSpec = cloudconfig.getPoolFipRange().split(",");
		
		List<Network> externalNetworks = networkService.getExternalNetworks(authToken);
		if(Util.isNullOrEmptyList(externalNetworks))
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale())); 
		
		if (floatingSpecs.length  != fipRangeSpec.length)
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
		
		Map<String,String> floatingPrices = getConfigPrice(tenantReating, ParamConstant.NETWORK,floatingPrice,authToken);
		
	//	String[] curFloatingPrices = getConfigPrice(tenantReating,ParamConstant.NETWORK, floatingSpecs, floatingPrices, authToken);

		String floatingipType = "CS_FLOTINGIP_TYPE_NAME";
		
		for (int index = 0; index < floatingSpecs.length; ++index) {
			ResourceSpec resource  = null;
			String floatingipTypeName = floatingipType.replaceFirst("TYPE", floatingSpecs[index].toUpperCase());
			for(Network network : externalNetworks){
				if(network.getName().matches("(?i)"+floatingSpecs[index]+".*")){
					resource = new ResourceSpec(network.getId(),Message.getMessage(floatingipTypeName,locale, false),Double.valueOf(floatingPrices.get(floatingSpecs[index])));
					resource.setType(floatingSpecs[index]);
					break; 
				}
			}
			if(null == resource)
				continue;
			String[] rang = fipRangeSpec[index].split(":");
			resource.setRange(new Range(Integer.parseInt(rang[0]), Integer.parseInt(rang[1])));
			poolConfig.addFipResource(resource);
		}
	}
	
	private void makeServiceConfig(PoolConfig poolConfig,TemplateTenantMapping tenantReating,TokenOs authToken) throws BusinessException {
		if (Util.isNullOrEmptyValue(cloudconfig.getSystemServiceSpec()))
			return;
		String[] serviceSpecs = cloudconfig.getSystemServiceSpec().split(",");
	//	String[] normalServiceSpecs = new String[serviceSpecs.length];
	//	for (int index = 0; index < serviceSpecs.length; ++index)
	//		normalServiceSpecs[index] = serviceSpecs[index] + "_service";
		String servicePrice = cloudconfig.getServicePrice();
		Map<String,String> servicePrices = getConfigPrice(tenantReating, ParamConstant.EXTEND_SERVICE,servicePrice,authToken) ;

	//	String[] curServicePrices = getConfigPrice(tenantReating, ParamConstant.EXTEND_SERVICE, normalServiceSpecs,
	//			servicePrices, authToken);
	//	if (serviceSpecs.length != curServicePrices.length)
	//		throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,
	//				new Locale(authToken.getLocale()));
		for (int index = 0; index < serviceSpecs.length; ++index) {
			poolConfig.addResource(serviceSpecs[index], Double.valueOf(servicePrices.get(serviceSpecs[index])), true);
		}
	}
	
	private void makeImageConfig(PoolConfig poolConfig,TemplateTenantMapping tenantReating,TokenOs authToken) throws BusinessException {
		if (Util.isNullOrEmptyValue(cloudconfig.getImagePrice()))
			return;
		ImageConfig imageConfig = getImageConfig(tenantReating,authToken);
		poolConfig.setImage(imageConfig);
	}
	
	
	private void makeInstanceConfig(PoolConfig poolConfig, TemplateTenantMapping tenantReating,TokenOs authToken) throws BusinessException {
		Locale locale = new Locale(authToken.getLocale());
		
		List<HostAggregate> aggregates = filterAggregates(hostAggregateMapper.selectAll());
		String instanceTypeSpec =  getInstanceTypes(aggregates);
		if(Util.isNullOrEmptyValue(instanceTypeSpec))
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));

		Map<String,String> cpuResourceRanges = getInstanceTypeResourceCpuRanges(authToken);
		Map<String,String> ramResourceRanges = getInstanceTypeResourceRamRanges(authToken);
		Map<String,String> cpuResourcePrices = getInstanceTypeCpuPrices(authToken);
		Map<String,String> ramResourcePrices = getInstanceTypeRamPrices(authToken);
		
		String[] instanceTypes = instanceTypeSpec.split(",");
		String[] coreInstanceTypes = new String[instanceTypes.length];
		String[] ramInstanceTypes = new String[instanceTypes.length];
		for (int i = 0; i < instanceTypes.length; i++) {
			coreInstanceTypes[i] = instanceTypes[i] + "_core";
			ramInstanceTypes[i] = instanceTypes[i] + "_ram";
		}

		Map<String,String> curCorePrices = getConfigPrice(tenantReating,ParamConstant.COMPUTE, coreInstanceTypes,
				cpuResourcePrices, authToken);
		Map<String,String> curRamPrices = getConfigPrice(tenantReating,ParamConstant.COMPUTE, ramInstanceTypes,
				ramResourcePrices, authToken);

		for (int index = 0; index < instanceTypes.length; ++index) {
			InstanceType instanceType = new InstanceType();
			// instanceType.setName(instanceTypes[index]);
			instanceType.setId(instanceTypes[index]);
			//instanceType.setName(Message.getMessage(instanceTypes[index].toUpperCase(),locale, false));
			instanceType.setName(getInstanceTypeName(aggregates, instanceTypes[index], locale));
			String price = curCorePrices.get(instanceTypes[index]+"_core");
			if(null == price)
				price = curCorePrices.get(ParamConstant.GENERAL_ZONE);
			ResourceSpec coreResource = new ResourceSpec(ParamConstant.CORE, Double.valueOf(price));
			
			price = curRamPrices.get(instanceTypes[index]+"_ram");
			if(null == price)
				price = curRamPrices.get(ParamConstant.GENERAL_ZONE);
			ResourceSpec ramResource = new ResourceSpec(ParamConstant.RAM, Double.valueOf(price));
			
			String range = cpuResourceRanges.get(instanceTypes[index]);
			if(null == range)
				range = cpuResourceRanges.get(ParamConstant.GENERAL_ZONE);
			String[] coreRange = range.split(":");
			coreResource.setRange(new Range(Integer.parseInt(coreRange[0]), Integer.parseInt(coreRange[1])));
			
			range = ramResourceRanges.get(instanceTypes[index]);
			if(null == range)
				range = ramResourceRanges.get(ParamConstant.GENERAL_ZONE);
			
			String[] ramRange = range.split(":");
			ramResource.setRange(new Range(Integer.parseInt(ramRange[0]), Integer.parseInt(ramRange[1])));
			instanceType.setCore(coreResource);
			instanceType.setRam(ramResource);
			poolConfig.addInstanceType(instanceType);
		}
	}
	
	private ImageConfig getImageConfig(TemplateTenantMapping tenantReating,TokenOs authToken) throws BusinessException{
		ImageConfig imageConfig = new ImageConfig();
		String imagePrice = cloudconfig.getImagePrice();
		Map<String,String> imagePrices = getConfigPrice(tenantReating, ParamConstant.IMAGE,imagePrice,authToken) ;
		if(null == imagePrices)
			return imageConfig;
		
		for (Map.Entry<String, String> entry : imagePrices.entrySet()){
			ResourceSpec imageType = new ResourceSpec();
			imageType.setName(entry.getValue());
			imageType.setUnitPrice(Double.valueOf(entry.getValue()));
			imageConfig.addImageType(imageType);
		}
		return imageConfig;
	}
	
	private String getVolumeTypeVisibleName(List<VolumeType> volumeTypes,String type){
		if(Util.isNullOrEmptyList(volumeTypes))
			return null;
		for(VolumeType volumeType : volumeTypes){
			if(type.equals(volumeType.getName()))
				return volumeType.getDisplayName();
		}
		return null;
	}
	
	private VolumeConfig getVolumeConfig(TemplateTenantMapping tenantReating,Boolean pool,TokenOs authToken) throws BusinessException{
		
		VolumeConfig volumeConfig = new VolumeConfig();
		List<VolumeType> volumeTypes = volumeTypeMapper.selectAll();
		Map<String,String> volumeTypeMap = new HashMap<String,String>();
		if(Util.isNullOrEmptyList(volumeTypes)){
			return volumeConfig;
		}else{
			for(VolumeType volumeType : volumeTypes){
				volumeTypeMap.put(volumeType.getName(), volumeType.getId());
			}
		}
		
		String volumePrice = cloudconfig.getVolumePrice();
		Map<String,String> volumePrices = getConfigPrice(tenantReating, ParamConstant.STORAGE,volumePrice,authToken) ;
		if(null == volumePrices)
			return volumeConfig;
		
		for (Map.Entry<String, String> entry : volumePrices.entrySet()){
			String typeName = getVolumeTypeVisibleName(volumeTypes,entry.getKey());
			if(Util.isNullOrEmptyValue(typeName))
				typeName = entry.getKey();
			
			if(true == pool)
				volumeConfig.addType(entry.getKey(),typeName,Double.parseDouble(entry.getValue()));
			else
				volumeConfig.addType(volumeTypeMap.get(entry.getKey()),typeName,Double.parseDouble(entry.getValue()));
		}
	
		String[] volumeRange = null;
		if(true == pool){
			volumeRange = cloudconfig.getPoolVolumeRange().split(",");
		}else{
			volumeRange = cloudconfig.getVolumeRange().split(",");
		}
		if (2 != volumeRange.length)
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
		volumeConfig.setRange(new Range(Integer.parseInt(volumeRange[0]), Integer.parseInt(volumeRange[1])));
        
		return volumeConfig;
	}
	
	@Override
	public InstanceConfig getInstanceConfig(String type, TokenOs authToken) throws BusinessException {
		InstanceConfig instanceConfig = new InstanceConfig();
		TemplateTenantMapping tenantReating = null;
		try {
			tenantReating = templateTenantMappingMapper.selectByTenantId(authToken.getTenantid());
		} catch (Exception e) {
			log.error(e);
			tenantReating = null;
		}
		
		VolumeConfig volumeConfig = getVolumeConfig(tenantReating,false,authToken);
		volumeConfig.setSize(Integer.parseInt(cloudconfig.getSystemVolumeSize()));
		volumeConfig.setWindowsSystemVolumeSize(Integer.parseInt(cloudconfig.getSystemWindowsVolumeSize()));
	    volumeConfig.setLinuxSystemVolumeSize(Integer.parseInt(cloudconfig.getSystemLinuxVolumeSize()));
		instanceConfig.setVolume(volumeConfig);

		
		String[] instanceTypes = null;
		List<HostAggregate> aggregates = null;
		if (type.equals(ParamConstant.VDI_TYPE)) {
			CloudService service = serviceMapper.selectByType(ParamConstant.VDI_TYPE);
			aggregates = hostAggregateMapper.selectByServiceId(service.getId());
		} else {
			CloudService kvmService = serviceMapper.selectByType(ParamConstant.INSTANCE_TYPE);
			CloudService vmwareService = serviceMapper.selectByType(ParamConstant.VMWARE_TYPE);
			List<String> serviceIds = new ArrayList<String>();
			serviceIds.add(kvmService.getId());
			serviceIds.add(vmwareService.getId());
			aggregates = hostAggregateMapper.selectByServiceIds(serviceIds);
		}
		aggregates = filterAggregates(aggregates);
		if(Util.isNullOrEmptyList(aggregates))
			return new InstanceConfig();

		instanceTypes = getInstanceTypes(aggregates).split(",");
		String[] coreInstanceTypes = new String[instanceTypes.length];
		String[] ramInstanceTypes = new String[instanceTypes.length];
		for (int i = 0; i < instanceTypes.length; i++) {
			coreInstanceTypes[i] = instanceTypes[i] + "_core";
			ramInstanceTypes[i] = instanceTypes[i] + "_ram";
		}
		
		Map<String,String> cpuResourcePrices = getInstanceTypeCpuPrices(authToken);
		Map<String,String> ramResourcePrices = getInstanceTypeRamPrices(authToken);
		Map<String,String> curCorePrices = getConfigPrice(tenantReating,ParamConstant.COMPUTE, coreInstanceTypes,
				cpuResourcePrices, authToken);
		Map<String,String> curRamPrices = getConfigPrice(tenantReating,ParamConstant.COMPUTE, ramInstanceTypes,
				ramResourcePrices, authToken);
		Locale locale = new Locale(authToken.getLocale());
		
		String[] coreSize = cloudconfig.getSystemCpuSpec().split(",");
		String[] ramSize = cloudconfig.getSystemRamSpec().split(",");
		
		for (int index = 0; index < instanceTypes.length; ++index) {
			InstanceType instanceType = new InstanceType();
			instanceType.setId(instanceTypes[index]);
			instanceType.setName(getInstanceTypeName(aggregates, instanceTypes[index], locale));
			String price = curCorePrices.get(instanceTypes[index]+"_core");
			if (null == price)
				price = curCorePrices.get(ParamConstant.GENERAL_CORE);
		
			instanceType.addCore(coreSize, Double.valueOf(price));
			price = curRamPrices.get(instanceTypes[index]+"_ram");
			if (null == price)
				price = curRamPrices.get(ParamConstant.GENERAL_RAM);
			
			instanceType.addRam(ramSize, Double.valueOf(price));
			instanceConfig.addInstanceType(instanceType);
		}

		ImageConfig imageConfig = getImageConfig(tenantReating, authToken);
		instanceConfig.setImage(imageConfig);
		
		return instanceConfig;
	}
	
	@Override
	public VolumeConfig getVolumeConfig(TokenOs ostoken) throws BusinessException{
		TemplateTenantMapping tenantReating = null;
		try {
			tenantReating = templateTenantMappingMapper.selectByTenantId(ostoken.getTenantid());
		} catch (Exception e) {
			log.error(e);
			tenantReating = null;
		}
		
		VolumeConfig volumeConfig = this.getVolumeConfig(tenantReating,false,ostoken);
		volumeConfig.setSize(Integer.parseInt(cloudconfig.getSystemVolumeSize()));
		volumeConfig.setWindowsSystemVolumeSize(Integer.parseInt(cloudconfig.getSystemWindowsVolumeSize()));
	    volumeConfig.setLinuxSystemVolumeSize(Integer.parseInt(cloudconfig.getSystemLinuxVolumeSize()));
		return volumeConfig;
	}
	
	@Override
	public FloatingIPConfig getFloatingIPConfig(TokenOs ostoken) throws BusinessException{
	
		TemplateTenantMapping tenantReating = null;
		try {
			tenantReating = templateTenantMappingMapper.selectByTenantId(ostoken.getTenantid());
		} catch (Exception e) {
			log.error(e);
			tenantReating = null;
		}
		
		String floatingPrice = cloudconfig.getFloatingPrice();
		Map<String,String> floatingIPPrices = getConfigPrice(tenantReating, ParamConstant.NETWORK,floatingPrice,ostoken) ;
	
		List<Network> externalNetworks = networkService.getExternalNetworks(ostoken);
		if(Util.isNullOrEmptyList(externalNetworks))
			 return new FloatingIPConfig();
		
		FloatingIPConfig floatingIPConfig = new FloatingIPConfig();
		floatingIPConfig.setUnit(ParamConstant.MBPS);
		String[] range = cloudconfig.getFloatingRange().split(",");
		if(2 != range.length)
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale())); 
		floatingIPConfig.setRange(new Range(Integer.parseInt(range[0]),Integer.parseInt(range[1])));
		
		String floatingipType = "CS_FLOTINGIP_TYPE_NAME";
		for (Map.Entry<String, String> entry : floatingIPPrices.entrySet()){
			for(Network network : externalNetworks){
				if(entry.getKey().equals(network.getName())){
					String name = null;
					try{
						 String floatingipTypeName = floatingipType.replaceFirst("TYPE", entry.getKey().toUpperCase());
						 name = Message.getMessage(floatingipTypeName, new Locale(ostoken.getLocale()),false);
					}catch(Exception e){
						name = entry.getKey();
					}
					floatingIPConfig.addResource(network.getId(),name,Double.valueOf(entry.getValue()));
					break;
				}
			}
		}
		
		/*
		String[] curFloatingPrices = getConfigPrice(tenantReating,ParamConstant.NETWORK, floatingSpecs, floatingPrices, ostoken);
		if(floatingSpecs.length != curFloatingPrices.length  || floatingSpecs.length != externalNetworks.size())
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale())); 
		String floatingipType = "CS_FLOTINGIP_TYPE_NAME";
		for(int index = 0 ; index < floatingSpecs.length; ++index){
			String floatingipTypeName = floatingipType.replaceFirst("TYPE", floatingSpecs[index].toUpperCase());
			for(Network network : externalNetworks){
				if(network.getName().matches("(?i)"+floatingSpecs[index]+".*")){
					floatingIPConfig.addResource(network.getId(),Message.getMessage(floatingipTypeName, new Locale(ostoken.getLocale()),false), Double.valueOf(curFloatingPrices[index]));
                    break; 
				}
			}
		}*/
		return floatingIPConfig;
	}
	
	@Override
	public PoolConfig getPoolConfig(TokenOs authToken) throws BusinessException{
		PoolConfig poolConfig = new PoolConfig();
		TemplateTenantMapping tenantReating = null;
		try {
			tenantReating = templateTenantMappingMapper.selectByTenantId(authToken.getTenantid());
		} catch (Exception e) {
			log.error(e);
			tenantReating = null;
		}
		
		makeVolumeConfig(poolConfig,tenantReating,authToken);
		makeFloatingIPConfig(poolConfig,tenantReating,authToken);
		makeServiceConfig(poolConfig,tenantReating,authToken);
		makeInstanceConfig(poolConfig,tenantReating,authToken);
		makeImageConfig(poolConfig,tenantReating,authToken);
		return poolConfig;
	}
	
	@Override
	public StackConfig getStackConfig(TokenOs authToken) throws BusinessException{
		List<String> az = new ArrayList<String>();
		Locale locale = new Locale(authToken.getLocale());
		Map<String, Object> core = new HashMap<String, Object>();
		Map<String, Object> ram = new HashMap<String, Object>();
		Map<String, Object> volume = new HashMap<String, Object>();
		List<Map<String, String>> instanceType = new ArrayList<Map<String, String>>();
		
		//String[] instanceTypes = this.cloudconfig.getSystemInstanceSpec().split(",");
		
		List<HostAggregate> aggregates = filterAggregates(hostAggregateMapper.selectAll());
		String instanceTypeSpec =  getInstanceTypes(aggregates);
		if(Util.isNullOrEmptyValue(instanceTypeSpec))
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
		String[] instanceTypes = instanceTypeSpec.split(",");
		
		az.addAll(Arrays.asList(instanceTypes));
		//az.add(cloudconfig.getSystemVdiSpec());

		String[] coreSize = this.cloudconfig.getSystemCpuSpec().split(",");
		List<Integer> coreList = new ArrayList<Integer>();
		for (String cS : coreSize) {
			int a = Integer.valueOf(cS);
			coreList.add(a);
		}
		core.put("core", coreList);

		String[] ramSize = this.cloudconfig.getSystemRamSpec().split(",");
		List<Integer> sizeList = new ArrayList<Integer>();
		for (String rS : ramSize) {
			float a = Float.parseFloat(rS);
			int s = (int) (a * 1024);
			sizeList.add(s);
		}
		ram.put("size", sizeList);

		String volSize = this.cloudconfig.getSystemVolumeSize();
		List<VolumeType> volumeTypes = volumeTypeMapper.selectAll();
		List<Map<String, String>> type = new ArrayList<Map<String, String>>();
		if(Util.isNullOrEmptyList(volumeTypes)){
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
		}else{
			for(VolumeType volumeType : volumeTypes){
				String typeName = volumeType.getDisplayName();
				if(Util.isNullOrEmptyValue(typeName))
					typeName = volumeType.getName();//Message.getMessage(volumeType.getName().toUpperCase(), locale,false);
				String id = volumeType.getId();
				Map<String, String> m = new HashMap<String, String>();
				m.put("id", id);
				m.put("name", typeName);
				type.add(m);
			}
		}
		
		//List<VolumeType> volumeTypes = this.volumeTypeService.getVolumeTypeList(null,authToken);
	
		volume.put("type", type);
		volume.put("size", Integer.valueOf(volSize));
		volume.put("windowsSystemVolumeSize", Integer.parseInt(cloudconfig.getSystemWindowsVolumeSize()));
		volume.put("linuxSystemVolumeSize", Integer.parseInt(cloudconfig.getSystemLinuxVolumeSize()));

		for (String iT : az) {
			Map<String, String> m = new HashMap<String, String>();
			m.put("id", iT);
			String name = getInstanceTypeName(aggregates, iT, locale);
			m.put("name", name);
			instanceType.add(m);
		}

		List<Map<String, String>> floatingIpType = new ArrayList<Map<String, String>>();
		FloatingIPConfig floatingipConfig = getFloatingIPConfig(authToken);
		for (ResourceSpec ft : floatingipConfig.getTypes()) {
			Map<String, String> m = new HashMap<String, String>();
			m.put("id", ft.getId());
			m.put("name", ft.getName());
			floatingIpType.add(m);
		}
		List<Datastore> datastores = this.dbService.getDatastores(authToken,null);
		StackConfig sc = new StackConfig();
		sc.setAz(az);
		sc.setCore(core);
		sc.setInstanceType(instanceType);
		sc.setRam(ram);
		sc.setVolume(volume);
		sc.setFloatingIpType(floatingIpType);
		sc.setDatastore(datastores);
		return sc;
	}
	
	private List<HostAggregate> filterAggregates(List<HostAggregate> aggregates){
		if(Util.isNullOrEmptyList(aggregates))
			return null;
		List<HostAggregate> filters = new ArrayList<HostAggregate>();
		CloudService service = null;
		for(HostAggregate aggregate : aggregates){
			if(Util.isNullOrEmptyValue(aggregate.getHostIds()))
				continue;
			service = serviceMapper.selectByPrimaryKey(aggregate.getServiceId());
			if(null == service)
				continue;
			if(ParamConstant.CONTAINER_TYPE.equals(service.getType()) || ParamConstant.BAREMETAL_TYPE.equals(service.getType()))
				continue;
			filters.add(aggregate);
		}
		return filters;
	}
	
	private String getInstanceTypes(List<HostAggregate> aggregates){
		if(null == aggregates || 0 ==aggregates.size())
			return null;
		List<String> zones = new ArrayList<String>();
		for(HostAggregate aggregate : aggregates){
			if(null != aggregate.getSource() && !aggregate.getSource().equals(ParamConstant.OPENSTACK_ZONE))
				continue;
			zones.add(aggregate.getAvailabilityZone());
		}
		return Util.listToString(zones, ',');
	}
	
	private Map<String,String> getInstanceTypeResourceCpuRanges(TokenOs authToken) throws BusinessException {
		Map<String,String> resourceRanges = new HashMap<String,String>();
		String poolCoreRangeSpec = cloudconfig.getPoolCpuRange();
		if(Util.isNullOrEmptyValue(poolCoreRangeSpec))
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
        String[] ranges = poolCoreRangeSpec.split(",");
        if(ranges.length != 2)
        	throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
        resourceRanges.put(ParamConstant.KVM_ZONE,ranges[0].trim());
        resourceRanges.put(ParamConstant.VMWARE_ZONE,ranges[1].trim());
        
        String poolVDICoreRangeSpec = cloudconfig.getPoolVdiCpuRange();
        if(!Util.isNullOrEmptyValue(poolVDICoreRangeSpec))
        	resourceRanges.put(ParamConstant.VDI_ZONE,poolVDICoreRangeSpec.trim());
        
		String poolGenralCoreRangeSpec = cloudconfig.getPoolGeneralCpuRange();
		if(Util.isNullOrEmptyValue(poolGenralCoreRangeSpec))
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
		resourceRanges.put(ParamConstant.GENERAL_ZONE,poolGenralCoreRangeSpec.trim());
		
		return resourceRanges;
	}
	
	private Map<String,String> getInstanceTypeResourceRamRanges(TokenOs authToken) throws BusinessException {
		Map<String,String> resourceRanges = new HashMap<String,String>();
		
		String poolRamRangeSpec = cloudconfig.getPoolRamfipRange();
		if(Util.isNullOrEmptyValue(poolRamRangeSpec))
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
        String[] ranges = poolRamRangeSpec.split(",");
        if(ranges.length != 2)
        	throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
        resourceRanges.put(ParamConstant.KVM_ZONE,ranges[0].trim());
        resourceRanges.put(ParamConstant.VMWARE_ZONE,ranges[1].trim());
        
        String poolVDIRamRangeSpec = cloudconfig.getPoolVdiRamRange();
        if(!Util.isNullOrEmptyValue(poolVDIRamRangeSpec))
        	resourceRanges.put(ParamConstant.VDI_ZONE,poolVDIRamRangeSpec.trim());
        
		String poolGenralRamRangeSpec = cloudconfig.getPoolGeneralRamRange();
		if(Util.isNullOrEmptyValue(poolGenralRamRangeSpec))
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
		resourceRanges.put(ParamConstant.GENERAL_ZONE,poolGenralRamRangeSpec.trim());
		
		return resourceRanges;
	}
	
	private Map<String,String> getInstanceTypeCpuPrices(TokenOs authToken) throws BusinessException {
		Map<String,String> resourcePrices = new HashMap<String,String>();
		
		String corePrice = cloudconfig.getCorePrice();
	//	if(Util.isNullOrEmptyValue(corePrices))
		//	throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
      //  String[] prices = corePrices.split(",");
      //  if(prices.length != 2)
     //   	throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
        resourcePrices.put(ParamConstant.KVM_ZONE+"_core",corePrice.trim());
        resourcePrices.put(ParamConstant.VMWARE_ZONE+"_core",corePrice.trim());
        
        String vdiCorePrice = cloudconfig.getVdiCorePrice();
        if(!Util.isNullOrEmptyValue(vdiCorePrice))
        	resourcePrices.put(ParamConstant.VDI_ZONE+"_core",vdiCorePrice.trim());
        
		//String genralPrice = cloudconfig.getGeneralCorePrice();
		//if(Util.isNullOrEmptyValue(genralPrice))
		//	throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
		resourcePrices.put(ParamConstant.GENERAL_ZONE,corePrice.trim());
		
		return resourcePrices;
	}
	
	private Map<String,String> getInstanceTypeRamPrices(TokenOs authToken) throws BusinessException {
		Map<String,String> resourcePrices = new HashMap<String,String>();
		
		String ramPrice = cloudconfig.getRamPrice();
	//	if(Util.isNullOrEmptyValue(ramPrice))
	//		throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
    //    String[] prices = ramPrice.split(",");
    //    if(prices.length != 2)
    //    	throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
        resourcePrices.put(ParamConstant.KVM_ZONE+"_ram",ramPrice.trim());
        resourcePrices.put(ParamConstant.VMWARE_ZONE+"_ram",ramPrice.trim());
        
        String vdiRamPrice = cloudconfig.getVdiRamPrice();
        if(!Util.isNullOrEmptyValue(vdiRamPrice))
        	resourcePrices.put(ParamConstant.VDI_ZONE+"_ram",vdiRamPrice.trim());
        
	//	String genralPrice = cloudconfig.getGeneralRamPrice();
	//	if(Util.isNullOrEmptyValue(genralPrice))
	//		throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
		resourcePrices.put(ParamConstant.GENERAL_ZONE,ramPrice.trim());
		
		return resourcePrices;
	}
	
	private String getInstanceTypeName(List<HostAggregate> aggregates,String instanceType,Locale locale){
		if(null == aggregates)
			return Message.getMessage(instanceType,locale,false);
		for(HostAggregate aggregate : aggregates){
			if(aggregate.getAvailabilityZone().equals(instanceType))
				return StringHelper.ncr2String(aggregate.getName());
		}
		return Message.getMessage(instanceType.toUpperCase(),locale,false);
	}
	
	@Override
	public String[] getConfigPrice(TemplateTenantMapping tenantRating, String serviceCode, String[] typeList,
			String[] defaultPrice, TokenOs ostoken) {
		if (null == tenantRating)
			return defaultPrice;
		TemplateService service = templateServiceMapper.selectByServiceCode(serviceCode);
		if(null == service)
			return defaultPrice;
		TemplateTenantMapping tenantMapping = templateTenantMappingMapper.selectByTenantId(ostoken.getTenantid());
		if(null == tenantMapping)
			return defaultPrice;
		
		List<TemplateFieldRating> fields = templateFieldRatingMapper.selectByTemplateAndVersionId(tenantMapping.getTemplate_id(), tenantMapping.getVersion_id());
		if(Util.isNullOrEmptyList(fields))
			return defaultPrice;
		
		for(int index = 0; index < typeList.length; ++index){
			for(TemplateFieldRating field : fields){
				if(field.getCharging_keys().equals(typeList[index])){
				      Float price = field.getPrice()  * tenantMapping.getTenant_discount();
				      defaultPrice[index] = price.toString();
				}
			}
		}
		return defaultPrice;
	}
	
	@Override
	public Map<String,String> getConfigPrice(TemplateTenantMapping tenantRating, String serviceCode, String[] typeList,
			Map<String,String> prices, TokenOs ostoken) {
		if (null == tenantRating)
			return prices;
		TemplateService service = templateServiceMapper.selectByServiceCode(serviceCode);
		if(null == service)
			return prices;
		TemplateTenantMapping tenantMapping = templateTenantMappingMapper.selectByTenantId(ostoken.getTenantid());
		if(null == tenantMapping)
			return prices;
		
		List<TemplateFieldRating> fields = templateFieldRatingMapper.selectByTemplateAndVersionId(tenantMapping.getTemplate_id(), tenantMapping.getVersion_id());
		if(Util.isNullOrEmptyList(fields))
			return prices;
		
		for(int index = 0; index < typeList.length; ++index){
			for(TemplateFieldRating field : fields){
				if(field.getCharging_keys().equalsIgnoreCase(typeList[index])){
				      Float price = field.getPrice()  * tenantMapping.getTenant_discount();
				      prices.put(field.getCharging_keys(), price.toString());
				      break;
				}
			}
		}
		return prices;
	}
	
	@Override
	public Map<String,String> getConfigPrice(TemplateTenantMapping tenantRating, String serviceCode,String defaultPrice,TokenOs ostoken) {
		TemplateService templateService = templateServiceMapper.selectByServiceCode(serviceCode);
	    if(null == templateService){
	    	return null;
	    }
	       
	    List<TemplateField> fields = templateFieldMapper.selectRatingFiledsByServiceId(templateService.getService_id());
	    if(Util.isNullOrEmptyList(fields)){
	    	return null;
	    }
	    
		if (null == tenantRating){
			return makeFieldPrices(null,fields,null,defaultPrice);
		}
	
		TemplateTenantMapping tenantMapping = templateTenantMappingMapper.selectByTenantId(ostoken.getTenantid());
		if(null == tenantMapping){
			return makeFieldPrices(null,fields,null,defaultPrice);
		}
		
		List<TemplateFieldRating> ratingFields = templateFieldRatingMapper.selectByTemplateAndVersionId(tenantMapping.getTemplate_id(), tenantMapping.getVersion_id());
		if(Util.isNullOrEmptyList(ratingFields)){
			return makeFieldPrices(null,fields,null,defaultPrice);
		}
		
		return makeFieldPrices(tenantMapping,fields,ratingFields,defaultPrice);
	}
	
	@Override
	public Map<String,String> getConfigPrice(TemplateTenantMapping tenantRating, String serviceCode,String defaultCpuPrice,String defaultRamPrice,TokenOs ostoken) {
		TemplateService templateService = templateServiceMapper.selectByServiceCode(serviceCode);
	    if(null == templateService){
	    	return null;
	    }
	       
	    List<TemplateField> fields = templateFieldMapper.selectRatingFiledsByServiceId(templateService.getService_id());
	    if(Util.isNullOrEmptyList(fields)){
	    	return null;
	    }
	    
		if (null == tenantRating){
			return makeFieldPrices(null,fields,null,defaultCpuPrice,defaultRamPrice);
		}
	
		TemplateTenantMapping tenantMapping = templateTenantMappingMapper.selectByTenantId(ostoken.getTenantid());
		if(null == tenantMapping){
			return makeFieldPrices(null,fields,null,defaultCpuPrice,defaultRamPrice);
		}
		
		List<TemplateFieldRating> ratingFields = templateFieldRatingMapper.selectByTemplateAndVersionId(tenantMapping.getTemplate_id(), tenantMapping.getVersion_id());
		if(Util.isNullOrEmptyList(ratingFields)){
			return makeFieldPrices(null,fields,null,defaultCpuPrice,defaultRamPrice);
		}
		
		return makeFieldPrices(tenantMapping,fields,ratingFields,defaultCpuPrice,defaultRamPrice);
	}
	
	private Map<String,String> makeFieldPrices(TemplateTenantMapping tenantMapping,List<TemplateField> fields,List<TemplateFieldRating> ratingFields,String defaultPrice){
		Map<String,String> prices = new HashMap<String,String>();
		for(TemplateField field : fields){
			Boolean hit = false;
			if(null != tenantMapping){
				for(TemplateFieldRating ratingField : ratingFields){
					if(field.getField_code().equals(ratingField.getCharging_keys())){
						Float price = ratingField.getPrice()  * tenantMapping.getTenant_discount();
						prices.put(field.getField_code(), price.toString());
						hit = true;
						break;
					}
				}	
			}
			if(false == hit)
				prices.put(field.getField_code(), defaultPrice);
		}
		return prices;
	}
	
	private Map<String,String> makeFieldPrices(TemplateTenantMapping tenantMapping,List<TemplateField> fields,List<TemplateFieldRating> ratingFields,String defaultCpuPrice,String defaultRamPrice){
		Map<String,String> prices = new HashMap<String,String>();
		for(TemplateField field : fields){
			Boolean hit = false;
			if(null != tenantMapping){
				for(TemplateFieldRating ratingField : ratingFields){
					if(field.getField_code().equals(ratingField.getCharging_keys())){
						Float price = ratingField.getPrice()  * tenantMapping.getTenant_discount();
						prices.put(field.getField_code(), price.toString());
						hit = true;
						break;
					}
				}	
			}
			if(false == hit){
				if(field.getField_code().contains(ParamConstant.CORE))
					prices.put(field.getField_code(), defaultCpuPrice);
				else
					prices.put(field.getField_code(), defaultRamPrice);
			}
				
		}
		return prices;
	}
}
