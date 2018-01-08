/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vmware.idm.samples.oauth2;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Validates an Access Token issued by VMware Identity Manager and populates user's logged-in information (by reading the access token content).
 * <p>
 * It will validate either locally or using the API on Identity Manager.
 */
public class VMwareValidateTokenServices implements ResourceServerTokenServices {

    private static Logger logger = Logger.getLogger(VMwareValidateTokenServices.class.getSimpleName());

    private static final String ISSUER_KEY = "iss";
    private static final String ISSUED_AT_KEY = "iat";
    public static final long ALLOWED_SKEW_IN_MS = 1000;

    private URI validateTokenUrl;
    private boolean validateLocally = true;
    private RestOperations restTemplate;
    private JwtTokenStore tokenStore;
    private final String expectedIssuer;

    public VMwareValidateTokenServices(String validateTokenUrl, JwtTokenStore tokenStore) throws URISyntaxException {
        this.validateTokenUrl = new URI(validateTokenUrl);
        this.tokenStore = tokenStore;
        this.expectedIssuer = inferExpectedIssuer(validateTokenUrl);
        this.restTemplate = new RestTemplate();
    }

    /**
     * Calculate the expected issuer URL from the given token URL.
     * (we could hard-code the expected URL as well, but for demo purposes, it is easier to infer it).
     */
    private String inferExpectedIssuer(String validateTokenUrl) {
        int indexOfSaas = validateTokenUrl.indexOf("/SAAS");
        if (indexOfSaas < 0) {
            throw new IllegalArgumentException("Can not infer expected issuer URL from validation URL: " + validateTokenUrl);
        }
        return validateTokenUrl.substring(0, indexOfSaas) + "/SAAS/auth";
    }

    @Override
    public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
        // This call is already checking for the signature and some basic expiration dates
        OAuth2AccessToken oAuth2AccessToken = tokenStore.readAccessToken(accessToken);
        if (oAuth2AccessToken == null) {
            throw new InvalidTokenException("Invalid access token: " + accessToken);
        } else if (oAuth2AccessToken.isExpired()) {
            throw new InvalidTokenException("Access token expired: " + oAuth2AccessToken.getExpiration());
        }

        if (validateLocally) {
            validateAccessTokenLocally(oAuth2AccessToken);
        } else {
            validateAccessToken(accessToken);
        }

        OAuth2Authentication result = tokenStore.readAuthentication(accessToken);
        if (result == null) {
            // in case of race condition
            throw new InvalidTokenException("Invalid access token, could not extract authentication information: " + accessToken);
        }
        return result;
    }

    /**
     * Check the access token locally by validating additional information.
     * will throw {@link InvalidTokenException} on errors.
     *
     * @param accessToken the decoded access token used to access this resource server APIs
     */
    private void validateAccessTokenLocally(OAuth2AccessToken accessToken) {
        logger.info("Validate access token locally: " + accessToken.getAdditionalInformation());

        long now = new Date().getTime() / 1000;
        Map<String, Object> map = accessToken.getAdditionalInformation();

        // check issuer
        String issuer = (String) map.get(ISSUER_KEY);
        if (issuer == null || !issuer.equals(expectedIssuer)) {
            throw new InvalidTokenException(String.format("Invalid issuer: '%s', expected: '%s'", issuer, expectedIssuer));
        }

        // check iat. Allow for a time skew between the different parties
        Object issuedAt = map.get(ISSUED_AT_KEY);
        if (issuedAt == null || !(issuedAt instanceof Integer)) {
            throw new InvalidTokenException("Missing or invalid 'iat' key, expecting a valid timestamp value.");
        }
        if ((Integer) issuedAt > now + ALLOWED_SKEW_IN_MS) {
            throw new InvalidTokenException("Token has been issued in the future: " + issuedAt);
        }

        // check audience
        // The Spring OAuth2 filter will check the audience matches the resource ID(s) defined in the configuration YAML file
        logger.info("The access token has been successfully validated locally.");
    }

    /**
     * Call the VMware endpoint to validate the access token.
     */
    private void validateAccessToken(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        logger.info("Validate the token remotely using: " + this.validateTokenUrl);
        String isValid = restTemplate.getForObject(this.validateTokenUrl, String.class);
        if (isValid == null || Boolean.FALSE.toString().equals(isValid)) {
            throw new InvalidTokenException("The token is not valid: " + accessToken);
        }
    }

    @Override
    public OAuth2AccessToken readAccessToken(String accessToken) {
        return tokenStore.readAccessToken(accessToken);
    }

    public void setValidateLocally(boolean validateLocally) {
        this.validateLocally = validateLocally;
    }

    public void setRestTemplate(RestOperations restTemplate) {
        this.restTemplate = restTemplate;
    }
}
