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

import io.apiman.manager.api.beans.policies.PolicyChainBean;
import io.apiman.manager.api.beans.policies.PolicyType;
import io.apiman.manager.api.beans.summary.PolicySummaryBean;
import io.apiman.manager.ui.client.local.AppMessages;
import io.apiman.manager.ui.client.local.events.PoliciesReorderedEvent;
import io.apiman.manager.ui.client.local.events.RemovePolicyEvent;
import io.apiman.manager.ui.client.local.pages.plan.PlanPolicyList;
import io.apiman.manager.ui.client.local.services.rest.IRestInvokerCallback;
import io.apiman.manager.ui.client.local.util.MultimapUtil;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.ui.nav.client.local.Page;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.Templated;

import com.google.gwt.user.client.ui.Anchor;


/**
 * The "Plan" page, with the Policies tab displayed.
 *
 * @author eric.wittmann@redhat.com
 */
@Templated("/io/apiman/manager/ui/client/local/site/plan-policies.html#page")
@Page(path="plan-policies")
@Dependent
public class PlanPoliciesPage extends AbstractPlanPage {

    private List<PolicySummaryBean> policyBeans;

    @Inject @DataField
    Anchor toNewPolicy;
    @Inject @DataField
    PlanPolicyList policies;
    
    /**
     * Constructor.
     */
    public PlanPoliciesPage() {
    }

    /**
     * Called after the bean is created.
     */
    @PostConstruct
    protected void postConstruct() {
        policies.addRemovePolicyHandler(new RemovePolicyEvent.Handler() {
            @Override
            public void onRemovePolicy(RemovePolicyEvent event) {
                doRemovePolicy(event.getPolicy());
            }
        });
        policies.addPoliciesReorderedHandler(new PoliciesReorderedEvent.Handler() {
            @Override
            public void onPoliciesReordered(PoliciesReorderedEvent event) {
                doReorderPolicies();
            }
        });
    }

    /**
     * @see io.apiman.manager.ui.client.local.pages.AbstractPlanPage#doLoadPageData()
     */
    @Override
    protected int doLoadPageData() {
        int rval = super.doLoadPageData();
        // we'll trigger an additional load after the service version has been loaded (hence the +1)
        return rval + 1;
    }
    
    /**
     * @see io.apiman.manager.ui.client.local.pages.AbstractPlanPage#onPlanVersionLoaded()
     */
    @Override
    protected void onPlanVersionLoaded() {
        String orgId = org;
        String planId = plan;
        String planVersion = versionBean.getVersion();
        rest.getPlanPolicies(orgId, planId, planVersion, new IRestInvokerCallback<List<PolicySummaryBean>>() {
            @Override
            public void onSuccess(List<PolicySummaryBean> response) {
                policyBeans = response;
                dataPacketLoaded();
            }
            @Override
            public void onError(Throwable error) {
                dataPacketError(error);
            }
        });
    }
    
    /**
     * @see io.apiman.manager.ui.client.local.pages.AbstractAppPage#renderPage()
     */
    @Override
    protected void renderPage() {
        super.renderPage();
        
        String newPolicyHref = navHelper.createHrefToPage(NewPolicyPage.class,
                MultimapUtil.fromMultiple("org", this.planBean.getOrganization().getId(), "id", this.planBean.getId(),  //$NON-NLS-1$ //$NON-NLS-2$
                        "ver", this.versionBean.getVersion(), "type", PolicyType.Plan.toString())); //$NON-NLS-1$ //$NON-NLS-2$
        toNewPolicy.setHref(newPolicyHref);
        policies.setEntityInfo(org, plan, versionBean.getVersion(), PolicyType.Plan);
        policies.setValue(policyBeans);
    }

    /**
     * Called when the user chooses to remove a policy.
     * @param policy
     */
    protected void doRemovePolicy(final PolicySummaryBean policy) {
        rest.removePolicy(PolicyType.Plan, org, versionBean.getPlan().getId(), versionBean.getVersion(),
                policy.getId(), new IRestInvokerCallback<Void>() {
            @Override
            public void onSuccess(Void response) {
                policyBeans.remove(policy);
                policies.setValue(policyBeans);
            }
            @Override
            public void onError(Throwable error) {
                dataPacketError(error);
            }
        });
    }

    /**
     * Reorder the policies according to the user's actions.
     */
    protected void doReorderPolicies() {
        PolicyChainBean chain = new PolicyChainBean();
        chain.getPolicies().addAll(policies.getValue());
        rest.reorderPlanPolicies(org, versionBean.getPlan().getId(), versionBean.getVersion(), chain, new IRestInvokerCallback<Void>() {
            @Override
            public void onSuccess(Void response) {
            }
            @Override
            public void onError(Throwable error) {
                dataPacketError(error);
            }
        });
    }
    
    /**
     * @see io.apiman.manager.ui.client.local.pages.AbstractPage#getPageTitle()
     */
    @Override
    protected String getPageTitle() {
        return i18n.format(AppMessages.TITLE_PLAN_POLICIES, planBean.getName());
    }

}
