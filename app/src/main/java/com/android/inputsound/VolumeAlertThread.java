package com.android.inputsound;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

public class VolumeAlertThread extends Thread {

    private boolean VolumeAlertStarted = false;

    private double SPL;
    private NotificationCompat.Builder builder;

    private Context c;

    public VolumeAlertThread(Context c) {

        this.c = c;

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
        SharedPreferences sp = c.getApplicationContext().getSharedPreferences("pref", c.getApplicationContext().MODE_PRIVATE);

        double[] VoltagePerVol =
                {0.0, 0.7, 1.79, 3.15, 4.56, 6.63, 8.18, 10.4, 12.98, 16.63, 21.03, 25.98, 32.83, 41.25, 51.85, 57.92};
        int Impedance = 16;
        double OhmofImp = 1;
        int Sensitivity = 112;

        AudioManager audiomanager = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        while (true) {

            VolumeAlertStarted = sp.getBoolean("VolumeAlert", false);
            if (VolumeAlertStarted == true) {
                try {
                    int mCurvol = audiomanager.getStreamVolume(audiomanager.STREAM_MUSIC);

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

                    if (SPL >= 90) {
                        timeCount++;
                    } else {
                        timeCount = 0;
                    }

                    if (timeCount == 15) {
                        NotificationManager manager = (NotificationManager) c.getSystemService(c.NOTIFICATION_SERVICE);

                        manager.notify(1, builder.build());
                    }
                    Thread.sleep(1000);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            } else {
                try {
                    Thread.sleep(1000);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }
}