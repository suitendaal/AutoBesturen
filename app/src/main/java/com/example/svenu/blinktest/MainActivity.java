package com.example.svenu.blinktest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private SeekBar seekBarSpeed;
    private SeekBar seekBarDirection;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the Bluetooth services
    private BluetoothChatService mBluetoothService = null;

    private BooleanChanger connected = new BooleanChanger(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        startDeviceListActivity();
        connected.setVariableChangeListener(new ConnectedChangeListener());

        setButtons();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.my_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuConnect:
                startDeviceListActivity();
                break;
            case R.id.menuDisconnect:
                disconnectDevices();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setButtons() {
        seekBarSpeed = findViewById(R.id.seekBarSpeed);
        seekBarDirection = findViewById(R.id.seekBarDirection);

        seekBarSpeed.setMax(510);

        seekBarSpeed.setProgress(255);
        seekBarDirection.setProgress(50);
    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, "BlueTooth not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            message += "\n";
            // Get the message bytes and tell the BluetoothService to write
            byte[] send = message.getBytes();
            mBluetoothService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }

    private void disconnectDevices()
    {
        if (mBluetoothService != null)
        {
            mBluetoothService.stop();
        }
    }

    private void startDeviceListActivity() {
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    @Override
    public void onStart() {
        super.onStart();

        // If BT is not on, request that it be enabled.
        // setupUserInterface() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the Bluetooth session
        } else {
            if (mBluetoothService == null) {
                setupUserInterface();
            }
        }
    }

    private void setupUserInterface()
    {
        // Initialize the send button with a listener that for click events
        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                if (fromUser) {
//                    sendSpeed(progress);
//                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                sendSpeed(progress);
            }
        });

        seekBarDirection.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                sendDeviation(progress);
            }
        });

        // Initialize the BluetoothService to perform Bluetooth connections
        mBluetoothService = new BluetoothChatService(mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    private void sendDeviation(int progress) {
        String message = "d";
        int deviation = progress - 50;
        if (deviation >= 0) {
            message += "r";
        }
        else {
            message += "l";
        }
        message += abs(deviation);

        Log.d("deviationMessage", message);
        sendMessage(message);
    }

    private void sendSpeed(int progress) {
        String message = "s";
        int speed = progress - 255;
        if (speed >= 0) {
            message += "f";
        }
        else {
            message += "b";
        }
        message += abs(speed);

        Log.d("speedMessage", message);
        sendMessage(message);
    }

    private int abs(int value) {
        if (value > 0) {
            return value;
        }
        else {
            return value * -1;
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (mBluetoothService != null)
        {
            mBluetoothService.stop();
        }
    }

    // The Handler that gets information back from the BluetoothService
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            connected.setValue(true);
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            break;
                        case BluetoothChatService.STATE_NONE:
                            connected.setValue(false);
                            startDeviceListActivity();
                            break;
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mBluetoothService.connect(device);
                }
                break;

            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode != Activity.RESULT_OK) {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(this, "BT NOT ON", Toast.LENGTH_SHORT).show();
                }
                setupUserInterface();
        }
    }

    private class ConnectedChangeListener implements BooleanChanger.VariableChangeListener {
        @Override
        public void onVariableChanged(BooleanChanger booleanChanger) {
            boolean isConnected = booleanChanger.getValue();
            if (!isConnected) {
                startDeviceListActivity();
            }
        }
    }
}
