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

/** Class wrapping claims set for access token. */
public final class AccessTokenClaimsSet extends TokenClaimsSet {

    /** Value of access token claims set type. */
    private static final String VALUE_TYPE_AT = "at";

    /** Class logger. */
    @Nonnull
    private Logger log = LoggerFactory.getLogger(AccessTokenClaimsSet.class);

    /**
     * Constructor for access token claims set when derived from authz code.
     * 
     * @param tokenClaimSet Authorize Code / Refresh Token this token is based on. Must not be NULL.
     * @param scope Validated and possibly reduced scope of the authentication request. Must not be NULL.
     * @param iat Issue time of the token. Must not be NULL.
     * @param exp Expiration time of the token. Must not be NULL.
     * @throws RuntimeException if called with non allowed null parameters
     */
    public AccessTokenClaimsSet(@Nonnull TokenClaimsSet tokenClaimSet, @Nonnull Scope scope,
            @Nullable ClaimsSet dlClaims, @Nullable ClaimsSet dlClaimsUI, @Nonnull Date iat, @Nonnull Date exp) {
        super(VALUE_TYPE_AT, tokenClaimSet.getID(), tokenClaimSet.getClientID(),
                tokenClaimSet.getClaimsSet().getIssuer(), tokenClaimSet.getPrincipal(),
                tokenClaimSet.getClaimsSet().getSubject(),
                tokenClaimSet.getACR() == null ? null : new ACR(tokenClaimSet.getACR()), iat, exp,
                tokenClaimSet.getNonce(), tokenClaimSet.getAuthenticationTime(), tokenClaimSet.getRedirectURI(), scope, tokenClaimSet.getSessionId(),
                tokenClaimSet.getClaimsRequest(), dlClaims, null, dlClaimsUI, tokenClaimSet.getConsentableClaims(),
                tokenClaimSet.getConsentedClaims());
    }

    /**
     * Constructor for access token claims set.
     * 
     * @param idGenerator Generator for pseudo unique identifier for the code. Must not be NULL.
     * @param clientID Client Id of the rp. Must not be NULL.
     * @param issuer OP issuer value. Must not be NULL.
     * @param userPrincipal User Principal of the authenticated user. Must not be NULL.
     * @param subject subject of the authenticated user. Must not be NULL
     * @param acr Authentication context class reference value of the authentication. May be NULL.
     * @param iat Issue time of the authorize code. Must not be NULL.
     * @param exp Expiration time of the authorize code. Must not be NULL.
     * @param nonce Nonce of the authentication request. May be NULL.
     * @param authTime Authentication time of the user. Must not be NULL.
     * @param redirectURI Validated redirect URI of the authentication request. Must not be NULL.
     * @param scope Scope of the authentication request. Must not be NULL.
     * @param claims Claims request of the authentication request. May be NULL.
     * @throws RuntimeException if called with nonallowed null parameters
     */
    public AccessTokenClaimsSet(@Nonnull IdentifierGenerationStrategy idGenerator, @Nonnull ClientID clientID,
            @Nonnull String issuer, @Nonnull String userPrincipal, @Nonnull String subject, @Nullable ACR acr,
            @Nonnull Date iat, @Nonnull Date exp, @Nullable Nonce nonce, @Nonnull Date authTime,
            @Nonnull URI redirectURI, @Nonnull Scope scope, @Nonnull String idpSessionId, @Nonnull ClaimsRequest claims, @Nullable ClaimsSet dlClaims,
            @Nullable ClaimsSet dlClaimsUI, JSONArray consentableClaims, JSONArray consentedClaims) {
        super(VALUE_TYPE_AT, idGenerator.generateIdentifier(), clientID, issuer, userPrincipal, subject, acr, iat, exp,
                nonce, authTime, redirectURI, scope, idpSessionId, claims, dlClaims, null, dlClaimsUI, consentableClaims,
                consentedClaims);
    }

    /**
     * Private constructor for the parser.
     * 
     * @param accessTokenClaimsSet access token claims set
     */
    private AccessTokenClaimsSet(JWTClaimsSet accessTokenClaimsSet) {
        tokenClaimsSet = accessTokenClaimsSet;
    }

    /**
     * Parses access token from string (JSON).
     * 
     * @param accessTokenClaimsSet JSON String representation of the code
     * @return AccessTokenClaimsSet instance if parsing is successful.
     * @throws ParseException if parsing fails for example due to incompatible types.
     */
    public static AccessTokenClaimsSet parse(String accessTokenClaimsSet) throws ParseException {
        JWTClaimsSet atClaimsSet = JWTClaimsSet.parse(accessTokenClaimsSet);
        // Throws exception if parsing result is not expected one.
        verifyParsedClaims(VALUE_TYPE_AT, atClaimsSet);
        return new AccessTokenClaimsSet(atClaimsSet);
    }

    /**
     * Parses access token from sealed access token.
     * 
     * @param wrappedAccessToken wrapped access token
     * @param dataSealer sealer to unwrap the access token
     * @return access token claims set.
     * @throws ParseException is thrown if unwrapped access token is not understood
     * @throws DataSealerException is thrown if unwrapping fails
     */
    public static AccessTokenClaimsSet parse(@Nonnull String wrappedAccessToken, @Nonnull DataSealer dataSealer)
            throws ParseException, DataSealerException {
        return parse(dataSealer.unwrap(wrappedAccessToken));
    }

}
