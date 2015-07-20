package com.android.inputsound;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.inputsound.FFT.RealDoubleFFT;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    // AudioReader Variables //
    private static final float MAX_16_BIT = 32768;
    private static final float FUDGE = 0.6f;
    // AudioReader Variables //

    // AudioRecord 객체에서 주파수는 8kHz, 오디오 채널은 하나, 샘플은 16비트를 사용
    int frequency = 8000;
    int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;

    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    // 우리의 FFT 객체는 transformer고, 이 FFT 객체를 통해 AudioRecord 객체에서 한 번에 256가지 샘플을 다룬다. 사용하는 샘플의 수는 FFT 객체를 통해
    // 샘플들을 실행하고 가져올 주파수의 수와 일치한다. 다른 크기를 마음대로 지정해도 되지만, 메모리와 성능 측면을 반드시 고려해야 한다.
    // 적용될 수학적 계산이 프로세서의 성능과 밀접한 관계를 보이기 때문이다.

    private RealDoubleFFT transformer;
    int blockSize = 256;

    private Button startStopButton;
    private Button EcoButton;
    private Button inputDecibelButton;
    private EditText DecibelEdit;
    private SeekBar seekbar;

    private int MIN_DECIBEL=75;
    private boolean started = false;
    private boolean Ecostarted = false;

    // RecordAudio는 여기에서 정의되는 내부 클래스로서 AsyncTask를 확장한다.

    private RecordAudio recordTask;
 //   private EcoVolume ecoTask;

    // Bitmap 이미지를 표시하기 위해 ImageView를 사용한다. 이 이미지는 현재 오디오 스트림에서 주파수들의 레벨을 나타낸다.

    // 이 레벨들을 그리려면 Bitmap에서 구성한 Canvas 객체와 Paint객체가 필요하다.
    private ImageView imageView;
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint paint;
    private TextView dBValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.h_test_main);

        SaveUserSetting.SetLimitDcb((double) 75);
        startStopButton = (Button)findViewById(R.id.StartStopButton);
        startStopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(started){
                    started = false;
                    startStopButton.setText("Visualization Start");
                    recordTask.cancel(true);
                }else{
                    started = true;
                    startStopButton.setText("Visualization Stop");
                    recordTask = new RecordAudio();
                    recordTask.execute();
                }
            }
        });

        EcoButton = (Button)findViewById(R.id.EcoButton);

        // Service 실행 여부 판단
        boolean svcRunning = isServiceRunning("com.android.inputsound.EcoVolumeServices");
        Log.w("svc Check", "" + svcRunning);
        if(svcRunning) {
            Ecostarted = true;
            EcoButton.setText("Eco Volume Stop");
        }
        else {
            Ecostarted = false;
            EcoButton.setText("Eco Volume Start");
        }

        EcoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Ecostarted){
                    Ecostarted = false;
                    EcoButton.setText("Eco Volume Start");
                    stopService(new Intent("com.android.inputsound.service"));
                }else{
                    Ecostarted = true;
                    EcoButton.setText("Eco Volume Stop");
                    Intent intent = new Intent("com.android.inputsound.service");
                    intent.putExtra("minDecibel", MIN_DECIBEL);
                    startService(intent);
                }
            }
        });

        inputDecibelButton = (Button)findViewById(R.id.btn_input_decibel);
        DecibelEdit = (EditText)findViewById(R.id.edit_decibel);

        inputDecibelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String strDecibel = DecibelEdit.getText().toString();

                if(strDecibel.equals("")){
                    Toast.makeText(getApplicationContext(), "Please Type a Minimum Decibel.", Toast.LENGTH_SHORT).show();
                    return;
                }
                MIN_DECIBEL = Integer.parseInt(strDecibel);
                // restart Service
                stopService(new Intent("com.android.inputsound.service"));

                Intent intent = new Intent("com.android.inputsound.service");
                intent.putExtra("minDecibel", MIN_DECIBEL);
                startService(intent);
            }
        });

        seekbar = (SeekBar)findViewById(R.id.LimitTest);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                SaveUserSetting.SetLimitDcb((double)(progress+75));
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // RealDoubleFFT 클래스 컨스트럭터는 한번에 처리할 샘플들의 수를 받는다. 그리고 출력될 주파수 범위들의 수를 나타낸다.
        transformer = new RealDoubleFFT(blockSize);

        // ImageView 및 관련 객체 설정 부분
        imageView = (ImageView)findViewById(R.id.ImageView01);
        bitmap = Bitmap.createBitmap((int)256, (int)100, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        paint.setColor(Color.GREEN);
        imageView.setImageBitmap(bitmap);
        dBValue = (TextView)findViewById(R.id.dbValue);
    }

    // 이 액티비티의 작업들은 대부분 RecordAudio라는 클래스에서 진행된다. 이 클래스는 AsyncTask를 확장한다.
    // AsyncTask를 사용하면 사용자 인터페이스를 멍하니 있게 하는 메소드들을 별도의 스레드로 실행한다.
    // doInBackground 메소드에 둘 수 있는 것이면 뭐든지 이런 식으로 실행할 수 있다.
    private class RecordAudio extends AsyncTask<Void, double[], Void>{

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
                            dBValue.setText("" + result);
                        }
                    });
                }

                audioRecord.stop();
            }catch(Throwable t){
                Log.e("AudioRecord", "Recording Failed");
            }

            return null;

        }

        // onProgressUpdate는 우리 엑티비티의 메인 스레드로 실행된다. 따라서 아무런 문제를 일으키지 않고 사용자 인터페이스와 상호작용할 수 있다.
        // 이번 구현에서는 onProgressUpdate가 FFT 객체를 통해 실행된 다음 데이터를 넘겨준다. 이 메소드는 최대 100픽셀의 높이로 일련의 세로선으로
        // 화면에 데이터를 그린다. 각 세로선은 배열의 요소 하나씩을 나타내므로 범위는 15.625Hz다. 첫 번째 행은 범위가 0에서 15.625Hz인 주파수를 나타내고,
        // 마지막 행은 3,984.375에서 4,000Hz인 주파수를 나타낸다.

        @Override
        protected void onProgressUpdate(double[]... toTransform) {
            canvas.drawColor(Color.BLACK);

            for(int i = 0; i < toTransform[0].length; i++){
                int x = i;
                int downy = (int) (100 - (toTransform[0][i] * 10));
                int upy = 100;

                canvas.drawLine(x, downy, x, upy, paint);
            }
            imageView.invalidate();
        }
    }

    /*private class EcoVolume extends AsyncTask<Void, Double, Void>{

        // Sample Smartphone 볼륨 당 음압전류
        *//*
        0 = 0.00
        1 = 0.70
        2 = 1.79
        3 = 3.15
        4 = 4.56
        5 = 6.63
        6 = 8.18
        7 = 10.40
        8 = 12.98
        9 = 16.63
        10 = 21.03
        11 = 25.98
        12 = 32.83
        13 = 41.25
        14 = 51.85
        15 = 57.92
        *//*
        // Sample Ear Receiver 음압 : 112dB/mW, 임피던스 : 16ohm
        private double[] VoltagePerVol =
                {0.0, 0.7, 1.79, 3.15, 4.56, 6.63, 8.18, 10.4, 12.98, 16.63, 21.03, 25.98, 32.83, 41.25, 51.85, 57.92};
        private int Impedance = 16;
        private double OhmofImp = 1;
        private int Sensitivity = 112;

        private double MIN_DECIBEL = 75;
        @Override
        protected Void doInBackground(Void... params) {
            try{
                AudioManager audiomanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                while(Ecostarted) {
                    int mCurvol = audiomanager.getStreamVolume(audiomanager.STREAM_MUSIC);
                    Log.w("Current Volume", "volume : " + mCurvol);

                    // 전력 계산식 : W = V * V / R
                    double Watt = (VoltagePerVol[mCurvol] * VoltagePerVol[mCurvol]) / Impedance;
                    double MillWatt = Watt/1000;
                    // 전력에서의 dB 계산식 : dB = 10 * log(임피던스의 전력/현재 볼륨 전력)
                    double dB = 10 * Math.log10(OhmofImp / MillWatt);

                    // 실제 출력 볼륨 dB : 감도의 데시벨 - 현재 전력의 데시벨
                    double SPL = Sensitivity - dB;

                    publishProgress(SPL);

                    if(SPL > MIN_DECIBEL)
                        audiomanager.setStreamVolume(audiomanager.STREAM_MUSIC,
                                mCurvol - 1, audiomanager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                    Thread.sleep(2000);
                }
            }catch(Throwable t){
                Log.e("EcoVolume", "EcoVolume Failed");
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Double... SPL) {
            Log.w("Current Decibel", "decibel : " + SPL[0]);
        }
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

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

    // serviceName : manifest에서 설정한 서비스의 이름
    public Boolean isServiceRunning(String serviceName) {
        ActivityManager manager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningServiceInfo> RunningService = manager.getRunningServices(Integer.MAX_VALUE);
        for (int i=0; i< RunningService.size(); i++) {
            ActivityManager.RunningServiceInfo rsi = RunningService.get(i);
            Log.w("run service","Package Name : " + rsi.service.getClassName()+" / pid ="+rsi.pid);
            if( serviceName.equals(rsi.service.getClassName()))
                return true;
        }
        return false;
    }
}
