package net.openid.appauth;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * Unit tests for the device registration request against VMware Identity Manager.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = com.vmware.idm.samples.appauth.BuildConfig.class, sdk = 16)
public class DeviceRegistrationRequestTest extends VMwareAppAuthTest {


    private static final String TEST_JSON = "{\n"
            + " \"app_product_id\": \"" + TEST_APP_TEMPLATE + "\",\n"
            + " \"redirect_uri\": \"" + TEST_APP_REDIRECT_URI + "\",\n"
            + " \"device_name\": \"" + TEST_DEVICE_NAME + "\",\n"
            + " \"user_device\": \"{ \\\"info\\\" : \\\"user_device\\\" }\",\n"
            + " \"type\": \"" + DeviceRegistrationRequest.TYPE_REGISTER + "\",\n"
            + " \"state\": \"" + TEST_STATE + "\",\n"
            + " \"scope\": \"" + TEST_SCOPE + "\",\n"
            + " \"response_type\": \"" + DeviceRegistrationRequest.RESPONSE_TYPE_CODE + "\",\n"
            + " \"activation_uri\": \"" + TEST_IDP_ACTIVATION_ENDPOINT + "\"\n"
            + "}";

    private DeviceRegistrationRequest.Builder mMinimalRequestBuilder;
    private DeviceRegistrationRequest.Builder mMaximalRequestBuilder;

    private JSONObject mJson;

    @Before
    public void setUp() throws JSONException {
        mMinimalRequestBuilder = getTestDeviceRegistrationRequestBuilder();
        mMaximalRequestBuilder =
                getTestDeviceRegistrationRequestBuilder().setScope(TEST_SCOPE).setState(TEST_STATE);
        mJson = new JSONObject(TEST_JSON);
    }


    @Test
    public void testBuilder() {
        assertValues(mMinimalRequestBuilder.build());
    }

    @Test
    public void testToUri() throws Exception {
        DeviceRegistrationRequest request = mMaximalRequestBuilder.build();
        Uri requestUri = request.toUri();
        assertThat(requestUri.toString()).isEqualTo(
                TEST_IDP_REGISTRATION_ENDPOINT + "?" +
                        "redirect_uri=" + Uri.encode(TEST_APP_REDIRECT_URI.toString()) +
                        "&app_product_id=" + Uri.encode(TEST_APP_TEMPLATE) +
                        "&device_name=" + Uri.encode(TEST_DEVICE_NAME) +
                        "&user_device=" + Uri.encode(TEST_USER_DEVICE_JSON) +
                        "&response_type=" + DeviceRegistrationRequest.RESPONSE_TYPE_CODE +
                        "&type=" + DeviceRegistrationRequest.TYPE_REGISTER +
                        "&state=" + Uri.encode(TEST_STATE) +
                        "&scope=" + Uri.encode(TEST_SCOPE)
        );


    }

    @Test
    public void testSerialize() throws JSONException {
        DeviceRegistrationRequest request = mMaximalRequestBuilder.build();
        JSONObject json = request.jsonSerialize();
        assertMaximalValuesInJson(request, json);
        assertThat(json.getJSONObject(DeviceRegistrationRequest.KEY_CONFIGURATION).toString())
                .isEqualTo(request.configuration.toJson().toString());
    }

    @Test
    public void testJsonDeserialize() throws Exception {
        mJson.put(DeviceRegistrationRequest.KEY_CONFIGURATION, getTestServiceConfig().toJson());
        DeviceRegistrationRequest request = DeviceRegistrationRequest.jsonDeserialize(mJson);
        assertThat(request.configuration.toJsonString())
                .isEqualTo(getTestServiceConfig().toJsonString());
        assertMaximalValuesInJson(request, mJson);
    }

    @Test
    public void testToAuthorizationRequest() throws Exception {
        AuthorizationRequest request =
                mMaximalRequestBuilder.build().toAuthorizationRequest("client_id");
        assertEquals("unexpected client id", "client_id", request.clientId);


    }

    private void assertValues(DeviceRegistrationRequest request) {
        assertEquals("unexpected redirect URI", TEST_APP_REDIRECT_URI.toString(),
                request.redirectUri.toString());
        assertEquals("unexpected register type", DeviceRegistrationRequest.TYPE_REGISTER,
                request.registerType);
        assertEquals("unexpected response type", DeviceRegistrationRequest.RESPONSE_TYPE_CODE,
                request.responseType);
        assertEquals("unexpected device name", TEST_DEVICE_NAME,
                request.deviceName);
        assertEquals("unexpected user's device info", TEST_USER_DEVICE_JSON,
                request.userDevice);
        assertEquals("unexpected user's device info", TEST_APP_TEMPLATE,
                request.appProductId);

    }

    private void assertMaximalValuesInJson(DeviceRegistrationRequest request, JSONObject json)
            throws JSONException {
        assertThat(json.get(DeviceRegistrationRequest.PARAM_REDIRECT_URI))
                .isEqualTo(request.redirectUri.toString());
        assertThat(json.get(DeviceRegistrationRequest.PARAM_RESPONSE_TYPE))
                .isEqualTo(DeviceRegistrationRequest.RESPONSE_TYPE_CODE);
        assertThat(json.get(DeviceRegistrationRequest.PARAM_TYPE))
                .isEqualTo(DeviceRegistrationRequest.TYPE_REGISTER);
        assertThat(json.get(DeviceRegistrationRequest.PARAM_DEVICE_NAME))
                .isEqualTo(request.deviceName);
        assertThat(json.get(DeviceRegistrationRequest.PARAM_ACTIVATION_URI.toString()))
                .isEqualTo(request.activationEndpointUri.toString());
        assertThat(json.get(DeviceRegistrationRequest.PARAM_APP_PRODUCT_ID))
                .isEqualTo(request.appProductId);
        assertThat(json.get(DeviceRegistrationRequest.PARAM_SCOPE))
                .isEqualTo(request.scope);
        assertThat(json.get(DeviceRegistrationRequest.PARAM_STATE))
                .isEqualTo(request.state);
        assertThat(json.get(DeviceRegistrationRequest.PARAM_USER_DEVICE))
                .isEqualTo(request.userDevice);

    }

}