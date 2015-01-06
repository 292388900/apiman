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
package io.apiman.manager.ui.client.local.pages.admin;

import io.apiman.manager.api.beans.idm.PermissionType;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.Templated;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.SimpleCheckBox;

/**
 * Allows the user to choose permissions for a role.
 *
 * @author eric.wittmann@redhat.com
 */
@Templated("/io/apiman/manager/ui/client/local/site/new-role.html#permissions")
public class PermissionSelector extends Composite implements HasValue<Set<PermissionType>> {
    
    @Inject @DataField
    SimpleCheckBox orgView;
    @Inject @DataField
    SimpleCheckBox orgEdit;
    @Inject @DataField
    SimpleCheckBox orgAdmin;

    @Inject @DataField
    SimpleCheckBox planView;
    @Inject @DataField
    SimpleCheckBox planEdit;
    @Inject @DataField
    SimpleCheckBox planAdmin;

    @Inject @DataField
    SimpleCheckBox svcView;
    @Inject @DataField
    SimpleCheckBox svcEdit;
    @Inject @DataField
    SimpleCheckBox svcAdmin;

    @Inject @DataField
    SimpleCheckBox appView;
    @Inject @DataField
    SimpleCheckBox appEdit;
    @Inject @DataField
    SimpleCheckBox appAdmin;

    private Set<PermissionType> value;
    
    /**
     * Constructor.
     */
    public PermissionSelector() {
    }
    
    /**
     * Bind some events.
     */
    @PostConstruct
    protected void postConstrct() {
        ClickHandler handler = new ClickHandler() {
            /**
             * @see com.google.gwt.event.dom.client.ClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
             */
            @Override
            public void onClick(ClickEvent event) {
                Set<PermissionType> permissions = new HashSet<PermissionType>();
                if (orgView.getValue()) { permissions.add(PermissionType.orgView); }
                if (orgEdit.getValue()) { permissions.add(PermissionType.orgEdit); }
                if (orgAdmin.getValue()) { permissions.add(PermissionType.orgAdmin); }

                if (planView.getValue()) { permissions.add(PermissionType.planView); }
                if (planEdit.getValue()) { permissions.add(PermissionType.planEdit); }
                if (planAdmin.getValue()) { permissions.add(PermissionType.planAdmin); }

                if (svcView.getValue()) { permissions.add(PermissionType.svcView); }
                if (svcEdit.getValue()) { permissions.add(PermissionType.svcEdit); }
                if (svcAdmin.getValue()) { permissions.add(PermissionType.svcAdmin); }

                if (appView.getValue()) { permissions.add(PermissionType.appView); }
                if (appEdit.getValue()) { permissions.add(PermissionType.appEdit); }
                if (appAdmin.getValue()) { permissions.add(PermissionType.appAdmin); }
                
                setValue(permissions, true);
            }
        };
        orgView.addClickHandler(handler);
        orgEdit.addClickHandler(handler);
        orgAdmin.addClickHandler(handler);

        planView.addClickHandler(handler);
        planEdit.addClickHandler(handler);
        planAdmin.addClickHandler(handler);

        svcView.addClickHandler(handler);
        svcEdit.addClickHandler(handler);
        svcAdmin.addClickHandler(handler);

        appView.addClickHandler(handler);
        appEdit.addClickHandler(handler);
        appAdmin.addClickHandler(handler);
    }

    /**
     * @see com.google.gwt.event.logical.shared.HasValueChangeHandlers#addValueChangeHandler(com.google.gwt.event.logical.shared.ValueChangeHandler)
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Set<PermissionType>> handler) {
        return super.addHandler(handler, ValueChangeEvent.getType());
    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#getValue()
     */
    @Override
    public Set<PermissionType> getValue() {
        return value;
    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#setValue(java.lang.Object)
     */
    @Override
    public void setValue(Set<PermissionType> value) {
        if (value == null) {
            value = new HashSet<PermissionType>();
        }
        setValue(value, false);
    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#setValue(java.lang.Object, boolean)
     */
    @Override
    public void setValue(Set<PermissionType> value, boolean fireEvents) {
        Set<PermissionType> oldValue = this.value;
        this.value = value;
        refresh();
        if (fireEvents) {
            ValueChangeEvent.fireIfNotEqual(this, oldValue, value);
        }
    }

    /**
     * Refresh the UI.
     */
    private void refresh() {
        orgView.setValue(value.contains(PermissionType.orgView));
        orgEdit.setValue(value.contains(PermissionType.orgEdit));
        orgAdmin.setValue(value.contains(PermissionType.orgAdmin));

        planView.setValue(value.contains(PermissionType.planView));
        planEdit.setValue(value.contains(PermissionType.planEdit));
        planAdmin.setValue(value.contains(PermissionType.planAdmin));

        svcView.setValue(value.contains(PermissionType.svcView));
        svcEdit.setValue(value.contains(PermissionType.svcEdit));
        svcAdmin.setValue(value.contains(PermissionType.svcAdmin));

        appView.setValue(value.contains(PermissionType.appView));
        appEdit.setValue(value.contains(PermissionType.appEdit));
        appAdmin.setValue(value.contains(PermissionType.appAdmin));
    }

}
