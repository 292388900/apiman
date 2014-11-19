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

package org.overlord.apiman.dt.api.rest.contract;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.overlord.apiman.dt.api.beans.actions.ActionBean;
import org.overlord.apiman.dt.api.rest.contract.exceptions.ActionException;

/**
 * The Action API.  This API allows callers to perform actions on various 
 * entities - actions other than the standard REST "crud" actions.
 * 
 * @author eric.wittmann@redhat.com
 */
@Path("actions")
public interface IActionResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void performAction(ActionBean action) throws ActionException;

}
