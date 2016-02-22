/*
 * Copyright 2016 JBoss Inc
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

package io.apiman.gateway.engine.jdbc;

import io.apiman.gateway.engine.IRegistry;
import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.Api;
import io.apiman.gateway.engine.beans.ApiContract;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.Client;
import io.apiman.gateway.engine.beans.Contract;
import io.apiman.gateway.engine.beans.exceptions.InvalidContractException;
import io.apiman.gateway.engine.beans.exceptions.PublishingException;
import io.apiman.gateway.engine.beans.exceptions.RegistrationException;
import io.apiman.gateway.engine.jdbc.i18n.Messages;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A JDBC implementation of the gateway registry.  Only suitable for a 
 * synchronous environment - should not be used when running an async 
 * Gateway (e.g. vert.x).
 * 
 * Must be configured with the JNDI location of the datasource to use.
 * Example:
 * 
 *     apiman-gateway.registry=io.apiman.gateway.engine.jdbc.JdbcRegistry
 *     apiman-gateway.registry.datasource.jndi-location=java:jboss/datasources/apiman-gateway
 * 
 * @author ewittman
 */
public class JdbcRegistry implements IRegistry {
    
    private static final ObjectMapper mapper = new ObjectMapper();
    private DataSource ds;

    /**
     * Constructor.
     * @param config map of configuration options
     */
    public JdbcRegistry(Map<String, String> config) {
        String dsJndiLocation = config.get("datasource.jndi-location"); //$NON-NLS-1$
        if (dsJndiLocation == null) {
            throw new RuntimeException("Missing datasource JNDI location from JdbcRegistry configuration."); //$NON-NLS-1$
        }
        ds = lookupDS(dsJndiLocation);
    }
    
