package com.example.ai_breath.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ai_breath.apis.ApiService;
import com.example.ai_breath.model.ApiResponse;
import com.google.gson.Gson;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AudioRepository {
    private ApiService apiService;

    public AudioRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.22.38:8000/") // ou ton IP locale réelle
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public void sendAudio(File file, MutableLiveData<ApiResponse> liveData) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("audio/wav"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        apiService.analyzeBreath(body).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) {
                    liveData.postValue(response.body());
                } else {
                    liveData.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                // Log the error or handle it as needed
                t.printStackTrace();
                liveData.postValue(null);
            }
        });
    }
}

