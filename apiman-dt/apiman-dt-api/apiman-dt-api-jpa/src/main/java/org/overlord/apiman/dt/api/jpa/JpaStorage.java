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
package org.overlord.apiman.dt.api.jpa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.overlord.apiman.dt.api.beans.apps.ApplicationBean;
import org.overlord.apiman.dt.api.beans.apps.ApplicationVersionBean;
import org.overlord.apiman.dt.api.beans.audit.AuditEntityType;
import org.overlord.apiman.dt.api.beans.audit.AuditEntryBean;
import org.overlord.apiman.dt.api.beans.contracts.ContractBean;
import org.overlord.apiman.dt.api.beans.orgs.OrganizationBean;
import org.overlord.apiman.dt.api.beans.plans.PlanBean;
import org.overlord.apiman.dt.api.beans.plans.PlanVersionBean;
import org.overlord.apiman.dt.api.beans.policies.PolicyBean;
import org.overlord.apiman.dt.api.beans.policies.PolicyType;
import org.overlord.apiman.dt.api.beans.search.PagingBean;
import org.overlord.apiman.dt.api.beans.search.SearchCriteriaBean;
import org.overlord.apiman.dt.api.beans.search.SearchCriteriaFilterBean;
import org.overlord.apiman.dt.api.beans.search.SearchResultsBean;
import org.overlord.apiman.dt.api.beans.services.ServiceBean;
import org.overlord.apiman.dt.api.beans.services.ServicePlanBean;
import org.overlord.apiman.dt.api.beans.services.ServiceVersionBean;
import org.overlord.apiman.dt.api.beans.summary.ApplicationSummaryBean;
import org.overlord.apiman.dt.api.beans.summary.ContractSummaryBean;
import org.overlord.apiman.dt.api.beans.summary.OrganizationSummaryBean;
import org.overlord.apiman.dt.api.beans.summary.PlanSummaryBean;
import org.overlord.apiman.dt.api.beans.summary.ServicePlanSummaryBean;
import org.overlord.apiman.dt.api.beans.summary.ServiceSummaryBean;
import org.overlord.apiman.dt.api.core.IApiKeyGenerator;
import org.overlord.apiman.dt.api.core.IStorage;
import org.overlord.apiman.dt.api.core.IStorageQuery;
import org.overlord.apiman.dt.api.core.exceptions.AlreadyExistsException;
import org.overlord.apiman.dt.api.core.exceptions.DoesNotExistException;
import org.overlord.apiman.dt.api.core.exceptions.StorageException;
import org.overlord.apiman.dt.api.core.util.PolicyTemplateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JPA implementation of the storage interface.
 *
 * @author eric.wittmann@redhat.com
 */
@ApplicationScoped
public class JpaStorage extends AbstractJpaStorage implements IStorage, IStorageQuery, IApiKeyGenerator {

    private static Logger logger = LoggerFactory.getLogger(JpaStorage.class);

