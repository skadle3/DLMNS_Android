
package com.gt.seniordesign.dlmns;


import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings.Global;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

public class UpdateTagConfig extends Activity {
	
	KnownDevice newDevicefinal;
	EditText text_tag;
	String new_tag_name;
	EditText text_duty;
	String new_dutycycle_str;
	int new_duty_cycle;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

	    getActionBar().setTitle("Update Tag Information");
	    setContentView(R.layout.activity_update);	
	    
	    Spinner spinner = (Spinner) findViewById(R.id.spinner1);
	    ArrayAdapter<CharSequence> adapter =  ArrayAdapter.createFromResource(this,
	            R.array.duty_cycle_array, android.R.layout.simple_spinner_item);
	    spinner.setAdapter(adapter);
	    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
	        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
	        	Spinner spinner = (Spinner) findViewById(R.id.spinner1);
	            new_dutycycle_str = spinner.getAdapter().getItem(pos).toString();
	        }
	        public void onNothingSelected(AdapterView<?> parent) {
	        	new_dutycycle_str = "32";
	        }
	    });
	            
	    //setup OnClickListeners    
	    Button save_pressed = (Button) findViewById(R.id.add_tag_id_update);
	    Button cancel_pressed = (Button) findViewById(R.id.cancel_id_update);
	    
	    //set variable to a default value in case blanks
	    new_tag_name = "Wallet";
	    new_duty_cycle = 32;
	    
	    save_pressed.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// do this if SAVE gets pressed
			    //editText2 is from new tag input field
			    text_tag = (EditText)findViewById(R.id.editText2);
			    new_tag_name = text_tag.getText().toString();
			    //editText1 is from new dutycycle input field
			    //check if strings are NULL before proceeding
			    if(new_tag_name!=null && new_tag_name.length()>0){
			        //its not NULL
			    	//make new tag name - update knowndevice
			    	//newDevicefinal.setName(new_tag_name);
			    }else{
			    	//it is NULL
			    	// leave tag's name as is - Wallet
			    	new_tag_name = "Wallet";
				}
			    if(new_dutycycle_str!=null && new_dutycycle_str.length()>0){
			        //its not NULL - update knowndevice
			    	new_duty_cycle = Integer.parseInt(new_dutycycle_str);
			    	//newDevicefinal.setDutyCycle(new_duty_cycle);
			    }else{
			    	//it is NULL
			    	// old duty cycle = new duty cycle
			    	new_duty_cycle = 32; //default in case nothing is inputed in dutycycle field
			    	//newDevicefinal.setDutyCycle(new_duty_cycle);
			    }
			    //call this to let mainactivity do its thing to update the itemlist
			    //MainActivity.addKnownDevice(newDevicefinal);
			    //close back to other activity
			    MainActivity.addKnownDevice
			    (new KnownDevice(ScanActivity.selectedDevice, new_tag_name, new_duty_cycle));
			    finish();
			}
		});
	    
	    cancel_pressed.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
			    //do this if CANCEL gets pressed - nothing - go back
			    finish();				
			}
		});
	    
	    
	}
	
	//this populates the menu / creates menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.update, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.close_menu_update:
				// finish activity and close to previous activity (scanActivity)
				finish();
				break;
		}
		
		return true;
	}
	
	
	
}