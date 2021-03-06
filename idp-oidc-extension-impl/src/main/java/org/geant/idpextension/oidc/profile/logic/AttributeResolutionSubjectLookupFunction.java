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

package org.geant.idpextension.oidc.profile.logic;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minidev.json.JSONObject;
import net.shibboleth.idp.attribute.AttributeEncoder;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.geant.idpextension.oidc.attribute.encoding.impl.AbstractOIDCAttributeEncoder;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Function;
import com.google.common.base.Functions;

/**
 * A function that returns subject identifier from filtered claims. Encoded value of the first attribute that would be
 * encoded as claim sub is returned.
 */
@SuppressWarnings("rawtypes")
public class AttributeResolutionSubjectLookupFunction extends AbstractIdentifiableInitializableComponent
        implements ContextDataLookupFunction<ProfileRequestContext, String> {

    /** Class logger. */
    @Nonnull
    private Logger log = LoggerFactory.getLogger(AttributeResolutionSubjectLookupFunction.class);

    /**
     * Strategy used to locate the {@link AttributeContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nonnull
    private Function<ProfileRequestContext, AttributeContext> attributeContextLookupStrategy;

    /**
     * Name of the sub claim.
     */
    @Nonnull
    final private static String subClaimName = "sub";

    /**
     * Constructor.
     */
    AttributeResolutionSubjectLookupFunction() {
        attributeContextLookupStrategy = Functions.compose(new ChildContextLookup<>(AttributeContext.class),
                new ChildContextLookup<ProfileRequestContext, RelyingPartyContext>(RelyingPartyContext.class));
    }

    /**
     * Set the strategy used to locate the {@link AttributeContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link AttributeContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public void setAttributeContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, AttributeContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        attributeContextLookupStrategy =
                Constraint.isNotNull(strategy, "AttributeContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public String apply(@Nullable final ProfileRequestContext input) {
        AttributeContext attributeCtx = attributeContextLookupStrategy.apply(input);
        if (attributeCtx == null) {
            log.debug("No AttributeSubcontext available, nothing to do");
            return null;
        }
        for (IdPAttribute attribute : attributeCtx.getIdPAttributes().values()) {
            final Set<AttributeEncoder<?>> encoders = attribute.getEncoders();
            if (encoders.isEmpty()) {
                log.debug("Attribute {} does not have any encoders, nothing to do", attribute.getId());
                continue;
            }
            for (final AttributeEncoder<?> encoder : encoders) {
                try {
                    if (encoder instanceof AbstractOIDCAttributeEncoder
                            && subClaimName.equals(((AbstractOIDCAttributeEncoder) encoder).getName())) {
                        if (encoder.getActivationCondition() != null
                                && !encoder.getActivationCondition().apply(input)) {
                            log.debug("Encoder not active");
                            continue;
                        }
                        return (String) ((JSONObject) encoder.encode(attribute)).get(subClaimName);

                    }
                } catch (AttributeEncodingException e) {
                    log.warn("{} Unable to encode attribute {} as OIDC attribute", attribute.getId(), e);
                }
            }
        }
        return null;
    }
}