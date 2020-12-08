package com.example.paymentapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.stripe.android.ApiResultCallback;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.Stripe;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity { ;

    private PaymentsClient paymentsClient;
    ImageView payButton;
    Stripe stripe;

    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 53;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       // stripe.("pk_test_51HuPmDHvu6787W3KV1EkgNpdCTYa3XmhEEfCzDAxkcfZMskHGrMyK2P02imbysrCCKmalso34dz1KZuh9kKWP6ds009iv7WScs");

        paymentsClient = Wallet.getPaymentsClient(this,
                new Wallet.WalletOptions.Builder()
                        .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                        .build());


        try {
            isReadyToPay();
        } catch (JSONException e) {
            e.printStackTrace();
        }

//        try {
//            dataRequest();
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case LOAD_PAYMENT_DATA_REQUEST_CODE: {
                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        if (data != null) {
                            try {
                                onGooglePayResult(data);
                                Log.d("GOOGLEPAY", "SUCCESS");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                    case Activity.RESULT_CANCELED: {
                        // Canceled
                        break;
                    }
                    case AutoResolveHelper.RESULT_ERROR: {
                        // Log the status for debugging
                        // Generally there is no need to show an error to
                        // the user as the Google Payment API will do that
                        final Status status =
                                AutoResolveHelper.getStatusFromIntent(data);
                        break;
                    }
                    default: {
                        // Do nothing.
                    }
                }
                break;
            }
            default: {
                // Handle any other startActivityForResult calls you may have made.
            }
        }
    }

    private void onGooglePayResult(@NonNull Intent data) throws JSONException {
        final PaymentData paymentData = PaymentData.getFromIntent(data);
        if (paymentData == null) {
            return;
        } else {
            Log.d("PAYMENTDATA", String.valueOf(paymentData));
        }

        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParams.createFromGooglePay(
                        new JSONObject(paymentData.toJson()));


        stripe.createPaymentMethod(
                paymentMethodCreateParams,
                new ApiResultCallback<PaymentMethod>() {
                    @Override
                    public void onSuccess(@NonNull PaymentMethod result) {
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                    }
                }
        );
    }

    private void isReadyToPay() throws JSONException {
        final IsReadyToPayRequest request = createIsReadyToPayRequest();
         paymentsClient.isReadyToPay(request)
                .addOnCompleteListener(
                        new OnCompleteListener<Boolean>() {
                            public void onComplete(Task<Boolean> task) {
                                if (task.isSuccessful()) {
                                    // show Google Pay as payment option
                                    Log.d("ISREADY", "YES");
                                    payButton = (ImageView) findViewById(R.id.pay_button);
                                    payButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            try {
                                                payWithGoogle();
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                } else {
                                    // hide Google Pay as payment option
                                    Log.d("ISREADY", "NO");
                                }
                            }
                        }
                );
    }

    @NonNull
    private IsReadyToPayRequest createIsReadyToPayRequest() throws JSONException {
        final JSONArray allowedAuthMethods = new JSONArray();
        allowedAuthMethods.put("PAN_ONLY");
        allowedAuthMethods.put("CRYPTOGRAM_3DS");

        final JSONArray allowedCardNetworks = new JSONArray();
        allowedCardNetworks.put("AMEX");
        allowedCardNetworks.put("DISCOVER");
        allowedCardNetworks.put("MASTERCARD");
        allowedCardNetworks.put("VISA");

        final JSONObject isReadyToPayRequestJson = new JSONObject();
        isReadyToPayRequestJson.put("allowedAuthMethods", allowedAuthMethods);
        isReadyToPayRequestJson.put("allowedCardNetworks", allowedCardNetworks);

        return IsReadyToPayRequest.fromJson(isReadyToPayRequestJson.toString());
    }

    private static JSONObject getGatewayTokenizationSpecification() throws JSONException {
        return new JSONObject() {{
            put("type", "PAYMENT_GATEWAY");
            put("parameters", new JSONObject() {{

                put("gateway", "stripe");
                put("stripe:version", "2018-10-31");
                put("stripe:publishableKey", "pk_test_51HuPmDHvu6787W3KV1EkgNpdCTYa3XmhEEfCzDAxkcfZMskHGrMyK2P02imbysrCCKmalso34dz1KZuh9kKWP6ds009iv7WScs");
            }});
        }};
    }

    @NonNull
    private PaymentDataRequest createPaymentDataRequest() throws JSONException {
        final JSONObject tokenizationSpec =
                getGatewayTokenizationSpecification();
        final JSONObject cardPaymentMethod = new JSONObject()
                .put("type", "CARD")
                .put(
                        "parameters",
                        new JSONObject()
                                .put("allowedAuthMethods", new JSONArray()
                                        .put("PAN_ONLY")
                                        .put("CRYPTOGRAM_3DS"))
                                .put("allowedCardNetworks",
                                        new JSONArray()
                                                .put("AMEX")
                                                .put("DISCOVER")
                                                .put("MASTERCARD")
                                                .put("VISA"))

                                // require billing address
                                .put("billingAddressRequired", false)
                                .put(
                                        "billingAddressParameters",
                                        new JSONObject()
                                                // require full billing address
                                                .put("format", "MIN")

                                                // require phone number
                                                .put("phoneNumberRequired", true)
                                )
                )
                .put("tokenizationSpecification", tokenizationSpec);

        // create PaymentDataRequest
        final JSONObject paymentDataRequest = new JSONObject()
                .put("apiVersion", 2)
                .put("apiVersionMinor", 0)
                .put("allowedPaymentMethods",
                        new JSONArray().put(cardPaymentMethod))
                .put("transactionInfo",
                        new JSONObject()
                        .put("totalPrice", "10.00")
                        .put("totalPriceStatus", "FINAL")
                        .put("currencyCode", "USD")
                )
                .put("merchantInfo",
                        new JSONObject()
                        .put("merchantName", "Example Merchant"))

                // require email address
                .put("emailRequired", true);
                // .toString();

        return PaymentDataRequest.fromJson(String.valueOf(paymentDataRequest));
    }

    private void payWithGoogle() throws JSONException {
        AutoResolveHelper.resolveTask(
                paymentsClient.loadPaymentData(createPaymentDataRequest()),
                this,
                LOAD_PAYMENT_DATA_REQUEST_CODE
        );
    }
}