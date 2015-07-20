package com.android.inputsound;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;


public class NoiseCancelingServices extends Service implements Runnable {

    private boolean cancelingStarted = false;

    private Thread cancelingThread;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        cancelingThread = new Thread(this);

        cancelingThread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w("ServiceLog", "CancelingService Started");

        cancelingStarted = true;
        if(cancelingThread.getState().toString().equals("TERMINATED")){
            cancelingThread = new Thread(this);

            cancelingThread.start();
            Log.w("ServiceLog", "CancelingService Restarted");
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        Log.w("ServiceLog", "CancelingService Destroyed");

        cancelingStarted = false;
        super.onDestroy();
    }

    int frequency = 8000;
    int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;

    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    int blockSize = 256;

    @Override
    public void run() {

        int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);

        AudioRecord audioRecord = SaveDCB.getAudioRecord();

        int maxJitter = AudioTrack.getMinBufferSize(frequency, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        AudioTrack audioTrack = new AudioTrack(AudioManager.MODE_IN_COMMUNICATION, 8000, AudioFormat.CHANNEL_OUT_MONO,
                audioEncoding, maxJitter, AudioTrack.MODE_STREAM);

        short[] buffer = new short[blockSize];

        audioTrack.play();
        while (cancelingStarted) {
                if(audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED)
                    audioRecord.startRecording();
                int bufferReadResult = audioRecord.read(buffer, 0, blockSize);

                audioTrack.write(buffer, 0, bufferReadResult);
        }

        audioTrack.stop();
        audioRecord.stop();
    }
}
