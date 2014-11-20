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

import io.apiman.manager.api.beans.idm.UserBean;
import io.apiman.manager.ui.client.local.PageErrorPanel;
import io.apiman.manager.ui.client.local.PageLoadingWidget;
import io.apiman.manager.ui.client.local.pages.common.PageHeader;
import io.apiman.manager.ui.client.local.services.CurrentContextService;
import io.apiman.manager.ui.client.local.services.LoggerService;
import io.apiman.manager.ui.client.local.services.NavigationHelperService;
import io.apiman.manager.ui.client.local.services.RestInvokerService;
import io.apiman.manager.ui.client.local.services.rest.IRestInvokerCallback;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.jboss.errai.bus.client.api.ClientMessageBus;
import org.jboss.errai.enterprise.client.cdi.api.CDI;
import org.jboss.errai.ui.client.local.spi.TranslationService;
import org.jboss.errai.ui.nav.client.local.Navigation;
import org.jboss.errai.ui.nav.client.local.PageHiding;
import org.jboss.errai.ui.nav.client.local.PageShowing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.user.client.ui.Composite;

/**
 * Base class for all pages, includes/handles the header and footer.
 *
 * @author eric.wittmann@redhat.com
 */
public abstract class AbstractPage extends Composite {

    @Inject
    protected ClientMessageBus bus;
    @Inject
    protected PageLoadingWidget pageLoadingWidget;
    @Inject
    protected Navigation navigation;
    @Inject
    protected RestInvokerService rest;
    @Inject 
    protected NavigationHelperService navHelper;
    @Inject
    protected PageErrorPanel errorPanel;
    @Inject
    protected TranslationService i18n;
    @Inject
    protected CurrentContextService currentContext;
    @Inject
    protected LoggerService logger;
    @Inject
    private PageHeader pageHeader;

    private int expectedDataPackets;
    private int dataPacketsReceived;
    
    private static UserBean currentUserBean;

    /**
     * Constructor.
     */
    public AbstractPage() {
    }
    
    /**
     * @return the title for this page
     */
    protected abstract String getPageTitle();

    /**
     * Called after the page is constructed.
     */
    @PostConstruct
    private final void _onPostConstruct() {
    }
    
    /**
     * Called when a page is about to be shown (either when the app is first loaded or
     * when navigating TO this page from another).
     */
    @PageShowing
    private final void _onPageShowing() {
        // Do initial page loading work, but do it as a post-init task
        // of the errai bus so that all RPC endpoints are ready.  This
        // is only necessary on initial app load, but it doesn't hurt
        // to always do it.
        CDI.addPostInitTask(new Runnable() {
            @Override
            public void run() {
                doPageLoadingLifecycle();
            }
        });
        onPageShowing();
    }
    
    /**
     * Subclasses can implement onPageShowing by overriding this method.
     */
    protected void onPageShowing() {
    }

    @PageHiding
    private final void _onPageHiding() {
        pageLoadingWidget.show();
        navigation.getContentPanel().asWidget().getElement().getStyle().setVisibility(Visibility.HIDDEN);
        navigation.getContentPanel().asWidget().getElement().getStyle().setDisplay(Display.NONE);
    }

    /**
     * Execute the "page loading" lifecycle.  The lifecycle consists of the following
     * steps:
     * 
     * 1) begin data loading (pages can kick off async data fetching here)
     * 2) wait for all data to be loaded (pages must call dataLoaded() to indicate a piece of data was loaded successfully)
     * 3) render and display page
     */
    protected void doPageLoadingLifecycle() {
        logger.debug("Starting page loading lifecycle."); //$NON-NLS-1$
        onPageLoading();
        pageLoadingWidget.show();
        errorPanel.clear();
        errorPanel.hide();
        navigation.getContentPanel().asWidget().getElement().getStyle().setVisibility(Visibility.HIDDEN);
        navigation.getContentPanel().asWidget().getElement().getStyle().setDisplay(Display.NONE);
        dataPacketsReceived = 0;
        expectedDataPackets = loadPageData();
        if (expectedDataPackets == 0) {
            showPage();
        }
    }

