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
}
