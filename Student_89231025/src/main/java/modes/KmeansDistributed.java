package modes;

import basics.Cluster;
import basics.Facility;
import basics.Centroid;
import mpi.MPI;
import util.LogLevel;
import util.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class KmeansDistributed {
    private final int numOfClusters;
    private final int maxCycles;
    private final List<Facility> facilities;
    private List<Cluster> clusters = new ArrayList<>();

    private static final int SEED = 17;

    public KmeansDistributed(int numClusters, int maxCycles, List<Facility> facilities) {
        this.numOfClusters = numClusters;
        this.maxCycles = maxCycles;
        this.facilities = facilities;
    }

    public void runDistributed() {
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        int totalFacilities = facilities.size();
        int facilitiesPerProc = totalFacilities / size;

        Facility[] allFacilities = new Facility[totalFacilities];
        Facility[] localFacilities = new Facility[facilitiesPerProc];

        if (rank == 0) {
            allFacilities = facilities.toArray(new Facility[0]);
        }

        // Scatter facilities evenly to all processes
        MPI.COMM_WORLD.Scatter(allFacilities, 0, facilitiesPerProc, MPI.OBJECT,
                localFacilities, 0, facilitiesPerProc, MPI.OBJECT, 0);

        if (rank == 0) {
            initializeCentroids();
            Logger.log("Centroids initialized on rank 0", LogLevel.Status);
        }

        // Broadcast initial clusters to all processes
        Cluster[] clusterArray = new Cluster[numOfClusters];
        if (rank == 0) {
            clusterArray = clusters.toArray(new Cluster[0]);
        }
        MPI.COMM_WORLD.Bcast(clusterArray, 0, numOfClusters, MPI.OBJECT, 0);
        clusters = new ArrayList<>(Arrays.asList(clusterArray));

        for (int cycle = 0; cycle < maxCycles; cycle++) {
            Logger.log("Cycle " + cycle + " started on rank " + rank, LogLevel.Status);

            // Local accumulators for sums and counts per cluster
            double[] sumLat = new double[numOfClusters];
            double[] sumLon = new double[numOfClusters];
            int[] counts = new int[numOfClusters];

            // Assign local facilities to nearest clusters and accumulate sums/counts
            for (Facility f : localFacilities) {
                int nearestIdx = findNearestClusterIndex(f);
                sumLat[nearestIdx] += f.getLatitude();
                sumLon[nearestIdx] += f.getLongitude();
                counts[nearestIdx]++;
            }

            // Global accumulators for centroid calculation
            double[] globalLat = new double[numOfClusters];
            double[] globalLon = new double[numOfClusters];
            int[] globalCounts = new int[numOfClusters];

            // Reduce sums and counts from all processes to rank 0
            MPI.COMM_WORLD.Reduce(sumLat, 0, globalLat, 0, numOfClusters, MPI.DOUBLE, MPI.SUM, 0);
            MPI.COMM_WORLD.Reduce(sumLon, 0, globalLon, 0, numOfClusters, MPI.DOUBLE, MPI.SUM, 0);
            MPI.COMM_WORLD.Reduce(counts, 0, globalCounts, 0, numOfClusters, MPI.INT, MPI.SUM, 0);

            // On rank 0, compute new centroids and update clusters
            if (rank == 0) {
                for (int i = 0; i < numOfClusters; i++) {
                    if (globalCounts[i] > 0) {
                        double avgLat = globalLat[i] / globalCounts[i];
                        double avgLon = globalLon[i] / globalCounts[i];
                        clusters.get(i).setCentroid(new Centroid(avgLon, avgLat));
                    }
                }
                clusterArray = clusters.toArray(new Cluster[0]);
            }

            // Broadcast updated clusters to all processes
            MPI.COMM_WORLD.Bcast(clusterArray, 0, numOfClusters, MPI.OBJECT, 0);
            clusters = new ArrayList<>(Arrays.asList(clusterArray));
        }

        if (rank == 0) {
            Logger.log("Distributed K-means completed successfully.", LogLevel.Success);
        }
    }

    // Returns the index of the nearest cluster centroid for the given facility
    private int findNearestClusterIndex(Facility facility) {
        int nearestIndex = -1;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < clusters.size(); i++) {
            Cluster cluster = clusters.get(i);
            double dist = distance(facility, cluster.getCentroid());
            if (dist < minDistance) {
                minDistance = dist;
                nearestIndex = i;
            }
        }
        return nearestIndex;
    }

    private void initializeCentroids() {
        if (facilities.size() < numOfClusters) {
            throw new IllegalArgumentException("Not enough facilities to initialize centroids. " +
                    "Need at least " + numOfClusters + ", but got " + facilities.size());
        }

        Random r = new Random(SEED);
        clusters.clear();
        for (int i = 0; i < numOfClusters; i++) {
            Facility f = facilities.get(i);
            clusters.add(new Cluster(new Centroid(f.getLongitude(), f.getLatitude())));
        }
    }


    private double distance(Facility facility, Centroid centroid) {
        double dx = facility.getLongitude() - centroid.getLongitude();
        double dy = facility.getLatitude() - centroid.getLatitude();
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (Double.isNaN(dist) || Double.isInfinite(dist)) {
            Logger.log("Invalid distance computed between facility and centroid", LogLevel.Error);
        }

        return dist;
    }
}
