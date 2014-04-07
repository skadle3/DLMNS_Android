package com.gt.seniordesign.dlmns;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ModifyTagMain extends Activity{

	private KnownDevice selectedDevice;
	private EditText text_tag;
	private String new_tag_name;
	private EditText text_duty;
	private String new_dutycycle_str;
	private int new_duty_cycle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getActionBar().setTitle("Modify Tag Information");
		setContentView(R.layout.activity_modify);	

		//setup OnClickListeners    
		Button modify_pressed = (Button) findViewById(R.id.modify_tag_modify);
		Button cancel_pressed = (Button) findViewById(R.id.cancel_id_modify);
		Button delete_pressed = (Button) findViewById(R.id.delete_button);
		
		// set the display of the duty cycle and tag name to current tag's	    
		TextView text_name = (TextView) findViewById(R.id.editText2);
		TextView text_dutycycle = (TextView) findViewById(R.id.editText1);

		// Get the Intent
		Intent currentIntent = getIntent();
		int pos = currentIntent.getIntExtra("position", 0);
		selectedDevice = MainActivity.monitoredDevicesAdapter.getDevice(pos);
		
		text_name.setText(selectedDevice.getName());
		text_dutycycle.setText(selectedDevice.getDutyCycle());

		modify_pressed.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// do this if SAVE gets pressed
				//editText2 is from new tag input field
				text_tag = (EditText)findViewById(R.id.editText2);
				new_tag_name = text_tag.getText().toString();
				//editText1 is from new dutycycle input field
				text_duty = (EditText)findViewById(R.id.editText1);
				new_dutycycle_str = text_duty.getText().toString();
				
				//check if strings are NULL before proceeding
				if(new_tag_name == null || new_tag_name.length() <= 0){
					new_tag_name = selectedDevice.getName();
				}
				
				if (new_dutycycle_str != null && new_dutycycle_str.length() > 0) {
					new_duty_cycle = Integer.parseInt(new_dutycycle_str);
				} else {
					new_duty_cycle = 32;
				}
				
				selectedDevice.setName(new_tag_name);
				selectedDevice.setDutyCycle(new_duty_cycle);
			}
		});

		cancel_pressed.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				//do this if CANCEL gets pressed - nothing - go back
				finish();				
			}
		});
		
		delete_pressed.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				MainActivity.removeKnownDevice(selectedDevice);
				MainActivity.monitoredDevicesAdapter.notifyDataSetChanged();
				finish();				
			}
		});

	}

}
