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
package org.overlord.apiman.rt.war.servlets;

import org.overlord.apiman.rt.engine.IEngine;
import org.overlord.apiman.rt.engine.beans.ServiceRequest;
import org.overlord.apiman.rt.engine.beans.ServiceResponse;
import org.overlord.apiman.rt.gateway.servlet.GatewayServlet;
import org.overlord.apiman.rt.war.WarGateway;

/**
 * The API Management gateway servlet.  This servlet is responsible for converting inbound
 * http servlet requests into {@link ServiceRequest}s so that they can be fed into the 
 * API Management machinery.  It also is responsible for converting the resulting 
 * {@link ServiceResponse} into an HTTP Servlet Response that is suitable for returning
 * to the caller.
 *
 * @author eric.wittmann@redhat.com
 */
public class WarGatewayServlet extends GatewayServlet {

    private static final long serialVersionUID = 958726685958622333L;
    
    /**
     * Constructor.
     */
    public WarGatewayServlet() {
    }
    
    /**
     * @see org.overlord.apiman.rt.gateway.servlet.GatewayServlet#getEngine()
     */
    @Override
    protected IEngine getEngine() {
        return WarGateway.engine;
    }

}
