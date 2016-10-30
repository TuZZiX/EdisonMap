package com.example.haotian.tutorial32.Activity;

import DAO.MapsDao;
import Entity.Maps;
import Entity.MapsTable;
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
import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.widget.Toast;
import com.example.haotian.tutorial32.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class MapsActivity extends FragmentActivity {
    public static final String TAG = "MapsActivity";
    public static final int THUMBNAIL = 1;
    private final int REQUEST_TAKE_PHOTO = 1;

    private String photos_dir = Environment.getExternalStorageDirectory() + "/DCIM/Maps";
    private String csv_dir = Environment.getExternalStorageDirectory() + "/DCIM/Maps/maps.csv";
    private String photos_prefix = "maps";
    private DateFormat photo_timeformat = new SimpleDateFormat("yyyyMMdd_HH_mm_ss");
    private MapsTable mapsTable = new MapsTable();
    private MapsDao mapsDao = new MapsDao();

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Button picButton; //takes user to camera
    private GPSTracker gps;


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

        gps = new GPSTracker(MapsActivity.this);
        List<String[]> image_list = readImages();
        setUpMapIfNeeded(image_list);


        picButton = (Button) findViewById(R.id.photobutton);

        picButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });
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
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
//            Toast.makeText(getApplicationContext(), getText(R.string.msg_open_camera), Toast.LENGTH_SHORT).show();
            // Check if GPS enabled
            double latitude = 41.502825;
            double longitude = -81.606651;

            if (gps.canGetLocation()) {

                Location location = gps.getLocation();
                latitude = gps.getLatitude();
                longitude = gps.getLongitude();

                // \n is for new line
                Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
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
            maps.setMoisture(123.123);
            maps.setLight(123.123);
            maps.setTemp(123.123);
            maps.setHumi(123.123);
            maps.setUV(123.123);
            maps.setPIR(1);

            mapsTable.add(maps);
            mapsDao.writeToCSV(mapsTable, csv_dir);


            List<String[]> image_list = readImages();
            setUpMapIfNeeded(image_list);
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
                    mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(image[2]), Double.parseDouble(image[3]))).snippet(image[4]+", "+image[5]+","+image[6]+","+image[7]+","+image[8]+","+image[9]).title(Util.truncateFileName(image[0])).icon(icon));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

    }


}
