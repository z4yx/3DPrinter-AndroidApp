/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fitctu.printer.BluetoothChat;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.example.android.BluetoothChat.R;
import com.fitctu.printer.BluetoothChat.Decoder.MsgType;
import com.fitctu.printer.BluetoothChat.Decoder.Result;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class BluetoothChat extends Activity implements OnClickListener, BluetoothIface {
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;
    
    private enum MachMode{
    	StandBy,
    	Printing,
    	USBStorage
    }

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Layout Views
    private RelativeLayout mInfoLayout;
    private TextView mConnectionState;
    private TextView mPrinterState;
    
    private RadioButton mExtruderState;
    private TextView mExtruderTemp;
    private TextView mExtruderPower;

    private RadioButton mHeatbedState;
    private TextView mHeatbedTemp;
    private TextView mHeatbedPower;
    
    private Timer mStatusQueryTmr;
    
    private Button mStartPrinting, mStopPrinting;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
    
    private MachMode printerMode = MachMode.StandBy;
    private ArrayAdapter<String> mGCodeAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.main);

        mConnectionState = (TextView) findViewById(R.id.device_state);
        mPrinterState = (TextView)findViewById(R.id.printer_state);
        mInfoLayout = (RelativeLayout)findViewById(R.id.info_layout);
        
        mExtruderState = (RadioButton)findViewById(R.id.extruder_on);
        mExtruderTemp = (TextView)findViewById(R.id.extruder_temp);
        mExtruderPower = (TextView)findViewById(R.id.extruder_power);
        
        mHeatbedState = (RadioButton)findViewById(R.id.heatbed_on);
        mHeatbedTemp = (TextView)findViewById(R.id.heatbed_temp);
        mHeatbedPower = (TextView)findViewById(R.id.heatbed_power);
        
        mStartPrinting = (Button)findViewById(R.id.btn_start);
        mStopPrinting = (Button)findViewById(R.id.btn_stop);
        mStartPrinting.setOnClickListener(this);
        mStopPrinting.setOnClickListener(this);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        mStatusQueryTmr = new Timer(false);
        mStatusQueryTmr.schedule(new TimerTask() {
			
			@Override
			public void run() {
				if(mChatService != null && mChatService.getState() == BluetoothChatService.STATE_CONNECTED)
					sendMessage(buildMessage("QRY", ""));
				
			}
		}, 500, 1600);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);
        
        openDeviceListActivity();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void updateButtonState() {
    	mStopPrinting.setEnabled(printerMode == MachMode.Printing);
    	mStartPrinting.setEnabled(printerMode == MachMode.StandBy);
	}

    public String buildMessage(String cmd, String arg)
    {
    	return "!"+cmd+"#"+arg;
    }
    
    public synchronized void sendMessage(String message) {
    	Log.w("sendMessage", message);
    	
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, "未连接设备", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
        	message = message+"\r\n";
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }
    
    private void handlePrinterReport(String inMsg) {
//		if(mMessageBuffer == null)
//			return;
//		Log.i("inMsg", inMsg);
//		mMessageBuffer.append(inMsg);
//		
//		int index = mMessageBuffer.indexOf("\n");
//		if(index == -1)
//			index = mMessageBuffer.indexOf("\r");
//		if(index >= 0){
//			String line = mMessageBuffer.substring(0, index+1).trim();
//			mMessageBuffer.delete(0, index+1);
			String line = inMsg;
			try {
				Decoder.Result result = Decoder.Decode(line);
				if(result == null)
					return;
				Log.i("decoded", result.Type.toString() + " "+result.Params);
				switch (result.Type) {
				case INFO_EXTRUDER:
					if(result.Params!=null && result.Params.length==3){
						mExtruderState.setChecked(result.Params[2].equals("1"));
						if(mExtruderState.isChecked()){
							String temp = (result.Params[0].equals("-1") ? "null" : result.Params[0]+"℃");
							mExtruderTemp.setText("温度"+temp);
							mExtruderPower.setText("功率"+result.Params[1]+"%");
						}else{
							mExtruderTemp.setText("");
							mExtruderPower.setText("");
						}
					}
					break;
				case INFO_HEATBED:
					if(result.Params!=null && result.Params.length==3){
						mHeatbedState.setChecked(result.Params[2].equals("1"));
						if(mHeatbedState.isChecked()){
							String temp = (result.Params[0].equals("-1") ? "null" : result.Params[0]+"℃");
							mHeatbedTemp.setText("温度"+temp);
							mHeatbedPower.setText("功率"+result.Params[1]+"%");
						}else{
							mHeatbedTemp.setText("");
							mHeatbedPower.setText("");
						}
					}
					break;
				case INFO_PRINT:
					if(result.Params!=null && result.Params.length==3){
						String mode = result.Params[0];
						if(mode.equals("0")){
							printerMode = MachMode.StandBy;
							mPrinterState.setText("空闲");
						}else if(mode.equals("2")){
							printerMode = MachMode.USBStorage;
							mPrinterState.setText("USB存储模式");
						}else if(mode.equals("1")){
							printerMode = MachMode.Printing;
							
							String add;
							if(result.Params[1].equals("1"))
								add = "(回原点)";
							else if(result.Params[1].equals("2"))
								add = "(等待加热)";
							else
								add = "";
							mPrinterState.setText("正在打印"+add+"... "+result.Params[2]+"%");
						}
						updateButtonState();
					}
					break;
				case INFO_REPLY:
					if(result.Params!=null && result.Params.length==1){
						if(result.Params[0].equals("1"))
							Toast.makeText(this, "操作成功", Toast.LENGTH_SHORT).show();
						else
							Toast.makeText(this, "操作失败", Toast.LENGTH_SHORT).show();
					}
					break;
				case INFO_LIST_FILES:
					if(result.Params!=null && result.Params.length==1){

						if(mGCodeAdapter!=null){
							mGCodeAdapter.add(result.Params[0]);
							mGCodeAdapter.notifyDataSetChanged();
						}
					}
					break;
				default:
					break;
				}
				
			} catch (Exception e) {
				Log.e("handlePrinterReport", ""+e.getMessage());
				e.printStackTrace();
			}
//		}
	}


    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                	mConnectionState.setText("已连接: ");
                	mConnectionState.append(mConnectedDeviceName);
                    mInfoLayout.setVisibility(View.VISIBLE);
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                	mConnectionState.setText("连接中...");
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                	mConnectionState.setText("未连接设备");
                    mInfoLayout.setVisibility(View.INVISIBLE);
                    break;
                }
                break;