    /**
     * Increments the number of data packets we're expecting to receive during the 
     * data load phase of page lifecycle.
     * @param additionalExpectedPackets
     */
    protected void increaseExpectedDataPackets(int additionalExpectedPackets) {
        expectedDataPackets += additionalExpectedPackets;
    }
    
    /**
     * Load the page data asynchronously.
     */
    private final int loadPageData() {
        if (currentUserBean == null) {
            rest.getCurrentUserInfo(new IRestInvokerCallback<UserBean>() {
                @Override
                public void onSuccess(UserBean response) {
                    currentUserBean = response;
                    pageHeader.setValue(currentUserBean);
                    increaseExpectedDataPackets(doLoadPageData());
                    dataPacketLoaded();
                }
                @Override
                public void onError(Throwable error) {
                    dataPacketError(error);
                }
            });
            return 1;
        } else {
            return doLoadPageData();
        }
    }
    
    /**
     * Subclasses may implement this method.  This method represents an opportunity
     * for the page to load its data asynchronously.  The method should return the
     * number of data packets it expects to receive asynchronously.  Only after all
     * of the packets have been received will the page render.  Subclasses should
     * call dataPacketLoaded() each time an asynchronous data packet is returned
     * from the server.
     */
    protected int doLoadPageData() {
        return 0;
    }

    /**
     * Called when a single piece of data that the page requires is successfully
     * loaded.  Subclasses should call this method upon return of an async data
     * call.
     */
    protected void dataPacketLoaded() {
        dataPacketsReceived++;
        logger.debug("A data packet was loaded.  Count=" + dataPacketsReceived); //$NON-NLS-1$
        if (dataPacketsReceived == expectedDataPackets) {
            showPage();
        }
    }

    /**
     * Called when an error occurs trying to load page data.
     * 
     * TODO also support a version of this with additional provided context information - sometimes we know what we were doing when a problem happened
     */
    protected void dataPacketError(Throwable t) {
        logger.error("Data packet error: " + t.getMessage()); //$NON-NLS-1$
        errorPanel.clear();
        errorPanel.displayError(t);
        pageLoadingWidget.hide();
        navigation.getContentPanel().asWidget().getElement().getStyle().setVisibility(Visibility.HIDDEN);
        navigation.getContentPanel().asWidget().getElement().getStyle().setDisplay(Display.NONE);
        errorPanel.show();
    }

    /**
     * Called after all data has been loaded.
     */
    protected void showPage() {
        logger.debug("All data packets received, showing page."); //$NON-NLS-1$
        setPageTitle(getPageTitle());
        renderPage();
        pageLoadingWidget.hide();
        navigation.getContentPanel().asWidget().getElement().getStyle().clearVisibility();
        navigation.getContentPanel().asWidget().getElement().getStyle().clearDisplay();
        GWT.runAsync(new RunAsyncCallback() {
            public void onSuccess() {
                onPageLoaded();
            }
            public void onFailure(Throwable caught) {
              // can't really fail
            }
        });
    }

    /**
     * Subclasses can implement this to do any work they need done when the page
     * is about to be shown.
     */
    protected void onPageLoading() {
    }
    
    /**
     * Called once all page data has been loaded and the page is ready to be
     * rendered.
     */
    protected void renderPage() {
        
    }
    
    /**
     * Called once all data has been loaded and the page has been rendered.
     */
    protected void onPageLoaded() {
    }
    
    /**
     * Sets the page title.
     * @param title
     */
    protected void setPageTitle(String title) {
        if (Document.get() != null && title != null) {
            Document.get().setTitle (title);
        }
    }

    /**
     * @return the currentUserBean
     */
    public UserBean getCurrentUserBean() {
        return currentUserBean;
    }

}
