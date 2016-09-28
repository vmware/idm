package net.openid.appauth;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests of the calls to perform VMware Identity Manager mobile dynamic registration.
 * Based on @{link AuthorizationServiceTest}.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = com.vmware.idm.samples.appauth.BuildConfig.class, sdk = 16, manifest = "src/main/AndroidManifest.xml")
public class DeviceAuthorizationServiceTest extends VMwareAppAuthTest {

    private static final int CALLBACK_TIMEOUT_MILLIS = 1000;
    private static final String TEST_BROWSER_PACKAGE = "com.browser.test";

    private static final String ACTIVATION_CODE_EXCHANGE_RESPONSE_JSON = "{" +
            "\"client_id\": \"" + TEST_CLIENT_ID + "\"," +
            "\"client_secret\": \"" + TEST_CLIENT_SECRET + "\"" +
            "}";
    private URL mUrl;

    @Mock
    HttpURLConnection mHttpConnection;
    @Mock
    PendingIntent mPendingIntent;
    @Mock
    Context mContext;
    @Mock
    CustomTabsClient mClient;
    @Mock
    BrowserHandler mBrowserHandler;

    /**
     * Service under test.
     */
    private DeviceAuthorizationService mService;

    private ByteArrayOutputStream mOutputStream;
    private InjectedUrlBuilder mBuilder;
    private DeviceActivationResponseCallbackTest mActivationCallback;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        DeviceRegistrationPendingIntentStore.getInstance().clearAll();
        URLStreamHandler urlStreamHandler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) throws IOException {
                return mHttpConnection;
            }
        };
        mUrl = new URL("foo", "bar", -1, "/foobar", urlStreamHandler);
        mActivationCallback = new DeviceActivationResponseCallbackTest();
        mBuilder = new InjectedUrlBuilder();
        mService = new DeviceAuthorizationService(mContext, mBuilder, mBrowserHandler);
        mOutputStream = new ByteArrayOutputStream();
        when(mHttpConnection.getOutputStream()).thenReturn(mOutputStream);
        //when(mContext.bindService(serviceIntentEq(), any(CustomTabsServiceConnection.class),
        //         anyInt())).thenReturn(true);
        when(mBrowserHandler.createCustomTabsIntentBuilder())
                .thenReturn(new CustomTabsIntent.Builder());
        when(mBrowserHandler.getBrowserPackage()).thenReturn(TEST_BROWSER_PACKAGE);

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testDeviceRegistrationRequest() throws Exception {
        DeviceRegistrationRequest request = getTestDeviceRegistrationRequestBuilder().build();
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                .setToolbarColor(Color.GREEN)
                .build();

        mService.performDeviceRegistrationRequest(request, mPendingIntent, customTabsIntent);
        Intent intent = captureRegistrationRequestIntent();
        assertColorMatch(intent, Color.GREEN);
    }

    @Test
    public void testActivationRequest() throws Exception {
        InputStream is =
                new ByteArrayInputStream(ACTIVATION_CODE_EXCHANGE_RESPONSE_JSON.getBytes());
        when(mHttpConnection.getInputStream()).thenReturn(is);

        DeviceActivationRequest request = getTestDeviceActivationRequest();
        mService.performActivationRequest(request, mActivationCallback);
        mActivationCallback.waitForCallback();

        assertRegistrationResponse(mActivationCallback.response);
        String postBody = mOutputStream.toString();
        assertThat(postBody).isEqualTo(request.activationCode);
        assertEquals(TEST_IDP_ACTIVATION_ENDPOINT.toString(), mBuilder.mUri);
    }

    @Test
    public void testActivationRequestDoesNotReturn200() throws Exception {

        Exception ex = new IOException();
        when(mHttpConnection.getInputStream()).thenThrow(ex);

        DeviceActivationRequest request = getTestDeviceActivationRequest();
        mService.performActivationRequest(request, mActivationCallback);
        mActivationCallback.waitForCallback();

        assertNotNull(mActivationCallback.error);
        assertEquals(AuthorizationException.GeneralErrors.NETWORK_ERROR, mActivationCallback.error);
    }

    @Test
    public void testActivationRequestDoesNotReturnValidJson() throws Exception {
        InputStream is =
                new ByteArrayInputStream("{".getBytes());
        when(mHttpConnection.getInputStream()).thenReturn(is);

        DeviceActivationRequest request = getTestDeviceActivationRequest();
        mService.performActivationRequest(request, mActivationCallback);
        mActivationCallback.waitForCallback();

        assertNotNull(mActivationCallback.error);
        assertEquals(AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR,
                mActivationCallback.error);
    }

    @Test
    public void testActivationRequestDoesNotReturnValidActivationJsonResponse() throws Exception {
        InputStream is =
                new ByteArrayInputStream("{\"foo\":\"bar\"}".getBytes());
        when(mHttpConnection.getInputStream()).thenReturn(is);

        DeviceActivationRequest request = getTestDeviceActivationRequest();
        mService.performActivationRequest(request, mActivationCallback);
        mActivationCallback.waitForCallback();

        assertNotNull(mActivationCallback.error);
        assertEquals(AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR,
                mActivationCallback.error);
    }

    private void assertRegistrationResponse(RegistrationResponse response) {
        assertNotNull(response);
        assertEquals(TEST_CLIENT_ID, response.clientId);
        assertEquals(TEST_CLIENT_SECRET, response.clientSecret);
    }


    private void assertColorMatch(Intent intent, Integer expected) {
        int color = intent.getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, Color.TRANSPARENT);
        assertTrue((expected == null) || ((expected == color) && (color != Color.TRANSPARENT)));
    }

    private Intent captureRegistrationRequestIntent() {
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(intentCaptor.capture());
        return intentCaptor.getValue();
    }

    private class InjectedUrlBuilder implements AuthorizationService.UrlBuilder {
        public String mUri;

        public URL buildUrlFromString(String uri) throws IOException {
            mUri = uri;
            return mUrl;
        }
    }

    /* Capture activation callback response */
    private class DeviceActivationResponseCallbackTest
            implements DeviceAuthorizationService.DeviceActivationResponseCallback {
        private Semaphore mSemaphore = new Semaphore(0);
        public RegistrationResponse response;
        public AuthorizationException error;

        @Override
        public void onActivationRequestCompleted(@Nullable RegistrationResponse response,
                                                 @Nullable AuthorizationException ex) {
            assertTrue((response == null) ^ (ex == null));
            this.response = response;
            this.error = ex;
            mSemaphore.release();
        }

        public void waitForCallback() throws Exception {
            assertTrue(mSemaphore.tryAcquire(CALLBACK_TIMEOUT_MILLIS,
                    TimeUnit.MILLISECONDS));
        }

    }
}