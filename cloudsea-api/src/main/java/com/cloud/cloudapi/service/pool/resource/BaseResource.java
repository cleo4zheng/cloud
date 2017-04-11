package com.cloud.cloudapi.service.pool.resource;

import java.util.Map;

public abstract class BaseResource {
	public abstract String getResourceName();

	public abstract Map<String, Object> getResourceMap();
}
