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

package org.overlord.apiman.dt.api.rest.impl;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.overlord.apiman.dt.api.beans.apps.ApplicationBean;
import org.overlord.apiman.dt.api.beans.orgs.OrganizationBean;
import org.overlord.apiman.dt.api.beans.search.SearchCriteriaBean;
import org.overlord.apiman.dt.api.beans.search.SearchResultsBean;
import org.overlord.apiman.dt.api.beans.services.ServiceBean;
import org.overlord.apiman.dt.api.beans.summary.ApplicationSummaryBean;
import org.overlord.apiman.dt.api.beans.summary.ServiceSummaryBean;
import org.overlord.apiman.dt.api.core.IStorage;
import org.overlord.apiman.dt.api.core.exceptions.StorageException;
import org.overlord.apiman.dt.api.rest.contract.ISearchResource;
import org.overlord.apiman.dt.api.rest.contract.exceptions.InvalidSearchCriteriaException;
import org.overlord.apiman.dt.api.rest.contract.exceptions.OrganizationNotFoundException;
import org.overlord.apiman.dt.api.rest.contract.exceptions.SystemErrorException;
import org.overlord.apiman.dt.api.rest.impl.util.SearchCriteriaUtil;

/**
 * Implementation of the Search API.
 * 
 * @author eric.wittmann@redhat.com
 */
@ApplicationScoped
public class SearchResourceImpl implements ISearchResource {

    @Inject IStorage storage;
    
    /**
     * Constructor.
     */
    public SearchResourceImpl() {
    }
    
    /**
     * @see org.overlord.apiman.dt.api.rest.contract.ISearchResource#searchOrgs(org.overlord.apiman.dt.api.beans.search.SearchCriteriaBean)
     */
    @Override
    public SearchResultsBean<OrganizationBean> searchOrgs(SearchCriteriaBean criteria)
            throws InvalidSearchCriteriaException {
        // TODO only return organizations that the user is permitted to see?
        try {
            SearchCriteriaUtil.validateSearchCriteria(criteria);
            storage.beginTx();
            SearchResultsBean<OrganizationBean> rval = storage.find(criteria, OrganizationBean.class);
            storage.commitTx();
            return rval;
        } catch (StorageException e) {
            storage.rollbackTx();
            throw new SystemErrorException(e);
        }
    }
    
    /**
     * @see org.overlord.apiman.dt.api.rest.contract.ISearchResource#searchApps(org.overlord.apiman.dt.api.beans.search.SearchCriteriaBean)
     */
    @Override
    public SearchResultsBean<ApplicationSummaryBean> searchApps(SearchCriteriaBean criteria)
            throws OrganizationNotFoundException, InvalidSearchCriteriaException {
        // TODO only return applications that the user is permitted to see?
        SearchCriteriaUtil.validateSearchCriteria(criteria);
        try {
            storage.beginTx();
            SearchResultsBean<ApplicationBean> result = storage.find(criteria, ApplicationBean.class);
            SearchResultsBean<ApplicationSummaryBean> rval = new SearchResultsBean<ApplicationSummaryBean>();
            rval.setTotalSize(result.getTotalSize());
            List<ApplicationBean> beans = result.getBeans();
            rval.setBeans(new ArrayList<ApplicationSummaryBean>(beans.size()));
            for (ApplicationBean application : beans) {
                ApplicationSummaryBean summary = new ApplicationSummaryBean();
                OrganizationBean organization = storage.get(application.getOrganizationId(), OrganizationBean.class);
                summary.setId(application.getId());
                summary.setName(application.getName());
                summary.setDescription(application.getDescription());
                // TODO find the number of contracts
                summary.setNumContracts(0);
                summary.setOrganizationId(application.getOrganizationId());
                summary.setOrganizationName(organization.getName());
                rval.getBeans().add(summary);
            }
            storage.commitTx();
            return rval;
        } catch (StorageException e) {
            storage.rollbackTx();
            throw new SystemErrorException(e);
        }
    }

    /**
     * @see org.overlord.apiman.dt.api.rest.contract.ISearchResource#searchServices(org.overlord.apiman.dt.api.beans.search.SearchCriteriaBean)
     */
    @Override
    public SearchResultsBean<ServiceSummaryBean> searchServices(SearchCriteriaBean criteria)
            throws OrganizationNotFoundException, InvalidSearchCriteriaException {
        // TODO only return services that the user is permitted to see?
        SearchCriteriaUtil.validateSearchCriteria(criteria);
        try {
            storage.beginTx();
            SearchResultsBean<ServiceBean> result = storage.find(criteria, ServiceBean.class);
            SearchResultsBean<ServiceSummaryBean> rval = new SearchResultsBean<ServiceSummaryBean>();
            rval.setTotalSize(result.getTotalSize());
            List<ServiceBean> beans = result.getBeans();
            rval.setBeans(new ArrayList<ServiceSummaryBean>(beans.size()));
            for (ServiceBean service : beans) {
                ServiceSummaryBean summary = new ServiceSummaryBean();
                OrganizationBean organization = storage.get(service.getOrganizationId(), OrganizationBean.class);
                summary.setId(service.getId());
                summary.setName(service.getName());
                summary.setDescription(service.getDescription());
                summary.setCreatedOn(service.getCreatedOn());
                summary.setOrganizationId(service.getOrganizationId());
                summary.setOrganizationName(organization.getName());
                rval.getBeans().add(summary);
            }
            storage.commitTx();
            return rval;
        } catch (StorageException e) {
            storage.rollbackTx();
            throw new SystemErrorException(e);
        }
    }

    /**
     * @return the storage
     */
    public IStorage getStorage() {
        return storage;
    }

    /**
     * @param storage the storage to set
     */
    public void setStorage(IStorage storage) {
        this.storage = storage;
    }
}
