package com.braintreepayments.demo;

import com.google.gson.annotations.SerializedName;

class ClientToken {

    @SerializedName("client_token")
    private String mClientToken;

    String getClientToken() {
        return mClientToken;
    }
}