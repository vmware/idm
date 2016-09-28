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
import android.support.annotation.VisibleForTesting;
import android.support.customtabs.CustomTabsIntent;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton to mimic PendingIntentStore to store our {@link net.openid.appauth.DeviceRegistrationRequest}
 * <p>
 * Stores {@link android.app.PendingIntent} associated with each {@link DeviceRegistrationRequest} made via
 * {@link DeviceAuthorizationService#performDeviceRegistrationRequest(DeviceRegistrationRequest, PendingIntent, CustomTabsIntent)}.
 * The pending intents are read and sent by
 * the {@link RedirectUriRegistrationReceiverActivity} when the redirect Uri is received.
 */
class DeviceRegistrationPendingIntentStore {
    private Map<String, DeviceRegistrationRequest> mRequests = new HashMap<>();
    private Map<String, PendingIntent> mPendingIntents = new HashMap<>();

    private static DeviceRegistrationPendingIntentStore sInstance;

    private DeviceRegistrationPendingIntentStore() {
    }

    public static synchronized DeviceRegistrationPendingIntentStore getInstance() {
        if (sInstance == null) {
            sInstance = new DeviceRegistrationPendingIntentStore();
        }
        return sInstance;
    }

    public void addPendingIntent(DeviceRegistrationRequest request, PendingIntent intent) {
        Logger.verbose("Adding pending intent for state %s", request.state);
        mRequests.put(request.state, request);
        mPendingIntents.put(request.state, intent);
    }

    public DeviceRegistrationRequest getOriginalRequest(String state) {
        Logger.verbose("Retrieving original request for state %s", state);
        return mRequests.remove(state);
    }

    public PendingIntent getPendingIntent(String state) {
        Logger.verbose("Retrieving pending intent for scheme %s", state);
        return mPendingIntents.remove(state);
    }

    @VisibleForTesting
    void clearAll() {
        mPendingIntents.clear();
        mRequests.clear();
    }
}


