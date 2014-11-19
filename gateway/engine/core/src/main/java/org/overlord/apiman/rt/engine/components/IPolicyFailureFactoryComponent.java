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

package org.overlord.apiman.rt.engine.components;

import org.overlord.apiman.rt.engine.IComponent;
import org.overlord.apiman.rt.engine.beans.PolicyFailure;
import org.overlord.apiman.rt.engine.beans.PolicyFailureType;

/**
 * Component that can be used to create policy failures.
 *
 * @author eric.wittmann@redhat.com
 */
public interface IPolicyFailureFactoryComponent extends IComponent {
    
    /**
     * Creates a policy failure for the given information.
     * @param type
     * @param failureCode
     * @param message
     */
    PolicyFailure createFailure(PolicyFailureType type, int failureCode, String message);

}
