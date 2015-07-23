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

    private short[][] noisePattern;
    private int numPattern;
    private boolean finPattern;

    private int frequency = 8000;
    private int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private int blockSize = 256;

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

        //패턴초기화
        numPattern = 0;
        noisePattern = new short[50][blockSize];
        finPattern = false;


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



    @Override
    public void run() {

        int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);

        AudioRecord audioRecord;
        if(SaveDCB.getAudioRecord() == null)
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, audioEncoding, bufferSize);
        else
             audioRecord = SaveDCB.getAudioRecord();

        int maxJitter = AudioTrack.getMinBufferSize(frequency, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        AudioTrack audioTrack = new AudioTrack(AudioManager.MODE_IN_COMMUNICATION, 8000, AudioFormat.CHANNEL_OUT_MONO,
                audioEncoding, maxJitter, AudioTrack.MODE_STREAM);

        short[] buffer = new short[blockSize];

        audioTrack.play();
        while (cancelingStarted) {
            audioRecord.startRecording();
            int bufferReadResult = audioRecord.read(buffer, 0, blockSize);

            //50개의 연속적인 short 배열을 저장한다
            noisePattern[numPattern] = buffer;

            //50개의 배열이 완성되면 패턴을 분석한다
            if(finPattern)
                analysisPattern();

            audioTrack.write(noisePattern[numPattern], 0, bufferReadResult);

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //패턴입력 처음으로 초기화
            if(numPattern==49) {
                numPattern = 0;
                finPattern = true;
            }
            else
                numPattern++;

        }

        audioTrack.stop();
        audioRecord.stop();
    }


    //패턴 분석 함수
    public void analysisPattern()
    {
















    }
}
