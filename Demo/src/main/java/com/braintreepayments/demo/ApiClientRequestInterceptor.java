package com.braintreepayments.demo;

import retrofit.RequestInterceptor;

public class ApiClientRequestInterceptor implements RequestInterceptor {

    @Override
    public void intercept(RequestFacade request) {
        request.addHeader("User-Agent", "braintree/android-demo-app/" + BuildConfig.VERSION_NAME);
        request.addHeader("Accept", "application/json");
    }
}
