package com.braintreepayments.api.models;

import android.os.Bundle;
import android.os.Parcel;

import com.braintreepayments.api.Json;
import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.wallet.CardInfo;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentMethodToken;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Constructor;

import static com.braintreepayments.api.models.BinData.NO;
import static com.braintreepayments.api.models.BinData.UNKNOWN;
import static com.braintreepayments.api.models.BinData.YES;
import static com.braintreepayments.api.test.Assertions.assertBinDataEqual;
import static com.braintreepayments.api.test.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class GooglePaymentCardNonceUnitTest {

    @Test
    public void fromPaymentData_createsGooglePaymentCardNonce() throws Exception {
        String response = stringFromFixture("payment_methods/google_pay_card_response.json");
        JSONObject billing = new JSONObject(response).getJSONObject("paymentMethodData")
            .getJSONObject("info")
            .getJSONObject("billingAddress");
        JSONObject shipping = new JSONObject(response).getJSONObject("shippingAddress");

        PostalAddress billingPostalAddress = getPostalAddressObject(billing);
        PostalAddress shippingPostalAddress = getPostalAddressObject(shipping);

        PaymentData paymentData = getPaymentData(response);

        GooglePaymentCardNonce googlePaymentCardNonce = GooglePaymentCardNonce.fromPaymentData(paymentData);

        assertEquals("Google Pay", googlePaymentCardNonce.getTypeLabel());
        assertEquals("fake-android-pay-nonce", googlePaymentCardNonce.getNonce());
        assertEquals("MasterCard 0276", googlePaymentCardNonce.getDescription());
        assertEquals("Visa", googlePaymentCardNonce.getCardType());
        assertEquals("11", googlePaymentCardNonce.getLastTwo());
        assertEquals("1234", googlePaymentCardNonce.getLastFour());
        assertEquals("android-user@example.com", googlePaymentCardNonce.getEmail());
        assertNull(googlePaymentCardNonce.getBillingAddress());
        assertNull(googlePaymentCardNonce.getShippingAddress());
        assertPostalAddress(billingPostalAddress, googlePaymentCardNonce.getBillingPostalAddress());
        assertPostalAddress(shippingPostalAddress, googlePaymentCardNonce.getShippingPostalAddress());
    }

    @Test
    public void fromJson_withoutBillingAddress_createsGooglePayCardNonce() throws Exception {
        String response = stringFromFixture("payment_methods/google_pay_card_response.json");
        JSONObject json = new JSONObject(response);
        json.getJSONObject("paymentMethodData").getJSONObject("info").remove("billingAddress");
        response = json.toString();
        JSONObject billing = new JSONObject();

        PostalAddress billingPostalAddress = getPostalAddressObject(billing);

        PaymentData paymentData = getPaymentData(response);

        GooglePaymentCardNonce googlePaymentCardNonce = GooglePaymentCardNonce.fromPaymentData(paymentData);

        assertPostalAddress(billingPostalAddress, googlePaymentCardNonce.getBillingPostalAddress());
    }

    @Test
    public void fromJson_withoutShippingAddress_createsGooglePayCardNonce() throws Exception {

        String response = stringFromFixture("payment_methods/google_pay_card_response.json");
        JSONObject json = new JSONObject(response);
        json.remove("shippingAddress");
        response = json.toString();
        JSONObject shipping = new JSONObject();

        PostalAddress shippingPostalAddress = getPostalAddressObject(shipping);

        PaymentData paymentData = getPaymentData(response);

        GooglePaymentCardNonce googlePaymentCardNonce = GooglePaymentCardNonce.fromPaymentData(paymentData);

        assertPostalAddress(shippingPostalAddress, googlePaymentCardNonce.getShippingPostalAddress());
    }

    @Test
    public void fromJson_withoutEmail_createsGooglePayCardNonce() throws JSONException {
        String response = stringFromFixture("payment_methods/google_pay_card_response.json");

        JSONObject json = new JSONObject(response);
        json.remove("email");
        response = json.toString();

        PaymentData paymentData = getPaymentData(response);

        GooglePaymentCardNonce googlePaymentCardNonce = GooglePaymentCardNonce.fromPaymentData(paymentData);

        assertEquals("", googlePaymentCardNonce.getEmail());
    }

    @Test
    public void fromJson_createsGooglePayCardNonce() throws JSONException {
        GooglePaymentCardNonce googlePaymentCardNonce = GooglePaymentCardNonce.fromJson(
                stringFromFixture("payment_methods/android_pay_card_response.json"));

        assertEquals("Google Pay", googlePaymentCardNonce.getTypeLabel());
        assertEquals("fake-android-pay-nonce", googlePaymentCardNonce.getNonce());
        assertEquals("Google Pay", googlePaymentCardNonce.getDescription());
        assertEquals("Visa", googlePaymentCardNonce.getCardType());
        assertEquals("11", googlePaymentCardNonce.getLastTwo());
        assertEquals("1234", googlePaymentCardNonce.getLastFour());
        assertNotNull(googlePaymentCardNonce.getBinData());
        assertEquals(UNKNOWN, googlePaymentCardNonce.getBinData().getPrepaid());
        assertEquals(YES, googlePaymentCardNonce.getBinData().getHealthcare());
        assertEquals(NO, googlePaymentCardNonce.getBinData().getDebit());
        assertEquals(UNKNOWN, googlePaymentCardNonce.getBinData().getDurbinRegulated());
        assertEquals(UNKNOWN, googlePaymentCardNonce.getBinData().getCommercial());
        assertEquals(UNKNOWN, googlePaymentCardNonce.getBinData().getPayroll());
        assertEquals(UNKNOWN, googlePaymentCardNonce.getBinData().getIssuingBank());
        assertEquals("Something", googlePaymentCardNonce.getBinData().getCountryOfIssuance());
        assertEquals("123", googlePaymentCardNonce.getBinData().getProductId());
    }

    @Test
    public void parcelsCorrectly() throws Exception {
        String response = stringFromFixture("payment_methods/google_pay_card_response.json");
        JSONObject billing = new JSONObject(response).getJSONObject("paymentMethodData")
                .getJSONObject("info")
                .getJSONObject("billingAddress");
        JSONObject shipping = new JSONObject(response).getJSONObject("shippingAddress");

        PostalAddress billingPostalAddress = getPostalAddressObject(billing);
        PostalAddress shippingPostalAddress = getPostalAddressObject(shipping);
        PaymentData paymentData = getPaymentData(response);

        GooglePaymentCardNonce googlePaymentCardNonce = GooglePaymentCardNonce.fromPaymentData(paymentData);

        Parcel parcel = Parcel.obtain();
        googlePaymentCardNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        GooglePaymentCardNonce parceled = GooglePaymentCardNonce.CREATOR.createFromParcel(parcel);

        assertEquals("Google Pay", parceled.getTypeLabel());
        assertEquals("fake-android-pay-nonce", parceled.getNonce());
        assertEquals("MasterCard 0276", parceled.getDescription());
        assertEquals("Visa", parceled.getCardType());
        assertEquals("11", parceled.getLastTwo());
        assertEquals("1234", parceled.getLastFour());
        assertEquals("android-user@example.com", parceled.getEmail());
        assertNull(parceled.getBillingAddress());
        assertNull(parceled.getShippingAddress());
        assertPostalAddress(billingPostalAddress, parceled.getBillingPostalAddress());
        assertPostalAddress(shippingPostalAddress, parceled.getShippingPostalAddress());

        assertBinDataEqual(googlePaymentCardNonce.getBinData(), parceled.getBinData());
    }

    private PaymentData getPaymentData(String response) {
        try {
            String email = Json.optString(new JSONObject(response), "email", null);

            Constructor<PaymentMethodToken> paymentMethodTokenConstructor = PaymentMethodToken.class
                    .getDeclaredConstructor(int.class, String.class);
            paymentMethodTokenConstructor.setAccessible(true);
            PaymentMethodToken paymentMethodToken = paymentMethodTokenConstructor.newInstance(0, response);

            Constructor<CardInfo> cardInfoConstructor = CardInfo.class.getDeclaredConstructor(String.class, String.class,
                    String.class, int.class, UserAddress.class);
            cardInfoConstructor.setAccessible(true);
            CardInfo cardInfo = cardInfoConstructor.newInstance("MasterCard 0276", null, null, 0, null);

            Constructor<PaymentData> paymentDataConstructor = PaymentData.class.getDeclaredConstructor(String.class,
                    CardInfo.class, UserAddress.class, PaymentMethodToken.class, String.class, Bundle.class, String.class);
            paymentDataConstructor.setAccessible(true);

            return paymentDataConstructor.newInstance(email, cardInfo, null, paymentMethodToken, null, null, response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PostalAddress getPostalAddressObject(JSONObject address) throws JSONException {
        return new PostalAddress()
                .recipientName(Json.optString(address, "name", ""))
                .streetAddress(Json.optString(address, "address1", ""))
                .extendedAddress(
                        String.join("\n",
                                Json.optString(address, "address2", ""),
                                Json.optString(address, "address3", "")
                        ).trim())
                .locality(Json.optString(address, "locality", ""))
                .region(Json.optString(address, "administrativeArea", ""))
                .countryCodeAlpha2(Json.optString(address, "countryCode", ""))
                .postalCode(Json.optString(address, "postalCode", ""));
    }

    private void assertPostalAddress(PostalAddress expected, PostalAddress actual) {
        assertEquals(expected.toString(), actual.toString());
    }
}
