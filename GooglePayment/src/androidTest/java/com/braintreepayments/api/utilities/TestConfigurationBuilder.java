package com.braintreepayments.api.utilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class TestConfigurationBuilder extends JSONBuilder {

    public static <T> T basicConfig() {
        return new TestConfigurationBuilder().buildConfiguration();
    }

    public TestConfigurationBuilder() {
        super();
        clientApiUrl("client_api_url");
        environment("test");
        merchantId("integration_merchant_id");
    }

    public TestConfigurationBuilder clientApiUrl(String clientApiUrl) {
        put(clientApiUrl);
        return this;
    }

    public TestConfigurationBuilder environment(String environment) {
        put(environment);
        return this;
    }

    public TestConfigurationBuilder merchantId(String merchantId) {
        put(merchantId);
        return this;
    }

    public TestConfigurationBuilder withAnalytics() {
        analytics("http://example.com");
        return this;
    }

    public TestConfigurationBuilder analytics(String analyticsUrl) {
        try {
            JSONObject analyticsJson = new JSONObject();
            analyticsJson.put("url", analyticsUrl);
            put(analyticsJson);
        } catch (JSONException ignored) {}
        return this;
    }

    public TestConfigurationBuilder googlePayment(TestGooglePaymentConfigurationBuilder builder) {
        try {
            put("androidPay", new JSONObject(builder.build()));
        } catch (JSONException ignored) {}
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T buildConfiguration() {
        try {
            Class configuration = Class.forName("com.braintreepayments.api.models.Configuration");
            Method fromJson = configuration.getDeclaredMethod("fromJson", String.class);
            return (T) fromJson.invoke(null, build());
        } catch (NoSuchMethodException ignored) {}
        catch (InvocationTargetException ignored) {}
        catch (IllegalAccessException ignored) {}
        catch (ClassNotFoundException ignored) {}

        return (T) build();
    }

    public TestGooglePaymentConfigurationBuilder googlePayment() {
        try {
            return new TestGooglePaymentConfigurationBuilder(mJsonBody.getJSONObject("androidPay"));
        } catch (JSONException ignored) {}
        return new TestGooglePaymentConfigurationBuilder();
    }

    public static class TestGooglePaymentConfigurationBuilder extends JSONBuilder {

        public TestGooglePaymentConfigurationBuilder() {
            super();
        }

        protected TestGooglePaymentConfigurationBuilder(JSONObject json) {
            super(json);
        }

        public TestGooglePaymentConfigurationBuilder enabled(boolean enabled) {
            put(Boolean.toString(enabled));
            return this;
        }

        public TestGooglePaymentConfigurationBuilder googleAuthorizationFingerprint(String fingerprint) {
            put(fingerprint);
            return this;
        }

        public TestGooglePaymentConfigurationBuilder environment(String environment) {
            put(environment);
            return this;
        }

        public TestGooglePaymentConfigurationBuilder displayName(String dislayName) {
            put(dislayName);
            return this;
        }

        public TestGooglePaymentConfigurationBuilder supportedNetworks(String[] supportedNetworks) {
            put(new JSONArray(Arrays.asList(supportedNetworks)));
            return this;
        }

        public TestGooglePaymentConfigurationBuilder paypalClientId(String paypalClientId) {
            put(paypalClientId);
            return this;
        }
    }
}
