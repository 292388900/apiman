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

    private String writeQuery;
    private static ObjectMapper objectMapper = new ObjectMapper();

    public InfluxDb09Driver(IHttpClientComponent httpClient, String endpoint, String username, String password) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
        this.username = username;
        this.password = password;

        this.writeQuery = buildParams(new StringBuffer(endpoint + "/write"), null).toString(); //$NON-NLS-1$
    }

    /**
     * Simple write to "/write". Must be valid Influx JSON.
     * 
     * @param jsonDocument
     */
    public void write(String jsonDocument, String encoding,
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
        request.addHeader("Content-Type", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$
        request.write(jsonDocument, encoding);
        request.end();
    }

    /**
     * List all databases
     * 
     * @param handler
     */
    @SuppressWarnings("nls")
    public void listDatabases(final IAsyncResultHandler<List<String>> handler) {
        final StringBuffer url = new StringBuffer(endpoint + "/query");
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
    private StringBuffer buildParams(StringBuffer url, String query) {
        url.append("?");
        addQueryParam(url, "u", username);
        addQueryParam(url, "p", password);

        if (query != null)
            addQueryParam(url, "q", query);

        return url;
    }

    @SuppressWarnings("nls")
    private StringBuffer addQueryParam(StringBuffer url, String key, String value) {
        try {
            url.append("&" + key + "=" + URLEncoder.encode(value, "utf-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return url;
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
