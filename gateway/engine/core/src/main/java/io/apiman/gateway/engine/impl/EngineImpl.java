/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apiman.gateway.engine.impl;

import io.apiman.gateway.engine.IComponentRegistry;
import io.apiman.gateway.engine.IConnectorFactory;
import io.apiman.gateway.engine.IEngine;
import io.apiman.gateway.engine.IEngineResult;
import io.apiman.gateway.engine.IRegistry;
import io.apiman.gateway.engine.IServiceRequestExecutor;
import io.apiman.gateway.engine.Version;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.Application;
import io.apiman.gateway.engine.beans.Contract;
import io.apiman.gateway.engine.beans.Policy;
import io.apiman.gateway.engine.beans.Service;
import io.apiman.gateway.engine.beans.ServiceContract;
import io.apiman.gateway.engine.beans.ServiceRequest;
import io.apiman.gateway.engine.beans.exceptions.ConfigurationParseException;
import io.apiman.gateway.engine.beans.exceptions.InvalidContractException;
import io.apiman.gateway.engine.beans.exceptions.InvalidServiceException;
import io.apiman.gateway.engine.beans.exceptions.PolicyNotFoundException;
import io.apiman.gateway.engine.beans.exceptions.PublishingException;
import io.apiman.gateway.engine.beans.exceptions.RegistrationException;
import io.apiman.gateway.engine.i18n.Messages;
import io.apiman.gateway.engine.policy.IPolicy;
import io.apiman.gateway.engine.policy.IPolicyFactory;
import io.apiman.gateway.engine.policy.PolicyContextImpl;
import io.apiman.gateway.engine.policy.PolicyWithConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The implementation of the API Management runtime engine.
 *
 * @author eric.wittmann@redhat.com
 */
public class EngineImpl implements IEngine {

    private IRegistry registry;
    private IComponentRegistry componentRegistry;
    private IConnectorFactory connectorFactory;
    private IPolicyFactory policyFactory;
    private Map<Policy, Object> policyConfigCache = new HashMap<Policy, Object>();

    /**
     * Constructor.
     * @param registry
     * @param componentRegistry
     * @param connectorFactory
     * @param policyFactory
     */
    public EngineImpl(final IRegistry registry, final IComponentRegistry componentRegistry,
            final IConnectorFactory connectorFactory, final IPolicyFactory policyFactory) {
        setRegistry(registry);
        setComponentRegistry(componentRegistry);
        setConnectorFactory(connectorFactory);
        setPolicyFactory(policyFactory);
    }

    /**
     * @see io.apiman.gateway.engine.IEngine#getVersion()
     */
    @Override
    public String getVersion() {
        return Version.get().getVersionString();
    }

    /**
     * @see io.apiman.gateway.engine.IEngine#request()
     */
    @Override
    public IServiceRequestExecutor executor(ServiceRequest request, final IAsyncResultHandler<IEngineResult> resultHandler) {
        Service service = null;
        List<PolicyWithConfiguration> policies = null;
        
        // If no API Key provided - the service must be public.  If an API Key *is* provided
        // then we lookup the Contract and use that.
        if (request.getApiKey() == null) {
            service = getService(request.getServiceOrgId(), request.getServiceId(), request.getServiceVersion());
            if (service == null) {
                throw new InvalidServiceException(Messages.i18n.format("EngineImpl.ServiceNotFound")); //$NON-NLS-1$
            }
            if (!service.isPublicService()) {
                throw new InvalidServiceException(Messages.i18n.format("EngineImpl.ServiceNotPublic")); //$NON-NLS-1$
            }
            policies = getPolicies(service);
        } else {
            ServiceContract serviceContract = getContract(request);
            service = serviceContract.getService();
            request.setContract(serviceContract);
            policies = getPolicies(serviceContract);
            if (request.getServiceOrgId() != null) {
                validateRequest(request);
            }
        }
        
        return new ServiceRequestExecutorImpl(request, 
                resultHandler,
                service,
                new PolicyContextImpl(getComponentRegistry()),
                policies,
                getConnectorFactory());
    }

    /**
     * Gets a published service by its service coordinates.
     * @param serviceOrgId
     * @param serviceId
     * @param serviceVersion
     */
    protected Service getService(String serviceOrgId, String serviceId, String serviceVersion) {
        return getRegistry().getService(serviceOrgId, serviceId, serviceVersion);
    }

    /**
     * Validates that the contract being used for the request is valid against the
     * service information included in the request.  Basically the request includes
     * information indicating which specific service is being invoked.  This method
     * ensures that the service information in the contract matches the requested
     * service.
     * @param request
     */
    protected void validateRequest(ServiceRequest request) throws InvalidContractException {
        ServiceContract contract = request.getContract();
        
        boolean matches = true;
        if (!contract.getService().getOrganizationId().equals(request.getServiceOrgId())) {
            matches = false;
        }
        if (!contract.getService().getServiceId().equals(request.getServiceId())) {
            matches = false;
        }
        if (!contract.getService().getVersion().equals(request.getServiceVersion())) {
            matches = false;
        }
        if (!matches) {
            throw new InvalidContractException(Messages.i18n.format("EngineImpl.InvalidContractForService", //$NON-NLS-1$
                    request.getServiceOrgId(), request.getServiceId(), request.getServiceVersion()));
        }
    }

