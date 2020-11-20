package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;

import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.GooglePaymentCardNonce;
import com.braintreepayments.api.models.GooglePaymentRequest;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.ReadyForGooglePaymentRequest;
import com.braintreepayments.api.test.Fixtures;
import com.braintreepayments.api.test.TestConfigurationBuilder;
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

import static com.braintreepayments.api.GooglePaymentClient.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.GooglePaymentClient.EXTRA_PAYMENT_DATA_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(RobolectricTestRunner.class)
@PrepareForTest({GoogleApiAvailability.class, Wallet.class})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
public class GooglePaymentClientUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private FragmentActivity activity;
    private BraintreeClient braintreeClient;
    private Configuration configuration;

    private GooglePaymentRequest baseRequest;

    private ReadyToPayListener readyToPayListener;
    private RequestPaymentListener requestPaymentListener;
    private TokenizationListener tokenizationListener;

    private ActivityInfo activityInfo;

    @Before
    public void beforeEach() throws JSONException {
        activity = mock(FragmentActivity.class);
        braintreeClient = mock(BraintreeClient.class);
        readyToPayListener = mock(ReadyToPayListener.class);
        requestPaymentListener = mock(RequestPaymentListener.class);
        tokenizationListener = mock(TokenizationListener.class);
        activityInfo = mock(ActivityInfo.class);

        baseRequest = new GooglePaymentRequest()
                .transactionInfo(TransactionInfo.newBuilder()
                        .setTotalPrice("1.00")
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .setCurrencyCode("USD")
                        .build());

        String configString = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .supportedNetworks(new String[]{"amex", "visa"})
                        .enabled(true))
                .build();

        configuration = Configuration.fromJson(configString);


        braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .build();

        GoogleApiAvailability mockGoogleApiAvailability = mock(GoogleApiAvailability.class);
        when(mockGoogleApiAvailability.isGooglePlayServicesAvailable(any(Context.class))).thenReturn(ConnectionResult.SUCCESS);

        mockStatic(GoogleApiAvailability.class);
        when(GoogleApiAvailability.getInstance()).thenReturn(mockGoogleApiAvailability);

        when(activityInfo.getThemeResource()).thenReturn(R.style.bt_transparent_activity);
    }

    @Test
    public void isReadyToPay_sendsReadyToPayRequest() throws JSONException {
        PaymentsClient mockPaymentsClient = mock(PaymentsClient.class);
        when(mockPaymentsClient.isReadyToPay(any(IsReadyToPayRequest.class))).thenReturn(Tasks.forResult(true));

        mockStatic(Wallet.class);
        when(Wallet.getPaymentsClient(any(Activity.class), any(Wallet.WalletOptions.class))).thenReturn(mockPaymentsClient);

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);
        sut.isReadyToPay(activity, null, readyToPayListener);

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

        ReadyForGooglePaymentRequest readyForGooglePaymentRequest = new ReadyForGooglePaymentRequest().existingPaymentMethodRequired(true);

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);
        sut.isReadyToPay(activity, readyForGooglePaymentRequest, readyToPayListener);

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
    public void requestPayment_whenMerchantNotConfigured_returnsExceptionToFragment() throws JSONException {
        String configString = new TestConfigurationBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(configString))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);
        sut.requestPayment(activity, baseRequest, requestPaymentListener);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(requestPaymentListener).onResult(captor.capture(), eq(false));
        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals("Google Pay enabled is not enabled for your Braintree account, or Google Play Services are not configured correctly.",
                captor.getValue().getMessage());
    }

    @Test
    public void requestPayment_whenSandbox_setsTestEnvironment() throws JSONException, InvalidArgumentException {
        String configString = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(configString))
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, baseRequest, requestPaymentListener);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals(WalletConstants.ENVIRONMENT_TEST, intent.getIntExtra(EXTRA_ENVIRONMENT, -1));
        assertEquals("TEST", paymentDataRequestJson.getString("environment"));
    }

    @Test
    public void requestPayment_whenProduction_setsProductionEnvironment() throws JSONException, InvalidArgumentException {
        String configString = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("production")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(configString))
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, baseRequest, requestPaymentListener);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals(WalletConstants.ENVIRONMENT_PRODUCTION, intent.getIntExtra(EXTRA_ENVIRONMENT, -1));
        assertEquals("PRODUCTION", paymentDataRequestJson.getString("environment"));
    }

    @Test
    public void requestPayment_withGoogleMerchantId_sendGoogleMerchantId() throws JSONException, InvalidArgumentException {
        String configString = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(configString))
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentRequest googlePaymentRequest = baseRequest
                .googleMerchantId("google-merchant-id-override");

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, googlePaymentRequest, requestPaymentListener);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals("google-merchant-id-override", paymentDataRequestJson
                .getJSONObject("merchantInfo")
                .getString("merchantId"));
    }

    @Test
    public void requestPayment_withGoogleMerchantName_sendGoogleMerchantName() throws JSONException, InvalidArgumentException {
        String configString = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(configString))
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentRequest googlePaymentRequest = baseRequest
                .googleMerchantName("google-merchant-name-override");

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, googlePaymentRequest, requestPaymentListener);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));

        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals("google-merchant-name-override", paymentDataRequestJson
                .getJSONObject("merchantInfo")
                .getString("merchantName"));
    }

    @Test
    public void requestPayment_whenGooglePayCanProcessPayPal_tokenizationPropertiesIncludePayPal() throws JSONException, InvalidArgumentException {
        String configString = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(configString))
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentRequest googlePaymentRequest = baseRequest
                .googleMerchantName("google-merchant-name-override");

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, googlePaymentRequest, requestPaymentListener);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePayment(activity)
                .getJSONArray("allowedPaymentMethods");

        assertEquals(2, allowedPaymentMethods.length());
        assertEquals("PAYPAL", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(1)
                .getString("type"));
    }

    @Test
    public void requestPayment_whenPayPalDisabledByRequest_tokenizationPropertiesLackPayPal() throws JSONException, InvalidArgumentException {
        String configString = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(configString))
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentRequest googlePaymentRequest = baseRequest
                .googleMerchantName("google-merchant-name-override")
                .paypalEnabled(false);

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, googlePaymentRequest, requestPaymentListener);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePayment(activity)
                .getJSONArray("allowedPaymentMethods");

        assertEquals(1, allowedPaymentMethods.length());
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));
    }

    @Test
    public void requestPayment_whenPayPalDisabledInConfigurationAndGooglePayHasPayPalClientId_tokenizationPropertiesContainPayPal() throws JSONException, InvalidArgumentException {
        String configString = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .paypalEnabled(false)
                .paypal(new TestConfigurationBuilder.TestPayPalConfigurationBuilder(false))
                .withAnalytics()
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(configString))
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentRequest googlePaymentRequest = baseRequest
                .googleMerchantName("google-merchant-name-override");

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, googlePaymentRequest, requestPaymentListener);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePayment(activity)
                .getJSONArray("allowedPaymentMethods");

        assertEquals(2, allowedPaymentMethods.length());
        assertEquals("PAYPAL", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(1)
                .getString("type"));
    }

    @Test
    public void requestPayment_usesGooglePaymentConfigurationClientId() throws JSONException, InvalidArgumentException {
         String configString = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                 .paypal(new TestConfigurationBuilder.TestPayPalConfigurationBuilder(true)
                         .clientId("paypal-client-id-for-paypal"))
                 .withAnalytics()
                 .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(configString))
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentRequest googlePaymentRequest = baseRequest
                .googleMerchantName("google-merchant-name-override");

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, googlePaymentRequest, requestPaymentListener);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePayment(activity)
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
    public void requestPayment_whenGooglePaymentConfigurationLacksClientId_tokenizationPropertiesLackPayPal() throws JSONException, InvalidArgumentException {
        String configString = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(configString))
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        GooglePaymentRequest googlePaymentRequest = baseRequest
                .googleMerchantName("google-merchant-name-override");

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.requestPayment(activity, googlePaymentRequest, requestPaymentListener);

        JSONArray allowedPaymentMethods = getPaymentDataRequestJsonSentToGooglePayment(activity)
                .getJSONArray("allowedPaymentMethods");

        assertEquals(1, allowedPaymentMethods.length());
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(0)
                .getString("type"));

        assertFalse(allowedPaymentMethods.toString().contains("paypal-client-id-for-google-payment"));
    }

    @Test
    public void tokenize_withCardToken_returnsGooglePaymentNonce() throws JSONException, InvalidArgumentException {
        String paymentDataJson = Fixtures.RESPONSE_GOOGLE_PAYMENT_CARD;

        String configString = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(configString))
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        PaymentData pd = PaymentData.fromJson(paymentDataJson);

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.tokenize(activity, pd, tokenizationListener);

        ArgumentCaptor<PaymentMethodNonce> captor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(tokenizationListener).onResult((Exception)isNull(), captor.capture());

        assertTrue(captor.getValue() instanceof GooglePaymentCardNonce);
    }

        @Test
    public void tokenize_withPayPalToken_returnsPayPalAccountNonce() throws JSONException, InvalidArgumentException {
        String paymentDataJson = Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE;

        String configString = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .googleAuthorizationFingerprint("google-auth-fingerprint")
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .enabled(true))
                .withAnalytics()
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(configString))
                .authorization(Authorization.fromString("sandbox_tokenization_string"))
                .activityInfo(activityInfo)
                .build();

        PaymentData pd = PaymentData.fromJson(paymentDataJson);

        GooglePaymentClient sut = new GooglePaymentClient(braintreeClient);

        sut.tokenize(activity, pd, tokenizationListener);

        ArgumentCaptor<PaymentMethodNonce> captor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(tokenizationListener).onResult((Exception)isNull(), captor.capture());

        assertTrue(captor.getValue() instanceof PayPalAccountNonce);
    }

    private JSONObject getPaymentDataRequestJsonSentToGooglePayment(FragmentActivity activity) {
        JSONObject result = new JSONObject();
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));

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