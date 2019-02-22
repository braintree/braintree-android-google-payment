# Braintree Android Google Pay SDK Release Notes

## Unreleased

* Resolve issue where optional shipping parameters were treated as if they were required

## 3.0.0

* Convert to AndroidX
* Replace AndroidPayConfiguration with GooglePaymentConfiguration

## 2.0.1

* Disable PayPal payment method in Google Payment when the merchant is not able to process PayPal

## 2.0.0

* Add support for Google Pay v2
* Remove support for Google Pay v1
  * To continue using v1, add google-payment:1.0.0 to your build.gradle
  * v1 will remain the defaul for braintree android until the next major version bump
* Replace all UserAddress objects with PostalAddress objects

## 1.0.0

* Enable Google Payment as a separate and independently versioned moduled from Braintree Android
* Supports Google Pay v1
* Represents Google Payment as it was in Braintree Android version 2.18.1
