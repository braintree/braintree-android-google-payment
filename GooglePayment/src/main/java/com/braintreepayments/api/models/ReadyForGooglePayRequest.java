package com.braintreepayments.api.models;

public class ReadyForGooglePayRequest {

    private boolean mExistingPaymentMethodRequired;

    public ReadyForGooglePayRequest existingPaymentMethodRequired(boolean value) {
        mExistingPaymentMethodRequired = value;
        return this;
    }

    public boolean isExistingPaymentMethodRequired() {
        return mExistingPaymentMethodRequired;
    }
}
