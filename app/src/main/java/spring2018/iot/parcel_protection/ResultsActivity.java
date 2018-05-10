package spring2018.iot.parcel_protection;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static app.akexorcist.bluetotohspp.library.BluetoothState.REQUEST_ENABLE_BT;

/**
 * Created by Arnab Purkayastha on 5/5/2018.
 */

public class ResultsActivity extends AppCompatActivity {

    private TextView textRead;
    private TextView textStatus;

    BluetoothAdapter bluetoothAdapter;

    ArrayList<BluetoothDevice> pairedDeviceArrayList;

    ArrayAdapter<BluetoothDevice> pairedDeviceAdapter;
    private UUID myUUID;
    private final String UUID_STRING_WELL_KNOWN_SPP = "00001101-0000-1000-8000-00805F9B34FB";

    ThreadConnectBTdevice myThreadConnectBTdevice;
    ThreadConnected myThreadConnected;
    Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        Button goBackButton = (Button)findViewById(R.id.goBackButton);
        Button sendSignalButton = (Button)findViewById(R.id.button5);

        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ResultsActivity.this, MainActivity.class));
            }
        });

        sendSignalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myThreadConnected != null){
                    byte[] signal = {0x02, 0x01, 0x00, 0x03};
                    myThreadConnected.write(signal);
                }
            }
        });

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
            Toast.makeText(this,
                    "FEATURE_BLUETOOTH NOT SUPPORTED",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null){
            Toast.makeText(this,
                    "Blueooth is not supported on this device",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        textRead = (TextView)findViewById(R.id.textRead);
        textStatus = (TextView)findViewById(R.id.textStatus);

        String stInfo = "Device connected: " + bluetoothAdapter.getName() + " - " + bluetoothAdapter.getAddress();
        textStatus.setText(stInfo);



    }

    @Override
    protected void onStart(){
        super.onStart();

        if(!bluetoothAdapter.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        setup();
    }

    private void setup(){
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        BluetoothDevice parcel = null;
        if (pairedDevices.size() > 0){
            pairedDeviceArrayList = new ArrayList<BluetoothDevice>();

            for (BluetoothDevice device : pairedDevices){
                if(device.getName().equals("HC-06")){
                    parcel = device;
                }
            }

            if(parcel != null){
                Toast.makeText(ResultsActivity.this,
                        "Name: " + parcel.getName() + "\n"
                                + "Address: " + parcel.getAddress() + "\n"
                                + "BondState: " + parcel.getBondState() + "\n"
                                + "BluetoothClass: " + parcel.getBluetoothClass() + "\n"
                                + "Class: " + parcel.getClass(),
                        Toast.LENGTH_LONG).show();

                textStatus.setText("Status: Connected to " + parcel.getName());
                myThreadConnectBTdevice = new ThreadConnectBTdevice(parcel);
                myThreadConnectBTdevice.start();
            }
            else {
                Toast.makeText(ResultsActivity.this,
                        "Parcel Device Not Found",
                        Toast.LENGTH_LONG).show();

                textStatus.setText("Stats: Not Connected");
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(myThreadConnectBTdevice!=null){
            myThreadConnectBTdevice.cancel();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_ENABLE_BT){
            if(resultCode == Activity.RESULT_OK){
                setup();
            }else{
                Toast.makeText(this,
                        "Bluetooth NOT enabled",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    //Called in ThreadConnectBTdevice once connect successed
    //to start ThreadConnected
    private void startThreadConnected(BluetoothSocket socket){

        myThreadConnected = new ThreadConnected(socket);
        myThreadConnected.start();
    }

    /*
ThreadConnectBTdevice:
Background Thread to handle BlueTooth connecting
*/
    private class ThreadConnectBTdevice extends Thread {

        private BluetoothSocket bluetoothSocket = null;
        private final BluetoothDevice bluetoothDevice;


        private ThreadConnectBTdevice(BluetoothDevice device) {
            bluetoothDevice = device;

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
                textStatus.setText("bluetoothSocket: \n" + bluetoothSocket);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                bluetoothSocket.connect();
                success = true;
            } catch (IOException e) {
                e.printStackTrace();

                final String eMessage = e.getMessage();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        textStatus.setText("something wrong bluetoothSocket.connect(): \n" + eMessage);
                    }
                });

                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            if(success){
                //connect successful
                final String msgconnected = "connect successful:\n"
                        + "BluetoothSocket: " + bluetoothSocket + "\n"
                        + "BluetoothDevice: " + bluetoothDevice;

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        textStatus.setText("");
                        Toast.makeText(ResultsActivity.this, msgconnected, Toast.LENGTH_LONG).show();
                    }
                });

                startThreadConnected(bluetoothSocket);

            }else{
                //fail
            }
        }

        public void cancel() {

            Toast.makeText(getApplicationContext(),
                    "close bluetoothSocket",
                    Toast.LENGTH_LONG).show();

            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }

    //init option menu
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_connection, menu);
        return true;
    }

    //handle when an item in the menu options is selected.
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_device_connect) {
            //connecting other kind of devices
        } else if (id == R.id.menu_disconnect) {
            //disconnect from a device
        }
        return super.onOptionsItemSelected(item);
    }

    /*
    ThreadConnected:
    Background Thread to handle Bluetooth data communication
    after connected
     */
    private class ThreadConnected extends Thread {
        private final BluetoothSocket connectedBluetoothSocket;
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;

        public ThreadConnected(BluetoothSocket socket) {
            connectedBluetoothSocket = socket;
            InputStream in = null;
            OutputStream out = null;

            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[2];
            byte[] payload_header = new byte[4];
            byte[] event0 = new byte[4];
            byte[] event1 = new byte[4];
            byte[] event2 = new byte[4];
            byte[] event3 = new byte[4];
            byte[] crc = new byte[1];
            int bytes;
            try {
                String strReceived = "";
                connectedInputStream.read(buffer, 0, 2);
                int payload_len = buffer[1];
                Log.i("Confirmation Recieved", Integer.toString(payload_len));
                if(buffer[0] == -126){
                    Log.i("Reading Payload Header", "Confirmed");
                    connectedInputStream.read(payload_header, 0, 4);
                    int numEvents = payload_header[3];
                    Log.i("Payload Header Recieved", Integer.toString(numEvents));
                    for(int i=0; i < numEvents; i++){
                        connectedInputStream.read(event0, 0, 4);
                        connectedInputStream.read(event1, 0, 4);
                        connectedInputStream.read(event2, 0, 4);
                        connectedInputStream.read(event3, 0, 4);
                        TimeUnit.MILLISECONDS.sleep(500);
                        if (event0[3] == 0) {
                            strReceived += "    DROP\t";
                            Log.i("Event Added", "DROP");
                        } else if (event0[3] == 1) {
                            strReceived += "    FLIP\t\t\t";
                            Log.i("Event Added", "FLIP");
                        }

                        int year = 2018;
                        int month = event2[1];
                        int dow = event2[2];
                        int day = event2[3];
                        int hour = event3[0];
                        int minute = event3[1];
                        int second = event3[2];

                        strReceived += "                  " + month + "/" + day + "/" + year;

                        strReceived += "\n";
                    }
                    connectedInputStream.read(crc, 0, 1);
                }
                final String finalString = strReceived;
                runOnUiThread(new Runnable(){

                    @Override
                    public void run() {
                        textRead.append(finalString);
                    }});

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();

                final String msgConnectionLost = "Connection lost:\n"
                        + e.getMessage();
                runOnUiThread(new Runnable(){

                    @Override
                    public void run() {
                        textStatus.setText(msgConnectionLost);
                    }});
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                connectedBluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}

