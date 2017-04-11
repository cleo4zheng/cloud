package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.Locale;

import com.cloud.cloudapi.util.Message;

public class ResourceUsedInfo {

	private String name;
	private String hostDisplayName;
	private String source;
	private HostDetail cpu;
	private HostDetail mem;
	private HostDetail disk;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHostDisplayName() {
		return hostDisplayName;
	}

	public void setHostDisplayName(String hostDisplayName) {
		this.hostDisplayName = hostDisplayName;
	}

	public HostDetail getCpu() {
		return cpu;
	}

	public void setCpu(HostDetail cpu) {
		this.cpu = cpu;
	}

	public HostDetail getMem() {
		return mem;
	}

	public void setMem(HostDetail mem) {
		this.mem = mem;
	}

	public HostDetail getDisk() {
		return disk;
	}

	public void setDisk(HostDetail disk) {
		this.disk = disk;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void normalInfo(Locale locale){
		
		/*
		if (Util.isNullOrEmptyValue(this.name)){
			this.hostDisplayName = Message.getMessage("DEFAULT-COMPUTE", locale,false);
		}else{
			try {
				this.hostDisplayName = Message.getMessage(this.name.toUpperCase(), locale, false);
			} catch (Exception e) {
				this.hostDisplayName = Message.getMessage("DEFAULT-COMPUTE",locale,false);
			}	
		}*/
		if(null == this.hostDisplayName)
			this.hostDisplayName = this.name;
		this.name = null;
		if(null != this.cpu){
			//String coreType = "CS_CORE_TYPE_NAME";
			//String type = this.cpu.getType().substring(0, this.cpu.getType().indexOf('_'));
			//String coreTypeName = coreType.replaceFirst("TYPE", type.toUpperCase());
			//this.cpu.setTypeName(Message.getMessage(coreTypeName, locale,false));
			this.cpu.setUnitName(Message.getMessage(this.cpu.getUnit(),locale,false));
			this.cpu.setName(null);
			this.cpu.setUnit(null);
			if(null == this.cpu.getUsed())
				this.cpu.setUsed(0);
		}
		if(null != this.mem){
			//String ramType = "CS_RAM_TYPE_NAME";
			//String type = this.mem.getType().substring(0, this.mem.getType().indexOf('_'));
			//String ramTypeName = ramType.replaceFirst("TYPE", type.toUpperCase());
			//this.mem.setTypeName(Message.getMessage(ramTypeName, locale,false));
			this.mem.setUnitName(Message.getMessage(this.mem.getUnit(),locale,false));
			this.mem.setName(null);
			this.mem.setUnit(null);
			if(null == this.mem.getUsed())
				this.mem.setUsed(0);
		}
		if(null != this.disk){
			this.disk.setTypeName(Message.getMessage(Message.CS_DISK_NAME,locale,false));
			this.disk.setUnitName(Message.getMessage(this.disk.getUnit(),locale,false));
			this.disk.setName(null);
			this.disk.setUnit(null);
			if(null == this.disk.getUsed())
				this.disk.setUsed(0);
		}
	}
}
