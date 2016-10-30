package ted.bluetoothsensorreader;

import android.os.Environment;
import android.util.Log;
import com.opencsv.CSVWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RadonSensorCSVWriter {

    private static final String TAG = "CSVWriter";
    private static final String attributes = "Timestamp,Temperature,Humidity,Light,UV,Radon,Latitude, Longitude";
    private static final String[] attributesArr = {"Timestamp","Temperature","Humidity","Light","UV","Radon","Latitude","Longitude"};


    /**
     * writes the csv using the opencsv library
     * @param streamContents
     * @param latitude
     * @param longitude
     */
    public static void writeCSV(String streamContents, double latitude, double longitude) {
        String [] splitContents = streamContents.split("\n");
        List<String[]> csvContent = new ArrayList<String[]>();

        String filename= "" + latitude + "," + longitude + ".csv";
        String root = Environment.getExternalStorageDirectory().toString();
        File csvDir = new File(root + "/DCIM/IntelEdisonRadon");
        csvDir.mkdir();
        File file = new File (csvDir, filename);
        boolean exists = file.exists();

        try {
            CSVWriter csvWriter = new CSVWriter(new FileWriter(file, true));
            fillCSV(csvContent, splitContents, latitude, longitude);
            if (!exists) {
                csvWriter.writeNext(attributesArr);
            }
            csvWriter.writeAll(csvContent);
            csvWriter.close();
        }
        catch (IOException e){
            Log.wtf(TAG, "IOException", e);
        }
        Log.d(TAG, String.format("File is at: %s", file.getAbsolutePath()));
    }

    /**
     * Fills up the List of String array.
     *
     * @param csvContent
     * @param splitContents
     * @param latitude
     * @param longitude
     */
    private static void fillCSV(List<String[]> csvContent, String[] splitContents, double latitude, double longitude){
        for (String line : splitContents){
            //writer.write(splitContents[i] + "," +latLongFormat);
            int lengthWithoutNewLine = line.length()-1;
            if (lengthWithoutNewLine>0) {
                String[] originalLine = line.split(",");
                // Not enough data, most likely corrupted stream
                if (originalLine.length < 6){
                    continue;
                }
                String[] newLine = new String[originalLine.length+2];
                copyStringArr(newLine, originalLine);
                newLine[newLine.length-2] = ""+latitude;
                newLine[newLine.length-1] = ""+longitude;
                csvContent.add(newLine);
            }
        }
    }

    /**
     * copies over contents of originalLine to newLine
     * newLine must be larger than originalLine in terms of length
     * @param newLine
     * @param originalLine
     */
    private static void copyStringArr (String[] newLine, String[] originalLine){
        for (int i = 0; i<originalLine.length; i++){
            newLine[i] = originalLine[i];
        }
    }

    /**
     * @deprecated
     * old write CSV implementation using plain text
     * don't use!
     * @param streamContents
     * @param latitude
     * @param longitude
     */
    public static void writeCSV_plainText(String streamContents, double latitude, double longitude){

        String [] splitContents = streamContents.split("\n");
        String filename = String.format("%f_%f.csv", latitude, longitude);

        String root = Environment.getExternalStorageDirectory().toString();
        File csvDir = new File(root + "/IntelEdisonRadon");
        csvDir.mkdir();
        File file = new File (csvDir, filename);
        boolean exists = file.exists();

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            if (!exists) {
                writer.write(attributes);
            }
            writer.newLine();
            for (int i = 1; i<splitContents.length; i++){
                //writer.write(splitContents[i] + "," +latLongFormat);
                int lengthWithoutNewLine = splitContents[i].length()-1;
                if (lengthWithoutNewLine>0) {
                    writer.write(splitContents[i].substring(0, lengthWithoutNewLine) + "," + latitude + "," + longitude);
                    if ( i != splitContents.length-1) {
                        writer.newLine();
                    }
                }
                writer.flush();
            }
            writer.close();
        }
        catch(IOException e){
            Log.wtf(TAG, "IOException", e);
        }
        Log.d(TAG, String.format("File is at: %s", file.getAbsolutePath()));
    }
}

