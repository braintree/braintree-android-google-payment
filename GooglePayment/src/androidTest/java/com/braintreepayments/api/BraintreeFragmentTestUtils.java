package com.braintreepayments.api;

import androidx.appcompat.app.AppCompatActivity;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.utilities.TestTokenizationKey;

import static androidx.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.api.utilities.SharedPreferencesHelper.writeMockConfiguration;
import static junit.framework.Assert.fail;

class BraintreeFragmentTestUtils {

    static BraintreeFragment getFragmentWithConfiguration(AppCompatActivity activity, String configuration) {
        return getFragment(activity, null, configuration);
    }

    static BraintreeFragment getFragment(AppCompatActivity activity, String authorization, String configuration) {
        try {
            if (authorization == null) {
                authorization = TestTokenizationKey.TOKENIZATION_KEY;
            }

            if (configuration != null) {
                setConfiguration(authorization, configuration);
            }

            BraintreeFragment fragment = BraintreeFragment.newInstance(activity, authorization);

            while (!fragment.isAdded()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}
            }

            return fragment;
        } catch (InvalidArgumentException e) {
            fail(e.getMessage());
            return new BraintreeFragment();
        }
    }

    private static void setConfiguration(String authorization, String configuration) throws InvalidArgumentException {
        Authorization auth = Authorization.fromString(authorization);
        writeMockConfiguration(getTargetContext(), auth.getConfigUrl(), auth.getBearer(), configuration);
    }
}
