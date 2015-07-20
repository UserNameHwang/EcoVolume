package com.android.inputsound;

import android.media.AudioRecord;

/**
 * Created by Sungjung on 2015-06-23.
 */
public class SaveDCB {

    public static int[] getOutDCB() {
        return outDCB;
    }

    public static void setOutDCB(int outDCB) {

        for(int i=0; i<4; i++){
            SaveDCB.outDCB[i] = SaveDCB.outDCB[i+1];
        }
        SaveDCB.outDCB[4] = outDCB;
    }

    public static int[] getInDCB() {
        return inDCB;
    }

    public static void setInDCB(int inDCB) {
        for(int i=0; i<4; i++){
            SaveDCB.inDCB[i] = SaveDCB.inDCB[i+1];
        }
        SaveDCB.inDCB[4] = inDCB;
    }

    public static AudioRecord getAudioRecord() {
        return audioRecord;
    }

    public static void setAudioRecord(AudioRecord audioRecord) {
        SaveDCB.audioRecord = audioRecord;
    }

    private static int[] inDCB = new int[5];
    private static int[] outDCB = new int[5];
    private static AudioRecord audioRecord;
}
