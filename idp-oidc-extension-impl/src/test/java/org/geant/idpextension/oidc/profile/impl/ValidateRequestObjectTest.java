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

import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

import org.geant.idpextension.oidc.messaging.context.OIDCMetadataContext;
import org.geant.idpextension.oidc.profile.OidcEventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;

import junit.framework.Assert;

/** {@link ValidateUserPresence} unit test. */
public class ValidateRequestObjectTest extends BaseOIDCResponseActionTest {

    private ValidateRequestObject action;

    private OIDCMetadataContext oidcCtx;

    @BeforeTest
    public void init() throws ComponentInitializationException, URISyntaxException {
        action = new ValidateRequestObject();
        action.initialize();
    }

    @SuppressWarnings("rawtypes")
    public void initClientMetadata() {
        final ProfileRequestContext prc = new WebflowRequestContextProfileRequestContextLookup().apply(requestCtx);
        oidcCtx = prc.getInboundMessageContext().getSubcontext(OIDCMetadataContext.class, true);
        OIDCClientMetadata metaData = new OIDCClientMetadata();
        OIDCClientInformation information = new OIDCClientInformation(new ClientID("test"), null, metaData,
                new Secret("ultimatetopsecretultimatetopsecret"), null, null);
        oidcCtx.setClientInformation(information);
    }

    /**
     * Test that success in case of not having request object
     */
    @Test
    public void testSuccessNoObject()
            throws NoSuchAlgorithmException, ComponentInitializationException, URISyntaxException {
        initClientMetadata();
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertProceedEvent(event);
    }

    /**
     * Test success in case of having non signed request object and no registered algorithm
     */
    @Test
    public void testRequestObjectNoMatchingClaims()
            throws NoSuchAlgorithmException, ComponentInitializationException, URISyntaxException {
        initClientMetadata();
        JWTClaimsSet ro = new JWTClaimsSet.Builder().subject("alice").build();
        AuthenticationRequest req = new AuthenticationRequest.Builder(new ResponseType("code"), new Scope("openid"),
                new ClientID("000123"), URI.create("https://example.com/callback")).requestObject(new PlainJWT(ro))
                        .state(new State()).build();
        setAuthenticationRequest(req);
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertProceedEvent(event);
    }

    /**
     * Test failure case of having non signed request object and registered algorithm other than 'none'
     */
    @Test
    public void testRequestObjectAlgMismatch()
            throws NoSuchAlgorithmException, ComponentInitializationException, URISyntaxException {
        initClientMetadata();
        oidcCtx.getClientInformation().getOIDCMetadata().setRequestObjectJWSAlg(JWSAlgorithm.RS256);
        JWTClaimsSet ro = new JWTClaimsSet.Builder().subject("alice").build();
        AuthenticationRequest req = new AuthenticationRequest.Builder(new ResponseType("code"), new Scope("openid"),
                new ClientID("000123"), URI.create("https://example.com/callback")).requestObject(new PlainJWT(ro))
                        .state(new State()).build();
        setAuthenticationRequest(req);
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertEvent(event, OidcEventIds.INVALID_REQUEST_OBJECT);
    }

    /**
     * Test success case of having non signed request object and registered algorithm 'none'
     */
    @Test
    public void testRequestObjectAlgMatch()
            throws NoSuchAlgorithmException, ComponentInitializationException, URISyntaxException {
        initClientMetadata();
        oidcCtx.getClientInformation().getOIDCMetadata().setRequestObjectJWSAlg(new JWSAlgorithm("none"));
        JWTClaimsSet ro = new JWTClaimsSet.Builder().subject("alice").build();
        AuthenticationRequest req = new AuthenticationRequest.Builder(new ResponseType("code"), new Scope("openid"),
                new ClientID("000123"), URI.create("https://example.com/callback")).requestObject(new PlainJWT(ro))
                        .state(new State()).build();
        setAuthenticationRequest(req);
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertProceedEvent(event);
    }

    /**
     * Test failure case of mismatch in client_id values
     */
    @Test
    public void testRequestObjectClientMismatch()
            throws NoSuchAlgorithmException, ComponentInitializationException, URISyntaxException {
        initClientMetadata();
        JWTClaimsSet ro = new JWTClaimsSet.Builder().claim("client_id", "not_matching").build();
        AuthenticationRequest req = new AuthenticationRequest.Builder(new ResponseType("code"), new Scope("openid"),
                new ClientID("000123"), URI.create("https://example.com/callback")).requestObject(new PlainJWT(ro))
                        .state(new State()).build();
        setAuthenticationRequest(req);
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertEvent(event, OidcEventIds.INVALID_REQUEST_OBJECT);
    }

    /**
     * Test failure in case of mismatch in response_type values
     */
    @Test
    public void testRequestObjectRespTypeMismatch()
            throws NoSuchAlgorithmException, ComponentInitializationException, URISyntaxException {
        initClientMetadata();
        JWTClaimsSet ro = new JWTClaimsSet.Builder().claim("response_type", "id_token").build();
        AuthenticationRequest req = new AuthenticationRequest.Builder(new ResponseType("code"), new Scope("openid"),
                new ClientID("000123"), URI.create("https://example.com/callback")).requestObject(new PlainJWT(ro))
                        .state(new State()).build();
        setAuthenticationRequest(req);
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertEvent(event, OidcEventIds.INVALID_REQUEST_OBJECT);
    }

