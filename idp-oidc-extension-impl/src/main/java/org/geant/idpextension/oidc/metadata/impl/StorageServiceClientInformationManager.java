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

package org.geant.idpextension.oidc.metadata.impl;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.geant.idpextension.oidc.metadata.resolver.ClientInformationManager;
import org.geant.idpextension.oidc.metadata.resolver.ClientInformationManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;

/**
 * A {@link ClientInfomationManager} exploiting {@link StorageService} for storing the data.
 */
public class StorageServiceClientInformationManager extends BaseStorageServiceClientInformationComponent 
    implements ClientInformationManager {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StorageServiceClientInformationResolver.class);

    /**
     * Constructor.
     */
    public StorageServiceClientInformationManager() {
        super();
    }
    
    /** {@inheritDoc} */
    @Override
    public void storeClientInformation(final OIDCClientInformation clientInformation, final Long expiration)
            throws ClientInformationManagerException {
        log.debug("Attempting to store client information");
        final String clientId = clientInformation.getID().getValue();
        //TODO: configurable serialization
        final String serialized = clientInformation.toJSONObject().toJSONString();
        try {
            getStorageService().create(CONTEXT_NAME, clientId, serialized, expiration);
        } catch (IOException e) {
            log.error("Could not store the client information", e);
            throw new ClientInformationManagerException("Could not store the client information", e);
        }
        log.info("Successfully stored the client information for id {}", clientId);
    }

    /** {@inheritDoc} */
    @Override
    public void destroyClientInformation(ClientID clientId) {
        if (clientId == null) {
            log.warn("The null clientId cannot be destroyed, nothing to do");
            return;
        }
        try {
            getStorageService().delete(CONTEXT_NAME, clientId.getValue());
        } catch (IOException e) {
            log.error("Could not delete the client ID {}", clientId.getValue(), e);
        }
    }

}