    /**
     * Lookup the datasource in JNDI.
     * @param dsJndiLocation
     */
    private static DataSource lookupDS(String dsJndiLocation) {
        DataSource ds;
        try {
            InitialContext ctx = new InitialContext();
            ds = (DataSource) ctx.lookup(dsJndiLocation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (ds == null) {
            throw new RuntimeException("Datasource not found: " + dsJndiLocation); //$NON-NLS-1$
        }
        return ds;
    }

    /**
     * @see io.apiman.gateway.engine.IRegistry#publishApi(io.apiman.gateway.engine.beans.Api, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void publishApi(Api api, IAsyncResultHandler<Void> handler) {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            conn.setAutoCommit(false);
            QueryRunner run = new QueryRunner();

            // First delete any record we might already have.
            run.update(conn, "DELETE FROM apis WHERE org_id = ? AND id = ? AND version = ?",  //$NON-NLS-1$
                    api.getOrganizationId(), api.getApiId(), api.getVersion());

            // Now insert a row for the api.
            String bean = mapper.writeValueAsString(api);
            run.update(conn, "INSERT INTO apis (org_id, id, version, bean) VALUES (?, ?, ?, ?)",  //$NON-NLS-1$
                    api.getOrganizationId(), api.getApiId(), api.getVersion(), bean);

            DbUtils.commitAndClose(conn);
            handler.handle(AsyncResultImpl.create((Void) null, Void.class));
        } catch (SQLException | JsonProcessingException e) {
            handler.handle(AsyncResultImpl.create(e));
        }
    }
    
    /**
     * @see io.apiman.gateway.engine.IRegistry#registerClient(io.apiman.gateway.engine.beans.Client, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void registerClient(Client client, IAsyncResultHandler<Void> handler) {
        final Map<String, Api> apiMap = new HashMap<>();

        Connection conn = null;
        try {
            conn = ds.getConnection();
            conn.setAutoCommit(false);
            QueryRunner run = new QueryRunner();

            // Validate the client and populate the api map with apis found during validation.
            validateClient(client, apiMap, conn);

            // Remove any old data first, then (re)insert
            run.update(conn, "DELETE FROM contracts WHERE client_org_id = ? AND client_id = ? AND client_version = ?",  //$NON-NLS-1$
                    client.getOrganizationId(), client.getClientId(), client.getVersion());
            run.update(conn, "DELETE FROM clients WHERE org_id = ? AND id = ? AND version = ?",  //$NON-NLS-1$
                    client.getOrganizationId(), client.getClientId(), client.getVersion());

            String bean = mapper.writeValueAsString(client);
            run.update(conn, "INSERT INTO clients (org_id, id, version, bean) VALUES (?, ?, ?, ?)",  //$NON-NLS-1$
                    client.getOrganizationId(), client.getClientId(), client.getVersion(), bean);
            
            // Register all the api contracts.
            Set<Contract> contracts = client.getContracts();
            client.setContracts(null);
            for (Contract contract : contracts) {
                registerContract(client, contract, apiMap, conn);
            }
            
            DbUtils.commitAndClose(conn);
            handler.handle(AsyncResultImpl.create((Void) null));
        } catch (Exception re) {
            DbUtils.rollbackAndCloseQuietly(conn);
            handler.handle(AsyncResultImpl.create(re, Void.class));
        }
    }

    /**
     * Removes all of the api contracts from the database.
     * @param client
     * @param connection 
     * @throws SQLException
     */
    protected void unregisterApiContracts(Client client, Connection connection) throws SQLException {
        QueryRunner run = new QueryRunner();
        run.update(connection, "DELETE FROM contracts WHERE client_org_id = ? AND client_id = ? AND client_version = ?",  //$NON-NLS-1$
                client.getOrganizationId(), client.getClientId(), client.getVersion());
    }

    /**
     * Ensures that the api referenced by the Contract actually exists (is published).
     * @param contract
     * @param apiMap
     * @param connection
     * @throws RegistrationException
     */
    private void validateContract(final Contract contract, final Map<String, Api> apiMap, Connection connection)
            throws RegistrationException {
        QueryRunner run = new QueryRunner();
        try {
            ResultSetHandler<Api> handler = (ResultSet rs) -> {
                if (!rs.next()) {
                    return null;
                }
                Clob clob = rs.getClob(1);
                InputStream is = clob.getAsciiStream();
                try {
                    return mapper.reader(Api.class).readValue(is);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
            Api api = run.query(connection, "SELECT bean FROM apis WHERE org_id = ? AND id = ? AND version = ?", //$NON-NLS-1$
                    handler, contract.getApiOrgId(), contract.getApiId(), contract.getApiVersion());
            if (api == null) {
                String apiId = contract.getApiId();
                String orgId = contract.getApiOrgId();
                throw new RegistrationException(Messages.i18n.format("JdbcRegistry.ApiNotFoundInOrg", apiId, orgId));  //$NON-NLS-1$
            }
            api.setApiPolicies(null);
            final String id = getApiId(contract);
            apiMap.put(id, api);
        } catch (SQLException e) {
            throw new RegistrationException(Messages.i18n.format("JdbcRegistry.ErrorValidatingApp"), e); //$NON-NLS-1$
        }
    }

    /**
     * Register a single contract by inserting it into the database.
     * @param client
     * @param contracts
     * @param apiMap
     * @param connection 
     */
    private void registerContract(final Client client, final Contract contract,
            final Map<String, Api> apiMap, Connection connection) throws RegistrationException {
        QueryRunner run = new QueryRunner();
        try {
            String apiId = getApiId(contract);
            Api api = apiMap.get(apiId);
            ApiContract sc = new ApiContract(contract.getApiKey(), api, client,
                    contract.getPlan(), contract.getPolicies());
            String bean = mapper.writeValueAsString(sc);
            run.update(connection, "INSERT INTO contracts (api_key, client_org_id, client_id, client_version, bean) VALUES (?, ?, ?, ?, ?)",  //$NON-NLS-1$
                    sc.getApikey(), sc.getClient().getOrganizationId(), sc.getClient().getClientId(), sc.getClient().getVersion(), bean);
        } catch (Exception e) {
            throw new RegistrationException(Messages.i18n.format("JdbcRegistry.ErrorRegisteringContract"), e);  //$NON-NLS-1$
        }
    }

    /**
     * Validate that the client should be registered.
     * @param client
     * @param apiMap
     * @param connection
     */
    private void validateClient(Client client, Map<String, Api> apiMap, Connection connection) throws RegistrationException {
        Set<Contract> contracts = client.getContracts();
        if (contracts.isEmpty()) {
            throw new RegistrationException(Messages.i18n.format("JdbcRegistry.NoContracts")); //$NON-NLS-1$
        }
        for (Contract contract : contracts) {
            validateContract(contract, apiMap, connection);
        }
    }

    /**
     * @see io.apiman.gateway.engine.IRegistry#retireApi(io.apiman.gateway.engine.beans.Api, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void retireApi(Api api, IAsyncResultHandler<Void> handler) {
        QueryRunner run = new QueryRunner(ds);
        try {
            run.update("DELETE FROM apis WHERE org_id = ? AND id = ? AND version = ?",  //$NON-NLS-1$
                    api.getOrganizationId(), api.getApiId(), api.getVersion());
            handler.handle(AsyncResultImpl.create((Void) null, Void.class));
        } catch (SQLException e) {
            handler.handle(AsyncResultImpl.create(e));
        }
    }
    
    /**
     * @see io.apiman.gateway.engine.IRegistry#unregisterClient(io.apiman.gateway.engine.beans.Client, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void unregisterClient(Client client, IAsyncResultHandler<Void> handler) {
        Connection conn = null;
        QueryRunner run = new QueryRunner();
        try {
            conn = ds.getConnection();
            conn.setAutoCommit(false);
            run.update(conn, "DELETE FROM contracts WHERE client_org_id = ? AND client_id = ? AND client_version = ?",  //$NON-NLS-1$
                    client.getOrganizationId(), client.getClientId(), client.getVersion());
            run.update(conn, "DELETE FROM clients WHERE org_id = ? AND id = ? AND version = ?",  //$NON-NLS-1$
                    client.getOrganizationId(), client.getClientId(), client.getVersion());
            
            DbUtils.commitAndClose(conn);
            handler.handle(AsyncResultImpl.create((Void) null));
        } catch (SQLException e) {
            handler.handle(AsyncResultImpl.create(new PublishingException(Messages.i18n.format("JdbcRegistry.ErrorUnregisteringApp"), e), Void.class)); //$NON-NLS-1$
        }
    }
    
    /**
     * @see io.apiman.gateway.engine.IRegistry#getApi(java.lang.String, java.lang.String, java.lang.String, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void getApi(String organizationId, String apiId, String apiVersion,
            IAsyncResultHandler<Api> handler) {
        QueryRunner run = new QueryRunner(ds);
        try {
            Api api = getApi(organizationId, apiId, apiVersion, run);
            handler.handle(AsyncResultImpl.create(api));
        } catch (SQLException e) {
            handler.handle(AsyncResultImpl.create(e));
        }
        
    }

    /**
     * Gets an API by its orgid, id, version.  Return null if not found.
     * @param organizationId
     * @param apiId
     * @param apiVersion
     * @param run
     * @throws SQLException
     */
    private Api getApi(String organizationId, String apiId, String apiVersion, QueryRunner run)
            throws SQLException {
        return run.query("SELECT bean FROM apis WHERE org_id = ? AND id = ? AND version = ?", //$NON-NLS-1$
                Handlers.API_HANDLER, organizationId, apiId, apiVersion);
    }
    
    /**
     * @see io.apiman.gateway.engine.IRegistry#getContract(io.apiman.gateway.engine.beans.ApiRequest, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void getContract(ApiRequest request, IAsyncResultHandler<ApiContract> handler) {
        QueryRunner run = new QueryRunner(ds);

        try {
            ApiContract contract = run.query("SELECT bean FROM contracts WHERE api_key = ?", //$NON-NLS-1$
                    Handlers.API_CONTRACT_HANDLER, request.getApiKey());
            if (contract == null) {
                Exception error = new InvalidContractException(Messages.i18n.format("JdbcRegistry.NoContractForAPIKey", request.getApiKey())); //$NON-NLS-1$
                handler.handle(AsyncResultImpl.create(error, ApiContract.class));
            } else {
                checkApi(contract);
                handler.handle(AsyncResultImpl.create(contract));
            }
        } catch (Exception e) {
            handler.handle(AsyncResultImpl.create(e, ApiContract.class));
        }
    }

    /**
     * Ensure that the api still exists.  If not, it was retired.
     * @param contract
     * @throws InvalidContractException
     * @throws IOException
     */
    protected void checkApi(final ApiContract contract) throws InvalidContractException, SQLException {
        final Api api = contract.getApi();
        Api storedApi = getApi(api.getOrganizationId(), api.getApiId(), api.getVersion(), new QueryRunner(ds));
        if (storedApi == null) {
            throw new InvalidContractException(Messages.i18n.format("JdbcRegistry.ApiWasRetired", //$NON-NLS-1$
                    api.getApiId(), api.getOrganizationId()));
        }
    }

    /**
     * Generates a valid document ID for a api referenced by a contract, used to
     * retrieve the api from ES.
     * @param contract
     */
    private String getApiId(Contract contract) {
        return getApiId(contract.getApiOrgId(), contract.getApiId(), contract.getApiVersion());
    }

    /**
     * Generates a valid document ID for a api, used to index the api in ES.
     * @param orgId
     * @param apiId
     * @param version
     * @return a api key
     */
    private String getApiId(String orgId, String apiId, String version) {
        return orgId + ":" + apiId + ":" + version; //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static final class Handlers {
        public static final ResultSetHandler<Api> API_HANDLER = (ResultSet rs) -> {
            if (!rs.next()) {
                return null;
            }
            Clob clob = rs.getClob(1);
            InputStream is = clob.getAsciiStream();
            try {
                return mapper.reader(Api.class).readValue(is);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        public static final ResultSetHandler<ApiContract> API_CONTRACT_HANDLER = (ResultSet rs) -> {
            if (!rs.next()) {
                return null;
            }
            Clob clob = rs.getClob(1);
            InputStream is = clob.getAsciiStream();
            try {
                return mapper.reader(ApiContract.class).readValue(is);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

    }
    
}
