package com.android.inputsound;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.view.LineGraphSetting;
import com.android.view.SlidingTabLayout;
import com.gc.materialdesign.views.ButtonRectangle;
import com.gc.materialdesign.views.CheckBox;
import com.gc.materialdesign.views.Slider;
import com.handstudio.android.hzgrapherlib.graphview.LineGraphView;
import com.handstudio.android.hzgrapherlib.vo.linegraph.LineGraphVO;

import java.util.List;


public class SlidingTabsBasicFragment extends Fragment {

	private SlidingTabLayout mSlidingTabLayout;

	private ViewPager mViewPager;
	private View homeView, logView, visualView, settingView;

	private RecordAudio AudioDCBTask;
	int blockSize = 256;

	private TextView indBValue;
	private TextView outdBValue;

	private TextView inputAmnt;
	private TextView listenAmnt;

	private LinearLayout lineGraph;

	private Handler infohandler;

	private int inDCB, outDCB;
	private int FirstCheck=0, SecondCheck=0, ThirdCheck=0, LastCheck=0;

	//visualizer
	private LinearLayout mInputLayout,mAnalyLayout,mOutputLayout;
	private VisualizerView mInputView,mAnalyView,mOutputView;
	private static final float VISUALIZER_HEIGHT_DIP = 130f;
	private short[] inBuffer,analyBuffer,outBuffer;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		infohandler = new Handler() {
			public void handleMessage(Message msg){
				infohandler.sendEmptyMessageDelayed(0, 15000); // 15초에 한 번 refresh
				SaveDCB.setInDCB(inDCB);
				SaveDCB.setOutDCB(outDCB);

				refreshGraph();
			}
		};

		return inflater.inflate(R.layout.fragment_sample, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		mViewPager = (ViewPager) view.findViewById(R.id.viewpager);

		mViewPager.setAdapter(new SamplePagerAdapter());

		mSlidingTabLayout = (SlidingTabLayout) view
				.findViewById(R.id.sliding_tabs);
		mSlidingTabLayout.setViewPager(mViewPager);
	}

	// PageAdapter
	class SamplePagerAdapter extends PagerAdapter {

		// Tab의 갯수를 지정한다.
		@Override
		public int getCount() {
			return 4;
		}

		@Override

		public boolean isViewFromObject(View view, Object o) {
			return o == view;
		}

		@Override
		public int getItemPosition(Object object) {
			// TODO Auto-generated method stub
			return super.getItemPosition(object);
		}

		// TabTitle 관리.
		@Override
		public CharSequence getPageTitle(int position) {

			if (position == 0) {
				return "Home";
			} else if (position == 1) {
				return "Log";
			} else if (position == 2) {
				return "Infomation";
			} else {
				return "Setting";
			}
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {

			if (position == 0) {
				if(FirstCheck == 0 ){
					homeView = getActivity().getLayoutInflater().inflate(
							R.layout.first_main, container, false);

					// Material Forms
					ButtonRectangle EcoButton = (ButtonRectangle)homeView.findViewById(R.id.ecoButton);
					ButtonRectangle NoiseButton = (ButtonRectangle)homeView.findViewById(R.id.noiseButton);

					com.gc.materialdesign.views.Switch ecoSwitch = (com.gc.materialdesign.views.Switch)homeView.findViewById(R.id.ecoSwitch);
					com.gc.materialdesign.views.Switch noiseSwitch = (com.gc.materialdesign.views.Switch)homeView.findViewById(R.id.noiseSwitch);

					indBValue = (TextView)homeView.findViewById(R.id.inputDB);
					outdBValue = (TextView)homeView.findViewById(R.id.outputDB);

					NoiseButton.setTextColor(Color.parseColor("#000000"));
					EcoButton.setTextColor(Color.parseColor("#000000"));

					// 터치 이벤트 disable
					// true 를 리턴하면 disable
					ecoSwitch.setOnTouchListener(new View.OnTouchListener() {
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							return true;
						}
					});

					noiseSwitch.setOnTouchListener(new View.OnTouchListener() {
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							return true;
						}
					});

					// Service 실행 여부 판단
					boolean EcosvcRunning = isServiceRunning("com.android.inputsound.EcoVolumeServices");
					Log.w("svc Check", "" + EcosvcRunning);
					if(EcosvcRunning) {
						ecoSwitch.setChecked(true);
						EcoButton.setText("에코볼륨\n중단하기");
						SaveUserSetting.setEcoVolumeStarted(true);
					}
					else {
						EcoButton.setText("에코볼륨\n시작하기");
						SaveUserSetting.setEcoVolumeStarted(false);
					}

					boolean NCsvcRunning = isServiceRunning("com.android.inputsound.NoiseCancelingServices");
					Log.w("svc Check", "" + NCsvcRunning);
					if(NCsvcRunning) {
						noiseSwitch.setChecked(true);
						NoiseButton.setText("노이즈캔슬링\n중단하기");
						SaveUserSetting.setNoiseCancelStarted(true);
					}
					else {
						NoiseButton.setText("노이즈캔슬링\n시작하기");
						SaveUserSetting.setNoiseCancelStarted(false);
					}
					container.addView(homeView);

					AudioDCBTask = new RecordAudio();
					AudioDCBTask.start();

					FirstCheck++;
					return homeView;
				}else{
					container.addView(homeView);
					return homeView;
				}
			}

