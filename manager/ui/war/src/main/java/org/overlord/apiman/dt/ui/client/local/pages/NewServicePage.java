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
package org.overlord.apiman.dt.ui.client.local.pages;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.ui.nav.client.local.Page;
import org.jboss.errai.ui.nav.client.local.TransitionTo;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.EventHandler;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.overlord.apiman.dt.api.beans.orgs.OrganizationBean;
import org.overlord.apiman.dt.api.beans.services.ServiceBean;
import org.overlord.apiman.dt.api.beans.services.ServiceVersionBean;
import org.overlord.apiman.dt.api.beans.summary.OrganizationSummaryBean;
import org.overlord.apiman.dt.ui.client.local.AppMessages;
import org.overlord.apiman.dt.ui.client.local.pages.common.OrganizationSelector;
import org.overlord.apiman.dt.ui.client.local.services.ContextKeys;
import org.overlord.apiman.dt.ui.client.local.services.CurrentContextService;
import org.overlord.apiman.dt.ui.client.local.services.rest.IRestInvokerCallback;
import org.overlord.apiman.dt.ui.client.local.util.MultimapUtil;
import org.overlord.commons.gwt.client.local.widgets.AsyncActionButton;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.TextBox;


/**
 * Page that lets the user create a new Service.
 *
 * @author eric.wittmann@redhat.com
 */
@Templated("/org/overlord/apiman/dt/ui/client/local/site/new-service.html#page")
@Page(path="new-service")
@Dependent
public class NewServicePage extends AbstractPage {
    
    @Inject
    CurrentContextService context;
    @Inject
    TransitionTo<ServiceOverviewPage> toServiceOverview;
    
    List<OrganizationSummaryBean> organizations;
    
    @Inject @DataField
    OrganizationSelector orgSelector;
    @Inject @DataField
    TextBox name;
    @Inject @DataField
    TextBox version;
    @Inject @DataField
    TextBox description;
    @Inject @DataField
    AsyncActionButton createButton;
    
    /**
     * Constructor.
     */
    public NewServicePage() {
    }
    
    @PostConstruct
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void postConstruct() {
        orgSelector.addValueChangeHandler(new ValueChangeHandler<OrganizationSummaryBean>() {
            @Override
            public void onValueChange(ValueChangeEvent<OrganizationSummaryBean> event) {
                name.setFocus(true);
            }
        });
        orgSelector.addValueChangeHandler(new ValueChangeHandler() {
            @Override
            public void onValueChange(ValueChangeEvent event) {
                onFormUpdated();
            }
        });
        KeyUpHandler kph = new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                onFormUpdated();
            }
        };
        name.addKeyUpHandler(kph);
        version.addKeyUpHandler(kph);
    }
    
    /**
     * @see org.overlord.apiman.dt.ui.client.local.pages.AbstractPage#doLoadPageData()
     */
    @Override
    protected int doLoadPageData() {
        int rval = super.doLoadPageData();
        rest.getCurrentUserOrgs(new IRestInvokerCallback<List<OrganizationSummaryBean>>() {
            @Override
            public void onSuccess(List<OrganizationSummaryBean> response) {
                organizations = response;
                dataPacketLoaded();
            }
            @Override
            public void onError(Throwable error) {
                dataPacketError(error);
            }
        });
        return rval+1;
    }
    
    /**
     * @see org.overlord.apiman.dt.ui.client.local.pages.AbstractPage#renderPage()
     */
    @Override
    protected void renderPage() {
        orgSelector.setOrganizations(organizations);
        OrganizationBean org = (OrganizationBean) context.getAttribute(ContextKeys.CURRENT_ORGANIZATION);
        if (org != null) {
            for (OrganizationSummaryBean bean : organizations) {
                if (bean.getId().equals(org.getId())) {
                    orgSelector.setValue(bean);
                    break;
                }
            }
        }
    }
    
    /**
     * @see org.overlord.apiman.dt.ui.client.local.pages.AbstractPage#onPageLoaded()
     */
    @Override
    protected void onPageLoaded() {
        if (orgSelector.getValue() == null)
            orgSelector.setFocus(true);
        else
            name.setFocus(true);
        createButton.reset();
        onFormUpdated();
    }
    
    /**
     * Called when the user clicks the Create Organization button.
     * @param event
     */
    @EventHandler("createButton")
    public void onCreate(ClickEvent event) {
        createButton.onActionStarted();
        final String orgId = orgSelector.getValue().getId();
        final String serviceVersion = version.getValue();
        ServiceBean bean = new ServiceBean();
        bean.setName(name.getValue());
        bean.setDescription(description.getValue());
        // Create the service and then create an initial service version.
        rest.createService(orgId, bean, new IRestInvokerCallback<ServiceBean>() {
            @Override
            public void onSuccess(final ServiceBean response) {
                final String serviceId = response.getId();
                ServiceVersionBean vb = new ServiceVersionBean();
                vb.setVersion(serviceVersion);
                rest.createServiceVersion(orgId, serviceId, vb, new IRestInvokerCallback<ServiceVersionBean>() {
                    @Override
                    public void onSuccess(ServiceVersionBean response) {
                        createButton.onActionComplete();
                        toServiceOverview.go(MultimapUtil.fromMultiple("org", orgId, "service", serviceId, "version", serviceVersion)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    }
                    @Override
                    public void onError(Throwable error) {
                        dataPacketError(error);
                    }
                });
            }
            @Override
            public void onError(Throwable error) {
                dataPacketError(error);
            }
        });
    }

    /**
     * Called whenever the user modifies the form.  Checks for form validity and then
     * enables or disables the Create button as appropriate.
     */
    protected void onFormUpdated() {
        boolean formComplete = true;
        if (orgSelector.getValue() == null)
            formComplete = false;
        if (name.getValue() == null || name.getValue().trim().length() == 0)
            formComplete = false;
        if (version.getValue() == null || version.getValue().trim().length() == 0)
            formComplete = false;
        createButton.setEnabled(formComplete);
    }

    /**
     * @see org.overlord.apiman.dt.ui.client.local.pages.AbstractPage#getPageTitle()
     */
    @Override
    protected String getPageTitle() {
        return i18n.format(AppMessages.TITLE_NEW_SERVICE);
    }

}
