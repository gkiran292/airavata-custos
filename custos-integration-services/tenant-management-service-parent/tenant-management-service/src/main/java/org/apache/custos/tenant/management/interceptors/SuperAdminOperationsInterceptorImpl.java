/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.custos.tenant.management.interceptors;

import io.grpc.Metadata;
import org.apache.custos.credential.store.client.CredentialStoreServiceClient;
import org.apache.custos.identity.client.IdentityClient;
import org.apache.custos.integration.core.exceptions.NotAuthorizedException;
import org.apache.custos.integration.services.commons.interceptors.AuthInterceptor;
import org.apache.custos.integration.services.commons.model.AuthClaim;
import org.apache.custos.tenant.management.service.Credentials;
import org.apache.custos.tenant.profile.client.async.TenantProfileClient;
import org.apache.custos.tenant.profile.service.UpdateStatusRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SuperAdminOperationsInterceptorImpl extends AuthInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthInterceptorImpl.class);

    private CredentialStoreServiceClient credentialStoreServiceClient;


    public SuperAdminOperationsInterceptorImpl(CredentialStoreServiceClient credentialStoreServiceClient,
                                               TenantProfileClient tenantProfileClient, IdentityClient identityClient) {
        super(credentialStoreServiceClient, tenantProfileClient, identityClient);
        this.credentialStoreServiceClient = credentialStoreServiceClient;
    }

    @Override
    public <ReqT> ReqT intercept(String method, Metadata headers, ReqT msg) {

        if (method.equals("updateTenantStatus")) {
            AuthClaim claim = null;
            try {
                claim = authorizeUsingUserToken(headers);
            } catch (Exception ex) {
                LOGGER.error(" Authorizing error "+ ex.getMessage());
                throw new NotAuthorizedException("Request is not authorized", ex);
            }
            if (claim == null || !claim.isSuperTenant() || !claim.isAdmin()) {
                throw new NotAuthorizedException("Request is not authorized", null);
            }

            return (ReqT) ((UpdateStatusRequest) msg).toBuilder().setUpdatedBy(claim.getPerformedBy()).build();

        } else if (method.equals("getAllTenants")) {
            AuthClaim claim = null;
            try {
                claim = authorizeUsingUserToken(headers);
            } catch (Exception ex) {
                throw new NotAuthorizedException("Request is not authorized", ex);
            }
            if (claim == null || !claim.isSuperTenant() || !claim.isAdmin()) {
                throw new NotAuthorizedException("Request is not authorized", null);
            }

            return msg;

        }

        return msg;
    }


    private Credentials getCredentials(AuthClaim claim) {
        return Credentials.newBuilder()
                .setCustosClientId(claim.getCustosId())
                .setCustosClientSecret(claim.getCustosSecret())
                .setCustosClientIdIssuedAt(claim.getCustosIdIssuedAt())
                .setCustosClientSecretExpiredAt(claim.getCustosSecretExpiredAt())
                .setIamClientId(claim.getIamAuthId())
                .setIamClientSecret(claim.getIamAuthSecret())
                .setCiLogonClientId(claim.getCiLogonId())
                .setCiLogonClientSecret(claim.getCiLogonSecret()).build();

    }
}