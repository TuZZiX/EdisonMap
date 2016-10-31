package com.example.haotian.tutorial32.Activity;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.example.haotian.tutorial32.Activity.BluetoothChat.*;
import DAO.MapsDao;
import Entity.Maps;
import Entity.MapsTable;
import Logger.*;
import Util.GPSTracker;
import Util.Util;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;

import com.example.haotian.tutorial32.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.opencsv.CSVWriter;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MapsActivity extends FragmentActivity {
    public static final String TAG = "MapsActivity";
    public static final int THUMBNAIL = 1;
    private final int REQUEST_TAKE_PHOTO = 4;

    private String photos_dir = Environment.getExternalStorageDirectory() + "/DCIM/Maps";
    private String csv_dir = Environment.getExternalStorageDirectory() + "/DCIM/Maps/maps.csv";
    private String photos_prefix = "maps";
    private DateFormat photo_timeformat = new SimpleDateFormat("yyyyMMdd_HH_mm_ss");
    private MapsTable mapsTable = new MapsTable();
    private MapsDao mapsDao = new MapsDao();
    private BluetoothChatFragment fragment;

    private String dataFromEdison = "";
    private boolean received = false;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Button picButton; //takes user to camera
    private GPSTracker gps;

    // Intent request codes
    public static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    public static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    public static final int REQUEST_ENABLE_BT = 3;


    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    //CB variables to save information for CSV
    private String SENSORCSVDIR;
    private final String[] columnCSV = {"TimeStamp", "Light", "UV", "Motion", "Moisture"};
    private String mTimestamp;
    private final String fileName = "SensorData.csv";


    private void setupChat() {
        // Initialize the BluetoothChatService to perform bluetooth connections
//        Log.d(TAG, "setupChat()");
//
//        // Initialize the array adapter for the conversation thread
//        mConversationArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.message);
//
//        mConversationView.setAdapter(mConversationArrayAdapter);
//
//        // Initialize the compose field with a listener for the return key
//        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getApplicationContext(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        initializeLogging();

//        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        fragment = new BluetoothChatFragment();
//        transaction.replace(R.id.sample_content_fragment, fragment);
//        transaction.commit();

        gps = new GPSTracker(MapsActivity.this);
        List<String[]> image_list = readImages();
        setUpMapIfNeeded(image_list);


//        picButton = (Button) findViewById(R.id.photobutton);
//
//        picButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dispatchTakePictureIntent();
//            }
//        });

        //CB save information necessary for CSV creation
        SENSORCSVDIR = android.os.Environment.getExternalStorageDirectory() + "/DCIM/SensorCSV";
        File CSVdir = new File(SENSORCSVDIR);
        if (!CSVdir.exists()) new File(SENSORCSVDIR).mkdir();

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
//            mChatService = new BluetoothChatService(getApplicationContext(), mHandler);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.bluetooth_chat, menu);
        return true;
    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
//                             @Nullable Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
//    }
//
//    @Override
//    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
//        mConversationView = (ListView) view.findViewById(R.id.in);
//        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
//        mSendButton = (Button) view.findViewById(R.id.button_send);
//    }
//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        inflater.inflate(R.menu.bluetooth_chat, menu);
//    }
//

    /**
     * Makes this device discoverable.
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getApplicationContext(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
//            Toast.makeText(getApplicationContext(), mOutStringBuffer.toString(), Toast.LENGTH_SHORT).show();
//            mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * CB The Handler that gets information back from the BluetoothChatService and will also save into a CSV
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
//                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
//                case Constants.MESSAGE_WRITE:
//                    byte[] writeBuf = (byte[]) msg.obj;
//                    // construct a string from the buffer
//                    String writeMessage = new String(writeBuf);
//                    mConversationArrayAdapter.add("Me:  " + writeMessage);
//                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    dataFromEdison = new String(readBuf, 0, msg.arg1);
                    received = true;
//                    Toast.makeText(getApplicationContext(), dataFromEdison, Toast.LENGTH_SHORT).show();
//                    //CB Here we'll save readMessage in a CSV file
//                    String[] incomingData = readMessage.split(",");
//                    if (incomingData.length==4) //AB CB we have 4 sensor values in the format 45,65,45....?
//                        saveCSV(incomingData);
//                    else System.out.println("Chat recieved Does not comply...");
//                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
            case R.id.take_a_picture: {
                sendMessage("start");
                dispatchTakePictureIntent();
                return true;
            }
        }
        return false;
    }

    /**
     * Establish connection with other divice
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }


    //CB Method to save CSV
    private void saveCSV(String[] data) {

        //CB CSV file creation
        CSVWriter writer = null;
        try {
            String baseDir = SENSORCSVDIR;
            String filePath = baseDir + File.separator + fileName;
            File f = new File(filePath);
            if (!f.exists()) {
                writer = new CSVWriter(new FileWriter(filePath));
                String[] column = columnCSV;
                writer.writeNext(column);
                writer.close();
                System.out.println("CSV file Created for the first time");
            }
            if (f.exists()) {
                String[] alldata = new String[5]; //AB 5 values from 0 to 4
                for (int i = 1; i < alldata.length; i++) //AB 1 to 4
                    alldata[i] = data[i - 1];
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd-hhmmss");
                mTimestamp = simpleDateFormat.format(new Date()); //AB Timestamp...
                alldata[0] = mTimestamp; //CB to store the current time.
                writer = new CSVWriter(new FileWriter(filePath, true));
                String[] values = alldata; //CB All should be strings
                writer.writeNext(values); //CB Means append to the file...
                writer.close();
            }
        } catch (IOException e) {
            //error
        }
    }


    Uri imageUri;
    String takenTime;

    private void dispatchTakePictureIntent() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (!checkAndCreateDirectory(photos_dir)) {
            Toast.makeText(getApplicationContext(), "Fail to create photo folder", Toast.LENGTH_SHORT).show();
            return;
        }
        takenTime = photo_timeformat.format(new Date());
        imageUri = Uri.fromFile(new File(photos_dir, photos_prefix + takenTime + ".jpg"));
        takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageUri);
        Log.i("photo_dir:", imageUri.toString());
        startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
    }

    private boolean checkAndCreateDirectory(String Dir) {
        File imageFolder = new File(Dir);
        return imageFolder.exists() || imageFolder.mkdirs();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    setupChat();
//                    mChatService = new BluetoothChatService(getApplicationContext(), mHandler);
                    // Bluetooth is now enabled, so set up a chat session
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getApplicationContext(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                }
            case REQUEST_TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    // Check if GPS enabled
                    sendMessage("start");
                    double latitude = 41.502825;
                    double longitude = -81.606651;

                    if (gps.canGetLocation()) {

                        Location location = gps.getLocation();
                        latitude = gps.getLatitude();
                        longitude = gps.getLongitude();

                        // \n is for new line
//                        Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
                    } else {
                        // Can't get location.
                        // GPS or network is not enabled.
                        // Ask user to enable GPS/network in settings.
                        gps.showSettingsAlert();
                    }

                    //get other info from edison board

                    Maps maps = new Maps();
                    maps.setPhoto(imageUri.toString());
                    maps.setTimestamp(takenTime);
                    maps.setLatitude(latitude);
                    maps.setLongitude(longitude);
                    maps.setMoisture(0);
                    maps.setLight(0);
                    maps.setTemp(0);
                    maps.setHumi(0);
                    maps.setUV(0);
                    maps.setPIR(0);

                    //waiting to receive data
                    while (received == false) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    //once received message, reset it to false for next message.
                    received = false;

//                    String dataFromEdison = "{" +
//                            "    \"data\" : {\"Moisture\": 10," +
//                            "    \"Light\": 512," +
//                            "    \"Temp\": 26," +
//                            "    \"Humi\": 26," +
//                            "    \"UV\": 0," +
//                            "    \"PIR\": 0}" +
//                            "}";

                    try {
                        JSONObject jsonRootObject = new JSONObject(dataFromEdison);
                        JSONObject jsonObject = jsonRootObject.optJSONObject("data");
                        Double bb = jsonObject.optDouble("Moisture");
                        maps.setMoisture(bb);
                        maps.setLight(jsonObject.optDouble("Light"));
                        maps.setTemp(jsonObject.optDouble("Temp"));
                        maps.setHumi(jsonObject.optDouble("Humi"));
                        maps.setUV(jsonObject.optDouble("UV"));
                        maps.setPIR(jsonObject.optInt("PIR"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    mapsTable.add(maps);
                    mapsDao.writeToCSV(mapsTable, csv_dir);


                    List<String[]> image_list = readImages();
                    setUpMapIfNeeded(image_list);
                }
        }
    }

    private List<String[]> readImages() {
        FilenameFilter imgFilter = new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                File sel = new File(dir, filename);
                if (!sel.canRead()) return false;
                else {
                    boolean endsWith = filename.toLowerCase().endsWith(".jpg");
                    return endsWith || sel.isDirectory();
                }
            }
        };
        List<String[]> image_list = new ArrayList<>();
        File photo_path = new File(photos_dir);
        if (photo_path.exists()) {
            File[] fileList = photo_path.listFiles(imgFilter);
            if (fileList != null)
                for (File file : fileList) {
                    if (file.isFile()) {
                        String photo_dir = photos_dir + "/" + file.getName();
//                        String[] exif = readExifLocation(photo_dir);
//                        if(exif[0] == null || exif[1] == null || exif[2] == null)
//                            continue;
                        /*
                        private String photo;
    private String timestamp;
    private double latitude;
    private double longitude;
    private double moisture;
    private double light;
    private double temp;
    private double humi;
    private double UV;
    private int PIR;
                         */
                        if (mapsTable.isEmpty() || mapsTable == null)
                            mapsDao.readCSV(mapsTable, csv_dir);
                        int index = mapsTable.find(photo_dir);
                        if (index != -1) {
                            Maps maps = mapsTable.get(index);
                            String[] image = new String[10];
                            image[0] = photo_dir;
                            image[1] = maps.getTimestamp();
                            image[2] = Double.toString(maps.getLatitude());
                            image[3] = Double.toString(maps.getLongitude());
                            image[4] = Double.toString(maps.getMoisture());
                            image[5] = Double.toString(maps.getLight());
                            image[6] = Double.toString(maps.getTemp());
                            image[7] = Double.toString(maps.getHumi());
                            image[8] = Double.toString(maps.getUV());
                            image[9] = Double.toString(maps.getPIR());

                            image_list.add(image);
                        }
                    }
                }
        }

        return image_list;
    }

    //string[0] is latitude, string[1] is longitude, string[3] is timestamp
    private String[] readExifLocation(String path) {
        String[] exif = new String[3];
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            exif[0] = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            exif[1] = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            exif[2] = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
        }

        return exif;
    }

    private String readExif(String file) {
        String exif = "Exif: " + file;
        try {
            ExifInterface exifInterface = new ExifInterface(file);

            exif += "\nIMAGE_LENGTH: " + exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
            exif += "\nIMAGE_WIDTH: " + exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
            exif += "\n DATETIME: " + exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
            exif += "\n TAG_MAKE: " + exifInterface.getAttribute(ExifInterface.TAG_MAKE);
            exif += "\n TAG_MODEL: " + exifInterface.getAttribute(ExifInterface.TAG_MODEL);
            exif += "\n TAG_ORIENTATION: " + exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
            exif += "\n TAG_WHITE_BALANCE: " + exifInterface.getAttribute(ExifInterface.TAG_WHITE_BALANCE);
            exif += "\n TAG_FOCAL_LENGTH: " + exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
            exif += "\n TAG_FLASH: " + exifInterface.getAttribute(ExifInterface.TAG_FLASH);
            exif += "\nGPS related:";
            exif += "\n TAG_GPS_DATESTAMP: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_DATESTAMP);
            exif += "\n TAG_GPS_TIMESTAMP: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP);
            exif += "\n TAG_GPS_LATITUDE: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            exif += "\n TAG_GPS_LATITUDE_REF: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            exif += "\n TAG_GPS_LONGITUDE: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            exif += "\n TAG_GPS_LONGITUDE_REF: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
            exif += "\n TAG_GPS_PROCESSING_METHOD: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD);
            Toast.makeText(getApplicationContext(),
                    "finished",
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
        }
        return exif;
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<String[]> image_list = readImages();
        setUpMapIfNeeded(image_list);

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

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap(List)} ()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded(List<String[]> image_list) {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap != null)
            mMap.clear();
        // Try to obtain the map from the SupportMapFragment.
        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                .getMap();
        // Check if we were successful in obtaining the map.
        if (mMap != null) {
            setUpMap(image_list);
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap(List<String[]> image_list) {
        if (image_list != null && image_list.size() > 0)
            for (String[] image : image_list) {
                try {
                    FileInputStream in = new FileInputStream(image[0]);
                    BufferedInputStream buf = new BufferedInputStream(in);
                    byte[] bMapArray = new byte[buf.available()];
                    buf.read(bMapArray);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bMapArray, 0, bMapArray.length);
                    bitmap = bitmap.createScaledBitmap(bitmap, 200, 200, false);
//                Bitmap bitmap = Bitmap.createBitmap(image[0])
                    BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(bitmap);
                    mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(image[2]), Double.parseDouble(image[3]))).snippet(image[4] + ", " + image[5] + "," + image[6] + "," + image[7] + "," + image[8] + "," + image[9]).title(Util.truncateFileName(image[0])).icon(icon));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

    }

    public void initializeLogging() {
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        // Wraps Android's native log framework
        LogWrapper logWrapper = new LogWrapper();
        Log.setLogNode(logWrapper);

        Log.i(TAG, "Ready");
    }


}
