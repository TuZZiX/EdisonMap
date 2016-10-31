package ted.bluetoothsensorreader;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Created by ted on 11/7/2015.
 */
public class BluetoothSensorService {
    private static final String TAG = "BluetoothSensorService";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private final BluetoothAdapter mBluetoothAdapter;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  // now connected to a remote device

    private int mState;
    private SensorCSVWriter csvWriter = new SensorCSVWriter();

    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private Location homeLocation;
    private boolean isFileTransferMode;
    private Context callingContext;

    //Handler used to make toast inside threads
    private Handler toastHandler;
    private int i = 0;

    public BluetoothSensorService(Context context, Location location){
        this.callingContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.homeLocation = location;
        isFileTransferMode = false;
        toastHandler = new Handler();
    }

    public synchronized void setState (int state){
        Log.d(TAG, "turning state from " + mState +"to " +state);
        mState = state;
    }

    public synchronized int getState (){
        return mState;
    }

    /**
     * This is called to start a bluetooth connection with another device
     * @param device
     */
    public synchronized void connect (BluetoothDevice device){
        Log.d(TAG, "Connecting to " + device);

        if (mState == STATE_CONNECTING){
            if (mConnectThread != null){
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        if (mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * This is called to start managing the bluetooth connection
     *
     * @param socket
     * @param device
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device){
        Log.d(TAG, "connected to " + device);
        //Optional make send button visible

        //free the connect thread since we're already connected
        if (mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        //We are going to use the thread, so cancel that
        if (mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        setState(STATE_CONNECTED);
    }

    //free all the threads
    public synchronized void stop(){
        Log.d(TAG, "STOP!");
        if (mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        csvWriter.close();
        setState(STATE_NONE);
    }

    /**
     * writes the message to the server
     * @param message
     */
    public synchronized void write(String message){
        if (message.equals("start")){
            isFileTransferMode = false;
        }
        else{
            isFileTransferMode = true;
        }
        byte[] buffer = message.getBytes();

        ConnectedThread thread;
        if (mState != STATE_CONNECTED) return;
        thread = mConnectedThread;

        thread.write(buffer);
    }

    private class ConnectThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tempSocket = null;
            try {
                tempSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "failed to create socket", e);
            }
            mmSocket = tempSocket;
        }

        public void run() {
            Log.i(TAG, "Begin mConnectThread");
            setName("ConnectThread");
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() the socket during connection failure", e2);
                }

                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothSensorService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }


    private class ConnectedThread extends Thread{
        private final BluetoothSocket connectedBluetoothSocket;
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;
        private static final int MESSAGE_READ = 2;

        public ConnectedThread(BluetoothSocket socket) {
            connectedBluetoothSocket = socket;
            InputStream tmpInStream = null;
            OutputStream tmpOutStream = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpInStream = socket.getInputStream();
                tmpOutStream = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            connectedInputStream = tmpInStream;
            connectedOutputStream = tmpOutStream;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    bytes = connectedInputStream.read(buffer);

                    //convert from byte[] to string
                    //if that didn't work use BufferedReader
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
                    int size = byteArrayInputStream.available();
                    byte[] decode = new byte[size];
                    byteArrayInputStream.read(decode, 0, size);
                    String result = new String(decode, StandardCharsets.UTF_8);
                    Log.d(TAG, "isFileTransferMode: " + isFileTransferMode);
                    if (isFileTransferMode){
                        //writes radon data to CSV
                        RadonSensorCSVWriter.writeCSV(result, homeLocation.getLatitude(), homeLocation.getLongitude());
                        toastHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(callingContext, String.format("Finished receiving data from Edison: %d", i), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    else{
                        //write to CSV
                        //TODO:  这段代码改过
//                        String timestamp = DateFormat.getTimeInstance().format((new Date()));
//                        csvWriter.writeLine(timestamp + "," + result);
                        csvWriter.writeLine(result);
                    }
                    Log.d(TAG, "Received data " + result);
                } catch (IOException e) {
                    Log.wtf(TAG, "disconnected during connected thread", e);
                    toastHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(callingContext, "Edison ended the connection", Toast.LENGTH_SHORT).show();
                        }
                    });
                    csvWriter.close();
                    BluetoothSensorService.this.stop();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                Log.wtf(TAG, "Failed to write", e);
            }
        }

        public void cancel() {
            try {
                connectedBluetoothSocket.close();
            } catch (IOException e) {
                Log.wtf(TAG, "Failed to close the connect socket failed", e);
            }
        }
    }
}
