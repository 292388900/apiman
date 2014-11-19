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

package org.overlord.apiman.rt.api.rest.impl.mappers;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.overlord.apiman.rt.api.rest.contract.exceptions.NotAuthorizedException;
import org.overlord.apiman.rt.engine.beans.exceptions.AbstractEngineException;

/**
 * Provider that maps an error.
 *
 * @author eric.wittmann@redhat.com
 */
@Provider
public class RestExceptionMapper implements ExceptionMapper<AbstractEngineException> {
    
    /**
     * Constructor.
     */
    public RestExceptionMapper() {
    }
    
    /**
     * @see javax.ws.rs.ext.ExceptionMapper#toResponse(java.lang.Throwable)
     */
    @Override
    public Response toResponse(AbstractEngineException data) {
        int errorCode = 500;
        if (data instanceof NotAuthorizedException) {
            errorCode = 403;
        }
        ResponseBuilder builder = Response.status(errorCode);
        builder.type(MediaType.TEXT_PLAIN_TYPE);
        StringWriter writer = new StringWriter();
        data.printStackTrace(new PrintWriter(writer));
        return builder.entity(writer.getBuffer().toString()).build();
    }

}
