package com.gt.seniordesign.dlmns;

import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;

public class KnownDevice {

	private BluetoothDevice thisDevice;
	private String shortName;
	private int duty_cycle;
	public boolean lastconnect_success = false;
	public boolean ignoreNext = false;
	public int connectCount = 0;
	public PendingIntent pi;
	
	public KnownDevice(BluetoothDevice newDevice, String newName, int duty_cycle) {
		thisDevice = newDevice;
		shortName = newName;
		this.duty_cycle = duty_cycle;
	}
	
	public String getName() {
		return shortName;
	}
	
	public void setName(String newName) {
		shortName = newName;
	}
	
	public void setDutyCycle(int newDutyCycle) {
		duty_cycle = newDutyCycle;
	}
	
	public int getDutyCycle() {
		return duty_cycle;
	}
	
	public BluetoothDevice getDeviceContext() {
		return thisDevice;
	}
	
	@Override
	public boolean equals(Object o) {
		BluetoothDevice otherDevice = (BluetoothDevice) o;
		return thisDevice.getAddress().equals(otherDevice.getAddress());		
	}
} 
