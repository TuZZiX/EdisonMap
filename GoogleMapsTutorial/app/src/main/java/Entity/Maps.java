package Entity;

/**
 * Created by majunqi0102 on 10/28/16.
 */

public class Maps {

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

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getMoisture() {
        return moisture;
    }

    public void setMoisture(double moisture) {
        this.moisture = moisture;
    }

    public double getLight() {
        return light;
    }

    public void setLight(double light) {
        this.light = light;
    }

    public double getTemp() {
        return temp;
    }

    public void setTemp(double temp) {
        this.temp = temp;
    }

    public double getHumi() {
        return humi;
    }

    public void setHumi(double humi) {
        this.humi = humi;
    }

    public double getUV() {
        return UV;
    }

    public void setUV(double UV) {
        this.UV = UV;
    }

    public int getPIR() {
        return PIR;
    }

    public void setPIR(int PIR) {
        this.PIR = PIR;
    }
}
