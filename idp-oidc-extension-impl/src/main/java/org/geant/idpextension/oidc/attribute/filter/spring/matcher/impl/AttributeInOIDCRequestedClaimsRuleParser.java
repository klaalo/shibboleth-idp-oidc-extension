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

package org.geant.idpextension.oidc.attribute.filter.spring.matcher.impl;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;
import net.shibboleth.idp.attribute.filter.spring.matcher.BaseAttributeValueMatcherParser;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import org.geant.idpextension.oidc.attribute.filter.matcher.impl.AttributeInOIDCRequestedClaimsMatcher;
import org.geant.idpextension.oidc.attribute.filter.spring.impl.AttributeFilterNamespaceHandler;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Bean definition parser for {@link AttributeInOIDCRequestedClaimsMatcher}.
 */
public class AttributeInOIDCRequestedClaimsRuleParser extends BaseAttributeValueMatcherParser {

    /** Schema type - afp. */
    public static final QName SCHEMA_TYPE_AFP = new QName(AttributeFilterNamespaceHandler.NAMESPACE,
            "AttributeInOIDCRequestedClaims");

    /** {@inheritDoc} */
    @Override
    protected QName getAFPName() {
        return SCHEMA_TYPE_AFP;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull
    protected Class<AttributeInOIDCRequestedClaimsMatcher> getNativeBeanClass() {
        return AttributeInOIDCRequestedClaimsMatcher.class;
    }

    /** {@inheritDoc} */
    @Override
    protected void doNativeParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, builder);

        if (config.hasAttributeNS(null, "onlyIfValuesMatch")) {
            builder.addPropertyValue("onlyIfValuesMatch",
                    StringSupport.trimOrNull(config.getAttributeNS(null, "onlyIfValuesMatch")));
        }

        if (config.hasAttributeNS(null, "onlyIfEssential")) {
            builder.addPropertyValue("onlyIfEssential",
                    StringSupport.trimOrNull(config.getAttributeNS(null, "onlyIfEssential")));
        }

        if (config.hasAttributeNS(null, "matchOnlyIDToken")) {
            builder.addPropertyValue("matchOnlyIDToken",
                    StringSupport.trimOrNull(config.getAttributeNS(null, "matchOnlyIDToken")));
        }

        if (config.hasAttributeNS(null, "matchOnlyUserInfo")) {
            builder.addPropertyValue("matchOnlyUserInfo",
                    StringSupport.trimOrNull(config.getAttributeNS(null, "matchOnlyUserInfo")));
        }

        if (config.hasAttributeNS(null, "matchIfRequestedClaimsSilent")) {
            builder.addPropertyValue("matchIfRequestedClaimsSilent",
                    StringSupport.trimOrNull(config.getAttributeNS(null, "matchIfRequestedClaimsSilent")));
        }

    }
}