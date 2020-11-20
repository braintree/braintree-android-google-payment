package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.GoogleApiClientException;
import com.braintreepayments.api.googlepayment.R;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.GooglePaymentConfiguration;
import com.braintreepayments.api.models.GooglePaymentRequest;
import com.braintreepayments.api.models.PaymentMethodNonceFactory;
import com.braintreepayments.api.models.ReadyForGooglePaymentRequest;
import com.braintreepayments.api.models.TokenizationKey;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

// TODO: Unit test analytics events and errors thrown
public class GooglePaymentClient {

    protected static final String EXTRA_ENVIRONMENT = "com.braintreepayments.api.EXTRA_ENVIRONMENT";
    protected static final String EXTRA_PAYMENT_DATA_REQUEST = "com.braintreepayments.api.EXTRA_PAYMENT_DATA_REQUEST";

    private static final String VISA_NETWORK = "visa";
    private static final String MASTERCARD_NETWORK = "mastercard";
    private static final String AMEX_NETWORK = "amex";
    private static final String DISCOVER_NETWORK = "discover";

    private static final String CARD_PAYMENT_TYPE = "CARD";
    private static final String PAYPAL_PAYMENT_TYPE = "PAYPAL";

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
                IsReadyToPayRequest request = IsReadyToPayRequest.fromJson(json.toString());

