package com.braintreepayments.api;

public interface RequestPaymentListener {

    void onResult(Exception error, boolean paymentRequested);
}
