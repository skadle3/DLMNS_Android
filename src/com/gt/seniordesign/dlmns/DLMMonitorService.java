package com.gt.seniordesign.dlmns;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
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
				 
				 connection_success = true;
				 
				 KnownDevice myDevice = getKnownDevice(gatt.getDevice().hashCode());
				 myDevice.lastconnect_success = true;
				 //setConnectAlarm(myDevice.getDutyCycle(), gatt.getDevice().hashCode());
				 
				 try {
					 Thread.sleep(1000);
				 } catch (Exception ex) {
						
				 }
				 
				 gatt.disconnect();
				 gatt.close();
				 
			 } else {
				 connection_success = true;
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
	 
	 boolean connection_success = false;
	 BluetoothDevice currentDevice;
	 
	 private class connectionCheck implements Runnable {
		 
		 private int dutyCycle;
		 private int deviceHash;
		 
		 public connectionCheck(int dutyCycle, int deviceHash) {
			 this.dutyCycle = dutyCycle;
			 this.deviceHash = deviceHash;
		 }
		 
		 public void run() {
			 	// see if the device actually got connected
		    	// if not we need to stop the connection (it's bad i know)
		    	if (!connection_success) {
		    		// Set the alarm for duty cycle - 1 second
		    		//setConnectAlarm(dutyCycle - 1, deviceHash);
		    		resetBluetooth();
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
 	 			connectionCheck r = new connectionCheck(foundDevice.getDutyCycle(), deviceHash);
 	 		
 	 			foundDevice.getDeviceContext().connectGatt(getApplicationContext(), false, new MyBleCallback());
 	 			scan_handler.postDelayed(r, 2000);
 	 			
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
		 myDevice.getDeviceContext().connectGatt(getApplicationContext(), false, new MyBleCallback());
		 
		 // Now, start the alarm (here or on disconnect??)...Probably on Disconnect
		 
		 
		 try {
   			Thread.sleep(2000);
   		 } catch (Exception ex) {}
		 
		 resetBluetooth(); 
		 
		 
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
		 PendingIntent pi = PendingIntent.getBroadcast(this, (int)System.currentTimeMillis(), newIntent,0);
		 am = (AlarmManager)(this.getSystemService(Context.ALARM_SERVICE ));
		 am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime() + 1000*time_secs, pi);
	 }
}
