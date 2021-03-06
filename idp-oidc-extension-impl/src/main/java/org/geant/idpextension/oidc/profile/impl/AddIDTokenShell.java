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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import org.geant.idpextension.oidc.config.OIDCCoreProtocolConfiguration;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Function;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;

/**
 * Action that creates a {@link IDTokenClaimsSet} object shell, and sets it to work context
 * {@link OIDCAuthenticationResponseContext} located under {@link ProfileRequestContext#getOutboundMessageContext()}.
 */
@SuppressWarnings("rawtypes")
public class AddIDTokenShell extends AbstractOIDCResponseAction {

    /** Class logger. */
    @Nonnull
    private Logger log = LoggerFactory.getLogger(AddIDTokenShell.class);

    /** Strategy used to obtain the response issuer value. */
    @Nonnull
    private Function<ProfileRequestContext, String> issuerLookupStrategy;
    
    /** Strategy used to obtain the audiences to add. */
    @Nullable private Function<ProfileRequestContext,Collection<String>> audienceRestrictionsLookupStrategy;

    /** EntityID to populate into Issuer element. */
    @Nonnull
    private String issuerId;
   
    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nonnull
    private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** The RelyingPartyContext to operate on. */
    @Nullable
    private RelyingPartyContext rpCtx;
    
    /** Audiences to add. */
    @Nullable private Collection<String> audiences;

    /** Constructor. */
    public AddIDTokenShell() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
    }

    /**
     * Set the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link RelyingPartyContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the issuer value to use.
     * 
     * @param strategy lookup strategy
     */
    public void setIssuerLookupStrategy(@Nonnull final Function<ProfileRequestContext, String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        issuerLookupStrategy = Constraint.isNotNull(strategy, "IssuerLookupStrategy lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to obtain the audience restrictions to apply.
     * 
     * @param strategy lookup strategy
     */
    public void setAudienceRestrictionsLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Collection<String>> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        audienceRestrictionsLookupStrategy =
                Constraint.isNotNull(strategy, "Audience restriction lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (audienceRestrictionsLookupStrategy == null) {
            throw new ComponentInitializationException("Audience restriction lookup strategy cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        rpCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpCtx == null) {
            log.debug("{} No relying party context associated with this profile request", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return false;
        }
        issuerId = issuerLookupStrategy.apply(profileRequestContext);
        
        audiences = audienceRestrictionsLookupStrategy.apply(profileRequestContext);
        if (audiences == null || audiences.isEmpty()) {
            log.debug("{} No audiences to add, nothing to do", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return false;
        }

        return super.doPreExecute(profileRequestContext);
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        List<Audience> aud = new ArrayList<>();
        for (String audience : audiences) {
            aud.add(new Audience(audience));
        }
        Date exp = null;
        final ProfileConfiguration pc = rpCtx.getProfileConfig();
        if (pc != null && pc instanceof OIDCCoreProtocolConfiguration) {
            long lifetime = ((OIDCCoreProtocolConfiguration) pc).getIDTokenLifetime();
            exp = new Date(System.currentTimeMillis() + lifetime);
        } else {
            log.debug("{} No oidc profile configuration associated with this profile request", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return;
        }
        IDTokenClaimsSet idToken = new IDTokenClaimsSet(new Issuer(issuerId),
                new Subject(getOidcResponseContext().getSubject()), aud, exp, new Date());
        log.debug("{} Setting id token shell to response context {}", getLogPrefix(),
                idToken.toJSONObject().toJSONString());
        getOidcResponseContext().setIDToken(idToken);
    }

}