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
package io.apiman.manager.api.core;

import io.apiman.manager.api.beans.apps.ApplicationVersionBean;
import io.apiman.manager.api.beans.plans.PlanVersionBean;
import io.apiman.manager.api.beans.policies.PolicyBean;
import io.apiman.manager.api.beans.policies.PolicyType;
import io.apiman.manager.api.beans.services.ServiceVersionBean;
import io.apiman.manager.api.beans.summary.ApplicationSummaryBean;
import io.apiman.manager.api.beans.summary.ContractSummaryBean;
import io.apiman.manager.api.beans.summary.OrganizationSummaryBean;
import io.apiman.manager.api.beans.summary.PlanSummaryBean;
import io.apiman.manager.api.beans.summary.ServicePlanSummaryBean;
import io.apiman.manager.api.beans.summary.ServiceSummaryBean;
import io.apiman.manager.api.core.exceptions.StorageException;

import java.util.List;
import java.util.Set;


/**
 * Specific querying of the storage layer.
 *
 * @author eric.wittmann@redhat.com
 */
public interface IStorageQuery {
    
    /**
     * Returns summary info for all organizations in the given set.
     * @param orgIds
     * @throws StorageException
     */
    public List<OrganizationSummaryBean> getOrgs(Set<String> orgIds) throws StorageException;

    /**
     * Returns summary info for all applications in all organizations in the given set.
     * @param orgIds
     * @throws StorageException
     */
    public List<ApplicationSummaryBean> getApplicationsInOrgs(Set<String> orgIds) throws StorageException;

    /**
     * Returns summary info for all applications in the given organization.
     * @param orgId
     * @throws StorageException
     */
    public List<ApplicationSummaryBean> getApplicationsInOrg(String orgId) throws StorageException;

    /**
     * Returns the application version bean with the given info (orgId, appId, version).
     * @param organizationId
     * @param applicationId
     * @param version
     */
    public ApplicationVersionBean getApplicationVersion(String organizationId, String applicationId,
            String version) throws StorageException;

    /**
     * Returns all application versions for a given app.
     * @param organizationId
     * @param applicationId
     */
    public List<ApplicationVersionBean> getApplicationVersions(String organizationId, String applicationId)
            throws StorageException;

    /**
     * Returns all Contracts for the application.
     * @param organizationId
     * @param applicationId
     * @param version
     */
    public List<ContractSummaryBean> getApplicationContracts(String organizationId, String applicationId, String version)
            throws StorageException;

    /**
     * Returns summary info for all services in all organizations in the given set.
     * @param orgIds
     * @throws StorageException
     */
    public List<ServiceSummaryBean> getServicesInOrgs(Set<String> orgIds) throws StorageException;

    /**
     * Returns summary info for all services in the given organization.
     * @param orgId
     * @throws StorageException
     */
    public List<ServiceSummaryBean> getServicesInOrg(String orgId) throws StorageException;
    
    /**
     * Returns the service version bean with the given info (orgid, serviceid, version).
     * @param orgId
     * @param serviceId
     * @param version
     * @throws StorageException
     */
    public ServiceVersionBean getServiceVersion(String orgId, String serviceId, String version) throws StorageException;
    
    /**
     * Returns all service versions for a given service.
     * @param orgId
     * @param serviceId
     * @throws StorageException
     */
    public List<ServiceVersionBean> getServiceVersions(String orgId, String serviceId) throws StorageException;

    /**
     * Returns the service plans configured for the given service version.
     * @param organizationId
     * @param serviceId
     * @param version
     */
    public List<ServicePlanSummaryBean> getServiceVersionPlans(String organizationId, String serviceId,
            String version) throws StorageException;

    /**
     * Returns summary info for all plans in all organizations in the given set.
     * @param orgIds
     * @throws StorageException
     */
    public List<PlanSummaryBean> getPlansInOrgs(Set<String> orgIds) throws StorageException;

    /**
     * Returns summary info for all plans in the given organization.
     * @param orgId
     * @throws StorageException
     */
    public List<PlanSummaryBean> getPlansInOrg(String orgId) throws StorageException;

    /**
     * Returns the plan version bean with the given info (orgId, appId, version).
     * @param organizationId
     * @param planId
     * @param version
     * @throws StorageException
     */
    public PlanVersionBean getPlanVersion(String organizationId, String planId, String version)
            throws StorageException;

    /**
     * Returns all plan versions for a given plan.
     * @param organizationId
     * @param planId
     * @throws StorageException
     */
    public List<PlanVersionBean> getPlanVersions(String organizationId, String planId)
            throws StorageException;

    /**
     * Returns all policies of the given type for the given entity/version.  This could be
     * any of Application, Plan, Service.
     * @param organizationId
     * @param entityId
     * @param version
     * @param type
     */
    public List<PolicyBean> getPolicies(String organizationId, String entityId, String version,
            PolicyType type) throws StorageException;

    /**
     * Gets a list of contracts for the given service.  This is paged.
     * @param organizationId
     * @param serviceId
     * @param version
     * @param page
     * @param pageSize
     */
    public List<ContractSummaryBean> getServiceContracts(String organizationId,
            String serviceId, String version, int page, int pageSize) throws StorageException;
    
}
