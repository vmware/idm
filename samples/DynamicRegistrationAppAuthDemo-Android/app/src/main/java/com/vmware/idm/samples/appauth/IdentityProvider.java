/*
 * Copyright 2015 The AppAuth for Android Authors. All Rights Reserved.
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

package com.vmware.idm.samples.appauth;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.BoolRes;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.AuthorizationServiceConfiguration.RetrieveConfigurationCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An abstraction of identity providers, containing all necessary info for the demo app.
 * Based on the demo app of AppAuth.
 */
class IdentityProvider {

    /**
     * Value used to indicate that a configured property is not specified or required.
     */
    public static final int NOT_SPECIFIED = -1;

    public static final IdentityProvider VMWARE = new IdentityProvider(
            "VMware", // name of the provider, for debug strings,
            R.bool.vmware_enabled,
            R.string.vmware_url,
            R.string.vmware_activation_endpoint_uri, // activation endpoint
            R.string.vmware_auth_endpoint_uri, // authorization endpoint
            R.string.vmware_token_endpoint_uri, // token endpoint
            R.string.vmware_registration_endpoint_uri,
            NOT_SPECIFIED, // no client id, doing dynamic registration
            R.string.vmware_auth_redirect_uri,
            R.string.vmware_scope_string,
            R.string.vmware_template, // the app product id
            R.drawable.btn_vmware, // button image asset
            R.string.vmware_name, // button text
            android.R.color.black // text color on the button
    );

    public static final List<IdentityProvider> PROVIDERS = Arrays.asList(VMWARE);
    private static final Uri NO_BASE_URL = null;

    public static List<IdentityProvider> getEnabledProviders(Context context) {
        ArrayList<IdentityProvider> providers = new ArrayList<>();
        for (IdentityProvider provider : PROVIDERS) {
            provider.readConfiguration(context);
            if (provider.isEnabled()) {
                providers.add(provider);
            }
        }
        return providers;
    }

    @NonNull
    public final String name;

    @DrawableRes
    public final int buttonImageRes;

    @StringRes
    public final int buttonContentDescriptionRes;

    public final int buttonTextColorRes;

    @BoolRes
    private final int mEnabledRes;

    @StringRes
    private final int mActivationEndpointRes;

    @StringRes
    private final int mAuthEndpointRes;

    @StringRes
    private final int mTokenEndpointRes;

    @StringRes
    private final int mRegistrationEndpointRes;

    @StringRes
    private final int mClientIdRes;

    @StringRes
    private final int mRedirectUriRes;

    @StringRes
    private final int mScopeRes;

    @StringRes
    private final int mBaseUrlRes;

    @StringRes
    private final int mTemplateRes;

    private boolean mConfigurationRead = false;
    private boolean mEnabled;
    private Uri mActivationEndpoint;
    private Uri mAuthEndpoint;
    private Uri mTokenEndpoint;
    private Uri mRegistrationEndpoint;
    private String mClientId;
    private Uri mRedirectUri;
    private String mScope;
    private String mTemplate;
    private Uri mBaseUrl;

    IdentityProvider(
            @NonNull String name,
            @BoolRes int enabledRes,
            @NonNull @StringRes int baseUrlRes,
            @StringRes int activationEndpointRes,
            @StringRes int authEndpointRes,
            @StringRes int tokenEndpointRes,
            @StringRes int registrationEndpointRes,
            @StringRes int clientIdRes,
            @StringRes int redirectUriRes,
            @StringRes int scopeRes,
            @StringRes int templateRes,
            @DrawableRes int buttonImageRes,
            @StringRes int buttonContentDescriptionRes,
            @ColorRes int buttonTextColorRes) {
        if (!isSpecified(baseUrlRes)) {
            throw new IllegalArgumentException(
                    "the base VMware organization URL must be specified");
        }

        this.name = name;
        this.mEnabledRes = checkSpecified(enabledRes, "enabledRes");
        this.mBaseUrlRes = checkSpecified(baseUrlRes, "baseUrlRes");
        this.mActivationEndpointRes = activationEndpointRes;
        this.mAuthEndpointRes = authEndpointRes;
        this.mTokenEndpointRes = tokenEndpointRes;
        this.mRegistrationEndpointRes = registrationEndpointRes;
        this.mClientIdRes = clientIdRes;
        this.mRedirectUriRes = checkSpecified(redirectUriRes, "redirectUriRes");
        this.mScopeRes = checkSpecified(scopeRes, "scopeRes");
        this.mTemplateRes = checkSpecified(templateRes, "templateRes");
        this.buttonImageRes = checkSpecified(buttonImageRes, "buttonImageRes");
        this.buttonContentDescriptionRes =
                checkSpecified(buttonContentDescriptionRes, "buttonContentDescriptionRes");
        this.buttonTextColorRes = checkSpecified(buttonTextColorRes, "buttonTextColorRes");
    }

