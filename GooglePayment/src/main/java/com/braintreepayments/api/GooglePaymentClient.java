package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.exceptions.GoogleApiClientException;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.GooglePaymentConfiguration;
import com.braintreepayments.api.models.ReadyForGooglePaymentRequest;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class GooglePaymentClient {

    private static final String VISA_NETWORK = "visa";
    private static final String MASTERCARD_NETWORK = "mastercard";
    private static final String AMEX_NETWORK = "amex";
    private static final String DISCOVER_NETWORK = "discover";

    private BraintreeClient braintreeClient;

    GooglePaymentClient(BraintreeClient braintreeClient) {
        this.braintreeClient = braintreeClient;
    }

    public void isReadyToPay(final FragmentActivity activity, final ReadyForGooglePaymentRequest request, final ReadyToPayListener listener) {

        try {
            Class.forName(PaymentsClient.class.getName());
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            listener.onResult(null, false);
            return;
        }

        braintreeClient.getConfiguration(activity, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(@Nullable Exception e, @Nullable Configuration configuration) {
                if(!configuration.getGooglePayment().isEnabled(activity)) {
                    listener.onResult(null, false);
                }

                if(activity == null) {
                    listener.onResult(new GoogleApiClientException(GoogleApiClientException.ErrorType.NotAttachedToActivity, 1), false);
                }

                PaymentsClient paymentsClient = Wallet.getPaymentsClient(activity,
                        new Wallet.WalletOptions.Builder()
                                .setEnvironment(getEnvironment(configuration.getGooglePayment()))
                                .build());

                JSONObject json = new JSONObject();
                JSONArray allowedCardNetworks = buildCardNetworks(configuration);

                try {
                    json
                            .put("apiVersion", 2)
                            .put("apiVersionMinor", 0)
                            .put("allowedPaymentMethods", new JSONArray()
                                    .put(new JSONObject()
                                            .put("type", "CARD")
                                            .put("parameters", new JSONObject()
                                                    .put("allowedAuthMethods", new JSONArray()
                                                            .put("PAN_ONLY")
                                                            .put("CRYPTOGRAM_3DS"))
                                                    .put("allowedCardNetworks", allowedCardNetworks))));

                    if (request != null) {
                        json.put("existingPaymentMethodRequired", request.isExistingPaymentMethodRequired());
                    }

                } catch (JSONException ignored) {
                }
                IsReadyToPayRequest request2 = IsReadyToPayRequest.fromJson(json.toString());

                paymentsClient.isReadyToPay(request2).addOnCompleteListener(new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
//                        try {
//                            listener.onResult(null, task.getResult(ApiException.class));
//                        } catch (ApiException e) {
//                            listener.onResult(e, false);
//                        }
                    }
                });
            }
        });
    }

    static int getEnvironment(GooglePaymentConfiguration configuration) {
        if ("production".equals(configuration.getEnvironment())) {
            return WalletConstants.ENVIRONMENT_PRODUCTION;
        } else {
            return WalletConstants.ENVIRONMENT_TEST;
        }
    }

    static ArrayList<Integer> getAllowedCardNetworks(Configuration configuration) {
        ArrayList<Integer> allowedNetworks = new ArrayList<>();
        for (String network : configuration.getGooglePayment().getSupportedNetworks()) {
            switch (network) {
                case VISA_NETWORK:
                    allowedNetworks.add(WalletConstants.CARD_NETWORK_VISA);
                    break;
                case MASTERCARD_NETWORK:
                    allowedNetworks.add(WalletConstants.CARD_NETWORK_MASTERCARD);
                    break;
                case AMEX_NETWORK:
                    allowedNetworks.add(WalletConstants.CARD_NETWORK_AMEX);
                    break;
                case DISCOVER_NETWORK:
                    allowedNetworks.add(WalletConstants.CARD_NETWORK_DISCOVER);
                    break;
                default:
                    break;
            }
        }

        return allowedNetworks;
    }

    private static JSONArray buildCardNetworks(Configuration configuration) {
        JSONArray cardNetworkStrings = new JSONArray();

        for (int network : getAllowedCardNetworks(configuration)) {
            switch (network) {
                case WalletConstants.CARD_NETWORK_AMEX:
                    cardNetworkStrings.put("AMEX");
                    break;
                case WalletConstants.CARD_NETWORK_DISCOVER:
                    cardNetworkStrings.put("DISCOVER");
                    break;
                case WalletConstants.CARD_NETWORK_JCB:
                    cardNetworkStrings.put("JCB");
                    break;
                case WalletConstants.CARD_NETWORK_MASTERCARD:
                    cardNetworkStrings.put("MASTERCARD");
                    break;
                case WalletConstants.CARD_NETWORK_VISA:
                    cardNetworkStrings.put("VISA");
                    break;
            }
        }
        return cardNetworkStrings;
    }
}