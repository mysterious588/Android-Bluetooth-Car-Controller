package com.ieee.ras.bluetooth_rc;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "bluetooth1";
    int angle_lifter, angle_gripper;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    TextView speedText, gripperTextView, lifterTextView;
    SeekBar seekBar, liftSeekBar, gripperSeekBar;
    Button connectButton, up, down, left, right;
    // SPP UUID service HC05 & HC06 addresses
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // MAC-address of Bluetooth module (you must edit this line)
    //address of our HC05
    private static String address = "20:18:04:10:04:32";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        speedText = findViewById(R.id.speedTextView);
        seekBar = findViewById(R.id.speedSeekBar);
        liftSeekBar = findViewById(R.id.servoLiftAngle);
        gripperSeekBar = findViewById(R.id.servoGripperAngle);
        gripperTextView = findViewById(R.id.GripperAngle);
        lifterTextView = findViewById(R.id.LiftAngle);
        down = findViewById(R.id.down);
        up = findViewById(R.id.up);
        right = findViewById(R.id.right);
        left = findViewById(R.id.left);
        liftSeekBar.setMax(150);
        liftSeekBar.setMin(90);
        gripperSeekBar.setMax(90);
        gripperSeekBar.setMin(50);
        seekBar.setMax(4);//90% pwm
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sendData(Integer.toString(i));
                speedText.setText(Integer.toString(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        liftSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                lifterTextView.setText(Integer.toString(i));
                angle_lifter = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (btSocket.isConnected()) {
                    sendData("Z");
                    sendData(Integer.toString(seekBar.getProgress()));
                } else
                    Toast.makeText(MainActivity.this, "Not Connected", Toast.LENGTH_SHORT).show();

            }
        });
        gripperSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                gripperTextView.setText(Integer.toString(i));
                angle_gripper = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (btSocket.isConnected()) {
                    sendData("P");
                    sendData(Integer.toString(seekBar.getProgress()));
                } else
                    Toast.makeText(MainActivity.this, "Not Connected", Toast.LENGTH_SHORT).show();
            }
        });

        connectButton = findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothDevice device = btAdapter.getRemoteDevice(address);

                // Two things are needed to make a connection:
                //   A MAC address, which we got above.
                //   A Service ID or UUID.  In this case we are using the
                //     UUID for SPP.
                if (!btSocket.isConnected()) {
                    try {
                        btSocket = createBluetoothSocket(device);
                    } catch (IOException e1) {
                        errorExit("Fatal Error", "In onResume() and socket create failed: " + e1.getMessage() + ".");
                    }

                    // Discovery is resource intensive.  Make sure it isn't going on
                    // when you attempt to connect and pass your message.
                    btAdapter.cancelDiscovery();

                    // Establish the connection.  This will block until it connects.
                    Log.d(TAG, "...Connecting...");
                    try {
                        btSocket.connect();
                        Log.d(TAG, "...Connection ok...");
                    } catch (IOException e) {
                        try {
                            btSocket.close();
                        } catch (IOException e2) {
                            errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
                        }
                    }

                    // Create a data stream so we can talk to server.
                    Log.d(TAG, "...Create Socket...");

                    try {
                        outStream = btSocket.getOutputStream();
                    } catch (IOException e) {
                        errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Already connected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        JoystickView joystick = findViewById(R.id.joystickView);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                if (btSocket.isConnected()) {
                    if (strength != 0) {
                        if (isBetween(angle, 0, 45) || isBetween(angle, 315, 360)) sendData("R");
                        else if (isBetween(angle, 46, 135)) sendData("F");
                        else if (isBetween(angle, 136, 225)) sendData("L");
                        else if (isBetween(angle, 226, 314)) sendData("B");
                    } else sendData("S");
                } else {
                    Toast.makeText(MainActivity.this, "Not Connected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[]{UUID.class});
            return (BluetoothSocket) m.invoke(device, MY_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }

        return device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "...onResume - try connect...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e1) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e1.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting...");
        try {
            btSocket.connect();
            Log.d(TAG, "...Connection ok...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Create Socket...");

        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");

        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        try {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (btAdapter == null) {
            errorExit("Fatal Error", "Bluetooth not supported");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void errorExit(String title, String message) {
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        Log.d(TAG, "...Send data: " + message + "...");

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            if (address.equals("00:00:00:00:00:00"))
                msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 35 in the java code";
            msg = msg + ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";

            errorExit("Fatal Error", msg);
        }
    }

    private boolean isBetween(int number, int x, int y) {
        return (number >= x && number <= y);
    }

    private void connect() {
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e1) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e1.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting...");
        try {
            btSocket.connect();
            Log.d(TAG, "...Connection ok...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Create Socket...");

        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
        }
    }

    public void switch_controls(View view) {
        if (gripperSeekBar.getVisibility() == View.VISIBLE) {
            gripperSeekBar.setVisibility(View.GONE);
            liftSeekBar.setVisibility(View.GONE);
            up.setVisibility(View.VISIBLE);
            down.setVisibility(View.VISIBLE);
            right.setVisibility(View.VISIBLE);
            left.setVisibility(View.VISIBLE);

        } else {
            gripperSeekBar.setVisibility(View.VISIBLE);
            liftSeekBar.setVisibility(View.VISIBLE);
            up.setVisibility(View.GONE);
            down.setVisibility(View.GONE);
            right.setVisibility(View.GONE);
            left.setVisibility(View.GONE);
        }
    }

    public void down(View view) {
        if (btSocket.isConnected()) {
            if (angle_lifter <= 91) angle_lifter = 100;
            angle_lifter -= 10;
            lifterTextView.setText(Integer.toString(angle_lifter));
            liftSeekBar.setProgress(angle_lifter);
            sendData("Z");
            sendData(Integer.toString(angle_lifter));
        } else Toast.makeText(MainActivity.this, "Not Connected", Toast.LENGTH_SHORT).show();
    }

    public void up(View view) {
        if (btSocket.isConnected()) {
            if (angle_lifter <= 150) angle_lifter += 10;
            liftSeekBar.setProgress(angle_lifter);
            lifterTextView.setText(Integer.toString(angle_lifter));
            sendData("Z");
            sendData(Integer.toString(angle_lifter));
        } else Toast.makeText(MainActivity.this, "Not Connected", Toast.LENGTH_SHORT).show();

    }

    public void left(View view) {
        if (btSocket.isConnected()) {
            if (angle_gripper < 51) angle_gripper = 55;
            angle_gripper -= 5;
            gripperSeekBar.setProgress(angle_gripper);
            gripperTextView.setText(Integer.toString(angle_gripper));
            sendData("P");
            sendData(Integer.toString(angle_gripper));
        } else Toast.makeText(MainActivity.this, "Not Connected", Toast.LENGTH_SHORT).show();

    }


    public void right(View view) {
        if (btSocket.isConnected()) {
            if (angle_gripper <= gripperSeekBar.getMax() - 5) angle_gripper += 5;
            gripperSeekBar.setProgress(angle_gripper);
            gripperTextView.setText(Integer.toString(angle_gripper));
            sendData("P");
            sendData(Integer.toString(angle_gripper));
        } else Toast.makeText(MainActivity.this, "Not Connected", Toast.LENGTH_SHORT).show();

    }
}