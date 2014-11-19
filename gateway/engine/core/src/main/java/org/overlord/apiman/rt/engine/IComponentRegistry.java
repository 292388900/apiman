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
package org.overlord.apiman.rt.engine;

import org.overlord.apiman.rt.engine.beans.exceptions.ComponentNotFoundException;
import org.overlord.apiman.rt.engine.policy.IPolicyContext;

/**
 * A registry to discover and lookup components used by the policy implementations.  All
 * of the components in the registry are made available via the {@link IPolicyContext} for 
 * lookup by the policy implementations.  This interface is internal to the policy engine
 * and should never be made available to the policy impls.
 *
 * @author eric.wittmann@redhat.com
 */
public interface IComponentRegistry {
    
    /**
     * Gets a specific type of component from the registry.
     * @param componentType
     * @throws ComponentNotFoundException
     */
    public <T extends IComponent> T getComponent(Class<T> componentType) throws ComponentNotFoundException;

}
