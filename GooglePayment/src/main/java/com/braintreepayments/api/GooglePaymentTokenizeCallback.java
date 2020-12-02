package com.braintreepayments.api;

import com.braintreepayments.api.models.PaymentMethodNonce;

public interface GooglePaymentTokenizeCallback {

    void onResult(PaymentMethodNonce paymentMethodNonce, Exception error);
}
