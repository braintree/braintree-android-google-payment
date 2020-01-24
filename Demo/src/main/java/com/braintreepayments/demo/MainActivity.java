package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.braintreepayments.api.dropin.utils.PaymentMethodType;
import com.braintreepayments.api.models.GooglePaymentCardNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends BaseActivity {

    static final String EXTRA_DEVICE_DATA = "device_data";
    static final String EXTRA_PAYMENT_RESULT = "payment_result";

    private static final int GOOGLE_PAYMENT_REQUEST = 2;
    private static final String KEY_NONCE = "nonce";

    private Button mCreateTransactionButton;
    private TextView mDeviceData;
    private Button mGooglePaymentButton;
    private PaymentMethodNonce mNonce;
    private TextView mNonceDetails;
    private ImageView mNonceIcon;
    private TextView mNonceString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNonceIcon = findViewById(R.id.nonce_icon);
        mNonceString = findViewById(R.id.nonce);
        mNonceDetails = findViewById(R.id.nonce_details);
        mDeviceData = findViewById(R.id.device_data);

        mGooglePaymentButton = findViewById(R.id.google_payment);
        mCreateTransactionButton = findViewById(R.id.create_transaction);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_NONCE)) {
                mNonce = savedInstanceState.getParcelable(KEY_NONCE);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mNonce != null) {
            outState.putParcelable(KEY_NONCE, mNonce);
        }
    }

    public void launchGooglePayment(View v) {
        Intent intent = new Intent(this, GooglePayActivity.class);
        startActivityForResult(intent, GOOGLE_PAYMENT_REQUEST);
    }

    public void createTransaction(View v) {
        Intent intent = new Intent(this, CreateTransactionActivity.class)
                .putExtra(CreateTransactionActivity.EXTRA_PAYMENT_METHOD_NONCE, mNonce);
        startActivity(intent);

        mCreateTransactionButton.setEnabled(false);
        clearNonce();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Parcelable returnedData = data.getParcelableExtra(EXTRA_PAYMENT_RESULT);
            String deviceData = data.getStringExtra(EXTRA_DEVICE_DATA);
            if (returnedData instanceof PaymentMethodNonce) {
                displayNonce((PaymentMethodNonce) returnedData, deviceData);
            }

            mCreateTransactionButton.setEnabled(true);
        }
    }

    @Override
    protected void reset() {
        enableButtons(false);
        mCreateTransactionButton.setEnabled(false);

        clearNonce();
    }

    @Override
    protected void onAuthorizationFetched() {
        enableButtons(true);
    }

    private void displayNonce(PaymentMethodNonce paymentMethodNonce, String deviceData) {
        mNonce = paymentMethodNonce;

        mNonceIcon.setImageResource(PaymentMethodType.forType(mNonce).getDrawable());
        mNonceIcon.setVisibility(VISIBLE);

        mNonceString.setText(getString(R.string.nonce_placeholder, mNonce.getNonce()));
        mNonceString.setVisibility(VISIBLE);

        String details = "";
        if (mNonce instanceof GooglePaymentCardNonce) {
            details = GooglePayActivity.getDisplayString((GooglePaymentCardNonce) mNonce);
        }

        mNonceDetails.setText(details);
        mNonceDetails.setVisibility(VISIBLE);

        mDeviceData.setText(getString(R.string.device_data_placeholder, deviceData));
        mDeviceData.setVisibility(VISIBLE);

        mCreateTransactionButton.setEnabled(true);
    }

    private void clearNonce() {
        mNonceIcon.setVisibility(GONE);
        mNonceString.setVisibility(GONE);
        mNonceDetails.setVisibility(GONE);
        mDeviceData.setVisibility(GONE);
        mCreateTransactionButton.setEnabled(false);
    }

    private void enableButtons(boolean enable) {
        mGooglePaymentButton.setEnabled(enable);
    }
}