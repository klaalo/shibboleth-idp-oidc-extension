/*
 * GÉANT BSD Software License
 *
 * Copyright (c) 2017 - 2020, GÉANT
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the GÉANT nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * Disclaimer:
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.geant.idpextension.oidc.profile.impl;

import javax.annotation.Nonnull;

import org.geant.idpextension.oidc.messaging.context.OIDCClientRegistrationResponseContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformationResponse;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Action that adds an outbound {@link MessageContext} and related OIDC context
 * to the {@link ProfileRequestContext}. The {@link OIDCClientRegistrationResponseContext} is also initialized to
 * contain empty {@link OIDCClientMetadata}.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 */
@SuppressWarnings("rawtypes")
public class InitializeOutboundRegistrationResponseMessageContext extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(InitializeOutboundRegistrationResponseMessageContext.class);
    
    /** Strategy that will return or create a {@link OIDCClientRegistrationResponseContext}. */
    @Nonnull
    private Function<MessageContext, OIDCClientRegistrationResponseContext> oidcResponseContextCreationStrategy;

    /** Constructor. */
    public InitializeOutboundRegistrationResponseMessageContext() {
        super();
        oidcResponseContextCreationStrategy = 
                new ChildContextLookup<>(OIDCClientRegistrationResponseContext.class, true);
    }

    /**
     * Set the strategy used to return or create the {@link OIDCClientRegistrationResponseContext}
     * .
     * @param strategy
     *            creation strategy
     */
    public void setRelyingPartyContextCreationStrategy(
            @Nonnull final Function<MessageContext, OIDCClientRegistrationResponseContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        oidcResponseContextCreationStrategy = Constraint.isNotNull(strategy,
                "OIDCClientRegistrationResponseContext creation strategy cannot be null");
    }

    
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final MessageContext<OIDCClientInformationResponse> msgCtx = 
                new MessageContext<OIDCClientInformationResponse>();
        final OIDCClientRegistrationResponseContext oidcResponseCtx = 
                oidcResponseContextCreationStrategy.apply(msgCtx);
        oidcResponseCtx.setClientMetadata(new OIDCClientMetadata());
        profileRequestContext.setOutboundMessageContext(msgCtx);
        log.debug("{} Initialized outbound message context", getLogPrefix());
    }
}