package com.android.inputsound;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.inputsound.FFT.RealDoubleFFT;

import java.util.HashMap;


public class NoiseCancelingServices extends Service implements Runnable {

    private boolean cancelingStarted = false;

    private Thread cancelingThread;
    private short[][] noisePattern;
    private int numPattern;

    private int frequency = 8000;
    private int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    private int patternSize = 50;
    private int blockSize = 256;

    private final int duration = 4;
    private final int sampleRate = 8000;
    private final int numSamples = duration * sampleRate;
    private final double sample[] = new double[numSamples];
    private double freqOfTone;

    private byte generatedSnd[][] = new byte[3][2 * numSamples];

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
        noisePattern = new short[patternSize][blockSize];

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

        AudioRecord audioRecord;
        if(SaveDCB.getAudioRecord() == null)
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, audioEncoding, 8000);
        else
            audioRecord = SaveDCB.getAudioRecord();

        audioRecord.startRecording();
        while (cancelingStarted) {
            audioRecord.startRecording();

            if(numPattern < patternSize) {
                short[] buffer = new short[blockSize];
                buffer = new short[blockSize];

                audioRecord.read(buffer, 0, blockSize);

                //50개의 연속적인 short 배열을 저장한다
                noisePattern[numPattern] = buffer;
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                numPattern++;

                if( numPattern == patternSize ){
                    analysisPattern(noisePattern);
                    numPattern = 0;
                }
            }
        }

        audioRecord.stop();
    }

    //패턴 분석 함수
    public void analysisPattern(short[][] inPattern) {
        RealDoubleFFT transformer = new RealDoubleFFT(blockSize);
        double[][] toTransform = new double[patternSize][blockSize];

        int[] countPattern = new int[blockSize];
        int[] NumPattern = new int[blockSize];

        for(int i=0; i<blockSize; i++){
            NumPattern[i] = i;
        }

        for(int i=0; i<patternSize; i++ ) {
            for (int j=0; j<blockSize; j++) {
                toTransform[i][j] = (double) inPattern[i][j] / Short.MAX_VALUE;
            }
            transformer.ft(toTransform[i]);
        }

        double maxValue;
        int maxNum;
        for(int i=0; i<patternSize; i++) {
            maxNum = 0;
            maxValue = 0.0;
            for(int j=0 ; j<blockSize; j++) {
                if (toTransform[i][j] > maxValue) {
                    maxValue = toTransform[i][j];
                    maxNum = j;
                }
            }
            countPattern[maxNum]++;
        }
        countPattern[blockSize-1]=0;

        String log = "";
        log += numPattern + "-- ";

        for (int i=0; i<blockSize; i++) {
            log += i+":"+countPattern[i] + ",,";
        }
        Log.w("Analysis ori Log "+countPattern, log);
        int max, temp;
        for(int i=0; i<countPattern.length-1; i++) {
            max = i;

            for(int j=i+1; j<countPattern.length; j++) {
                if(countPattern[j] > countPattern[max]) {
                    max = j;
                }
            }
            temp = countPattern[i];
            countPattern[i] = countPattern[max];
            countPattern[max] = temp;

            temp = NumPattern[i];
            NumPattern[i] = NumPattern[max];
            NumPattern[max] = temp;

        }

        log = numPattern + "-- ";
        for (int i=0; i<blockSize; i++) {
            log += NumPattern[i] + ":" + countPattern[i] + ",,";
        }
        Log.w("Analysis Sorted Log "+countPattern, log);

        int maxIntValue = 0;
        maxNum = 0;
        for(int i=0; i<blockSize; i++) {
            if(countPattern[i] > maxIntValue) {
                maxIntValue = countPattern[i];
                maxNum = i;
            }
        }

        for(int i = 0; i < 3; i++) {

            freqOfTone = 15.625 * NumPattern[i];
            Log.w("Freq & maxNum", freqOfTone + " Hz, " + NumPattern[i]);
            // fill out the array
            for (int j = 0; j < numSamples; ++j) {
                sample[j] = Math.sin(2 * Math.PI * j / (sampleRate / freqOfTone));
            }

            // convert to 16 bit pcm sound array
            // assumes the sample buffer is normalised.
            int idx = 0;
            for (final double dVal : sample) {
                // scale to maximum amplitude / 32
                final short val = (short) ((dVal * 32767 / 32));
                // in 16 bit wav PCM, first byte is the low order byte
                generatedSnd[i][idx++] = (byte) (val & 0x00ff);
                generatedSnd[i][idx++] = (byte) ((val & 0xff00) >>> 8);
            }
        }

        for(int i=0; i<3; i++) {
            TrackWrite tw = new TrackWrite(generatedSnd[i]);
            tw.start();
        }

    }

    private class TrackWrite extends Thread{

        private byte[] playingSnd;
        private AudioTrack audioTrack;
        public TrackWrite(byte[] generatedSnd){
            this.playingSnd = generatedSnd;

            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_MONO,
                    audioEncoding, 8000, AudioTrack.MODE_STREAM);
            audioTrack.play();
        }
        @Override
        public void run() {

            audioTrack.write(playingSnd, 0, playingSnd.length);

        }
    }
}

