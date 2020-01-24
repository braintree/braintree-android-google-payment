package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;

import androidx.annotation.NonNull;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.internal.ManifestValidator;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.GooglePaymentCardNonce;
import com.braintreepayments.api.models.GooglePaymentRequest;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.ReadyForGooglePayRequest;
import com.braintreepayments.api.test.FixturesHelper;
import com.braintreepayments.api.test.TestConfigurationBuilder;
import com.braintreepayments.api.test.TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import static com.braintreepayments.api.GooglePaymentActivity.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.GooglePaymentActivity.EXTRA_PAYMENT_DATA_REQUEST;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(RobolectricTestRunner.class)
@PrepareForTest({GoogleApiAvailability.class, ManifestValidator.class, Wallet.class})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
public class GooglePaymentUnitTest {
    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private GooglePaymentRequest mBaseRequest;
    private BraintreeFragment mMockFragment;

    @Before
    public void setup() {
        mBaseRequest = new GooglePaymentRequest()
                .transactionInfo(TransactionInfo.newBuilder()
                        .setTotalPrice("1.00")
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .setCurrencyCode("USD")
                        .build());

        String configuration = new TestConfigurationBuilder()
                .googlePayment(new TestGooglePaymentConfigurationBuilder()
                        .supportedNetworks(new String[]{"AMEX", "VISA"})
                        .enabled(true))
                .build();

        mMockFragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();

        GoogleApiAvailability mockGoogleApiAvailability = mock(GoogleApiAvailability.class);
        when(mockGoogleApiAvailability.isGooglePlayServicesAvailable(any(Context.class))).thenReturn(ConnectionResult.SUCCESS);

        mockStatic(GoogleApiAvailability.class);
        when(GoogleApiAvailability.getInstance()).thenReturn(mockGoogleApiAvailability);

        ActivityInfo mockActivityInfo = mock(ActivityInfo.class);
        when(mockActivityInfo.getThemeResource()).thenReturn(2132083045);

        mockStatic(ManifestValidator.class);
        when(ManifestValidator.getActivityInfo(any(Context.class), any(Class.class))).thenReturn(mockActivityInfo);
    }

    @Test
    public void isReadyToPay_sendsIsReadyToPayRequest() throws JSONException {
        PaymentsClient mockPaymentsClient = mock(PaymentsClient.class);
        when(mockPaymentsClient.isReadyToPay(any(IsReadyToPayRequest.class))).thenReturn(Tasks.forResult(true));

        mockStatic(Wallet.class);
        when(Wallet.getPaymentsClient(any(Activity.class), any(Wallet.WalletOptions.class))).thenReturn(mockPaymentsClient);

        GooglePayment.isReadyToPay(mMockFragment, null, new BraintreeResponseListener<Boolean>() {
            @Override
            public void onResponse(Boolean aBoolean) {
                // do nothing
            }
        });

        ArgumentCaptor<IsReadyToPayRequest> captor = ArgumentCaptor.forClass(IsReadyToPayRequest.class);
        verify(mockPaymentsClient).isReadyToPay(captor.capture());
        String actualJson = captor.getValue().toJson();

        String expectedJson = "{\n" +
                "  \"apiVersion\": 2,\n" +
                "  \"apiVersionMinor\": 0,\n" +
                "  \"allowedPaymentMethods\": [\n" +
                "    {\n" +
                "      \"type\": \"CARD\",\n" +
                "      \"parameters\": {\n" +
                "        \"allowedAuthMethods\": [\n" +
                "          \"PAN_ONLY\",\n" +
                "          \"CRYPTOGRAM_3DS\"\n" +
                "        ],\n" +
                "        \"allowedCardNetworks\": [\n" +
                "          \"AMEX\",\n" +
                "          \"VISA\"\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        JSONAssert.assertEquals(expectedJson, actualJson, false);
    }

    @Test
    public void isReadyToPay_whenExistingPaymentMethodRequired_sendsIsReadyToPayRequestWithExistingPaymentRequired() throws JSONException {
        PaymentsClient mockPaymentsClient = mock(PaymentsClient.class);
        when(mockPaymentsClient.isReadyToPay(any(IsReadyToPayRequest.class))).thenReturn(Tasks.forResult(true));

        mockStatic(Wallet.class);
        when(Wallet.getPaymentsClient(any(Activity.class), any(Wallet.WalletOptions.class))).thenReturn(mockPaymentsClient);

        ReadyForGooglePayRequest readyForGooglePayRequest = new ReadyForGooglePayRequest().existingPaymentMethodRequired(true);
        GooglePayment.isReadyToPay(mMockFragment, readyForGooglePayRequest, new BraintreeResponseListener<Boolean>() {
            @Override
            public void onResponse(Boolean aBoolean) {
                // do nothing
            }
        });

        ArgumentCaptor<IsReadyToPayRequest> captor = ArgumentCaptor.forClass(IsReadyToPayRequest.class);
        verify(mockPaymentsClient).isReadyToPay(captor.capture());
        String actualJson = captor.getValue().toJson();

        String expectedJson = "{\n" +
                "  \"apiVersion\": 2,\n" +
                "  \"apiVersionMinor\": 0,\n" +
                "  \"allowedPaymentMethods\": [\n" +
                "    {\n" +
                "      \"type\": \"CARD\",\n" +
                "      \"parameters\": {\n" +
                "        \"allowedAuthMethods\": [\n" +
                "          \"PAN_ONLY\",\n" +
                "          \"CRYPTOGRAM_3DS\"\n" +
                "        ],\n" +
                "        \"allowedCardNetworks\": [\n" +
                "          \"AMEX\",\n" +
                "          \"VISA\"\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"existingPaymentMethodRequired\": true\n" +
                "}";

        JSONAssert.assertEquals(expectedJson, actualJson, false);
    }

