// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iothubmanager.webservice.v1.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.iotsolutions.iothubmanager.services.models.DeploymentServiceListModel;
import com.microsoft.azure.iotsolutions.iothubmanager.webservice.v1.Version;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;

public class DeploymentListApiModel {
    private List<DeploymentApiModel> items;

    public DeploymentListApiModel() {}

    public DeploymentListApiModel(final DeploymentServiceListModel deployments) {
        if (CollectionUtils.isNotEmpty(deployments.getItems())) {
            this.items = deployments.getItems().stream().map(DeploymentApiModel::new)
                    .collect(Collectors.toList());
        } else {
            this.items = Collections.emptyList();
        }
    }

    @JsonProperty("$metadata")
    public Dictionary<String, String> getMetadata() {
        return new Hashtable<String, String>() {{
            put("$type", "DeploymentsList;" + Version.NUMBER);
            put("$uri", "/" + Version.PATH + "/deployments");
        }};
    }

    @JsonProperty("Items")
    public List<DeploymentApiModel> getItems() {
        return this.items;
    }
}
