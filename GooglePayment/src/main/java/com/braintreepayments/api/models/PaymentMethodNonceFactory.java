package com.braintreepayments.api.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class PaymentMethodNonceFactory {
    public static PaymentMethodNonce fromString(String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);
        Iterator<String> keys = json.keys();

        while (keys.hasNext()) {
            String key = keys.next();

            switch (key) {
                case GooglePaymentCardNonce.API_RESOURCE_KEY:
                    return GooglePaymentCardNonce.fromJson(jsonString);

                case PayPalAccountNonce.API_RESOURCE_KEY:
                    return PayPalAccountNonce.fromJson(jsonString);
            }
        }

        throw new JSONException("Could not parse JSON for a payment method nonce");
    }
}
