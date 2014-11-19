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
package org.overlord.apiman.engine.policies.auth;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.lang.text.StrSubstitutor;
import org.overlord.apiman.engine.policies.config.basicauth.LDAPIdentitySource;
import org.overlord.apiman.rt.engine.async.AsyncResultImpl;
import org.overlord.apiman.rt.engine.async.IAsyncResultHandler;
import org.overlord.apiman.rt.engine.beans.ServiceRequest;
import org.overlord.apiman.rt.engine.policy.IPolicyContext;

/**
 * An identity validator that uses the static information in the config
 * to validate the user.
 *
 * @author eric.wittmann@redhat.com
 */
public class LDAPIdentityValidator implements IIdentityValidator<LDAPIdentitySource> {
    
    /**
     * Constructor.
     */
    public LDAPIdentityValidator() {
    }

    /**
     * @see org.overlord.apiman.engine.policies.auth.IIdentityValidator#validate(java.lang.String, java.lang.String, org.overlord.apiman.rt.engine.beans.ServiceRequest, org.overlord.apiman.rt.engine.policy.IPolicyContext, java.lang.Object, org.overlord.apiman.rt.engine.async.IAsyncHandler)
     */
    @Override
    public void validate(String username, String password, ServiceRequest request, IPolicyContext context,
            LDAPIdentitySource config, IAsyncResultHandler<Boolean> handler) {
        String url = config.getUrl();
        String dn = formatDn(config.getDnPattern(), username, request);
        
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory"); //$NON-NLS-1$
        env.put(Context.PROVIDER_URL, url);

        env.put(Context.SECURITY_AUTHENTICATION, "simple"); //$NON-NLS-1$
        env.put(Context.SECURITY_PRINCIPAL, dn);
        env.put(Context.SECURITY_CREDENTIALS, password);
        try {
            new InitialDirContext(env);
            handler.handle(AsyncResultImpl.create(Boolean.TRUE));
        } catch (AuthenticationException e) {
            handler.handle(AsyncResultImpl.create(Boolean.FALSE));
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Formats the configured DN by replacing any properties it finds.
     * @param dnPattern
     * @param username
     * @param request
     */
    private String formatDn(String dnPattern, String username, ServiceRequest request) {
        Map<String, String> valuesMap = new HashMap<String, String>();
        valuesMap.putAll(request.getHeaders());
        valuesMap.put("username", username); //$NON-NLS-1$
        StrSubstitutor sub = new StrSubstitutor(valuesMap);
        return sub.replace(dnPattern);
    }

}
