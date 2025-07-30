package basics;

import java.io.Serializable;

public class Centroid implements Serializable {

    // A centroid is a data point that represents the center of the cluster (the mean), and it might not necessarily be a member of the dataset
    // basically a point that is a "center" of clusters
    // just need coordinates

    private static final long serialVersionUID = 1L;
    private double longitude; // vertical lines, y osa
    private double latitude; // horizontal lines, x osa

    public Centroid() {
        // Required for Jackson
    }

    public Centroid(double longitude, double latitude){
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
}
