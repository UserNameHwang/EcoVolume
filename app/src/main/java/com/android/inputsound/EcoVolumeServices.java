package com.android.inputsound;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

public class EcoVolumeServices extends Service implements Runnable {

    private boolean ecoStarted = false;
    private boolean VolumeAlertStarted = false;

    private Thread ecoThread;
    //private VolumeAlertThread va;
    private double MIN_DECIBEL = 75;
    private double SPL = 75;

    private NotificationCompat.Builder builder;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        ecoThread = new Thread(this);
        //va = new VolumeAlertThread(this);

        ecoThread.start();
        //va.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w("ServiceLog", "EcoService Started");

        ecoStarted = true;
        if (ecoThread.getState().toString().equals("TERMINATED")) {
            ecoThread = new Thread(this);

            ecoThread.start();
            Log.w("ServiceLog", "EcoService Restarted");
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        Log.w("ServiceLog", "EcoService Destroyed");

        ecoStarted = false;
        VolumeAlertStarted = false;
        super.onDestroy();
    }

    @Override
    public void run() {
        // Sample Smartphone 볼륨 당 음압전류
        /*
        0 = 0.00        1 = 0.70
        2 = 1.79        3 = 3.15
        4 = 4.56        5 = 6.63
        6 = 8.18        7 = 10.40
        8 = 12.98        9 = 16.63
        10 = 21.03        11 = 25.98
        12 = 32.83        13 = 41.25
        14 = 51.85        15 = 57.92
        */
        // Sample Ear Receiver 음압 : 112dB/mW, 임피던스 : 16ohm
        double[] VoltagePerVol =
                {0.0, 0.7, 1.79, 3.15, 4.56, 6.63, 8.18, 10.4, 12.98, 16.63, 21.03, 25.98, 32.83, 41.25, 51.85, 57.92};
        int Impedance = 16;
        double OhmofImp = 1;
        int Sensitivity = 112;

        AudioManager audiomanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        while (ecoStarted) {

            try {
                int mCurvol = audiomanager.getStreamVolume(audiomanager.STREAM_MUSIC);
                Log.w("Current Volume", "volume : " + mCurvol);

                // 전력 계산식 : W = V * V / R
                double Watt = (VoltagePerVol[mCurvol] * VoltagePerVol[mCurvol]) / Impedance;
                double MillWatt = Watt / 1000;
                // 전력에서의 dB 계산식 : dB = 10 * log(임피던스의 전력/현재 볼륨 전력)
                double dB;
                if (MillWatt != 0)
                    dB = 10 * Math.log10(OhmofImp / MillWatt);
                else
                    dB = Sensitivity;
                // 실제 출력 볼륨 dB : 감도의 데시벨 - 현재 전력의 데시벨
                SPL = Sensitivity - dB;

                SharedPreferences sp = getSharedPreferences("pref", MODE_PRIVATE);
                MIN_DECIBEL = sp.getInt("MIN_DCB", 75);
                //        MIN_DECIBEL = SaveUserSetting.GetLimitDcb();

                if ((int) SPL > MIN_DECIBEL)
                    audiomanager.setStreamVolume(audiomanager.STREAM_MUSIC,
                            mCurvol - 1, audiomanager.FLAG_REMOVE_SOUND_AND_VIBRATE);

                Thread.sleep(3000);
            } catch (Throwable t) {
                Log.e("EcoVolume", "EcoVolume Failed");
            }
        }
    }

    /*
    private class VolumeAlertThread extends Thread {

        public VolumeAlertThread(Context c) {
            builder = new NotificationCompat.Builder(c);

            // 작은 아이콘 이미지.
            builder.setSmallIcon(R.mipmap.ic_launcher);
            // 알림이 출력될 때 상단에 나오는 문구.
            builder.setTicker("사용자의 볼륨이 너무 높아요!");
            // 알림 출력 시간.
            builder.setWhen(System.currentTimeMillis());
            // 알림 제목.
            builder.setContentTitle("에코볼륨");
            // 프로그래스 바.
            //builder.setProgress(100, 50, false);
            // 알림 내용.
            builder.setContentText("사용자의 볼륨이 너무 높아요!");
            // 알림시 사운드, 진동, 불빛을 설정 가능.
            builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS);
            // 알림 터치시 반응.
            //PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
            //builder.setContentIntent(pendingIntent);
            // 알림 터치시 반응 후 알림 삭제 여부.
            builder.setAutoCancel(true);
            // 우선순위.
            builder.setPriority(NotificationCompat.PRIORITY_MAX);
            //진동
            builder.setVibrate(new long[]{1000});

        }

        @Override
        public void run() {
            int timeCount = 0;

            while(true) {

                VolumeAlertStarted = SaveUserSetting.isVolumeAlertStarted();
                if(VolumeAlertStarted == true) {
                    try {
                        if (SPL >= 90) {
                            timeCount++;
                        } else {
                            timeCount = 0;
                        }

                        if(timeCount == 15) {
                            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                            manager.notify(1, builder.build());
                        }
                        Thread.sleep(1000);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
                else{
                    if(ecoStarted == true) {
                        try {
                            Thread.sleep(1000);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                }
            }
        }
    }
    */

}
