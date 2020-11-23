package com.braintreepayments.api;

import com.braintreepayments.api.models.PaymentMethodNonce;

public interface GooglePaymentActivityResultListener {
    void onResult(Exception error, PaymentMethodNonce paymentMethodNonce);
}
