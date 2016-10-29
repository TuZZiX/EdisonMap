package DAO;

import Entity.Maps;
import Entity.MapsTable;
import Util.Util;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by majunqi0102 on 10/28/16.
 */

public class MapsDao {

    public boolean writeToCSV(MapsTable mapsTable, String csvDir) {

        if (mapsTable.isEmpty()) {
            return false;
        }
        List<String[]> formatted = new ArrayList<>();   // CSV writer only accept this type, so we have to do conversion
        String[] nextLine;          // Each row in CSV
        nextLine = ("photo" + "," + "timestamp" + "," + "latitude" + "," + "longitude" + "," + "moistrue" + "," + "light" + "," + "temp" + "," + "humi" + "," + "UV" + "," + "PIR").split(",");
        formatted.add(nextLine);
        for (int it = 0; it < mapsTable.size(); it++) {
            Maps record = mapsTable.get(it);      // Get every record from grading table
            nextLine = (Util.truncateFileName(record.getPhoto()) + "," +
                    record.getTimestamp() + "," + record.getLatitude() + "," + record.getLongitude() + "," +
                    record.getMoisture() + "," + record.getLight() + "," + record.getTemp() + "," +
                    record.getHumi() + "," + record.getUV() + "," + record.getPIR()).split(",");   // format each record
            formatted.add(nextLine);
        }
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(csvDir));
            writer.writeAll(formatted, false);          // Write all records in to CSV
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean readCSV(MapsTable mapsTable, String csvDir) {
        CSVReader reader;
        String[] reading;
        File csv = new File(csvDir);
        if (csv.exists()) {
            try {
                String suffix = csvDir.substring(0, csvDir.lastIndexOf("/"));       // Get path of the folder that contains this csv file
                reader = new CSVReader(new FileReader(csvDir));
                List<String[]> csvRead = reader.readAll();                          // read all records from this csv file
                for (int it = 1; it < csvRead.size(); it++) {
                    reading = csvRead.get(it);
                    Maps maps = new Maps();
                    maps.setPhoto(suffix+"/"+reading[0]);
                    maps.setTimestamp(reading[1]);
                    maps.setLatitude(Double.parseDouble(reading[2]));
                    maps.setLongitude(Double.parseDouble(reading[3]));
                    maps.setMoisture(Double.parseDouble(reading[4]));
                    maps.setLight(Double.parseDouble(reading[5]));
                    maps.setTemp(Double.parseDouble(reading[6]));
                    maps.setHumi(Double.parseDouble(reading[7]));
                    maps.setUV(Double.parseDouble(reading[8]));
                    maps.setPIR(Integer.parseInt(reading[9]));
                    mapsTable.add(maps);
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

}
