package com.example.ai_breath.service;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import androidx.core.app.ActivityCompat;

import java.io.*;

public class AudioRecorder {

    private static final int SAMPLE_RATE = 16000; // 16kHz = adapté pour la voix
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder;
    private boolean isRecording = false;
    private Thread recordingThread;
    private File wavFile;


    public File startRecording(File outputDir, String filename) {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);


        recorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
        );

        wavFile = new File(outputDir, filename);
        recorder.startRecording();
        isRecording = true;

        recordingThread = new Thread(() -> writeAudioDataToFile(bufferSize), "AudioRecorder Thread");
        recordingThread.start();

        return wavFile;
    }

    public void stopRecording() {
        if (recorder != null) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;

            try {
                // Convertir le fichier PCM temporaire en WAV
                rawToWave(wavFile, wavFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeAudioDataToFile(int bufferSize) {
        byte[] audioData = new byte[bufferSize];

        try (FileOutputStream os = new FileOutputStream(wavFile)) {
            while (isRecording) {
                int read = recorder.read(audioData, 0, bufferSize);
                if (read > 0) {
                    os.write(audioData, 0, read);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void rawToWave(File rawFile, File waveFile) throws IOException {
        byte[] rawData = new byte[(int) rawFile.length()];
        try (DataInputStream input = new DataInputStream(new FileInputStream(rawFile))) {
            input.read(rawData);
        }

        try (DataOutputStream output = new DataOutputStream(new FileOutputStream(waveFile))) {
            // WAV header
            writeWaveFileHeader(output, rawData.length, SAMPLE_RATE, 1, 16);
            output.write(rawData);
        }
    }

    private void writeWaveFileHeader(
            DataOutputStream out, long totalAudioLen, long sampleRate, int channels, int bitsPerSample
    ) throws IOException {
        long byteRate = sampleRate * channels * bitsPerSample / 8;
        long totalDataLen = totalAudioLen + 36;

        out.writeBytes("RIFF");
        out.writeInt(Integer.reverseBytes((int) totalDataLen));
        out.writeBytes("WAVE");
        out.writeBytes("fmt ");
        out.writeInt(Integer.reverseBytes(16)); // Subchunk1Size
        out.writeShort(Short.reverseBytes((short) 1)); // PCM format
        out.writeShort(Short.reverseBytes((short) channels));
        out.writeInt(Integer.reverseBytes((int) sampleRate));
        out.writeInt(Integer.reverseBytes((int) byteRate));
        out.writeShort(Short.reverseBytes((short) (channels * bitsPerSample / 8))); // Block align
        out.writeShort(Short.reverseBytes((short) bitsPerSample));
        out.writeBytes("data");
        out.writeInt(Integer.reverseBytes((int) totalAudioLen));
    }
}
