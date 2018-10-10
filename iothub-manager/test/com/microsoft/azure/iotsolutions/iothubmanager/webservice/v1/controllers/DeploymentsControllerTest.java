// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iothubmanager.webservice.v1.controllers;

import com.microsoft.azure.iotsolutions.iothubmanager.services.IDeployments;
import com.microsoft.azure.iotsolutions.iothubmanager.services.exceptions.InvalidInputException;
import com.microsoft.azure.iotsolutions.iothubmanager.services.helpers.TestUtils;
import com.microsoft.azure.iotsolutions.iothubmanager.services.models.DeploymentServiceListModel;
import com.microsoft.azure.iotsolutions.iothubmanager.services.models.DeploymentServiceModel;
import com.microsoft.azure.iotsolutions.iothubmanager.services.models.DeploymentType;
import com.microsoft.azure.iotsolutions.iothubmanager.webservice.v1.models.DeploymentApiModel;
import com.microsoft.azure.iotsolutions.iothubmanager.webservice.v1.models.DeploymentListApiModel;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import play.mvc.Result;

import java.util.ArrayList;
import java.util.List;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.when;

@RunWith(JUnitParamsRunner.class)
public class DeploymentsControllerTest {
    private static final String DEPLOYMENT_NAME = "depname";
    private static final String DEVICE_GROUP_ID = "dvcGroupId";
    private static final String DEVICE_GROUP_QUERY = "dvcGroupQuery";
    private static final String PACKAGE_CONTENT = "packageContent";
    private static final String DEPLOYMENT_ID = "dvcGroupId-packageId";
    private static final int PRIORITY = 10;

    private final DeploymentsController deploymentsController;

    @Mock
    private IDeployments deployments;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    public DeploymentsControllerTest() {
        MockitoAnnotations.initMocks(this);
        this.deploymentsController = new DeploymentsController(this.deployments);
    }

    @Test
    public void getDeploymentTest() throws Exception {
        // Arrange
        DeploymentServiceModel deploymentModel = new DeploymentServiceModel(DEPLOYMENT_NAME,
                                                                            DEVICE_GROUP_ID,
                                                                            DEVICE_GROUP_QUERY,
                                                                            PACKAGE_CONTENT,
                                                                            PRIORITY,
                                                                            DeploymentType.edgeManifest);

        when(this.deployments.getAsync(DEPLOYMENT_ID, false)).thenReturn(completedFuture(deploymentModel));

        // Act
        final Result response = this.deploymentsController.getDeployment(DEPLOYMENT_ID, false)
                .toCompletableFuture().get();
        final DeploymentApiModel deployment = TestUtils.getResult(response, DeploymentApiModel.class);

        // Assert
        assertEquals(DEPLOYMENT_NAME, deployment.getName());
        assertEquals(DEVICE_GROUP_ID, deployment.getDeviceGroupId());
        assertEquals(PRIORITY, deployment.getPriority());
        assertEquals(DeploymentType.edgeManifest, deployment.getType());
    }

    @Test
    @Parameters({"0","1","5"})
    public void getDeploymentsTest(int numDeployments) throws Exception {
        List<DeploymentServiceModel> deploymentsList = new ArrayList<>();
        for(int i = 0; i < numDeployments; i++) {
            DeploymentServiceModel dep = new DeploymentServiceModel(DEPLOYMENT_NAME + i,
                                                                    DEVICE_GROUP_ID + i,
                                                                    DEVICE_GROUP_QUERY + i,
                                                                    PACKAGE_CONTENT + i,
                                                                    PRIORITY + i,
                                                                    DeploymentType.edgeManifest);
            deploymentsList.add(dep);
        }

        when(this.deployments.listAsync()).thenReturn(completedFuture(new DeploymentServiceListModel
                (deploymentsList)));

        // Act
        final Result response = this.deploymentsController.getDeploymentsAsync().toCompletableFuture().get();
        final DeploymentListApiModel deployments = TestUtils.getResult(response, DeploymentListApiModel
                .class);

        // Assert
        assertEquals(numDeployments, deployments.getItems().size());
        for (int i = 0; i < numDeployments; i++) {
            final DeploymentApiModel deployment = deployments.getItems().get(i);
            assertEquals(DEPLOYMENT_NAME + i, deployment.getName());
            assertEquals(DEVICE_GROUP_ID + i, deployment.getDeviceGroupId());
            assertEquals(PRIORITY + i, deployment.getPriority());
        }
    }

    @Test
    @Parameters({
        "depName, dvcGroupId, pkgId, 10, false",
        ", dvcGroupId, pkgId, 10, true",
        "depName, , pkgId, 10, true",
        "depName, dvcGroupId, , 10, true",
        "depName, dvcGroupId, pkgId, -5, true"
    })
    public void postDeploymentTest(String deploymentName, String deviceGroupId, String packageContent, int
            priority, boolean exceptionExpected) throws Exception {
        // Arrange
        String deviceGroupQuery = "[]";
        DeploymentMatcher matchesDeployment = new DeploymentMatcher(deploymentName, deviceGroupId,
                deviceGroupQuery, packageContent, priority);

        final DeploymentServiceModel deploymentMode = new DeploymentServiceModel(deploymentName,
                deviceGroupId, packageContent,
                deploymentName, priority, DeploymentType.edgeManifest);
        when(this.deployments.createAsync(argThat(matchesDeployment))).thenReturn(completedFuture
                (deploymentMode));

        final DeploymentApiModel depApiModel = new DeploymentApiModel(deploymentName, deviceGroupId,
                deviceGroupQuery, packageContent, priority, DeploymentType.edgeManifest);

        // Act
        TestUtils.setRequest(depApiModel);
        if (exceptionExpected) {
            exception.expect(InvalidInputException.class);
            this.deploymentsController.postAsync().toCompletableFuture().get();
        } else {
            final Result response = this.deploymentsController.postAsync().toCompletableFuture().get();
            final DeploymentApiModel dep = TestUtils.getResult(response, DeploymentApiModel.class);

            // Assert
            assertEquals(deploymentName, dep.getName());
            assertEquals(deviceGroupId, dep.getDeviceGroupId());
            assertEquals(priority, dep.getPriority());
            assertEquals(DeploymentType.edgeManifest, dep.getType());
        }
    }

    class DeploymentMatcher implements ArgumentMatcher<DeploymentServiceModel> {
        private final String deploymentName;
        private final String deviceGroupId;
        private final String deviceGroupQuery;
        private final String packageContent;
        private final int priority;

        DeploymentMatcher(final String deploymentName, final String deviceGroupId,
                          final String deviceGroupQuery, final String packageContent, final int priority) {
            this.deploymentName = deploymentName;
            this.deviceGroupId = deviceGroupId;
            this.deviceGroupQuery = deviceGroupQuery;
            this.packageContent = packageContent;
            this.priority = priority;
        }

        @Override
        public boolean matches(DeploymentServiceModel config) {
            return this.deploymentName.equals(config.getName()) &&
                    this.deviceGroupId.equals(config.getDeviceGroupId()) &&
                    this.deviceGroupQuery.equals(config.getDeviceGroupQuery()) &&
                    this.packageContent.equals(config.getPackageContent()) &&
                    this.priority == config.getPriority();
        }
    }
}