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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.customtabs.CustomTabsIntent;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import static net.openid.appauth.Utils.*;

/**
 * To support the flow of registering devices dynamically with VMware Identity Manager.
 */
public class DeviceAuthorizationService extends AuthorizationService {

    private static String TAG = DeviceAuthorizationService.class.getSimpleName();
    private final BrowserHandler browserHandler;
    private final UrlBuilder urlBuilder;

    /**
     * Creates an AuthorizationService instance based on the provided configuration. Note that
     * instances of this class must be manually disposed when no longer required, to avoid
     * leaks (see {@link #dispose()}.
     *
     * @param context the application context
     */
    public DeviceAuthorizationService(@NonNull Context context) {
        this(context, AuthorizationService.DefaultUrlBuilder.INSTANCE, new BrowserHandler(context));
    }

    @VisibleForTesting
    DeviceAuthorizationService(@NonNull Context context,
                               @NonNull AuthorizationService.UrlBuilder urlBuilder,
                               @NonNull BrowserHandler browserHandler) {
        super(context, urlBuilder, browserHandler);
        this.browserHandler = browserHandler;
        this.urlBuilder = urlBuilder;
    }

    /**
     * Sends a device registration request to the authorization service, using a
     * <a href="https://developer.chrome.com/multidevice/android/customtabs">custom tab</a>.
     * The parameters of this request are determined by both the authorization service
     * configuration and the provided {@link DeviceRegistrationRequest request object}. Upon completion
     * of this request, the provided {@link PendingIntent result handler intent} will be invoked.
     *
     * @param customTabsIntent The intent that will be used to start the custom tab. It is recommended that this intent
     *                         be created with the help of {@link #createCustomTabsIntentBuilder()}, which will ensure
     *                         that a warmed-up version of the browser will be used, minimizing latency.
     */
    public void performDeviceRegistrationRequest(
            @NonNull DeviceRegistrationRequest request,
            @NonNull PendingIntent resultHandlerIntent,
            @NonNull CustomTabsIntent customTabsIntent) {
        // checkNotDisposed();
        Uri requestUri = request.toUri();
        DeviceRegistrationPendingIntentStore.getInstance()
                .addPendingIntent(request, resultHandlerIntent);
        Intent intent = customTabsIntent.intent;
        intent.setData(requestUri);
        if (TextUtils.isEmpty(intent.getPackage())) {
            intent.setPackage(browserHandler.getBrowserPackage());
        }

        Logger.debug("Using %s as browser for auth", intent.getPackage());
        intent.putExtra(CustomTabsIntent.EXTRA_TITLE_VISIBILITY_STATE, CustomTabsIntent.NO_TITLE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        Logger.debug("Initiating device registration request to %s (%s)",
                request.configuration.authorizationEndpoint, requestUri);
        mContext.startActivity(intent);
    }

    /**
     * Sends a request to activate the device by exchanging the activation code
     * and get back the client id and secret.
     *
     * @param request the activation request
     */
    public void performActivationRequest(@NonNull DeviceActivationRequest request,
                                         @NonNull DeviceActivationResponseCallback callback) {
        // TODO:  checkNotDisposed();
        Log.d(TAG, String.format("Exchanging activation code to %s",
                request.activationEndpoint.toString()));
        new DeviceActivationRequestTask(request, callback).execute();
    }


    /**
     * Callback interface for device activation endpoint requests.
     *
     * @see DeviceAuthorizationService#performActivationRequest
     */
    public interface DeviceActivationResponseCallback {
        /**
         * Invoked when the request completes successfully or fails.
         * <p>
         * <p>Exactly one of {@code response} or {@code ex} will be non-null. If
         * {@code response} is {@code null}, a failure occurred during the request. This can
         * happen if a bad URI was provided, no connection to the server could be established, or
         * the response JSON was incomplete or badly formatted.
         *
         * @param response the retrieved token response, if successful; {@code null} otherwise.
         * @param ex       a description of the failure, if one occurred: {@code null} otherwise.
         */
        void onActivationRequestCompleted(@Nullable RegistrationResponse response,
                                          @Nullable AuthorizationException ex);
    }

    private class DeviceActivationRequestTask
            extends AsyncTask<Void, Void, JSONObject> {
        private DeviceActivationRequest mRequest;
        private DeviceActivationResponseCallback mCallback;

        private AuthorizationException mException;

        DeviceActivationRequestTask(DeviceActivationRequest request,
                                    DeviceActivationResponseCallback callback) {
            mRequest = request;
            mCallback = callback;
        }

        @Override
        protected JSONObject doInBackground(Void... voids) {
            InputStream is = null;
            String postData = mRequest.getActivationCode();

            try {
                URL requestUrl = DeviceAuthorizationService.this.urlBuilder
                        .buildUrlFromString(mRequest.getActivationEndpoint().toString());
                HttpURLConnection conn = (HttpURLConnection) requestUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Length", String.valueOf(postData.length()));
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(postData);
                wr.flush();

                int code = conn.getResponseCode();

                Logger.debug("Sent activation request:" + requestUrl.toString());
                Logger.debug("with body:" + postData);
                Logger.debug("Response code: " + code);

                is = conn.getInputStream();
                String response = Utils.readInputStream(is);
                return new JSONObject(response);
            } catch (IOException ex) {
                Log.e(TAG, "Failed to complete device activation request", ex);
                mException = AuthorizationException.fromTemplate(
                        AuthorizationException.GeneralErrors.NETWORK_ERROR, ex);
            } catch (JSONException ex) {
                Log.e(TAG, "Failed to complete device activation request", ex);
                mException = AuthorizationException.fromTemplate(
                        AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR, ex);
            } finally {
                closeQuietly(is);
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject json) {
            if (mException != null) {
                mCallback.onActivationRequestCompleted(null, mException);
                return;
            }
            Log.d(TAG, "Activation completed");
            RegistrationResponse response;
            try {
                DeviceActivationResponse activationResponse =
                        new DeviceActivationResponse.Builder(mRequest).fromResponseJson(json)
                                .build();
                response =
                        new RegistrationResponse.Builder(mRequest.toRegistrationRequest())
                                .setClientId(activationResponse.clientId)
                                .setClientSecret(activationResponse.clientSecret).build();

            } catch (JSONException jsonEx) {
                mCallback.onActivationRequestCompleted(null,
                        AuthorizationException.fromTemplate(
                                AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR,
                                jsonEx));
                return;
            }

            Logger.debug("Device activation completed");
            mCallback.onActivationRequestCompleted(response, null);
        }
    }


}
