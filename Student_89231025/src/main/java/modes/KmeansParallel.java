package modes;

import basics.Centroid;
import basics.Cluster;
import basics.Facility;
import util.LogLevel;
import util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KmeansParallel {
    private final int numOfClusters; // Number of clusters
    private final int maxCycles;
    private final List<Facility> facilities; // Input facilities - in our case read from JSON file
    private final List<Cluster> clusters = new ArrayList<>(); // List of clusters

    private static final int SEED = 17;

    public KmeansParallel(int nc, int mc, List<Facility> f){
        this.numOfClusters = nc;
        this.maxCycles=mc;
        this.facilities=f;
    }
    public void run() {
        long startTime = System.currentTimeMillis();

        initializeCentroids();

        for (int cycle = 0; cycle < maxCycles; cycle++) {
            assignFacilitiesToClustersParallel(cycle);
            recalculateCentroidsParallel();
        }

        long endTime = System.currentTimeMillis();
        Logger.log("Parallel run: Time for " + maxCycles + " cycles, " + numOfClusters + " clusters is " + (endTime - startTime) + "ms", LogLevel.Success);
    }

    private void initializeCentroids() {
        Random r = new Random(SEED);
        clusters.clear();
        for (int i = 0; i < numOfClusters; i++) {
            int randomIndex = r.nextInt(facilities.size());

            // taking one of facilities to be centroid, to ensure that it is on a map :,)
            // Facility randomFacility = facilities.get(randomIndex);
            Facility randomFacility = facilities.get(i);
            clusters.add(new Cluster(new Centroid(randomFacility.getLongitude(),randomFacility.getLatitude())));
        }
    }

    private void assignFacilitiesToClustersParallel(int cycle) {
        if (cycle == 0) Logger.log("assignFacilitiesToClustersParallel - clearing all clusters");

        // Clear all clusters
        for (int i = 0; i < clusters.size(); i++) {
            clusters.get(i).clearFacilities();
        }

        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < facilities.size(); i++) {
            final Facility facility = facilities.get(i); // final cause jdk 8?
            pool.submit(new Runnable() {
                @Override
                public void run() {
                    Cluster closestCluster = null;
                    double minDistance = Double.MAX_VALUE;

                    for (int j = 0; j < clusters.size(); j++) {
                        Cluster cluster = clusters.get(j);
                        double distance = Distance(facility, cluster.getCentroid());
                        if (distance < minDistance) {
                            minDistance = distance;
                            closestCluster = cluster;
                        }
                    }

                    if (closestCluster != null) {
                        synchronized (closestCluster) {
                            closestCluster.addFacility(facility);
                        }
                    } else {
                        Logger.log("Facility at (" + facility.getLongitude() + ", " + facility.getLatitude() + ") was not assigned to any cluster!", LogLevel.Error);
                    }
                }
            });
        }

        pool.shutdown();
        try {
            pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Logger.log("Thread pool interrupted", LogLevel.Error);
        }

       // Logger.log("assignFacilitiesToClustersParallel - assignment complete", LogLevel.Success);
    }

    private double Distance(Facility f, Centroid c) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(c.getLatitude() - f.getLatitude());
        double lonDistance = Math.toRadians(c.getLongitude() - f.getLongitude());
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(f.getLatitude())) * Math.cos(Math.toRadians(c.getLatitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double cVal = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * cVal;
    }

    private void recalculateCentroidsParallel() {
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < clusters.size(); i++) {
            Cluster cluster = clusters.get(i);

            pool.submit(new Runnable() {
                @Override
                public void run() {
                    cluster.recalculateCentroid();
                }
            });
        }

        pool.shutdown();
        try {
            pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Logger.log("Thread pool interrupted", LogLevel.Error);
        }
    }


    private Cluster findNearestCluster(Facility facility) {
        Cluster nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Cluster cluster : clusters) {
            double dist = Distance(facility, cluster.getCentroid());
            if (dist < minDistance) {
                minDistance = dist;
                nearest = cluster;
            }
        }
        return nearest;
    }

    public List<Cluster> getClusters() {
        return clusters;
    }
}
