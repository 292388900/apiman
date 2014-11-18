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
package org.overlord.apiman.rt.engine.impl;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.overlord.apiman.rt.engine.async.IAsyncHandler;
import org.overlord.apiman.rt.engine.beans.PolicyFailure;
import org.overlord.apiman.rt.engine.beans.ServiceRequest;
import org.overlord.apiman.rt.engine.beans.ServiceResponse;
import org.overlord.apiman.rt.engine.io.IBuffer;
import org.overlord.apiman.rt.engine.policy.IPolicy;
import org.overlord.apiman.rt.engine.policy.IPolicyContext;
import org.overlord.apiman.rt.engine.policy.RequestChain;
import org.overlord.apiman.rt.engine.policy.ResponseChain;

/**
 * Test {@link RequestChain} and {@link ResponseChain} functionality.
 * 
 * @author Marc Savy <msavy@redhat.com>
 */
@SuppressWarnings("nls")
public class PolicyChainTest {

    private RequestChain requestChain;
    private ResponseChain responseChain;

    private ServiceRequest mockRequest;
    private ServiceResponse mockResponse;
    
    private IBuffer mockBuffer;
    private IAsyncHandler<IBuffer> mockBodyHandler;
    private IAsyncHandler<Void> mockEndHandler;
    private PassthroughPolicy policyOne;
    private PassthroughPolicy policyTwo;
    private IPolicyContext mockContext;
    private List<IPolicy> policies;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {   
        policies = new ArrayList<IPolicy>();
        policyOne = spy(new PassthroughPolicy("1")); //Refactor to use mock?
        policyTwo = spy(new PassthroughPolicy("2"));
        
        //mockChain = mock(IPolicyChain.class);
        mockContext = mock(IPolicyContext.class);
        
        mockRequest = mock(ServiceRequest.class);
        given(mockRequest.getApiKey()).willReturn("bacon"); //$NON-NLS-1$
        given(mockRequest.getDestination()).willReturn("mars"); //$NON-NLS-1$
        given(mockRequest.getType()).willReturn("request"); //$NON-NLS-1$
        
        mockResponse = mock(ServiceResponse.class);
        given(mockRequest.getApiKey()).willReturn("bacon"); //$NON-NLS-1$
        given(mockRequest.getDestination()).willReturn("mars"); //$NON-NLS-1$
        given(mockRequest.getType()).willReturn("response"); //$NON-NLS-1$
        
        mockBuffer = mock(IBuffer.class);
        given(mockBuffer.toString()).willReturn("bananas"); 
        
        mockBodyHandler = (IAsyncHandler<IBuffer>) mock(IAsyncHandler.class);
        mockEndHandler = (IAsyncHandler<Void>) mock(IAsyncHandler.class);
    }
    
    @Test
    public void shouldExecuteRequestChainTwice() {
        policies.add(policyOne);
        policies.add(policyTwo);
        
        requestChain = new RequestChain(policies, mockContext);

        requestChain.bodyHandler(mockBodyHandler);
        requestChain.endHandler(mockEndHandler);
        
        requestChain.doApply(mockRequest);
        requestChain.write(mockBuffer);
        requestChain.write(mockBuffer);
        requestChain.end();

        verify(mockBodyHandler, times(2)).handle(mockBuffer);
        verify(mockEndHandler, times(1)).handle((Void) null);
        
        InOrder order = inOrder(policyOne, policyTwo);
        order.verify(policyOne).apply(mockRequest, mockContext, requestChain);
        order.verify(policyTwo).apply(mockRequest, mockContext, requestChain);
    }
    
    @Test
    public void shouldExecuteResponseChainTwice() {
        policies.add(policyOne);
        policies.add(policyTwo);
        
        responseChain = new ResponseChain(policies, mockContext);
        
        responseChain.bodyHandler(mockBodyHandler);
        responseChain.endHandler(mockEndHandler);
        
        responseChain.doApply(mockResponse);
        responseChain.write(mockBuffer);
        responseChain.write(mockBuffer);
        responseChain.end();

        verify(mockBodyHandler, times(2)).handle(mockBuffer);
        verify(mockEndHandler, times(1)).handle((Void) null);   
        
        InOrder order = inOrder(policyTwo, policyOne);
        order.verify(policyTwo).apply(mockResponse, mockContext, responseChain);
        order.verify(policyOne).apply(mockResponse, mockContext, responseChain);
    }
    
