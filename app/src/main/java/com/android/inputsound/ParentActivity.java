package com.android.inputsound;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.inputsound.FFT.RealDoubleFFT;
import com.gc.materialdesign.views.ButtonFlat;
import com.gc.materialdesign.views.ButtonRectangle;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.List;

public class ParentActivity extends AppCompatActivity {

	private SlidingTabsBasicFragment fragment;

	private NotificationManager nm;

	private final long INTERVAL = 2000;
	private long backTime = 0;

	private final static int ID_ACTIVITY = 1;
	private final static int SCH_ACTIVITY = 2;

	private int MIN_DECIBEL=75;

	private boolean Ecostarted = false;

	private RecordAudio recordTask;
	private boolean started = false;

	//	private Components
	private SeekBar seekbar;
	private TextView indBValue;

	int blockSize = 256;

	public static RealDoubleFFT getTransformer() {
		return transformer;
	}

	private static RealDoubleFFT transformer;


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

		indBValue = (TextView)findViewById(R.id.inputDB);

		// Service 실행 여부 판단
		boolean svcRunning = isServiceRunning("com.android.inputsound.Services");
		Log.w("svc Check", "" + svcRunning);
		if(svcRunning) {
			Ecostarted = true;
		}
		else {
			Ecostarted = false;
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

	// AudioRecord 객체에서 주파수는 8kHz, 오디오 채널은 하나, 샘플은 16비트를 사용
	int frequency = 8000;
	int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;

	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

	// 우리의 FFT 객체는 transformer고, 이 FFT 객체를 통해 AudioRecord 객체에서 한 번에 256가지 샘플을 다룬다. 사용하는 샘플의 수는 FFT 객체를 통해
	// 샘플들을 실행하고 가져올 주파수의 수와 일치한다. 다른 크기를 마음대로 지정해도 되지만, 메모리와 성능 측면을 반드시 고려해야 한다.
	// 적용될 수학적 계산이 프로세서의 성능과 밀접한 관계를 보이기 때문이다.

	// 이 액티비티의 작업들은 대부분 RecordAudio라는 클래스에서 진행된다. 이 클래스는 AsyncTask를 확장한다.
	// AsyncTask를 사용하면 사용자 인터페이스를 멍하니 있게 하는 메소드들을 별도의 스레드로 실행한다.
	// doInBackground 메소드에 둘 수 있는 것이면 뭐든지 이런 식으로 실행할 수 있다.
	private class RecordAudio extends AsyncTask<Void, double[], Void> {

		@Override
		protected Void doInBackground(Void... params) {
			try{
				// AudioRecord를 설정하고 사용한다.
				int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);

				AudioRecord audioRecord = new AudioRecord(
						MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, audioEncoding, bufferSize);


				// short로 이뤄진 배열인 buffer는 원시 PCM 샘플을 AudioRecord 객체에서 받는다.
				// double로 이뤄진 배열인 toTransform은 같은 데이터를 담지만 double 타입인데, FFT 클래스에서는 double타입이 필요해서이다.

				short[] buffer = new short[blockSize];
				double[] toTransform = new double[blockSize];
				audioRecord.startRecording();

				while(started){
					int bufferReadResult = audioRecord.read(buffer, 0, blockSize);

					// AudioRecord 객체에서 데이터를 읽은 다음에는 short 타입의 변수들을 double 타입으로 바꾸는 루프를 처리한다.
					// 직접 타입 변환(casting)으로 이 작업을 처리할 수 없다. 값들이 전체 범위가 아니라 -1.0에서 1.0 사이라서 그렇다
					// short를 32,768.0(Short.MAX_VALUE) 으로 나누면 double로 타입이 바뀌는데, 이 값이 short의 최대값이기 때문이다.

					for(int i = 0; i < blockSize && i < bufferReadResult; i++){
						toTransform[i] = (double)buffer[i] / Short.MAX_VALUE; // 부호 있는 16비트

						//    Log.d("Read Value", " #i value : " + i +" #Buffer : " + buffer[i] + " #Transform Value : " + toTransform[i]);
					}

					// 이제 double값들의 배열을 FFT 객체로 넘겨준다. FFT 객체는 이 배열을 재사용하여 출력 값을 담는다. 포함된 데이터는 시간 도메인이 아니라
					// 주파수 도메인에 존재한다. 이 말은 배열의 첫 번째 요소가 시간상으로 첫 번째 샘플이 아니라는 얘기다. 배열의 첫 번째 요소는 첫 번째 주파수 집합의 레벨을 나타낸다.

					// 256가지 값(범위)을 사용하고 있고 샘플 비율이 8,000 이므로 배열의 각 요소가 대략 15.625Hz를 담당하게 된다. 15.625라는 숫자는 샘플 비율을 반으로 나누고(캡쳐할 수 있는
					// 최대 주파수는 샘플 비율의 반이다.), 다시 256으로 나누어 나온 것이다. 따라서 배열의 첫 번째 요소로 나타난 데이터는 영(0)과 15.625Hz 사이에
					// 해당하는 오디오 레벨을 의미한다.

					transformer.ft(toTransform);

					// publishProgress를 호출하면 onProgressUpdate가 호출된다.
					publishProgress(toTransform);

					final int result = calculatePowerDb(buffer, 0, blockSize)+90;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							indBValue.setText("" + result);
						}
					});

					Thread.sleep(1000);
				}

