package Util;


/**
 * Created by majunqi0102 on 9/18/16.
 */
public class Util {


    public static String truncateFileName(String full_path) {
        String[] temp = full_path.split("/");
        return temp[temp.length - 1];
    }


}
