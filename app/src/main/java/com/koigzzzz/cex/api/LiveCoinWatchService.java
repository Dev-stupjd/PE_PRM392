package com.koigzzzz.cex.api;

import com.koigzzzz.cex.models.LiveCoinWatchResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import java.util.Map;

public interface LiveCoinWatchService {
    @POST("coins/single")
    Call<LiveCoinWatchResponse> getCoinPrice(
        @Header("x-api-key") String apiKey,
        @Body Map<String, Object> request
    );
}

