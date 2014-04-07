package com.gt.seniordesign.dlmns;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class ScanActivity extends ListActivity {
	
	private BluetoothAdapter bluetoothAdapter;
	private BleDevicesAdapter leDeviceListAdapter;
	public static BluetoothDevice selectedDevice;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    getActionBar().setTitle("Scan for New Tags");
	    
	    // Setup the Bluetooth Adapter
	 	final BluetoothManager bluetoothManager =
	 			(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
	 	bluetoothAdapter = bluetoothManager.getAdapter();
	
	 	// Start the bluetooth Scanner
	 	startScan();
	 	
	 	// Create the list adapter
	 	leDeviceListAdapter = new BleDevicesAdapter(getBaseContext());
	 	setListAdapter(leDeviceListAdapter);
	    
	    setContentView(R.layout.activity_scan);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.scan, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
			case R.id.action_close_scan:
				// Clost the scan activity and turn off the BLE Scan
				Toast.makeText(this, "Ending Scan", Toast.LENGTH_SHORT).show();
				bluetoothAdapter.stopLeScan(mLeScanCallback);
				finish();
				break;
		}
		
		return true;
	}
	
	public void startScan() {
		Thread scanThread = new Thread() {
			@Override
			public void run() {
				bluetoothAdapter.startLeScan(mLeScanCallback);
			}
		};
		scanThread.start();
	}
	
	// Callback for BLE Scan
	private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        	if (!MainActivity.isKnownDevice(device)) {
                        		leDeviceListAdapter.addDevice(device, rssi);
                            	leDeviceListAdapter.notifyDataSetChanged();
                        	}
                        }
                    });
                }
	};
	
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = leDeviceListAdapter.getDevice(position);
        if (device == null)
            return;
        
        selectedDevice = device;
        bluetoothAdapter.stopLeScan(mLeScanCallback);
        
        Intent i = new Intent(getBaseContext(), UpdateTagConfig.class);
        startActivity(i);
        finish();

    }
   
}
