package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.service.openstackapi.ImageService;
import com.cloud.cloudapi.service.openstackapi.NetworkService;
import com.cloud.cloudapi.service.openstackapi.PoolEntityService;
import com.cloud.cloudapi.service.openstackapi.SubnetService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class StackResource {
	private String name;
	private String id;
	private String updatedAt;
	private String[] requiredBy;
	private String status;
	private String statusReason;
	private String physicalResourceId;
	private String resourceType;
	private String attributes;
	private String createdAt;
	
	private String stackId;
	private String required;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(String updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String[] getRequiredBy() {
		return requiredBy;
	}

	public void setRequiredBy(String[] requiredBy) {
		this.requiredBy = requiredBy;
		if(requiredBy == null || requiredBy.length == 0){
			return;
		}
		String temp = "";
		for (String r : requiredBy) {
			temp += r + ";";
		}
		this.required = temp.substring(0, temp.length() - 1);
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStatusReason() {
		return statusReason;
	}

	public void setStatusReason(String statusReason) {
		this.statusReason = statusReason;
	}

	public String getPhysicalResourceId() {
		return physicalResourceId;
	}

	public void setPhysicalResourceId(String physicalResourceId) {
		this.physicalResourceId = physicalResourceId;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public String getAttributes() {
		return attributes;
	}

	public void setAttributes(String attributes) {
		this.attributes = attributes;
	}

	public static String toJSON(StackResource sr) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("id", sr.getPhysicalResourceId());
		map.put("name", sr.getName());

		String attr = sr.getAttributes();
		Map<String, Object> attrMap = null;
		try {
			attrMap = mapper.readValue(attr, new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception e) {
			Logger log = LogManager.getLogger(StackResource.class);
			log.error(e);
		}
		if (attrMap.get("status") != null) {
			map.put("status", attrMap.get("status"));
		}
		if (sr.getResourceType().equals("OS::Nova::Server")) {
			map.put("type", "instance");
		} else if (sr.getResourceType().equals("OS::Cinder::Volume")) {
			map.put("type", "volume");
		} else if (sr.getResourceType().equals("OS::Nova::FloatingIP")) {
			map.put("type", "floatingIp");
		} else {
			return null;
		}
		map.put("updateAt", sr.getUpdatedAt());
		map.put("createAt", sr.getUpdatedAt());
		try {
			return mapper.writeValueAsString(map);
		} catch (Exception e) {
			Logger log = LogManager.getLogger(StackResource.class);
			log.error(e);
		}
		return null;
	}

	public static String toJSON(List<StackResource> srl) {
		StringBuffer sb = new StringBuffer("[");
		for (StackResource p : srl) {
			String sp = StackResource.toJSON(p);
			if (sp == null) {
				continue;
			}
			sb.append(sp).append(",");
		}
		return sb.substring(0, sb.length() - 1) + "]";
	}

	public static String toJSON(Stack s, List<StackResource> srl, PoolEntityService poolEntityService, ImageService imageService,
			NetworkService networkService, SubnetService subnetService, TokenOs authToken) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		String stackId= s.getId();
		PoolStack poolstack = poolEntityService.getPoolStack(stackId);
		map.put("id", poolstack.getId());
		map.put("name", poolstack.getDisplayName());
		map.put("description", "");
		map.put("status", poolstack.getStatus());
		map.put("createdAt", poolstack.getCreateAt());
		map.put("updatedAt", poolstack.getUpdateAt());

		List<Map<String, Object>> compute = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> storage = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> network = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> router = new ArrayList<Map<String, Object>>();
		List<Image> imageList = new ArrayList<Image>();//null;
		List<Network> networkList = null;
		List<Subnet> subnetkList = null;
		Map<String, String> resourceIdNameMap = null;
		Map<String, String> resourceNameIdMap = null;
		Map<String, List<String>> netInstanceRelation = null;
		Map<String, List<String>> routerInstanceRelation = null;
		Map<String, List<String>> routerNetRelation = null;
		Map<String, List<String>> instanceNetRelation = null;
		Map<String, String> routerSubnetGateway = null;
		try {
			resourceIdNameMap = getResourceIdNameMap(srl);
			resourceNameIdMap = getResourceNameIdMap(srl);
			Map<String,List<Image>> images = imageService.getImageList(null, authToken);
			for(Map.Entry<String,List<Image>>  entry : images.entrySet()){
				imageList.addAll(entry.getValue());
			}
			networkList = networkService.getNetworkList(null, authToken);
			subnetkList = subnetService.getSubnetList(null, authToken);
			netInstanceRelation = getNetInstanceRelation(srl);
			routerNetRelation = getRouterNetRelation(srl);
			routerInstanceRelation = getRouterInstanceRelation(routerNetRelation, netInstanceRelation,
					resourceIdNameMap);
			instanceNetRelation = getInstanceNetRelation(netInstanceRelation, resourceNameIdMap);
			routerSubnetGateway = getRouterSubnetGateway(srl, subnetkList);
		} catch (BusinessException e1) {
			e1.printStackTrace();
		}
		for (StackResource sr : srl) {
			if (sr.getResourceType().equals("OS::Nova::Server")) {
				Map<String, Object> m = new LinkedHashMap<String, Object>();
				m.put("id", sr.getPhysicalResourceId());
				m.put("name", sr.getName());
				m.put("type", "instance");
				m.put("status", getResourceStatus(sr.getAttributes()));
				m.put("image", getImageFromServerAttrMeta(sr.getAttributes(), imageList));
				m.put("createdAt", s.getCreatedAt());
				m.put("updatedAt", sr.getUpdatedAt());
				Map<String, List<String>> networkInfo = getNetworkFromServerAttr(sr.getAttributes(), networkList);
				m.put("ips", networkInfo.get("ips"));
				// m.put("networks", networkInfo.get("networks"));
				m.put("networks", instanceNetRelation.get(sr.getPhysicalResourceId()));
				m.put("floatingIps", networkInfo.get("floatingIps"));
				compute.add(m);
			} else if (sr.getResourceType().equals("OS::Cinder::Volume")) {
				Map<String, Object> m = new LinkedHashMap<String, Object>();
				m.put("id", sr.getPhysicalResourceId());
				m.put("name", sr.getName());
				m.put("status", getResourceStatus(sr.getAttributes()));
				m.put("createdAt", s.getCreatedAt());
				m.put("updatedAt", sr.getUpdatedAt());
				m.put("type", "volume");
				storage.add(m);
			} else if (sr.getResourceType().equals("OS::Neutron::Net")) {
				Map<String, Object> m = new LinkedHashMap<String, Object>();
				m.put("id", sr.getPhysicalResourceId());
				m.put("name", sr.getName());
				m.put("status", getResourceStatus(sr.getAttributes()));
				m.put("createdAt", s.getCreatedAt());
				m.put("updatedAt", sr.getUpdatedAt());
				m.put("type", "network");
				m.put("servers", netInstanceRelation.get(sr.getName()));
				network.add(m);
			} else if (sr.getResourceType().equals("OS::Neutron::Router")) {
				Map<String, Object> m = new LinkedHashMap<String, Object>();
				m.put("id", sr.getPhysicalResourceId());
				m.put("name", sr.getName());
				m.put("status", getResourceStatus(sr.getAttributes()));
				m.put("createdAt", s.getCreatedAt());
				m.put("updatedAt", sr.getUpdatedAt());
				m.put("type", "router");
				m.put("servers", routerInstanceRelation.get(sr.getName()));
				m.put("networks", routerNetRelation.get(sr.getName()));
				m.put("gateway", routerSubnetGateway.get(sr.getPhysicalResourceId()));
				router.add(m);
			} else {
				continue;
			}
		}
		map.put("compute", compute);
		map.put("storage", storage);
		map.put("network", network);
		map.put("router", router);
		try {
			return mapper.writeValueAsString(map);
		} catch (Exception e) {
			Logger log = LogManager.getLogger(StackResource.class);
			log.error(e);
		}
		return null;
	}

	public static String getResourceStatus(String attr) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> attrMap = null;
		try {
			attrMap = mapper.readValue(attr, new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception e) {
			Logger log = LogManager.getLogger(StackResource.class);
			log.error(e);
		}
		if (attrMap.get("status") != null) {
			return (String) attrMap.get("status");
		}
		return "";
	}

	@SuppressWarnings("unchecked")
	public static Map<String, String> getImageFromServerAttrMeta(String attr, List<Image> imageList) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> attrMap = null;
		try {
			attrMap = mapper.readValue(attr, new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception e) {
			Logger log = LogManager.getLogger(StackResource.class);
			log.error(e);
		}
		Map<String, String> meta = (Map<String, String>) attrMap.get("metadata");

		Map<String, String> imageInfo = new HashMap<String, String>();
		String imageId = meta.get("image_id");
		Image image = null;
		for (Image i : imageList) {
			if (i.getId().equals(imageId)) {
				image = i;
				break;
			}
		}

		imageInfo.put("id", imageId);
		if (image != null) {
			imageInfo.put("name", image.getName());
		}
		return imageInfo;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, List<String>> getNetworkFromServerAttr(String attr, List<Network> networkList) {
		List<String> ips = new ArrayList<String>();
		List<String> networks = new ArrayList<String>();
		List<String> floatingIps = new ArrayList<String>();
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> attrMap = null;
		try {
			attrMap = mapper.readValue(attr, new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception e) {
			Logger log = LogManager.getLogger(StackResource.class);
			log.error(e);
		}
		try {
			Map<String, Object> addresses = (Map<String, Object>) attrMap.get("addresses");
			for (String key : addresses.keySet()) {
				for (Network net : networkList) {
					if (net.getName().equals(key)) {
						networks.add(net.getId());
					}
				}
				List<Map<String, String>> details = (List<Map<String, String>>) addresses.get(key);
				for (Map<String, String> map : details) {
					if (map.get("OS-EXT-IPS:type").equals("fixed")) {
						ips.add(map.get("addr"));

					} else if (map.get("OS-EXT-IPS:type").equals("floating")) {
						floatingIps.add(map.get("addr"));

					}
				}
			}
		} catch (Exception e) {
			Logger log = LogManager.getLogger(StackResource.class);
			log.error(e);
		}
		result.put("ips", ips);
		result.put("networks", networks);
		result.put("floatingIps", floatingIps);
		return result;
	}

	public static Map<String, List<String>> getNetInstanceRelation(List<StackResource> srl) {
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		for (StackResource sr : srl) {
			if (sr.getResourceType().equals("OS::Neutron::Net")) {
				String[] required_by = sr.getRequiredBy();
				List<String> instanceList = new ArrayList<String>();
				for (String r : required_by) {
					for (StackResource sr2 : srl) {
						if (sr2.getResourceType().equals("OS::Nova::Server") && sr2.getName().equals(r)) {
							instanceList.add(sr2.getPhysicalResourceId());
							break;
						}
					}
				}
				result.put(sr.getName(), instanceList);
			}
		}
		return result;
	}

	public static Map<String, List<String>> getRouterNetRelation(List<StackResource> srl) {
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		for (StackResource sr : srl) {
			if (sr.getResourceType().equals("OS::Neutron::Router")) {
				String name = sr.getName();
				List<String> netList = new ArrayList<String>();
				for (StackResource sr2 : srl) {
					if (sr2.getResourceType().equals("OS::Neutron::Net") && name.startsWith(sr2.getName())) {
						netList.add(sr2.getPhysicalResourceId());
						break;
					}
				}
				result.put(sr.getName(), netList);
			}
		}
		return result;
	}

	public static Map<String, List<String>> getRouterInstanceRelation(Map<String, List<String>> rn,
			Map<String, List<String>> ni, Map<String, String> idName) {
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		for (String router : rn.keySet()) {
			List<String> list = new ArrayList<String>();
			List<String> netList = rn.get(router);
			for (String net : netList) {
				String n = idName.get(net);
				list.addAll(ni.get(n));
			}
			result.put(router, list);
		}
		return result;
	}

	public static Map<String, List<String>> getInstanceNetRelation(Map<String, List<String>> ni,
			Map<String, String> resourceNameIdMap) {
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		for (String net : ni.keySet()) {
			List<String> list = ni.get(net);
			for (String ins : list) {
				if (result.get(ins) == null) {
					List<String> nets = new ArrayList<String>();

					nets.add(resourceNameIdMap.get(net));
					result.put(ins, nets);
				} else {
					List<String> nets = result.get(ins);
					if (!nets.contains(resourceNameIdMap.get(net))) {
						nets.add(resourceNameIdMap.get(net));
					}
				}
			}

		}
		return result;
	}

	public static Map<String, String> getRouterSubnetGateway(List<StackResource> srl, List<Subnet> subnetList) {
		Map<String, String> result = new HashMap<String, String>();
		for (StackResource sr : srl) {
			if (sr.getResourceType().equals("OS::Neutron::RouterInterface")) {
				String phyId = sr.getPhysicalResourceId();
				String[] phyIdSplit = phyId.split(":");
				String routerId = phyIdSplit[0];
				String subnetId = phyIdSplit[1].split("=")[1];
				String gateway = "";
				for (Subnet s : subnetList) {
					if (s.getId().equals(subnetId)) {
						gateway = s.getGateway();
						break;
					}
				}
				result.put(routerId, gateway);
			}
		}
		return result;
	}

	public static Map<String, String> getResourceIdNameMap(List<StackResource> srl) {
		Map<String, String> result = new HashMap<String, String>();
		for (StackResource sr : srl) {
			result.put(sr.getPhysicalResourceId(), sr.getName());
		}
		return result;
	}

	public static Map<String, String> getResourceNameIdMap(List<StackResource> srl) {
		Map<String, String> result = new HashMap<String, String>();
		for (StackResource sr : srl) {
			result.put(sr.getName(), sr.getPhysicalResourceId());
		}
		return result;
	}

	public String getStackId() {
		return stackId;
	}

	public void setStackId(String stackId) {
		this.stackId = stackId;
	}

	public String getRequired() {
		return required;
	}

	public void setRequired(String required) {
		this.required = required;
		if (required == null || required.equals("")) {
			return;
		}
		this.requiredBy = required.split(";");
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}
}
