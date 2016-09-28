/*
 * Copyright 2016 The AppAuth for Android Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openid.appauth;

import android.net.Uri;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import static net.openid.appauth.JsonUtil.getString;
import static net.openid.appauth.JsonUtil.getStringIfDefined;
import static net.openid.appauth.JsonUtil.put;
import static net.openid.appauth.Preconditions.checkNotNull;

/**
 * VMware Identity Manager request to activate the device in echange of the activation code
 * and get a unique pair of clientID and secret.
 */
public class DeviceActivationRequest {

    static final String KEY_ACTIVATION_CODE = "activation_code";
    static final String KEY_ENDPOINT = "endpoint";
    static final String KEY_CONFIGURATION = "configuration";

    /**
     * The service's {@link AuthorizationServiceConfiguration configuration}.
     * This configuration specifies how to connect to a particular OAuth provider.
     * Configurations may be
     * {@link AuthorizationServiceConfiguration#AuthorizationServiceConfiguration(Uri,
     * Uri, Uri) created manually}, or
     * {@link AuthorizationServiceConfiguration#fetchFromUrl(Uri,
     * AuthorizationServiceConfiguration.RetrieveConfigurationCallback)
     * via an OpenID Connect Discovery Document}.
     */
    @NonNull
    public final AuthorizationServiceConfiguration configuration;

    /**
     * The activation endpoint. As the {@link AuthorizationServiceConfiguration} does not contain  our special activation endpoint, we need to add it here.
     */
    @NonNull
    public final Uri activationEndpoint;

    /**
     * The activation code to redeem.
     */
    @NonNull
    public final String activationCode;


    protected DeviceActivationRequest(
            @NonNull AuthorizationServiceConfiguration configuration,
            @NonNull Uri activationEndpoint, @NonNull String activationCode
    ) {
        this.configuration = configuration;
        this.activationCode = activationCode;
        this.activationEndpoint = activationEndpoint;
    }

    /**
     * Reads a registration request from a JSON string representation produced by
     * {@link #jsonSerialize()}.
     *
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    public static DeviceActivationRequest jsonDeserialize(@NonNull JSONObject json)
            throws JSONException {
        checkNotNull(json, "json must not be null");

        Builder builder = new DeviceActivationRequest.Builder(
                Uri.parse(getString(json, KEY_ENDPOINT)),
                AuthorizationServiceConfiguration.fromJson(json.getJSONObject(KEY_CONFIGURATION)),
                getStringIfDefined(json, KEY_ACTIVATION_CODE));

        return builder.build();
    }

    /**
     * @return The activation code.
     */
    @NonNull
    public String getActivationCode() {
        return activationCode;
    }

    /**
     * @return The activation endpoint.
     */
    @NonNull
    public Uri getActivationEndpoint() {
        return activationEndpoint;
    }

    /**
     * Create the expected registration request from AppAuth library.
     *
     * @return The registration request.
     */
    public RegistrationRequest toRegistrationRequest() {
        // @todo For now, we do not put back the redirect URI
        return new RegistrationRequest.Builder(configuration,
                Arrays.asList(Uri.parse("redirect_uri_unused")))
                .build();
    }

    /**
     * Produces a JSON representation of the activation request for persistent storage or
     * local transmission (e.g. between activities).
     */
    @NonNull
    public JSONObject jsonSerialize() {
        JSONObject json = new JSONObject();
        put(json, KEY_ENDPOINT, activationEndpoint.toString());
        put(json, KEY_ACTIVATION_CODE, activationCode);
        put(json, KEY_CONFIGURATION, configuration.toJson());
        return json;
    }

    /**
     * Creates instances of {@link DeviceActivationRequest}.
     */
    public static final class Builder {
        @NonNull
        private AuthorizationServiceConfiguration mConfiguration;

        @NonNull
        private String mActivationCode;

        @NonNull
        private Uri mActivationEndpoint;

        /**
         * Creates a device activation request builder with the specified mandatory properties.
         */
        public Builder(
                @NonNull Uri activationEndpoint,
                @NonNull AuthorizationServiceConfiguration configuration,
                @NonNull String activationCode
        ) {
            setActivationEnpoint(activationEndpoint);
            setConfiguration(configuration);
            setActivationCode(activationCode);
        }

        private Builder setActivationEnpoint(Uri activationEndpoint) {
            this.mActivationEndpoint = activationEndpoint;
            return this;
        }


        public Builder setActivationCode(@NonNull String activationCode) {
            this.mActivationCode = activationCode;
            return this;
        }

        /**
         * Specifies the authorization service configuration for the request, which must not
         * be null or empty.
         */
        @NonNull
        public Builder setConfiguration(@NonNull AuthorizationServiceConfiguration configuration) {
            mConfiguration = checkNotNull(configuration);
            return this;
        }

        /**
         * Constructs the activation request.
         */
        @NonNull
        public DeviceActivationRequest build() {
            return new DeviceActivationRequest(
                    mConfiguration,
                    mActivationEndpoint,
                    mActivationCode);
        }
    }
}
