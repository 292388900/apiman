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
package org.overlord.apiman.dt.api.gateway;

import org.overlord.apiman.rt.engine.beans.Application;
import org.overlord.apiman.rt.engine.beans.Service;
import org.overlord.apiman.rt.engine.beans.exceptions.PublishingException;
import org.overlord.apiman.rt.engine.beans.exceptions.RegistrationException;

/**
 * Links the design time API with a Gateway.  This allows the design time API
 * to interface with the runtime Gateway in order to do things like publishing
 * Services and Contracts.
 *
 * @author eric.wittmann@redhat.com
 */
public interface IGatewayLink {

    /**
     * Publishes a new {@link Service}.
     * @param service the service being published
     * @throws PublishingException
     */
    public void publishService(Service service) throws PublishingException;

    /**
     * Retires (removes) a {@link Service} from the registry.
     * @param service the service to retire/remove
     * @throws PublishingException
     */
    public void retireService(Service service) throws PublishingException;
    
    /**
     * Registers a new {@link Application}.  An application is ultimately a collection of
     * contracts to managed services.
     * @param application the application being registered
     * @throws PublishingException
     */
    public void registerApplication(Application application) throws RegistrationException;

    /**
     * Removes an {@link Application} from the registry.
     * @param application the application to remove
     * @throws RegistrationException
     */
    public void unregisterApplication(Application application) throws RegistrationException;
    
}
