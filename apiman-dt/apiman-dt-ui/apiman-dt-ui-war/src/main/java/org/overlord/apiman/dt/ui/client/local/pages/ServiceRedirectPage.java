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

import javax.enterprise.context.Dependent;

import org.jboss.errai.ui.nav.client.local.Page;
import org.jboss.errai.ui.nav.client.local.PageState;
import org.overlord.apiman.dt.ui.client.local.util.MultimapUtil;

/**
 * The default service page.  This page is responsible for simply
 * redirecting to the proper specific service page.
 *
 * @author eric.wittmann@redhat.com
 */
@Page(path="service")
@Dependent
public class ServiceRedirectPage extends AbstractRedirectPage {
    
    @PageState
    protected String org;
    @PageState
    protected String service;
    @PageState
    protected String version;

    /**
     * Constructor.
     */
    public ServiceRedirectPage() {
    }

    /**
     * @see org.overlord.apiman.dt.ui.client.local.pages.AbstractRedirectPage#doRedirect()
     */
    @Override
    protected void doRedirect() {
        if (version == null) {
            nav.goTo(ServiceOverviewPage.class, MultimapUtil.fromMultiple("org", org, "service", service)); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            nav.goTo(ServiceOverviewPage.class, MultimapUtil.fromMultiple("org", org, "service", service, "version", version)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

}
