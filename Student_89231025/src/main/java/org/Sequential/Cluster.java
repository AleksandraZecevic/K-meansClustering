package org.Sequential;
import java.util.ArrayList;
import java.util.List;
public class Cluster {

    private Centroid centroid;          // centroid that's closest to a cluster
    private List<Facility> facilities; // facilities assigned to this cluster

    public Cluster(Centroid centroid) {
        this.centroid = centroid;
        this.facilities = new ArrayList<>();
    }

    // setters, getters and adding facility
    public List<Facility> getFacilities() {
        return facilities;
    }

    public void addFacility(Facility f){
        // how will this work if we read from JSON?
        // try out later !!
        facilities.add(f);

    }

    public void clearFacilities(){
        facilities.clear();
    }

    public Centroid getCentroid() {
        return centroid;
    }

    public void setCentroid(Centroid centroid) {
        this.centroid = centroid;
    }

    public void recalculateCentroid() {
        if (facilities.isEmpty()) {
            return; // no change if empty
        }

        double sumLongitude = 0;
        double sumLatitude = 0;

        for (Facility f : facilities) {
            sumLongitude += f.getLongitude();
            sumLatitude += f.getLatitude();
        }

        double avgLongitude = sumLongitude / facilities.size();
        double avgLatitude = sumLatitude / facilities.size();

        this.centroid = new Centroid(avgLongitude, avgLatitude);
    }
}
