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

package org.overlord.apiman.rt.api.rest.impl;

import org.overlord.apiman.rt.api.rest.contract.IServiceResource;
import org.overlord.apiman.rt.api.rest.contract.exceptions.NotAuthorizedException;
import org.overlord.apiman.rt.engine.beans.Service;
import org.overlord.apiman.rt.engine.beans.exceptions.PublishingException;
import org.overlord.apiman.rt.engine.beans.exceptions.RegistrationException;

/**
 * Implementation of the Service API.
 * 
 * @author eric.wittmann@redhat.com
 */
public class ServiceResourceImpl extends AbstractResourceImpl implements IServiceResource {

    /**
     * Constructor.
     */
    public ServiceResourceImpl() {
    }
    
    /**
     * @see org.overlord.apiman.rt.api.rest.contract.IServiceResource#publish(org.overlord.apiman.rt.engine.beans.Service)
     */
    @Override
    public void publish(Service service) throws PublishingException, NotAuthorizedException {
        getEngine().publishService(service);
    }
    
    /**
     * @see org.overlord.apiman.rt.api.rest.contract.IServiceResource#retire(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void retire(String organizationId, String serviceId, String version) throws RegistrationException,
            NotAuthorizedException {
        getEngine().retireService(organizationId, serviceId, version);
    }
    
}
