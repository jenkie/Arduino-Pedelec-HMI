package de.jenkie.arduinopedelechmi;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.StringTokenizer;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import java.io.File;
import android.os.Environment;
import java.util.Date;
import java.util.UUID;
import java.text.SimpleDateFormat;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;


public class FullscreenActivity extends Activity {
    private TextView tv_battery, tv_power, tv_power_human, tv_wh, tv_wh_human, tv_speed, tv_km, tv_support, tv_whkm, tv_range;
    private ProgressBar pg_battery;
    //private Button bt_support_more,bt_support_less;

    private int capacity = 200;
    private int support = 0;
    private int support_stepsize = 25;
    private boolean logging = false;
    private boolean createdlogfile = false;

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_SELECT_DEVICE = 2;

    BluetoothAdapter mBluetoothAdapter = null;
    BluetoothSocket socket = null;
    InputStream mmInStream = null;
    OutputStream mmOutStream = null;

    SharedPreferences prefs = null;

    private MenuItem menuItem;
    private File outFile;
    private FileOutputStream fOut;
    private OutputStreamWriter osw;
    SimpleDateFormat dateFormatShort = new SimpleDateFormat("HH:mm:ss");

    private static final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        tv_battery = (TextView) findViewById(R.id.tv_battery);
        tv_power = (TextView) findViewById(R.id.tv_power);
        tv_power_human = (TextView) findViewById(R.id.tv_power_human);
        tv_wh = (TextView) findViewById(R.id.tv_wh);
        tv_wh_human = (TextView) findViewById(R.id.tv_wh_human);
        tv_speed = (TextView) findViewById(R.id.tv_speed);
        tv_km = (TextView) findViewById(R.id.tv_km);
        tv_support = (TextView) findViewById(R.id.tv_support);
        tv_whkm = (TextView) findViewById(R.id.tv_whkm);
        tv_range = (TextView) findViewById(R.id.tv_range);
        pg_battery = (ProgressBar) findViewById(R.id.progressbar_battery);
        pg_battery.setMax(100);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent settings = new Intent(this, SettingsActivity.class);
                startActivityForResult(settings, REQUEST_SELECT_DEVICE);
                break;
            default:
                break;
        }
        return true;
    }


    @Override
    public void onStart() {
        super.onStart();
        //if BT is not on, request that it be enabled.
        //connection will then be made during onActivityResult
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            }
            if (socket == null)
                login();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        close();
    }


    private void close() {
        try {
            if (socket != null) socket.close();
            Thread.sleep(2000);
        } catch (Exception e) {
        }

    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SELECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    capacity = prefs.getInt("capacity", 200);
                    logging = prefs.getBoolean("logging", false);
                    login();
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                } else {
                    // User did not enable Bluetooth or an error occured
                    Toast.makeText(this, "Can't work without Bluetooth - bye", Toast.LENGTH_LONG).show();
                    finish();
                }
        }
    }

    private void login() {
        try {
            String deviceId = prefs.getString("device_id", ""); //load device address from preferences
            if ("".equals(deviceId)) {
                Intent serverIntent = new Intent(this, SettingsActivity.class);
                startActivityForResult(serverIntent, REQUEST_SELECT_DEVICE);
                return;
            }
            if (socket != null) {
                close();
            }
            BluetoothDevice mmDevice = mBluetoothAdapter.getRemoteDevice(deviceId);
            Method m = mmDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
            socket = (BluetoothSocket) m.invoke(mmDevice, Integer.valueOf(1));
            mBluetoothAdapter.cancelDiscovery();
            socket.connect();
            mmInStream = socket.getInputStream();
            mmOutStream = socket.getOutputStream();
            final InputStream inStream = mmInStream;
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
            //here comes the thread updating the user interface with the received data from the APC
            new Thread(new Runnable() {

                public void run() {
                    try {
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            try {
                                final String fline = line;
                                FullscreenActivity.this.runOnUiThread(new Runnable() {
                                    public void run() {
                                        updateUI(fline); //process the line and update the user interface
                                    }
                                });

                            } catch (Exception e) {
                                //processing line failed
                            }


                        }
                    } catch (Exception e) {
                        //error in reading thread
                    } finally {

                        FullscreenActivity.this.runOnUiThread(new Runnable() {

                            public void run() {
                                //textView.setText(R.string.title_not_connected);
                            }
                        });

                        try {
                            socket.close();
                        } catch (Exception e) {
                        }
                    }
                }

            }).start();

            Thread.sleep(500);

            //textView.setText(R.string.title_connected_to);
            // MK

        } catch (Exception e) {
        } finally {
        }

    }

    private void updateUI(String line) {
        float whkm = 99;
        float range = 0;

        StringTokenizer T = new StringTokenizer(line, ";");
        if (T.countTokens() >= 11) {
            float voltage = (float) Float.parseFloat(T.nextToken());
            float current = (float) Float.parseFloat(T.nextToken());
            int power = (int) Integer.parseInt(T.nextToken());
            float speed = (float) Float.parseFloat(T.nextToken());
            float km = (float) Float.parseFloat(T.nextToken());
            int cadence = (int) Integer.parseInt(T.nextToken());
            int wh = (int) Integer.parseInt(T.nextToken());
            int power_human = (int) Integer.parseInt(T.nextToken());
            int wh_human = (int) Integer.parseInt(T.nextToken());
            support = (int) Integer.parseInt(T.nextToken());
            int controlmode = (int) Integer.parseInt(T.nextToken());
            int battery_percent = (int) ((capacity - wh) / (float) capacity * 100);

            if (wh > 0) {
                range = capacity / wh * km - km;
            }
            if (km > 0) {
                whkm = wh / km;
            }
            tv_battery.setText(battery_percent + " %" + "\n" + voltage + " V");
            tv_power.setText(power + "");
            tv_speed.setText(speed + "");
            tv_km.setText((int) km + "");
            tv_wh.setText(wh + "");
            tv_power_human.setText(power_human + "");
            tv_wh_human.setText(wh_human + "");
            tv_whkm.setText(String.format("%.1f", whkm));
            tv_range.setText((int) range + "");
            pg_battery.setProgress(battery_percent);

            switch (controlmode) {
                case 0:
                    tv_support.setText(support + " W");
                    break;
                case 1:
                    tv_support.setText(support + " wh/km");
                    break;
                case 2:
                    tv_support.setText(support + " %");
                    break;
            }
        }

        if (logging)
            dolog(line);
    }

    public void bt_support_more_click(View v) {
        try {
            if (socket != null) {
                support = ((int) Math.round((double) support / support_stepsize) + 1) * support_stepsize;
                mmOutStream.write(("ps" + support + "\r").getBytes());
            }
        } catch (Exception e) {
        }
    }

    public void bt_support_less_click(View v) {
        try {
            if (socket != null) {
                support = ((int) Math.round((double) support / support_stepsize) - 1) * support_stepsize;
                if (support < 0)
                    support = 0;
                mmOutStream.write(("ps" + support + "\r").getBytes());
            }
        } catch (Exception e) {
        }
    }

    private void dolog(String logstring) {
        if (!createdlogfile) {
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + "/pedeleclogs/");
            if (!dir.exists()) dir.mkdirs();
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
            String outFileName = dateFormat.format(date) + ".txt";
            outFile = new File(dir, outFileName);
            try {
                fOut = new FileOutputStream(outFile, true);
                osw = new OutputStreamWriter(fOut);
                osw.write("This is the log file of " + dateFormat.format(date));
                osw.write('\n');
                osw.flush();
                createdlogfile = true;
            } catch (Exception e) {
            }
        }
        String currentDateTimeString = dateFormatShort.format(new Date());
        try {
            osw.write(currentDateTimeString + ";" + logstring);
            osw.write('\n');
            osw.flush();
        } catch (Exception e) {
        }
    }

}
