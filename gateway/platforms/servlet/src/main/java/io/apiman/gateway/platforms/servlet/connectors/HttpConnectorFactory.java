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
package io.apiman.gateway.platforms.servlet.connectors;

import io.apiman.gateway.engine.IConnectorFactory;
import io.apiman.gateway.engine.IServiceConnection;
import io.apiman.gateway.engine.IServiceConnectionResponse;
import io.apiman.gateway.engine.IServiceConnector;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.Service;
import io.apiman.gateway.engine.beans.ServiceRequest;
import io.apiman.gateway.engine.beans.exceptions.ConnectorException;

/**
 * Connector factory that uses HTTP to invoke back end systems.
 *
 * @author eric.wittmann@redhat.com
 */
public class HttpConnectorFactory implements IConnectorFactory {

    /**
     * Constructor.
     */
    public HttpConnectorFactory() {
    }

    /**
     * @see io.apiman.gateway.engine.IConnectorFactory#createConnector(io.apiman.gateway.engine.beans.ServiceRequest, io.apiman.gateway.engine.beans.Service)
     */
    @Override
    public IServiceConnector createConnector(ServiceRequest request, final Service service) {
        return new IServiceConnector() {
            /**
             * @see io.apiman.gateway.engine.IServiceConnector#connect(io.apiman.gateway.engine.beans.ServiceRequest, io.apiman.gateway.engine.async.IAsyncResultHandler)
             */
            @Override
            public IServiceConnection connect(ServiceRequest request,
                    IAsyncResultHandler<IServiceConnectionResponse> handler) throws ConnectorException {
                HttpServiceConnection connection = new HttpServiceConnection(request, service, handler);
                return connection;
            }
        };
    }

}
