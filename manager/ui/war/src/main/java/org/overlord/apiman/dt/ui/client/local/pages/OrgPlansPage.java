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
import org.overlord.apiman.dt.api.beans.summary.PlanSummaryBean;
import org.overlord.apiman.dt.ui.client.local.AppMessages;
import org.overlord.apiman.dt.ui.client.local.pages.org.OrgPlanList;
import org.overlord.apiman.dt.ui.client.local.services.rest.IRestInvokerCallback;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.TextBox;


/**
 * The "Organization" page, with the Plans tab displayed.
 *
 * @author eric.wittmann@redhat.com
 */
@Templated("/org/overlord/apiman/dt/ui/client/local/site/org-plans.html#page")
@Page(path="org-plans")
@Dependent
public class OrgPlansPage extends AbstractOrgPage {
    
    private List<PlanSummaryBean> planBeans;

    @Inject @DataField
    TransitionAnchor<NewPlanPage> toNewPlan;

    @Inject @DataField
    TextBox planFilter;
    @Inject @DataField
    OrgPlanList plans;

    /**
     * Constructor.
     */
    public OrgPlansPage() {
    }

    /**
     * Called after the bean is created.
     */
    @PostConstruct
    protected void postConstruct() {
        planFilter.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                filterPlans();
            }
        });
    }
    
    /**
     * @see org.overlord.apiman.dt.ui.client.local.pages.AbstractOrgPage#doLoadPageData()
     */
    @Override
    protected int doLoadPageData() {
        int rval = super.doLoadPageData();
        rest.getOrgPlans(org, new IRestInvokerCallback<List<PlanSummaryBean>>() {
            @Override
            public void onSuccess(List<PlanSummaryBean> response) {
                planBeans = response;
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
        plans.setValue(planBeans);
    }

    /**
     * Apply a filter to the list of plans.
     */
    protected void filterPlans() {
        List<PlanSummaryBean> filtered = new ArrayList<PlanSummaryBean>();
        for (PlanSummaryBean plan : planBeans) {
            if (matchesFilter(plan)) {
                filtered.add(plan);
            }
        }
        plans.setFilteredValue(filtered);
    }

    /**
     * Returns true if the given plan matches the current filter.
     * @param plan
     */
    private boolean matchesFilter(PlanSummaryBean plan) {
        if (planFilter.getValue() == null || planFilter.getValue().trim().length() == 0)
            return true;
        if (plan.getName().toUpperCase().contains(planFilter.getValue().toUpperCase()))
            return true;
        return false;
    }

    /**
     * @see org.overlord.apiman.dt.ui.client.local.pages.AbstractPage#getPageTitle()
     */
    @Override
    protected String getPageTitle() {
        return i18n.format(AppMessages.TITLE_ORG_PLANS, organizationBean.getName());
    }

}
