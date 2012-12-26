package com.wiley.aoa.kitchen_lamp;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.wiley.wroxaccessories.UsbConnection12;
import com.wiley.wroxaccessories.WroxAccessory;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.TelephonyManager;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.NumberPicker;
import android.widget.ViewFlipper;

public class MainActivity extends Activity {

//	private GestureDetector mGestureDetector;

	private ViewFlipper mViewFlipper;

	private Animation next_in, next_out, previous_in, previous_out;

	private NumberPicker minutes, seconds;

	private CountDownTimer timer;

	private boolean counting;

	private CheckBox chk_sms, chk_phone, chk_weather;

	private IntentFilter smsFilter, phoneFilter, weatherFilter;

	private boolean smsRegistered, phoneRegistered, weatherRegistered;

	/** The Wrox Accessory class, handles communication for us */
	private WroxAccessory mAccessory;

	/**
	 * The USB Manager, change this to com.android.hardware.UsbManager if you
	 * want the SDK 12 version of the Accessory
	 */
	private UsbManager mUsbManager;

	/**
	 * The Connection object, need to change this too if you want to use another
	 * type of accessory.
	 */
	private UsbConnection12 connection;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

//		mGestureDetector = new GestureDetector(this, mGestureListener);

		mViewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper1);

		next_in = AnimationUtils.loadAnimation(this, R.anim.transition_next_in);
		next_out = AnimationUtils.loadAnimation(this, R.anim.transition_next_out);
		previous_in = AnimationUtils.loadAnimation(this, R.anim.transition_previous_in);
		previous_out = AnimationUtils.loadAnimation(this, R.anim.transition_previous_out);

		minutes = (NumberPicker) findViewById(R.id.picker_minutes);
		minutes.setMaxValue(60);
		seconds = (NumberPicker) findViewById(R.id.picker_seconds);
		seconds.setMaxValue(60);

		chk_phone = (CheckBox) findViewById(R.id.check_phone);
		chk_phone.setOnCheckedChangeListener(checkboxListener);
		chk_sms = (CheckBox) findViewById(R.id.check_sms);
		chk_sms.setOnCheckedChangeListener(checkboxListener);
		chk_weather = (CheckBox) findViewById(R.id.check_weather);
		chk_weather.setOnCheckedChangeListener(checkboxListener);

		// 1. Get a reference to the UsbManager (there's only one, so you don't
		// instantiate it)
		mUsbManager = (UsbManager) getSystemService(USB_SERVICE);

		// 2. Create the Connection object
		connection = new UsbConnection12(this, mUsbManager);

		// 3. Instantiate the WroxAccessory
		mAccessory = new WroxAccessory(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		try {
			mAccessory.connect(WroxAccessory.USB_ACCESSORY_12, connection);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (phoneRegistered)
			registerPhone(false);

		if (smsRegistered)
			registerSms(false);

		if (weatherRegistered)
			registerWeather(false);
		
		try {
			mAccessory.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public void setAlarm(View v) {
		if (!counting) {
			long millis = TimeUnit.MINUTES.toMillis(minutes.getValue()) + TimeUnit.SECONDS.toMillis(seconds.getValue());

			timer = new CountDownTimer(millis, Constants.TIMER_COUNTDOWN) {
				@Override
				public void onTick(long millisUntilFinished) {
					updateTime(millisUntilFinished);
				}

				@Override
				public void onFinish() {
					updateTime(0);
					counting = false;
				}
			}.start();

			counting = true;
		} else {
			timer.cancel();
			counting = false;
		}
	}

	private void updateTime(long millis) {
		minutes.setValue((int) TimeUnit.MILLISECONDS.toMinutes(millis));
		seconds.setValue((int) TimeUnit.MILLISECONDS.toSeconds(millis));
	}

//	@Override
//	public boolean onTouchEvent(MotionEvent event) {
//		return mGestureDetector.onTouchEvent(event);
//	}

//	private SimpleOnGestureListener mGestureListener = new SimpleOnGestureListener() {
//
//		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//			float x_len = Math.abs(e2.getX() - e1.getX());
//
//			if (x_len > Constants.MIN_SWIPE_LENGTH) {
//				if (e1.getX() < e2.getX()) {
//					mViewFlipper.setInAnimation(next_in);
//					mViewFlipper.setOutAnimation(next_out);
//					mViewFlipper.showNext();
//				} else {
//					mViewFlipper.setInAnimation(previous_in);
//					mViewFlipper.setOutAnimation(previous_out);
//					mViewFlipper.showPrevious();
//				}
//			}
//			return true;
//		};
//	};

	private OnCheckedChangeListener checkboxListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (buttonView.getId() == R.id.check_phone) {
				registerPhone((phoneRegistered = isChecked));
			} else if (buttonView.getId() == R.id.check_sms) {
				registerSms((smsRegistered = isChecked));
			} else if (buttonView.getId() == R.id.check_weather) {
				registerWeather((weatherRegistered = isChecked));
			}
		}
	};

	private void registerPhone(boolean register) {
		if (phoneFilter == null) {
			phoneFilter = new IntentFilter();
			phoneFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
		}
		if (register)
			registerReceiver(phoneReciever, phoneFilter);
		else
			unregisterReceiver(phoneReciever);
	}

	private void registerSms(boolean register) {
		if (smsFilter == null) {
			smsFilter = new IntentFilter();
			smsFilter.addAction(Constants.SMS_RECIEVED);
		}
		if (register)
			registerReceiver(smsReciever, smsFilter);
		else
			unregisterReceiver(smsReciever);
	}

	private void registerWeather(boolean register) {
		if (weatherFilter == null) {
			weatherFilter = new IntentFilter();
			weatherFilter.addAction(WeatherHandler.WEATHER_EVENT);
		}
		if (register)
			registerReceiver(weatherReciever, weatherFilter);
		else
			unregisterReceiver(weatherReciever);
	}

	private BroadcastReceiver phoneReciever = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String state = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
			if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
				byte[] message = new byte[1];
				message[0] = Constants.PHONE_EVENT;
				try {
					mAccessory.publish("kl", message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};

	private BroadcastReceiver smsReciever = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			byte[] message = new byte[1];
			message[0] = Constants.SMS_EVENT;
			try {
				mAccessory.publish("kl", message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};

	private BroadcastReceiver weatherReciever = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			byte[] message = new byte[2];
			message[0] = Constants.WEATHER_EVENT;
			message[1] = intent.getByteExtra("code", (byte) 0);
			try {
				mAccessory.publish("kl", message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};
}
