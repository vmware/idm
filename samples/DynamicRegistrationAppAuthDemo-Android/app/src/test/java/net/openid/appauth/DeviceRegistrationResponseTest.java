package net.openid.appauth;

import android.content.Intent;
import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static net.openid.appauth.AuthorizationException.KEY_CODE;
import static net.openid.appauth.AuthorizationResponse.KEY_STATE;
import static net.openid.appauth.DeviceActivationRequest.KEY_ACTIVATION_CODE;
import static net.openid.appauth.DeviceRegistrationResponse.EXTRA_RESPONSE;
import static net.openid.appauth.DeviceRegistrationResponse.KEY_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DeviceRegistrationResponse}
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = com.vmware.idm.samples.appauth.BuildConfig.class, sdk = 16)
public class DeviceRegistrationResponseTest extends VMwareAppAuthTest {

    private static final String TEST_JSON = "{\n"
            + " \"code\": \"" + TEST_AUTHORIZATION_CODE + "\",\n"
            + " \"activation_code\": \"" + TEST_ACTIVATION_CODE + "\",\n"
            + " \"state\": \"" + TEST_STATE + "\"\n"
            + "}";

    private static final Uri TEST_REGISTRATION_REDIRECT_URI =
            Uri.parse(
                    "scheme://my-redirect-uri?activation_code=" + TEST_ACTIVATION_CODE + "&code=" +
                            TEST_AUTHORIZATION_CODE + "&state=" + TEST_STATE);

    private JSONObject mJson;

    @Before
    public void setUp() throws Exception {
        mJson = new JSONObject(TEST_JSON);
    }

    @Test
    public void fromUri() throws Exception {
        DeviceRegistrationResponse response = DeviceRegistrationResponse
                .fromUri(getTestDeviceRegistrationRequestBuilder().build(),
                        TEST_REGISTRATION_REDIRECT_URI);
        assertThat(response.activationCode).isEqualTo(TEST_ACTIVATION_CODE);
        assertThat(response.authorizationCode).isEqualTo(TEST_AUTHORIZATION_CODE);
        assertThat(response.state).isEqualTo(TEST_STATE);
    }

    @Test
    public void fromUriWithNoParams() throws Exception {
        DeviceRegistrationResponse response = DeviceRegistrationResponse
                .fromUri(getTestDeviceRegistrationRequestBuilder().build(),
                        Uri.parse("uri-with-no-params"));
        assertThat(response.activationCode).isNull();
        assertThat(response.authorizationCode).isNull();
        assertThat(response.state).isNull();
    }

    @Test
    public void toIntent() throws Exception {
        DeviceRegistrationResponse response = DeviceRegistrationResponse
                .fromUri(getTestDeviceRegistrationRequestBuilder().build(),
                        TEST_REGISTRATION_REDIRECT_URI);
        Intent intent = response.toIntent();

        assertThat(intent.getStringExtra(EXTRA_RESPONSE)).isNotNull();
        JSONObject json = new JSONObject(intent.getStringExtra(EXTRA_RESPONSE));

        assertThat(json.getJSONObject(KEY_REQUEST).toString())
                .isEqualTo(response.request.jsonSerialize().toString());
        assertThat(json.getString(KEY_CODE)).isEqualTo(TEST_AUTHORIZATION_CODE);
        assertThat(json.getString(KEY_ACTIVATION_CODE)).isEqualTo(TEST_ACTIVATION_CODE);
        assertThat(json.getString(KEY_STATE)).isEqualTo(TEST_STATE);
    }

    @Test
    public void fromIntent() throws Exception {
        Intent intent = new Intent();
        DeviceRegistrationRequest request = getTestDeviceRegistrationRequestBuilder().build();
        mJson.put(KEY_REQUEST, request.jsonSerialize());
        intent.putExtra(EXTRA_RESPONSE, mJson.toString());
        DeviceRegistrationResponse response = DeviceRegistrationResponse
                .fromIntent(intent);

        assertThat(response.activationCode).isEqualTo(TEST_ACTIVATION_CODE);
        assertThat(response.authorizationCode).isEqualTo(TEST_AUTHORIZATION_CODE);
        assertThat(response.state).isEqualTo(TEST_STATE);
        assertThat(response.request.jsonSerialize().toString())
                .isEqualTo(request.jsonSerialize().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromIntentWithoutRequestThrowsException() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_RESPONSE, mJson.toString());
        DeviceRegistrationResponse.fromIntent(intent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromIntentWithBadExtraThrowsException() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_RESPONSE, "{badJson}");
        DeviceRegistrationResponse.fromIntent(intent);
    }

    @Test
    public void fromIntentWithNoExtraReturnsNull() throws Exception {
        DeviceRegistrationResponse response = DeviceRegistrationResponse.fromIntent(new Intent());
        assertThat(response).isNull();
    }


    @Test
    public void createDeviceActivationRequest() throws Exception {
        DeviceRegistrationResponse response = DeviceRegistrationResponse
                .fromUri(getTestDeviceRegistrationRequestBuilder().build(),
                        TEST_REGISTRATION_REDIRECT_URI);
        DeviceActivationRequest request =
                response.createDeviceActivationRequest(TEST_IDP_ACTIVATION_ENDPOINT);
        assertThat(request.activationCode).isEqualTo(TEST_ACTIVATION_CODE);
        assertThat(request.activationEndpoint).isEqualTo(TEST_IDP_ACTIVATION_ENDPOINT);
    }

    @Test(expected = IllegalStateException.class)
    public void createDeviceActivationRequestWithNoActivationCode() throws Exception {
        DeviceRegistrationResponse response = DeviceRegistrationResponse
                .fromUri(getTestDeviceRegistrationRequestBuilder().build(),
                        Uri.parse("scheme://my-redirect-uri-no-params"));
        response.createDeviceActivationRequest(TEST_IDP_ACTIVATION_ENDPOINT);
    }

    @Test
    public void toAuthorizationResponse() throws Exception {
        DeviceRegistrationResponse response = DeviceRegistrationResponse
                .fromUri(getTestDeviceRegistrationRequestBuilder().build(),
                        TEST_REGISTRATION_REDIRECT_URI);
        AuthorizationResponse authResponse = response.toAuthorizationResponse(TEST_CLIENT_ID);
        assertThat(authResponse.authorizationCode).isEqualTo(TEST_AUTHORIZATION_CODE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toAuthorizationResponseWithNoClientId() throws Exception {
        DeviceRegistrationResponse response = DeviceRegistrationResponse
                .fromUri(getTestDeviceRegistrationRequestBuilder().build(),
                        TEST_REGISTRATION_REDIRECT_URI);
        response.toAuthorizationResponse("");
    }
}