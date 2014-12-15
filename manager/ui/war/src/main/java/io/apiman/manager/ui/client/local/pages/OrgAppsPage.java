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

import io.apiman.manager.api.beans.idm.PermissionType;
import io.apiman.manager.api.beans.summary.ApplicationSummaryBean;
import io.apiman.manager.ui.client.local.AppMessages;
import io.apiman.manager.ui.client.local.pages.org.OrgApplicationList;
import io.apiman.manager.ui.client.local.services.rest.IRestInvokerCallback;
import io.apiman.manager.ui.client.local.widgets.SearchBox;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.ui.nav.client.local.Page;
import org.jboss.errai.ui.nav.client.local.TransitionAnchor;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.Templated;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;


/**
 * The "Organization" page, with the Applications tab displayed.
 *
 * @author eric.wittmann@redhat.com
 */
@Templated("/io/apiman/manager/ui/client/local/site/org-apps.html#page")
@Page(path="org-apps")
@Dependent
public class OrgAppsPage extends AbstractOrgPage {

    private List<ApplicationSummaryBean> apps;

    @Inject @DataField
    TransitionAnchor<NewAppPage> toNewApp;

    @Inject @DataField
    SearchBox appFilter;
    @Inject @DataField
    OrgApplicationList applications;

    /**
     * Constructor.
     */
    public OrgAppsPage() {
    }

    /**
     * @see io.apiman.manager.ui.client.local.pages.AbstractPage#isAuthorized()
     */
    @Override
    protected boolean isAuthorized() {
        return hasPermission(PermissionType.appView);
    }

    /**
     * Called after the bean is created.
     */
    @PostConstruct
    protected void postConstruct() {
        appFilter.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                filterApps();
            }
        });
    }
    
    /**
     * @see io.apiman.manager.ui.client.local.pages.AbstractOrgPage#doLoadPageData()
     */
    @Override
    protected int doLoadPageData() {
        int rval = super.doLoadPageData();
        rest.getOrgApplications(org, new IRestInvokerCallback<List<ApplicationSummaryBean>>() {
            @Override
            public void onSuccess(List<ApplicationSummaryBean> response) {
                apps = response;
                dataPacketLoaded();
            }
            @Override
            public void onError(Throwable error) {
                dataPacketError(error);
            }
        });
        return rval + 1;
    }

    /**
     * @see io.apiman.manager.ui.client.local.pages.AbstractUserPage#renderPage()
     */
    @Override
    protected void renderPage() {
        super.renderPage();
        applications.setValue(apps);
    }
    
    /**
     * Apply a filter to the list of applications.
     */
    protected void filterApps() {
        List<ApplicationSummaryBean> filtered = new ArrayList<ApplicationSummaryBean>();
        for (ApplicationSummaryBean app : apps) {
            if (matchesFilter(app)) {
                filtered.add(app);
            }
        }
        applications.setFilteredValue(filtered);
    }

    /**
     * Returns true if the given app matches the current filter.
     * @param app
     */
    private boolean matchesFilter(ApplicationSummaryBean app) {
        if (appFilter.getValue() == null || appFilter.getValue().trim().length() == 0)
            return true;
        if (app.getName().toUpperCase().contains(appFilter.getValue().toUpperCase()))
            return true;
        return false;
    }

    /**
     * @see io.apiman.manager.ui.client.local.pages.AbstractPage#getPageTitle()
     */
    @Override
    protected String getPageTitle() {
        return i18n.format(AppMessages.TITLE_ORG_APPS, organizationBean.getName());
    }

}
