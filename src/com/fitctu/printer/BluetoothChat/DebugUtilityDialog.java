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

public class DebugUtilityDialog extends Dialog implements android.view.View.OnClickListener, OnSeekBarChangeListener, OnCheckedChangeListener {
	
	Context mContext;
	BluetoothIface mBluetoothIface;
	
	EditText mXValText, mYValText, mZValText, mAValText;
	TextView mExValTextView, mHbValTextView;
	SeekBar mExSeekBar, mHbSeekBar;
	ToggleButton mFanToggleButton;

	public DebugUtilityDialog(Context context, BluetoothIface bt) {
		super(context);
		mContext = context;
		mBluetoothIface = bt;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.debug_dialog);
		
		findViewById(R.id.dbg_x_m).setOnClickListener(this);
		findViewById(R.id.dbg_x_p).setOnClickListener(this);
		findViewById(R.id.dbg_y_m).setOnClickListener(this);
		findViewById(R.id.dbg_y_p).setOnClickListener(this);
		findViewById(R.id.dbg_z_m).setOnClickListener(this);
		findViewById(R.id.dbg_z_p).setOnClickListener(this);
		findViewById(R.id.dbg_a_m).setOnClickListener(this);
		findViewById(R.id.dbg_a_p).setOnClickListener(this);

		mXValText = (EditText)findViewById(R.id.dbg_x_val);
		mYValText = (EditText)findViewById(R.id.dbg_y_val);
		mZValText = (EditText)findViewById(R.id.dbg_z_val);
		mAValText = (EditText)findViewById(R.id.dbg_a_val);

		mExValTextView = (TextView)findViewById(R.id.dbg_ex_val);
		mHbValTextView = (TextView)findViewById(R.id.dbg_hb_val);
		
		mExSeekBar = (SeekBar)findViewById(R.id.dbg_ex_bar);
		mHbSeekBar = (SeekBar)findViewById(R.id.dbg_hb_bar);
		
		mFanToggleButton = (ToggleButton)findViewById(R.id.dbg_fan_tb);
		
		mFanToggleButton.setOnCheckedChangeListener(this);
		
		mExSeekBar.setOnSeekBarChangeListener(this);
		mHbSeekBar.setOnSeekBarChangeListener(this);
	}

	@Override
	public void onClick(View arg0) {
		int id = arg0.getId();
		try{
			int sign = 1, val = 0;
			String axis="";
			switch (id) {
			case R.id.dbg_x_m:
			case R.id.dbg_y_m:
			case R.id.dbg_z_m:
			case R.id.dbg_a_m:
				sign = -1;
				break;
			default:
				break;
			}
			switch (id) {
			case R.id.dbg_x_m:
			case R.id.dbg_x_p:
				axis = "X";
				val = Integer.parseInt(mXValText.getText().toString());
				break;
	
			case R.id.dbg_y_m:
			case R.id.dbg_y_p:
				axis = "Y";
				val = Integer.parseInt(mYValText.getText().toString());
				break;
	
			case R.id.dbg_z_m:
			case R.id.dbg_z_p:
				axis = "Z";
				val = Integer.parseInt(mZValText.getText().toString());
				break;
	
			case R.id.dbg_a_m:
			case R.id.dbg_a_p:
				axis = "A";
				val = Integer.parseInt(mAValText.getText().toString());
				break;
	
			default:
				break;
			}
			val *= sign;
			
			Log.w("value", ""+val);
			
			if(axis.length() > 0){
				mBluetoothIface.sendMessage(mBluetoothIface.buildMessage("DBG", axis+val));
			}
		}catch (Exception e) {
			Log.e("DebugUtilityDialog.onClick", ""+e.getMessage());
		}
	}

	@Override
	public void onProgressChanged(SeekBar sb, int progress, boolean arg2) {
		if(sb == mExSeekBar)
			mExValTextView.setText(String.valueOf(progress)+"%");
		else if(sb == mHbSeekBar)
			mHbValTextView.setText(String.valueOf(progress)+"%");
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar sb) {
		int progress=sb.getProgress();
		String name;
		if(sb == mExSeekBar)
			name="e";
		else if(sb == mHbSeekBar)
			name="h";
		else
			return;

		Log.w("onStopTrackingTouch", name+": "+progress+"%");
		mBluetoothIface.sendMessage(mBluetoothIface.buildMessage("DBG", name+progress));
	}

	@Override
	public void onCheckedChanged(CompoundButton arg0, boolean state) {
		mBluetoothIface.sendMessage(mBluetoothIface.buildMessage("DBG", "f"+(state ? 1 : 0)));
	}

}