				audioRecord.stop();
			}catch(Throwable t){
				Log.e("AudioRecord", "Recording Failed");
			}

			return null;

		}
	}

	private static final float MAX_16_BIT = 32768;
	private static final float FUDGE = 0.6f;

	private int calculatePowerDb(short[] sdata, int off, int samples)
	{
		double sum = 0;
		double sqsum = 0;
		for (int i = 0; i < samples; i++)
		{
			final long v = sdata[off + i];
			sum += v;
			sqsum += v * v;
		}

		// sqsum is the sum of all (signal+bias)², so
		// sqsum = sum(signal²) + samples * bias²
		// hence
		// sum(signal²) = sqsum - samples * bias²
		// Bias is simply the average value, i.e.
		// bias = sum / samples
		// Since power = sum(signal²) / samples, we have
		// power = (sqsum - samples * sum² / samples²) / samples
		// so
		// power = (sqsum - sum² / samples) / samples
		double power = (sqsum - sum * sum / samples) / samples;

		// Scale to the range 0 - 1.
		power /= MAX_16_BIT * MAX_16_BIT;

		// Convert to dB, with 0 being max power. Add a fudge factor to make
		// a "real" fully saturated input come to 0 dB.
		double result = Math.log10(power) * 10f + FUDGE;
		return (int)result;
	}


	// EcoVolume onClick Function
	public void onEcoVolume(View v){

		ButtonRectangle EcoButton = (ButtonRectangle) v;
		com.gc.materialdesign.views.Switch ecoSwitch = (com.gc.materialdesign.views.Switch)findViewById(R.id.ecoSwitch);

		if(Ecostarted){
			Ecostarted = false;
			ecoSwitch.setChecked(false);
			EcoButton.setText("에코볼륨\n시작하기");
			stopService(new Intent(getApplicationContext(), Services.class));
		}else{
			Ecostarted = true;
			ecoSwitch.setChecked(true);
			EcoButton.setText("에코볼륨\n중단하기");
			startService(new Intent(getApplicationContext(), Services.class));
		}

		return;
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
			editor.commit();

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
	protected void onDestroy() {
		// 사용자가 설정한 하한 볼륨을 저장
		SharedPreferences sp = getSharedPreferences("pref", MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();

		editor.putInt("MIN_DCB", (int) SaveUserSetting.GetLimitDcb());
		editor.commit();

		super.onDestroy();
	}
}