    /**
     * @see io.apiman.gateway.engine.IEngine#publishService(io.apiman.gateway.engine.beans.Service)
     */
    @Override
    public void publishService(final Service service) throws PublishingException {
        getRegistry().publishService(service);
    }

    /**
     * @see io.apiman.gateway.engine.IEngine#retireService(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void retireService(String organizationId, String serviceId, String version)
            throws PublishingException {
        Service svc = new Service();
        svc.setOrganizationId(organizationId);
        svc.setServiceId(serviceId);
        svc.setVersion(version);
        getRegistry().retireService(svc);
    }

    /**
     * @see io.apiman.gateway.engine.IEngine#registerApplication(io.apiman.gateway.engine.beans.Application)
     */
    @Override
    public void registerApplication(final Application application) throws RegistrationException {
        Set<Contract> contracts = application.getContracts();
        for (Contract contract : contracts) {
            List<Policy> policies = contract.getPolicies();
            for (Policy policy : policies) {
                try {
                    // Load the policy class and validate the policy config.
                    IPolicy policyImpl = getPolicyFactory().newPolicy(policy.getPolicyImpl());
                    policyImpl.parseConfiguration(policy.getPolicyJsonConfig());
                } catch (PolicyNotFoundException e) {
                    throw new RegistrationException(e);
                } catch (ConfigurationParseException e) {
                    throw new RegistrationException(e);
                }
            }
        }
        getRegistry().registerApplication(application);
    }

    /**
     * @see io.apiman.gateway.engine.IEngine#unregisterApplication(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void unregisterApplication(String organizationId, String applicationId, String version)
            throws RegistrationException {
        Application app = new Application();
        app.setOrganizationId(organizationId);
        app.setApplicationId(applicationId);
        app.setVersion(version);
        getRegistry().unregisterApplication(app);
    }
    
    /**
     * Gets the service contract to use for the given request.
     * @param request
     */
    private ServiceContract getContract(ServiceRequest request) {
        return getRegistry().getContract(request);
    }

    /**
     * Creates the policies that should be applied for this service invocation.
     * This is achieved by using the policy information set on the contract.
     * @param contract
     */
    private List<PolicyWithConfiguration> getPolicies(ServiceContract contract) {
        return getPolicies(contract.getPolicies());
    }

    /**
     * Creates the policies that should be applied for this service invocation.
     * @param service
     */
    private List<PolicyWithConfiguration> getPolicies(Service service) {
        return getPolicies(service.getServicePolicies());
    }

    /**
     * Get/resolve the list of policies into a list of policies with config.
     * @param policies
     */
    private List<PolicyWithConfiguration> getPolicies(List<Policy> policies) {
        List<PolicyWithConfiguration> instances = new ArrayList<PolicyWithConfiguration>();
        for (Policy policy : policies) {
            IPolicy policyImpl = getPolicyFactory().newPolicy(policy.getPolicyImpl());
            Object policyConfig = getPolicyConfig(policyImpl, policy);
            PolicyWithConfiguration pwc = new PolicyWithConfiguration(policyImpl, policyConfig);
            instances.add(pwc);
        }
        return instances;
    }

    /**
     * Gets the policy config object for the given policy.
     * @param policyImpl
     * @param policy
     */
    protected Object getPolicyConfig(IPolicy policyImpl, Policy policy) {
        Object parsedConfig = policyConfigCache.get(policy);
        if (parsedConfig == null) {
            parsedConfig = policyImpl.parseConfiguration(policy.getPolicyJsonConfig());
            policyConfigCache.put(policy, parsedConfig);
        }
        return parsedConfig;
    }

    /**
     * @return the registry
     */
    public IRegistry getRegistry() {
        return registry;
    }

    /**
     * @param registry the registry to set
     */
    public void setRegistry(final IRegistry registry) {
        this.registry = registry;
    }

    /**
     * @return the connectorFactory
     */
    public IConnectorFactory getConnectorFactory() {
        return connectorFactory;
    }

    /**
     * @param connectorFactory the connectorFactory to set
     */
    public void setConnectorFactory(final IConnectorFactory connectorFactory) {
        this.connectorFactory = connectorFactory;
    }

    /**
     * @return the policyFactory
     */
    public IPolicyFactory getPolicyFactory() {
        return policyFactory;
    }

    /**
     * @param policyFactory the policyFactory to set
     */
    public void setPolicyFactory(IPolicyFactory policyFactory) {
        this.policyFactory = policyFactory;
    }

    /**
     * @return the componentRegistry
     */
    public IComponentRegistry getComponentRegistry() {
        return componentRegistry;
    }

    /**
     * @param componentRegistry the componentRegistry to set
     */
    public void setComponentRegistry(IComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

}
