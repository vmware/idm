package net.openid.appauth;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static net.openid.appauth.DeviceActivationResponse.PARAM_CLIENT_ID;
import static net.openid.appauth.DeviceActivationResponse.PARAM_CLIENT_SECRET;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for the @{link DeviceActivationResponse} class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = com.vmware.idm.samples.appauth.BuildConfig.class, sdk = 16)
public class DeviceActivationResponseTest extends VMwareAppAuthTest {

    private static final Object TEST_CLIENT_ID = "client_id";
    private static final Object TEST_CLIENT_SECRET = "client_secret";

    private static final String TEST_JSON = "{\n"
            + " \"client_id\": \"" + TEST_CLIENT_ID + "\"}";

    private DeviceActivationResponse.Builder mMinimalBuilder;
    private JSONObject mJson;

    @Before
    public void setUp() throws Exception {
        mJson = new JSONObject(TEST_JSON);
        mMinimalBuilder =
                new DeviceActivationResponse.Builder(getTestDeviceActivationRequest());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderSetEmptyClientId() {
        mMinimalBuilder.setClientId("");
    }

    @Test
    public void testFromJson() throws Exception {
        DeviceActivationResponse response =
                new DeviceActivationResponse.Builder(getTestDeviceActivationRequest())
                        .fromResponseJson(mJson).build();
        assertThat(response.clientId).isEqualTo(TEST_CLIENT_ID);
    }

    @Test
    public void testFromJsonWithClientSecret() throws Exception {
        mJson.put(PARAM_CLIENT_SECRET, TEST_CLIENT_SECRET);
        DeviceActivationResponse response =
                new DeviceActivationResponse.Builder(getTestDeviceActivationRequest())
                        .fromResponseJson(mJson).build();

        assertThat(response.clientId).isEqualTo(TEST_CLIENT_ID);
        assertThat(response.clientSecret).isEqualTo(TEST_CLIENT_SECRET);
    }


}