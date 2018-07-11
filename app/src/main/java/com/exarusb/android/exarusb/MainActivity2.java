/*
*Copyright 2014 Exar Corporation
*
*Licensed under the Apache License, Version 2.0 (the "License");
*you may not use this file except in compliance with the License.
*You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing, software
*distributed under the License is distributed on an "AS IS" BASIS,
*WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*See the License for the specific language governing permissions and
*limitations under the License.
**Revision history:
*initial version 1.0
*version 1.1: added support for the XR21V1412 and XR21V1414
*/

package com.exarusb.android.exarusb;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.exarusb.android.exarusb.file.FileSelectActivity;
import com.exarusb.android.usbdriver.XRDriver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 
 * @author Exar Corporation
 * 
 * Sample UART Application
 */

public class MainActivity2 extends Activity implements OnCheckedChangeListener,View.OnClickListener{

	XRDriver mSerial;

	//private static final String ACTION_USB_PERMISSION = "com.exarusb.android.exarusb.USB_PERMISSION";
	private static final String ACTION_USB_PERMISSION = MainActivity2.class.getName() + ".ACTION_FOR_PERMISSION";
	//Variables for Main UI window components
	private String[] baud_values;
	//private Button btnWrite;
	//private TextView deviceInfo;
	//private TextView productDesc;
	private TextView tvMonitor; //Characters Sent Info Box
	private TextView tvMonitorRight; //Characters Received Info Box
	private TextView tv_send, tv_receive, tv_baudrate, tv_databits, tv_parity,
			tv_stopbits, tv_flowcontrol,tv_channel;
	private ToggleButton toggleConnect;
	private AutoCompleteTextView textBaudRate;
	private Spinner spinDataBits;
	private Spinner spinParityBits;
	private Spinner spinStopBits;
	private Spinner spinChannelSel;
	private Spinner spinFlowControl;
	private CheckBox cb_loopback;
	//private EditText editText;
	Button btn_register,btn_capture,btn_check_version,btn_clear_record,btn_close_port,btn_reset_preview,btn_delete_user;
	EditText et_userId;
	CheckBox cb_led_off,cb_led_level_1,cb_led_level_2,cb_led_level_3,cb_face_enable,cb_face_disable;
	//private CheckBox cb_send_u;
	// CheckBox cb_continuous_send;
	private CheckBox cb_hexsend;
	private CheckBox cb_hexreceive;

	//Register Read/Write
	private RadioButton radioButtonWrite;
	private RadioButton radioButtonRead;
	private EditText edit_regOffset;
	private EditText edit_regValue;
	private TextView tview_regValue;
	private Button btn_regRW;

	//Send/Receive
	private StringBuilder mText = new StringBuilder();
	private StringBuilder mTextRight = new StringBuilder();
	private StringBuilder st = new StringBuilder();
	private StringBuilder sb = new StringBuilder();
	private StringBuilder datasb = new StringBuilder();
	private StringBuilder datasent = new StringBuilder();
	private ScrollView scroller;
	private ScrollView scrollerRight;
	private boolean readFlag, isSendU, isContSend, flagLoopBack, isHexSend,
			isHexReceive;

	static final int HANDLE_READ = 1;
	static final int UPDATE_UI = 2;
	static final int UPDATE_SEND = 3;
	static final int UPDATE_SEND_UI = 4;

	Thread receiveThread, contsendUThread, contsendThread;
	int databits;
	Timer timer; //Timer Object created

	//Default Values
	private String TAG = "EXAR UART";
	private static final int REQUEST_CODE = 184;
	File mSelectedFile;
	ProgressDialog progressDialog;
	private int default_baudrate = 115200;
	private int min_baudrate = 300;
	private int max_baudrate = 12000000;

