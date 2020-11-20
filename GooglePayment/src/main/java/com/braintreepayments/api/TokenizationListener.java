package com.braintreepayments.api;

import com.braintreepayments.api.models.PaymentMethodNonce;

public interface TokenizationListener {

    void onResult(Exception error, PaymentMethodNonce paymentMethodNonce);
}
