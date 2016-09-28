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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import static net.openid.appauth.Preconditions.checkNotEmpty;
import static net.openid.appauth.Preconditions.checkNotNull;

/**
 * The response of the device activation request.
 * VMware Identity Manager service will exchange the activation code with a unique pair of
 * (client id, client secret) for this device.
 *
 * @see {DeviceActivationRequest}
 */
public class DeviceActivationResponse {

    static final String PARAM_CLIENT_ID = "client_id";
    static final String PARAM_CLIENT_SECRET = "client_secret";

    /**
     * The original request associated with this response.
     */
    @NonNull
    public final DeviceActivationRequest request;

    /**
     * The registered client identifier.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4"> "The OAuth 2.0 Authorization
     * Framework" (RFC 6749), Section 4</a>
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.1"> "The OAuth 2.0
     * Authorization
     * Framework" (RFC 6749), Section 4.1.1</a>
     */
    @NonNull
    public final String clientId;

    /**
     * The client secret, which is part of the client credentials, if provided.
     *
     * @see <a href="https://openid.net/specs/openid-connect-registration-1_0.html#RegistrationResponse">
     * "OpenID Connect Dynamic Client Registration 1.0", Section 3.2</a>
     */
    @Nullable
    public final String clientSecret;

    private DeviceActivationResponse(
            DeviceActivationRequest mRequest, String mClientId, String mClientSecret) {
        this.request = mRequest;
        this.clientId = mClientId;
        this.clientSecret = mClientSecret;
    }

    public static final class Builder {
        @NonNull
        private DeviceActivationRequest mRequest;
        @NonNull
        private String mClientId;

        @Nullable
        private String mClientSecret;

        /**
         * Creates an activation response associated with the specified request.
         */
        public Builder(@NonNull DeviceActivationRequest request) {
            setRequest(request);
        }

        /**
         * Specifies the request associated with this response. Must not be null.
         */
        @NonNull
        public DeviceActivationResponse.Builder setRequest(
                @NonNull DeviceActivationRequest request) {
            mRequest = checkNotNull(request, "request cannot be null");
            return this;
        }

        /**
         * Specifies the client identifier.
         */
        public DeviceActivationResponse.Builder setClientId(@NonNull String clientId) {
            checkNotEmpty(clientId, "client ID cannot be null or empty");
            mClientId = clientId;
            return this;
        }

        /**
         * Specifies the client secret.
         */
        public DeviceActivationResponse.Builder setClientSecret(@Nullable String clientSecret) {
            mClientSecret = clientSecret;
            return this;
        }


        /**
         * Creates the activation response instance.
         */
        public DeviceActivationResponse build() {
            return new DeviceActivationResponse(
                    mRequest,
                    mClientId,
                    mClientSecret);
        }

        /**
         * Extracts activation response fields from a JSON object.
         *
         * @throws JSONException if the JSON is malformed or has incorrect value types for fields.
         */
        @NonNull
        public DeviceActivationResponse.Builder fromResponseJson(@NonNull JSONObject json)
                throws JSONException {
            setClientId(JsonUtil.getString(json, PARAM_CLIENT_ID));

            if (json.has(PARAM_CLIENT_SECRET)) {
                setClientSecret(json.getString(PARAM_CLIENT_SECRET));
            }
            return this;
        }
    }

}
