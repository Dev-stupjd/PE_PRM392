package com.koigzzzz.cex.api;

import com.koigzzzz.cex.models.TokenPrice;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import java.util.List;
import java.util.Map;

public interface CoinGeckoService {
    @GET("api/v3/simple/price")
    Call<Map<String, Map<String, Double>>> getPrices(
        @Query("ids") String ids,
        @Query("vs_currencies") String vsCurrencies,
        @Query("include_24hr_change") boolean include24hrChange,
        @Query("include_24hr_vol") boolean include24hrVol
    );
}

