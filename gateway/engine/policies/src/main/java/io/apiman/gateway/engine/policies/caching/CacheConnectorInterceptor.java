/*
 * Copyright 2015 JBoss Inc
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
package io.apiman.gateway.engine.policies.caching;

import io.apiman.gateway.engine.IServiceConnection;
import io.apiman.gateway.engine.IServiceConnectionResponse;
import io.apiman.gateway.engine.IServiceConnector;
import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncHandler;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.ServiceRequest;
import io.apiman.gateway.engine.beans.ServiceResponse;
import io.apiman.gateway.engine.beans.exceptions.ConnectorException;
import io.apiman.gateway.engine.io.IApimanBuffer;
import io.apiman.gateway.engine.io.ISignalReadStream;
import io.apiman.gateway.engine.policy.IConnectorInterceptor;

/**
 * A connector interceptor responsible for skipping the invokation to the
 * back end API.  Instead of connecting to the real back-end, this
 * simply provides the previously-cached back-end response.
 *
 * @author eric.wittmann@redhat.com
 */
public class CacheConnectorInterceptor implements IConnectorInterceptor, IServiceConnector,
        IServiceConnection, IServiceConnectionResponse {

    private ISignalReadStream<ServiceResponse> cacheEntry;
    private IAsyncResultHandler<IServiceConnectionResponse> handler;
    private boolean finished = false;
    private boolean connected = false;

    /**
     * Constructor.
     * @param cacheEntry
     */
    public CacheConnectorInterceptor(ISignalReadStream<ServiceResponse> cacheEntry) {
        this.cacheEntry = cacheEntry;
    }

    /**
     * @see io.apiman.gateway.engine.policy.IConnectorInterceptor#createConnector()
     */
    @Override
    public IServiceConnector createConnector() {
        return this;
    }

    /**
     * @see io.apiman.gateway.engine.IServiceConnector#connect(io.apiman.gateway.engine.beans.ServiceRequest, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public IServiceConnection connect(ServiceRequest request,
            IAsyncResultHandler<IServiceConnectionResponse> handler) throws ConnectorException {
        this.handler = handler;
        this.connected = true;
        return this;
    }

    /**
     * @see io.apiman.gateway.engine.io.IWriteStream#write(io.apiman.gateway.engine.io.IApimanBuffer)
     */
    @Override
    public void write(IApimanBuffer chunk) {
        // Don't care about the payload sent.
    }

    /**
     * @see io.apiman.gateway.engine.io.IWriteStream#end()
     */
    @Override
    public void end() {
        // Called when the upload to the 'service' is complete.  This is when we
        // need to response with the connection response.
        handler.handle(AsyncResultImpl.<IServiceConnectionResponse>create(this));
    }

    /**
     * @see io.apiman.gateway.engine.io.IStream#isFinished()
     */
    @Override
    public boolean isFinished() {
        return finished;
    }

    /**
     * @see io.apiman.gateway.engine.io.IAbortable#abort()
     */
    @Override
    public void abort() {
        if (!finished) {
            finished = true;
            connected = false;
            cacheEntry.abort();
        }
    }

    /**
     * @see io.apiman.gateway.engine.IServiceConnection#isConnected()
     */
    @Override
    public boolean isConnected() {
        return connected;
    }

    /**
     * @see io.apiman.gateway.engine.io.ISignalReadStream#transmit()
     */
    @Override
    public void transmit() {
        cacheEntry.transmit();
    }

    /**
     * @see io.apiman.gateway.engine.io.IReadStream#bodyHandler(io.apiman.gateway.engine.async.IAsyncHandler)
     */
    @Override
    public void bodyHandler(final IAsyncHandler<IApimanBuffer> bodyHandler) {
        cacheEntry.bodyHandler(bodyHandler);
    }

    /**
     * @see io.apiman.gateway.engine.io.IReadStream#endHandler(io.apiman.gateway.engine.async.IAsyncHandler)
     */
    @Override
    public void endHandler(final IAsyncHandler<Void> endHandler) {
        cacheEntry.endHandler(new IAsyncHandler<Void>() {
            @Override
            public void handle(Void result) {
                endHandler.handle(result);
                connected = false;
                finished = true;
            }
        });
    }

    /**
     * @see io.apiman.gateway.engine.io.IReadStream#getHead()
     */
    @Override
    public ServiceResponse getHead() {
        return cacheEntry.getHead();
    }

}