                paymentsClient.isReadyToPay(request).addOnCompleteListener(new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        try {
                            listener.onResult(null, task.getResult(ApiException.class));
                        } catch (ApiException e) {
                            listener.onResult(e, false);
                        }
                    }
                });
            }
        });
    }

    public void requestPayment(final FragmentActivity activity, final GooglePaymentRequest request, final RequestPaymentListener listener) {
        braintreeClient.sendAnalyticsEvent(activity, "google-payment.selected");

        if (!validateManifest(activity)) {
            listener.onResult(new BraintreeException("GooglePaymentActivity was not found in the Android " +
                    "manifest, or did not have a theme of R.style.bt_transparent_activity"), false);
            braintreeClient.sendAnalyticsEvent(activity,"google-payment.failed");
            return;
        }

        if (request == null) {
            listener.onResult(new BraintreeException("Cannot pass null GooglePaymentRequest to requestPayment"), false);
            braintreeClient.sendAnalyticsEvent(activity, "google-payment.failed");
            return;
        }

        if (request.getTransactionInfo() == null) {
            listener.onResult(new BraintreeException("Cannot pass null TransactionInfo to requestPayment"), false);
            braintreeClient.sendAnalyticsEvent(activity, "google-payment.failed");
            return;
        }

        braintreeClient.getConfiguration(activity, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(@Nullable Exception e, @Nullable Configuration configuration) {
                if(!configuration.getGooglePayment().isEnabled(activity)) {
                    listener.onResult(new BraintreeException("Google Pay enabled is not enabled for your Braintree account," +
                            " or Google Play Services are not configured correctly."), false);
                    return;
                }

                setGooglePaymentRequestDefaults(activity, configuration, request);

                braintreeClient.sendAnalyticsEvent(activity, "google-payment.started");

                PaymentDataRequest paymentDataRequest = PaymentDataRequest.fromJson(request.toJson());
                Intent intent = new Intent(activity, GooglePaymentActivity.class)
                        .putExtra(EXTRA_ENVIRONMENT, getEnvironment(configuration.getGooglePayment()))
                        .putExtra(EXTRA_PAYMENT_DATA_REQUEST, paymentDataRequest);

                activity.startActivityForResult(intent, BraintreeRequestCodes.GOOGLE_PAYMENT);
            }
        });

    }

    public void tokenize(FragmentActivity activity, PaymentData paymentData, TokenizationListener listener) {
        try {
            listener.onResult(null, PaymentMethodNonceFactory.fromString(paymentData.toJson()));
            braintreeClient.sendAnalyticsEvent(activity, "google-payment.nonce-received");
        } catch (JSONException | NullPointerException e) {
            braintreeClient.sendAnalyticsEvent(activity, "google-payment.failed");

            try {
                String token = new JSONObject(paymentData.toJson())
                        .getJSONObject("paymentMethodData")
                        .getJSONObject("tokenizationData")
                        .getString("token");
                listener.onResult(ErrorWithResponse.fromJson(token), null);
            } catch (JSONException | NullPointerException e1) {
                listener.onResult(e1, null);
            }
        }
    }

    private void setGooglePaymentRequestDefaults(FragmentActivity activity, Configuration configuration,
                                                        GooglePaymentRequest request) {
        if (request.isEmailRequired() == null) {
            request.emailRequired(false);
        }

        if (request.isPhoneNumberRequired() == null) {
            request.phoneNumberRequired(false);
        }

        if (request.isBillingAddressRequired() == null) {
            request.billingAddressRequired(false);
        }

        if (request.isBillingAddressRequired() &&
                request.getBillingAddressFormat() == null) {
            request.billingAddressFormat(WalletConstants.BILLING_ADDRESS_FORMAT_MIN);
        }

        if (request.isShippingAddressRequired() == null) {
            request.shippingAddressRequired(false);
        }

        if (request.getAllowPrepaidCards() == null) {
            request.allowPrepaidCards(true);
        }

        if (request.getAllowedPaymentMethod(CARD_PAYMENT_TYPE) == null) {
            request.setAllowedPaymentMethod(CARD_PAYMENT_TYPE,
                    buildCardPaymentMethodParameters(request, configuration));
        }

        if (request.getTokenizationSpecificationForType(CARD_PAYMENT_TYPE) == null) {
            request.setTokenizationSpecificationForType("CARD",
                    buildCardTokenizationSpecification(activity, configuration));
        }

        boolean googlePaymentCanProcessPayPal = request.isPayPalEnabled() &&
                !TextUtils.isEmpty(configuration.getGooglePayment().getPaypalClientId());

        if (googlePaymentCanProcessPayPal) {
            if (request.getAllowedPaymentMethod("PAYPAL") == null) {
                request.setAllowedPaymentMethod(PAYPAL_PAYMENT_TYPE,
                        buildPayPalPaymentMethodParameters(configuration));
            }


            if (request.getTokenizationSpecificationForType(PAYPAL_PAYMENT_TYPE) == null) {
                request.setTokenizationSpecificationForType("PAYPAL",
                        buildPayPalTokenizationSpecification(activity, configuration));
            }
        }

        request.environment(configuration.getGooglePayment().getEnvironment());
    }

    private JSONObject buildCardPaymentMethodParameters(GooglePaymentRequest request, Configuration configuration) {
        JSONObject defaultParameters = new JSONObject();

        try {
            if (request.getAllowedCardNetworksForType(CARD_PAYMENT_TYPE) == null) {
                JSONArray cardNetworkStrings = buildCardNetworks(configuration);

                if (request.getAllowedAuthMethodsForType(CARD_PAYMENT_TYPE) == null) {
                    request.setAllowedAuthMethods(CARD_PAYMENT_TYPE,
                            new JSONArray()
                                    .put("PAN_ONLY")
                                    .put("CRYPTOGRAM_3DS"));
                } else {
                    request.setAllowedAuthMethods(CARD_PAYMENT_TYPE,
                            request.getAllowedAuthMethodsForType(CARD_PAYMENT_TYPE));
                }

                request.setAllowedCardNetworks(CARD_PAYMENT_TYPE, cardNetworkStrings);
            }

            defaultParameters
                    .put("billingAddressRequired", request.isBillingAddressRequired())
                    .put("allowPrepaidCards", request.getAllowPrepaidCards())
                    .put("allowedAuthMethods",
                            request.getAllowedAuthMethodsForType(CARD_PAYMENT_TYPE))
                    .put("allowedCardNetworks",
                            request.getAllowedCardNetworksForType(CARD_PAYMENT_TYPE));

            if (request.isBillingAddressRequired()) {
                defaultParameters
                        .put("billingAddressParameters", new JSONObject()
                                .put("format", request.billingAddressFormatToString())
                                .put("phoneNumberRequired", request.isPhoneNumberRequired()));
            }
        } catch (JSONException ignored) {
        }
        return defaultParameters;
    }

    private JSONObject buildPayPalPaymentMethodParameters(Configuration configuration) {
        JSONObject defaultParameters = new JSONObject();

        try {
            JSONObject purchaseContext = new JSONObject()
                    .put("purchase_units", new JSONArray()
                            .put(new JSONObject()
                                    .put("payee", new JSONObject()
                                            .put("client_id", configuration.getGooglePayment().getPaypalClientId())
                                    )
                                    .put("recurring_payment", "true")
                            )
                    );

            defaultParameters.put("purchase_context", purchaseContext);
        } catch (JSONException ignored) {
        }

        return defaultParameters;

    }

    private JSONObject buildCardTokenizationSpecification(FragmentActivity activity, Configuration configuration) {
        JSONObject cardJson = new JSONObject();
        JSONObject parameters = new JSONObject();
        String googlePaymentVersion = com.braintreepayments.api.googlepayment.BuildConfig.VERSION_NAME;

        try {
            parameters
                    .put("gateway", "braintree")
                    .put("braintree:apiVersion", "v1")
                    .put("braintree:sdkVersion", googlePaymentVersion)
                    .put("braintree:merchantId", configuration.getMerchantId())
                    .put("braintree:metadata", (new JSONObject()
                            .put("source", "client")
                            .put("integration", braintreeClient.getIntegrationType(activity))
                            .put("sessionId", braintreeClient.getSessionId())
                            .put("version", googlePaymentVersion)
                            .put("platform", "android")).toString());

            if(braintreeClient.getAuthorization() instanceof TokenizationKey) {
                parameters
                        .put("braintree:clientKey", braintreeClient.getAuthorization().toString());
            } else {
                parameters
                        .put("braintree:authorizationFingerprint", configuration
                                .getGooglePayment()
                                .getGoogleAuthorizationFingerprint());
            }
        } catch (JSONException ignored) {
        }

        try {
            cardJson
                    .put("type", "PAYMENT_GATEWAY")
                    .put("parameters", parameters);
        } catch (JSONException ignored) {
        }

        return cardJson;
    }

    private JSONObject buildPayPalTokenizationSpecification(FragmentActivity activity, Configuration configuration) {
        JSONObject json = new JSONObject();
        String googlePaymentVersion = com.braintreepayments.api.googlepayment.BuildConfig.VERSION_NAME;

        try {
            json.put("type", "PAYMENT_GATEWAY")
                    .put("parameters", new JSONObject()
                            .put("gateway", "braintree")
                            .put("braintree:apiVersion", "v1")
                            .put("braintree:sdkVersion", googlePaymentVersion)
                            .put("braintree:merchantId", configuration.getMerchantId())
                            .put("braintree:paypalClientId", configuration.getGooglePayment().getPaypalClientId())
                            .put("braintree:metadata", (new JSONObject()
                                    .put("source", "client")
                                    .put("integration", braintreeClient.getIntegrationType(activity))
                                    .put("sessionId", braintreeClient.getSessionId())
                                    .put("version", googlePaymentVersion)
                                    .put("platform", "android")).toString()));
        } catch (JSONException ignored) {
        }

        return json;
    }

    private boolean validateManifest(Context context) {
        ActivityInfo activityInfo = braintreeClient.getManifestActivityInfo(context, GooglePaymentActivity.class);
        return activityInfo != null && activityInfo.getThemeResource() == R.style.bt_transparent_activity;
    }

    int getEnvironment(GooglePaymentConfiguration configuration) {
        if ("production".equals(configuration.getEnvironment())) {
            return WalletConstants.ENVIRONMENT_PRODUCTION;
        } else {
            return WalletConstants.ENVIRONMENT_TEST;
        }
    }

    ArrayList<Integer> getAllowedCardNetworks(Configuration configuration) {
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

    private JSONArray buildCardNetworks(Configuration configuration) {
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