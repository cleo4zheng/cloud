package com.cloud.cloudapi.service.openstackapi.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.PoolEntityMapper;
import com.cloud.cloudapi.dao.common.PoolStackMapper;
import com.cloud.cloudapi.dao.common.ResourceEventMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PoolEntity;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PoolStack;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Quota;
import com.cloud.cloudapi.pojo.openstackapi.forgui.QuotaDetail;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceEvent;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeType;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.HostService;
import com.cloud.cloudapi.service.openstackapi.HypervisorService;
import com.cloud.cloudapi.service.openstackapi.PoolEntityService;
import com.cloud.cloudapi.service.openstackapi.QuotaService;
import com.cloud.cloudapi.service.openstackapi.ResourceSpecService;
import com.cloud.cloudapi.service.openstackapi.VolumeTypeService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PoolEntityServiceImpl implements PoolEntityService {

	@Resource
	private PoolEntityMapper poolEntityMapper;
	
	@Resource
	private HypervisorService hypervisorService;
	
	@Resource
	private PoolStackMapper poolStackMapper;
	
	@Resource
	private QuotaService quotaService;
	
	@Resource
	private AuthService authService;
	
	@Resource
	private HostService hostService;
	
//	@Autowired
//	private CloudConfig cloudconfig;
	
	@Resource
	private VolumeTypeService volumeTypeService;
	
	@Resource
	private ResourceSpecService resourceSpecService;
	
	@Resource
	private ResourceEventMapper resourceEventMapper;
	
	private Logger log = LogManager.getLogger(PoolEntityServiceImpl.class);
	
	@SuppressWarnings("unchecked")
	@Override
	public PoolEntity createPoolEntity(Map<String, Object> params, TokenOs ostoken) throws BusinessException {
		PoolEntity pool = this.initNewPoolEntity(params);
		String poolName = (String) params.get("name");
		pool.setName(poolName);
		String tenantId = ostoken.getTenantid();
		Locale locale = new Locale(ostoken.getLocale());

//		TokenOs adminToken = null;
//		try {
//			adminToken = this.authService.createDefaultAdminOsToken();
//		} catch (Exception e1) {
//			e1.printStackTrace();
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
//		}

		PoolEntity existPool = this.getPoolEntityByTenantId(tenantId);
		if (existPool != null) {
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		pool.setTenantId(tenantId);
		pool.setCreatedAt(Util.getCurrentDate());
		ObjectMapper mapper = new ObjectMapper();
		
		Map<String, Integer> tCpuMap = (Map<String, Integer>) params.get("cpus");
		String tCpu = null;
		Map<String, Integer> tMemMap = (Map<String, Integer>) params.get("mems");
		String tMem = null;
		try {
			tCpu = mapper.writeValueAsString(tCpuMap);
			tMem = mapper.writeValueAsString(tMemMap);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		pool.settCpus(tCpu);
		pool.settMems(tMem);
		
		int tCpus=0, tMems=0;
		for (Map.Entry<String, Integer> entry : tCpuMap.entrySet()) {
			tCpus += entry.getValue();
		}
		for (Map.Entry<String, Integer> entry : tMemMap.entrySet()) {
			tMems += entry.getValue();
			String instanceType = entry.getKey();
			String az = instanceType;
			int mems = entry.getValue();
			int cpus = tCpuMap.get(entry.getKey());
			this.quotaService.checkResource(tenantId, cpus, mems*ParamConstant.MB, 0, null, az, locale);
		}
		
		Map<String, Integer> tFipMap = (Map<String, Integer>) params.get("fips");
		String tFips = null;
		try {
			tFips = mapper.writeValueAsString(tFipMap);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		pool.settFips(tFips);

		Map<String, Integer> tVolMap = (Map<String, Integer>) params.get("volumes");
		String tVolumes = null;
		try {
			tVolumes = mapper.writeValueAsString(tVolMap);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		pool.settVolumes(tVolumes);

		String dbaas = Boolean.toString((Boolean) params.get("dbaas"));
		String maas = Boolean.toString((Boolean) params.get("maas"));
		String vpnaas = Boolean.toString((Boolean) params.get("vpnaas"));
		String lbaas = Boolean.toString((Boolean) params.get("lbaas"));
		String fwaas = Boolean.toString((Boolean) params.get("fwaas"));
		pool.setDbaas(dbaas);
		pool.setMaas(maas);
		pool.setVpnaas(vpnaas);
		pool.setLbaas(lbaas);
		pool.setFwaas(fwaas);

		/* Compute quota */
		List<String> computeQuotaDetailTypes = new ArrayList<String>();
		List<String> cunits = new ArrayList<String>();
		List<Integer> ctotals = new ArrayList<Integer>();
		computeQuotaDetailTypes.add(Message.getMessage(Message.CS_CPU_NAME, locale,false));
		cunits.add(Message.getMessage(Message.CS_COUNT_UNIT, locale,false));
		ctotals.add(tCpus);
		computeQuotaDetailTypes.add(Message.getMessage(Message.CS_MEMORY_NAME, locale,false));
		cunits.add(Message.getMessage(Message.CS_RAM_UNIT, locale,false));
		ctotals.add(tMems);

		/* Network quota */
//		List<String> netQuotaDetailTypes = new ArrayList<String>();
//		List<String> nunits = new ArrayList<String>();
//		List<Integer> ntotals = new ArrayList<Integer>();
		
		for (Map.Entry<String, Integer> entry : tFipMap.entrySet()) {
			String name = entry.getKey();
			int count = entry.getValue();
			if(0 == count)
				continue;
			this.quotaService.checkFipResource(name, count,locale);
		}
		
		/*for (Map.Entry<String, Integer> entry : tFipMap.entrySet()) {
			if (entry.getKey().contains("CTCC")) {
				netQuotaDetailTypes.add(Message.getMessage(Message.CS_CTCC_NAME, false));
				nunits.add(Message.getMessage(Message.CS_COUNT_UNIT, false));
				ntotals.add(entry.getValue());
			}
			if (entry.getKey().contains("CMCC")) {
				netQuotaDetailTypes.add(Message.getMessage(Message.CS_CMCC_NAME, false));
				nunits.add(Message.getMessage(Message.CS_COUNT_UNIT, false));
				ntotals.add(entry.getValue());
			}
			if (entry.getKey().contains("CUCC")) {
				netQuotaDetailTypes.add(Message.getMessage(Message.CS_CUCC_NAME, false));
				nunits.add(Message.getMessage(Message.CS_COUNT_UNIT, false));
				ntotals.add(entry.getValue());
			}
			if (entry.getKey().contains("BGP")) {
				netQuotaDetailTypes.add(Message.getMessage(Message.CS_BGP_NAME, false));
				nunits.add(Message.getMessage(Message.CS_COUNT_UNIT, false));
				ntotals.add(entry.getValue());
			}
		}

		 Storage quota 
		List<String> volumeQuotaDetailTypes = new ArrayList<String>();
		List<String> vunits = new ArrayList<String>();
		List<Integer> vtotals = new ArrayList<Integer>();
		for (Map.Entry<String, Integer> entry : tVolMap.entrySet()) {
			if (entry.getKey().equals("capacity")) {
				volumeQuotaDetailTypes.add(Message.getMessage(Message.CS_CAPACITY_VOLUME_NAME, false));
				vunits.add(Message.getMessage(Message.CS_CAPACITY_UNIT, false));
				vtotals.add(entry.getValue());
			}
			if (entry.getKey().equals("performance")) {
				volumeQuotaDetailTypes.add(Message.getMessage(Message.CS_PERFORMANCE_VOLUME_NAME, false));
				vunits.add(Message.getMessage(Message.CS_CAPACITY_UNIT, false));
				vtotals.add(entry.getValue());
			}
		}*/
		/* Check Resource */
//		List<Host> hosts = this.hostService.getHostList(null, adminToken, null);
//		this.quotaService.checkResource(ostoken, "compute", computeQuotaDetailTypes, cunits, ctotals, hosts);
//		this.quotaService.checkResource(ostoken, ParamConstant.STORAGE, volumeQuotaDetailTypes, vunits, vtotals, hosts);
//		this.quotaService.checkResource(ostoken, ParamConstant.FLOATINGIP, netQuotaDetailTypes, nunits, ntotals, hosts);

		//this.quotaService.checkResoure(tenantId, tCpus, tMems, diskSize, volumeTypeName);
		List<VolumeType> volumeTypes = this.volumeTypeService.getVolumeTypeList(null, ostoken);
		for(VolumeType vt : volumeTypes){
			if(tVolMap.containsKey(vt.getName())){
				int size = tVolMap.get(vt.getName());
				if(size == 0)
					continue;
				this.quotaService.checkResource(tenantId, 0, 0, size, vt.getName(), null, locale); //later change 10/24
			}
		}
		
		/* Create Quota */
//		Quota computeQuota = this.quotaService.createQuota(ostoken,
//				Message.getMessage(Message.CS_QUOTA_COMPUTE_TYPE, false), computeQuotaDetailTypes, cunits, ctotals);
//
//		Quota newworkQuota = this.quotaService.createQuota(ostoken,
//				Message.getMessage(Message.CS_QUOTA_NETWORK_TYPE, false), netQuotaDetailTypes, nunits, ntotals);
//
//		Quota storageQuota = this.quotaService.createQuota(ostoken,
//				Message.getMessage(Message.CS_QUOTA_STORAGE_TYPE, false), volumeQuotaDetailTypes, vunits, vtotals);

		
		// move tenant used quota into pool
		Map<String, Integer> uFips = null;
		Map<String, Integer> uVols = null;
		Map<String, Integer> uCpus = null;
		Map<String, Integer> uMems = null;
		Map<String, Integer> tenantQuota = new HashMap<String, Integer>();
		try {
			uFips = mapper.readValue(pool.getuFips(), new TypeReference<HashMap<String, Integer>>() {
			});
			uVols = mapper.readValue(pool.getuVolumes(), new TypeReference<HashMap<String, Integer>>() {
			});
			uCpus = mapper.readValue(pool.getuCpus(), new TypeReference<HashMap<String, Integer>>() {
			});
			uMems = mapper.readValue(pool.getuMems(), new TypeReference<HashMap<String, Integer>>() {
			});
		} catch (Exception e) {
			log.error(e);
		}
		
		List<String> resourceName = new ArrayList<String>();
		List<Quota> quotas = this.quotaService.getQuotas(null, ostoken);
		for (Quota quota : quotas) {
			if (quota.getQuotaType().equals(ParamConstant.COMPUTE)) {
				List<QuotaDetail> qds = quota.getData();
				for (QuotaDetail qd : qds) {
					for (Map.Entry<String, Integer> entry : tCpuMap.entrySet()) {
						String instanceType = entry.getKey();
						if (qd.getType().equals(instanceType + "_" + ParamConstant.CORE)) {
							if (qd.getUsed() > entry.getValue()) {
								throw new ResourceBusinessException(Message.CS_RESOURCE_INSUFFICIENT,ParamConstant.BAD_REQUEST_RESPONSE_CODE, locale);
							}
							uCpus.put(instanceType, qd.getUsed());
							tenantQuota.put(instanceType + "_" + ParamConstant.CORE, entry.getValue()-qd.getUsed());
							resourceName.add(instanceType + "_" + ParamConstant.CORE);
							this.quotaService.updateQuotaTotal(instanceType + "_" + ParamConstant.CORE, ostoken, entry.getValue());
						}
					}
					for (Map.Entry<String, Integer> entry : tMemMap.entrySet()) {
						String instanceType = entry.getKey();
						if (qd.getType().equals(instanceType + "_" + ParamConstant.RAM)) {
							if (qd.getUsed() > entry.getValue()) {
								throw new ResourceBusinessException(Message.CS_RESOURCE_INSUFFICIENT, ParamConstant.BAD_REQUEST_RESPONSE_CODE, locale);
							}
							uMems.put(instanceType, qd.getUsed());
							tenantQuota.put(instanceType + "_" + ParamConstant.RAM, (entry.getValue()-qd.getUsed())*ParamConstant.MB);
							resourceName.add(instanceType + "_" + ParamConstant.RAM);
							this.quotaService.updateQuotaTotal(instanceType + "_" + ParamConstant.RAM, ostoken, entry.getValue());
						}
					}
				}
			}
			if (quota.getQuotaType().equals(ParamConstant.NETWORK)) {
				List<QuotaDetail> qds = quota.getData();
				for (QuotaDetail qd : qds) {
					for (Map.Entry<String, Integer> entry : tFipMap.entrySet()) {
						String fipType = entry.getKey();
						if (qd.getType().equals(fipType)) {
							if (qd.getUsed() > entry.getValue()) {
								throw new ResourceBusinessException(Message.CS_RESOURCE_INSUFFICIENT, ParamConstant.BAD_REQUEST_RESPONSE_CODE, locale);
							}
							uFips.put(fipType, qd.getUsed());
							tenantQuota.put(fipType, entry.getValue()-qd.getUsed());
							resourceName.add(fipType);
							this.quotaService.updateQuotaTotal(fipType, ostoken, entry.getValue());
						}
					}
				}
			}
			if (quota.getQuotaType().equals(ParamConstant.STORAGE)) {
				List<QuotaDetail> qds = quota.getData();
				for (QuotaDetail qd : qds) {
					for (Map.Entry<String, Integer> entry : tVolMap.entrySet()) {
						String volType = entry.getKey();
						if (qd.getType().equals(volType)) {
							if (qd.getUsed() > entry.getValue()) {
								throw new ResourceBusinessException(Message.CS_RESOURCE_INSUFFICIENT, ParamConstant.BAD_REQUEST_RESPONSE_CODE, locale);
							}
							uVols.put(volType, qd.getUsed());
							tenantQuota.put(volType, entry.getValue()-qd.getUsed());
							resourceName.add(volType);
							this.quotaService.updateQuotaTotal(volType, ostoken, entry.getValue());
						}
					}
				}
			}
		}
		
		try {
			String ufips = mapper.writeValueAsString(uFips);
			String uvols = mapper.writeValueAsString(uVols);
			String ucpus = mapper.writeValueAsString(uCpus);
			String umems = mapper.writeValueAsString(uMems);
			pool.setuFips(ufips);
			pool.setuVolumes(uvols);
			pool.setuCpus(ucpus);
			pool.setuMems(umems);
		} catch (Exception e) {
			log.error(e);
		}
		
//		this.resourceSpecService.updateResourceSpecQuota(ParamConstant.CORE, null, tCpus, true);
//		this.resourceSpecService.updateResourceSpecQuota(ParamConstant.RAM, null, tMems * ParamConstant.MB, true);
//		for (Map.Entry<String, Integer> entry : tVolMap.entrySet()) {
//			this.resourceSpecService.updateResourceSpecQuota(ParamConstant.DISK, entry.getKey(), entry.getValue(), true);
//		}
		resourceSpecService.updateTotalResourcesQuota(tenantQuota, resourceName, true);
//		for (Map.Entry<String, Integer> entry : tFipMap.entrySet()) {
//			this.resourceSpecService.updateResourceSpecQuota(entry.getKey(), null, entry.getValue(), true);
//		}
		this.quotaService.setQuotaSharedStatus(ostoken.getTenantid());
		this.poolEntityMapper.insert(pool);
		this.storeResourceEventInfo(tenantId, pool.getId(), ParamConstant.POOL, null, ParamConstant.ACTIVE_STATUS,
				Util.getCurrentMillionsecond());
		return this.poolEntityMapper.selectByPrimaryKey(pool.getId());
	}

	@SuppressWarnings("unchecked")
	@Override
	public PoolEntity updatePoolEntity(Map<String, Object> params, String poolId, TokenOs ostoken)throws BusinessException {
		PoolEntity pool = this.getPoolEntityById(poolId);
		Locale locale = new Locale(ostoken.getLocale());
		if (pool == null) {
			throw new ResourceBusinessException(Message.CS_POOL_NOT_EXISTS_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE, locale);
		}

		ObjectMapper mapper = new ObjectMapper();
		
		Map<String, Integer> tFips = null;
		Map<String, Integer> tVols = null;
		Map<String, Integer> tCpus = null;
		Map<String, Integer> tMems = null;
		try {
			tFips = mapper.readValue(pool.gettFips(), new TypeReference<HashMap<String, Integer>>() {
			});
			tVols = mapper.readValue(pool.gettVolumes(), new TypeReference<HashMap<String, Integer>>() {
			});
			tCpus = mapper.readValue(pool.gettCpus(), new TypeReference<HashMap<String, Integer>>() {
			});
			tMems = mapper.readValue(pool.gettMems(), new TypeReference<HashMap<String, Integer>>() {
			});
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,  ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		
		Map<String, Integer> tCpuMap = (Map<String, Integer>) params.get("cpus");
		String tCpu = null;
		Map<String, Integer> tMemMap = (Map<String, Integer>) params.get("mems");
		String tMem = null;
		try {
			tCpu = mapper.writeValueAsString(tCpuMap);
			tMem = mapper.writeValueAsString(tMemMap);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		pool.settCpus(tCpu);
		pool.settMems(tMem);

		Map<String,Integer> tenantQuota = new HashMap<String,Integer>();
		List<String> resourceName = new ArrayList<String>();
		
	//	int extCpus=0, extMems=0;
		for (Map.Entry<String, Integer> entry : tCpuMap.entrySet()) {
	//		extCpus += entry.getValue() - tCpus.get(entry.getKey());
			
			tenantQuota.put(entry.getKey() + "_" + ParamConstant.CORE, (entry.getValue()-tCpus.get(entry.getKey())));
			resourceName.add(entry.getKey() + "_" + ParamConstant.CORE);
		}
		for (Map.Entry<String, Integer> entry : tMemMap.entrySet()) {
	//		extMems += entry.getValue() - tMems.get(entry.getKey());
			String instanceType = entry.getKey();
			String az = instanceType;
			int mems = entry.getValue() - tMems.get(entry.getKey());
			int cpus = tCpuMap.get(entry.getKey()) - tCpus.get(entry.getKey());
			this.quotaService.checkResource(pool.getTenantId(), cpus, mems, 0, null, az, locale);
			
			tenantQuota.put(entry.getKey() + "_" + ParamConstant.RAM, (entry.getValue()-tMems.get(entry.getKey()))*ParamConstant.MB);
			resourceName.add(entry.getKey() + "_" + ParamConstant.RAM);
			
		}
		Map<String, Integer> extFipMap = new HashMap<String, Integer>();
		Map<String, Integer> extVolMap = new HashMap<String, Integer>();
		Map<String, Integer> tFipMap = (Map<String, Integer>) params.get("fips");
		String tfips = null;
		
		try {
			tfips = mapper.writeValueAsString(tFipMap);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		pool.settFips(tfips);

		Map<String, Integer> tVolMap = (Map<String, Integer>) params.get("volumes");
		String tVolumes = null;
		try {
			tVolumes = mapper.writeValueAsString(tVolMap);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,  ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		pool.settVolumes(tVolumes);
		
		for (Map.Entry<String, Integer> entry : tFipMap.entrySet()) {
			String name = entry.getKey();
			int extcount = entry.getValue() - tFips.get(name);
			this.quotaService.checkFipResource(name, extcount,locale);
			extFipMap.put(name, extcount);
			
			tenantQuota.put(entry.getKey(),extcount);
			resourceName.add(entry.getKey());
		}
		
		List<VolumeType> volumeTypes = this.volumeTypeService.getVolumeTypeList(null, ostoken);
		for(VolumeType vt : volumeTypes){
			if(tVolMap.containsKey(vt.getName())){
				int extsize = tVolMap.get(vt.getName()) - tVols.get(vt.getName());
				this.quotaService.checkResource(pool.getTenantId(), 0, 0, extsize, vt.getName(), null, locale);
				extVolMap.put(vt.getName(), extsize);
				
				tenantQuota.put(vt.getName(),extsize);
				resourceName.add(vt.getName());
			}
		}
		
		List<Quota> quotas = this.quotaService.getQuotas(null, ostoken);
		for (Quota quota : quotas) {
			if (quota.getQuotaType().equals(Message.getMessage(Message.CS_QUOTA_COMPUTE_TYPE, locale, false).toLowerCase())) {
				List<QuotaDetail> qds = quota.getData();
				for (QuotaDetail qd : qds) {
					for (Map.Entry<String, Integer> entry : tCpuMap.entrySet()) {
						String instanceType = entry.getKey();
						if (qd.getType().equals(instanceType + "_" + ParamConstant.CORE)) {
							this.quotaService.updateQuotaTotal(instanceType + "_" + ParamConstant.CORE, ostoken, entry.getValue());
						}
					}
					for (Map.Entry<String, Integer> entry : tMemMap.entrySet()) {
						String instanceType = entry.getKey();
						if (qd.getType().equals(instanceType + "_" + ParamConstant.RAM)) {
							this.quotaService.updateQuotaTotal(instanceType + "_" + ParamConstant.RAM, ostoken, entry.getValue());
						}
					}
				}
			}
			if (quota.getQuotaType().equals(Message.getMessage(Message.CS_QUOTA_NETWORK_TYPE, locale, false).toLowerCase())) {
				List<QuotaDetail> qds = quota.getData();
				for (QuotaDetail qd : qds) {
					for (Map.Entry<String, Integer> entry : tFipMap.entrySet()) {
						String fipType = entry.getKey();
						if (qd.getType().equals(fipType)) {
							this.quotaService.updateQuotaTotal(fipType, ostoken, entry.getValue());
						}
					}
				}
			}
			if (quota.getQuotaType().equals(Message.getMessage(Message.CS_QUOTA_STORAGE_TYPE, locale, false).toLowerCase())) {
				List<QuotaDetail> qds = quota.getData();
				for (QuotaDetail qd : qds) {
					for (Map.Entry<String, Integer> entry : tVolMap.entrySet()) {
						String volType = entry.getKey();
						if (qd.getType().equals(volType)) {
							this.quotaService.updateQuotaTotal(volType, ostoken, entry.getValue());
						}
					}
				}
			}
		}
		String dbaas = Boolean.toString((Boolean) params.get("dbaas"));
		String maas = Boolean.toString((Boolean) params.get("maas"));
		String vpnaas = Boolean.toString((Boolean) params.get("vpnaas"));
		String lbaas = Boolean.toString((Boolean) params.get("lbaas"));
		String fwaas = Boolean.toString((Boolean) params.get("fwaas"));
		pool.setDbaas(dbaas);
		pool.setMaas(maas);
		pool.setVpnaas(vpnaas);
		pool.setLbaas(lbaas);
		pool.setFwaas(fwaas);

//		this.resourceSpecService.updateResourceSpecQuota(ParamConstant.CORE, null, extCpus, true);
//		this.resourceSpecService.updateResourceSpecQuota(ParamConstant.RAM, null, extMems * ParamConstant.MB, true);
//		for (Map.Entry<String, Integer> entry : extVolMap.entrySet()) {
//			this.resourceSpecService.updateResourceSpecQuota(ParamConstant.DISK, entry.getKey(), entry.getValue(), true);
//		}
//		for (Map.Entry<String, Integer> entry : extFipMap.entrySet()) {
//			this.resourceSpecService.updateResourceSpecQuota(entry.getKey(), null, entry.getValue(), true);
//		}
		resourceSpecService.updateTotalResourcesQuota(tenantQuota, resourceName, true);
		this.storeResourceEventInfo(pool.getTenantId(), pool.getId(), ParamConstant.POOL, ParamConstant.ACTIVE_STATUS, ParamConstant.UPDATED_STATUS,
				Util.getCurrentMillionsecond());
		this.poolEntityMapper.updateByPrimaryKey(pool);
		return this.poolEntityMapper.selectByPrimaryKey(pool.getId());
	}
	
	@Override
	public PoolStack getPoolStack(String id) {
		return this.poolStackMapper.selectByPrimaryKey(id);
	}

	
	/*@SuppressWarnings("unchecked")
	public PoolEntity updatePoolEntity2(Map<String, Object> params, String poolId, TokenOs ostoken)
			throws BusinessException {
		PoolEntity pool = this.getPoolEntityById(poolId);
		if (pool == null) {
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
		}

		int tCpus = (Integer) params.get("cpus");
		int tMems = (Integer) params.get("mems");

		if (tCpus < pool.getuCpus()) {
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
		}

		if (tMems < pool.getuMems()) {
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
		}

		try {
			this.enoughCpuAndMem(tCpus, tMems, poolId, ostoken);
		} catch (BusinessException e1) {
			throw e1;
		}

		pool.settCpus(tCpus);
		pool.settMems(tMems);

		Map<String, Integer> tFipMap = (Map<String, Integer>) params.get("fips");
		String tFips = null;
		ObjectMapper mapper = new ObjectMapper();
		try {
			tFips = mapper.writeValueAsString(tFipMap);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
		}
		pool.settFips(tFips);

		Map<String, Integer> tVolMap = (Map<String, Integer>) params.get("volumes");
		String tVolumes = null;
		try {
			tVolumes = mapper.writeValueAsString(tVolMap);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
		}
		pool.settVolumes(tVolumes);

		String dbaas = Boolean.toString((Boolean) params.get("dbaas"));
		String maas = Boolean.toString((Boolean) params.get("maas"));
		String vpnaas = Boolean.toString((Boolean) params.get("vpnaas"));
		String lbaas = Boolean.toString((Boolean) params.get("lbaas"));
		String fwaas = Boolean.toString((Boolean) params.get("fwaas"));
		pool.setDbaas(dbaas);
		pool.setMaas(maas);
		pool.setVpnaas(vpnaas);
		pool.setLbaas(lbaas);
		pool.setFwaas(fwaas);

		this.poolEntityMapper.updateByPrimaryKey(pool);
		return this.poolEntityMapper.selectByPrimaryKey(pool.getId());
	}*/

	/*private Boolean enoughCpuAndMem(int cpu, int mem, String updatePool, TokenOs ostoken) throws BusinessException {
		try {
			List<Hypervisor> hvs = hypervisorService.listHypervisorDetail(ostoken, null);
			float freeMems = 0;
			float freeCpus = 0;

			float ram_allocation_ratio = 1.5f;
			float cpu_allocation_ratio = 16.0f;
			for (Hypervisor hv : hvs) {
				if (hv.getStatus().equals("enabled") && hv.getState().equals("up")) {
					freeMems += hv.getFree_ram_mb() * ram_allocation_ratio;
					float freecpu = hv.getVcpus() * cpu_allocation_ratio - hv.getVcpus_used();
					freeCpus += freecpu;
				}
			}
			System.out.println("FREE CPU: " + freeCpus + ", FREE MEM:" + freeMems);

			float reqCpu = cpu;
			float reqMem = mem;
			List<PoolEntity> pl = this.poolEntityMapper.selectAll();
			for (PoolEntity p : pl) {
				if (p.getId().equals(updatePool)) {
					continue;
				}
				reqCpu += p.gettCpus();
				reqMem += p.gettMems();
			}
			System.out.println("REQ CPU: " + reqCpu + ", REQ MEM:" + reqMem);
			if (reqCpu > freeCpus) {
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
			}
			if (reqMem > freeMems) {
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
			}

		} catch (BusinessException e1) {
			e1.printStackTrace();
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
		}

		return false;
	}

	private Boolean enoughCpuAndMem2(int cpu, int mem, String updatePool, TokenOs ostoken) throws BusinessException {
		try {
			List<Hypervisor> hvs = hypervisorService.listHypervisorDetail(ostoken, null);
			float freeMems = 0;
			float freeCpus = 0;

			float ram_allocation_ratio = 1.5f;
			float cpu_allocation_ratio = 16.0f;
			for (Hypervisor hv : hvs) {
				if (hv.getStatus().equals("enabled") && hv.getState().equals("up")) {
					freeMems += hv.getFree_ram_mb() * ram_allocation_ratio;
					float freecpu = hv.getVcpus() * cpu_allocation_ratio - hv.getVcpus_used();
					freeCpus += freecpu;
				}
			}
			System.out.println("FREE CPU: " + freeCpus + ", FREE MEM:" + freeMems);

			float reqCpu = cpu;
			float reqMem = mem;
			List<PoolEntity> pl = this.poolEntityMapper.selectAll();
			for (PoolEntity p : pl) {
				if (p.getId().equals(updatePool)) {
					continue;
				}
				reqCpu += p.gettCpus();
				reqMem += p.gettMems();
			}
			System.out.println("REQ CPU: " + reqCpu + ", REQ MEM:" + reqMem);
			if (reqCpu > freeCpus) {
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
			}
			if (reqMem > freeMems) {
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
			}

		} catch (BusinessException e1) {
			e1.printStackTrace();
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
		}

		return false;
	}*/

	@SuppressWarnings("unchecked")
	private PoolEntity initNewPoolEntity(Map<String, Object> params) {
		Map<String, Integer> fip = (Map<String, Integer>) params.get("fips");
		Map<String, Integer> ufip = new LinkedHashMap<>();
		for (Map.Entry<String, Integer> entry : fip.entrySet()) {
			ufip.put(entry.getKey(), 0);
		}

		Map<String, Integer> vol = (Map<String, Integer>) params.get("volumes");
		Map<String, Integer> uvol = new LinkedHashMap<>();
		for (Map.Entry<String, Integer> entry : vol.entrySet()) {
			uvol.put(entry.getKey(), 0);
		}
		
		Map<String, Integer> cpu = (Map<String, Integer>) params.get("cpus");
		Map<String, Integer> ucpu = new LinkedHashMap<>();
		for (Map.Entry<String, Integer> entry : cpu.entrySet()) {
			ucpu.put(entry.getKey(), 0);
		}
		
		Map<String, Integer> mem = (Map<String, Integer>) params.get("mems");
		Map<String, Integer> umem = new LinkedHashMap<>();
		for (Map.Entry<String, Integer> entry : mem.entrySet()) {
			umem.put(entry.getKey(), 0);
		}

		PoolEntity p = new PoolEntity();
		p.setId(Util.makeUUID());
		List<LinkedHashMap<String, String>> stacks = new ArrayList<LinkedHashMap<String, String>>();
		ObjectMapper mapper = new ObjectMapper();
		try {
			p.setuFips(mapper.writeValueAsString(ufip));
			p.setuVolumes(mapper.writeValueAsString(uvol));
			p.setStacks(mapper.writeValueAsString(stacks));
			p.setuCpus(mapper.writeValueAsString(ucpu));
			p.setuMems(mapper.writeValueAsString(umem));
		} catch (Exception e) {
			log.error(e);
		}
		return p;
	}
	
	private void storeResourceEventInfo(String tenantId, String id, String type, String beginState, String endState,
			long time) {
		ResourceEvent event = new ResourceEvent();
		event.setTenantId(tenantId);
		event.setResourceId(id);
		event.setResourceType(type);
		event.setBeginState(beginState);
		event.setEndState(endState);
		event.setMillionSeconds(time);
		resourceEventMapper.insertSelective(event);
	}

	@Override
	public List<PoolEntity> listPoolEntity(TokenOs ostoken) {
		String tenantId = ostoken.getTenantid();
		return this.poolEntityMapper.selectByTenantId(tenantId);
	}

	@Override
	public PoolEntity getPoolEntityById(String id) {
		return this.poolEntityMapper.selectByPrimaryKey(id);
	}

	@Override
	public void updatePoolEntity(PoolEntity p) {
		this.poolEntityMapper.updateByPrimaryKey(p);
	}

	@Override
	public PoolEntity getPoolEntityByTenantId(String id) {
		List<PoolEntity> pl = this.poolEntityMapper.selectByTenantId(id);
		if (pl.size() == 0) {
			return null;
		}
		return this.poolEntityMapper.selectByTenantId(id).get(0);
	}

	@Override
	public List<PoolStack> listPoolStackByPoolId(String poolId) {
		List<PoolStack> psl = this.poolStackMapper.selectByPoolId(poolId);
		return psl;
	}

	@Override
	public int createPoolStack(PoolStack poolStack) {
		int result = this.poolStackMapper.insert(poolStack);
		return result;
	}

	@Override
	public int deletePoolStack(String id) {
		int result = this.poolStackMapper.deleteByPrimaryKey(id);
		return result;
	}

	@Override
	public int updatePoolStack(PoolStack poolStack) {
		int result = this.poolStackMapper.updateByPrimaryKeySelective(poolStack);
		return result;
	}

}