//            case MESSAGE_WRITE:
//                byte[] writeBuf = (byte[]) msg.obj;
//                String writeMessage = new String(writeBuf);
//                mConversationArrayAdapter.add("Me:  " + writeMessage);
//                break;
            case MESSAGE_READ:
//                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
//                String readMessage = new String(readBuf, 0, msg.arg1);
//                handlePrinterReport(readMessage);
            	Log.w("handleMessage", "MESSAGE_READ: " + (String)msg.obj);
                handlePrinterReport((String)msg.obj);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
//                Toast.makeText(getApplicationContext(), "Connected to "
//                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mChatService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, "蓝牙未打开", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    private void openDeviceListActivity() {

        // Launch the DeviceListActivity to see devices and do scan
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
        	openDeviceListActivity();
            return true;
        case R.id.manualheat:
        	openManualHeatDialog();
        	return true;
        case R.id.opendebug:
        	openDebugDialog();
        	return true;
//        case R.id.discoverable:
//            // Ensure this device is discoverable by others
//            ensureDiscoverable();
//            return true;
        }
        return false;
    }
    
    
    
    @Override
	protected Dialog onCreateDialog(int id) {
    	Dialog d = null;
    	if(id == 1){
    		d = new DebugUtilityDialog(this, this);
    		d.requestWindowFeature(Window.FEATURE_NO_TITLE);
    	}else if(id==2){
    		d = new ManualHeatDialog(this, this);
    		d.requestWindowFeature(Window.FEATURE_NO_TITLE);
    	}
		return d;
	}

	private void openDebugDialog() {
		showDialog(1);
	}
	private void openManualHeatDialog() {
		showDialog(2);
	}
    
    private void openChooseFileDialog() {
    	new AlertDialog.Builder(this)
		.setTitle("选择G代码文件")
		.setAdapter(mGCodeAdapter, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				sendMessage(buildMessage("START", String.valueOf(arg1)));
			}
		})
		.setNegativeButton("取消", null)
		.show();
	}
    
    private void openStopPrintingDialog() {
    	new AlertDialog.Builder(this)
		.setTitle("提示")
		.setMessage("确认停止打印吗?")
		.setPositiveButton("确认", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				sendMessage(buildMessage("STOP", ""));
			}
		})
		.setNegativeButton("取消", null)
		.show();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_start:
			mGCodeAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice);
			sendMessage(buildMessage("LIST", ""));
			openChooseFileDialog();
			break;

		case R.id.btn_stop:
			openStopPrintingDialog();
			break;
		}
	}

}