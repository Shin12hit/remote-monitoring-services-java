// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iothubmanager.services;

import com.microsoft.azure.iotsolutions.iothubmanager.services.exceptions.InvalidInputException;
import com.microsoft.azure.iotsolutions.iothubmanager.services.models.DeploymentServiceListModel;
import com.microsoft.azure.iotsolutions.iothubmanager.services.models.DeploymentServiceModel;
import com.microsoft.azure.iotsolutions.iothubmanager.services.models.DeploymentType;
import com.microsoft.azure.sdk.iot.service.Configuration;
import com.microsoft.azure.sdk.iot.service.RegistryManager;
import com.microsoft.azure.sdk.iot.service.devicetwin.DeviceTwin;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import play.libs.Json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.when;

@RunWith(JUnitParamsRunner.class)
public class DeploymentsTest {
    private final Deployments deployments;
    private static final String DEPLOYMENT_NAME_LABEL = "Name";
    private static final String DEPLOYMENT_GROUP_ID_LABEL = "DeviceGroupId";
    private static final String DEPLOYMENT_PACKAGE_ID_LABEL = "PackageId";
    private static final String RM_CREATED_LABEL = "RMDeployment";

    @Mock
    private RegistryManager registry;

    @Mock
    private DeviceTwin deviceTwin;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    public DeploymentsTest() {
        MockitoAnnotations.initMocks(this);
        this.deployments = new Deployments("hubname",
                                           this.deviceTwin,
                                           this.registry);
    }

    @Test
    @Parameters({"depname, dvcgroupid, dvcGroupQuery, true, 10, false",
                 ", dvcgroupid, dvcGroupQuery, true, 10, true",
                 "depname, , dvcGroupQuery, true, 10, true",
                 "depname, dvcgroupid, , true, 10, true",
                 "depname, dvcgroupid, dvcGroupQuery, false, 10, true",
                 "depname, dvcgroupid, dvcGroupQuery, true, -1, true"})
    public void createDeploymentTest(String deploymentName, String deviceGroupId,
                                    String dvcGroupQuery, boolean addPackageContent, int priority,
                                    boolean exceptionExpected) throws Exception {
        // Provides a different value to ensure that the configuration object returned
        // from creating the deployment is different than the one provided to the registry manager
        final String registryManagerDeploymentId = "test-config";

        final Configuration config = new Configuration(registryManagerDeploymentId);
        config.setLabels(new HashMap<>());
        config.getLabels().put(DEPLOYMENT_NAME_LABEL, deploymentName);
        config.getLabels().put(DEPLOYMENT_GROUP_ID_LABEL, deviceGroupId);
        config.getLabels().put(RM_CREATED_LABEL, "true");
        config.setPriority(priority);
        final String packageContent = addPackageContent ? Json.toJson(config).toString() : StringUtils.EMPTY;

        final DeploymentServiceModel model = new DeploymentServiceModel(deploymentName,
                                                                        deviceGroupId,
                                                                        dvcGroupQuery,
                                                                        packageContent,
                                                                        priority,
                                                                        DeploymentType.edgeManifest);

        final IsValidConfiguration isValidConfig = new IsValidConfiguration(deploymentName, deviceGroupId);
        when(this.registry.addConfiguration(argThat(isValidConfig))).thenReturn(config);

        if (exceptionExpected) {
            exception.expect(InvalidInputException.class);
            this.deployments.createAsync(model).toCompletableFuture().get();
        } else {
            DeploymentServiceModel createdDeployment = this.deployments.createAsync(model).toCompletableFuture().get();
            assertEquals(registryManagerDeploymentId, createdDeployment.getId());
            assertEquals(deploymentName, createdDeployment.getName());
            assertEquals(deviceGroupId, createdDeployment.getDeviceGroupId());
            assertEquals(priority, createdDeployment.getPriority());
        }
    }

    @Test
    @Parameters({"0","1","5"})
    public void getDeploymentsTest(int numDeployments) throws Exception {
        List<Configuration> configurations = new ArrayList<>();
        for (int i = numDeployments - 1; i >= 0; i--) {
            configurations.add(this.createConfiguration(i, true));
        }

        when(this.registry.getConfigurations(20)).thenReturn(configurations);
        DeploymentServiceListModel returnedDeployments = this.deployments.listAsync().toCompletableFuture()
                .get();
        assertEquals(numDeployments, returnedDeployments.getItems().size());

        // verify deployments are ordered by name
        for (int i = 0; i < numDeployments; i++)
        {
            assertEquals("deployment" + i, returnedDeployments.getItems().get(i).getName());
        }
    }

    @Test
    public void filterOutNonRmDeploymentsTest() throws Exception {

        final List<Configuration> configurations = new ArrayList<>();
        configurations.add(this.createConfiguration(0, true));
        configurations.add(this.createConfiguration(1, false));

        when(this.registry.getConfigurations(20)).thenReturn(configurations);
        DeploymentServiceListModel returnedDeployments = this.deployments.listAsync().toCompletableFuture()
                .get();

        assertEquals(1, returnedDeployments.getItems().size());
        assertEquals("deployment0", returnedDeployments.getItems().get(0).getName());
    }

    private Configuration createConfiguration(int idx, boolean addCreatedByRmLabel)
    {
        final Configuration conf = new Configuration("test-config"+idx);
        final HashMap<String, String> labels = new HashMap<String, String>() {
            {
                put(DEPLOYMENT_NAME_LABEL, "deployment" + idx);
                put(DEPLOYMENT_GROUP_ID_LABEL, "dvcGroupId" + idx);
                put(DEPLOYMENT_PACKAGE_ID_LABEL, "packageId" + idx);
            }
        };
        conf.setLabels(labels);
        conf.setPriority(10);

        if (addCreatedByRmLabel) {
            labels.put(RM_CREATED_LABEL, "true");
        }

        return conf;
    }

    class IsValidConfiguration implements ArgumentMatcher<Configuration> {

        private final String deploymentName;
        private final String deviceGroupId;

        IsValidConfiguration(final String deploymentName,
                                    final String deviceGroupId) {
            this.deploymentName = deploymentName;
            this.deviceGroupId = deviceGroupId;
        }

        @Override
        public boolean matches(Configuration config) {
            final Map<String, String> labels = config.getLabels();

            return labels.getOrDefault( DEPLOYMENT_NAME_LABEL,"").equals(deploymentName) &&
                    labels.getOrDefault( DEPLOYMENT_GROUP_ID_LABEL,"").equals(deviceGroupId) &&
                    labels.getOrDefault( RM_CREATED_LABEL,"").equals("true");
        }
    }
}