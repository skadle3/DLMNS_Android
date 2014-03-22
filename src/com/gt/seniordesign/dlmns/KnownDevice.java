package com.gt.seniordesign.dlmns;

import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

public class KnownDevice {

	private BluetoothDevice thisDevice;
	private String shortName;
	private int duty_cycle = 16;
	public boolean ignoreNext = false;
	public int connectCount = 0;
	public int tagCount = 0;
	public BluetoothGatt currentGattConnection;
	public int connectAttempts = 0;
	public int new_duty_cycle = 0;
	
	
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
