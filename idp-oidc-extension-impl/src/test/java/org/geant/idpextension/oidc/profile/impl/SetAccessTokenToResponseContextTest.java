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

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.security.DataSealerException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Date;
import java.util.Set;

import org.geant.idpextension.oidc.messaging.context.OIDCAuthenticationResponseConsentContext;
import org.geant.idpextension.oidc.messaging.context.OIDCAuthenticationResponseTokenClaimsContext;
import org.geant.idpextension.oidc.token.support.AccessTokenClaimsSet;
import org.geant.idpextension.oidc.token.support.AuthorizeCodeClaimsSet;
import org.geant.idpextension.oidc.token.support.TokenClaimsSet;
import org.opensaml.profile.action.EventIds;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.claims.ACR;

/** {@link SetAccessTokenToResponseContext} unit test. */
public class SetAccessTokenToResponseContextTest extends BaseOIDCResponseActionTest {

    private SetAccessTokenToResponseContext action;

    private void init() throws ComponentInitializationException, NoSuchAlgorithmException, URISyntaxException {
        respCtx.setScope(new Scope());
        TokenClaimsSet claims = new AuthorizeCodeClaimsSet(new idStrat(), new ClientID(), "issuer", "userPrin",
                "subject", new ACR("0"), new Date(), new Date(), new Nonce(), new Date(), new URI("http://example.com"),
                new Scope(), "id",null, null, null, null, null, null);
        respCtx.setSubject("subject");
        respCtx.setAuthTime(System.currentTimeMillis());
        respCtx.setTokenClaimsSet(claims);
        respCtx.setAcr("0");
        respCtx.setRedirectURI(new URI("http://example.com"));
        action = new SetAccessTokenToResponseContext(getDataSealer());
        action.initialize();
        SubjectContext subjectCtx = profileRequestCtx.getSubcontext(SubjectContext.class, true);
        SessionContext sessionCtx = profileRequestCtx.getSubcontext(SessionContext.class, true);
        sessionCtx.setIdPSession(new MockIdPSession());
        subjectCtx.setPrincipalName("userPrin");
    }

    /**
     * Basic success case.
     * 
     * @throws ComponentInitializationException
     * @throws NoSuchAlgorithmException
     * @throws URISyntaxException
     * @throws DataSealerException
     * @throws ParseException
     */
    @Test
    public void testSuccess() throws ComponentInitializationException, NoSuchAlgorithmException, URISyntaxException,
            ParseException, DataSealerException {
        init();
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNotNull(respCtx.getAccessToken());
        AccessTokenClaimsSet at = AccessTokenClaimsSet.parse(respCtx.getAccessToken().getValue(), getDataSealer());
        Assert.assertNotNull(at);
    }

    /**
     * Basic success case for non derived token.
     * 
     * @throws ComponentInitializationException
     * @throws NoSuchAlgorithmException
     * @throws URISyntaxException
     * @throws DataSealerException
     * @throws ParseException
     */
    @Test
    public void testSuccess2() throws ComponentInitializationException, NoSuchAlgorithmException, URISyntaxException,
            ParseException, DataSealerException {
        init();
        respCtx.setTokenClaimsSet(null);
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNotNull(respCtx.getAccessToken());
        AccessTokenClaimsSet at = AccessTokenClaimsSet.parse(respCtx.getAccessToken().getValue(), getDataSealer());
        Assert.assertNotNull(at);
    }

    /**
     * Basic success case for non derived token. Test for consent.
     * 
     * @throws ComponentInitializationException
     * @throws NoSuchAlgorithmException
     * @throws URISyntaxException
     * @throws DataSealerException
     * @throws ParseException
     */
    @Test
    public void testSuccess2Consent() throws ComponentInitializationException, NoSuchAlgorithmException,
            URISyntaxException, ParseException, DataSealerException {
        init();
        respCtx.setTokenClaimsSet(null);
        OIDCAuthenticationResponseConsentContext consCtx = (OIDCAuthenticationResponseConsentContext) respCtx
                .addSubcontext(new OIDCAuthenticationResponseConsentContext());
        consCtx.getConsentableAttributes().add("1");
        consCtx.getConsentableAttributes().add("2");
        consCtx.getConsentedAttributes().add("3");
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNotNull(respCtx.getAccessToken());
        AccessTokenClaimsSet at = AccessTokenClaimsSet.parse(respCtx.getAccessToken().getValue(), getDataSealer());
        Assert.assertNotNull(at);
        Assert.assertEquals(at.getConsentableClaims(), consCtx.getConsentableAttributes());
        Assert.assertEquals(at.getConsentedClaims(), consCtx.getConsentedAttributes());
    }

