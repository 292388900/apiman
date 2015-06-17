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
package io.apiman.gateway.engine.metrics.impl.influxdb;

import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncHandler;
import io.apiman.gateway.engine.async.IAsyncResult;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.components.IHttpClientComponent;
import io.apiman.gateway.engine.components.http.HttpMethod;
import io.apiman.gateway.engine.components.http.IHttpClientRequest;
import io.apiman.gateway.engine.components.http.IHttpClientResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * A simple async HTTP impl of the influxdb driver. Contains only the subset of functionality we need.
 *
 * @author Marc Savy <msavy@redhat.com>
 */
public class InfluxDb09Driver {
    private IHttpClientComponent httpClient;
    private String endpoint;
    private String username;
    private String password;
    private String database;
    private String retentionPolicy;

    private String writeQuery;
    private static ObjectMapper objectMapper = new ObjectMapper();

    public InfluxDb09Driver(IHttpClientComponent httpClient, String endpoint,
            String username, String password,
            String database,
            String retentionPolicy) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
        this.username = username;
        this.password = password;
        this.database = database;
        this.retentionPolicy = retentionPolicy;

        StringBuilder writeEndpoint = new StringBuilder();

        if (!endpoint.startsWith("http")) { //$NON-NLS-1$
            writeEndpoint.append("http"); //$NON-NLS-1$
        }

        writeEndpoint.append(endpoint + "/write"); //$NON-NLS-1$

        this.writeQuery = buildParams(writeEndpoint, null).toString();
    }

    /**
     * Simple write to "/write". Must be valid Influx line format.
     *
     * @param lineDocument document to write, as string
     * @param encoding encoding of string
     * @param failureHandler handler in case of failure
     */
    public void write(String lineDocument, String encoding,
            final IAsyncHandler<InfluxException> failureHandler) {
        // Make request to influx
        IHttpClientRequest request = httpClient.request(writeQuery, HttpMethod.POST,
                new IAsyncResultHandler<IHttpClientResponse>() {

                    @Override
                    public void handle(IAsyncResult<IHttpClientResponse> result) {
                        if (result.isError() || result.getResult().getResponseCode() != 200) {
                            failureHandler.handle(new InfluxException(result.getResult()));
                        }
                    }
                });
        request.addHeader("Content-Type", "encoding/binary"); //$NON-NLS-1$ //$NON-NLS-2$
        request.write(lineDocument, encoding);
        request.end();
    }

    /**
     * List all databases
     *
     * @param handler the result handler
     */
    @SuppressWarnings("nls")
    public void listDatabases(final IAsyncResultHandler<List<String>> handler) {
        final StringBuilder url = new StringBuilder(endpoint + "/query");
        buildParams(url, "SHOW DATABASES");

        IHttpClientRequest request = httpClient.request(url.toString(), HttpMethod.GET,
                new IAsyncResultHandler<IHttpClientResponse>() {

                    @Override
                    public void handle(IAsyncResult<IHttpClientResponse> result) {
                        try {

                            if (result.isError() || result.getResult().getResponseCode() != 200) {
                                handleError(result, handler);
                                return;
                            }

                            List<String> results = new ArrayList<>();

                            // {"results":
                            JsonNode arrNode = objectMapper.readTree(result.getResult().getBody())
                                    .path("results").getElements().next() // results: [ first-elem
                                    .path("series").getElements().next(); // series: [ first-elem
                            // values: [[db1], [db2], [...]] => db1, db2
                            flattenArrays(arrNode.get("values"), results);

                            // send results
                            handler.handle(AsyncResultImpl.<List<String>> create(results));

                        } catch (IOException e) {
                            AsyncResultImpl.create(new RuntimeException(
                                    "Unable to parse Influx JSON response", e));
                        }
                    }
                });

        request.end();
    }

    protected <T> void handleError(IAsyncResult<IHttpClientResponse> result, IAsyncResultHandler<T> handler) {
        if (result.isError()) {
            handler.handle(AsyncResultImpl.<T> create(result.getError()));
        } else if (result.getResult().getResponseCode() != 200) {
            handler.handle(AsyncResultImpl.<T> create(new RuntimeException("Influx: " //$NON-NLS-1$
                    + result.getResult().getResponseCode() + " " + result.getResult().getResponseMessage()))); //$NON-NLS-1$
        }
    }

    private void flattenArrays(JsonNode arrNode, List<String> results) {
        if (arrNode.isArray()) {
            for (JsonNode entry : arrNode) {
                flattenArrays(entry, results);
            }
        } else {
            results.add(arrNode.getTextValue());
        }
    }

    @SuppressWarnings("nls")
    private StringBuilder buildParams(StringBuilder url, String query) {
        addQueryParam(url, "db", database, "?");
        addQueryParam(url, "u", username, "&");
        addQueryParam(url, "p", password, "&");
        addQueryParam(url, "rp", retentionPolicy, "&");
        addQueryParam(url, "q", query, "&");

        return url;
    }

    @SuppressWarnings("nls")
    private void addQueryParam(StringBuilder url, String key, String value, String connector) {

        if (value == null)
            return;

        try {
            url.append(connector + key + "=" + URLEncoder.encode(value, "utf-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public final class InfluxException extends RuntimeException {
        private static final long serialVersionUID = 3481055452967828740L;
        private IHttpClientResponse response;
        private RuntimeException exception;

        public InfluxException(IHttpClientResponse r) {
            this.response = r;
        }

        public InfluxException(RuntimeException e) {
            super(e);
            this.exception = e;
        }

        public boolean isBadResponse() {
            return response != null;
        }

        public IHttpClientResponse getResponse() {
            return response;
        }

        public boolean isException() {
            return exception != null;
        }

        public RuntimeException getException() {
            return exception;
        }
    }
}
