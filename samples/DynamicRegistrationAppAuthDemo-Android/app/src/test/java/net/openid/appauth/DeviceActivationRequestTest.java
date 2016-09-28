package net.openid.appauth;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

/**
 * Unit tests for the activation request.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = com.vmware.idm.samples.appauth.BuildConfig.class, sdk = 16)
public class DeviceActivationRequestTest extends VMwareAppAuthTest {

    private static final String TEST_JSON = "{\n"
            + " \"activation_code\": \"" + TEST_ACTIVATION_CODE + "\",\n"
            + " \"endpoint\": \"" + TEST_IDP_ACTIVATION_ENDPOINT + "\"\n"
            + "}";
    private DeviceActivationRequest.Builder mRequestBuilder;

    private JSONObject mJson;


    @Before
    public void setUp() throws Exception {
        mRequestBuilder =
                new DeviceActivationRequest.Builder(TEST_IDP_ACTIVATION_ENDPOINT,
                        getTestServiceConfig(),
                        TEST_ACTIVATION_CODE);
        mJson = new JSONObject(TEST_JSON);
    }

    @Test
    public void testBuilder() {
        assertValues(mRequestBuilder.build());
    }

    @Test
    public void jsonDeserialize() throws Exception {
        mJson.put(DeviceRegistrationRequest.KEY_CONFIGURATION, getTestServiceConfig().toJson());
        DeviceActivationRequest request = DeviceActivationRequest.jsonDeserialize(mJson);
        assertThat(request.configuration.toJsonString())
                .isEqualTo(getTestServiceConfig().toJsonString());
        assertMaximalValuesInJson(request, mJson);
    }

    @Test
    public void getActivationCode() throws Exception {
        DeviceActivationRequest request = mRequestBuilder.build();
        assertThat(request.getActivationCode()).isEqualTo(TEST_ACTIVATION_CODE);
    }

    @Test
    public void getActivationEndpoint() throws Exception {
        DeviceActivationRequest request = mRequestBuilder.build();
        assertThat(request.getActivationEndpoint()).isEqualTo(TEST_IDP_ACTIVATION_ENDPOINT);
    }

    @Test
    public void toRegistrationRequest() throws Exception {
        DeviceActivationRequest request = mRequestBuilder.build();
        RegistrationRequest registrationRequest = request.toRegistrationRequest();

        assertThat(registrationRequest.configuration.toJsonString())
                .isEqualTo(getTestServiceConfig().toJsonString());
        assertThat(registrationRequest.redirectUris.iterator().next())
                .isEqualTo(Uri.parse("redirect_uri_unused"));
    }

    @Test
    public void testJsonSerialize() throws Exception {
        DeviceActivationRequest request = mRequestBuilder.build();
        JSONObject json = request.jsonSerialize();
        assertMaximalValuesInJson(request, json);
        assertThat(json.getJSONObject(DeviceRegistrationRequest.KEY_CONFIGURATION).toString())
                .isEqualTo(request.configuration.toJson().toString());
    }

    private void assertValues(DeviceActivationRequest request) {
        assertEquals("unexpected activation URI", TEST_IDP_ACTIVATION_ENDPOINT.toString(),
                request.activationEndpoint.toString());
        assertEquals("unexpected activation code", TEST_ACTIVATION_CODE,
                request.activationCode);
    }

    private void assertMaximalValuesInJson(DeviceActivationRequest request, JSONObject json)
            throws JSONException {
        assertThat(json.get(DeviceActivationRequest.KEY_ENDPOINT))
                .isEqualTo(request.activationEndpoint.toString());
        assertThat(json.get(DeviceActivationRequest.KEY_ACTIVATION_CODE))
                .isEqualTo(request.activationCode);


    }
}