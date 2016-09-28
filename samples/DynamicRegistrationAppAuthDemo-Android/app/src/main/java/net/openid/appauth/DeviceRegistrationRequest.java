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
import android.support.annotation.Nullable;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import static net.openid.appauth.JsonUtil.getStringIfDefined;
import static net.openid.appauth.JsonUtil.put;
import static net.openid.appauth.Preconditions.checkNotEmpty;
import static net.openid.appauth.Preconditions.checkNotNull;

public class DeviceRegistrationRequest {

    static final String PARAM_USER_DEVICE = "user_device";
    static final String PARAM_APP_PRODUCT_ID = "app_product_id";
    static final String PARAM_TYPE = "type";
    static final String PARAM_REDIRECT_URI = "redirect_uri";
    static final String PARAM_RESPONSE_TYPE = "response_type";
    static final String PARAM_DEVICE_NAME = "device_name";
    static final String PARAM_SCOPE = "scope";
    static final String PARAM_STATE = "state";
    static final String PARAM_ACTIVATION_URI = "activation_uri";

    static final String KEY_CONFIGURATION = "configuration";

    private static final int STATE_LENGTH = 16;

    /**
     * Instructs the device registration server to generate an authorization code.
     */
    public static final String RESPONSE_TYPE_CODE = "code";

    /**
     * Instructs the device registration server to register the device.
     */
    public static final String TYPE_REGISTER = "register";

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
     * The client's redirect URI.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.1.2"> "The OAuth 2.0
     * Authorization
     * Framework" (RFC 6749), Section 3.1.2</a>
     */
    @NonNull
    public final Uri redirectUri;

    /**
     * The response type, will always be 'code'.
     */
    @NonNull
    public final String responseType;

    /**
     * The action type, will always be 'register'.
     */
    @NonNull
    public final String registerType;

    /**
     * The device name type to use.
     */
    @NonNull
    public final String deviceName;

    /**
     * The user device information.
     */
    @NonNull
    public final String userDevice;

    /**
     * The name of the application temaplte as defined on the server.
     */
    @NonNull
    public final String appProductId;

    /**
     * The optional set of scopes expressed as a space-delimited, case-sensitive string.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.3"> "The OAuth 2.0 Authorization
     * Framework" (RFC 6749), Section 3.3</a>
     */
    @Nullable
    public final String scope;

    /**
     * An opaque value used by the client to maintain state between the request and callback. If
     * this value is not explicitly set, this library will automatically add state and perform
     * appropriate  validation of the state in the authorization response. It is recommended that
     * the default implementation of this parameter be used wherever possible. Typically used to
     * prevent CSRF attacks, as recommended in
     * <a href="https://tools.ietf.org/html/rfc6819#section-5.3.5">RFC6819 Section 5.3.5</a>.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.1"> "The OAuth 2.0
     * Authorization
     * Framework" (RFC 6749), Section 4.1.1</a>
     * @see <a href="https://tools.ietf.org/html/rfc6819#section-5.3.5"> "The OAuth 2.0
     * Authorization
     * Framework" (RFC 6749), Section 5.3.5</a>
     */
    @Nullable
    public final String state;

    /**
     * The activation endpoint (to activate the device after registration).
     * We can not put this in the @{link @AuthorizationServiceConfiguration}, so register it here.
     */
    @NonNull
    public final Uri activationEndpointUri;


    /**
     * Creates instances of {@link DeviceRegistrationRequest}.
     */
    public static final class Builder {
        @NonNull
        private AuthorizationServiceConfiguration mConfiguration;

        @NonNull
        private Uri mRedirectUri;

        @NonNull
        private Uri mActivationEndpointUri;

        @NonNull
        private String mDeviceName;

        @NonNull
        private String mUserDevice;

        @NonNull
        private String mAppProductId;

        @Nullable
        private String mScope;
        @Nullable
        private String mState;

        /**
         * Creates a device registration request builder with the specified mandatory properties.
         */
        public Builder(
                @NonNull AuthorizationServiceConfiguration configuration,
                @NonNull Uri redirectUri,
                @NonNull String deviceName,
                @NonNull String userDevice,
                @NonNull String appProductId,
                @NonNull Uri activationEndpointUri
        ) {
            setConfiguration(configuration);
            setRedirectUri(redirectUri);
            setDeviceName(deviceName);
            setUserDevice(userDevice);
            setAppProductId(appProductId);
            setState(DeviceRegistrationRequest.generateRandomState());
            setActivationEndpoint(activationEndpointUri);
        }

        @NonNull
        private Builder setActivationEndpoint(Uri activationEndpointUri) {
            this.mActivationEndpointUri = activationEndpointUri;
            return this;
        }

        @NonNull
        public String getAppProductId() {
            return mAppProductId;
        }

        public Builder setAppProductId(@NonNull String mAppProductId) {
            this.mAppProductId = mAppProductId;
            return this;
        }

        @NonNull
        public Uri getRedirectUri() {
            return mRedirectUri;
        }

        @Nullable
        public String getState() {
            return mState;
        }

        public Builder setState(@Nullable String mState) {
            this.mState = mState;
            return this;
        }

        /**
         * Specifies the redirect URI's.
         *
         * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.1.2"> "The OAuth 2.0
         * Authorization Framework" (RFC 6749), Section 3.1.2</a>
         */
        public Builder setRedirectUri(@NonNull Uri mRedirectUri) {
            this.mRedirectUri = mRedirectUri;
            return this;
        }

        @NonNull
        public String getDeviceName() {
            return mDeviceName;
        }

