package com.android.inputsound;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.List;

/**
 * Created by Administrator on 2015-06-21.
 */
public class NotificationServices extends Service implements Runnable {

    private boolean notiStarted = false;

    private Bitmap RedPoint;
    private Bitmap GreenPoint;

    private boolean EcoSvcRunning;
    private boolean NoiseSvcRunning;

    private Thread noitThread;

    private Notification.Builder builder;
    private BroadcastReceiver ecoBroadcastReceiver;
    private BroadcastReceiver cancelBroadcastReceiver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        builder = new Notification.Builder(getApplicationContext());
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setTicker("에코볼륨이 실행중입니다.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w("ServiceLog", "Notification Service Started");

        notiStarted = true;

        // Bitmap 로딩
        Bundle bitmapBundle = new Bundle();
        bitmapBundle = intent.getBundleExtra("bitmap");

        RedPoint = bitmapBundle.getParcelable("Red");
        GreenPoint = bitmapBundle.getParcelable("Green");

        // 프로세스가 종료되었을 때 Notification 을 이용하여 간단히 기능 조작
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification noti = builder.build();

        Intent ecoIntent = new Intent("notiEcoButton");
        PendingIntent ecoPendingIntent = PendingIntent.getBroadcast(this, 0, ecoIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent cancelIntent = new Intent("notiCancel");
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(this, 0, cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final RemoteViews contentiew = new RemoteViews(getPackageName(), R.layout.remote_view);

        contentiew.setOnClickPendingIntent(R.id.notiEcoButton, ecoPendingIntent);
        contentiew.setOnClickPendingIntent(R.id.notiCancel, cancelPendingIntent);

        // 에코버튼 서비스 실행 여부 확인
        EcoSvcRunning = isServiceRunning("com.android.inputsound.Services");

        if(EcoSvcRunning == true)
            contentiew.setImageViewBitmap(R.id.notiEcoImage, GreenPoint);
        else
            contentiew.setImageViewBitmap(R.id.notiEcoImage, RedPoint);

        ecoBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO Auto-generated method stub

                if(EcoSvcRunning == true) {
                    EcoSvcRunning = false;
                    refreshEcoNotification(RedPoint);
                    stopService(new Intent(getApplicationContext(), Services.class));
                }
                else {
                    EcoSvcRunning = true;
                    refreshEcoNotification(GreenPoint);
                    startService(new Intent(getApplicationContext(), Services.class));
                }
            }
        };

        cancelBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO Auto-generated method stub
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                stopService(new Intent(getApplicationContext(), Services.class));
                stopService(new Intent(getApplicationContext(), NotificationServices.class));

                Toast.makeText(getApplicationContext(),"에코볼륨 서비스가 종료됩니다!", Toast.LENGTH_LONG).show();
                nm.cancelAll();
            }
        };

        IntentFilter ecoFilter = new IntentFilter();
        ecoFilter.addAction("notiEcoButton");

        IntentFilter cancelFilter = new IntentFilter();
        cancelFilter.addAction("notiCancel");

        registerReceiver(ecoBroadcastReceiver, ecoFilter);
        registerReceiver(cancelBroadcastReceiver, cancelFilter);

        noti.contentView = contentiew;
        nm.notify(2, noti);
        //noti.flags |= Notification.FLAG_NO_CLEAR;

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        Log.w("ServiceLog", "Notification Service Destroyed");

        unregisterReceiver(ecoBroadcastReceiver);
        unregisterReceiver(cancelBroadcastReceiver);
        notiStarted = false;
        super.onDestroy();
    }

    @Override
    public void run() {

        while (notiStarted) {
            try {

                Thread.sleep(3000);
            } catch (Throwable t) {
                Log.e("EcoVolume", "Notification Service Failed");
            }
        }
    }

    // serviceName : manifest에서 설정한 서비스의 이름
    private Boolean isServiceRunning(String serviceName) {
        ActivityManager manager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningServiceInfo> RunningService = manager.getRunningServices(Integer.MAX_VALUE);
        for (int i=0; i< RunningService.size(); i++) {
            ActivityManager.RunningServiceInfo rsi = RunningService.get(i);

            if( serviceName.equals(rsi.service.getClassName()))
                return true;
        }
        return false;
    }

    private void refreshEcoNotification(Bitmap bitmap) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(2);

        Notification noti = builder.build();

        Intent ecoIntent = new Intent("notiEcoButton");
        PendingIntent ecoPendingIntent = PendingIntent.getBroadcast(this, 0, ecoIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent cancelIntent = new Intent("notiCancel");
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(this, 0, cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final RemoteViews contentiew = new RemoteViews(getPackageName(), R.layout.remote_view);
        contentiew.setOnClickPendingIntent(R.id.notiEcoButton, ecoPendingIntent);
        contentiew.setOnClickPendingIntent(R.id.notiCancel, cancelPendingIntent);

        contentiew.setImageViewBitmap(R.id.notiEcoImage, bitmap);

        IntentFilter ecoFilter = new IntentFilter();
        ecoFilter.addAction("notiEcoButton");

        IntentFilter cancelFilter = new IntentFilter();
        cancelFilter.addAction("notiCancel");

        unregisterReceiver(ecoBroadcastReceiver);
        unregisterReceiver(cancelBroadcastReceiver);

        registerReceiver(ecoBroadcastReceiver, ecoFilter);
        registerReceiver(cancelBroadcastReceiver, cancelFilter);

        noti.contentView = contentiew;
        nm.notify(2, noti);
    }
}