	private int parityBits = 0;
	private int flowControl = 0;
	private int baudrate = default_baudrate;
	private int stopBits = 1;
	private int serialportsel = 0;
	private int dataBits = 8;
	private final int BUFF_SIZE = 6;
	private final int TIMER_PERIOD = 600;
	private final int TIMER_DELAY = 20;
	private final int MAX_OUTPUT_LINES = 500;
	int max = 1000;
	private boolean isScrollBottom = false;
	private  List<String> list ;
	private static final String ACTION_FOR_PERMISSION = MainActivity2.class.getName() + ".ACTION_FOR_PERMISSION";
	final BroadcastReceiver permissionReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_FOR_PERMISSION.equals(action)) {
				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
				{
					Toast.makeText(context, "Authorization Successful", Toast.LENGTH_SHORT).show();
					mSerial.whenHadPermission(serialportsel);
					autoConnect();
					//deviceInfoDisplay();
					invalidateOptionsMenu();
				}
				else
				{
					Toast.makeText(context, "Authorization Failed", Toast.LENGTH_SHORT).show();
					finish();
				}
			}
		}
	};
	/**
	 * Application OnCreate method
	 *
        **/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_2);
		//setContentView(R.layout.reg_dialog);
		getActionBar().setIcon(R.drawable.logo);
		PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		setupUI(findViewById(R.id.LinearLayout1));
		baud_values = getResources().getStringArray(R.array.baud_array);
		initUI(); // Initializing the UI components
        mSerial = new XRDriver(
				(UsbManager) getSystemService(Context.USB_SERVICE));

        IntentFilter filter_permission = new IntentFilter(ACTION_FOR_PERMISSION);
        filter_permission.addAction(ACTION_FOR_PERMISSION);
		this.registerReceiver(permissionReceiver, filter_permission);

		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		filter.addAction(ACTION_USB_PERMISSION);
		registerReceiver(mUsbReceiver, filter);
		mSerial.setPermissionIntent(mPermissionIntent);
		if (toggleConnect.isChecked()) {

		} else {
			if (mSerial.begin(serialportsel,null)) {
				autoConnect();
				//deviceInfoDisplay();
			} else {
				Toast.makeText(getApplicationContext(),
						R.string.device_connection, Toast.LENGTH_SHORT).show();
	    		}
		}

		spinDataBits.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View arg1,
					int arg2, long arg3) {
				parent.getItemAtPosition(arg2);
				databits = Integer.parseInt(parent.getItemAtPosition(arg2)
						.toString());
				if (databits == 9) {
					spinParityBits.setSelection(0);
					spinParityBits.setEnabled(false);
				} else if (toggleConnect.isChecked()) {
					spinParityBits.setEnabled(false);
				} else if (databits == 7 || databits == 8) {
					spinParityBits.setEnabled(true);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}

		});

		tvMonitorRight.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				removeTextFromReceive();
			}
		});

		tvMonitor.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				removeTextFromSend();

			}
		});

	}

	/**
	 * Clear Receive info box window
	 *
        **/
	private void removeTextFromReceive() {
		int linesToRemove = tvMonitorRight.getLineCount() - MAX_OUTPUT_LINES;
		if (linesToRemove > 0) {
			tvMonitorRight.setText("");
		}
	}

	/**
	 * Clear Send Info Box window
	 *
        **/
	private void removeTextFromSend() {
		int linesToRemove = tvMonitor.getLineCount() - MAX_OUTPUT_LINES;
		if (linesToRemove > 0) {
			tvMonitor.setText("");
		}
	}

	/**
	 * Application drop down menu create
	 *
        **/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * Application drop down menu enable
	 *
        **/
	private void EnableMenuOptions(Menu menu) {

		menu.getItem(0).setEnabled(true);
		menu.getItem(1).setEnabled(true);
	}

	/**
	 * Application drop down menu disable
	 *
        **/
	private void DisableMenuOptions(Menu menu) {
		menu.getItem(0).setEnabled(false);
		menu.getItem(1).setEnabled(false);
	}

	/**
	 * Application drop down menu enable/disable on condition
	 *
        **/
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		if (toggleConnect.isChecked() && !isContSend && !isSendU) {
			EnableMenuOptions(menu);
		} else {
			DisableMenuOptions(menu);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Application drop down menu item activity
	 *
     **/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_sendfile:
			Intent intent = new Intent(MainActivity2.this,
					FileSelectActivity.class);
			try {
				startActivityForResult(intent, REQUEST_CODE);
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "Activity not found!");
			}
			return true;

		case R.id.action_regwrite:
			RegisterRWAlertDialog();
			return true;

		case R.id.action_about:
			Intent about = new Intent(
					Intent.ACTION_VIEW,
					Uri.parse("http://www.exar.com/connectivity/uart-and-bridging-solutions/usb-uarts/"));
			startActivity(about);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Application activity on device back key pressed event
	 *
        **/
	@Override
	public void onBackPressed() {
		exitDialog();
	}

	/**
	 * Application On Pause
	 *
        **/
	@Override
	protected void onPause() {
		super.onPause();
	}

	/**
	 * Activities on Application resume after pause.
	 *
        **/
	@Override
	protected void onResume() {
		super.onResume();
		if (databits == 9 || toggleConnect.isChecked()) {
			spinParityBits.setEnabled(false);
		}
	}

	/**
	 * This method performs cleanup activities at the time of application exit.
	 *
        **/
	@Override
	public void onDestroy() {
		super.onDestroy();
		readFlag = false;
	//	removeDeviceInfo();
		if(timer != null)
		CancelTimerTask();
		mSerial.end();
		unregisterReceiver(mUsbReceiver);
	}

	/**
	 * This method initialize all the UI components, at the time application
	 * launch.
	**/
	private void initUI() {
		tv_send = (TextView) findViewById(R.id.text_send);
		tv_receive = (TextView) findViewById(R.id.text_receive);
		tv_baudrate = (TextView) findViewById(R.id.text_baud);
		tv_databits = (TextView) findViewById(R.id.text_data_bits);
		tv_parity = (TextView) findViewById(R.id.text_parity_bits);
		tv_stopbits = (TextView) findViewById(R.id.text_stop_bits);
		tv_channel = (TextView) findViewById(R.id.text_channel_sel);
		tv_flowcontrol = (TextView) findViewById(R.id.tv_flow_control);
		toggleConnect = (ToggleButton) findViewById(R.id.toggleConnect);
		tvMonitor = (TextView) findViewById(R.id.tvMonitor);
		tvMonitorRight = (TextView) findViewById(R.id.tvMonitorRight);
		textBaudRate = (AutoCompleteTextView) findViewById(R.id.autocomplete_baud);
		textBaudRate.setThreshold(1);
		textBaudRate.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, baud_values));
		spinDataBits = (Spinner) findViewById(R.id.spin_data_bits);
		spinDataBits.setSelection(1);
		spinParityBits = (Spinner) findViewById(R.id.spin_parity_bits);
		spinStopBits = (Spinner) findViewById(R.id.spin_stop_bits);
		spinChannelSel = (Spinner) findViewById(R.id.spin_channel_sel);
		spinFlowControl = (Spinner) findViewById(R.id.spin_flow_control);
		cb_loopback = (CheckBox) findViewById(R.id.checkbox_loopback);
		scroller = (ScrollView) findViewById(R.id.scroller);
		scrollerRight = (ScrollView) findViewById(R.id.scrollerRight);
		cb_hexsend = (CheckBox) findViewById(R.id.hex_send);
		cb_hexreceive = (CheckBox) findViewById(R.id.hex_receive);

		btn_register = (Button) findViewById(R.id.btn_register);
		btn_capture = (Button) findViewById(R.id.btn_capture);
		btn_check_version = (Button) findViewById(R.id.btn_checkVersion);
		btn_clear_record = (Button) findViewById(R.id.btn_clear_record);
		btn_close_port = (Button) findViewById(R.id.btn_close_port);
		btn_reset_preview = (Button) findViewById(R.id.btn_reset_preview);
		btn_delete_user = (Button) findViewById(R.id.btn_delete_user);

        btn_register.setOnClickListener(this);
        btn_capture.setOnClickListener(this);
        btn_check_version.setOnClickListener(this);
        btn_clear_record.setOnClickListener(this);
        btn_close_port.setOnClickListener(this);
        btn_reset_preview.setOnClickListener(this);
        btn_delete_user.setOnClickListener(this);

		et_userId = (EditText) findViewById(R.id.et_user_id);
		cb_led_off = (CheckBox) findViewById(R.id.cb_led_off);
		cb_led_level_1 = (CheckBox) findViewById(R.id.cb_led_level_1);
		cb_led_level_2 = (CheckBox) findViewById(R.id.cb_led_level_2);
		cb_led_level_3 = (CheckBox) findViewById(R.id.cb_led_level_3);

        cb_led_off.setOnClickListener(this);
        cb_led_level_1.setOnClickListener(this);
        cb_led_level_2.setOnClickListener(this);
        cb_led_level_3.setOnClickListener(this);

		cb_face_enable = (CheckBox) findViewById(R.id.cb_face_on);
		cb_face_disable = (CheckBox) findViewById(R.id.cb_face_off);

        cb_face_enable.setOnClickListener(this);
        cb_face_disable.setOnClickListener(this);

		cb_loopback.setOnCheckedChangeListener(this);
		cb_hexsend.setEnabled(false);
		cb_hexreceive.setEnabled(false);
		cb_hexsend.setTextSize(15);
		cb_hexreceive.setTextSize(15);
		tv_send.setTextColor(getResources().getColor(R.color.gray));
		tv_receive.setTextColor(getResources().getColor(R.color.gray));
		cb_hexsend.setOnCheckedChangeListener(this);
		cb_hexreceive.setOnCheckedChangeListener(this);

	}


	/**
	 * Enables some UI component on device disconnect
	 *
	 */
	private void enableUI() {

		textBaudRate.setEnabled(true);
		spinDataBits.setEnabled(true);
		spinParityBits.setEnabled(true);
		spinStopBits.setEnabled(true);
		int PID;
		PID = mSerial.getproductid();
		if((mSerial.getproductid() == 0x1414) ||
		   (mSerial.getproductid() == 0x1412) ||
		   (mSerial.getproductid() == 0x1422) ||
		   (mSerial.getproductid() == 0x1424) ||
		   (mSerial.getproductid() == 0x1400) ||
		   (mSerial.getproductid() == 0x1401) ||
		   (mSerial.getproductid() == 0x1402) ||
		   (mSerial.getproductid() == 0x1403))
		{
			 list = new ArrayList<String>();
			 if((mSerial.getproductid() == 0x1414) ||
			    (mSerial.getproductid() == 0x1424) ||
			    (mSerial.getproductid() == 0x1402) ||
			    (mSerial.getproductid() == 0x1403))
			 {
	            list.add("COM1");
	            list.add("COM2");
	            list.add("COM3");
	            list.add("COM4");
			 }
			 else if((mSerial.getproductid() == 0x1412) ||
					 (mSerial.getproductid() == 0x1422) ||
					 (mSerial.getproductid() == 0x1400) ||
					 (mSerial.getproductid() == 0x1401))
			 {
				 list.add("COM1");
		         list.add("COM2");
			 }
			 else
			 {
				 list.add("COM1");
			 }
	         ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,list);
	         adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	         spinChannelSel.setAdapter(adapter);
	    	 spinChannelSel.setEnabled(true);
		}
		else
		{
			spinChannelSel.setEnabled(false);
			spinChannelSel.setSelection(0);

		}

		spinFlowControl.setEnabled(true);
		cb_loopback.setEnabled(true);

		if( cb_loopback.isChecked())
			cb_loopback.setTextColor(getResources().getColor(R.color.lime));
		else
			cb_loopback.setTextColor(getResources().getColor(R.color.white));

		tv_baudrate.setTextColor(getResources().getColor(R.color.white));
		tv_databits.setTextColor(getResources().getColor(R.color.white));
		tv_parity.setTextColor(getResources().getColor(R.color.white));
		tv_stopbits.setTextColor(getResources().getColor(R.color.white));
		tv_channel.setTextColor(getResources().getColor(R.color.white));
		tv_flowcontrol.setTextColor(getResources().getColor(R.color.white));


		cb_hexsend.setEnabled(false);
		cb_hexsend.setChecked(false);
		cb_hexreceive.setEnabled(false);
		cb_hexreceive.setChecked(false);


		cb_hexsend.setTextColor(getResources().getColor(R.color.gray));
		cb_hexreceive.setTextColor(getResources().getColor(R.color.gray));


		tv_send.setTextColor(getResources().getColor(R.color.gray));
		tv_receive.setTextColor(getResources().getColor(R.color.gray));
		//GL
		tvMonitorRight.setEnabled(false);
		tvMonitor.setEnabled(false);

	}

	/**
	 * Disable some UI component on device get connected.
	 *
	**/
	private void disableUI() {
		if (readFlag) {
			textBaudRate.setEnabled(false);
			spinDataBits.setEnabled(false);
			spinParityBits.setEnabled(false);
			spinStopBits.setEnabled(false);
			spinChannelSel.setEnabled(false);
			if((mSerial.getproductid() != 0x1412)&&
			   (mSerial.getproductid() != 0x1414)&&
			   (mSerial.getproductid() != 0x1422)&&
			   (mSerial.getproductid() != 0x1424)&&
			   (mSerial.getproductid() != 0x1400)&&
			   (mSerial.getproductid() != 0x1401)&&
			   (mSerial.getproductid() != 0x1402)&&
			   (mSerial.getproductid() != 0x1403))
			{
			  spinChannelSel.setSelection(0);
			}
			spinFlowControl.setEnabled(false);
			cb_loopback.setEnabled(false);

			//GL
			if(cb_loopback.isChecked()) {
				cb_loopback.setTextColor(getResources().getColor(R.color.teal));
			} else {
				cb_loopback.setTextColor(getResources().getColor(R.color.gray));
			}
			tvMonitor.setEnabled(true);
			tvMonitorRight.setEnabled(true);

			tv_baudrate.setTextColor(getResources().getColor(R.color.gray));
			tv_databits.setTextColor(getResources().getColor(R.color.gray));
			tv_parity.setTextColor(getResources().getColor(R.color.gray));
			tv_stopbits.setTextColor(getResources().getColor(R.color.gray));
			tv_channel.setTextColor(getResources().getColor(R.color.gray));
			tv_flowcontrol.setTextColor(getResources().getColor(R.color.gray));

			//editText.setEnabled(true);
			//btnWrite.setEnabled(true);
			//cb_continuous_send.setEnabled(true);
			//cb_send_u.setEnabled(true);
			cb_hexsend.setEnabled(true);
			cb_hexreceive.setEnabled(true);

			//cb_send_u.setTextColor(getResources().getColor(R.color.white));
			cb_hexsend.setTextColor(getResources().getColor(R.color.white));
			cb_hexreceive.setTextColor(getResources().getColor(R.color.white));
			//cb_continuous_send.setTextColor(getResources().getColor(R.color.white));

			tv_send.setTextColor(getResources().getColor(R.color.white));
			tv_receive.setTextColor(getResources().getColor(R.color.white));


		}

	}

	/**
	 * Display the device information, which is connected to Application.
	 *
	**/
	/*private void deviceInfoDisplay()
	{
		deviceInfo.setTextColor((getResources().getColor(R.color.blue2)));
		deviceInfo.setText("Vendor ID :  "
				+ String.format("0x%04X", mSerial.getvendorid())
				+ "\t\tProduct ID :  "
				+ String.format("0x%04X", mSerial.getproductid())
				+ "\t\tDevice Class :  "
				+ String.format("0x%02X", mSerial.getdeviceclass()));

		if (mSerial.getproductid() == 0x1410)
		{
			productDesc.setTextColor((getResources().getColor(R.color.blue1)));
			productDesc.setTextSize(15);
			productDesc
					.setText("Exar\tXR21V1410\t\t\t1-ch Full-Speed USB UART\t>>");

		}
		else if (mSerial.getproductid() == 0x1411)
		{
			productDesc.setTextColor((getResources().getColor(R.color.blue1)));
			productDesc.setTextSize(15);
			productDesc
					.setText("Exar\tXR21B1411\t\t\tEnhanced 1-ch Full-Speed USB UART\t>>");
		}
		else if (mSerial.getproductid() == 0x1412)
		{
			productDesc.setTextColor((getResources().getColor(R.color.blue1)));
			productDesc.setTextSize(15);
			productDesc
					.setText("Exar\tXR21V1412\t\t\tEnhanced 2-ch Full-Speed USB UART\t>>");
		}
		else if (mSerial.getproductid() == 0x1414)
		{
			productDesc.setTextColor((getResources().getColor(R.color.blue1)));
			productDesc.setTextSize(15);
			productDesc
					.setText("Exar\tXR21V1414\t\t\tEnhanced 4-ch Full-Speed USB UART\t>>");
		}
		else if (mSerial.getproductid() == 0x1420)
		{
			productDesc.setTextColor((getResources().getColor(R.color.blue1)));
			productDesc.setTextSize(15);
			productDesc
					.setText("Exar\tXR21B1420\t\t\tEnhanced 1-ch Full-Speed USB UART\t>>");
		}
		else if (mSerial.getproductid() == 0x1422)
		{
			productDesc.setTextColor((getResources().getColor(R.color.blue1)));
			productDesc.setTextSize(15);
			productDesc
					.setText("Exar\tXR21B1422\t\t\tEnhanced 2-ch Full-Speed USB UART\t>>");
		}
		else if (mSerial.getproductid() == 0x1424)
		{
			productDesc.setTextColor((getResources().getColor(R.color.blue1)));
			productDesc.setTextSize(15);
			productDesc
					.setText("Exar\tXR21B1424\t\t\tEnhanced 4-ch Full-Speed USB UART\t>>");
		}
		else if (mSerial.getproductid() == 0x1400)
		{
			productDesc.setTextColor((getResources().getColor(R.color.blue1)));
			productDesc.setTextSize(15);
			productDesc.setText("Exar\tXR22B80x\t\t\tEnhanced Channel No.1 Full-Speed USB UART\t>>");
		}
		else if (mSerial.getproductid() == 0x1401)
		{
			productDesc.setTextColor((getResources().getColor(R.color.blue1)));
			productDesc.setTextSize(15);
			productDesc.setText("Exar\tXR22B80x\t\t\tEnhanced Channel No.2 Full-Speed USB UART\t>>");
		}
		else if (mSerial.getproductid() == 0x1402)
		{
			productDesc.setTextColor((getResources().getColor(R.color.blue1)));
			productDesc.setTextSize(15);
			productDesc.setText("Exar\tXR22B80x\t\t\tEnhanced Channel No.3 Full-Speed USB UART\t>>");
		}
		else if (mSerial.getproductid() == 0x1403)
		{
			productDesc.setTextColor((getResources().getColor(R.color.blue1)));
			productDesc.setTextSize(15);
			productDesc.setText("Exar\tXR22B80x\t\t\tEnhanced Channel No.4 Full-Speed USB UART\t>>");
		}
		else
		{
			productDesc.setTextColor((getResources().getColor(R.color.red)));
			productDesc.setText("No device info found.");
		}
	}

	private void removeDeviceInfo() {
		deviceInfo.setText(null);
		productDesc.setText(null);
	}*/


	/**
	 * Launch the Browser web page containing information about the device.
	 *
	 * @param view
	**/
	/*public void onInfoClick(View view)
	{
		if (productDesc.getText().toString().contains("XR21V1410"))
		{
			startActivity(new Intent(
					Intent.ACTION_VIEW,
					Uri.parse("http://www.exar.com/connectivity/uart-and-bridging-solutions/usb-uarts/xr21v1410")));
		}
		else if (productDesc.getText().toString().contains("XR21B1411"))
		{
			startActivity(new Intent(
					Intent.ACTION_VIEW,
					Uri.parse("http://www.exar.com/connectivity/uart-and-bridging-solutions/usb-uarts/xr21b1411")));
		}
		else
		{
			startActivity(new Intent(
					Intent.ACTION_VIEW,
					Uri.parse("http://www.exar.com/connectivity/uart-and-bridging-solutions/usb-uarts/")));
		}
	}*/


	/**
	 * Method to be called to auto connect to device, if the USB UART is already
	 * connected.
	**/
	private void autoConnect() {

		if (getParameters()) {
			mSerial.setParameters(serialportsel, baudrate, dataBits, stopBits, parityBits,
					flowControl, flagLoopBack);

			StartTimerTask();


			readFlag = true;
			toggleConnect.setChecked(true);
			disableUI();

			StartReceive();
			Toast.makeText(this, R.string.device_connected, Toast.LENGTH_SHORT)
					.show();
		}
	}

	/**
	 * This method gets the setting values provided by the users.
	 *
	**/
	private boolean getParameters() {

		parityBits = 0;

		flowControl = spinFlowControl.getSelectedItemPosition();
		String baudstr = textBaudRate.getText().toString();
		baudrate = default_baudrate;
		if (baudstr.equals("")) {
			Toast.makeText(getApplicationContext(), R.string.baudrate_value,
					Toast.LENGTH_SHORT).show();
			toggleConnect.setChecked(false);
			return false;
		} else {
			baudrate = Integer.parseInt(baudstr);
			if (baudrate < min_baudrate || baudrate > max_baudrate) {
				Toast.makeText(getApplicationContext(),
						R.string.baudrate_value, Toast.LENGTH_SHORT).show();
				toggleConnect.setChecked(false);
				return false;
			}
		}

		stopBits = Integer.parseInt(String.valueOf(spinStopBits
				.getSelectedItem()));
		dataBits = Integer.parseInt(String.valueOf(spinDataBits
				.getSelectedItem()));
		int parityPos = spinParityBits.getSelectedItemPosition();
		if (dataBits != 9) {
			switch (parityPos) {

			case 0:
				parityBits = 0;
				break;
			case 1:
				parityBits = 1;
				break;
			case 2:
				parityBits = 2;
				break;
			case 3:
				parityBits = 3;
				break;
			case 4:
				parityBits = 4;
				break;
			default:
				parityBits = 0;
				break;
			}
		} else {
			parityBits = 0;
			spinParityBits.setSelection(0);
			spinParityBits.setSelected(false);
		}
		serialportsel = spinChannelSel.getSelectedItemPosition();
		return true;
	}

	/**
	 * Method called on Connect button clicked. Connect or Disconnect to device
	 * on Connect clicked.
	 *
	 * @param view
	**/
	public void onBeginClick(View view) {

		if (getParameters()) {

			if (toggleConnect.isChecked()) {
				if (mSerial.begin(serialportsel,null)) {

					autoConnect();
					//deviceInfoDisplay();
					invalidateOptionsMenu();

				} else {
					Toast.makeText(this, R.string.device_connection,
							Toast.LENGTH_SHORT).show();
					invalidateOptionsMenu();
					toggleConnect.setChecked(false);
				}
			} else {
				readFlag = false;
				CancelTimerTask();
				enableUI();
				clearData();
				mSerial.disable_tx_rx(serialportsel);
				mSerial.end();
				invalidateOptionsMenu();
				Toast.makeText(this, R.string.device_disconnected,
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	/**
	 * Clear some view when device get Disconnected.
	 *
	**/
	private void clearData() {
		isContSend = false;
		isSendU = false;
		mText.delete(0, mText.length());
		mTextRight.delete(0, mTextRight.length());
		tvMonitor.setText("");
		tvMonitorRight.setText("");
		//editText.setText(R.string.edit_text);

	}

	/**
	 * Method get called on Send clicked.
	 *
	 *
	**/
	public void onWrite(final byte[] bytes, String text)
	{
		if (bytes == null && bytes.length == 0)
		{
			Toast.makeText(this, "Nothing to send", Toast.LENGTH_SHORT).show();
			return;
		}
		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				// TODO Auto-generated method stub
				//mSerial.write_ext((text + "\n").getBytes(),serialportsel);
                mSerial.write_ext(bytes,serialportsel);
				startResponseTimer();
				try
				{
					Thread.sleep(20);
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		new Thread(r).start();
		datasent.append(byte2String(bytes)+ "\n" +text + "\n");

	}


	/*
         * Handler for Timer Msg
         *
        **/
	private int count;
	String Rcvstrng, Sndstring;
	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case UPDATE_UI:
				if (datasb.length() > 0) {
					Rcvstrng = hexConverterReceive(datasb.toString());
					sb.delete(0, sb.length());
					datasb.delete(0, datasb.length());
					tvMonitorRight.append(Rcvstrng);
					scrollerRight.smoothScrollTo(0, tvMonitorRight.getBottom());
					isScrollBottom = true;
				} else {
					if (isScrollBottom) {
						scrollerRight.smoothScrollTo(0,
								tvMonitorRight.getBottom());
						isScrollBottom = false;
					}
				}

			case UPDATE_SEND_UI:
				if (datasent.length() > 0) {
					Sndstring = hexConverter(datasent.toString());
					st.delete(0, st.length());
					datasent.delete(0, datasent.length());
					tvMonitor.append(Sndstring);
					scroller.smoothScrollTo(0, tvMonitor.getBottom());
					count = 0;
				} else {
					if (count <= 2) {
						scroller.smoothScrollTo(0, tvMonitor.getBottom());
						count++;
					}
				}
				break;

			default:
				break;
			}
		}

	};


	/**
	 * Handler to pass the Continuous read message from another thread.
	 *
	**/
	Handler handler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msgRW) {
			super.handleMessage(msgRW);
			switch (msgRW.what) {
			case HANDLE_READ:
				if(mSerial.get_port_status() == 1)
				{
				  datasb.append((String) msgRW.obj).append("\n");
				}
				else
				{
				  Log.e(TAG, "Skip the data message2");
				}
				break;

			case UPDATE_SEND:
				datasent.append((String) msgRW.obj);
				break;

			default:
				break;
			}
		}
	};

	/**
	 * This method starts another thread to continuous read the data from the
	 * UART.
	**/
	public void StartReceive() {
		receiveThread = new Thread(new Runnable() {
			Message msgRead;

			@Override
			public void run() {
				byte[] rbuf = new byte[BUFF_SIZE];

				while (readFlag) {
					try {
						int len = mSerial.read(rbuf,serialportsel);

						if (len > 0)
						{
							Log.e(TAG, "rbuf.size::" + len);
							if(spinChannelSel.getSelectedItemPosition() == serialportsel)
							{
							  if(mSerial.get_port_status() == 1)
							  {
								if(rbuf[0] == 0xa5 && rbuf[1] == 0x5a){
									//todo file
								}else{
									msgRead = handler.obtainMessage(HANDLE_READ,byte2String(rbuf));
									handler.sendMessage(msgRead);
									String[] strings =  CommandsUtil.compareBytes(rbuf);
									if(strings[1].equals("1")){
										cancelResponseTimerTask();
									}
									msgRead = handler.obtainMessage(HANDLE_READ, strings[0]);
									handler.sendMessage(msgRead);
								}

							  }
							  else
							  {
								  Log.e(TAG, "Skip the data message1");
							  }
							}
							else
							{
								Log.e(TAG, "spinChannelSel.getSelectedItemPosition() != serialportsel ");
							}
						}
					} catch (NullPointerException e1) {
						e1.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (!readFlag) {
					return;
				}
			}
		});
		receiveThread.start();
	}

	private String byte2String(byte[] bytes){

		if (bytes.length != 6 && (bytes[0] ^ bytes[1] ^ bytes[2] ^ bytes[3] ^ bytes[4]) != bytes[5])
			return "unknown command";
		StringBuffer stringBuffer = new StringBuffer();
		for(byte b : bytes){
			stringBuffer.append("0X");
			stringBuffer.append(Integer.toHexString(b));
			stringBuffer.append(" ");
		}
		return stringBuffer.toString();
	}
	/**
	 * Stop the timer thread.
	 *
        **/
	private void CancelTimerTask() {
		timer.purge();
		timer.cancel();
		timer = null;
		datasent.delete(0, datasent.length());
		datasb.delete(0, datasb.length());
	}


	private void cancelResponseTimerTask() {
		if(responseTimer != null) {
			responseTimer.purge();
			responseTimer.cancel();
			responseTimer = null;
			isResponed = true;
		}

	}

	private volatile boolean isResponed = true;

	private Timer responseTimer;
	private void startResponseTimer(){
		responseTimer = new Timer();
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				isResponed = true;
			}
		};
		timer.schedule(timerTask, 10 * 1000);
	}

	/**
	 * Start the timer thread.
	 *
        **/
	private void StartTimerTask() {
		timer = new Timer();
		TimerTask timerTask = new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				mHandler.sendEmptyMessage(UPDATE_SEND_UI);
				mHandler.sendEmptyMessage(UPDATE_UI);

			}
		};
		timer.schedule(timerTask, TIMER_DELAY, TIMER_PERIOD);

	}

	/**
	 * This method starts another thread to continuous send the data "U" to the
	 * UART.
	**/
	public void ContSendU() {

		contsendUThread = new Thread(new Runnable() {

			Message msgUsent;
			final String UString = "U";

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (isSendU) {
					try {
						synchronized (this) {
							mSerial.write_ext(UString.getBytes(),serialportsel);
						}
						msgUsent = handler
								.obtainMessage(UPDATE_SEND, UString);
						handler.sendMessage(msgUsent);
						//mSerial.write(youString.getBytes());
						Thread.sleep(60);
					} catch (NullPointerException e1) {
						e1.printStackTrace();
					}catch(InterruptedException e2){
						e2.printStackTrace();
					}catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (!isSendU) {
					//For printing a new line after the last "U"
					if(readFlag) {
						mSerial.write_ext("\n".getBytes(),serialportsel);
						msgUsent = handler
								.obtainMessage(UPDATE_SEND, "\n");
						handler.sendMessage(msgUsent);
					}
					return;
				}
			}

		});
		contsendUThread.start();
	}

	/**
	 * This method starts another thread to continuous send the data to the UART.
         *
	**/
	public void ContSend() {
		final String str;
		str = "" ;//editText.getText().toString();
		if (str.equals("")) {
			Toast.makeText(this, "Nothing to send", Toast.LENGTH_SHORT).show();
			return;
		}

		contsendThread = new Thread(new Runnable() {
			Message msgSent;
			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (isContSend) {
					try {
						synchronized (this) {
							mSerial.write_ext((str + "\n").getBytes(),serialportsel);
						}
						msgSent = handler
								.obtainMessage(UPDATE_SEND, str + "\n");
						handler.sendMessage(msgSent);
						Thread.sleep(150);
					} catch (NullPointerException e1) {
						e1.printStackTrace();
					} catch (InterruptedException e2) {
						e2.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (!isContSend) {
					return;
				}
			}

		});
		contsendThread.start();
	}

	/**
	 * Method to show a confirmation dialog on back button pressed.
	 *
	**/
	private void exitDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(
				MainActivity2.this);

		builder.setPositiveButton(R.string.dialog_confirm,
				new OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						if (toggleConnect.isChecked()) {
							Toast.makeText(getApplicationContext(),
									R.string.device_disconnected,
									Toast.LENGTH_SHORT).show();
						}
						finish();
					}
				});

		builder.setNegativeButton(R.string.dialog_cancel,
				new OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		builder.setTitle(R.string.dialog_title);
		builder.setMessage(R.string.dialog_desc);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.show();
	}

	/**
	 * Broadcast Receiver implementation to get the action of device attached
	 * and detached.
	**/
	BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				UsbDevice device = (UsbDevice) intent
						.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				if (device.getVendorId() == 1250)
				{
	 //Enable this code for getting the device info displayed as soon as device is
	 //connected and before the system pop-up is shown
					if((device.getProductId() == 0x0802) ||
					   (device.getProductId() == 0x1100) ||
					   (device.getProductId() == 0x1200) ||
					   (device.getProductId() == 0x1300))
					{
						Log.e(TAG, "Ignore Vendor ID: "
	                            + String.format("0x%04X", device.getVendorId())
	                            + "\t\t Ignore Product ID: "
	                            + String.format("0x%04X", device.getProductId())
	                            + "\t\t Ignore Device Class: "
	                            + String.format("0x%02X", device.getDeviceClass()));
						return;
					}

					serialportsel = spinChannelSel.getSelectedItemPosition();
					if((device.getProductId() == 0x1400)||
					   (device.getProductId() == 0x1401)||
					   (device.getProductId() == 0x1402)||
					   (device.getProductId() == 0x1403))
					{
						if((device.getProductId() != 0x1400 + serialportsel))
						{
							Log.e(TAG, "Ignore Vendor ID: "
		                            + String.format("0x%04X", device.getVendorId())
		                            + "\t\t Ignore Product ID: "
		                            + String.format("0x%04X", device.getProductId())
		                            + "\t\t Ignore Device Class: "
		                            + String.format("0x%02X", device.getDeviceClass()));
							return;
						}
					}
					Log.e(TAG, "Vendor ID: "
                            + String.format("0x%04X", device.getVendorId())
                            + "\t\tProduct ID: "
                            + String.format("0x%04X", device.getProductId())
                            + "\t\tDevice Class: "
                            + String.format("0x%02X", device.getDeviceClass()));

					/*
					deviceInfo.setText("Vendor ID: "
                            + String.format("0x%04X", device.getVendorId())
                            + "\t\tProduct ID: "
                            + String.format("0x%04X", device.getProductId())
                            + "\t\tDevice Class: "
                            + String.format("0x%02X", device.getDeviceClass()));

					if (device.getProductId() == 0x1410)
					{
						productDesc.setText("Exar\tXR21V1410\t\t1-ch Full-Speed USB UART\t>>");

					} else if (device.getProductId() == 0x1411)
					{
						productDesc.setText("Exar\tXR21B1411\t\tEnhanced 1-ch Full-Speed USB UART\t>>");
					}
					else
					{
						productDesc.setText("Device Information Not found");
					}*/
					//SystemClock.sleep(1000);
			    	if (mSerial.begin(serialportsel,device))
					{
						autoConnect();
						//deviceInfoDisplay();
						invalidateOptionsMenu();

					}
			    	else
			    	{
			    		//spinChannelSel.setSelection(0);
			    		spinChannelSel.setEnabled(false);

			    	}
				}

			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				UsbDevice device = (UsbDevice) intent
						.getParcelableExtra(UsbManager.EXTRA_DEVICE);

				if (device != null) {
					if (readFlag) {
						readFlag = false;
						//removeDeviceInfo();

						toggleConnect.setChecked(false);
						enableUI();
						clearData();
						mSerial.end();
						CancelTimerTask();
						Toast.makeText(getApplicationContext(),
								R.string.device_disconnected,
								Toast.LENGTH_SHORT).show();
						invalidateOptionsMenu();
					} else {
						//removeDeviceInfo();
						Toast.makeText(getApplicationContext(),
								R.string.device_removed, Toast.LENGTH_LONG)
								.show();
					}
				}

			}

		}
	};

	/**
	 * This method handles the keyboard hide functionality.
	 *
	 * @param view
	**/
	public static void hideSoftKeyboard(Activity activity) {
		InputMethodManager inputMethodManager = (InputMethodManager) activity
				.getSystemService(Activity.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus()
				.getWindowToken(), 0);
	}

	/**
	 * This method getting the touch event, to hide the soft keyboard.
	 *
	 * @param view
	**/
	public void setupUI(View view) {
		// Set up touch listener for non-text box views to hide keyboard.
		if (!(view instanceof EditText)) {

			view.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					try {
						hideSoftKeyboard(MainActivity2.this);
					} catch (Exception e) {
					}
					return false;
				}
			});
		}
		// If a layout container, iterate over children and seed recursion.
		if (view instanceof ViewGroup) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				View innerView = ((ViewGroup) view).getChildAt(i);
				setupUI(innerView);
			}
		}
	}

	/**
	 * This method creates a dialog when user click on Register R/W in Options
	 * Menu.
	**/
	public void RegisterRWAlertDialog() {

		final AlertDialog.Builder regDialog = new AlertDialog.Builder(this);

		final LayoutInflater inflater = (LayoutInflater) this
				.getSystemService(LAYOUT_INFLATER_SERVICE);

		final View Viewlayout = inflater.inflate(R.layout.reg_dialog,
				(ViewGroup) findViewById(R.id.layout_dialog));

		radioButtonWrite = (RadioButton) Viewlayout
				.findViewById(R.id.rb_regwrite);
		radioButtonRead = (RadioButton) Viewlayout
				.findViewById(R.id.rb_regRead);
		edit_regOffset = (EditText) Viewlayout.findViewById(R.id.edit_regaddr);
		edit_regValue = (EditText) Viewlayout.findViewById(R.id.edit_regvalue);
		tview_regValue = (TextView) Viewlayout.findViewById(R.id.tv_regValue);
		btn_regRW = (Button) Viewlayout.findViewById(R.id.btn_reg_rw);
		btn_regRW.setText(R.string.label_rb_regwrite);

		regDialog.setTitle(R.string.regdialog_title);
		regDialog.setView(Viewlayout);
		regDialog.setCancelable(true);

		regDialog.setPositiveButton("Done",
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
					}
				});

		regDialog.setNegativeButton("Cancel",
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
		regDialog.create();
		regDialog.show();
	}

	/**
	 * Handles Read/Write radio button click
	 * 
	**/
	public void onRadioButtonClicked(View view) {

		boolean checked = ((RadioButton) view).isChecked();
		// Check which radio button was clicked
		switch (view.getId()) {
		case R.id.rb_regwrite:
			if (checked) {
				tview_regValue.setVisibility(View.INVISIBLE);
				edit_regValue.setVisibility(View.VISIBLE);
				btn_regRW.setEnabled(true);
				btn_regRW.setText("Write");
				edit_regValue.setEnabled(true);
				edit_regOffset.setEnabled(true);
			}
			break;
		case R.id.rb_regRead:
			if (checked) {
				edit_regValue.setVisibility(View.INVISIBLE);
				tview_regValue.setVisibility(View.VISIBLE);
				btn_regRW.setText("Read");
				btn_regRW.setEnabled(true);
				edit_regOffset.setEnabled(true);
				edit_regValue.setEnabled(false);
				edit_regValue.setText("");
			}
			break;
		default:
			btn_regRW.setEnabled(false);
			btn_regRW.setText("Select");
			edit_regOffset.setEnabled(false);
			edit_regValue.setEnabled(false);
			edit_regOffset.setText("");
			edit_regValue.setText("");
		}
	}

	/**
	 * Register read write method implementation
	 * 
	 * @param view
	 */
	public void onRegRWClicked(View view) {
		String regOffset, regValue;
		int result, registrevalue;
		if (toggleConnect.isChecked()) {
			if (radioButtonWrite.isChecked()) {
				regOffset = edit_regOffset.getText().toString();
				regValue = edit_regValue.getText().toString();

				if (regOffset.equals("")) {
					Toast.makeText(getApplicationContext(),
							R.string.register_address, Toast.LENGTH_SHORT)
							.show();
				} else if (regValue.equals("")) {
					Toast.makeText(getApplicationContext(),
							R.string.register_value, Toast.LENGTH_SHORT).show();
				} else {
					registrevalue = Integer.parseInt(regValue, 16);
					if (regOffset.equals("C16")) {
						if (registrevalue == 1) {
							cb_loopback.setChecked(true);
							flagLoopBack = true;
							// Toast.makeText(this, R.string.loopback_enabled,
							// Toast.LENGTH_SHORT).show();
						} else {
							// Toast.makeText(this, R.string.loopback_disabled,
							// Toast.LENGTH_SHORT).show();
							cb_loopback.setChecked(false);
							flagLoopBack = false;
						}
					}
					result = mSerial.setRegisterValue(serialportsel,regOffset, registrevalue);
					if (result == 0) {
						Toast.makeText(getApplicationContext(),
								R.string.register_write, Toast.LENGTH_SHORT)
								.show();
					} else if (result == -1) {
						Toast.makeText(getApplicationContext(),
								R.string.register_write_error,
								Toast.LENGTH_SHORT).show();
					}
				}
			} else if (radioButtonRead.isChecked()) {
				regOffset = edit_regOffset.getText().toString();
				if (regOffset.equals("")) {
					Toast.makeText(getApplicationContext(),
							R.string.register_address, Toast.LENGTH_LONG)
							.show();
				} else {
					regValue = mSerial.getRegisterValue(serialportsel,regOffset);
					tview_regValue.setText(regValue);
					tview_regValue.setTextColor(getResources().getColor(R.color.blue));
					
				}

			} else {

			}
		} else {
			Toast.makeText(getApplicationContext(), R.string.device_connection,
					Toast.LENGTH_SHORT).show();
		}

	}

	/**
	 * Method for send file file selection
	 * 
	**/
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CODE:
			// If the file selection was successful
			if (resultCode == RESULT_OK) {
				if (data != null && data.getExtras().get("file") != null) {
					mSelectedFile = (File) data.getExtras().get("file");
					if (toggleConnect.isChecked()) {
						SendFile send = new SendFile();
						send.execute();
					} else {
						Toast.makeText(getApplicationContext(),
								R.string.device_connection, Toast.LENGTH_SHORT)
								.show();
					}
				}
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * This method handles check-box related UI activity
	 * 
	**/
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		switch (buttonView.getId()) {

		case R.id.checkbox_send_u:
			if (isChecked) {
				isSendU = true;
				//cb_send_u.setTextColor(getResources().getColor(R.color.lime));
				//cb_continuous_send.setEnabled(false);
				//cb_continuous_send.setTextColor(getResources().getColor(R.color.gray));
				//btnWrite.setEnabled(false);
				//editText.setEnabled(false);
				ContSendU();
			} else {
				//cb_send_u.setTextColor(getResources().getColor(R.color.white));
				//btnWrite.setEnabled(true);
				//.setEnabled(true);
				//cb_continuous_send.setEnabled(true);
				//cb_continuous_send.setTextColor(getResources().getColor(R.color.white));
				isSendU = false;
			}
			break;
		case R.id.checkbox_continuous_send:
			if (isChecked) {
				//btnWrite.setEnabled(false);
				isContSend = true;
				//cb_continuous_send.setTextColor(getResources().getColor(R.color.lime));
				//cb_send_u.setEnabled(false);
				//cb_send_u.setTextColor(getResources().getColor(R.color.gray));
				//editText.setEnabled(false);
				ContSend();
				
			} else {
				isContSend = false;
				//cb_continuous_send.setTextColor(getResources().getColor(R.color.white));
				//cb_send_u.setEnabled(true);
				//cb_send_u.setTextColor(getResources().getColor(R.color.white));
				//editText.setEnabled(true);
				//btnWrite.setEnabled(true);
			}
			break;
		case R.id.hex_send:
			if (isChecked) {
				isHexSend = true;
				cb_hexsend.setTextColor(getResources().getColor(R.color.lime));
			} else {
				isHexSend = false;
				cb_hexsend.setTextColor(getResources().getColor(R.color.white));
			}
			break;
		case R.id.hex_receive:
			if (isChecked) {
				isHexReceive = true;
				cb_hexreceive.setTextColor(getResources().getColor(R.color.lime));
			} else {
				isHexReceive = false;
				cb_hexreceive.setTextColor(getResources().getColor(R.color.white));
			}
			break;
		case R.id.checkbox_loopback:
			if (isChecked) {
				flagLoopBack = true;
				cb_loopback.setTextColor(getResources().getColor(R.color.lime));
			} else {
				flagLoopBack = false;
				cb_loopback.setTextColor(getResources().getColor(R.color.white));
			}

		}

	}


	/**
	 * This function takes string as parameter and returns string of HEX values
	 * of the input.
	 * 
	 * @param str
	 * @return string
	**/
	public String hexConverter(String str) {

		if (isHexSend) {
			final char[] chars = str.toCharArray();
			for (int i = 0; i < chars.length; i++) {
				if(String.valueOf(Integer.toHexString((int) chars[i])).equals("a")){
					st.append("\n");
				} else {
					st.append(String.valueOf(Integer.toHexString((int) chars[i])));
				}
				st.append(" ");				
			}
			return st.toString();			
		} else {
			st.append(str);
			return st.toString();
		
		}

	}

	/**
	 * This function takes string as parameter and returns string of HEX values
	 * of the input.
	 * 
	 * @param str
	 * @return string
	**/
	public String hexConverterReceive(String str) {
		final char[] chars = str.toCharArray();
		if(isHexReceive) {
			for (int i = 0; i < chars.length; i++) {
				if(String.valueOf(Integer.toHexString((int) chars[i])).equals("a")){
					sb.append("\n");

				}else{
					sb.append(String.valueOf(Integer.toHexString((int) chars[i])));
				}
				sb.append(" ");
			}
			return sb.toString();
		} else {
			sb.append(str);			
			return sb.toString();
		}

	}

	@Override
	public void onClick(View view) {
		if (!isResponed){
			Toast.makeText(this, "waiting for fmi response",Toast.LENGTH_SHORT).show();
			return;
		}
		isResponed = false;
	    String text = null;
	    byte[] command = new byte[6];
        int vID = view.getId();
        switch (vID){
            case R.id.btn_register:
                command = CommandsUtil.COMMAND_REGISTER_USER;
                text = "request register user";
                break;
            case R.id.btn_capture:
                command = CommandsUtil.COMMAND_CAPTURE_JPEG;
                text = "request capture jpeg";
                break;
            case R.id.btn_checkVersion:
                command = CommandsUtil.COMMADN_FMI_CHECH_VERSION;
                text = "request check fmi version";
                break;
            case R.id.btn_clear_record:
                command = CommandsUtil.COMMAND_CLEAR_RECORD;
                text = "request delete all users";
                break;
            case R.id.btn_close_port:
                break;
            case R.id.btn_reset_preview:
                break;
            case R.id.btn_delete_user:
                command = getDeleteUserCommand();
                if (command.length == 0){
                    return;
                }
                text = "request delete user";
                break;
            case R.id.cb_face_on:
                command = CommandsUtil.COMMADN_FACE_ENABLE;
                text = "request start face recognition";
                cb_face_enable.setChecked(true);
                cb_face_disable.setChecked(false);
                break;
            case R.id.cb_face_off:
                command = CommandsUtil.COMMADN_FACE_DISABLE;
                text = "request stop  face recognition";
                cb_face_enable.setChecked(false);
                cb_face_disable.setChecked(true);
                break;
            case R.id.cb_led_off:
                command = CommandsUtil.COMMADN_LED_OFF;
                text = "request led off";
                cb_led_off.setChecked(true);
                cb_led_level_1.setChecked(false);
                cb_led_level_2.setChecked(false);
                cb_led_level_3.setChecked(false);
                break;
            case R.id.cb_led_level_1:
                command = CommandsUtil.COMMADN_LED_LEVEL_O1;
                text = "request led level 1";
                cb_led_off.setChecked(false);
                cb_led_level_1.setChecked(true);
                cb_led_level_2.setChecked(false);
                cb_led_level_3.setChecked(false);
                break;
            case R.id.cb_led_level_2:
                command = CommandsUtil.COMMADN_LED_LEVEL_O2;
                text = "request led level 2";
                cb_led_off.setChecked(false);
                cb_led_level_1.setChecked(false);
                cb_led_level_2.setChecked(true);
                cb_led_level_3.setChecked(false);
                break;
            case R.id.cb_led_level_3:
                command = CommandsUtil.COMMADN_LED_LEVEL_O3;
                text = "request led level 3";
                cb_led_off.setChecked(false);
                cb_led_level_1.setChecked(false);
                cb_led_level_2.setChecked(false);
                cb_led_level_3.setChecked(true);
                break;
            default:
                text = "no command matched";
                break;

        }
        command[5] = (byte) (command[0]^command[1]^command[2]^command[3]^command[4]);
        onWrite(command,text);
	}

	private byte[] getDeleteUserCommand(){
	    String text = et_userId.getText().toString().trim();
	    if (text != null && text.length() != 0){
	        try {
                Integer integer = Integer.valueOf(text);
                if(integer < 0 || integer > 999){
					Toast.makeText(this,"UserId should in 0 ~ 999",Toast.LENGTH_SHORT).show();
                	return new byte[0];
				}
                byte[] del_command = {0x02,0x08,0x00,0x00,0x00,0x0A};
                del_command[2] = (byte) (integer & 0x7F);
                del_command[3] = (byte) (integer >> 7);
                return del_command;
            }catch (NumberFormatException e){
                Toast.makeText(this,"UserId must to be a number",Toast.LENGTH_SHORT).show();
            }
        }else {
			Toast.makeText(this,"UserId is empty",Toast.LENGTH_SHORT).show();
		}
        return new byte[0];
    }

	/**
	 * The class SendFile is an inner class of MainActivity class. This class
	 * implements sending file feature.
	 * 
	 * @author GRACELABS
	 * 
	**/
	class SendFile extends AsyncTask<String, Void, String> {

		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(MainActivity2.this);
			progressDialog.setTitle(R.string.progress_sendfile_title);
			progressDialog.setMessage(getResources().getString(
					R.string.progress_sendfile_msg));
			progressDialog.setCancelable(false);
			progressDialog.setIndeterminate(true);
			progressDialog.show();
		}

		@Override
		protected String doInBackground(String... params) {
//			Message msgFileSent;
			try {
				String fileContent = "";
				if (mSelectedFile != null)
					try {
						BufferedReader read = new BufferedReader(
								new InputStreamReader(new FileInputStream(
										mSelectedFile)));
						String line = "";
						while ((line = read.readLine()) != null) {
							fileContent = line + "\n";
							mSerial.write_ext(fileContent.getBytes(),serialportsel);
							//Enable this For displaying file contents in Sent Box
//							msgFileSent = handler
//									.obtainMessage(UPDATE_SEND, fileContent);
//							handler.sendMessage(msgFileSent);
//							Thread.sleep(40);
						}
						read.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (NullPointerException e1) {
						e1.printStackTrace();
//					} catch (InterruptedException e2) {
//						e2.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
			} catch (Exception e) {
				Log.e("FileSelectorActivity", "File select error", e);
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			progressDialog.dismiss();
			Toast.makeText(MainActivity2.this,
					"File sent: " + mSelectedFile.getAbsolutePath(),
					Toast.LENGTH_LONG).show();
		}

	}

}

