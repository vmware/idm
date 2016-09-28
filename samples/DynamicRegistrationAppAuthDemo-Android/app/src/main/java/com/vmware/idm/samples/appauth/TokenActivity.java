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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.vmware.idm.samples.appauth.R;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceDiscovery;
import net.openid.appauth.ClientAuthentication;
import net.openid.appauth.DeviceAuthorizationService;
import net.openid.appauth.DeviceRegistrationRequest;
import net.openid.appauth.DeviceRegistrationResponse;
import net.openid.appauth.RegistrationResponse;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;

/**
 * A sample activity to serve as a client to the Native OAuth library, using VMware Identity Manager as the IDP.
 * Based on the demo app of AppAuth.
 */
public class TokenActivity extends AppCompatActivity {
    private static final String TAG = "TokenActivity";

    private static final String KEY_AUTH_STATE = "authState";
    private static final String KEY_USER_INFO = "userInfo";

    private static final String EXTRA_AUTH_SERVICE_DISCOVERY = "authServiceDiscovery";
    private static final String EXTRA_AUTH_STATE = "authState";

    private static final int BUFFER_SIZE = 1024;

    private AuthState mAuthState;
    private DeviceAuthorizationService mAuthService;
    private JSONObject mUserInfoJson;
    private JSONObject mScimUserInfoJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token);

        mAuthService = new DeviceAuthorizationService(this);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_AUTH_STATE)) {
                try {
                    mAuthState = AuthState.jsonDeserialize(
                            savedInstanceState.getString(KEY_AUTH_STATE));
                } catch (JSONException ex) {
                    Log.e(TAG, "Malformed authorization JSON saved", ex);
                }
            }

            if (savedInstanceState.containsKey(KEY_USER_INFO)) {
                try {
                    mUserInfoJson = new JSONObject(savedInstanceState.getString(KEY_USER_INFO));
                } catch (JSONException ex) {
                    Log.e(TAG, "Failed to parse saved user info JSON", ex);
                }
            }
        }

        if (mAuthState == null) {
            mAuthState = getAuthStateFromIntent(getIntent());

            DeviceRegistrationResponse response =
                    DeviceRegistrationResponse.fromIntent(getIntent());
            // TODO to handle exception path from registration:
            AuthorizationException ex = AuthorizationException.fromIntent(getIntent());

            if (response != null) {
                showSnackbar(R.string.exchange_activation_code);
                Log.d(TAG, "Received DeviceRegistrationResponse.");
                exchangeActivationCode(response);
            } else {
                Log.i(TAG, "Device registration failed");
                showSnackbar(R.string.authorization_failed);
            }
        }
        refreshUi();

    }

    private void exchangeActivationCode(final DeviceRegistrationResponse registrationResponse) {
        mAuthService.performActivationRequest(registrationResponse.createDeviceActivationRequest(
                registrationResponse.request.activationEndpointUri),
                new DeviceAuthorizationService.DeviceActivationResponseCallback() {
                    @Override
                    public void onActivationRequestCompleted(
                            @Nullable RegistrationResponse response,
                            @Nullable AuthorizationException ex) {
                        receivedActivationResponse(registrationResponse, response, ex);
                    }
                });
    }

    private void receivedActivationResponse(
            @Nullable DeviceRegistrationResponse deviceRegistrationResponse,
            @Nullable RegistrationResponse response,
            @Nullable AuthorizationException ex) {

        Log.d(TAG, "Activation request complete");

        showSnackbar((response != null)
                ? R.string.exchange_complete
                : R.string.activation_failed);

        if (response != null) {
            // Now we can re-construct the original AppAuth Authorization response
            AuthorizationResponse authorizationResponse =
                    deviceRegistrationResponse.toAuthorizationResponse(response.clientId);

            mAuthState.update(response);
            mAuthState.update(authorizationResponse, ex);

            // now exchange the authorization code for an access token
            exchangeAuthorizationCode(authorizationResponse);
        }
        refreshUi();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        if (mAuthState != null) {
            state.putString(KEY_AUTH_STATE, mAuthState.jsonSerializeString());
        }

        if (mUserInfoJson != null) {
            state.putString(KEY_USER_INFO, mUserInfoJson.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAuthService.dispose();
    }

    private void receivedTokenResponse(
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {
        Log.d(TAG, "Token request complete");
        mAuthState.update(tokenResponse, authException);
        showSnackbar((tokenResponse != null)
                ? R.string.exchange_complete
                : R.string.refresh_failed);
        refreshUi();
    }

    private void refreshUi() {
        TextView refreshTokenInfoView = (TextView) findViewById(R.id.refresh_token_info);
        TextView accessTokenInfoView = (TextView) findViewById(R.id.access_token_info);
        TextView idTokenInfoView = (TextView) findViewById(R.id.id_token_info);
        Button refreshTokenButton = (Button) findViewById(R.id.refresh_token);

        if (mAuthState.isAuthorized()) {
            refreshTokenInfoView.setText((mAuthState.getRefreshToken() == null)
                    ? R.string.no_refresh_token_returned
                    : R.string.refresh_token_returned);

            idTokenInfoView.setText((mAuthState.getIdToken()) == null
                    ? R.string.no_id_token_returned
                    : R.string.id_token_returned);

            if (mAuthState.getAccessToken() == null) {
                accessTokenInfoView.setText(R.string.no_access_token_returned);
            } else {
                Long expiresAt = mAuthState.getAccessTokenExpirationTime();
                String expiryStr;
                if (expiresAt == null) {
                    expiryStr = getResources().getString(R.string.unknown_expiry);
                } else {
                    expiryStr = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL)
                            .format(new Date(expiresAt));
                }
                String tokenInfo = String.format(
                        getResources().getString(R.string.access_token_expires_at),
                        expiryStr);
                accessTokenInfoView.setText(tokenInfo);
            }
        }

        refreshTokenButton.setVisibility(mAuthState.getRefreshToken() != null
                ? View.VISIBLE
                : View.GONE);
        refreshTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshAccessToken();
            }
        });

        Button viewProfileButton = (Button) findViewById(R.id.view_profile);

        AuthorizationServiceDiscovery discoveryDoc = getDiscoveryDocFromIntent(getIntent());
        if (!mAuthState.isAuthorized()
                || discoveryDoc == null
                || discoveryDoc.getUserinfoEndpoint() == null) {
            viewProfileButton.setVisibility(View.GONE);
        } else {
            viewProfileButton.setVisibility(View.VISIBLE);
            viewProfileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            fetchUserInfo();
                            return null;
                        }
                    }.execute();
                }
            });
        }

        View userInfoCard = findViewById(R.id.userinfo_card);
        if (mUserInfoJson == null) {
            userInfoCard.setVisibility(View.INVISIBLE);
        } else {
            try {
                String name = "???";
                if (mUserInfoJson.has("name")) {
                    name = mUserInfoJson.getString("name");
                }
                ((TextView) findViewById(R.id.userinfo_name)).setText(name);

                if (mUserInfoJson.has("picture")) {
                    Glide.with(TokenActivity.this)
                            .load(Uri.parse(mUserInfoJson.getString("picture")))
                            .fitCenter()
                            .into((ImageView) findViewById(R.id.userinfo_profile));
                }

                ((TextView) findViewById(R.id.userinfo_json)).setText(mUserInfoJson.toString(2));

                userInfoCard.setVisibility(View.VISIBLE);
            } catch (JSONException ex) {
                Log.e(TAG, "Failed to read userinfo JSON", ex);
            }
        }


        // VMware has no discovery endpoint but has a user endpoint and a SCIM endpoint to also get user info
        refreshViewUserInfoScim();
    }

    private void refreshViewUserInfoScim() {

        Button viewScimUserButton = (Button) findViewById(R.id.view_scim_user);

        if (!mAuthState.isAuthorized()) {
            viewScimUserButton.setVisibility(View.GONE);
        } else {
            viewScimUserButton.setVisibility(View.VISIBLE);
            viewScimUserButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            fetchUserInfoByScim();
                            return null;
                        }
                    }.execute();
                }
            });
        }

        View scimUserInfoCard = findViewById(R.id.scim_userinfo_card);
        if (mScimUserInfoJson == null) {
            scimUserInfoCard.setVisibility(View.INVISIBLE);
        } else {
            try {
                String name = "???";
                if (mScimUserInfoJson.has("name")) {
                    JSONObject nameJson = mScimUserInfoJson.getJSONObject("name");
                    if (nameJson.has("givenName")) {
                        name = nameJson.getString("givenName");
                    }
                    if (nameJson.has("familyName")) {
                        name += " " + nameJson.getString("familyName");
                    }
                }
                ((TextView) findViewById(R.id.scim_userinfo_name)).setText(name);
                ((TextView) findViewById(R.id.scim_userinfo_json))
                        .setText(mScimUserInfoJson.toString(2));

                scimUserInfoCard.setVisibility(View.VISIBLE);
            } catch (JSONException ex) {
                Log.e(TAG, "Failed to read SCIM userinfo JSON", ex);
            }
        }

    }

    private void fetchUserInfo() {
        if (mAuthState.getAuthorizationServiceConfiguration() == null) {
            Log.e(TAG, "Cannot make userInfo request without service configuration");
        }

        mAuthState.performActionWithFreshTokens(mAuthService, new AuthState.AuthStateAction() {
            @Override
            public void execute(String accessToken, String idToken, AuthorizationException ex) {
                if (ex != null) {
                    Log.e(TAG, "Token refresh failed when fetching user info");
                    return;
                }
                /* VMware does not provide a discovery URL but has a userinfo endpoint nonetheless.
                So if we comment the following check on discovery doc and use the hard-coded endpoint,
                this would work as well. */

                AuthorizationServiceDiscovery discoveryDoc = getDiscoveryDocFromIntent(getIntent());
                if (discoveryDoc == null) {
                    throw new IllegalStateException("no available discovery doc");
                }

                URL userInfoEndpoint;
                try {
                    /* See comment above. Using the following call would work:
                    userInfoEndpoint = new URL(getResources().getString(R.string.vmware_userinfo_endpoint_uri));
                    */
                    userInfoEndpoint = new URL(discoveryDoc.getUserinfoEndpoint().toString());
                } catch (MalformedURLException urlEx) {
                    Log.e(TAG, "Failed to construct user info endpoint URL", urlEx);
                    return;
                }

                InputStream userInfoResponse = null;
                try {
                    HttpURLConnection conn = (HttpURLConnection) userInfoEndpoint.openConnection();
                    conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                    conn.setInstanceFollowRedirects(false);
                    userInfoResponse = conn.getInputStream();
                    String response = readStream(userInfoResponse);
                    updateUserInfo(new JSONObject(response));
                } catch (IOException ioEx) {
                    Log.e(TAG, "Network error when querying userinfo endpoint", ioEx);
                } catch (JSONException jsonEx) {
                    Log.e(TAG, "Failed to parse userinfo response");
                } finally {
                    if (userInfoResponse != null) {
                        try {
                            userInfoResponse.close();
                        } catch (IOException ioEx) {
                            Log.e(TAG, "Failed to close userinfo response stream", ioEx);
                        }
                    }
                }
            }
        });
    }

    private void fetchUserInfoByScim() {
        mAuthState.performActionWithFreshTokens(mAuthService, new AuthState.AuthStateAction() {
            @Override
            public void execute(String accessToken, String idToken, AuthorizationException ex) {
                if (ex != null) {
                    Log.e(TAG, "Token refresh failed when fetching SCIM user info");
                    return;
                }

                URL scimUserInfoEndpoint;
                try {
                    URL baseURL = new URL(getResources()
                            .getString(R.string.vmware_url));
                    scimUserInfoEndpoint = new URL(baseURL, getResources()
                            .getString(R.string.vmware_scim_userinfo_endpoint_uri));
                } catch (MalformedURLException urlEx) {
                    Log.e(TAG, "Failed to construct SCIM user info endpoint URL", urlEx);
                    return;
                }

                InputStream userInfoResponse = null;

                try {
                    HttpURLConnection conn =
                            (HttpURLConnection) scimUserInfoEndpoint.openConnection();
                    conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                    int code = conn.getResponseCode();
                    Log.d(TAG, "SCIM userinfo " + scimUserInfoEndpoint.toString() +
                            " completed with response code: " + code);

                    userInfoResponse = conn.getInputStream();
                    String response = readStream(userInfoResponse);
                    updateScimUserInfo(new JSONObject(response));
                } catch (IOException ioEx) {
                    // Note that we could come here with a response other than 200 (so not really a network error)
                    Log.e(TAG, "Network error when querying SCIM userinfo endpoint", ioEx);
                } catch (JSONException jsonEx) {
                    Log.e(TAG, "Failed to parse SCIM userinfo response");
                } finally {
                    if (userInfoResponse != null) {
                        try {
                            userInfoResponse.close();
                        } catch (IOException ioEx) {
                            Log.e(TAG, "Failed to close SCIM userinfo response stream", ioEx);
                        }
                    }
                }
            }
        });
    }

    private void updateScimUserInfo(final JSONObject jsonObject) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mScimUserInfoJson = jsonObject;
                refreshUi();
            }
        });
    }

    private void updateUserInfo(final JSONObject jsonObject) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mUserInfoJson = jsonObject;
                refreshUi();
            }
        });
    }

    private void refreshAccessToken() {
        performTokenRequest(mAuthState.createTokenRefreshRequest());
    }

    private void exchangeAuthorizationCode(AuthorizationResponse authorizationResponse) {
        performTokenRequest(authorizationResponse.createTokenExchangeRequest());
    }

    private void performTokenRequest(TokenRequest request) {
        ClientAuthentication clientAuthentication;
        try {
            clientAuthentication = mAuthState.getClientAuthentication();
        } catch (ClientAuthentication.UnsupportedAuthenticationMethod ex) {
            Log.d(TAG, "Token request cannot be made, client authentication for the token "
                    + "endpoint could not be constructed (%s)", ex);
            return;
        }

        mAuthService.performTokenRequest(
                request,
                clientAuthentication,
                new AuthorizationService.TokenResponseCallback() {
                    @Override
                    public void onTokenRequestCompleted(
                            @Nullable TokenResponse tokenResponse,
                            @Nullable AuthorizationException ex) {
                        receivedTokenResponse(tokenResponse, ex);
                    }
                });
    }

    @MainThread
    private void showSnackbar(@StringRes int messageId) {
        Snackbar.make(findViewById(R.id.coordinator),
                getResources().getString(messageId),
                Snackbar.LENGTH_SHORT)
                .show();
    }

    private static String readStream(InputStream stream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        char[] buffer = new char[BUFFER_SIZE];
        StringBuilder sb = new StringBuilder();
        int readCount;
        while ((readCount = br.read(buffer)) != -1) {
            sb.append(buffer, 0, readCount);
        }
        return sb.toString();
    }

    static PendingIntent createPostAuthorizationIntent(
            @NonNull Context context,
            @NonNull AuthorizationRequest request,
            @Nullable AuthorizationServiceDiscovery discoveryDoc,
            @NonNull AuthState authState) {
        Intent intent = new Intent(context, TokenActivity.class);
        intent.putExtra(EXTRA_AUTH_STATE, authState.jsonSerializeString());
        if (discoveryDoc != null) {
            intent.putExtra(EXTRA_AUTH_SERVICE_DISCOVERY, discoveryDoc.docJson.toString());
        }

        return PendingIntent.getActivity(context, request.hashCode(), intent, 0);
    }

    static AuthorizationServiceDiscovery getDiscoveryDocFromIntent(Intent intent) {
        if (!intent.hasExtra(EXTRA_AUTH_SERVICE_DISCOVERY)) {
            return null;
        }
        String discoveryJson = intent.getStringExtra(EXTRA_AUTH_SERVICE_DISCOVERY);
        try {
            return new AuthorizationServiceDiscovery(new JSONObject(discoveryJson));
        } catch (JSONException | AuthorizationServiceDiscovery.MissingArgumentException ex) {
            throw new IllegalStateException("Malformed JSON in discovery doc");
        }
    }

    static AuthState getAuthStateFromIntent(Intent intent) {
        if (!intent.hasExtra(EXTRA_AUTH_STATE)) {
            throw new IllegalArgumentException("The AuthState instance is missing in the intent.");
        }
        try {
            return AuthState.jsonDeserialize(intent.getStringExtra(EXTRA_AUTH_STATE));
        } catch (JSONException ex) {
            Log.e(TAG, "Malformed AuthState JSON saved", ex);
            throw new IllegalArgumentException("The AuthState instance is missing in the intent.");
        }
    }

    /**
     * Create the intent that will be used after the device registration step to start this activity.
     *
     * @param context   the activity context
     * @param request   the original device registration request
     * @param authState the initial auth state
     * @return The intent to start this activity.
     */
    public static PendingIntent createPostDeviceRegistrationIntent(
            @NonNull Context context,
            @NonNull DeviceRegistrationRequest request, AuthState authState) {
        Intent intent = new Intent(context, TokenActivity.class);
        intent.putExtra(EXTRA_AUTH_STATE, authState.jsonSerializeString());
        return PendingIntent.getActivity(context, request.hashCode(), intent, 0);
    }
}
