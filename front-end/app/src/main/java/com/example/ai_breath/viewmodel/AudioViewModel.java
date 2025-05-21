package com.example.ai_breath.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ai_breath.model.ApiResponse;
import com.example.ai_breath.repository.AudioRepository;

import java.io.File;

public class AudioViewModel extends ViewModel {
    private final AudioRepository repository = new AudioRepository();
    private final MutableLiveData<ApiResponse> responseLiveData = new MutableLiveData<>();

    public LiveData<ApiResponse> getResponse() {
        return responseLiveData;
    }

    public void sendAudioToServer(File audioFile) {
        repository.sendAudio(audioFile, responseLiveData);
    }
}

