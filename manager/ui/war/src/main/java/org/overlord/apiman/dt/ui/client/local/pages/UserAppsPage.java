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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.ui.nav.client.local.Page;
import org.jboss.errai.ui.nav.client.local.TransitionAnchor;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.overlord.apiman.dt.api.beans.summary.ApplicationSummaryBean;
import org.overlord.apiman.dt.ui.client.local.AppMessages;
import org.overlord.apiman.dt.ui.client.local.pages.user.UserApplicationList;
import org.overlord.apiman.dt.ui.client.local.services.rest.IRestInvokerCallback;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.TextBox;


/**
 * The "User" page, with the Organizations tab displayed.
 *
 * @author eric.wittmann@redhat.com
 */
@Templated("/org/overlord/apiman/dt/ui/client/local/site/user-apps.html#page")
@Page(path="user-apps")
@Dependent
public class UserAppsPage extends AbstractUserPage {

    private List<ApplicationSummaryBean> apps;

    @Inject @DataField
    TransitionAnchor<NewAppPage> toNewApp;
    
    @Inject @DataField
    TextBox appFilter;
    @Inject @DataField
    UserApplicationList applications;

    /**
     * Constructor.
     */
    public UserAppsPage() {
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
     * @see org.overlord.apiman.dt.ui.client.local.pages.AbstractUserPage#doLoadPageData()
     */
    @Override
    protected int doLoadPageData() {
        int rval = super.doLoadPageData();
        rest.getUserApps(user, new IRestInvokerCallback<List<ApplicationSummaryBean>>() {
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
     * @see org.overlord.apiman.dt.ui.client.local.pages.AbstractUserPage#renderPage()
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
        if (app.getOrganizationName().toUpperCase().contains(appFilter.getValue().toUpperCase()))
            return true;
        return false;
    }

    /**
     * @see org.overlord.apiman.dt.ui.client.local.pages.AbstractPage#getPageTitle()
     */
    @Override
    protected String getPageTitle() {
        return i18n.format(AppMessages.TITLE_USER_APPS, userBean.getFullName());
    }

}
