package com.gt.seniordesign.dlmns;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class KnownDeviceAdapter extends ArrayAdapter<KnownDevice> {
	private final Context context;
	private final ArrayList<KnownDevice> values;
 
	public KnownDeviceAdapter(Context context, ArrayList<KnownDevice> values) {
		super(context, R.layout.listitem_knowndevice, values);
		this.context = context;
		this.values = values;
	}
	
	public KnownDevice getDevice(int pos) {
		return values.get(pos);
	}
 
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
			.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
 
		View rowView = inflater.inflate(R.layout.listitem_knowndevice, parent, false);
		TextView textView = (TextView) rowView.findViewById(R.id.device_name);
		TextView notificationTimeView = (TextView) rowView.findViewById(R.id.notification_time);
		Switch onOffSwitch = (Switch) rowView.findViewById(R.id.onoffswitch);
		
		textView.setText(values.get(position).getName());
		notificationTimeView.setText("Notification Time: " + values.get(position).getDutyCycle() + " sec");
		onOffSwitch.setChecked(values.get(position).notificationsEnabled);
		onOffSwitch.setOnCheckedChangeListener(new ToggleSwitchListener(position));
		
		return rowView;
	}
	
	private class ToggleSwitchListener implements CompoundButton.OnCheckedChangeListener {
		
		private int position;
		public ToggleSwitchListener(int index) {
			this.position = index;
		}
		
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
	        values.get(position).notificationsEnabled = isChecked;
	    }
	}
}