    /**
     * Basic success case with delivery claims
     * 
     * @throws ComponentInitializationException
     * @throws NoSuchAlgorithmException
     * @throws URISyntaxException
     * @throws DataSealerException
     * @throws ParseException
     */
    @Test
    public void testSuccessWithTokenDelivery() throws ComponentInitializationException, NoSuchAlgorithmException,
            URISyntaxException, ParseException, DataSealerException {
        init();
        OIDCAuthenticationResponseTokenClaimsContext tokenCtx = (OIDCAuthenticationResponseTokenClaimsContext) respCtx
                .addSubcontext(new OIDCAuthenticationResponseTokenClaimsContext());
        tokenCtx.getClaims().setClaim("1", "1");
        tokenCtx.getIdtokenClaims().setClaim("2", "2");
        tokenCtx.getUserinfoClaims().setClaim("3", "3");
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNotNull(respCtx.getAccessToken());
        AccessTokenClaimsSet at = AccessTokenClaimsSet.parse(respCtx.getAccessToken().getValue(), getDataSealer());
        Assert.assertNotNull(at);
        Assert.assertNotNull(at.getDeliveryClaims().getClaim("1"));
        Assert.assertNotNull(at.getUserinfoDeliveryClaims().getClaim("3"));
        Assert.assertNull(at.getIDTokenDeliveryClaims());
    }

    /**
     * fails as request is of wrong type.
     * 
     * @throws URISyntaxException
     * @throws ComponentInitializationException
     * @throws NoSuchAlgorithmException
     * 
     * 
     */
    @Test
    public void testFailNoAuthnReqCase2()
            throws NoSuchAlgorithmException, ComponentInitializationException, URISyntaxException {
        init();
        respCtx.setTokenClaimsSet(null);
        TokenRequest req =
                new TokenRequest(new URI("http://example.com"), new RefreshTokenGrant(new RefreshToken()), null);
        setTokenRequest(req);
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_MSG_CTX);
    }

    /**
     * fails as there is no subject ctx.
     * 
     * @throws URISyntaxException
     * @throws ComponentInitializationException
     * @throws NoSuchAlgorithmException
     * 
     * 
     */
    @Test
    public void testFailNoSubjectCtxCase2()
            throws NoSuchAlgorithmException, ComponentInitializationException, URISyntaxException {
        init();
        respCtx.setTokenClaimsSet(null);
        profileRequestCtx.removeSubcontext(SubjectContext.class);
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_PROFILE_CTX);
    }

    /**
     * fails as there is no rp ctx.
     * 
     * @throws URISyntaxException
     * @throws ComponentInitializationException
     * @throws NoSuchAlgorithmException
     * 
     * 
     */
    @Test
    public void testFailNoRPCtx()
            throws NoSuchAlgorithmException, ComponentInitializationException, URISyntaxException {
        init();
        profileRequestCtx.removeSubcontext(RelyingPartyContext.class);
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertEvent(event, IdPEventIds.INVALID_RELYING_PARTY_CTX);
    }

    /**
     * fails as there is no profile conf.
     * 
     * @throws URISyntaxException
     * @throws ComponentInitializationException
     * @throws NoSuchAlgorithmException
     * 
     * 
     */
    @Test
    public void testFailNoProfileConf()
            throws NoSuchAlgorithmException, ComponentInitializationException, URISyntaxException {
        init();
        RelyingPartyContext rpCtx = profileRequestCtx.getSubcontext(RelyingPartyContext.class, false);
        rpCtx.setProfileConfig(null);
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertEvent(event, IdPEventIds.INVALID_RELYING_PARTY_CTX);
    }

    /**
     * fails as the token is of wrong type.
     * 
     * @throws URISyntaxException
     * @throws ComponentInitializationException
     * @throws NoSuchAlgorithmException
     * 
     * 
     */
    @Test
    public void testFailTokenNotCodeOrRefresh()
            throws NoSuchAlgorithmException, ComponentInitializationException, URISyntaxException {
        init();
        TokenClaimsSet claims = new AccessTokenClaimsSet(new idStrat(), new ClientID(), "issuer", "userPrin", "subject",
                new ACR("0"), new Date(), new Date(), new Nonce(), new Date(), new URI("http://example.com"),
                new Scope(), "id", null, null, null, null, null);
        respCtx.setTokenClaimsSet(claims);
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_PROFILE_CTX);
    }
  
    static class MockIdPSession implements IdPSession{

        @Override
        public String getId() {
            return "id";
        }

        @Override
        public AuthenticationResult addAuthenticationResult(AuthenticationResult arg0) throws SessionException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public SPSession addSPSession(SPSession arg0) throws SessionException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean checkAddress(String arg0) throws SessionException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean checkTimeout() throws SessionException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public AuthenticationResult getAuthenticationResult(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<AuthenticationResult> getAuthenticationResults() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getCreationInstant() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public long getLastActivityInstant() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public String getPrincipalName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public SPSession getSPSession(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<SPSession> getSPSessions() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean removeAuthenticationResult(AuthenticationResult arg0) throws SessionException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean removeSPSession(SPSession arg0) throws SessionException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void updateAuthenticationResultActivity(AuthenticationResult arg0) throws SessionException {
            // TODO Auto-generated method stub
            
        }
        
    }
}