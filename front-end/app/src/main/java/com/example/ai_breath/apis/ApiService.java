package com.example.ai_breath.apis;

import com.example.ai_breath.model.ApiResponse;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {

    @Multipart
    @POST("analyze_breath")
    Call<ApiResponse> analyzeBreath(
            @Part MultipartBody.Part file
    );
}
