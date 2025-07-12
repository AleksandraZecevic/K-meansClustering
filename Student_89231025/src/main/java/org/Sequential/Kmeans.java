package org.Sequential;

import util.LogLevel;
import util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Centroid - a central point of cluster
// Centroids are not actual data points; they are computed as the mean of the coordinates of all the data points in a cluster.

// A cluster is a group of data points that are close to each other based on the distance metric used

// input k - how many centroid and set of points (facilities)

// place centroids at random location
// for each centroid you recompute its position - you take all points that fit into that cluster, and you average them out

public class Kmeans {
    private final int numOfClusters; // Number of clusters
    private final int maxCycles;
    private final List<Facility> facilities; // Input facilities - in our case read from JSON file
    private final List<Cluster> clusters = new ArrayList<>(); // List of clusters
    // The ArrayList class is a Java class that you can use to store lists of objects - dynamic array

    private static final int SEED = 17; // - not helping with my code at all, randomness is a friend - dsa teacher

    public Kmeans(int nc, int mc, List<Facility> f){
        this.numOfClusters = nc;
        this.maxCycles=mc;
        this.facilities=f;
        makeCentroids();
    }

    private void makeCentroids() {
        Random r = new Random(SEED);
        for (int i = 0; i < numOfClusters; i++) {
            int randomIndex = r.nextInt(facilities.size());

            // taking one of facilities to be centroid, to ensure that it is on a map :,)
            // Facility randomFacility = facilities.get(randomIndex);
            Facility randomFacility = facilities.get(i);
            clusters.add(new Cluster(new Centroid(randomFacility.getLongitude(),randomFacility.getLatitude())));
        }
    }

    private double Distance(Facility facility, Centroid centroid) {
        double dx = facility.getLongitude() - centroid.getLongitude();
        double dy = facility.getLatitude() - centroid.getLatitude();

        double distance = Math.sqrt(dx * dx + dy * dy);

        if (Double.isNaN(distance) || Double.isInfinite(distance)){
            Logger.log("Invalid Distance for Facility: (" + facility.getLongitude() + ", " + facility.getLatitude() + "and Centroid: (" + centroid.getLongitude() + ", " + centroid.getLatitude() + ")", LogLevel.Error);
        }

        return distance;
    }

    private void assignFacilitiesToClusters() {
        // start with empty clusters always

        for(int i=0; i< clusters.size(); i++){
            clusters.get(i).clearFacilities();
        }

        for(int i=0; i<facilities.size(); i++){

            Facility facility = facilities.get(i);
            Cluster closestCluster = null;
            double minDistance = Double.MAX_VALUE;

            // Find the cluster with the nearest centroid
            for (int j = 0; j < clusters.size(); j++) {
                Cluster cluster = clusters.get(j);
                double distance = Distance(facility, cluster.getCentroid());
                if (distance < minDistance) {
                    minDistance = distance;
                    closestCluster = cluster;
                }
            }

            // Add the facility to the closest cluster
            if (closestCluster != null) {
                closestCluster.addFacility(facility);
            }else {
                Logger.log("Facility at (" + facility.getLongitude() + ", " + facility.getLatitude() + ") was not assigned to any cluster!", LogLevel.Error);
            }
        }
    }

    // at each cycle, if i understood the algorithm T-T
    private void recalculateCentroids() {
        for (int i = 0; i < clusters.size(); i++) {
            Cluster cluster = clusters.get(i);

            cluster.recalculateCentroid(); // prettier this way
        }
    }

    public List<Cluster> getClusters() {
        return clusters;
    }

    public void run() {
        long startTime = System.currentTimeMillis();

        // the run method is going to work in cycles
        for (int cycle = 0; cycle < maxCycles; cycle++) {
            assignFacilitiesToClusters();
            recalculateCentroids();

            // System.out.println("Cycle " + (cycle + 1));
            for (int i = 0; i < clusters.size(); i++) {
                Cluster cluster = clusters.get(i);
                // needed for checking : )
                //System.out.println("Cluster " + (i + 1) + ": Centroid: (" + cluster.getCentroid().getLongitude() + ", " + cluster.getCentroid().getLatitude() + ")");
            }
        }

        long endTime = System.currentTimeMillis();
        Logger.log("Time for " + maxCycles + " cycles, " + numOfClusters + " clusters is "  + (endTime - startTime) + "ms", LogLevel.Success);
    }

}
