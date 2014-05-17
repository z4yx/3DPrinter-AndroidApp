package com.fitctu.printer.BluetoothChat;


import com.example.android.BluetoothChat.R;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ManualHeatDialog extends Dialog implements  OnCheckedChangeListener {
	
	Context mContext;
	BluetoothIface mBluetoothIface;
	

	public ManualHeatDialog(Context context, BluetoothIface bt) {
		super(context);
		mContext = context;
		mBluetoothIface = bt;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.manual_heat_dialog);
		
		((ToggleButton)findViewById(R.id.extruder_toggle)).setOnCheckedChangeListener(this);
		((ToggleButton)findViewById(R.id.heatbed_toggle)).setOnCheckedChangeListener(this);

	}
//
//	@Override
//	public void onClick(View arg0) {
//		int id = arg0.getId();
//		try{
//			int sign = 1, val = 0;
//			String axis="";
//			val *= sign;
//			
//			Log.w("value", ""+val);
//			
//			if(axis.length() > 0){
//				mBluetoothIface.sendMessage(mBluetoothIface.buildMessage("DBG", axis+val));
//			}
//		}catch (Exception e) {
//			Log.e("DebugUtilityDialog.onClick", ""+e.getMessage());
//		}
//	}


	@Override
	public void onCheckedChanged(CompoundButton v, boolean state) {
		String value;
		String dev;
		EditText et;
		if(v.getId() == R.id.extruder_toggle){
			dev = "E";
			et = (EditText)findViewById(R.id.extruder_settemp);
		}else if(v.getId() == R.id.heatbed_toggle){
			dev = "H";
			et = (EditText)findViewById(R.id.heatbed_settemp);
		}else{
			return;
		}
		value = et.getEditableText().toString();
		mBluetoothIface.sendMessage(mBluetoothIface.buildMessage("DBG", dev+(state ? value : "-")));
		et.setEnabled(!state);
	}

}
