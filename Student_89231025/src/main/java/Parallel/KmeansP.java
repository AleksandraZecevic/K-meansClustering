package Parallel;

// cause the classes are in Sequential package, bad organization
import org.Sequential.Cluster;
import org.Sequential.Facility;
import org.Sequential.Centroid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.LogLevel;
import util.Logger;

public class KmeansP {
    private final int numOfClusters; // Number of clusters
    private final int maxCycles;
    private final List<Facility> facilities; // Input facilities - in our case read from JSON file
    private final List<Cluster> clusters = new ArrayList<>(); // List of clusters

    private static final int SEED = 17;

    public KmeansP(int nc, int mc, List<Facility> f){
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


      //  Logger.log("assignFacilitiesToClustersParallel - assigning facilities using parallelStream");

        // parallelStream() uses the source collection's default Spliterator to split the data source to enable parallel execution
        facilities.parallelStream().forEach(facility -> {
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
        });

       // Logger.log("assignFacilitiesToClustersParallel - assignment complete", LogLevel.Success);
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

    private void recalculateCentroidsParallel() {
        int numThreads = Runtime.getRuntime().availableProcessors(); // Or just pick a number manually
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
