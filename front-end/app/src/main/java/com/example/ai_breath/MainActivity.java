package com.example.ai_breath;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.ai_breath.service.AudioRecorder;
import com.example.ai_breath.viewmodel.AudioViewModel;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 123;

    private ImageView recordImage;
    private TextView recordTitle;

    private View animationLayout;
    private ImageView recordingGif;
    private TextView recordingTitle;

    private View playbackLayout;
    private ImageButton playPauseButton;
    private ImageButton submitButton, recordAgainButton;
    private TextView playbackTitle;
    private SeekBar seekBar;

    private CardView resultCard;
    private TextView predictionText, messageText;
    private ImageButton recordAgainResultButton;

    private AudioViewModel viewModel;
    private AudioRecorder audioRecorder;
    private File audioFile;

    private boolean isRecording = false;
    private boolean isPlaying = false;
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupViewModel();

        if (!checkPermissions()) {
            requestPermissions();
        }

        // Start recording via image or title click
        recordImage.setOnClickListener(v -> {
            if (checkPermissions()) {
                startRecordingUI();
            } else {
                requestPermissions();
            }
        });

        recordTitle.setOnClickListener(v -> {
            if (checkPermissions()) {
                startRecordingUI();
            } else {
                requestPermissions();
            }
        });

        // Stop recording on clicking animation layout (GIF)
        animationLayout.setOnClickListener(v -> stopRecordingAndPreparePlayback());

        // Audio controls
        playPauseButton.setOnClickListener(v -> togglePlayback());

        // Send audio to backend
        submitButton.setOnClickListener(v -> sendAudioToServer());

        // Record again from playback layout
        recordAgainButton.setOnClickListener(v -> resetForNewRecording());

        // Record again button inside result card
        recordAgainResultButton.setOnClickListener(v -> resetForNewRecording());
    }

    private void initViews() {
        recordImage = findViewById(R.id.recordImage);
        recordTitle = findViewById(R.id.recordTitle);

        animationLayout = findViewById(R.id.animationLayout);
        recordingGif = findViewById(R.id.recordingGif);
        recordingTitle = findViewById(R.id.recordingTitle);

        playbackLayout = findViewById(R.id.playbackLayout);
        playPauseButton = findViewById(R.id.playPauseButton);
        submitButton = findViewById(R.id.submitButton);
        recordAgainButton = findViewById(R.id.recordAgainButton);
        playbackTitle = findViewById(R.id.playbackTitle);
        seekBar = findViewById(R.id.seekBar);

        resultCard = findViewById(R.id.resultCard);
        predictionText = findViewById(R.id.predictionText);
        messageText = findViewById(R.id.messageText);
        recordAgainResultButton = findViewById(R.id.recordAgainResultButton);

        animationLayout.setVisibility(View.GONE);
        playbackLayout.setVisibility(View.GONE);
        resultCard.setVisibility(View.GONE);

        submitButton.setEnabled(false);
        recordAgainButton.setEnabled(false);
        recordAgainResultButton.setEnabled(false);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(AudioViewModel.class);

        viewModel.getResponse().observe(this, response -> {
            if (response != null) {
                predictionText.setText("Prediction : " + response.getPrediction());
                messageText.setText("Message : " + response.getMessage());
                resultCard.setVisibility(View.VISIBLE);
                playbackLayout.setVisibility(View.GONE);

                submitButton.setEnabled(true);
                recordAgainButton.setEnabled(true);
                recordAgainResultButton.setEnabled(true);
            } else {
                Toast.makeText(this, "Erreur de traitement du serveur.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startRecordingUI() {
        recordImage.setVisibility(View.GONE);
        recordTitle.setVisibility(View.GONE);
        resultCard.setVisibility(View.GONE);
        playbackLayout.setVisibility(View.GONE);

        animationLayout.setVisibility(View.VISIBLE);
        recordingTitle.setText("Click to stop recording");

        Glide.with(this).asGif().load(R.mipmap.voice_animation).into(recordingGif);

        startRecording();
    }

    private void startRecording() {
        audioRecorder = new AudioRecorder();
        audioFile = audioRecorder.startRecording(getExternalFilesDir(null), "audio_recorded.wav");
        isRecording = true;
        Toast.makeText(this, "Enregistrement en cours...", Toast.LENGTH_SHORT).show();
    }

    private void stopRecordingAndPreparePlayback() {
        if (isRecording && audioRecorder != null) {
            audioRecorder.stopRecording();
            isRecording = false;

            animationLayout.setVisibility(View.GONE);

            setupMediaPlayer();

            playbackLayout.setVisibility(View.VISIBLE);
            playbackTitle.setText("Playback recorded audio");
            playPauseButton.setImageResource(R.mipmap.circled_play_button); // set play icon initially
            submitButton.setEnabled(true);
            recordAgainButton.setEnabled(true);

            Toast.makeText(this, "Enregistrement terminé. Écoutez avant de soumettre.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioFile.getAbsolutePath());
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur lors de la préparation de l'audio.", Toast.LENGTH_SHORT).show();
            return;
        }

        seekBar.setMax(mediaPlayer.getDuration());

        mediaPlayer.setOnCompletionListener(mp -> {
            isPlaying = false;
            playPauseButton.setImageResource(R.mipmap.circled_play_button);
            seekBar.setProgress(0);
            handler.removeCallbacks(updateSeekBar);
        });

        seekBar.setOnSeekBarChangeListener(new AppCompatSeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b && mediaPlayer != null) {
                    mediaPlayer.seekTo(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    private void togglePlayback() {
        if (mediaPlayer == null) return;

        if (isPlaying) {
            mediaPlayer.pause();
            playPauseButton.setImageResource(R.mipmap.circled_play_button);
            isPlaying = false;
            handler.removeCallbacks(updateSeekBar);
        } else {
            mediaPlayer.start();
            playPauseButton.setImageResource(R.mipmap.pause_button);
            isPlaying = true;
            handler.post(updateSeekBar);
        }
    }

    private final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && isPlaying) {
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                handler.postDelayed(this, 100);
            }
        }
    };

    private void sendAudioToServer() {
        if (audioFile != null && audioFile.exists()) {
            Toast.makeText(this, "Envoi au serveur...", Toast.LENGTH_SHORT).show();
            viewModel.sendAudioToServer(audioFile);
            submitButton.setEnabled(false);
            recordAgainButton.setEnabled(false);
            recordAgainResultButton.setEnabled(false);
        }
    }

    private void resetForNewRecording() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlaying = false;

        playbackLayout.setVisibility(View.GONE);
        resultCard.setVisibility(View.GONE);

        submitButton.setEnabled(false);
        recordAgainButton.setEnabled(false);
        recordAgainResultButton.setEnabled(false);

        recordImage.setVisibility(View.VISIBLE);
        recordTitle.setVisibility(View.VISIBLE);
    }

    private boolean checkPermissions() {
        int recordPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return recordPermission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                startRecordingUI();
            } else {
                Toast.makeText(this, "Permissions nécessaires refusées.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
