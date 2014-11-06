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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.ui.nav.client.local.Page;
import org.jboss.errai.ui.nav.client.local.PageShown;
import org.jboss.errai.ui.nav.client.local.TransitionTo;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.EventHandler;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.overlord.apiman.dt.api.beans.orgs.OrganizationBean;
import org.overlord.apiman.dt.api.beans.services.EndpointType;
import org.overlord.apiman.dt.api.beans.services.ServiceBean;
import org.overlord.apiman.dt.api.beans.services.ServiceVersionBean;
import org.overlord.apiman.dt.ui.client.local.AppMessages;
import org.overlord.apiman.dt.ui.client.local.beans.JavaScriptFile;
import org.overlord.apiman.dt.ui.client.local.beans.JavaScriptFile.IDataReadHandler;
import org.overlord.apiman.dt.ui.client.local.beans.ServiceImportSourceType;
import org.overlord.apiman.dt.ui.client.local.pages.service.ImportServicesTable;
import org.overlord.apiman.dt.ui.client.local.pages.service.ServiceImportSourceSelectBox;
import org.overlord.apiman.dt.ui.client.local.services.ContextKeys;
import org.overlord.apiman.dt.ui.client.local.services.rest.IRestInvokerCallback;
import org.overlord.apiman.dt.ui.client.local.util.MultimapUtil;
import org.overlord.apiman.dt.ui.client.local.widgets.DropZone;
import org.overlord.apiman.dt.ui.client.local.widgets.LocalFileChooser;
import org.overlord.commons.gwt.client.local.widgets.AsyncActionButton;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;


/**
 * Page that lets the user import a new policy definition.
 *
 * @author eric.wittmann@redhat.com
 */
@Templated("/org/overlord/apiman/dt/ui/client/local/site/import-services.html#page")
@Page(path="import-services")
@Dependent
public class ImportServicesPage extends AbstractPage {
    
    private static final List<ServiceImportSourceType> IMPORT_TYPES = new ArrayList<ServiceImportSourceType>();
    static {
        IMPORT_TYPES.add(ServiceImportSourceType.Wadl);
    }

    @Inject
    TransitionTo<OrgServicesPage> toOrgServices;
    @Inject
    TransitionTo<DashboardPage> toDashboard;

    // Import Type/Source Page
    @Inject @DataField
    ServiceImportSourceSelectBox importSource;
    @Inject @DataField
    Anchor importTypeNext;
    
    // WADL Page
    @Inject @DataField
    TextBox wadlUrl;
    @Inject @DataField
    LocalFileChooser wadlFile;
    @Inject @DataField
    DropZone wadlDropZone;
    @Inject @DataField
    AsyncActionButton wadlNext;

    // Confirmation Page
    @Inject @DataField
    CheckBox servicesSelectAll;
    @Inject @DataField
    ImportServicesTable services;
    @Inject @DataField
    AsyncActionButton yesButton;

    /**
     * Constructor.
     */
    public ImportServicesPage() {
    }

