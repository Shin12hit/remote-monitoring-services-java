// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iothubmanager.services.runtime;

import java.util.List;

/**
 * Service layer configuration
 */
public class ServicesConfig implements IServicesConfig {

    private String hubConnString;
    private String storageAdapterServiceUrl;
    private String userManagementApiUrl;
    private int devicePropertiesTTL;
    private int devicePropertiesRebuildTimeout;
    private List<String> devicePropertiesWhiteList;

    public ServicesConfig(final String hubConnString, final String storageAdapterServiceUrl, String userManagementApiUrl,
            int devicePropertiesTTL, int devicePropertiesRebuildTimeout,
            List<String> devicePropertiesWhiteList) {
        this.hubConnString = hubConnString;
        this.storageAdapterServiceUrl = storageAdapterServiceUrl;
        this.userManagementApiUrl = userManagementApiUrl;
        this.devicePropertiesWhiteList = devicePropertiesWhiteList;
        this.devicePropertiesTTL = devicePropertiesTTL;
        this.devicePropertiesRebuildTimeout = devicePropertiesRebuildTimeout;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHubConnString() {
        return this.hubConnString;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStorageAdapterServiceUrl() {
        return storageAdapterServiceUrl;
    }

    /**
     * Get user management dependency url
     *
     * @return url for user management endpoint
     */
    public String getUserManagementApiUrl() {
        return this.userManagementApiUrl;
    }

    @Override
    public int getDevicePropertiesTTL() {
        return devicePropertiesTTL;
    }

    @Override
    public int getDevicePropertiesRebuildTimeout() {
        return devicePropertiesRebuildTimeout;
    }

    @Override
    public List<String> getDevicePropertiesWhiteList() {
        return devicePropertiesWhiteList;
    }
}
