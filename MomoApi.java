package com.example.ohms.api;

import com.google.gson.annotations.SerializedName;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface MomoApi {
    
    class Request {
        @SerializedName("amount") public String amount;
        @SerializedName("currency") public String currency;
        @SerializedName("externalId") public String externalId;
        @SerializedName("payer") public Payer payer;
        @SerializedName("payerMessage") public String payerMessage;
        @SerializedName("payeeNote") public String payeeNote;

        public Request(String amount, String phone, String externalId) {
            this.amount = amount;
            this.currency = "UGX";
            this.externalId = externalId;
            this.payer = new Payer(phone);
            this.payerMessage = "OHMS Loan Repayment";
            this.payeeNote = "Payment for loan " + externalId;
        }
    }

    class Payer {
        @SerializedName("partyIdType") public String partyIdType = "MSISDN";
        @SerializedName("partyId") public String partyId;
        public Payer(String phone) { this.partyId = phone; }
    }

    @POST("collection/v1_0/requesttopay")
    Call<Void> requestToPay(
        @Header("Authorization") String auth,
        @Header("X-Reference-Id") String referenceId,
        @Header("X-Target-Environment") String targetEnv,
        @Header("Ocp-Apim-Subscription-Key") String subKey,
        @Body Request request
    );
}