    @Test
    public void requestPayment_whenMerchantNotConfigured_returnsExceptionToFragment() {
        String configuration = new TestConfigurationBuilder().build();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();

        GooglePayment.requestPayment(fragment, mBaseRequest);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(fragment).postCallback(captor.capture());
        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals("Google Pay enabled is not enabled for your Braintree account, or Google Play Services are not configured correctly.",
                captor.getValue().getMessage());
    }

    @Test
    public void requestPayment_whenSandbox_setsTestEnvironment() throws JSONException {
        BraintreeFragment fragment = getSetupFragment("sandbox");

        GooglePayment.requestPayment(fragment, mBaseRequest);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals(WalletConstants.ENVIRONMENT_TEST, intent.getIntExtra(EXTRA_ENVIRONMENT, -1));
        assertEquals("TEST", paymentDataRequestJson.getString("environment"));
    }

    @Test
    public void requestPayment_whenProduction_setsProductionEnvironment() throws JSONException {
        BraintreeFragment fragment = getSetupFragment("production");

        GooglePayment.requestPayment(fragment, mBaseRequest);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals(WalletConstants.ENVIRONMENT_PRODUCTION, intent.getIntExtra(EXTRA_ENVIRONMENT, -1));
        assertEquals("PRODUCTION", paymentDataRequestJson.getString("environment"));
    }

    @Test
    public void requestPayment_withGoogleMerchantId_sendGoogleMerchantId() throws JSONException {
        BraintreeFragment fragment = getSetupFragment();
        GooglePaymentRequest googlePaymentRequest = mBaseRequest
                .googleMerchantId("google-merchant-id-override");

        GooglePayment.requestPayment(fragment, googlePaymentRequest);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals("google-merchant-id-override", paymentDataRequestJson
                .getJSONObject("merchantInfo")
                .getString("merchantId"));
    }

    @Test
    public void requestPayment_withGoogleMerchantName_sendGoogleMerchantName() throws JSONException {
        BraintreeFragment fragment = getSetupFragment();
        GooglePaymentRequest googlePaymentRequest = mBaseRequest
                .googleMerchantName("google-merchant-name-override");

        GooglePayment.requestPayment(fragment, googlePaymentRequest);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals("google-merchant-name-override", paymentDataRequestJson
                .getJSONObject("merchantInfo")
                .getString("merchantName"));
    }

    @Test
    public void requestPayment_whenGooglePayCanProcessPayPal_tokenizationPropertiesIncludePayPal() throws JSONException {
        BraintreeFragment fragment = getSetupFragment();
        GooglePaymentRequest googlePaymentRequest = mBaseRequest
                .googleMerchantName("google-merchant-name-override");

        GooglePayment.requestPayment(fragment, googlePaymentRequest);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePayment(fragment)
                .getJSONArray("allowedPaymentMethods");

        assertEquals(2, allowedPaymentMethods.length());
        assertEquals("PAYPAL", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(1)
                .getString("type"));
    }

