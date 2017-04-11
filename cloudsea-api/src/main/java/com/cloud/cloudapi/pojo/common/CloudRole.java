package com.cloud.cloudapi.pojo.common;

public class CloudRole {
    private String id;

    private String roleName;

    private String roleSign;

    private String displayPermission;
    
    private String operationPermission;
    
    private String description;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName == null ? null : roleName.trim();
    }

    public String getRoleSign() {
        return roleSign;
    }

    public void setRoleSign(String roleSign) {
        this.roleSign = roleSign == null ? null : roleSign.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }

    
	public String getDisplayPermission() {
		return displayPermission;
	}

	public void setDisplayPermission(String displayPermission) {
		this.displayPermission = displayPermission;
	}

	public String getOperationPermission() {
		return operationPermission;
	}

	public void setOperationPermission(String operationPermission) {
		this.operationPermission = operationPermission;
	}

	public void normalInfo(){
		this.roleSign = null;
	}
	
	@Override
    public String toString() {
        return "Role [id=" + id + ", roleName=" + roleName + ", roleSign=" + roleSign + ", description=" + description + "]";
    }

}
