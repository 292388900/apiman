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
package org.overlord.apiman.rt.test;

import org.junit.Assert;
import org.junit.Test;
import org.overlord.apiman.rt.test.policies.SimplePolicy;

/**
 * Make sure the gateway and test echo server are working.
 *
 * @author eric.wittmann@redhat.com
 */
public class SimplePolicyTest extends AbstractGatewayTest {
    
    @Test
    public void test() throws Exception {
        SimplePolicy.reset();
        runTestPlan("test-plans/simple/simple-policy-testPlan.xml"); //$NON-NLS-1$
        // This test invokes the echo service twice, so that should result in two
        // invokations of the simple policy
        Assert.assertEquals(2, SimplePolicy.inboundCallCounter);
        Assert.assertEquals(2, SimplePolicy.outboundCallCounter);
    }

}
