/*
 * Copyright 2012-2016 the original author or authors.
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Configuration for this resource server.
 * The @EnableResourceServer annotation enables the Spring Security filter that authenticates requests via an incoming OAuth2 token.
 */
@Configuration
@EnableResourceServer
public class ResourceApplicationConfiguration extends ResourceServerConfigurerAdapter {
    private static Logger logger = Logger.getLogger(ResourceApplicationConfiguration.class.getSimpleName());

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
        // VMware Identity Manager does not allow to populate the audience field of the access token
        // with a different resource server then itself right now, so the "id" defined in application.yml
        // must be "https://<tenant url>/SAAS/auth/oauthtoken"
        resources.resourceId(vmware().getResource().getResourceId());
    }

    /**
     * Allow "/" to be accessed without any permissions and anything else is protected by a valid Oauth2 token.
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/").permitAll()
                .anyRequest().authenticated();
    }

    /**
     * Defining a name allows us to use "vmware.xxx" instead of "security.oauth2.xxx" in the application.yml file
     */
    @Bean
    @ConfigurationProperties("vmware")
    public ResourceServerResources vmware() {
        return new ResourceServerResources();
    }

    /**
     * Configure the bean responsible for loading/decoding our access tokens.
     * We need the public key of VMware Identity Manager during the creation of the bean as Spring will not be able to decode the token
     * if the signature verification is failing.
     */
    @Bean
    protected JwtAccessTokenConverter jwtAccessTokenConverter() throws Exception {
        ResourceServerResources resources = vmware();
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();

        // Fetch the public key in PEM format from Identity Manager for demo purpose (instead of hard-coding as a file resource)
        RestTemplate template = new RestTemplate();
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(resources.getResource().getJwt().getKeyUri().toString())
                .queryParam("format", "pem");

        logger.info("Fetch public key from " + builder.toUriString());
        String publicKeyPem = template.getForObject(builder.toUriString(), String.class);
        converter.setVerifierKey(publicKeyPem);

        DefaultAccessTokenConverter defaultAccessTokenConverter = new DefaultAccessTokenConverter();
        defaultAccessTokenConverter.setUserTokenConverter(userAuthenticationConverter());
        converter.setAccessTokenConverter(defaultAccessTokenConverter);
        return converter;
    }

    @Bean
    protected VMwareUserAuthenticationConverter userAuthenticationConverter() {
        return new VMwareUserAuthenticationConverter();
    }

    /**
     * To validate the incoming token by either validating locally or by using the VMware Identity Manager check token endpoint.
     */
    @Bean
    public ResourceServerTokenServices remoteTokenServices() throws Exception {
        ResourceServerResources resources = vmware();
        final VMwareValidateTokenServices resourceServerTokenServices = new VMwareValidateTokenServices(
                resources.getCheckTokenUri(), new JwtTokenStore(jwtAccessTokenConverter()));
        resourceServerTokenServices.setValidateLocally(resources.isPerformLocalValidation());
        return resourceServerTokenServices;
    }


    /**
     * To access the resources properties defined in application.yml file.
     */
    class ResourceServerResources {

        @NestedConfigurationProperty
        private ResourceServerProperties resource = new ResourceServerProperties();

        @Value("${vmware.resource.localValidation}")
        private boolean performLocalValidation;

        /**
         * VMware Identity Manager does not provide a check_token endpoint yet,
         * but provides an endpoint to validate the access token.
         */
        @Value("${vmware.resource.checkTokenUri}")
        private String checkTokenUri;

        public String getCheckTokenUri() {
            return checkTokenUri;
        }

        public boolean isPerformLocalValidation() {
            return performLocalValidation;
        }

        public ResourceServerProperties getResource() {
            return resource;
        }
    }

    /**
     * Extract some more information from the VMware Identity Manager Access Token: the principal.
     * (The default implementation looks for "user_name" but VMware Identity Manager does not include such key.
     * It includes "user_id" which can be used to fetch information back or - here - we just use "prn" to extract principal.
     */
    private class VMwareUserAuthenticationConverter extends DefaultUserAuthenticationConverter {
        private static final String PRINCIPAL_KEY = "prn";

        @Override
        public Authentication extractAuthentication(Map<String, ?> map) {
            if (map.containsKey(PRINCIPAL_KEY)) {
                Object principal = map.get(PRINCIPAL_KEY);
                return new UsernamePasswordAuthenticationToken(principal, "N/A", new ArrayList<>());
            }
            return null;
        }
    }
}
