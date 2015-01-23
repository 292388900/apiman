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

import io.apiman.manager.api.beans.plans.PlanStatus;
import io.apiman.manager.api.beans.services.ServicePlanBean;
import io.apiman.manager.api.beans.services.UpdateServiceVersionBean;
import io.apiman.manager.api.beans.summary.PlanSummaryBean;
import io.apiman.manager.api.beans.summary.PlanVersionSummaryBean;
import io.apiman.manager.ui.client.local.AppMessages;
import io.apiman.manager.ui.client.local.pages.service.ServicePlansSelector;
import io.apiman.manager.ui.client.local.services.rest.IRestInvokerCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.ui.nav.client.local.Page;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.EventHandler;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.overlord.commons.gwt.client.local.widgets.AsyncActionButton;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.SimpleCheckBox;


/**
 * The "Service" page, with the Plans tab displayed.
 *
 * @author eric.wittmann@redhat.com
 */
@Templated("/io/apiman/manager/ui/client/local/site/service-plans.html#page")
@Page(path="service-plans")
@Dependent
public class ServicePlansPage extends AbstractServicePage {
    
    List<PlanSummaryBean> planBeans;
    Map<PlanSummaryBean, List<PlanVersionSummaryBean>> planVersions = new HashMap<PlanSummaryBean, List<PlanVersionSummaryBean>>();
    
    @Inject @DataField
    SimpleCheckBox publicService;
    @Inject @DataField
    ServicePlansSelector plans;
    @Inject @DataField
    AsyncActionButton saveButton;
    @Inject @DataField
    Button cancelButton;

    /**
     * Constructor.
     */
    public ServicePlansPage() {
    }
    
    @PostConstruct
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void postConstruct() {
        ValueChangeHandler handler = new ValueChangeHandler() {
            @Override
            public void onValueChange(ValueChangeEvent event) {
                onPlansChange();
            }
        };
        plans.addValueChangeHandler(handler);
        publicService.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onPlansChange();
            }
        });
    }

    /**
     * Called when the user changes something in the plan list.
     */
    protected void onPlansChange() {
        saveButton.setEnabled(true);
        cancelButton.setEnabled(true);
    }

    /**
     * @see io.apiman.manager.ui.client.local.pages.AbstractServicePage#doLoadPageData()
     */
    @Override
    protected int doLoadPageData() {
        int rval = super.doLoadPageData();
        rest.getOrgPlans(org, new IRestInvokerCallback<List<PlanSummaryBean>>() {
            @Override
            public void onSuccess(List<PlanSummaryBean> response) {
                planBeans = response;
                planVersions.clear();
                increaseExpectedDataPackets(response.size());
                dataPacketLoaded();
                for (final PlanSummaryBean planSummaryBean : response) {
                    rest.getPlanVersions(org, planSummaryBean.getId(), new IRestInvokerCallback<List<PlanVersionSummaryBean>>() {
                        @Override
                        public void onSuccess(List<PlanVersionSummaryBean> response) {
                            List<PlanVersionSummaryBean> lockedPlans = new ArrayList<PlanVersionSummaryBean>(response.size());
                            for (PlanVersionSummaryBean pvsb : response) {
                                if (pvsb.getStatus() == PlanStatus.Locked) {
                                    lockedPlans.add(pvsb);
                                }
                            }
                            
                            planVersions.put(planSummaryBean, lockedPlans);
                            dataPacketLoaded();
                        }
                        @Override
                        public void onError(Throwable error) {
                            dataPacketError(error);
                        }
                    });
                }
            }
            @Override
            public void onError(Throwable error) {
                dataPacketError(error);
            }
        });
        return rval + 1;
    }
    
    /**
     * @see io.apiman.manager.ui.client.local.pages.AbstractServicePage#renderPage()
     */
    @Override
    protected void renderPage() {
        super.renderPage();
        saveButton.reset();
        saveButton.setEnabled(false);
        cancelButton.setEnabled(false);
        plans.setChoices(planBeans, planVersions);
        Set<ServicePlanBean> theplans = new HashSet<ServicePlanBean>();
        if (versionBean.getPlans() != null) {
            theplans.addAll(versionBean.getPlans());
        }
        plans.setValue(theplans);
        publicService.setValue(versionBean.isPublicService());
    }
    
    /**
     * @see io.apiman.manager.ui.client.local.pages.AbstractPage#getPageTitle()
     */
    @Override
    protected String getPageTitle() {
        return i18n.format(AppMessages.TITLE_SERVICE_PLANS, serviceBean.getName());
    }
    
    /**
     * Called when the user clicks the Save button.
     */
    @EventHandler("saveButton")
    protected void onSave(ClickEvent event) {
        saveButton.onActionStarted();
        cancelButton.setEnabled(false);
        
        final Set<ServicePlanBean> newplans = plans.getValue();
        versionBean.setPlans(newplans);
        versionBean.setPublicService(this.publicService.getValue());
        
        UpdateServiceVersionBean update = new UpdateServiceVersionBean();
        update.setPlans(newplans);
        update.setPublicService(this.publicService.getValue());
        rest.updateServiceVersion(serviceBean.getOrganization().getId(), serviceBean.getId(),
                versionBean.getVersion(), update, new IRestInvokerCallback<Void>() {
            @Override
            public void onSuccess(Void response) {
                saveButton.onActionComplete();
                saveButton.setEnabled(false);
                refreshServiceVersion();
            }
            @Override
            public void onError(Throwable error) {
                dataPacketError(error);
            }
        });
    }

    /**
     * Called when the user clicks the Cancel button.
     */
    @EventHandler("cancelButton")
    protected void onCancel(ClickEvent event) {
        saveButton.reset();
        saveButton.setEnabled(false);
        cancelButton.setEnabled(false);
        Set<ServicePlanBean> resetValues = new HashSet<ServicePlanBean>();
        if (versionBean.getPlans() != null) {
            resetValues.addAll(versionBean.getPlans());
        }
        plans.setValue(resetValues);
    }

}
