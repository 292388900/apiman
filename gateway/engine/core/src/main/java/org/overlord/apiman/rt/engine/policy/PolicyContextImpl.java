/*
 * Copyright 2013 JBoss Inc
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

package org.overlord.apiman.rt.engine.policy;

import java.util.HashMap;
import java.util.Map;

import org.overlord.apiman.rt.engine.IComponent;
import org.overlord.apiman.rt.engine.IComponentRegistry;
import org.overlord.apiman.rt.engine.beans.exceptions.ComponentNotFoundException;

/**
 * A simple implementation of a {@link IPolicyContext}.
 *
 * @author eric.wittmann@redhat.com
 */
public class PolicyContextImpl implements IPolicyContext {
    
    private final IComponentRegistry componentRegistry;
    private final Map<String, Object> conversation = new HashMap<String, Object>();
    
    /**
     * Constructor.
     * @param componentRegistry
     */
    public PolicyContextImpl(IComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    /**
     * @see org.overlord.apiman.rt.engine.policy.IPolicyContext#setAttribute(java.lang.String, java.lang.Object)
     */
    @Override
    public void setAttribute(String name, Object value) {
        conversation.put(name, value);
    }

    /**
     * @see org.overlord.apiman.rt.engine.policy.IPolicyContext#getAttribute(java.lang.String, java.lang.Object)
     */
    @Override
    public <T> T getAttribute(String name, T defaultValue) {
        @SuppressWarnings("unchecked")
        T value = (T) conversation.get(name);
        if (value == null) {
            return defaultValue;
        } else {
            return value;
        }
    }

    /**
     * @see org.overlord.apiman.rt.engine.policy.IPolicyContext#removeAttribute(java.lang.String)
     */
    @Override
    public boolean removeAttribute(String name) {
        return conversation.remove(name) != null;
    }

    /**
     * @see org.overlord.apiman.rt.engine.policy.IPolicyContext#getComponent(java.lang.Class)
     */
    @Override
    public <T extends IComponent> T getComponent(Class<T> componentClass) throws ComponentNotFoundException {
        return this.componentRegistry.getComponent(componentClass);
    }

}
