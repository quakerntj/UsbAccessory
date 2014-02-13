package ch.serverbox.android.usbtest;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class UsbTest extends Activity {
	private UsbAccessory mAccessory = null;
	private Button mBtSend = null;
	private FileOutputStream mFout = null;
	private PendingIntent mPermissionIntent = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        IntentFilter i = new IntentFilter();
        i.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        i.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        i.addAction("ch.serverbox.android.usbtest.USBPERMISSION");
        registerReceiver(mUsbReceiver,i);
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);

        mBtSend = (Button)(findViewById(R.id.btSebd));
        mBtSend.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String s = ((EditText)findViewById(R.id.editText1)).getText().toString();
				queueWrite(s);
			}
		});
		mBtSend.setEnabled(false);

        if (getIntent().getAction().equals("android.hardware.usb.action.USB_ACCESSORY_ATTACHED")){
        	Log.d("USB", "Action is usb");
        	UsbAccessory accessory = getAccessory(getIntent());
        	mAccessory = accessory;
        	FileDescriptor fd = null;
        	try {
        		fd = manager.openAccessory(accessory).getFileDescriptor();
        	} catch (IllegalArgumentException e){
        		finish();
        	} catch (NullPointerException e){
        		finish();
        	}
        	mFout = new FileOutputStream(fd);
        	mBtSend.setEnabled(true);
        } else {
        	UsbAccessory[] accessories = manager.getAccessoryList();
        	if (accessories != null) {
            	for (UsbAccessory a : accessories) {
            		l("accessory: "+a.getManufacturer());
            		if (a.getManufacturer().equals("Nexus-Computing GmbH")) {
            			mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent("ch.serverbox.android.usbtest.USBPERMISSION"),0);
            			manager.requestPermission(a,mPermissionIntent);
            			Log.d("USB", "permission requested");
            			break;
            		}
            	}
            	mBtSend.setEnabled(true);
        	}
        }
    }
    
    @Override
    protected void onDestroy() {
    	unregisterReceiver(mUsbReceiver);
    	super.onDestroy();
    }
    
    public void queueWrite(final String data){
    	if(mAccessory == null){
    		return;
    	}
    	new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Log.d("USB", "Writing length "+data.length());
					mFout.write(String.valueOf(data.length()).getBytes());
					Log.d("USB", "Writing data: "+data);
					mFout.write(data.getBytes());
					Log.d("USB","Done writing");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
    }
    
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
			if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
					UsbAccessory accessory = getAccessory(intent);
					Log.d("USB","Attached!");
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						mAccessory = accessory;
			        	FileDescriptor fd = null;
			        	try {
			        		fd = manager.openAccessory(accessory).getFileDescriptor();
			        	} catch (IllegalArgumentException e){
			        		finish();
			        	} catch (NullPointerException e){
			        		finish();
			        	}
			        	mFout = new FileOutputStream(fd);
			        	mBtSend.setEnabled(true);
					} else {
						Log.d("USB", "permission denied for accessory "
								+ accessory);
					}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = getAccessory(intent);
				if (accessory != null && accessory.equals(mAccessory)) {
					if(mFout != null)
						try {
							mFout.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					mAccessory = null;
					mBtSend.setEnabled(false);
				}
			} else if ("ch.serverbox.android.usbtest.USBPERMISSION".equals(action)) {
				l("permission answered");
				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
		        	UsbAccessory[] accessories = manager.getAccessoryList();
		        	for (UsbAccessory a : accessories){
		        		l("accessory: "+a.getManufacturer());
		        		if (a.getManufacturer().equals("Nexus-Computing GmbH")) {
		        			mAccessory = a;
		                	FileDescriptor fd = null;
		                	try {
		                		fd = manager.openAccessory(a).getFileDescriptor();
		                	} catch (IllegalArgumentException e){
		                		finish();
		                	} catch (NullPointerException e){
		                		finish();
		                	}
		                	mFout = new FileOutputStream(fd);
		        			l("added accessory");
		        			break;
		        		}
		        	}
				}
			}
		}
	};
	
	private void l(String l){
		Log.d("USB", l);
	}

	private static UsbAccessory getAccessory(Intent intent) {
        UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        return accessory;
    }
}
