package com.gt.seniordesign.dlmns;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;
import android.app.AlarmManager;
import java.util.ArrayList;
import java.util.List;
import java.lang.Thread;

public class DLMMonitorService extends Service {

	private final IBinder mBinder = new LocalBinder();
	private ArrayList<KnownDevice> monitoredDevices;
	private AlarmManager am;
	private ArrayList<BluetoothDevice> connectedDevices;
	private long lastEntry  = 0;
	private BluetoothAdapter bluetoothAdapter;

	private class MyBleCallback extends BluetoothGattCallback  {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			if (newState == 2) {
				// Only do this when we need to...check whether duty cycle changed since last time
				// The timers stay in sync without writing the duty cycle every time
				KnownDevice myDevice = getKnownDevice(gatt.getDevice().hashCode());

				if (true || myDevice.new_duty_cycle != myDevice.getDutyCycle()) { // The duty cycle has changed since the last time
					gatt.discoverServices();
				} else {}
			} // The thing 
		}
		

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic,int status) { 

			KnownDevice myDevice = getKnownDevice(gatt.getDevice().hashCode());
			
			if (status == 0) {
				int val = characteristic.getIntValue(17, 0);
				if (myDevice.tagCount != characteristic.getIntValue(17, 0)) {

					myDevice.setDutyCycle(myDevice.new_duty_cycle); // Handshake complete...assume duty cycle write was succesful
					myDevice.tagCount = characteristic.getIntValue(17, 0);
					// We need to do a timer sync here
					// 1. Cancel the previous alarm that was created (am.cancel(PI) doesn't reliably always work)
					myDevice.ignoreNext = true;
					// 2. Set a new one (the tag should do the same)
					setConnectAlarm(myDevice.getDutyCycle() + 2, gatt.getDevice().hashCode());
					gatt.disconnect();
					gatt.close();
					connection_success = true;
				} else { // We didn't connect
					gatt.disconnect();
					gatt.close();
					myDevice.currentGattConnection = connectDevice(myDevice);
				}
			} else { // We didn't connect
				gatt.disconnect();
				gatt.close();
				myDevice.currentGattConnection = connectDevice(myDevice);
			}

		}
		
	

		@Override
		public void onCharacteristicWrite (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

			KnownDevice myDevice = getKnownDevice(gatt.getDevice().hashCode());

			try {
				BluetoothGattService dlmns_service = gatt.getService(DLMNSGattAttributes.lookup("DLMNS Service"));
				BluetoothGattCharacteristic ack_char = dlmns_service.getCharacteristic(DLMNSGattAttributes.lookup("Acknowledge"));
				if (!gatt.readCharacteristic(ack_char)) {
					gatt.disconnect();
					gatt.close();
					myDevice.currentGattConnection = connectDevice(myDevice);
				}
				// Looks like sometimes the onCharacteristicRead callback isn't always called. We may need to add a handler here
				// and check that the callback already executes, or we won't be able to catch it.
					
			} catch (Exception ex) { // We didn't connect...try again
				gatt.disconnect();
				gatt.close();
				myDevice.currentGattConnection = connectDevice(myDevice);
			}

		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status){

			KnownDevice myDevice = getKnownDevice(gatt.getDevice().hashCode());

			// Get the BLE Service
			try {
				BluetoothGattService dlmns_service = gatt.getService(DLMNSGattAttributes.lookup("DLMNS Service"));
				BluetoothGattCharacteristic duty_cycle_char = dlmns_service.getCharacteristic(DLMNSGattAttributes.lookup("Duty Cycle"));
				// Decode the new duty cycle here
				duty_cycle_char.setValue(decodeDutyCycle(myDevice.new_duty_cycle));
				gatt.writeCharacteristic(duty_cycle_char);
			} catch (Exception ex) { // We really didn't connect
				gatt.disconnect();
				gatt.close();
				myDevice.currentGattConnection = connectDevice(myDevice); // Try to connect again...haha (up to 3 times...this will re-sync the timings too)
				return;
			}
		}
	};

	public class LocalBinder extends Binder {
		DLMMonitorService getService() {
			return DLMMonitorService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	private KnownDevice getKnownDevice(int bleHash) {
		for (KnownDevice dev : monitoredDevices) {
			if (dev.getDeviceContext().hashCode() == bleHash) {
				return dev;
			}
		}
		return null;
	}

	private byte[] decodeDutyCycle(int dutyCycle) {
		switch (dutyCycle) {
		case 32:
			return new byte[] {(byte)0x01};
		case 64:
			return new byte[] {(byte)0x02};
		case 128:
			return new byte[] {(byte)0x03};
		case 256:
			return new byte[] {(byte)0x04};
		default:
			return null;
		}
	}

	boolean connection_success = false;
	BluetoothDevice currentDevice;

	private class connectionCheck implements Runnable {

		private KnownDevice foundDevice;

		public connectionCheck(KnownDevice foundDevice) {
			this.foundDevice = foundDevice;
		}

		public void run() {

			if (!connection_success) {

				try {
					foundDevice.currentGattConnection.disconnect();
					foundDevice.currentGattConnection.close();
				} catch (Exception ex) {}
				
				resetBluetooth();
				
				try {
					Thread.sleep(250);
				} catch (Exception ex) {}
				
				if (foundDevice.connectAttempts-- > 0) {
					Handler scan_handler = new Handler();
					connectionCheck r = new connectionCheck(foundDevice);
					foundDevice.currentGattConnection = connectDevice(foundDevice);
					scan_handler.postDelayed(r, 3000);

				} else {
					Toast.makeText(getApplicationContext(), "You forgot your " + foundDevice.getName() + "!", Toast.LENGTH_SHORT).show();
				}
			}
		} 
	}

	public BroadcastReceiver MyReceiver = new BroadcastReceiver()  {
		@Override
		public void onReceive(Context c, Intent i) {

			lastEntry = SystemClock.elapsedRealtime();

			// Try to get the hashcode of the BluetoothDevice
			int deviceHash = 0;
			try {
				deviceHash = i.getIntExtra("hash_id", 0);
			} catch (Exception ex) {
				return;
			}

			KnownDevice foundDevice = getKnownDevice(deviceHash);
			connection_success = false;

			if (!foundDevice.ignoreNext) {

				// Reset the alarm for the next time
				if (++(foundDevice.connectCount) >= 10) {
					foundDevice.connectCount = 0;
					setConnectAlarm(foundDevice.getDutyCycle() + 1, deviceHash);
				} else {
					foundDevice.connectCount++;
					setConnectAlarm(foundDevice.getDutyCycle(), deviceHash);
				}

				// Clear the connected devices
				connectedDevices.clear();

				// Make the connection
				if (foundDevice != null) {

					Handler scan_handler = new Handler();
					connectionCheck r = new connectionCheck(foundDevice);

					foundDevice.connectAttempts = 2;
					foundDevice.currentGattConnection = connectDevice(foundDevice);
					scan_handler.postDelayed(r, 3000);
				}
				
			} else {
				foundDevice.ignoreNext = false;
			}
		}
	};

	private class resetThread extends Thread {

		public resetThread() {
			super();
			run();
		}

		@Override
		public void run() {
			bluetoothAdapter.disable();
			while (bluetoothAdapter.isEnabled()) {/*spin*/}
			bluetoothAdapter.enable();
			while (!bluetoothAdapter.isEnabled()) {/*spin*/}
		}
	};

	public void resetBluetooth() {

		while ((new resetThread()).isAlive()) {/*spin*/}
		return;

	}

	public BluetoothGatt connectDevice(KnownDevice dev) {
		return dev.currentGattConnection = dev.getDeviceContext().connectGatt(getApplicationContext(), false, new MyBleCallback());
	}


	public boolean initialize() {

		// Setup the Bluetooth Adapter
		final BluetoothManager bluetoothManager =
				(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothAdapter = bluetoothManager.getAdapter();

		registerReceiver(MyReceiver, new IntentFilter("com.gt.seniordesign.connectTimer"));
		monitoredDevices = new ArrayList<KnownDevice>();
		connectedDevices = new ArrayList<BluetoothDevice>();
		return true;
	}

	public void addNewDevice(KnownDevice myDevice) {

		// Add the device to our list
		monitoredDevices.add(myDevice);

		// This is the hashcode for the actual BluetoothDevice instance
		int deviceHash = myDevice.getDeviceContext().hashCode();

		// See if we need to add space between the connections (needs to be verified)
		while (SystemClock.elapsedRealtime() < lastEntry + 1000*15) { /*spin*/ }
		lastEntry = SystemClock.elapsedRealtime();
		BluetoothGatt currentGatt = connectDevice(myDevice); // This creates a new thread and avoids locking up UI (Android Warning)

		setConnectAlarm(myDevice.getDutyCycle() + 2, deviceHash);
		myDevice.connectCount++;

	}

	public ArrayList<KnownDevice> getKnownDevices() {
		return monitoredDevices;
	}


	public void setConnectAlarm(int time_secs, int deviceHash) {
		Intent newIntent = new Intent("com.gt.seniordesign.connectTimer");
		newIntent.putExtra("hash_id", deviceHash);
		int requestCode = (int)System.currentTimeMillis();
		newIntent.putExtra("requestCode", requestCode);
		PendingIntent pi = PendingIntent.getBroadcast(this, requestCode, newIntent,0);
		am = (AlarmManager)(this.getSystemService(Context.ALARM_SERVICE ));
		am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime() + 1000*time_secs, pi);
	}
}