    /**
     * Post construct method.
     */
    @PostConstruct
    protected void postConstruct() {
        importSource.setOptions(IMPORT_TYPES);
        importTypeNext.getElement().removeAttribute("onclick"); //$NON-NLS-1$
        wadlNext.setEnabled(false);
        wadlUrl.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                checkValidityWadl();
            }
        });
        wadlFile.addValueChangeHandler(new ValueChangeHandler<List<JavaScriptFile>>() {
            @Override
            public void onValueChange(ValueChangeEvent<List<JavaScriptFile>> event) {
                checkValidityWadl();
            }
        });
        wadlDropZone.addValueChangeHandler(new ValueChangeHandler<List<JavaScriptFile>>() {
            @Override
            public void onValueChange(ValueChangeEvent<List<JavaScriptFile>> event) {
                checkValidityWadl();
            }
        });
        servicesSelectAll.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                if (event.getValue()) {
                    services.selectAll();
                } else {
                    services.deselectAll();
                }
            }
        });
        services.addValueChangeHandler(new ValueChangeHandler<List<ServiceVersionBean>>() {
            @Override
            public void onValueChange(ValueChangeEvent<List<ServiceVersionBean>> event) {
                servicesSelectAll.setValue(services.isAllSelected());
                yesButton.setEnabled(services.isValid());
            }
        });
    }

    /**
     * Checks for form validity on the WADL page.
     */
    protected void checkValidityWadl() {
        boolean valid = false;
        if (wadlUrl.getValue() != null && wadlUrl.getValue().trim().length() > 0) {
            valid = true;
        }
        if (!wadlFile.getValue().isEmpty()) {
            valid = true;
        }
        if (wadlDropZone.getValue().size() > 0) {
            valid = true;
        }
        wadlNext.setEnabled(valid);
    }
    
    /**
     * @see org.overlord.apiman.dt.ui.client.local.pages.AbstractPage#onPageShowing()
     */
    @Override
    protected void onPageShowing() {
        OrganizationBean org = (OrganizationBean) currentContext.getAttribute(ContextKeys.CURRENT_ORGANIZATION);
        if (org == null) {
            toDashboard.go();
        }
    }

    /**
     * Called once the page is shown.
     */
    @PageShown
    protected void onPageShown() {
    }

    /**
     * Called when the user clicks Next on the import source/type page of the wizard.
     * @param event
     */
    @EventHandler("importTypeNext")
    public void onImportTypeNext(ClickEvent event) {
        showWadlPage();
    }

    /**
     * Called when the user clicks Next on the wadl page of the wizard.
     * @param event
     */
    @EventHandler("wadlNext")
    public void onWadlNext(ClickEvent event) {
        wadlNext.onActionStarted();
        if (wadlUrl.getValue() != null && !wadlUrl.getValue().isEmpty()) {
            String proxyUrl = GWT.getHostPageBaseURL();
            if (!proxyUrl.endsWith("/")) { //$NON-NLS-1$
                proxyUrl = proxyUrl + "/"; //$NON-NLS-1$
            }
            proxyUrl = proxyUrl + "proxies/fetch"; //$NON-NLS-1$
            final String url = wadlUrl.getValue();
            RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, proxyUrl);
            builder.setHeader("X-Apiman-Url", url); //$NON-NLS-1$
            builder.setCallback(new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    if (response.getStatusCode() == 200) {
                        String wadlData = response.getText();
                        List<ServiceVersionBean> servicesToImport = new ArrayList<ServiceVersionBean>();
                        processWadl(wadlData, servicesToImport, url);
                        wadlNext.onActionComplete();
                        services.setValue(servicesToImport);
                        servicesSelectAll.setValue(Boolean.TRUE);
                        if (servicesToImport.isEmpty()) {
                            yesButton.setEnabled(false);
                        }
                        showConfirmationPage("wadlPage"); //$NON-NLS-1$
                    } else {
                        try {
                            throw new Exception(i18n.format(AppMessages.WADL_FETCH_FAIL, String.valueOf(response.getStatusCode()), response.getStatusText()));
                        } catch (Exception e) {
                            dataPacketError(e);
                        }
                    }
                }
                @Override
                public void onError(Request request, Throwable exception) {
                    dataPacketError(exception);
                }
            });
            try {
                builder.send();
            } catch (RequestException e) {
                dataPacketError(e);
            }
        } else if (!wadlFile.getValue().isEmpty() || !wadlDropZone.getValue().isEmpty()) {
            final List<JavaScriptFile> files = new ArrayList<JavaScriptFile>();
            if (!wadlFile.getValue().isEmpty()) {
                files.addAll(wadlFile.getValue());
            } else {
                files.addAll(wadlDropZone.getValue());
            }
            final List<JavaScriptFile> loadedFiles = new ArrayList<JavaScriptFile>();
            final List<ServiceVersionBean> servicesToImport = new ArrayList<ServiceVersionBean>();
            for (final JavaScriptFile file : files) {
                logger.info("Loading data from WADL: "  + file.getName()); //$NON-NLS-1$
                file.readAsText(new IDataReadHandler() {
                    @Override
                    public void onDataLoaded(String data) {
                        processWadl(data, servicesToImport, file.getName());
                        loadedFiles.add(file);
                        if (loadedFiles.size() == files.size()) {
                            wadlNext.onActionComplete();
                            services.setValue(servicesToImport);
                            servicesSelectAll.setValue(Boolean.TRUE);
                            if (servicesToImport.isEmpty()) {
                                yesButton.setEnabled(false);
                            }
                            showConfirmationPage("wadlPage"); //$NON-NLS-1$
                        }
                    }
                });
            }
        }
    }

    /**
     * Process the given WADL and add any services found therein to the services
     * list.
     * @param data
     * @param services
     * @param from
     */
    protected void processWadl(String data, List<ServiceVersionBean> services, String from) {
        try {
            Document wadlDom = XMLParser.parse(data);
            NodeList resourcesNodes = wadlDom.getElementsByTagName("resources"); //$NON-NLS-1$
            int counter = 1;
            for (int idx = 0; idx < resourcesNodes.getLength(); idx++) {
                Element resourcesNode = (Element) resourcesNodes.item(idx);
                String endpoint = resourcesNode.getAttribute("base"); //$NON-NLS-1$
                String serviceName = getServiceName(endpoint);
                if (serviceName == null) {
                    serviceName = "service-" + counter++; //$NON-NLS-1$
                }
                ServiceBean service = new ServiceBean();
                service.setName(serviceName);
                service.setDescription(i18n.format(AppMessages.SERVICE_IMPORTED_FROM, from));
                ServiceVersionBean version = new ServiceVersionBean();
                version.setVersion("1.0"); //$NON-NLS-1$
                version.setEndpoint(endpoint);
                version.setEndpointType(EndpointType.rest);
                version.setService(service);
                services.add(version);
            }
        } catch (Exception e) {
            logger.error("Error loading WADL: " + from + " :: " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * Generates a service name from the base endpoint URL.
     * @param endpoint
     */
    private String getServiceName(String endpoint) {
        try {
            int slashIdx = -1;
            for (int i = 0; i < 3; i++) {
                slashIdx = endpoint.indexOf('/', slashIdx + 1);
            }
            if (slashIdx == -1) {
                return null;
            }
            String path = endpoint.substring(slashIdx + 1);
            if (path.endsWith("/")) { //$NON-NLS-1$
                path = path.substring(0, path.length() - 1);
            }
            return path.replace('/', '-');
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Native helper method for showing the wadl page.
     */
    private native void showWadlPage() /*-{
        $wnd.jQuery("#importTypePage").animate( { "margin-left" : "-2000px" }, 200, function() { 
            $wnd.jQuery("#importTypePage").hide();
            $wnd.jQuery("#wadlPage").show();
            $wnd.jQuery("#wadlPage").animate( { "margin-left" : "0px" }, 200);
        });
    }-*/;
    
    /**
     * Native helper method for showing the confirmation page.
     */
    private native void showConfirmationPage(String fromPage) /*-{
        $wnd.jQuery("#" + fromPage).animate( { "margin-left" : "-2000px" }, 200, function() { 
            $wnd.jQuery("#" + fromPage).hide();
            $wnd.jQuery("#confirmPage").show();
            $wnd.jQuery("#confirmPage").animate( { "margin-left" : "0px" }, 200);
        });
    }-*/;

    /**
     * Called when the user clicks Yes to confirm the import.
     * @param event
     */
    @EventHandler("yesButton")
    public void onConfirm(ClickEvent event) {
        yesButton.onActionStarted();
        final OrganizationBean org = (OrganizationBean) currentContext.getAttribute(ContextKeys.CURRENT_ORGANIZATION);
        final List<ServiceVersionBean> servicesToCreate = services.getValue();
        final Set<String> completed = new HashSet<String>();
        for (final ServiceVersionBean serviceV : servicesToCreate) {
            ServiceBean service = serviceV.getService();
            service.setOrganizationId(org.getId());
            rest.createService(org.getId(), service, new IRestInvokerCallback<ServiceBean>() {
                @Override
                public void onSuccess(final ServiceBean serviceResp) {
                    rest.createServiceVersion(org.getId(), serviceResp.getId(), serviceV, new IRestInvokerCallback<ServiceVersionBean>() {
                        @Override
                        public void onSuccess(ServiceVersionBean svbResp) {
                            completed.add(serviceResp.getId());
                            if (completed.size() == servicesToCreate.size()) {
                                toOrgServices.go(MultimapUtil.singleItemMap("org", org.getId())); //$NON-NLS-1$
                            }
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
    }

    /**
     * @see org.overlord.apiman.dt.ui.client.local.pages.AbstractPage#getPageTitle()
     */
    @Override
    protected String getPageTitle() {
        return i18n.format(AppMessages.TITLE_IMPORT_SERVICES);
    }

}
