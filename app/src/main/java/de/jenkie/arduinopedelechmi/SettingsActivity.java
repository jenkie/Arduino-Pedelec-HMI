package de.jenkie.arduinopedelechmi;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Switch;
import android.preference.PreferenceManager;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * Created by jenkie on 23.06.13.
 */
public class SettingsActivity extends Activity {
    //public static String DEVICE_ADSRESS = "device_address";
    private BluetoothAdapter mBluetoothAdapter = null;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private Intent intent;
    private int capacity = 200;
    private String device_id = "";
    private boolean logging = false;
    private EditText et_capacity;
    private ToggleButton sw_log;
    private ListView pairedListView;

    SharedPreferences prefs = null;

    // Return Intent extra
    //public static String EXTRA_DEVICE_ADDRESS = "device_address";
    //public static String EXTRA_CAPACITY = "capacity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(Activity.RESULT_CANCELED);

        setContentView(R.layout.settings);

        //set up the ListView for paired devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        pairedListView = (ListView) findViewById(R.id.lv_paired);
        et_capacity = (EditText) findViewById(R.id.et_capacity);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);
        sw_log = (ToggleButton) findViewById(R.id.sw_log);
        intent = new Intent();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);


        //capacity
        capacity = prefs.getInt("capacity", 200);
        et_capacity.setText(Integer.toString(capacity));
        device_id = prefs.getString("device_id", "");
        logging = prefs.getBoolean("logging", false);
        sw_log.setChecked(logging);


    }

    @Override
    public void onStart() {
        super.onStart();
        listDevices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    //lists paired devices
    private void listDevices() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        mPairedDevicesArrayAdapter.clear();
        if (pairedDevices.size() > 0) {
            // Loop through paired devices

            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in the ListView
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }

    }

    // The on-click listener for all devices in the ListViews, also ends this activity
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            device_id = info.substring(info.length() - 17);
            v.setSelected(true);
        }
    };

    public void bt_save_click(View v) {
        // Set result and finish this Activity
        capacity = Integer.parseInt(et_capacity.getText().toString());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("device_id", device_id);
        editor.putInt("capacity", capacity);
        editor.putBoolean("logging", sw_log.isChecked());
        editor.commit();
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

}

