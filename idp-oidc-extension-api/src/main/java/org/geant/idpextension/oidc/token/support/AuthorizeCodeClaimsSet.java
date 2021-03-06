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

package org.geant.idpextension.oidc.token.support;

import java.util.Date;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.ClaimsRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.claims.ACR;
import com.nimbusds.openid.connect.sdk.claims.ClaimsSet;

import net.minidev.json.JSONArray;
import net.shibboleth.utilities.java.support.security.DataSealer;
import net.shibboleth.utilities.java.support.security.DataSealerException;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;
import java.net.URI;
import java.text.ParseException;

/** Class wrapping claims set for authorize code. */
public final class AuthorizeCodeClaimsSet extends TokenClaimsSet {

    /** Value of authorize code claims set type. */
    private static final String VALUE_TYPE_AC = "ac";

    /** Class logger. */
    @Nonnull
    private Logger log = LoggerFactory.getLogger(AuthorizeCodeClaimsSet.class);

    /**
     * Constructor for authorize code claims set.
     * 
     * @param idGenerator Generator for pseudo unique identifier for the code. Must not be NULL.
     * @param clientID Client Id of the rp. Must not be NULL.
     * @param issuer OP issuer value. Must not be NULL.
     * @param userPrincipal User Principal of the authenticated user. Must not be NULL.
     * @param subject subject of the authenticated user. Must not be NULL
     * @param acr Authentication context class reference value of the authentication. Must not be NULL.
     * @param iat Issue time of the authorize code. Must not be NULL.
     * @param exp Expiration time of the authorize code. Must not be NULL.
     * @param nonce Nonce of the authentication request. May be NULL.
     * @param authTime Authentication time of the user. Must not be NULL.
     * @param redirectURI Validated redirect URI of the authentication request. Must not be NULL.
     * @param scope Scope of the authentication request. Must not be NULL.
     * @param claims Claims request of the authentication request. May be NULL.
     * @throws RuntimeException if called with nonallowed null parameters
     */
    public AuthorizeCodeClaimsSet(@Nonnull IdentifierGenerationStrategy idGenerator, @Nonnull ClientID clientID,
            @Nonnull String issuer, @Nonnull String userPrincipal, @Nonnull String subject, @Nonnull ACR acr,
            @Nonnull Date iat, @Nonnull Date exp, @Nullable Nonce nonce, @Nonnull Date authTime,
            @Nonnull URI redirectURI, @Nonnull Scope scope, @Nonnull String idpSessionId, @Nullable ClaimsRequest claims, @Nullable ClaimsSet dlClaims,
            @Nullable ClaimsSet dlClaimsID, @Nullable ClaimsSet dlClaimsUI, JSONArray consentableClaims,
            JSONArray consentedClaims) {
        super(VALUE_TYPE_AC, idGenerator.generateIdentifier(), clientID, issuer, userPrincipal, subject, acr, iat, exp,
                nonce, authTime, redirectURI, scope, idpSessionId, claims, dlClaims, dlClaimsID, dlClaimsUI, consentableClaims,
                consentedClaims);
    }

    /**
     * Private constructor for the parser.
     * 
     * @param authzCodeClaimsSet authorize code claims set
     */
    private AuthorizeCodeClaimsSet(JWTClaimsSet authzCodeClaimsSet) {
        tokenClaimsSet = authzCodeClaimsSet;
    }

    /**
     * Parses authz code from string (JSON).
     * 
     * @param authorizeCodeClaimsSet JSON String representation of the code
     * @return AuthorizeCodeClaimsSet instance if parsing is successful.
     * @throws ParseException if parsing fails for example due to incompatible types.
     */
    public static AuthorizeCodeClaimsSet parse(String authorizeCodeClaimsSet) throws ParseException {
        JWTClaimsSet acClaimsSet = JWTClaimsSet.parse(authorizeCodeClaimsSet);
        // Throws exception if parsing result is not expected one.
        verifyParsedClaims(VALUE_TYPE_AC, acClaimsSet);
        return new AuthorizeCodeClaimsSet(acClaimsSet);
    }

    /**
     * Parses authz code from sealed authorization code.
     * 
     * @param wrappedAuthCode wrapped code
     * @param dataSealer sealer to unwrap the code
     * @return authorize code
     * @throws ParseException is thrown if unwrapped code is not understood
     * @throws DataSealerException is thrown if unwrapping fails
     */
    public static AuthorizeCodeClaimsSet parse(@Nonnull String wrappedAuthCode, @Nonnull DataSealer dataSealer)
            throws ParseException, DataSealerException {
        return parse(dataSealer.unwrap(wrappedAuthCode));
    }

}
