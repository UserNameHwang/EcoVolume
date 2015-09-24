package com.android.inputsound;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.support.v7.app.NotificationCompat;

public class VolumeAlertThread extends Thread {

    private boolean VolumeAlertStarted = false;

    private double SPL;
    private NotificationCompat.Builder builder;

    private Context c;

    public VolumeAlertThread(Context c) {

        this.c = c;

        builder = new NotificationCompat.Builder(c);

        // ���� ������ �̹���.
        builder.setSmallIcon(R.mipmap.ic_launcher);
        // �˸��� ��µ� �� ��ܿ� ������ ����.
        builder.setTicker("������� ������ �ʹ� ���ƿ�!");
        // �˸� ��� �ð�.
        builder.setWhen(System.currentTimeMillis());
        // �˸� ����.
        builder.setContentTitle("���ں���");
        // ���α׷��� ��.
        //builder.setProgress(100, 50, false);
        // �˸� ����.
        builder.setContentText("������� ������ �ʹ� ���ƿ�!");
        // �˸��� ����, ����, �Һ��� ���� ����.
        builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS);
        // �˸� ��ġ�� ����.
        //PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        //builder.setContentIntent(pendingIntent);
        // �˸� ��ġ�� ���� �� �˸� ���� ����.
        builder.setAutoCancel(true);
        // �켱����.
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        //����
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

                    // ���� ���� : W = V * V / R
                    double Watt = (VoltagePerVol[mCurvol] * VoltagePerVol[mCurvol]) / Impedance;
                    double MillWatt = Watt / 1000;
                    // ���¿����� dB ���� : dB = 10 * log(���Ǵ����� ����/���� ���� ����)
                    double dB;
                    if (MillWatt != 0)
                        dB = 10 * Math.log10(OhmofImp / MillWatt);
                    else
                        dB = Sensitivity;
                    // ���� ��� ���� dB : ������ ���ú� - ���� ������ ���ú�
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