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
package org.overlord.apiman.rt.gateway.servlet.connectors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.overlord.apiman.rt.engine.async.AsyncResultImpl;
import org.overlord.apiman.rt.engine.async.IAsyncHandler;
import org.overlord.apiman.rt.engine.async.IAsyncResultHandler;
import org.overlord.apiman.rt.engine.beans.Service;
import org.overlord.apiman.rt.engine.beans.ServiceRequest;
import org.overlord.apiman.rt.engine.beans.ServiceResponse;
import org.overlord.apiman.rt.engine.beans.exceptions.ConnectorException;
import org.overlord.apiman.rt.engine.io.IBuffer;
import org.overlord.apiman.rt.engine.io.ISignalReadStream;
import org.overlord.apiman.rt.engine.io.ISignalWriteStream;
import org.overlord.apiman.rt.gateway.servlet.GatewayThreadContext;
import org.overlord.apiman.rt.gateway.servlet.io.ByteBuffer;

/**
 * Models a live connection to a back end service.
 *
 * @author eric.wittmann@redhat.com
 */
public class HttpServiceConnection implements ISignalReadStream<ServiceResponse>, ISignalWriteStream {

    private static final Set<String> SUPPRESSED_HEADERS = new HashSet<String>();
    static {
        SUPPRESSED_HEADERS.add("Transfer-Encoding"); //$NON-NLS-1$
        SUPPRESSED_HEADERS.add("Content-Length"); //$NON-NLS-1$
        SUPPRESSED_HEADERS.add("X-API-Key"); //$NON-NLS-1$
    }

    private ServiceRequest request;
    private Service service;
    private IAsyncResultHandler<ISignalReadStream<ServiceResponse>> responseHandler;
    private boolean connected;
    
    private HttpURLConnection connection;
    private OutputStream outputStream;
    
    private IAsyncHandler<IBuffer> bodyHandler;
    private IAsyncHandler<Void> endHandler;
    
    private ServiceResponse response;

    /**
     * Constructor.
     * @param request
     * @param service
     * @param handler
     * @throws ConnectorException
     */
    public HttpServiceConnection(ServiceRequest request, Service service,
            IAsyncResultHandler<ISignalReadStream<ServiceResponse>> handler) throws ConnectorException {
        this.request = request;
        this.service = service;
        this.responseHandler = handler;
        
        connect();
    }

    /**
     * Connects to the back end system.
     */
    private void connect() throws ConnectorException {
        try {
            String endpoint = service.getEndpoint();
            if (endpoint.endsWith("/")) { //$NON-NLS-1$
                endpoint = endpoint.substring(0, endpoint.length() - 1);
            }
            endpoint += request.getDestination();
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(15000);
            connection.setConnectTimeout(10000);
            if (request.getType().equalsIgnoreCase("PUT") || request.getType().equalsIgnoreCase("POST")) { //$NON-NLS-1$ //$NON-NLS-2$
                connection.setDoOutput(true);
            } else {
                connection.setDoOutput(false);
            }
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod(request.getType());

            // Set the request headers
            for (Entry<String, String> entry : request.getHeaders().entrySet()) {
                String hname = entry.getKey();
                String hval = entry.getValue();
                if (!SUPPRESSED_HEADERS.contains(hname)) {
                    connection.setRequestProperty(hname, hval);
                }
            }
            
            connection.connect();
            connected = true;
        } catch (IOException e) {
            throw new ConnectorException(e);
        }
    }

    /**
     * @see org.overlord.apiman.rt.engine.io.IReadStream#bodyHandler(org.overlord.apiman.rt.engine.async.IAsyncHandler)
     */
    @Override
    public void bodyHandler(IAsyncHandler<IBuffer> bodyHandler) {
        this.bodyHandler = bodyHandler;
    }

    /**
     * @see org.overlord.apiman.rt.engine.io.IReadStream#endHandler(org.overlord.apiman.rt.engine.async.IAsyncHandler)
     */
    @Override
    public void endHandler(IAsyncHandler<Void> endHandler) {
        this.endHandler = endHandler;
    }

    /**
     * @see org.overlord.apiman.rt.engine.io.IReadStream#getHead()
     */
    @Override
    public ServiceResponse getHead() {
        return response;
    }

    /**
     * @see org.overlord.apiman.rt.engine.io.IStream#isFinished()
     */
    @Override
    public boolean isFinished() {
        return !connected;
    }

    /**
     * @see org.overlord.apiman.rt.engine.async.IAbortable#abort()
     */
    @Override
    public void abort() {
        try {
            if (connection != null) {
                IOUtils.closeQuietly(outputStream);
                IOUtils.closeQuietly(connection.getInputStream());
                connected = false;
                connection.disconnect();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see org.overlord.apiman.rt.engine.io.IWriteStream#write(org.overlord.apiman.rt.engine.io.IBuffer)
     */
    @Override
    public void write(IBuffer chunk) {
        try {
            if (outputStream == null) {
                outputStream = connection.getOutputStream();
            }
            if (chunk instanceof ByteBuffer) {
                byte [] buffer = (byte []) chunk.getNativeBuffer();
                outputStream.write(buffer, 0, chunk.length());
            } else {
                outputStream.write(chunk.getBytes());
            }
        } catch (IOException e) {
            // TODO log this error.
            throw new RuntimeException(e);
        }
    }

    /**
     * @see org.overlord.apiman.rt.engine.io.IWriteStream#end()
     */
    @Override
    public void end() {
        try {
            IOUtils.closeQuietly(outputStream);
            outputStream = null;
            // Process the response, convert to a ServiceResponse object, and return it
            response = GatewayThreadContext.getServiceResponse();
            Map<String, List<String>> headerFields = connection.getHeaderFields();
            for (String headerName : headerFields.keySet()) {
                if (headerName != null) {
                    response.getHeaders().put(headerName, connection.getHeaderField(headerName));
                }
            }
            response.setCode(connection.getResponseCode());
            response.setMessage(connection.getResponseMessage());
            responseHandler.handle(AsyncResultImpl.<ISignalReadStream<ServiceResponse>>create(this));
        } catch (Exception e) {
            throw new ConnectorException(e);
        }
    }

    /**
     * @see org.overlord.apiman.rt.engine.io.ISignalReadStream#transmit()
     */
    @Override
    public void transmit() {
        try {
            InputStream is = connection.getInputStream();
            ByteBuffer buffer = new ByteBuffer(2048);
            int numBytes = buffer.readFrom(is);
            while (numBytes != -1) {
                bodyHandler.handle(buffer);
                numBytes = buffer.readFrom(is);
            }
            IOUtils.closeQuietly(is);
            connection.disconnect();
            connected = false;
            endHandler.handle(null);
        } catch (IOException e) {
            // TODO log this
            throw new RuntimeException(e);
        }
    }

}
