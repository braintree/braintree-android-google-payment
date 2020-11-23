package com.braintreepayments.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class Settings {

    private static final String ENVIRONMENT = "environment";

    static final String SANDBOX_ENV_NAME = "Sandbox";
    static final String SANDBOX_INDIA_ENV_NAME = "Sandbox India";
    static final String PRODUCTION_ENV_NAME = "Production";

    private static final String PRODUCTION_BASE_SERVER_URL = "https://executive-sample-merchant.herokuapp.com";
    private static final String PRODUCTION_TOKENIZATION_KEY = "production_t2wns2y2_dfy45jdj3dxkmz5m";

    private static final String SANDBOX_BASE_SERVER_URL = "https://braintree-sample-merchant.herokuapp.com";
    private static final String SANDBOX_TOKENIZATION_KEY = "sandbox_tmxhyf7d_dcpspy2brwdjr3qn";

    private static final String SANDBOX_INDIA_BASE_SERVER_URL = "https://braintree-india-2fa-merchant.herokuapp.com/";

    private static SharedPreferences sSharedPreferences;

    public static SharedPreferences getPreferences(Context context) {
        if (sSharedPreferences == null) {
            sSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        }

        return sSharedPreferences;
    }

    public static String getEnvironment(Context context) {
        return getPreferences(context).getString(ENVIRONMENT, SANDBOX_ENV_NAME);
    }

    public static void setEnvironment(Context context, String environment) {
        getPreferences(context)
                .edit()
                .putString(ENVIRONMENT, environment)
                .apply();

//        DemoApplication.resetApiClient();
    }

    public static String getSandboxUrl() {
        return SANDBOX_BASE_SERVER_URL;
    }

    public static String getEnvironmentUrl(Context context) {
        String environment = getEnvironment(context);
        if (SANDBOX_ENV_NAME.equals(environment)) {
            return SANDBOX_BASE_SERVER_URL;
        } else if (SANDBOX_INDIA_ENV_NAME.equals(environment)) {
            return SANDBOX_INDIA_BASE_SERVER_URL;
        } else if (PRODUCTION_ENV_NAME.equals(environment)) {
            return PRODUCTION_BASE_SERVER_URL;
        } else {
            return "";
        }
    }

    public static String getAuthorizationType(Context context) {
        return "";
//        return getPreferences(context).getString("authorization_type", context.getString(R.string.client_token));
    }

    public static String getCustomerId(Context context) {
        return getPreferences(context).getString("customer", null);
    }

    public static String getMerchantAccountId(Context context) {
        return getPreferences(context).getString("merchant_account", null);
    }

    public static boolean shouldCollectDeviceData(Context context) {
        return getPreferences(context).getBoolean("collect_device_data", false);
    }

    public static boolean useTokenizationKey(Context context) {
        return false;
//        return getAuthorizationType(context).equals(context.getString(R.string.tokenization_key));
    }

    public static String getTokenizationKey(Context context) {
        String environment = getEnvironment(context);

        if (SANDBOX_ENV_NAME.equals(environment)) {
            return SANDBOX_TOKENIZATION_KEY;
        } else if (PRODUCTION_ENV_NAME.equals(environment)) {
            return PRODUCTION_TOKENIZATION_KEY;
        } else {
            return null;
        }
    }

    public static boolean areGooglePaymentPrepaidCardsAllowed(Context context) {
        return getPreferences(context).getBoolean("google_payment_allow_prepaid_cards", true);
    }

    public static boolean isGooglePaymentShippingAddressRequired(Context context) {
        return getPreferences(context).getBoolean("google_payment_require_shipping_address", false);
    }

    public static boolean isGooglePaymentBillingAddressRequired(Context context) {
        return getPreferences(context).getBoolean("google_payment_require_billing_address", false);
    }

    public static boolean isGooglePaymentPhoneNumberRequired(Context context) {
        return getPreferences(context).getBoolean("google_payment_require_phone_number", false);
    }

    public static boolean isGooglePaymentEmailRequired(Context context) {
        return getPreferences(context).getBoolean("google_payment_require_email", false);
    }

    public static String getGooglePaymentCurrency(Context context) {
        return getPreferences(context).getString("google_payment_currency", "USD");
    }

    public static String getGooglePaymentMerchantId(Context context) {
        return getPreferences(context).getString("google_payment_merchant_id", "18278000977346790994");
    }

    public static List<String> getGooglePaymentAllowedCountriesForShipping(Context context) {
        String[] preference = getPreferences(context).getString("google_payment_allowed_countries_for_shipping", "US")
                .split(",");
        List<String> countries = new ArrayList<>();
        for(String country : preference) {
            countries.add(country.trim());
        }

        return countries;
    }
}
