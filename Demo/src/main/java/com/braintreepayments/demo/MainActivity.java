package com.braintreepayments.demo;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.GooglePayCapabilities;
import com.braintreepayments.api.GooglePaymentClient;
import com.braintreepayments.api.ReadyToPayListener;
import com.braintreepayments.api.RequestPaymentListener;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.BinData;
import com.braintreepayments.api.models.GooglePaymentCardNonce;
import com.braintreepayments.api.models.GooglePaymentRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.PostalAddress;
import com.google.android.gms.wallet.ShippingAddressRequirements;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity {
    public static final String RETURN_URL_SCHEME = "com.braintreepayments.demo.braintree";

    protected String mAuthorization;
    protected String mCustomerId;

    private GooglePaymentClient googlePaymentClient;

    private BraintreeClient braintreeClient;

    private ImageButton mGooglePaymentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGooglePaymentButton = findViewById(R.id.google_payment_button);

        fetchAuthorization();
    }

    protected void reset() {
        mGooglePaymentButton.setVisibility(GONE);
    }

    protected void fetchAuthorization() {
        if (mAuthorization != null) {
            setProgressBarIndeterminateVisibility(false);
            onAuthorizationFetched();
        }

        String authType = Settings.getAuthorizationType(this);
        if (authType.equals(getString(R.string.tokenization_key))) {
            mAuthorization = Settings.getTokenizationKey(this);
            setProgressBarIndeterminateVisibility(false);
            onAuthorizationFetched();
        }
//        } else if (authType.equals(getString(R.string.client_token))) {
//            merchant.fetchClientToken(this, new FetchClientTokenListener() {
//                @Override
//                public void onResult(@Nullable Exception error, @Nullable String clientToken) {
//                    setProgressBarIndeterminateVisibility(false);
//                    if (clientToken != null) {
//                        mAuthorization = clientToken;
//                        initializeBraintree(clientToken, RETURN_URL_SCHEME);
//                    } else if (error != null) {
//                        showDialog(error.getMessage());
//                    }
//                }
//            });
//        }
    }

    protected void onAuthorizationFetched() {
        // TODO: move GooglePaymentActivity test to GooglePayment library
        try {
            googlePaymentClient = new GooglePaymentClient(BraintreeClient.newInstance(Authorization.fromString(mAuthorization), RETURN_URL_SCHEME));
        } catch (InvalidArgumentException e) {
            return;
        }
        googlePaymentClient.isReadyToPay(this, null, new ReadyToPayListener() {
            @Override
            public void onResult(Exception error, Boolean isReadyToPay) {
                if (isReadyToPay) {
                    mGooglePaymentButton.setVisibility(VISIBLE);
                } else {
//                        showDialog("Google Payments are not available. The following issues could be the cause:\n\n" +
//                                "No user is logged in to the device.\n\n" +
//                                "Google Play Services is missing or out of date.");
                }
            }
        });
//
//        if (GooglePayCapabilities.isGooglePayEnabled(this, configuration.getGooglePayment())) {
//            googlePaymentClient.isReadyToPay(this, null, new ReadyToPayListener() {
//                @Override
//                public void onResult(Exception error, Boolean isReadyToPay) {
//                    if (isReadyToPay) {
//                        mGooglePaymentButton.setVisibility(VISIBLE);
//                    } else {
////                        showDialog("Google Payments are not available. The following issues could be the cause:\n\n" +
////                                "No user is logged in to the device.\n\n" +
////                                "Google Play Services is missing or out of date.");
//                    }
//                }
//            });
//        } else {
////            showDialog("Google Payments are not available. The following issues could be the cause:\n\n" +
////                    "Google Payments are not enabled for the current merchant.\n\n" +
////                    "Google Play Services is missing or out of date.");
//        }
    }

    protected void onBraintreeInitialized() {
        // TODO: move GooglePaymentActivity test to GooglePayment library
//        if (GooglePayCapabilities.isGooglePayEnabled(this, configuration.getGooglePayment())) {
//            GooglePayment.isReadyToPay(mBraintreeFragment, new BraintreeResponseListener<Boolean>() {
//                @Override
//                public void onResponse(Boolean isReadyToPay) {
//                    if (isReadyToPay) {
//                        mGooglePaymentButton.setVisibility(VISIBLE);
//                    } else {
//                        showDialog("Google Payments are not available. The following issues could be the cause:\n\n" +
//                                "No user is logged in to the device.\n\n" +
//                                "Google Play Services is missing or out of date.");
//                    }
//                }
//            });
//        } else {
//            showDialog("Google Payments are not available. The following issues could be the cause:\n\n" +
//                    "Google Payments are not enabled for the current merchant.\n\n" +
//                    "Google Play Services is missing or out of date.");
//        }
    }

