package com.braintreepayments.api;

// TODO: fix after google pay supports BraintreeClient
//@RunWith(AndroidJUnit4ClassRunner.class)
//public class GooglePaymentTest {
//
//    @Rule
//    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
//            new BraintreeActivityTestRule<>(TestActivity.class);
//
//    private CountDownLatch mLatch;
//    private TestConfigurationBuilder mBaseConfiguration;
//
//    @Before
//    public void setup() {
//        mLatch = new CountDownLatch(1);
//        mBaseConfiguration = new TestConfigurationBuilder()
//                .googlePayment(new TestGooglePaymentConfigurationBuilder()
//                        .googleAuthorizationFingerprint("google-auth-fingerprint"))
//                .merchantId("android-pay-merchant-id");
//    }

//
//    @Test(timeout = 5000)
//    public void getTokenizationParameters_returnsCorrectParameters() throws Exception {
//        String config = mBaseConfiguration.withAnalytics().build();
//
//        final BraintreeFragment fragment = getFragment(mActivityTestRule.getActivity(), Fixtures.TOKENIZATION_KEY, config);
//
//        fragment.waitForConfiguration(new ConfigurationListener() {
//            @Override
//            public void onConfigurationFetched(Configuration configuration) {
//                Bundle tokenizationParameters = GooglePayment.getTokenizationParameters(fragment).getParameters();
//
//                assertEquals("braintree", tokenizationParameters.getString("gateway"));
//                assertEquals(configuration.getMerchantId(), tokenizationParameters.getString("braintree:merchantId"));
//                assertEquals(configuration.getGooglePayment().getGoogleAuthorizationFingerprint(),
//                        tokenizationParameters.getString("braintree:authorizationFingerprint"));
//                assertEquals("v1", tokenizationParameters.getString("braintree:apiVersion"));
//                assertEquals(BuildConfig.VERSION_NAME, tokenizationParameters.getString("braintree:sdkVersion"));
//
//                mLatch.countDown();
//            }
//        });
//
//        mLatch.await();
//    }
//
//    @Test(timeout = 5000)
//    public void getTokenizationParameters_doesNotIncludeATokenizationKeyWhenNotPresent() {
//        final BraintreeFragment fragment = getFragment(mActivityTestRule.getActivity(),
//                base64Encode(Fixtures.CLIENT_TOKEN), mBaseConfiguration.build());
//
//        fragment.waitForConfiguration(new ConfigurationListener() {
//            @Override
//            public void onConfigurationFetched(Configuration configuration) {
//                Bundle tokenizationParameters = GooglePayment.getTokenizationParameters(fragment).getParameters();
//
//                assertNull(tokenizationParameters.getString("braintree:clientKey"));
//
//                mLatch.countDown();
//            }
//        });
//
//        mLatch.countDown();
//    }
//
//    @Test(timeout = 5000)
//    public void getTokenizationParameters_includesATokenizationKeyWhenPresent() throws Exception {
//        final BraintreeFragment fragment = getFragment(mActivityTestRule.getActivity(), Fixtures.TOKENIZATION_KEY,
//                mBaseConfiguration.withAnalytics().build());
//
//        fragment.waitForConfiguration(new ConfigurationListener() {
//            @Override
//            public void onConfigurationFetched(Configuration configuration) {
//                Bundle tokenizationParameters = GooglePayment.getTokenizationParameters(fragment).getParameters();
//
//                assertEquals(Fixtures.TOKENIZATION_KEY,  tokenizationParameters.getString("braintree:clientKey"));
//
//                mLatch.countDown();
//            }
//        });
//
//        mLatch.await();
//    }
//
//    @Test(timeout = 5000)
//    public void getAllowedCardNetworks_returnsSupportedNetworks() throws InterruptedException {
//        String configuration = mBaseConfiguration.googlePayment(mBaseConfiguration.googlePayment()
//                .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
//                .build();
//
//        final BraintreeFragment fragment = getFragmentWithConfiguration(mActivityTestRule.getActivity(), configuration);
//
//        fragment.waitForConfiguration(new ConfigurationListener() {
//            @Override
//            public void onConfigurationFetched(Configuration configuration) {
//                Collection<Integer> allowedCardNetworks = GooglePayment.getAllowedCardNetworks(fragment);
//
//                assertEquals(4, allowedCardNetworks.size());
//                assertTrue(allowedCardNetworks.contains(CardNetwork.VISA));
//                assertTrue(allowedCardNetworks.contains(CardNetwork.MASTERCARD));
//                assertTrue(allowedCardNetworks.contains(CardNetwork.AMEX));
//                assertTrue(allowedCardNetworks.contains(CardNetwork.DISCOVER));
//
//                mLatch.countDown();
//            }
//        });
//
//        mLatch.await();
//    }
//
//    @Test(timeout = 5000)
//    public void getTokenizationParameters_returnsCorrectParametersInCallback() throws Exception {
//        String config = mBaseConfiguration.googlePayment(mBaseConfiguration.googlePayment()
//                .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
//                .build();
//        final Configuration configuration = Configuration.fromJson(config);
//
//        BraintreeFragment fragment = getFragment(mActivityTestRule.getActivity(), Fixtures.TOKENIZATION_KEY, config);
//
//        GooglePayment.getTokenizationParameters(fragment, new TokenizationParametersListener() {
//            @Override
//            public void onResult(PaymentMethodTokenizationParameters parameters,
//                    Collection<Integer> allowedCardNetworks) {
//                assertEquals("braintree", parameters.getParameters().getString("gateway"));
//                assertEquals(configuration.getMerchantId(),
//                        parameters.getParameters().getString("braintree:merchantId"));
//                assertEquals(configuration.getGooglePayment().getGoogleAuthorizationFingerprint(),
//                        parameters.getParameters().getString("braintree:authorizationFingerprint"));
//                assertEquals("v1",
//                        parameters.getParameters().getString("braintree:apiVersion"));
//                assertEquals(BuildConfig.VERSION_NAME,
//                        parameters.getParameters().getString("braintree:sdkVersion"));
//
//                try {
//                    JSONObject metadata = new JSONObject(parameters.getParameters().getString("braintree:metadata"));
//                    assertNotNull(metadata);
//                    assertEquals(BuildConfig.VERSION_NAME, metadata.getString("version"));
//                    assertNotNull(metadata.getString("sessionId"));
//                    assertEquals("custom", metadata.getString("integration"));
//                    assertEquals("android", metadata.get("platform"));
//                } catch (JSONException e) {
//                    fail("Failed to unpack json from tokenization parameters: " + e.getMessage());
//                }
//
//                assertEquals(4, allowedCardNetworks.size());
//                assertTrue(allowedCardNetworks.contains(CardNetwork.VISA));
//                assertTrue(allowedCardNetworks.contains(CardNetwork.MASTERCARD));
//                assertTrue(allowedCardNetworks.contains(CardNetwork.AMEX));
//                assertTrue(allowedCardNetworks.contains(CardNetwork.DISCOVER));
//
//                mLatch.countDown();
//            }
//        });
//
//        mLatch.await();
//    }
//
//    private BraintreeFragment getSetupFragment() {
//        String configuration = mBaseConfiguration.googlePayment(mBaseConfiguration.googlePayment()
//                .environment("sandbox")
//                .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
//                .googlePayment(new TestGooglePaymentConfigurationBuilder()
//                        .environment("sandbox")
//                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
//                        .paypalClientId("paypal-client-id-for-google-payment")
//                        .enabled(true))
//                .paypal(new TestConfigurationBuilder.TestPayPalConfigurationBuilder(true)
//                        .clientId("paypal-client-id-for-paypal"))
//                .withAnalytics()
//                .build();
//
//        BraintreeFragment fragment = new MockFragmentBuilder()
//                .configuration(configuration)
//                .build();
//
//        try {
//            when(fragment.getAuthorization()).thenReturn(Authorization.fromString("sandbox_tokenization_key"));
//        } catch (InvalidArgumentException e) {
//            throw new RuntimeException(e);
//        }
//
//        return fragment;
//    }
//}
