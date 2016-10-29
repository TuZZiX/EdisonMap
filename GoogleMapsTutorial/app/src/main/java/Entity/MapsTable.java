package Entity;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Created by majunqi0102 on 10/28/16.
 */

public class MapsTable {

    private ArrayList<Maps> mapsList = new ArrayList<>();

    public void add(Maps singleRecord) {
        mapsList.add(singleRecord);
    }

//    public void replaceAdd(Maps singleRecord) {
//        String dir = singleRecord.getPhoto();
//        int index = find(dir);
//        if (index >= 0 && index < mapsList.size()) {
//            replaceAdd(index, singleRecord);
//        } else {
//            add(singleRecord);
//        }
//    }
//
//    public void replaceAdd(int index, Maps singleRecord) {
//        mapsList.set(index, singleRecord);
//    }

    // only find last index
    public int find(String DIRECTORY) {
        return find(DIRECTORY, 0, mapsList.size() - 1);
    }

    // Note: including start index and end index
    public int find(String DIRECTORY, int start, int end) {
        if (start >= 0 && end >= 0 && start < mapsList.size() && end < mapsList.size()) {
            for (int it = end; it >= start; it--) {
                if (mapsList.get(it).getPhoto().compareTo(DIRECTORY) == 0) {
                    return it;
                }
            }
        }
        return -1;
    }

    // may have a better performance
    public void mergeAndSortByDir() {
        TreeMap<String, Maps> temp = new TreeMap<>();
        for (int it = 0; it < mapsList.size(); it++) {
            temp.put(mapsList.get(it).getPhoto(), mapsList.get(it));
        }
        mapsList.clear();
        for (String key : temp.keySet()) {
            mapsList.add(temp.get(key));
        }
    }

    public Maps get(int index) {
        return mapsList.get(index);
    }

    public Maps get(String directory) {
        return mapsList.get(find(directory));
    }

    public void clear() {
        mapsList.clear();
    }

    public int size() {
        return mapsList.size();
    }

    public boolean isEmpty() {
        return mapsList.isEmpty();
    }
}
