package ted.bluetoothsensorreader;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class SensorCSVWriter {
    private static final String TAG = "SensorCSVWriter";
    private static final String attributeLine = "TimeStamp,temp,humi,light,uv,pir,moist";
    private final String filename;
    private BufferedWriter bufferedWriter;

    public SensorCSVWriter(){
        filename = "EdisonSensor.csv";
    }

    /**
     * writes a single line of csv
     * @param data
     */
    public void writeLine(String data){
        initialize();
        try{
            bufferedWriter.append(data);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        }
        catch (IOException e){
            Log.wtf(TAG, "IOException while writing a line", e);
        }
    }

    /**
     * closes the writer
     */
    public void close(){
        try{
            bufferedWriter.close();
        }
        catch (IOException e){
            Log.wtf(TAG, "IOException Failed to close", e);
        }
    }

    /**
     * Initializes the csv writer
     *
     * doesn't do anything if it's already initialized
     * writer intialized as append mode
     */
    private void initialize(){
        if (bufferedWriter != null){
            return;
        }
        String root = Environment.getExternalStorageDirectory().toString();
        File csvDir = new File(root + "/IntelEdison/");
        csvDir.mkdir();
        File file = new File (csvDir, filename);
        boolean fileExists = file.exists();
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(file, true));
            if (!fileExists) {
                bufferedWriter.write(attributeLine);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        }
        catch (IOException e){
            Log.wtf(TAG, "IOExceptio", e);
        }
        Log.d(TAG, String.format("File is at: %s", file.getAbsolutePath()));
    }
}
