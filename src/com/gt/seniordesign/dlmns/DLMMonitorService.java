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

				KnownDevice myDevice = getKnownDevice(gatt.getDevice().hashCode());
				connection_success = true;
				
				// Only do this when we need to...check whether duty cycle changed since last time
				// The timers stay in sync without writing the duty cycle every time
				gatt.discoverServices();

				// We need to do a timer sync here
				// 1. Cancel the previous alarm that was created (am.cancel(PI) doesn't reliably always work)
				myDevice.ignoreNext = true;
				// 2. Set a new one (the tag should do the same)
				setConnectAlarm(myDevice.getDutyCycle() + 2, gatt.getDevice().hashCode());

			} else {
				connection_success = true;
			}
		}
		
		@Override
		public void onCharacteristicWrite (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if (status == 0) {
				gatt.disconnect();
				gatt.close();
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status){

			KnownDevice myDevice = getKnownDevice(gatt.getDevice().hashCode());

			// Get the BLE Service
			BluetoothGattService dlmns_service = gatt.getService(DLMNSGattAttributes.lookup("DLMNS Service"));
			BluetoothGattCharacteristic duty_cycle_char = dlmns_service.getCharacteristic(DLMNSGattAttributes.lookup("Duty Cycle"));

			duty_cycle_char.setValue(new byte[] {(byte)0x01});
			gatt.writeCharacteristic(duty_cycle_char);
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

	boolean connection_success = false;
	BluetoothDevice currentDevice;

	private class connectionCheck implements Runnable {

		private KnownDevice foundDevice;

		public connectionCheck(KnownDevice foundDevice) {
			this.foundDevice = foundDevice;
		}

		public void run() {
			// see if the device actually got connected
			// if not we need to stop the connection (it's bad i know)
			if (!connection_success) {

				//resetBluetooth();
				foundDevice.currentGattConnection.disconnect();
				Toast.makeText(getApplicationContext(), "Lost a Tag!!", Toast.LENGTH_SHORT).show();

			}
		} 
	}

	public BroadcastReceiver MyReceiver = new BroadcastReceiver()  {
		@Override
		public void onReceive(Context c, Intent i) {

			lastEntry = SystemClock.elapsedRealtime();

			// Get the hashcode of the BluetoothDevice
			int deviceHash = i.getIntExtra("hash_id", 0);
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

					foundDevice.currentGattConnection = foundDevice.getDeviceContext().connectGatt(getApplicationContext(), false, new MyBleCallback());
					scan_handler.postDelayed(r, 3000);

				}
			} else {
				foundDevice.ignoreNext = false;
			}



		}
	};

	public void resetBluetooth() {
		bluetoothAdapter.disable();
		try {
			Thread.sleep(500);
		} catch (Exception ex) {}
		bluetoothAdapter.enable();
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
		BluetoothGatt currentGatt = myDevice.getDeviceContext().connectGatt(getApplicationContext(), false, new MyBleCallback());

		// Now, start the alarm (here or on disconnect??)...Probably on Disconnect

		try {
			Thread.sleep(2000);
		} catch (Exception ex) {}

		//resetBluetooth();
		//currentGatt.disconnect();


		try {
			Thread.sleep(1500);
		} catch (Exception ex) {}

		setConnectAlarm(myDevice.getDutyCycle(), deviceHash);
		myDevice.connectCount++;

	}

	public ArrayList<KnownDevice> getKnownDevices() {
		return monitoredDevices;
	}

	public void setConnectAlarm(int time_secs, int deviceHash) {
		Intent newIntent = new Intent("com.gt.seniordesign.connectTimer");
		newIntent.putExtra("hash_id", deviceHash);
		KnownDevice myDevice = getKnownDevice(deviceHash);
		PendingIntent pi = PendingIntent.getBroadcast(this, (int)System.currentTimeMillis(), newIntent,0);
		am = (AlarmManager)(this.getSystemService(Context.ALARM_SERVICE ));
		am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime() + 1000*time_secs, pi);
	}
}
