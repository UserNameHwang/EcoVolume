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

    // AudioRecord ��ü���� ���ļ��� 8kHz, ����� ä���� �ϳ�, ������ 16��Ʈ�� ���
    int frequency = 8000;
    int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;

    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    // �츮�� FFT ��ü�� transformer��, �� FFT ��ü�� ���� AudioRecord ��ü���� �� ���� 256���� ������ �ٷ��. ����ϴ� ������ ���� FFT ��ü�� ����
    // ���õ��� �����ϰ� ������ ���ļ��� ���� ��ġ�Ѵ�. �ٸ� ũ�⸦ ������� �����ص� ������, �޸𸮿� ���� ������ �ݵ�� ����ؾ� �Ѵ�.
    // ����� ������ ����� ���μ����� ���ɰ� ������ ���踦 ���̱� �����̴�.

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

    // RecordAudio�� ���⿡�� ���ǵǴ� ���� Ŭ�����μ� AsyncTask�� Ȯ���Ѵ�.

    private RecordAudio recordTask;
 //   private EcoVolume ecoTask;

    // Bitmap �̹����� ǥ���ϱ� ���� ImageView�� ����Ѵ�. �� �̹����� ���� ����� ��Ʈ������ ���ļ����� ������ ��Ÿ����.

    // �� �������� �׸����� Bitmap���� ������ Canvas ��ü�� Paint��ü�� �ʿ��ϴ�.
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

        // Service ���� ���� �Ǵ�
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
                SaveUserSetting.SetLimitDcb((double) (progress + 75));
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // RealDoubleFFT Ŭ���� ����Ʈ���ʹ� �ѹ��� ó���� ���õ��� ���� �޴´�. �׸��� ��µ� ���ļ� �������� ���� ��Ÿ����.
        transformer = new RealDoubleFFT(blockSize);

        // ImageView �� ���� ��ü ���� �κ�
        imageView = (ImageView)findViewById(R.id.ImageView01);
        bitmap = Bitmap.createBitmap((int)256, (int)100, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        paint.setColor(Color.GREEN);
        imageView.setImageBitmap(bitmap);
        dBValue = (TextView)findViewById(R.id.dbValue);
    }

    // �� ��Ƽ��Ƽ�� �۾����� ��κ� RecordAudio��� Ŭ�������� ����ȴ�. �� Ŭ������ AsyncTask�� Ȯ���Ѵ�.
    // AsyncTask�� ����ϸ� ����� �������̽��� ���ϴ� �ְ� �ϴ� �޼ҵ���� ������ ������� �����Ѵ�.
    // doInBackground �޼ҵ忡 �� �� �ִ� ���̸� ������ �̷� ������ ������ �� �ִ�.
    private class RecordAudio extends AsyncTask<Void, double[], Void>{

        @Override
        protected Void doInBackground(Void... params) {
            try{
                // AudioRecord�� �����ϰ� ����Ѵ�.
                int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);

                AudioRecord audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, audioEncoding, bufferSize);


                // short�� �̷��� �迭�� buffer�� ���� PCM ������ AudioRecord ��ü���� �޴´�.
                // double�� �̷��� �迭�� toTransform�� ���� �����͸� ������ double Ÿ���ε�, FFT Ŭ���������� doubleŸ���� �ʿ��ؼ��̴�.

                short[] buffer = new short[blockSize];
                double[] toTransform = new double[blockSize];
                audioRecord.startRecording();

                while(started){
                    int bufferReadResult = audioRecord.read(buffer, 0, blockSize);

                    // AudioRecord ��ü���� �����͸� ���� �������� short Ÿ���� �������� double Ÿ������ �ٲٴ� ������ ó���Ѵ�.
                    // ���� Ÿ�� ��ȯ(casting)���� �� �۾��� ó���� �� ����. ������ ��ü ������ �ƴ϶� -1.0���� 1.0 ���̶� �׷���
                    // short�� 32,768.0(Short.MAX_VALUE) ���� ������ double�� Ÿ���� �ٲ�µ�, �� ���� short�� �ִ밪�̱� �����̴�.

                    for(int i = 0; i < blockSize && i < bufferReadResult; i++){
                        toTransform[i] = (double)buffer[i] / Short.MAX_VALUE; // ��ȣ �ִ� 16��Ʈ

                        //    Log.d("Read Value", " #i value : " + i +" #Buffer : " + buffer[i] + " #Transform Value : " + toTransform[i]);
                    }

                    // ���� double������ �迭�� FFT ��ü�� �Ѱ��ش�. FFT ��ü�� �� �迭�� �����Ͽ� ��� ���� ��´�. ���Ե� �����ʹ� �ð� �������� �ƴ϶�
                    // ���ļ� �����ο� �����Ѵ�. �� ���� �迭�� ù ��° ��Ұ� �ð������� ù ��° ������ �ƴ϶�� ����. �迭�� ù ��° ��Ҵ� ù ��° ���ļ� ������ ������ ��Ÿ����.

                    // 256���� ��(����)�� ����ϰ� �ְ� ���� ������ 8,000 �̹Ƿ� �迭�� �� ��Ұ� �뷫 15.625Hz�� ����ϰ� �ȴ�. 15.625��� ���ڴ� ���� ������ ������ ������(ĸ���� �� �ִ�
                    // �ִ� ���ļ��� ���� ������ ���̴�.), �ٽ� 256���� ������ ���� ���̴�. ���� �迭�� ù ��° ��ҷ� ��Ÿ�� �����ʹ� ��(0)�� 15.625Hz ���̿�
                    // �ش��ϴ� ����� ������ �ǹ��Ѵ�.

                    transformer.ft(toTransform);

                    // publishProgress�� ȣ���ϸ� onProgressUpdate�� ȣ��ȴ�.
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

        // onProgressUpdate�� �츮 ��Ƽ��Ƽ�� ���� ������� ����ȴ�. ���� �ƹ��� ������ ����Ű�� �ʰ� ����� �������̽��� ��ȣ�ۿ��� �� �ִ�.
        // �̹� ���������� onProgressUpdate�� FFT ��ü�� ���� ����� ���� �����͸� �Ѱ��ش�. �� �޼ҵ�� �ִ� 100�ȼ��� ���̷� �Ϸ��� ���μ�����
        // ȭ�鿡 �����͸� �׸���. �� ���μ��� �迭�� ��� �ϳ����� ��Ÿ���Ƿ� ������ 15.625Hz��. ù ��° ���� ������ 0���� 15.625Hz�� ���ļ��� ��Ÿ����,
        // ������ ���� 3,984.375���� 4,000Hz�� ���ļ��� ��Ÿ����.

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

        // Sample Smartphone ���� �� ��������
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
        // Sample Ear Receiver ���� : 112dB/mW, ���Ǵ��� : 16ohm
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

                    // ���� ���� : W = V * V / R
                    double Watt = (VoltagePerVol[mCurvol] * VoltagePerVol[mCurvol]) / Impedance;
                    double MillWatt = Watt/1000;
                    // ���¿����� dB ���� : dB = 10 * log(���Ǵ����� ����/���� ���� ����)
                    double dB = 10 * Math.log10(OhmofImp / MillWatt);

                    // ���� ��� ���� dB : ������ ���ú� - ���� ������ ���ú�
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

        // sqsum is the sum of all (signal+bias)��, so
        // sqsum = sum(signal��) + samples * bias��
        // hence
        // sum(signal��) = sqsum - samples * bias��
        // Bias is simply the average value, i.e.
        // bias = sum / samples
        // Since power = sum(signal��) / samples, we have
        // power = (sqsum - samples * sum�� / samples��) / samples
        // so
        // power = (sqsum - sum�� / samples) / samples
        double power = (sqsum - sum * sum / samples) / samples;

        // Scale to the range 0 - 1.
        power /= MAX_16_BIT * MAX_16_BIT;

        // Convert to dB, with 0 being max power. Add a fudge factor to make
        // a "real" fully saturated input come to 0 dB.
        double result = Math.log10(power) * 10f + FUDGE;
        return (int)result;
    }

    // serviceName : manifest���� ������ ������ �̸�
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