			else if (position == 1) {
				if(SecondCheck == 0) {
					logView = getActivity().getLayoutInflater().inflate(
							R.layout.second_log, container, false);
					container.addView(logView);

					lineGraph = (LinearLayout) logView.findViewById(R.id.GraphView1);
					inputAmnt = (TextView) logView.findViewById(R.id.inputAmnt);
					listenAmnt = (TextView) logView.findViewById(R.id.listenAmnt);

					LineGraphSetting LineSetting = new LineGraphSetting();
					LineGraphVO LineVo = LineSetting.makeLineGraphAllSetting();

					lineGraph.addView(new LineGraphView(getActivity(), LineVo));

					infohandler.sendEmptyMessage(0);

					SecondCheck++;
					return logView;
				}
				else{
					container.addView(logView);
					return logView;
				}
			}

			else if (position == 2) {
				if(ThirdCheck == 0) {
					visualView = getActivity().getLayoutInflater().inflate(
							R.layout.third_visual, container, false);
					container.addView(visualView);

					mInputLayout = (LinearLayout)visualView.findViewById(R.id.InputLine);
					mAnalyLayout = (LinearLayout) visualView.findViewById(R.id.AnalyLine);
					mOutputLayout = (LinearLayout) visualView.findViewById(R.id.OutputLine);



//					mInputView = new VisualizerView(getActivity());
//					mAnalyView = new VisualizerView(getActivity());
//					mOutputView = new VisualizerView(getActivity());

					mInputView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
							(int) (VISUALIZER_HEIGHT_DIP * getResources().getDisplayMetrics().density)));
					mAnalyView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
							(int) (VISUALIZER_HEIGHT_DIP * getResources().getDisplayMetrics().density)));
					mOutputView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
							(int) (VISUALIZER_HEIGHT_DIP * getResources().getDisplayMetrics().density)));

					mInputLayout.addView(mInputView);
					mAnalyLayout.addView(mAnalyView);
					mOutputLayout.addView(mOutputView);


					/*final Slider slider = (Slider)infoView.findViewById(R.id.testSlider);

					slider.setOnValueChangedListener(new Slider.OnValueChangedListener() {
						@Override
						public void onValueChanged(int i) {

						}
					});

					slider.setOnTouchListener(new View.OnTouchListener() {
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							if(event.getAction() == MotionEvent.ACTION_UP)
								mViewPager.requestDisallowInterceptTouchEvent(false);
							else
								mViewPager.requestDisallowInterceptTouchEvent(true);
							return false;
						}
					});*/

					ThirdCheck++;
					return visualView;
				}
				else{
					container.addView(visualView);
					return visualView;
				}

			} else {
				if(LastCheck == 0) {
					settingView = getActivity().getLayoutInflater().inflate(
							R.layout.fourth_setting, container, false);

					TextView settingDCB = (TextView) settingView.findViewById(R.id.DCBtext);

					final Slider seekbar;
					seekbar = (Slider) settingView.findViewById(R.id.lowLimit);

					seekbar.setValue((int) SaveUserSetting.GetLimitDcb());
					seekbar.setShowNumberIndicator(true);

					settingDCB.setText((int) SaveUserSetting.GetLimitDcb() + "dB");

					seekbar.setOnValueChangedListener(new Slider.OnValueChangedListener() {
						@Override
						public void onValueChanged(int i) {
							SaveUserSetting.SetLimitDcb((double) (seekbar.getValue()));

							SharedPreferences sp =
									getActivity().getApplicationContext().getSharedPreferences("pref", getActivity().getApplicationContext().MODE_PRIVATE);
							SharedPreferences.Editor editor = sp.edit();

							editor.putInt("MIN_DCB", i);
							editor.apply();

							TextView settingDCB = (TextView) settingView.findViewById(R.id.DCBtext);

							settingDCB.setText(seekbar.getValue() + "dB");
						}
					});

					seekbar.setOnTouchListener(new View.OnTouchListener() {
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							if (event.getAction() == MotionEvent.ACTION_UP)
								mViewPager.requestDisallowInterceptTouchEvent(false);
							else
								mViewPager.requestDisallowInterceptTouchEvent(true);
							return false;
						}
					});

					final com.gc.materialdesign.views.CheckBox upperAlert, highVolumeAlert, listenTimeAlert;
					upperAlert = (com.gc.materialdesign.views.CheckBox)settingView.findViewById(R.id.upperAlert);
					highVolumeAlert = (com.gc.materialdesign.views.CheckBox)settingView.findViewById(R.id.highNoiseAlert);
					listenTimeAlert = (com.gc.materialdesign.views.CheckBox)settingView.findViewById(R.id.listenTimeAlert);

					final SharedPreferences sp =
							getActivity().getApplicationContext().getSharedPreferences("pref", getActivity().getApplicationContext().MODE_PRIVATE);

					highVolumeAlert.setOncheckListener(new CheckBox.OnCheckListener() {
						@Override
						public void onCheck(boolean b) {

							SharedPreferences.Editor editor = sp.edit();

							editor.putBoolean("VolumeAlert", b);
							editor.apply();

						}
					});

					listenTimeAlert.setOncheckListener(new CheckBox.OnCheckListener() {
						@Override
						public void onCheck(boolean b) {
							SaveUserSetting.setTimeAlertStarted(b);
						}
					});

					//////////////////////////////////////////////////////////////////////////////////////
					upperAlert.setOncheckListener(new CheckBox.OnCheckListener() {
						@Override
						public void onCheck(boolean b) {
							if(b == false){
								upperAlert.setChecked(false);
								highVolumeAlert.setChecked(false);
								listenTimeAlert.setChecked(false);

								highVolumeAlert.setOnTouchListener(new View.OnTouchListener() {
									@Override
									public boolean onTouch(View v, MotionEvent event) {
										SharedPreferences.Editor editor = sp.edit();

										editor.putBoolean("VolumeAlert", false);
										editor.apply();
										return true;
									}
								});

								listenTimeAlert.setOnTouchListener(new View.OnTouchListener() {
									@Override
									public boolean onTouch(View v, MotionEvent event) {
										SaveUserSetting.setTimeAlertStarted(false);
										return true;
									}
								});
							}
							else{
								upperAlert.setChecked(true);

								highVolumeAlert.setOnTouchListener(new View.OnTouchListener() {
									@Override
									public boolean onTouch(View v, MotionEvent event) {
										return false;
									}
								});

								listenTimeAlert.setOnTouchListener(new View.OnTouchListener() {
									@Override
									public boolean onTouch(View v, MotionEvent event) {
										return false;
									}
								});
							}
						}
					});

					boolean VolumeAlert = sp.getBoolean("VolumeAlert", false);

					if(VolumeAlert)
						highVolumeAlert.setChecked(true);
					else
						highVolumeAlert.setChecked(false);

					container.addView(settingView);
					LastCheck++;
					return settingView;
				}
				else{
					container.addView(settingView);
					return settingView;
				}
			}
			// return v;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);

		}

		@Override
		public void finishUpdate(ViewGroup container) {
			// TODO Auto-generated method stub
			super.finishUpdate(container);

		}

		// serviceName : manifest에서 설정한 서비스의 이름
		public Boolean isServiceRunning(String serviceName) {
			Context c = getActivity();
			ActivityManager manager = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);

			List<ActivityManager.RunningServiceInfo> RunningService = manager.getRunningServices(Integer.MAX_VALUE);
			for (int i=0; i< RunningService.size(); i++) {
				ActivityManager.RunningServiceInfo rsi = RunningService.get(i);
				if( serviceName.equals(rsi.service.getClassName()))
					return true;
			}
			return false;
		}


	}

	// AudioRecord 객체에서 주파수는 8kHz, 오디오 채널은 하나, 샘플은 16비트를 사용
	int frequency = 8000;
	int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;

	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

	private class RecordAudio extends Thread {

		@Override
		public void run(){
			// Sample Smartphone 볼륨 당 음압전류
			/* Volume = mA
			0 = 0.00			1 = 0.70
			2 = 1.79			3 = 3.15
			4 = 4.56			5 = 6.63
			6 = 8.18			7 = 10.40
			8 = 12.98			9 = 16.63
			10 = 21.03			11 = 25.98
			12 = 32.83			13 = 41.25
			14 = 51.85			15 = 57.92
			*/
			// Sample Ear Receiver 음압 : 112dB/mW, 임피던스 : 16ohm
			double[] VoltagePerVol =
					{0.0, 0.7, 1.79, 3.15, 4.56, 6.63, 8.18, 10.4, 12.98, 16.63, 21.03, 25.98, 32.83, 41.25, 51.85, 57.92};
			int Impedance = 16;
			double OhmofImp = 1;
			int Sensitivity = 112;

			// AudioRecord를 설정하고 사용한다.
			int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);

			AudioRecord audioRecord = new AudioRecord(
					MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, audioEncoding, bufferSize);

			short[] buffer = new short[blockSize];
			//audioRecord.startRecording();

			//////////////////////////////////
			SaveDCB.setAudioRecord(audioRecord);
			//////////////////////////////////

			//////////////////////////////////////// visualizer /////////////////////////////////////////
			int isFirst=0;
			mInputView = new VisualizerView(getActivity());
			mAnalyView = new VisualizerView(getActivity());
			mOutputView = new VisualizerView(getActivity());

			inBuffer = new short[blockSize];
			analyBuffer = new short[blockSize];
			outBuffer = new short[blockSize];


			while(true) {
				//////////////////////////////////// Input dB Calculate ////////////////////////////////////

				// short로 이뤄진 배열인 buffer는 원시 PCM 샘플을 AudioRecord 객체에서 받는다.
				// double로 이뤄진 배열인 toTransform은 같은 데이터를 담지만 double 타입인데, FFT 클래스에서는 double타입이 필요해서이다.

				//short[] buffer = new short[blockSize];
				audioRecord.startRecording();
				audioRecord.read(buffer, 0, blockSize);

				if(isFirst==0){
					for(int i=0;i<blockSize;i++){
						inBuffer[i]=buffer[i];
						analyBuffer[i] = (short)(buffer[i]/5);
						outBuffer[i] = (short)(-1*buffer[i]/5);
					}
					isFirst++;
				}
				else{
					for(int i=0;i<blockSize;i++){
						if(buffer[i]!=0){
							inBuffer[i]=buffer[i];
							analyBuffer[i] = (short)(buffer[i]/5);
							outBuffer[i] = (short)(-1*buffer[i]/5);
						}
					}
				}


				final int result = calculatePowerDb(buffer, 0, blockSize) + 90;

				Log.w("Now Decibel", "input decibel : " + result);
				inDCB = result;


				///////////////////////////////////// visualizer /////////////////////////////////////////

				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mInputView.updateVisualizer(inBuffer);
						mAnalyView.updateVisualizer(analyBuffer);
						mOutputView.updateVisualizer(outBuffer);
					}
				});


				//////////////////////////////////// Output dB Calculate ////////////////////////////////////

				AudioManager audiomanager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);

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
				final double SPL = Sensitivity - dB;
				SaveDCB.setSPL(SPL);

				//	Log.w("Now Decibel", "output decibel : " + (int) SPL);
				outDCB = (int) SPL;

				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						indBValue.setText(result + " dB");
						outdBValue.setText((int) SPL + " dB");
					}
				});

				audioRecord.stop();

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		private static final float MAX_16_BIT = 32768;
		private static final float FUDGE = 0.6f;

		private int calculatePowerDb(short[] sdata, int off, int samples){
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
	}

	private void refreshGraph(){
		int[] invalue = SaveDCB.getInDCB();
		int[] outvalue = SaveDCB.getOutDCB();
		int valueCount=0, InputSum=0, OutputSum=0;
		int InputAvg, OutputAvg;
		for(int i=0; i<5; i++)
			if(invalue[i] != 0)
				valueCount++;

		for(int j=0; j<5; j++){
			InputSum += invalue[j];
			OutputSum += outvalue[j];
		}

		if(valueCount == 0){
			InputAvg = 0;
			OutputAvg = 0;
		}
		else{
			InputAvg = InputSum/valueCount;
			OutputAvg = OutputSum/valueCount;
		}

		inputAmnt.setText(InputAvg+" dB");
		listenAmnt.setText(OutputAvg + " dB");

		lineGraph.removeAllViews();
		LineGraphSetting LineSetting = new LineGraphSetting();
		LineGraphVO LineVo = LineSetting.makeLineGraphAllSetting();

		lineGraph.addView(new LineGraphView(getActivity(), LineVo));
	}
}
