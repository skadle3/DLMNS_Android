/////////////////////////////////////////////////////////////////////////////////////////
// Don't Leave Me Notification System Team (DLMNS)
// ECE 4012-L3A Senior Design
//
// Main Activity - This class implements the tag management functionality for the application
// On boot it is also responsible for starting a BLE scan to re-acquire device after system
// boot.
//
// Note - Part of this source code was adopted from the Android Open Source Project
/////////////////////////////////////////////////////////////////////////////////////////

package com.gt.seniordesign.dlmns;

import android.os.Bundle;
import android.os.IBinder;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;

public class MainActivity extends ListActivity {

	// Member Variables
	private static ArrayList<String> storedTagNames;
	private static ArrayAdapter<String> ListAdapter;
	private BluetoothAdapter bluetoothAdapter;
	private static DLMMonitorService mDLMMonitorService;
	
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
        	mDLMMonitorService = ((DLMMonitorService.LocalBinder) service).getService();
            if (!mDLMMonitorService.initialize()) {
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        	mDLMMonitorService = null;
        }
    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setTitle("Manage Existing Tags");
		Toast.makeText(this, "Never Leave Anything Behind!", Toast.LENGTH_SHORT).show();
		storedTagNames = new ArrayList<String>();
		
		// Setup the Bluetooth Adapter
		final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        } else {
        	// Toggle Bluetooth just to be safe (unstable)
        	toggleBluetooth(bluetoothAdapter);
        }
        
        // Start the DLM Monitor Service
        Intent dlmServiceIntent = new Intent(this, DLMMonitorService.class);
        bindService(dlmServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
		
		// Setup the ArrayAdapter. As devices are scanned and added, they will be added to this view
		ListAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, storedTagNames);
		setListAdapter(ListAdapter); 
		
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
		// Handles Selection on Known Devices from ListView
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
			case R.id.action_scan:
				// Open the Scan Activity
				Toast.makeText(this, "Opening Tag Scan Activity", Toast.LENGTH_SHORT).show();
				Intent intent = new Intent(this, ScanActivity.class);
				startActivity(intent);
				break;
			case R.id.action_settings:
				// Not sure what this is used for
				Toast.makeText(this, "Not sure what settings we need!", Toast.LENGTH_SHORT).show();
				break;
		}
		
		return true;
	}
	
	protected void onDestroy() { 
		super.onDestroy();
		getApplicationContext().unbindService(mServiceConnection); 
	}
	
	
	// Function to toggle Bluetooth on/off to ensure stability
	public static void toggleBluetooth(BluetoothAdapter adapter) {
		
		adapter.disable();
        try {
        	Thread.sleep(1000);
        } catch (Exception e) {
        	
        }
        adapter.enable();
	}
	
	
	
	// Helper function to remove a device from the known device list
	public static void removeKnownDevice(KnownDevice removeDevice) {
		
		// Notify the Service that something was removed
	}
	
	// Helper function to add device to known device list
	public static void addKnownDevice(KnownDevice newDevice) {
		storedTagNames.add(newDevice.getName()); // This is temporary...need to link to monitored devices array in service
		ListAdapter.notifyDataSetChanged();
		
		// Notify the Service that something was added
		mDLMMonitorService.addNewDevice(newDevice);
	}
	
	// Helper function to determine if a device is already in our monitored list
	public static boolean isKnownDevice(BluetoothDevice device) {
		for (KnownDevice dev : mDLMMonitorService.getKnownDevices()) {
			if (dev.equals(device)) {
				return true;
			}
		}
		return false;
	}
}
