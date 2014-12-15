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
package io.apiman.manager.ui.client.local.pages;

import io.apiman.manager.api.beans.members.MemberBean;
import io.apiman.manager.api.beans.orgs.OrganizationBean;
import io.apiman.manager.api.beans.summary.ServiceSummaryBean;
import io.apiman.manager.ui.client.local.AppMessages;
import io.apiman.manager.ui.client.local.events.RequestMembershipEvent;
import io.apiman.manager.ui.client.local.events.RequestMembershipEvent.Handler;
import io.apiman.manager.ui.client.local.pages.common.Breadcrumb;
import io.apiman.manager.ui.client.local.pages.consumer.ConsumerOrgMemberList;
import io.apiman.manager.ui.client.local.pages.consumer.ConsumerOrgServiceList;
import io.apiman.manager.ui.client.local.pages.consumer.OrganizationCard;
import io.apiman.manager.ui.client.local.services.rest.IRestInvokerCallback;
import io.apiman.manager.ui.client.local.util.MultimapUtil;
import io.apiman.manager.ui.client.local.widgets.SearchBox;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.ui.nav.client.local.Page;
import org.jboss.errai.ui.nav.client.local.PageState;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.Templated;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;


/**
 * The "Organization Details" page - part of the consumer UI.  This page
 * allows users to see details about an Organization.  It displays useful
 * information such as the list of members and the services offered by
 * the org.
 *
 * @author eric.wittmann@redhat.com
 */
@Templated("/io/apiman/manager/ui/client/local/site/consumer-org.html#page")
@Page(path="corg")
@Dependent
public class ConsumerOrgPage extends AbstractPage {
    
    @PageState
    protected String org;

    @Inject @DataField
    Breadcrumb breadcrumb;

    @Inject @DataField
    private OrganizationCard organizationCard;
    @Inject @DataField
    private ConsumerOrgMemberList members;
    @Inject @DataField
    private ConsumerOrgServiceList services;
    @Inject @DataField
    private SearchBox serviceFilter;

    protected OrganizationBean orgBean;
    protected List<MemberBean> memberBeans;
    protected List<ServiceSummaryBean> serviceBeans;

    /**
     * Constructor.
     */
    public ConsumerOrgPage() {
    }
    
    @PostConstruct
    protected void postConstruct() {
        serviceFilter.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                filterServices();
            }
        });
        organizationCard.addRequestMembershipHandler(new Handler() {
            @Override
            public void onRequestMembership(RequestMembershipEvent event) {
                // TODO impl
            }
        });
        serviceFilter.setPlaceholder(i18n.format(AppMessages.CONSUMER_ORG_FILTER_PLACEHOLDER));
    }
    
    /**
     * @see io.apiman.manager.ui.client.local.pages.AbstractPage#doLoadPageData()
     */
    @Override
    protected int doLoadPageData() {
        int rval = super.doLoadPageData();
        rest.getOrganization(org, new IRestInvokerCallback<OrganizationBean>() {
            @Override
            public void onSuccess(OrganizationBean response) {
                orgBean = response;
                dataPacketLoaded();
            }
            @Override
            public void onError(Throwable error) {
                dataPacketError(error);
            }
        });
        rest.getOrgMembers(org, new IRestInvokerCallback<List<MemberBean>>() {
            @Override
            public void onSuccess(List<MemberBean> response) {
                memberBeans = response;
                dataPacketLoaded();
            }
            @Override
            public void onError(Throwable error) {
                dataPacketError(error);
            }
        });
        rest.getOrgServices(org, new IRestInvokerCallback<List<ServiceSummaryBean>>() {
            @Override
            public void onSuccess(List<ServiceSummaryBean> response) {
                serviceBeans = response;
                dataPacketLoaded();
            }
            @Override
            public void onError(Throwable error) {
                dataPacketError(error);
            }
        });
        return rval + 3;
    }

    /**
     * Apply a filter to the list of services.
     */
    protected void filterServices() {
        List<ServiceSummaryBean> filtered = new ArrayList<ServiceSummaryBean>();
        for (ServiceSummaryBean service : serviceBeans) {
            if (matchesFilter(service)) {
                filtered.add(service);
            }
        }
        services.setFilteredValue(filtered);
    }

    /**
     * Returns true if the given service matches the current filter.
     * @param service
     */
    private boolean matchesFilter(ServiceSummaryBean service) {
        if (serviceFilter.getValue() == null || serviceFilter.getValue().trim().length() == 0)
            return true;
        if (service.getName().toUpperCase().contains(serviceFilter.getValue().toUpperCase()))
            return true;
        return false;
    }

    /**
     * @see io.apiman.manager.ui.client.local.pages.AbstractPage#renderPage()
     */
    @Override
    protected void renderPage() {
        organizationCard.setValue(orgBean);
        members.setValue(memberBeans);
        services.setValue(serviceBeans);

        String dashHref = navHelper.createHrefToPage(DashboardPage.class, MultimapUtil.emptyMap());
        String consumerOrgsHref = navHelper.createHrefToPage(ConsumerOrgsPage.class, MultimapUtil.emptyMap());
        breadcrumb.addItem(dashHref, "home", i18n.format(AppMessages.HOME)); //$NON-NLS-1$
        breadcrumb.addItem(consumerOrgsHref, "search", i18n.format(AppMessages.ORGANIZATIONS)); //$NON-NLS-1$
        breadcrumb.addActiveItem("shield", orgBean.getName()); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.ui.client.local.pages.AbstractPage#getPageTitle()
     */
    @Override
    protected String getPageTitle() {
        return i18n.format(AppMessages.TITLE_CONSUME_ORG);
    }

}
