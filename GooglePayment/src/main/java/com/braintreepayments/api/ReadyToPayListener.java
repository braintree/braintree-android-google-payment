package com.braintreepayments.api;

public interface ReadyToPayListener {

    void onResult(Exception error, Boolean isReadyToPay);
}