    @Test
    public void shouldExecuteWithoutHandlers() {
        policies.add(policyOne);
        requestChain = new RequestChain(policies, mockContext);
        requestChain.doApply(mockRequest);
        requestChain.end();
    }
    
    @Test
    public void shouldPreserveBufferOrder() {
        policies.add(policyOne);
        requestChain = new RequestChain(policies, mockContext);
        requestChain.bodyHandler(mockBodyHandler);
        requestChain.endHandler(mockEndHandler);
        
        requestChain.doApply(mockRequest);
        
        IBuffer buffer1 = (IBuffer) mock(IBuffer.class);
        IBuffer buffer2 = (IBuffer) mock(IBuffer.class);
        IBuffer buffer3 = (IBuffer) mock(IBuffer.class);
        
        requestChain.write(buffer1);
        requestChain.write(buffer2);
        requestChain.write(buffer3);
        
        requestChain.end();
        
        InOrder order = inOrder(mockBodyHandler, mockEndHandler);
        order.verify(mockBodyHandler).handle(buffer1);
        order.verify(mockBodyHandler).handle(buffer2);
        order.verify(mockBodyHandler).handle(buffer3);
        order.verify(mockEndHandler).handle((Void) null);
    }
//    
//    @Test
//    public void shouldSendAbortToAllPolicies() {
//        policies.add(policyOne);
//        policies.add(policyTwo);
//        
//        requestChain = new RequestChain(policies, mockContext);
//        requestChain.bodyHandler(mockBodyHandler);
//        requestChain.endHandler(mockEndHandler);
//        
//        requestChain.doApply(mockRequest);
//
//        requestChain.abort();
//        
//        InOrder order = inOrder(policyOne, policyTwo);
//        order.verify(policyOne, times(1)).abort();
//        order.verify(policyTwo, times(1)).abort();
//    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void shouldCallFailureHandlerOnDoFail() {
        policies.add(policyOne);
        policies.add(policyTwo);
        
        requestChain = new RequestChain(policies, mockContext);
        
        IAsyncHandler<PolicyFailure> mPolicyFailureHandler = mock(IAsyncHandler.class);
        
        PolicyFailure mPolicyFailure = mock(PolicyFailure.class);
        
        requestChain.policyFailureHandler(mPolicyFailureHandler);
        
        requestChain.bodyHandler(mockBodyHandler);
        requestChain.endHandler(mockEndHandler);
        
        requestChain.doApply(mockRequest);

        requestChain.doFailure(mPolicyFailure);
        
//        InOrder order = inOrder(policyOne, policyTwo);
//        order.verify(policyOne, times(1)).abort();
//        order.verify(policyTwo, times(1)).abort();

        verify(mPolicyFailureHandler).handle(mPolicyFailure);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void shouldCallErrorHandlerOnThrowError() {
        policies.add(policyOne);
        policies.add(policyTwo);
        
        requestChain = new RequestChain(policies, mockContext);
        
        IAsyncHandler<Throwable> mThrowableFailureHandler = mock(IAsyncHandler.class);
        
        Throwable mThrowable = mock(Throwable.class);
        
        requestChain.policyErrorHandler(mThrowableFailureHandler);
        
        requestChain.bodyHandler(mockBodyHandler);
        requestChain.endHandler(mockEndHandler);
        
        requestChain.doApply(mockRequest);

        requestChain.throwError(mThrowable);
        
//        InOrder order = inOrder(policyOne, policyTwo);
//        order.verify(policyOne, times(1)).abort();
//        order.verify(policyTwo, times(1)).abort();

        verify(mThrowableFailureHandler).handle(mThrowable);
    }
}