    /**
     * Constructor.
     */
    public JpaStorage() {
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IStorage#beginTx()
     */
    @Override
    public void beginTx() throws StorageException {
        super.beginTx();
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IStorage#commitTx()
     */
    @Override
    public void commitTx() throws StorageException {
        super.commitTx();
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IStorage#rollbackTx()
     */
    @Override
    public void rollbackTx() {
        super.rollbackTx();
    }

    /**
     * @see org.overlord.apiman.dt.api.core.IStorage#create(java.lang.Object)
     */
    @Override
    public <T> void create(T bean) throws StorageException, AlreadyExistsException {
        super.create(bean);
    }

    /**
     * @see org.overlord.apiman.dt.api.core.IStorage#update(java.lang.Object)
     */
    @Override
    public <T> void update(T bean) throws StorageException, DoesNotExistException {
        super.update(bean);
    }

    /**
     * @see org.overlord.apiman.dt.api.core.IStorage#delete(java.lang.Object)
     */
    @Override
    public <T> void delete(T bean) throws StorageException, DoesNotExistException {
        super.delete(bean);
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IStorage#get(java.lang.Long, java.lang.Class)
     */
    @Override
    public <T> T get(Long id, Class<T> type) throws StorageException, DoesNotExistException {
        return super.get(id, type);
    }

    /**
     * @see org.overlord.apiman.dt.api.core.IStorage#get(java.lang.String, java.lang.Class)
     */
    @Override
    public <T> T get(String id, Class<T> type) throws StorageException, DoesNotExistException {
        return super.get(id, type);
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IStorage#get(java.lang.String, java.lang.String, java.lang.Class)
     */
    @Override
    public <T> T get(String organizationId, String id, Class<T> type) throws StorageException,
            DoesNotExistException {
        return super.get(organizationId, id, type);
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IStorage#find(org.overlord.apiman.dt.api.beans.search.SearchCriteriaBean, java.lang.Class)
     */
    @Override
    public <T> SearchResultsBean<T> find(SearchCriteriaBean criteria, Class<T> type) throws StorageException {
        return super.find(criteria, type);
    }

    /**
     * @see org.overlord.apiman.dt.api.core.IStorage#createAuditEntry(org.overlord.apiman.dt.api.beans.audit.AuditEntryBean)
     */
    @Override
    public void createAuditEntry(AuditEntryBean entry) throws StorageException {
        super.create(entry);
    }

    /**
     * @see org.overlord.apiman.dt.api.core.IStorage#auditEntity(java.lang.String, java.lang.String, java.lang.String, java.lang.Class, org.overlord.apiman.dt.api.beans.search.PagingBean)
     */
    @Override
    public <T> SearchResultsBean<AuditEntryBean> auditEntity(String organizationId, String entityId, String entityVersion,
            Class<T> type, PagingBean paging) throws StorageException {
        SearchCriteriaBean criteria = new SearchCriteriaBean();
        if (paging != null) {
            criteria.setPaging(paging);
        } else {
            criteria.setPage(1);
            criteria.setPageSize(20);
        }
        criteria.setOrder("when", false); //$NON-NLS-1$
        if (organizationId != null) {
            criteria.addFilter("organizationId", organizationId, SearchCriteriaFilterBean.OPERATOR_EQ); //$NON-NLS-1$
        }
        if (entityId != null) {
            criteria.addFilter("entityId", entityId, SearchCriteriaFilterBean.OPERATOR_EQ); //$NON-NLS-1$
        }
        if (entityVersion != null) {
            criteria.addFilter("entityVersion", entityVersion, SearchCriteriaFilterBean.OPERATOR_EQ); //$NON-NLS-1$
        }
        if (type != null) {
            AuditEntityType entityType = null;
            if (type == OrganizationBean.class) {
                entityType = AuditEntityType.Organization;
            } else if (type == ApplicationBean.class) { 
                entityType = AuditEntityType.Application;
            } else if (type == ServiceBean.class) {
                entityType = AuditEntityType.Service;
            } else if (type == PlanBean.class) {
                entityType = AuditEntityType.Plan;
            }
            if (entityType != null) {
                criteria.addFilter("entityType", entityType.name(), SearchCriteriaFilterBean.OPERATOR_EQ); //$NON-NLS-1$
            }
        }
        
        return find(criteria, AuditEntryBean.class);
    }

    /**
     * @see org.overlord.apiman.dt.api.core.IStorage#auditUser(java.lang.String, java.lang.Class, org.overlord.apiman.dt.api.beans.search.PagingBean)
     */
    @Override
    public <T> SearchResultsBean<AuditEntryBean> auditUser(String userId, PagingBean paging)
            throws StorageException {
        SearchCriteriaBean criteria = new SearchCriteriaBean();
        if (paging != null) {
            criteria.setPaging(paging);
        } else {
            criteria.setPage(1);
            criteria.setPageSize(20);
        }
        criteria.setOrder("when", false); //$NON-NLS-1$
        if (userId != null) {
            criteria.addFilter("who", userId, SearchCriteriaFilterBean.OPERATOR_EQ); //$NON-NLS-1$
        }
        
        return find(criteria, AuditEntryBean.class);
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IStorageQuery#getOrgs(java.util.Set)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<OrganizationSummaryBean> getOrgs(Set<String> orgIds) throws StorageException {
        List<OrganizationSummaryBean> orgs = new ArrayList<OrganizationSummaryBean>();
        beginTx();
        try {
            EntityManager entityManager = getActiveEntityManager();
            String jpql = "SELECT o from OrganizationBean o WHERE o.id IN :orgs ORDER BY o.id ASC"; //$NON-NLS-1$
            Query query = entityManager.createQuery(jpql);
            query.setParameter("orgs", orgIds); //$NON-NLS-1$
            List<OrganizationBean> qr = (List<OrganizationBean>) query.getResultList();
            for (OrganizationBean bean : qr) {
                OrganizationSummaryBean summary = new OrganizationSummaryBean();
                summary.setId(bean.getId());
                summary.setName(bean.getName());
                summary.setDescription(bean.getDescription());
                orgs.add(summary);
            }
            return orgs;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new StorageException(t);
        } finally {
            commitTx();
        }
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IStorageQuery#getApplicationsInOrg(java.lang.String)
     */
    @Override
    public List<ApplicationSummaryBean> getApplicationsInOrg(String orgId) throws StorageException {
        Set<String> orgIds = new HashSet<String>();
        orgIds.add(orgId);
        return getApplicationsInOrgs(orgIds);
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IStorageQuery#getApplicationsInOrgs(java.util.Set)
     */
    @Override
    public List<ApplicationSummaryBean> getApplicationsInOrgs(Set<String> orgIds) throws StorageException {
        List<ApplicationSummaryBean> rval = new ArrayList<ApplicationSummaryBean>();
        beginTx();
        try {
            EntityManager entityManager = getActiveEntityManager();
            String jpql = "SELECT a from ApplicationBean a WHERE a.organizationId IN :orgs ORDER BY a.id ASC"; //$NON-NLS-1$
            Query query = entityManager.createQuery(jpql);
            query.setParameter("orgs", orgIds); //$NON-NLS-1$
            @SuppressWarnings("unchecked")
            List<ApplicationBean> qr = (List<ApplicationBean>) query.getResultList();
            for (ApplicationBean bean : qr) {
                ApplicationSummaryBean summary = new ApplicationSummaryBean();
                summary.setId(bean.getId());
                summary.setName(bean.getName());
                summary.setDescription(bean.getDescription());
                // TODO find the number of contracts
                summary.setNumContracts(0);
                OrganizationBean org = entityManager.find(OrganizationBean.class, bean.getOrganizationId());
                summary.setOrganizationId(org.getId());
                summary.setOrganizationName(org.getName());
                rval.add(summary);
            }
            return rval;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new StorageException(t);
        } finally {
            commitTx();
        }
    }

    /**
     * @see org.overlord.apiman.dt.api.core.IStorageQuery#getServicesInOrg(java.lang.String)
     */
    @Override
    public List<ServiceSummaryBean> getServicesInOrg(String orgId) throws StorageException {
        Set<String> orgIds = new HashSet<String>();
        orgIds.add(orgId);
        return getServicesInOrgs(orgIds);
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IStorageQuery#getServicesInOrgs(java.util.Set)
     */
    @Override
    public List<ServiceSummaryBean> getServicesInOrgs(Set<String> orgIds) throws StorageException {
        List<ServiceSummaryBean> rval = new ArrayList<ServiceSummaryBean>();
        beginTx();
        try {
            EntityManager entityManager = getActiveEntityManager();
            String jpql = "SELECT s from ServiceBean s WHERE s.organizationId IN :orgs ORDER BY s.id ASC"; //$NON-NLS-1$
            Query query = entityManager.createQuery(jpql);
            query.setParameter("orgs", orgIds); //$NON-NLS-1$
            @SuppressWarnings("unchecked")
            List<ServiceBean> qr = (List<ServiceBean>) query.getResultList();
            for (ServiceBean bean : qr) {
                ServiceSummaryBean summary = new ServiceSummaryBean();
                summary.setId(bean.getId());
                summary.setName(bean.getName());
                summary.setDescription(bean.getDescription());
                summary.setCreatedOn(bean.getCreatedOn());
                OrganizationBean org = entityManager.find(OrganizationBean.class, bean.getOrganizationId());
                summary.setOrganizationId(org.getId());
                summary.setOrganizationName(org.getName());
                rval.add(summary);
            }
            return rval;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new StorageException(t);
        } finally {
            commitTx();
        }
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IStorageQuery#getServiceVersion(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ServiceVersionBean getServiceVersion(String orgId, String serviceId, String version)
            throws StorageException {
        beginTx();
        try {
            EntityManager entityManager = getActiveEntityManager();
            String jpql = "SELECT v from ServiceVersionBean v JOIN v.service s WHERE s.organizationId = :orgId AND s.id = :serviceId AND v.version = :version"; //$NON-NLS-1$
            Query query = entityManager.createQuery(jpql);
            query.setParameter("orgId", orgId); //$NON-NLS-1$
            query.setParameter("serviceId", serviceId); //$NON-NLS-1$
            query.setParameter("version", version); //$NON-NLS-1$
            
            return (ServiceVersionBean) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new StorageException(t);
        } finally {
            commitTx();
        }
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IStorageQuery#getServiceVersions(java.lang.String, java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<ServiceVersionBean> getServiceVersions(String orgId, String serviceId)
            throws StorageException {
        beginTx();
        try {
            EntityManager entityManager = getActiveEntityManager();
            String jpql = "SELECT v from ServiceVersionBean v JOIN v.service s WHERE s.organizationId = :orgId AND s.id = :serviceId ORDER BY v.id DESC"; //$NON-NLS-1$
            Query query = entityManager.createQuery(jpql);
            query.setParameter("orgId", orgId); //$NON-NLS-1$
            query.setParameter("serviceId", serviceId); //$NON-NLS-1$
            
            return (List<ServiceVersionBean>) query.getResultList();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new StorageException(t);
        } finally {
            commitTx();
        }
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IStorageQuery#getServiceVersionPlans(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public List<ServicePlanSummaryBean> getServiceVersionPlans(String organizationId, String serviceId,
            String version) throws StorageException {
        List<ServicePlanSummaryBean> plans = new ArrayList<ServicePlanSummaryBean>();
        
        ServiceVersionBean versionBean = getServiceVersion(organizationId, serviceId, version);
        Set<ServicePlanBean> servicePlans = versionBean.getPlans();
        for (ServicePlanBean spb : servicePlans) {
            PlanVersionBean planVersion = getPlanVersion(organizationId, spb.getPlanId(), spb.getVersion());
            ServicePlanSummaryBean summary = new ServicePlanSummaryBean();
            summary.setPlanId(planVersion.getPlan().getId());
            summary.setPlanName(planVersion.getPlan().getName());
            summary.setPlanDescription(planVersion.getPlan().getDescription());
            summary.setVersion(spb.getVersion());
            plans.add(summary);
        }
        return plans;
    }

    /**
     * @see org.overlord.apiman.dt.api.core.IStorageQuery#getApplicationVersion(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ApplicationVersionBean getApplicationVersion(String orgId, String applicationId, String version)
            throws StorageException {
        beginTx();
        try {
            EntityManager entityManager = getActiveEntityManager();
            String jpql = "SELECT v from ApplicationVersionBean v JOIN v.application s WHERE s.organizationId = :orgId AND s.id = :applicationId AND v.version = :version"; //$NON-NLS-1$
            Query query = entityManager.createQuery(jpql);
            query.setParameter("orgId", orgId); //$NON-NLS-1$
            query.setParameter("applicationId", applicationId); //$NON-NLS-1$
            query.setParameter("version", version); //$NON-NLS-1$
            
            return (ApplicationVersionBean) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new StorageException(t);
        } finally {
            commitTx();
        }
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IStorageQuery#getApplicationVersions(java.lang.String, java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<ApplicationVersionBean> getApplicationVersions(String orgId, String applicationId)
            throws StorageException {
        beginTx();
        try {
            EntityManager entityManager = getActiveEntityManager();
            String jpql = "SELECT v from ApplicationVersionBean v JOIN v.application s WHERE s.organizationId = :orgId AND s.id = :applicationId ORDER BY v.id DESC"; //$NON-NLS-1$
            Query query = entityManager.createQuery(jpql);
            query.setParameter("orgId", orgId); //$NON-NLS-1$
            query.setParameter("applicationId", applicationId); //$NON-NLS-1$
            
            return (List<ApplicationVersionBean>) query.getResultList();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new StorageException(t);
        } finally {
            commitTx();
        }
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IStorageQuery#getApplicationContracts(java.lang.String, java.lang.String, java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<ContractSummaryBean> getApplicationContracts(String organizationId, String applicationId,
            String version) throws StorageException {
        List<ContractSummaryBean> rval = new ArrayList<ContractSummaryBean>();

        beginTx();
        try {
            EntityManager entityManager = getActiveEntityManager();
            String jpql = 
                    "SELECT c from ContractBean c " +  //$NON-NLS-1$
                    "  JOIN c.application appv " +  //$NON-NLS-1$
                    "  JOIN appv.application app " +  //$NON-NLS-1$
                    " WHERE app.id = :applicationId " +  //$NON-NLS-1$
                    "   AND app.organizationId = :orgId " +  //$NON-NLS-1$
                    "   AND appv.version = :version " +  //$NON-NLS-1$
                    " ORDER BY c.id ASC"; //$NON-NLS-1$
            Query query = entityManager.createQuery(jpql);
            query.setParameter("orgId", organizationId); //$NON-NLS-1$
            query.setParameter("applicationId", applicationId); //$NON-NLS-1$
            query.setParameter("version", version); //$NON-NLS-1$
            List<ContractBean> contracts = (List<ContractBean>) query.getResultList();
            for (ContractBean contractBean : contracts) {
                ApplicationBean application = contractBean.getApplication().getApplication();
                ServiceBean service = contractBean.getService().getService();
                PlanBean plan = contractBean.getPlan().getPlan();
                
                OrganizationBean appOrg = entityManager.find(OrganizationBean.class, application.getOrganizationId());
                OrganizationBean svcOrg = entityManager.find(OrganizationBean.class, service.getOrganizationId());
                
                ContractSummaryBean csb = new ContractSummaryBean();
                csb.setAppId(application.getId());
                csb.setKey(contractBean.getKey());
                csb.setAppOrganizationId(application.getOrganizationId());
                csb.setAppOrganizationName(appOrg.getName());
                csb.setAppName(application.getName());
                csb.setAppVersion(contractBean.getApplication().getVersion());
                csb.setContractId(contractBean.getId());
                csb.setCreatedOn(contractBean.getCreatedOn());
                csb.setPlanId(plan.getId());
                csb.setPlanName(plan.getName());
                csb.setPlanVersion(contractBean.getPlan().getVersion());
                csb.setServiceDescription(service.getDescription());
                csb.setServiceId(service.getId());
                csb.setServiceName(service.getName());
                csb.setServiceOrganizationId(svcOrg.getId());
                csb.setServiceOrganizationName(svcOrg.getName());
                csb.setServiceVersion(contractBean.getService().getVersion());
                
                rval.add(csb);
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new StorageException(t);
        } finally {
            commitTx();
        }
        return rval;
    }

    /**
     * @see org.overlord.apiman.dt.api.core.IStorageQuery#getPlansInOrg(java.lang.String)
     */
    @Override
    public List<PlanSummaryBean> getPlansInOrg(String orgId) throws StorageException {
        Set<String> orgIds = new HashSet<String>();
        orgIds.add(orgId);
        return getPlansInOrgs(orgIds);
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IStorageQuery#getPlansInOrgs(java.util.Set)
     */
    @Override
    public List<PlanSummaryBean> getPlansInOrgs(Set<String> orgIds) throws StorageException {
        List<PlanSummaryBean> rval = new ArrayList<PlanSummaryBean>();
        beginTx();
        try {
            EntityManager entityManager = getActiveEntityManager();
            String jpql = "SELECT p from PlanBean p WHERE p.organizationId IN :orgs ORDER BY p.id ASC"; //$NON-NLS-1$
            Query query = entityManager.createQuery(jpql);
            query.setParameter("orgs", orgIds); //$NON-NLS-1$
            @SuppressWarnings("unchecked")
            List<PlanBean> qr = (List<PlanBean>) query.getResultList();
            for (PlanBean bean : qr) {
                PlanSummaryBean summary = new PlanSummaryBean();
                summary.setId(bean.getId());
                summary.setName(bean.getName());
                summary.setDescription(bean.getDescription());
                OrganizationBean org = entityManager.find(OrganizationBean.class, bean.getOrganizationId());
                summary.setOrganizationId(org.getId());
                summary.setOrganizationName(org.getName());
                rval.add(summary);
            }
            return rval;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new StorageException(t);
        } finally {
            commitTx();
        }
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IStorageQuery#getPlanVersion(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public PlanVersionBean getPlanVersion(String orgId, String planId, String version)
            throws StorageException {
        beginTx();
        try {
            EntityManager entityManager = getActiveEntityManager();
            String jpql = "SELECT v from PlanVersionBean v JOIN v.plan s WHERE s.organizationId = :orgId AND s.id = :planId AND v.version = :version"; //$NON-NLS-1$
            Query query = entityManager.createQuery(jpql);
            query.setParameter("orgId", orgId); //$NON-NLS-1$
            query.setParameter("planId", planId); //$NON-NLS-1$
            query.setParameter("version", version); //$NON-NLS-1$
            
            return (PlanVersionBean) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new StorageException(t);
        } finally {
            commitTx();
        }
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IStorageQuery#getPlanVersions(java.lang.String, java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<PlanVersionBean> getPlanVersions(String orgId, String planId)
            throws StorageException {
        beginTx();
        try {
            EntityManager entityManager = getActiveEntityManager();
            String jpql = "SELECT v from PlanVersionBean v JOIN v.plan s WHERE s.organizationId = :orgId AND s.id = :planId ORDER BY v.id DESC"; //$NON-NLS-1$
            Query query = entityManager.createQuery(jpql);
            query.setParameter("orgId", orgId); //$NON-NLS-1$
            query.setParameter("planId", planId); //$NON-NLS-1$
            
            return (List<PlanVersionBean>) query.getResultList();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new StorageException(t);
        } finally {
            commitTx();
        }
    }
    
    /**
     * @see org.overlord.apiman.dt.api.core.IStorageQuery#getPolicies(java.lang.String, java.lang.String, java.lang.String, org.overlord.apiman.dt.api.beans.policies.PolicyType)
     */
    @SuppressWarnings({ "nls", "unchecked" })
    @Override
    public List<PolicyBean> getPolicies(String organizationId, String entityId, String version,
            PolicyType type) throws StorageException {
        beginTx();
        try {
            EntityManager entityManager = getActiveEntityManager();
            String jpql = 
                      "SELECT p from PolicyBean p "
                    + " WHERE p.organizationId = :orgId "
                    + "   AND p.entityId = :entityId "
                    + "   AND p.entityVersion = :entityVersion "
                    + "   AND p.type = :type"
                    + " ORDER BY p.orderIndex ASC";
            Query query = entityManager.createQuery(jpql);
            query.setParameter("orgId", organizationId);
            query.setParameter("entityId", entityId);
            query.setParameter("entityVersion", version);
            query.setParameter("type", type);
            
            List<PolicyBean> rval = (List<PolicyBean>) query.getResultList();
            for (PolicyBean policyBean : rval) {
                PolicyTemplateUtil.generatePolicyDescription(policyBean);
            }
            return rval;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw new StorageException(t);
        } finally {
            commitTx();
        }
    }

    /**
     * @see org.overlord.apiman.dt.api.core.IApiKeyGenerator#generate()
     */
    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }

}
