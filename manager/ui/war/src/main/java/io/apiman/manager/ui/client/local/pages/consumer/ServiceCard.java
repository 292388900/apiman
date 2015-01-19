/*
 * Copyright 2013 JBoss Inc
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

package io.apiman.manager.ui.client.local.pages.consumer;

import io.apiman.manager.api.beans.orgs.OrganizationBean;
import io.apiman.manager.api.beans.services.ServiceVersionBean;
import io.apiman.manager.api.beans.summary.ServiceVersionSummaryBean;
import io.apiman.manager.ui.client.local.pages.ConsumerOrgPage;
import io.apiman.manager.ui.client.local.services.NavigationHelperService;
import io.apiman.manager.ui.client.local.util.MultimapUtil;
import io.apiman.manager.ui.client.local.widgets.SimpleVersionSelectBox;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.Templated;

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.TakesValue;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;

/**
 * A way to display info about a service in the consumer UI.
 *
 * @author eric.wittmann@redhat.com
 */
@Templated("/io/apiman/manager/ui/client/local/site/consumer-service.html#serviceCard")
@Dependent
public class ServiceCard extends Composite implements TakesValue<ServiceVersionBean>, HasValueChangeHandlers<String> {

    @Inject 
    protected NavigationHelperService navHelper;

    @Inject @DataField
    private Anchor titleOrg;
    @Inject @DataField
    private InlineLabel titleService;
    @Inject @DataField
    private Label description;
    @Inject @DataField
    private SimpleVersionSelectBox versionSelector;
    @Inject @DataField
    private Label noVersionsMessage;
    
    private ServiceVersionBean value;

    /**
     * Constructor.
     */
    public ServiceCard() {
    }
    
    @PostConstruct
    protected void postConstruct() {
        noVersionsMessage.setVisible(false);
    }

    /**
     * @see com.google.gwt.event.logical.shared.HasValueChangeHandlers#addValueChangeHandler(com.google.gwt.event.logical.shared.ValueChangeHandler)
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
        return versionSelector.addValueChangeHandler(handler);
    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#getValue()
     */
    @Override
    public ServiceVersionBean getValue() {
        return value;
    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#setValue(java.lang.Object)
     */
    @Override
    public void setValue(ServiceVersionBean value) {
        this.value = value;
        refresh();
    }

    /**
     * Refresh the UI with the new data.
     */
    private void refresh() {
        titleService.setText(value.getService().getName());
        description.setText(value.getService().getDescription());
    }
    
    /**
     * Sets the list of service versions.
     * @param versions
     */
    public void setVersions(List<ServiceVersionSummaryBean> versions) {
        List<String> versionList = new ArrayList<String>();
        for (ServiceVersionSummaryBean version : versions) {
            versionList.add(version.getVersion());
        }
        versionSelector.setOptions(versionList);

        if (versions == null || versions.isEmpty()) {
            noVersionsMessage.setVisible(true);
        } else {
            noVersionsMessage.setVisible(false);
        }
    }
    
    /**
     * Sets the organization.
     * @param organization
     */
    public void setOrganization(OrganizationBean organization) {
        titleOrg.setText(organization.getName());
        String toOrgPage = navHelper.createHrefToPage(ConsumerOrgPage.class, MultimapUtil.singleItemMap("org", organization.getId())); //$NON-NLS-1$
        titleOrg.setHref(toOrgPage);
    }

    /**
     * @param version
     */
    public void selectVersion(String version) {
        versionSelector.setValue(version);
    }

}
