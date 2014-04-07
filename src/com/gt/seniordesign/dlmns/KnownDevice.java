package com.gt.seniordesign.dlmns;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.view.WindowManager;

public class KnownDevice {

	private BluetoothDevice thisDevice;
	private String shortName;
	private int duty_cycle = 16;
	public boolean ignoreNext = false;
	public boolean ignoreSecond = true;
	public int connectCount = 0;
	public int tagCount = 0;
	public BluetoothGatt currentGattConnection;
	public int connectAttempts = 0;
	public int new_duty_cycle = 0;
	public static Context application_context;
	private Camera camera;
	
	// State related variables
	public boolean connection_success = false;
	public boolean connectionStateCallBackCalled = false;
	public boolean possible_success = false;
	
	// Alert realated state variables
	private boolean objectOutOfRange = false;
	private boolean alertAcknowledged = false;
	private boolean firstAlertSent = false;
	public boolean notificationsEnabled = true;
	
	public KnownDevice(BluetoothDevice newDevice, String newName, int duty_cycle) {
		thisDevice = newDevice;
		shortName = newName;
		this.new_duty_cycle = duty_cycle;
	}
	
	public String getName() {
		return shortName;
	}
	
	public void setName(String newName) {
		shortName = newName;
	}
	
	public void setDutyCycle(int newDutyCycle) {
		this.duty_cycle = newDutyCycle;
	}
	
	public int getDutyCycle() {
		return this.duty_cycle;
	}
	
	public BluetoothDevice getDeviceContext() {
		return thisDevice;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof KnownDevice) {
			return thisDevice.getAddress().equals(((KnownDevice) o).getDeviceContext().getAddress());
		} else return false;	
	}
	
	@Override
	public int hashCode() {
		return thisDevice.hashCode();
	}
	
	public void notifyOutOfRange() {
		objectOutOfRange = true;
	}
	
	public void notifyInRange() {
		objectOutOfRange = false;
		alertAcknowledged = false;
		firstAlertSent = false;
	}
	
	// Notification Related Methods
	
	public void generateNotification() {

		Thread flashThread = new Thread()
		{
			@Override
			public void run() {
				for (int i = 0; i < 5; i++) {
					turnOnFlash();
					turnOffFlash();
				}
			}
		};

		Thread vibrateThread = new Thread()
		{
			@Override
			public void run() {
				Vibrator v = (Vibrator) application_context.getSystemService(Context.VIBRATOR_SERVICE);
				// Vibrate for 3 seconds
				v.vibrate(1000*2);
			}
		};

		Thread soundThread = new Thread()
		{
			@Override
			public void run() {
				Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				Ringtone r = RingtoneManager.getRingtone(application_context, notification);
				r.play(); // Need to do this longer...find a way
			}
		};

		if (notificationsEnabled && !alertAcknowledged) {
			
			if (!firstAlertSent) {
				generatePopup();
				firstAlertSent = true;
			}
			
			flashThread.start(); 
			vibrateThread.start();
			soundThread.start();
		}	
	}

	public void generatePopup() {

		AlertDialog.Builder builder = new AlertDialog.Builder(application_context);
		builder.setTitle("Don't Leave Me!");
		builder.setIcon(R.drawable.ic_launcher);
		builder.setMessage("You forgot your " + getName() + "! :(");
		builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				if (objectOutOfRange) {
					alertAcknowledged = true;
				}
				dialog.dismiss();
			}
		});
		AlertDialog alert = builder.create();
		alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		alert.show();
	}

	public void turnOnFlash(){
		camera = Camera.open();
		final Parameters p = camera.getParameters();
		//Set the flashmode to off                    
		p.setFlashMode(Parameters.FLASH_MODE_TORCH);
		//Pass the parameter ti camera object
		camera.setParameters(p);
		camera.release();
	}

	public void turnOffFlash(){
		camera  = Camera.open();
		final Parameters p = camera.getParameters();
		//Set the flashmode to off                    
		p.setFlashMode(Parameters.FLASH_MODE_OFF);
		//Pass the parameter ti camera object
		camera.setParameters(p);
		camera.release();
	}

	// End Notification Related Methods
	
} 