        public Builder setDeviceName(@NonNull String mDeviceName) {
            this.mDeviceName = mDeviceName;
            return this;
        }

        @NonNull
        public String getUserDevice() {
            return mUserDevice;
        }

        public Builder setUserDevice(@NonNull String mUserDevice) {
            this.mUserDevice = mUserDevice;
            return this;
        }

        @Nullable
        public String getScope() {
            return mScope;
        }

        public Builder setScope(@Nullable String mScope) {
            this.mScope = mScope;
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
         * Constructs the registration request. At a minimum the following fields must have been
         * set:
         * <ul>
         * <li>The redirect URIs</li>
         * <li>The device name</li>
         * <li>The product app id</li>
         * <li>The device information</li>
         * </ul> Failure to specify any of these parameters will result in a runtime exception.
         */
        @NonNull
        public DeviceRegistrationRequest build() {
            return new DeviceRegistrationRequest(
                    mConfiguration,
                    mRedirectUri,
                    mDeviceName,
                    mUserDevice,
                    mAppProductId,
                    mScope,
                    mState,
                    mActivationEndpointUri
            );
        }
    }

    private static String generateRandomState() {
        SecureRandom sr = new SecureRandom();
        byte[] random = new byte[STATE_LENGTH];
        sr.nextBytes(random);
        return Base64.encodeToString(random, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
    }

    private DeviceRegistrationRequest(
            @NonNull AuthorizationServiceConfiguration configuration,
            @NonNull Uri redirectUri,
            @NonNull String deviceName,
            @NonNull String userDevice,
            @NonNull String appProductId,
            @Nullable String scope,
            @Nullable String state,
            @NonNull Uri activationEndpointUri) {
        this.configuration = configuration;
        this.redirectUri = redirectUri;
        this.activationEndpointUri = activationEndpointUri;
        this.deviceName = deviceName;
        this.userDevice = userDevice;
        this.appProductId = appProductId;
        this.scope = scope;
        this.state = state;

        this.responseType = RESPONSE_TYPE_CODE;
        this.registerType = TYPE_REGISTER;
    }


    /**
     * Produces a request URI, that can be used to dispatch the device registration request.
     */
    @NonNull
    public Uri toUri() {
        Uri.Builder uriBuilder = configuration.registrationEndpoint.buildUpon()
                .appendQueryParameter(PARAM_REDIRECT_URI, redirectUri.toString())
                .appendQueryParameter(PARAM_APP_PRODUCT_ID, appProductId)
                .appendQueryParameter(PARAM_DEVICE_NAME, deviceName)
                .appendQueryParameter(PARAM_USER_DEVICE, userDevice)
                .appendQueryParameter(PARAM_RESPONSE_TYPE, responseType)
                .appendQueryParameter(PARAM_TYPE, registerType);

        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_STATE, state);
        UriUtil.appendQueryParameterIfNotNull(uriBuilder, PARAM_SCOPE, scope);
        return uriBuilder.build();
    }

    /**
     * Produces a JSON representation of the registration request for persistent storage or
     * local transmission (e.g. between activities).
     */
    @NonNull
    public JSONObject jsonSerialize() {
        JSONObject json = jsonSerializeParams();
        put(json, KEY_CONFIGURATION, configuration.toJson());
        return json;
    }

    private JSONObject jsonSerializeParams() {
        JSONObject json = new JSONObject();
        JsonUtil.put(json, PARAM_RESPONSE_TYPE, responseType);
        JsonUtil.put(json, PARAM_TYPE, registerType);

        JsonUtil.put(json, PARAM_REDIRECT_URI, redirectUri.toString());
        JsonUtil.put(json, PARAM_ACTIVATION_URI, activationEndpointUri.toString());
        JsonUtil.put(json, PARAM_APP_PRODUCT_ID, appProductId);
        JsonUtil.put(json, PARAM_DEVICE_NAME, deviceName);
        JsonUtil.put(json, PARAM_USER_DEVICE, userDevice);

        JsonUtil.putIfNotNull(json, PARAM_SCOPE, scope);
        JsonUtil.putIfNotNull(json, PARAM_STATE, state);
        return json;
    }

    /**
     * Reads a registration request from a JSON string representation produced by
     * {@link #jsonSerialize()}.
     *
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    public static DeviceRegistrationRequest jsonDeserialize(@NonNull JSONObject json)
            throws JSONException {
        checkNotNull(json, "json must not be null");
        Uri redirectUri = Uri.parse(checkNotNull(json.get(PARAM_REDIRECT_URI)).toString());
        Uri activationUri = Uri.parse(checkNotNull(json.get(PARAM_ACTIVATION_URI)).toString());

        Builder builder = new DeviceRegistrationRequest.Builder(
                AuthorizationServiceConfiguration.fromJson(json.getJSONObject(KEY_CONFIGURATION)),
                redirectUri,
                JsonUtil.getString(json, PARAM_DEVICE_NAME),
                JsonUtil.getString(json, PARAM_USER_DEVICE),
                JsonUtil.getString(json, PARAM_APP_PRODUCT_ID),
                activationUri)
                .setScope(getStringIfDefined(json, PARAM_SCOPE))
                .setState(getStringIfDefined(json, PARAM_STATE));

        return builder.build();
    }

    /**
     * Convert to an original AppAuth {@link AuthorizationRequest}, to be consumed by the AppAuth library.
     */
    public AuthorizationRequest toAuthorizationRequest(String clientId) {
        return new AuthorizationRequest.Builder(configuration, clientId, responseType, redirectUri)
                .setScope(scope).build();
    }
}
