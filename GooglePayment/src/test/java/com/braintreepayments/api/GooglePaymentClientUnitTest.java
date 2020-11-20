package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;


import androidx.annotation.RequiresPermission;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.MockBraintreeClientBuilder;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.test.TestConfigurationBuilder;
import com.braintreepayments.api.models.Configuration;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;

import junit.framework.TestCase;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GoogleApiAvailability.class, Wallet.class})
public class GooglePaymentClientUnitTest {

    private FragmentActivity activity;
    private BraintreeClient braintreeClient;
    private ReadyToPayListener readyToPayListener;
    private Configuration configuration;

    @Before
    public void beforeEach() throws JSONException {
        activity = mock(FragmentActivity.class);
        braintreeClient = mock(BraintreeClient.class);
        readyToPayListener = mock(ReadyToPayListener.class);

        String configString = new TestConfigurationBuilder()
                .googlePayment(new TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder()
                        .supportedNetworks(new String[]{"amex", "visa"})
                        .enabled(true))
                .build();

        configuration = Configuration.fromJson(configString);


        GoogleApiAvailability mockGoogleApiAvailability = mock(GoogleApiAvailability.class);
        when(mockGoogleApiAvailability.isGooglePlayServicesAvailable(any(Context.class))).thenReturn(ConnectionResult.SUCCESS);

        mockStatic(GoogleApiAvailability.class);
        when(GoogleApiAvailability.getInstance()).thenReturn(mockGoogleApiAvailability);

        ActivityInfo mockActivityInfo = mock(ActivityInfo.class);
        when(mockActivityInfo.getThemeResource()).thenReturn(R.style.bt_transparent_activity);
    }

    @Test
    public void isReadyToPay_sendsReadyToPayRequest() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .build();

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

        assertEquals(expectedJson, actualJson, false);
    }

    @Test
    public void isReadyToPay_whenExistingPaymentMethodRequired_sendsIsReadyToPayRequestWithExistingPaymentRequired() {

    }
}