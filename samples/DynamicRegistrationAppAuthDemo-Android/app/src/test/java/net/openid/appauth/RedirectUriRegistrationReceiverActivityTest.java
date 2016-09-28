package net.openid.appauth;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static net.openid.appauth.DeviceActivationRequest.KEY_ACTIVATION_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Testing registration redirection happens correctly on the activity.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = com.vmware.idm.samples.appauth.BuildConfig.class, sdk = 16, manifest = "src/main/AndroidManifest.xml")
public class RedirectUriRegistrationReceiverActivityTest extends VMwareAppAuthTest {

    private DeviceRegistrationRequest mRequest;

    @Mock
    PendingIntent mPendingIntent;

    private static final Intent CODE_INTENT;

    private static final Uri CODE_URI = new Uri.Builder()
            .scheme(TEST_APP_SCHEME)
            .appendQueryParameter(AuthorizationResponse.KEY_STATE, TEST_STATE)
            .appendQueryParameter(AuthorizationResponse.KEY_AUTHORIZATION_CODE,
                    TEST_AUTHORIZATION_CODE)
            .appendQueryParameter(KEY_ACTIVATION_CODE, TEST_ACTIVATION_CODE)
            .build();

    static {
        CODE_INTENT = new Intent();
        CODE_INTENT.setData(CODE_URI);
    }


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        DeviceRegistrationPendingIntentStore.getInstance().clearAll();
        mRequest = getTestDeviceRegistrationRequestBuilder()
                .setState(TEST_STATE)
                .build();
    }

    @After
    public void tearDown() {
        DeviceRegistrationPendingIntentStore.getInstance().clearAll();
    }

    @Test
    public void testRedirectUriActivity() throws Exception {
        DeviceRegistrationPendingIntentStore.getInstance()
                .addPendingIntent(mRequest, mPendingIntent);
        RedirectUriRegistrationReceiverActivity activity = Robolectric
                .buildActivity(RedirectUriRegistrationReceiverActivity.class)
                .withIntent(CODE_INTENT)
                .create()
                .get();

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mPendingIntent).send(eq(activity), anyInt(), intentCaptor.capture());

        Intent resultIntent = intentCaptor.getValue();
        DeviceRegistrationResponse response = DeviceRegistrationResponse.fromIntent(resultIntent);
        assertEquals(TEST_STATE, response.state);
        assertEquals(TEST_AUTHORIZATION_CODE, response.authorizationCode);
        assertEquals(TEST_ACTIVATION_CODE, response.activationCode);
        assertTrue(activity.isFinishing());
    }


    @Test
    public void testRedirectUriActivityWithMissingPendingIntent() throws Exception {
        RedirectUriRegistrationReceiverActivity activity = Robolectric
                .buildActivity(RedirectUriRegistrationReceiverActivity.class)
                .withIntent(CODE_INTENT)
                .create()
                .get();

        // no pending intent found, redirect uri should be ignored and activity should finish
        assertTrue(activity.isFinishing());
    }

    @Test
    public void testRedirectUriActivityWithCanceledPendingIntent() throws Exception {
        DeviceRegistrationPendingIntentStore.getInstance()
                .addPendingIntent(mRequest, mPendingIntent);
        doThrow(new PendingIntent.CanceledException()).when(mPendingIntent)
                .send(any(Context.class), anyInt(), any(Intent.class));
        RedirectUriRegistrationReceiverActivity activity = Robolectric
                .buildActivity(RedirectUriRegistrationReceiverActivity.class)
                .withIntent(CODE_INTENT)
                .create()
                .get();

        // exception thrown when trying to send pending intent, activity should finish
        assertTrue(activity.isFinishing());
    }


}
