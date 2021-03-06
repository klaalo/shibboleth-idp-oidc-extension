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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.ACR;

import net.shibboleth.utilities.java.support.security.DataSealer;
import net.shibboleth.utilities.java.support.security.DataSealerException;
import java.text.ParseException;

/** Class wrapping claims set for refresh token. */
public final class RefreshTokenClaimsSet extends TokenClaimsSet {

    /** Value of refresh token claims set type. */
    private static final String VALUE_TYPE_RF = "rf";

    /** Class logger. */
    @Nonnull
    private Logger log = LoggerFactory.getLogger(RefreshTokenClaimsSet.class);

    /**
     * Constructor for refresh token claims set when derived from authz code.
     * 
     * @param tokenClaimsSet Authorize Code / Refresh Token this token is based on. Must not be NULL.
     * @param iat Issue time of the token. Must not be NULL.
     * @param exp Expiration time of the token. Must not be NULL.
     * @throws RuntimeException if called with non allowed null parameters
     */
    public RefreshTokenClaimsSet(@Nonnull TokenClaimsSet tokenClaimsSet, @Nonnull Date iat, @Nonnull Date exp) {
        super(VALUE_TYPE_RF, tokenClaimsSet.getID(), tokenClaimsSet.getClientID(),
                tokenClaimsSet.getClaimsSet().getIssuer(), tokenClaimsSet.getPrincipal(),
                tokenClaimsSet.getClaimsSet().getSubject(),
                tokenClaimsSet.getACR() == null ? null : new ACR(tokenClaimsSet.getACR()), iat, exp,
                tokenClaimsSet.getNonce(), tokenClaimsSet.getAuthenticationTime(), tokenClaimsSet.getRedirectURI(),
                tokenClaimsSet.getScope(), tokenClaimsSet.getSessionId(),tokenClaimsSet.getClaimsRequest(), tokenClaimsSet.getDeliveryClaims(), null,
                tokenClaimsSet.getUserinfoDeliveryClaims(), tokenClaimsSet.getConsentableClaims(),
                tokenClaimsSet.getConsentedClaims());
    }

    /**
     * Private constructor for the parser.
     * 
     * @param refreshTokenClaimsSet refresh token claims set
     */
    private RefreshTokenClaimsSet(JWTClaimsSet refreshTokenClaimsSet) {
        tokenClaimsSet = refreshTokenClaimsSet;
    }

    /**
     * Parses refresh token from string (JSON).
     * 
     * @param refreshTokenClaimsSet JSON String representation of the code
     * @return AccessTokenClaimsSet instance if parsing is successful.
     * @throws ParseException if parsing fails for example due to incompatible types.
     */
    public static RefreshTokenClaimsSet parse(String refreshTokenClaimsSet) throws ParseException {
        JWTClaimsSet atClaimsSet = JWTClaimsSet.parse(refreshTokenClaimsSet);
        // Throws exception if parsing result is not expected one.
        verifyParsedClaims(VALUE_TYPE_RF, atClaimsSet);
        return new RefreshTokenClaimsSet(atClaimsSet);
    }

    /**
     * Parses refresh token from sealed refresh token.
     * 
     * @param wrappedAccessToken wrapped refresh token
     * @param dataSealer sealer to unwrap the refresh token
     * @return refresh token claims set.
     * @throws ParseException is thrown if unwrapped refresh token is not understood
     * @throws DataSealerException is thrown if unwrapping fails
     */
    public static RefreshTokenClaimsSet parse(@Nonnull String wrappedAccessToken, @Nonnull DataSealer dataSealer)
            throws ParseException, DataSealerException {
        return parse(dataSealer.unwrap(wrappedAccessToken));
    }

}
