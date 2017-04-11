package com.cloud.cloudapi.service.pool.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NovaServer extends BaseResource {
	public static final String OS_WINDOWS="windows";
	public static final String OS_LINUX="linux";
	
	private int no;
	private final String type = "OS::Nova::Server";
	private String name = null;
	private String flavor = null;
	private List<Map<String, Object>> block_device_mapping_v2 = null;
	private List<Map<String, String>> networks = null;
	private String image = null;
	private String volume_size = null;
	private String volume_type = null;
	private String resoureName = null;
	private String sysvol_res_name = null;
	private String netResourceName = null;
	private String availability_zone = null;
	private boolean enable_volume = true;
	private String key_name = null;
	private String os_type = null;
	private String username = null;
	private String password = null;

	public NovaServer(int no, String name, String flavor, String[] nets, String image, String volume_size,
			String volume_type, String availability_zone, boolean enable_volume, String key_name) {
		this.no = no;
		this.name = name + String.format("_%03d", this.no);
		this.flavor = flavor;
		this.setNetworks(nets);
		this.image = image;
		this.volume_size = volume_size;
		this.volume_type = volume_type;
		this.resoureName = this.name;
		this.sysvol_res_name = String.format("sysvol-%03d", this.no);
		this.setBDMv2(sysvol_res_name);
		this.availability_zone = availability_zone;
		this.enable_volume = enable_volume;
		this.key_name = key_name;
	}

	public NovaServer(int no, String name, String flavor, String netResourceName, String image, String volume_size,
			String volume_type, String availability_zone, boolean enable_volume, String key_name) {
		this.no = no;
		this.name = name + String.format("_%03d", this.no);
		this.flavor = flavor;
		this.netResourceName = netResourceName;
		this.image = image;
		this.volume_size = volume_size;
		this.volume_type = volume_type;
		this.resoureName = this.name;
		// this.sysvol_res_name = String.format("sysvol-%03d", this.no);
		this.sysvol_res_name = this.name + "_sysvol";
		this.setBDMv2(sysvol_res_name);
		this.availability_zone = availability_zone;
		this.enable_volume = enable_volume;
		this.key_name = key_name;
	}

	public NovaServer(int no, String name, String flavor, String netResourceName, String image, String volume_size,
			String volume_type, String availability_zone, boolean enable_volume, String key_name, String os_type,
			String username, String password) {
		this.no = no;
		this.name = name;
		this.flavor = flavor;
		this.netResourceName = netResourceName;
		this.image = image;
		this.volume_size = volume_size;
		this.volume_type = volume_type;
		this.resoureName = this.name;
		// this.sysvol_res_name = String.format("sysvol-%03d", this.no);
		//this.sysvol_res_name = this.name + "_sysvol";
		this.sysvol_res_name = this.name + "_volume-1";
		this.setBDMv2(sysvol_res_name);
		this.availability_zone = availability_zone;
		this.enable_volume = enable_volume;
		this.key_name = key_name;
		this.os_type = os_type;
		this.username = username;
		this.password = password;
	}

	public void resetName(String name) {
		this.name = name + String.format("_%03d", this.no);
		this.resoureName = this.name;
	}

	@Override
	public String getResourceName() {
		return this.resoureName;
	}

	private void setBDMv2(String vol_res) {
		Map<String, Object> bdm = new HashMap<String, Object>();
		Map<String, String> vid = new HashMap<String, String>();
		vid.put("get_resource", vol_res);
		bdm.put("volume_id", vid);
		bdm.put("device_type", "disk");
		bdm.put("boot_index", "0");
		bdm.put("delete_on_termination", "true");
		List<Map<String, Object>> bdmv2 = new ArrayList<Map<String, Object>>();
		bdmv2.add(bdm);
		this.block_device_mapping_v2 = bdmv2;
	}

	private void setNetworks(String[] nets) {
		List<Map<String, String>> networks = new ArrayList<Map<String, String>>();
		for (String net : nets) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("network", net);
			networks.add(map);
		}
		this.networks = networks;
	}

	@Override
	public Map<String, Object> getResourceMap() {
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("name", this.name);
		properties.put("flavor", this.flavor);
		if (!enable_volume) {
			properties.put("image", this.image);
		} else {
			properties.put("block_device_mapping_v2", this.block_device_mapping_v2);
		}
		if (netResourceName != null) {
			List<Map<String, Object>> ll = new ArrayList<Map<String, Object>>();
			Map<String, String> r1 = new LinkedHashMap<String, String>();
			r1.put("get_resource", this.netResourceName);
			Map<String, Object> r2 = new LinkedHashMap<String, Object>();
			r2.put("network", r1);
			ll.add(r2);
			properties.put("networks", ll);
		} else if (this.networks != null) {
			properties.put("networks", this.networks);
		}
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("image_id", this.image);
		metadata.put("pool", "true");
		if(this.os_type.equals(OS_WINDOWS)){
			metadata.put("admin_pass", password);
		}
		if(this.os_type.equals(OS_LINUX)){
			Map<String, Object> cloudConfig = new LinkedHashMap<String, Object>();
			Map<String, Object> chpasswd = new LinkedHashMap<String, Object>();
//			chpasswd.put("expire", "False");
//			cloudConfig.put("password", password);
//			cloudConfig.put("chpasswd", chpasswd);
//			cloudConfig.put("ssh_pwauth", "True");
			List<Map<String, String>> l = new ArrayList<Map<String, String>>();
			Map<String, String>	s = new HashMap<String, String>();
			s.put("root", password);
			l.add(s);
			chpasswd.put("list", "root:" + password);
			//chpasswd.put("root", password);
			chpasswd.put("expire", false);
			cloudConfig.put("chpasswd", chpasswd);
			
			String up = "#cloud-config\nchpasswd:\n list: |\n   root:" + password + "\n expire: False\n";
			
			Map<String, Object> properties_cc = new LinkedHashMap<String, Object>();
			properties_cc.put("cloud_config", cloudConfig);
			
			Map<String, Object> cc = new LinkedHashMap<String, Object>();
			cc.put("type", "OS::Heat::CloudConfig");
			cc.put("properties", properties_cc);
			String ccName = resoureName + "_cloudconfig";
			res.put(ccName, cc);
			
			properties.put("user_data_format", "RAW");
			Map<String, String> udr = new LinkedHashMap<String, String>();
			udr.put("get_resource", ccName);
//			properties.put("user_data", udr);
			properties.put("user_data", up);
			properties.put("user_data_format", "RAW");
		}
		
		properties.put("metadata", metadata);
		properties.put("availability_zone", this.availability_zone);
		properties.put("key_name", this.key_name);

		Map<String, Object> server = new LinkedHashMap<String, Object>();
		server.put("type", this.type);
		server.put("properties", properties);
		if (netResourceName != null) {
			server.put("depends_on", this.netResourceName + "_subnet");
		}

		Map<String, Object> properties_v = new LinkedHashMap<String, Object>();
		properties_v.put("image", this.image);
		properties_v.put("size", this.volume_size);
		properties_v.put("name", this.name+"_volume-1");
		properties_v.put("volume_type", this.volume_type);
		Map<String, Object> metadata_v = new LinkedHashMap<String, Object>();
		metadata_v.put("pool", "true");
		properties_v.put("metadata", metadata_v);

		Map<String, Object> vol = new LinkedHashMap<String, Object>();
		vol.put("type", "OS::Cinder::Volume");
		vol.put("properties", properties_v);

		

		res.put(this.resoureName, server);
		if (enable_volume) {
			res.put(this.sysvol_res_name, vol);
		}
		return res;
	}
	
	public static void main(String[] args){
		String up = "#cloud-config\nchpasswd:\n list: |\n   root:123456\n expire:False\n";
		System.out.println(up);
	}
}