//    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
//        super.onPaymentMethodNonceCreated(paymentMethodNonce);
//
//        Intent intent = new Intent().putExtra(MainActivity.EXTRA_PAYMENT_RESULT, paymentMethodNonce);
//        setResult(RESULT_OK, intent);
//        finish();
//    }

    public void launchGooglePayment(View v) {
        // TODO: move GooglePaymentActivity test to GooglePayment library
        setProgressBarIndeterminateVisibility(true);

        GooglePaymentRequest googlePaymentRequest = new GooglePaymentRequest()
                .transactionInfo(TransactionInfo.newBuilder()
                        .setCurrencyCode(Settings.getGooglePaymentCurrency(this))
                        .setTotalPrice("1.00")
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .build())
                .allowPrepaidCards(Settings.areGooglePaymentPrepaidCardsAllowed(this))
                .billingAddressFormat(WalletConstants.BILLING_ADDRESS_FORMAT_FULL)
                .billingAddressRequired(Settings.isGooglePaymentBillingAddressRequired(this))
                .emailRequired(Settings.isGooglePaymentEmailRequired(this))
                .phoneNumberRequired(Settings.isGooglePaymentPhoneNumberRequired(this))
                .shippingAddressRequired(Settings.isGooglePaymentShippingAddressRequired(this))
                .shippingAddressRequirements(ShippingAddressRequirements.newBuilder()
                        .addAllowedCountryCodes(Settings.getGooglePaymentAllowedCountriesForShipping(this))
                        .build())
                .googleMerchantId(Settings.getGooglePaymentMerchantId(this));

//        GooglePaymentClient googlePaymentClient = new GooglePaymentClient(BraintreeClient.newInstance(authorization, returnUrlScheme));
//        googlePaymentClient.requestPayment(this, googlePaymentRequest, new RequestPaymentListener() {
//            @Override
//            public void onResult(Exception error, boolean paymentRequested) {
//                // TODO:  handle result
//            }
//        });
    }

    public static String getDisplayString(GooglePaymentCardNonce nonce) {
        return "Underlying Card Last Two: " + nonce.getLastTwo() + "\n" +
                "Card Description: " + nonce.getDescription() + "\n" +
                "Email: " + nonce.getEmail() + "\n" +
                "Billing address: " + formatAddress(nonce.getBillingAddress()) + "\n" +
                "Shipping address: " + formatAddress(nonce.getShippingAddress()) + "\n" +
                getDisplayString(nonce.getBinData());
    }

    private static String formatAddress(PostalAddress address) {
        if (address == null) {
            return "null";
        }

        return address.getRecipientName() + " " +
                address.getStreetAddress() + " " +
                address.getExtendedAddress() + " " +
                address.getLocality() + " " +
                address.getRegion() + " " +
                address.getPostalCode() + " " +
                address.getSortingCode() + " " +
                address.getCountryCodeAlpha2() + " " +
                address.getPhoneNumber();
    }

    public static String getDisplayString(BinData binData) {
        return "Bin Data: \n"  +
                "         - Prepaid: " + binData.getHealthcare() + "\n" +
                "         - Healthcare: " + binData.getHealthcare() + "\n" +
                "         - Debit: " + binData.getDebit() + "\n" +
                "         - Durbin Regulated: " + binData.getDurbinRegulated() + "\n" +
                "         - Commercial: " + binData.getCommercial() + "\n" +
                "         - Payroll: " + binData.getPayroll() + "\n" +
                "         - Issuing Bank: " + binData.getIssuingBank() + "\n" +
                "         - Country of Issuance: " + binData.getCountryOfIssuance() + "\n" +
                "         - Product Id: " + binData.getProductId();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.reset:
//                performReset();
                return true;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return false;
        }
    }
}