    /**
     * Test success in case of matching client_id and response_type values
     */
    @Test
    public void testRequestObjectClientRespTypeMatch()
            throws NoSuchAlgorithmException, ComponentInitializationException, URISyntaxException {
        initClientMetadata();
        JWTClaimsSet ro =
                new JWTClaimsSet.Builder().claim("client_id", "000123").claim("response_type", "code token").build();
        ResponseType rt = new ResponseType();
        rt.add(ResponseType.Value.CODE);
        rt.add(ResponseType.Value.TOKEN);
        AuthenticationRequest req = new AuthenticationRequest.Builder(rt, new Scope("openid"), new ClientID("000123"),
                URI.create("https://example.com/callback")).requestObject(new PlainJWT(ro)).nonce(new Nonce())
                        .state(new State()).build();
        setAuthenticationRequest(req);
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertProceedEvent(event);
    }

    /**
     * Test success case of RSA signed request object.
     */
    @Test
    public void testRequestObjectSignedWithRSA() throws NoSuchAlgorithmException, ComponentInitializationException,
            URISyntaxException, JOSEException, InvalidAlgorithmParameterException {
        initClientMetadata();
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        JWKSet jwkSet = generateKeySet(new RSAKey((RSAPublicKey) kp.getPublic(), (RSAPrivateKey) kp.getPrivate(), null,
                null, JWSAlgorithm.RS256, "kid", null, null, null, null, null));
        oidcCtx.getClientInformation().getOIDCMetadata().setJWKSet(jwkSet);
        JWSSigner signer = new RSASSASigner(kp.getPrivate());
        JWTClaimsSet ro = new JWTClaimsSet.Builder().subject("alice").build();
        SignedJWT signed = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), ro);
        signed.sign(signer);
        AuthenticationRequest req =
                new AuthenticationRequest.Builder(new ResponseType("code"), new Scope("openid"), new ClientID("000123"),
                        URI.create("https://example.com/callback")).requestObject(signed).state(new State()).build();
        setAuthenticationRequest(req);
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertProceedEvent(event);
    }

    /**
     * Test success case of EC signed request object.
     */
    @Test
    public void testRequestObjectSignedWithEC() throws NoSuchAlgorithmException, ComponentInitializationException,
            URISyntaxException, JOSEException, InvalidAlgorithmParameterException {
        initClientMetadata();
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        KeyPair kp = kpg.generateKeyPair();
        kpg.initialize(Curve.P_256.toECParameterSpec());
        JWKSet jwkSet = generateKeySet(new ECKey(Curve.P_256, (ECPublicKey) kp.getPublic(),
                (ECPrivateKey) kp.getPrivate(), null, null, JWSAlgorithm.ES256, null, null, null, null, null, null));
        oidcCtx.getClientInformation().getOIDCMetadata().setJWKSet(jwkSet);
        JWSSigner signer = new ECDSASigner((ECPrivateKey) kp.getPrivate());
        JWTClaimsSet ro = new JWTClaimsSet.Builder().subject("alice").build();
        SignedJWT signed = new SignedJWT(new JWSHeader(JWSAlgorithm.ES256), ro);
        signed.sign(signer);
        AuthenticationRequest req =
                new AuthenticationRequest.Builder(new ResponseType("code"), new Scope("openid"), new ClientID("000123"),
                        URI.create("https://example.com/callback")).requestObject(signed).state(new State()).build();
        setAuthenticationRequest(req);
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertProceedEvent(event);
    }

    /**
     * Test fail case of EC signed request object, no key
     */
    @Test
    public void testRequestObjectSignedWithECFailNoKey() throws NoSuchAlgorithmException,
            ComponentInitializationException, URISyntaxException, JOSEException, InvalidAlgorithmParameterException {
        initClientMetadata();
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        KeyPair kp = kpg.generateKeyPair();
        kpg.initialize(Curve.P_256.toECParameterSpec());
        JWKSet jwkSet = generateKeySet(null);
        oidcCtx.getClientInformation().getOIDCMetadata().setJWKSet(jwkSet);
        JWSSigner signer = new ECDSASigner((ECPrivateKey) kp.getPrivate());
        JWTClaimsSet ro = new JWTClaimsSet.Builder().subject("alice").build();
        SignedJWT signed = new SignedJWT(new JWSHeader(JWSAlgorithm.ES256), ro);
        signed.sign(signer);
        AuthenticationRequest req =
                new AuthenticationRequest.Builder(new ResponseType("code"), new Scope("openid"), new ClientID("000123"),
                        URI.create("https://example.com/callback")).requestObject(signed).state(new State()).build();
        setAuthenticationRequest(req);
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertEvent(event, OidcEventIds.INVALID_REQUEST_OBJECT);
    }

    /**
     * Test fail case of EC signed request object, no key set
     */
    @Test
    public void testRequestObjectSignedWithECFailNoKeySet() throws NoSuchAlgorithmException,
            ComponentInitializationException, URISyntaxException, JOSEException, InvalidAlgorithmParameterException {
        initClientMetadata();
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        KeyPair kp = kpg.generateKeyPair();
        kpg.initialize(Curve.P_256.toECParameterSpec());
        JWSSigner signer = new ECDSASigner((ECPrivateKey) kp.getPrivate());
        JWTClaimsSet ro = new JWTClaimsSet.Builder().subject("alice").build();
        SignedJWT signed = new SignedJWT(new JWSHeader(JWSAlgorithm.ES256), ro);
        signed.sign(signer);
        AuthenticationRequest req =
                new AuthenticationRequest.Builder(new ResponseType("code"), new Scope("openid"), new ClientID("000123"),
                        URI.create("https://example.com/callback")).requestObject(signed).state(new State()).build();
        setAuthenticationRequest(req);
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertEvent(event, OidcEventIds.INVALID_REQUEST_OBJECT);
    }

    /**
     * Test success case of HS signed request object.
     */
    @Test
    public void testRequestObjectSignedWithHS() throws NoSuchAlgorithmException, ComponentInitializationException,
            URISyntaxException, JOSEException, InvalidAlgorithmParameterException {
        initClientMetadata();
        String secret = oidcCtx.getClientInformation().getSecret().getValue();
        JWSSigner signer = new MACSigner(secret);
        JWTClaimsSet ro = new JWTClaimsSet.Builder().subject("alice").build();
        SignedJWT signed = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), ro);
        signed.sign(signer);
        AuthenticationRequest req =
                new AuthenticationRequest.Builder(new ResponseType("code"), new Scope("openid"), new ClientID("000123"),
                        URI.create("https://example.com/callback")).requestObject(signed).state(new State()).build();
        setAuthenticationRequest(req);
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertProceedEvent(event);
    }

    /**
     * Test failure in case of HS signed request object.
     */
    @Test
    public void testRequestObjectSignedWithHSFail() throws NoSuchAlgorithmException, ComponentInitializationException,
            URISyntaxException, JOSEException, InvalidAlgorithmParameterException {
        initClientMetadata();
        String secret = oidcCtx.getClientInformation().getSecret().getValue() + "_not";
        JWSSigner signer = new MACSigner(secret);
        JWTClaimsSet ro = new JWTClaimsSet.Builder().subject("alice").build();
        SignedJWT signed = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), ro);
        signed.sign(signer);
        AuthenticationRequest req =
                new AuthenticationRequest.Builder(new ResponseType("code"), new Scope("openid"), new ClientID("000123"),
                        URI.create("https://example.com/callback")).requestObject(signed).state(new State()).build();
        setAuthenticationRequest(req);
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertEvent(event, OidcEventIds.INVALID_REQUEST_OBJECT);
    }

    /**
     * Helper generating keyset with prefilled keys + the input key
     * 
     * @throws InvalidAlgorithmParameterException
     */
    private JWKSet generateKeySet(JWK key) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        List<JWK> keys = new ArrayList<JWK>();
        if (key != null) {
            keys.add(key);
        }
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        keys.add(new RSAKey((RSAPublicKey) kp.getPublic(), (RSAPrivateKey) kp.getPrivate(), null, null,
                JWSAlgorithm.RS256, null, null, null, null, null, null));
        keys.add(new RSAKey((RSAPublicKey) kp.getPublic(), (RSAPrivateKey) kp.getPrivate(), null, null,
                JWSAlgorithm.RS256, null, null, null, null, null, null));
        keys.add(new RSAKey((RSAPublicKey) kp.getPublic(), (RSAPrivateKey) kp.getPrivate(), null, null,
                JWSAlgorithm.RS384, null, null, null, null, null, null));
        keys.add(new RSAKey((RSAPublicKey) kp.getPublic(), (RSAPrivateKey) kp.getPrivate(), null, null,
                JWSAlgorithm.RS512, null, null, null, null, null, null));
        kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(Curve.P_256.toECParameterSpec());
        kp = kpg.generateKeyPair();
        keys.add(new ECKey(Curve.P_256, (ECPublicKey) kp.getPublic(), (ECPrivateKey) kp.getPrivate(), null, null,
                JWSAlgorithm.ES256, null, null, null, null, null, null));
        keys.add(new ECKey(Curve.P_256, (ECPublicKey) kp.getPublic(), (ECPrivateKey) kp.getPrivate(), null, null,
                JWSAlgorithm.ES256, null, null, null, null, null, null));
        keys.add(new ECKey(Curve.P_256, (ECPublicKey) kp.getPublic(), (ECPrivateKey) kp.getPrivate(), null, null,
                JWSAlgorithm.ES256, null, null, null, null, null, null));
        keys.add(new ECKey(Curve.P_256, (ECPublicKey) kp.getPublic(), (ECPrivateKey) kp.getPrivate(), null, null,
                JWSAlgorithm.ES256, null, null, null, null, null, null));
        return new JWKSet(keys);
    }

}