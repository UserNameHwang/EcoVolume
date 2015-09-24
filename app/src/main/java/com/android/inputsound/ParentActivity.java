package com.android.inputsound;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.gc.materialdesign.views.ButtonRectangle;

import java.util.List;

public class ParentActivity extends AppCompatActivity {

	private SlidingTabsBasicFragment fragment;

	private final long INTERVAL = 2000;
	private long backTime = 0;

	private final static int ID_ACTIVITY = 1;
	private final static int SCH_ACTIVITY = 2;

	private boolean Ecostarted = false;
	private boolean NCstarted = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		
		FragmentTransaction transaction = getSupportFragmentManager()
				.beginTransaction();
		fragment = new SlidingTabsBasicFragment();
		transaction.replace(R.id.sample_content_fragment, fragment);
		transaction.commit();

		ActionBar bar = getSupportActionBar();
		bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#1E88E5")));
		bar.setDisplayUseLogoEnabled(false);


		// Service 실행 여부 판단
		boolean EcosvcRunning = isServiceRunning("com.android.inputsound.EcoVolumeServices");
		Log.w("svc Check", "" + EcosvcRunning);
		if(EcosvcRunning) {
			Ecostarted = true;
		}
		else {
			Ecostarted = false;
		}

		boolean NCsvcRunning = isServiceRunning("com.android.inputsound.NoiseCancelingServices");
		Log.w("svc Check", "" + NCsvcRunning);
		if(NCsvcRunning) {
			NCstarted = true;
		}
		else {
			NCstarted = false;
		}

		// Notification Service 실행 여부 판단
		boolean NsvcRunning = isServiceRunning("com.android.inputsound.NotificationServices");
		Log.w("Nsvc Check", "" + NsvcRunning);
		if(NsvcRunning) {
			stopService(new Intent(getApplicationContext(), NotificationServices.class) );
		}

		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(2);


		// default MIN_DECIBEL
		SharedPreferences sp = getSharedPreferences("pref", MODE_PRIVATE);

		SaveUserSetting.SetLimitDcb((double) sp.getInt("MIN_DCB", 75));

		// Alert 기능 실행
		VolumeAlertThread vat = new VolumeAlertThread(this);
		vat.start();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, intent);

		if( requestCode == ID_ACTIVITY )
		{
			if( resultCode == RESULT_OK )
			{

			}
		}

		else if( requestCode == SCH_ACTIVITY)
		{
			if( resultCode == RESULT_OK)
			{

			}
		}
	}

	// EcoVolume onClick Function
	public void onEcoVolume(View v){

		ButtonRectangle EcoButton = (ButtonRectangle) v;
		com.gc.materialdesign.views.Switch ecoSwitch = (com.gc.materialdesign.views.Switch)findViewById(R.id.ecoSwitch);

		if(Ecostarted){
			Ecostarted = false;
			SaveUserSetting.setEcoVolumeStarted(false);
			ecoSwitch.setChecked(false);
			EcoButton.setText("에코볼륨\n시작하기");
			stopService(new Intent(getApplicationContext(), EcoVolumeServices.class));
		}else{
			Ecostarted = true;
			SaveUserSetting.setEcoVolumeStarted(true);
			ecoSwitch.setChecked(true);
			EcoButton.setText("에코볼륨\n중단하기");
			startService(new Intent(getApplicationContext(), EcoVolumeServices.class));
		}
	}

	// NoiseCanceling onClick Function
	public void onNoiseCancel(View v){

		ButtonRectangle NoiseButton = (ButtonRectangle) v;
		com.gc.materialdesign.views.Switch noiseSwitch = (com.gc.materialdesign.views.Switch)findViewById(R.id.noiseSwitch);

		if(NCstarted){
			NCstarted = false;
			SaveUserSetting.setNoiseCancelStarted(false);
			noiseSwitch.setChecked(false);
			NoiseButton.setText("노이즈캔슬링\n시작하기");
			stopService(new Intent(getApplicationContext(), NoiseCancelingServices.class));
		}else{
			NCstarted = true;
			SaveUserSetting.setNoiseCancelStarted(true);
			noiseSwitch.setChecked(true);
			NoiseButton.setText("노이즈캔슬링\n중단하기");
			startService(new Intent(getApplicationContext(), NoiseCancelingServices.class));
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

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		long tempTime = System.currentTimeMillis();
		long interval = tempTime - backTime;

		if (interval >= 0 && INTERVAL >= interval) {

			// 사용자가 설정한 하한 볼륨을 저장
			SharedPreferences sp = getSharedPreferences("pref", MODE_PRIVATE);
			SharedPreferences.Editor editor = sp.edit();

			editor.putInt("MIN_DCB", (int) SaveUserSetting.GetLimitDcb());
			editor.apply();

			// Notification Service Start
			Intent intent = new Intent(getApplicationContext(), NotificationServices.class);

			Bundle bitmapBundle = new Bundle();

			Bitmap RedPoint = BitmapFactory.decodeResource(getResources(), R.drawable.red_point);
			Bitmap GreenPoint = BitmapFactory.decodeResource(getResources(), R.drawable.green_point);

			bitmapBundle.putParcelable("Red", RedPoint);
			bitmapBundle.putParcelable("Green", GreenPoint);

			intent.putExtra("bitmap", bitmapBundle);
			startService(intent);

			// Kill Process
			android.os.Process.killProcess(android.os.Process.myPid());
			super.onBackPressed();
		} else {
			backTime = tempTime;
			Toast.makeText(ParentActivity.this, "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.",
					Toast.LENGTH_SHORT).show();

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getTitle().equals("Information")){
			Intent infoIntent = new Intent(ParentActivity.this, InfoActivity.class);
			startActivity(infoIntent);
			Toast toto = Toast.makeText(this, "Information이 선택되었습니다",Toast.LENGTH_SHORT);
			toto.show();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		// 사용자가 설정한 하한 볼륨을 저장
		SharedPreferences sp = getSharedPreferences("pref", MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();

		editor.putInt("MIN_DCB", (int) SaveUserSetting.GetLimitDcb());
		editor.apply();

		super.onDestroy();
	}
}