    @Test
    public void requestPayment_whenPayPalDisabledByRequest_tokenizationPropertiesLackPayPal() throws JSONException {
        BraintreeFragment fragment = getSetupFragment();
        GooglePaymentRequest googlePaymentRequest = mBaseRequest
                .googleMerchantName("google-merchant-name-override")
                .paypalEnabled(false);

        GooglePayment.requestPayment(fragment, googlePaymentRequest);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePayment(fragment)
                .getJSONArray("allowedPaymentMethods");

        assertEquals(1, allowedPaymentMethods.length());
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));
    }

    @Test
    public void requestPayment_whenPayPalDisabledInConfigurationAndGooglePayHasPayPalClientId_tokenizationPropertiesContainPayPal() throws JSONException {
        TestConfigurationBuilder configuration = new TestConfigurationBuilder()
                .googlePayment(new TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .paypalEnabled(false)
                .paypal(new TestConfigurationBuilder.TestPayPalConfigurationBuilder(false))
                .withAnalytics();

        BraintreeFragment fragment = getSetupFragment(configuration);
        GooglePaymentRequest googlePaymentRequest = mBaseRequest
                .googleMerchantName("google-merchant-name-override");

        GooglePayment.requestPayment(fragment, googlePaymentRequest);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePayment(fragment)
                .getJSONArray("allowedPaymentMethods");

        assertEquals(2, allowedPaymentMethods.length());
        assertEquals("PAYPAL", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(1)
                .getString("type"));
    }

    @Test
    public void requestPayment_usesGooglePaymentConfigurationClientId() throws JSONException {
         TestConfigurationBuilder configuration = new TestConfigurationBuilder()
                .googlePayment(new TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                 .paypal(new TestConfigurationBuilder.TestPayPalConfigurationBuilder(true)
                         .clientId("paypal-client-id-for-paypal"))
                .withAnalytics();

        BraintreeFragment fragment = getSetupFragment(configuration);
        GooglePaymentRequest googlePaymentRequest = mBaseRequest
                .googleMerchantName("google-merchant-name-override");

        GooglePayment.requestPayment(fragment, googlePaymentRequest);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePayment(fragment)
                .getJSONArray("allowedPaymentMethods");

        JSONObject paypal = allowedPaymentMethods.getJSONObject(0);

        assertEquals("paypal-client-id-for-google-payment",
                paypal.getJSONObject("parameters")
                        .getJSONObject("purchase_context")
                        .getJSONArray("purchase_units")
                        .getJSONObject(0)
                        .getJSONObject("payee")
                        .getString("client_id"));

        assertEquals("paypal-client-id-for-google-payment",
                paypal.getJSONObject("tokenizationSpecification")
                        .getJSONObject("parameters")
                        .getString("braintree:paypalClientId"));
    }

    @Test
    public void requestPayment_whenGooglePaymentConfigurationLacksClientId_tokenizationPropertiesLackPayPal() throws JSONException {
        TestConfigurationBuilder configuration = new TestConfigurationBuilder()
                .googlePayment(new TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics();

        BraintreeFragment fragment = getSetupFragment(configuration);
        GooglePaymentRequest googlePaymentRequest = mBaseRequest
                .googleMerchantName("google-merchant-name-override");

        GooglePayment.requestPayment(fragment, googlePaymentRequest);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePayment(fragment)
                .getJSONArray("allowedPaymentMethods");

        assertEquals(1, allowedPaymentMethods.length());
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));

        assertFalse(allowedPaymentMethods.toString().contains("paypal-client-id-for-google-payment"));
    }

    @Test
    public void tokenize_withCardToken_returnsGooglePaymentNonce() {
        String paymentDataJson = FixturesHelper.stringFromFixture("response/google_payment/card.json");
        BraintreeFragment fragment = getSetupFragment();
        PaymentData pd = PaymentData.fromJson(paymentDataJson);

        GooglePayment.tokenize(fragment, pd);

        ArgumentCaptor<PaymentMethodNonce> captor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(fragment).postCallback(captor.capture());

        assertTrue(captor.getValue() instanceof GooglePaymentCardNonce);
    }

    @Test
    public void tokenize_withPayPalToken_returnsPayPalAccountNonce() {
        String paymentDataJson = FixturesHelper.stringFromFixture("payment_methods/paypal_account_response.json");

        BraintreeFragment fragment = getSetupFragment();
        PaymentData pd = PaymentData.fromJson(paymentDataJson);

        GooglePayment.tokenize(fragment, pd);

        ArgumentCaptor<PaymentMethodNonce> captor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(fragment).postCallback(captor.capture());

        assertTrue(captor.getValue() instanceof PayPalAccountNonce);
    }

    private BraintreeFragment getSetupFragment() {
        return getSetupFragment("sandbox");
    }

    private BraintreeFragment getSetupFragment(String environment) {
        String configuration = new TestConfigurationBuilder()
                .googlePayment(new TestGooglePaymentConfigurationBuilder()
                        .environment(environment)
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .build();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();

        try {
            when(fragment.getAuthorization()).thenReturn(Authorization.fromString("sandbox_tokenization_key"));
        } catch (InvalidArgumentException e) {
            throw new RuntimeException(e);
        }

        return fragment;
    }

    private BraintreeFragment getSetupFragment(TestConfigurationBuilder configurationBuilder) {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(configurationBuilder.build())
                .build();

        try {
            when(fragment.getAuthorization()).thenReturn(Authorization.fromString("sandbox_tokenization_key"));
        } catch (InvalidArgumentException e) {
            throw new RuntimeException(e);
        }

        return fragment;
    }

    @NonNull
    private JSONObject getPaymentDataRequestJsonSentToGooglePayment(BraintreeFragment fragment) {
        JSONObject result = new JSONObject();
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        try {
            result = new JSONObject(paymentDataRequest.toJson());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }
}