    /**
     * This must be called before any of the getters will function.
     */
    public void readConfiguration(Context context) {
        if (mConfigurationRead) {
            return;
        }

        Resources res = context.getResources();
        mEnabled = res.getBoolean(mEnabledRes);
        mBaseUrl = getUriResource(NO_BASE_URL, res, mBaseUrlRes, "baseUrlRes");

        mActivationEndpoint = isSpecified(mActivationEndpointRes)
                ? getUriResource(mBaseUrl, res, mActivationEndpointRes, "activationEndpointRes")
                : null;
        mAuthEndpoint = isSpecified(mAuthEndpointRes)
                ? getUriResource(mBaseUrl, res, mAuthEndpointRes, "authEndpointRes")
                : null;
        mTokenEndpoint = isSpecified(mTokenEndpointRes)
                ? getUriResource(mBaseUrl, res, mTokenEndpointRes, "tokenEndpointRes")
                : null;
        mRegistrationEndpoint = isSpecified(mRegistrationEndpointRes)
                ? getUriResource(mBaseUrl, res, mRegistrationEndpointRes, "registrationEndpointRes")
                : null;
        mClientId = isSpecified(mClientIdRes)
                ? res.getString(mClientIdRes)
                : null;
        mRedirectUri = getUriResource(NO_BASE_URL, res, mRedirectUriRes, "mRedirectUriRes");
        mScope = res.getString(mScopeRes);
        mTemplate = res.getString(mTemplateRes);

        mConfigurationRead = true;
    }

    private void checkConfigurationRead() {
        if (!mConfigurationRead) {
            throw new IllegalStateException("Configuration not read");
        }
    }

    public boolean isEnabled() {
        checkConfigurationRead();
        return mEnabled;
    }

    @Nullable
    public Uri getActivationEndpoint() {
        checkConfigurationRead();
        return mActivationEndpoint;
    }

    @Nullable
    public Uri getAuthEndpoint() {
        checkConfigurationRead();
        return mAuthEndpoint;
    }

    @Nullable
    public Uri getTokenEndpoint() {
        checkConfigurationRead();
        return mTokenEndpoint;
    }

    public String getClientId() {
        checkConfigurationRead();
        return mClientId;
    }


    public void setClientId(String clientId) {
        mClientId = clientId;
    }

    @NonNull
    public Uri getRedirectUri() {
        checkConfigurationRead();
        return mRedirectUri;
    }

    @NonNull
    public String getScope() {
        checkConfigurationRead();
        return mScope;
    }

    @NonNull
    public String getTemplate() {
        checkConfigurationRead();
        return mTemplate;
    }

    public void retrieveConfig(Context context,
                               RetrieveConfigurationCallback callback) {
        readConfiguration(context);
        AuthorizationServiceConfiguration config =
                new AuthorizationServiceConfiguration(mAuthEndpoint, mTokenEndpoint,
                        mRegistrationEndpoint);
        callback.onFetchConfigurationCompleted(config, null);
    }

    private static boolean isSpecified(int value) {
        return value != NOT_SPECIFIED;
    }

    private static int checkSpecified(int value, String valueName) {
        if (value == NOT_SPECIFIED) {
            throw new IllegalArgumentException(valueName + " must be specified");
        }
        return value;
    }

    private static Uri getUriResource(Uri baseUrl, Resources res, @StringRes int resId,
                                      String resName) {

        if (baseUrl == null) {
            return Uri.parse(res.getString(resId));
        }
        return Uri.withAppendedPath(baseUrl, res.getString(resId));
    }

}
