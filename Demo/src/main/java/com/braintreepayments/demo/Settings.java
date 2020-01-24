package com.braintreepayments.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

class Settings {

    private static final String ENVIRONMENT = "environment";

    private static final String SANDBOX_BASE_SERVER_URL = "https://braintree-sample-merchant.herokuapp.com";
    private static final String PRODUCTION_BASE_SERVER_URL = "https://executive-sample-merchant.herokuapp.com";

    private static final String SANDBOX_TOKENIZATION_KEY = "sandbox_tmxhyf7d_dcpspy2brwdjr3qn";
    private static final String PRODUCTION_TOKENIZATION_KEY = "production_t2wns2y2_dfy45jdj3dxkmz5m";

    private static SharedPreferences sSharedPreferences;

    private static SharedPreferences getPreferences(Context context) {
        if (sSharedPreferences == null) {
            sSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        }

        return sSharedPreferences;
    }

    static int getEnvironment(Context context) {
        return getPreferences(context).getInt(ENVIRONMENT, 0);
    }

    static void setEnvironment(Context context, int environment) {
        getPreferences(context)
                .edit()
                .putInt(ENVIRONMENT, environment)
                .apply();

        DemoApplication.resetApiClient();
    }

    static String getEnvironmentUrl(Context context) {
        int environment = getEnvironment(context);
        if (environment == 0) {
            return SANDBOX_BASE_SERVER_URL;
        } else if (environment == 1) {
            return PRODUCTION_BASE_SERVER_URL;
        } else {
            return "";
        }
    }

    static String getCustomerId(Context context) {
        return getPreferences(context).getString("customer", null);
    }

    static String getMerchantAccountId(Context context) {
        return getPreferences(context).getString("merchant_account", null);
    }

    static boolean useTokenizationKey(Context context) {
        return getPreferences(context).getBoolean("tokenization_key", false);
    }

    static String getEnvironmentTokenizationKey(Context context) {
        int environment = getEnvironment(context);
        if (environment == 0) {
            return SANDBOX_TOKENIZATION_KEY;
        } else if (environment == 1) {
            return PRODUCTION_TOKENIZATION_KEY;
        } else {
            return "";
        }
    }

    static boolean areGooglePaymentPrepaidCardsAllowed(Context context) {
        return getPreferences(context).getBoolean("google_payment_allow_prepaid_cards", true);
    }

    static boolean isGooglePaymentShippingAddressRequired(Context context) {
        return getPreferences(context).getBoolean("google_payment_require_shipping_address", false);
    }

    static boolean isGooglePaymentBillingAddressRequired(Context context) {
        return getPreferences(context).getBoolean("google_payment_require_billing_address", false);
    }

    static boolean isGooglePaymentPhoneNumberRequired(Context context) {
        return getPreferences(context).getBoolean("google_payment_require_phone_number", false);
    }

    static boolean isGooglePaymentEmailRequired(Context context) {
        return getPreferences(context).getBoolean("google_payment_require_email", false);
    }

    static String getGooglePaymentCurrency(Context context) {
        return getPreferences(context).getString("google_payment_currency", "USD");
    }

    static String getGooglePaymentMerchantId(Context context) {
        return getPreferences(context).getString("google_payment_merchant_id", "18278000977346790994");
    }

    static List<String> getGooglePaymentAllowedCountriesForShipping(Context context) {
        String[] preference = getPreferences(context).getString("google_payment_allowed_countries_for_shipping", "US")
                .split(",");
        List<String> countries = new ArrayList<>();
        for(String country : preference) {
            countries.add(country.trim());
        }

        return countries;
    }
}

