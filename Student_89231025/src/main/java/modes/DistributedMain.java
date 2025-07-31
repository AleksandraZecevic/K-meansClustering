package modes;

import basics.Cluster;
import basics.Facility;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mpi.MPI;
import util.LogLevel;
import util.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Arrays;

public class DistributedMain {
    public static void main(String[] args) {
        /* skipping first 3 cause mpi sends these
        Arguments received:
            0: '2'
            1: '4'
            2: 'smpdev'
        */

        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();

        int numFacilities = 100;
        int numClusters = 10;
        int maxCycles = 100;
        String jsonPath = "germany/germany.json";

        int userArgsStartIndex = 3; // skip MPJ internal args

        if (rank == 0) {
           /* System.out.println("Arguments received:");
            for (int i = 0; i < args.length; i++) {
                System.out.println(i + ": '" + args[i] + "'");
            }*/

            int userArgsCount = args.length - userArgsStartIndex;
            try {
                if (userArgsCount >= 4) {
                    numFacilities = Integer.parseInt(args[userArgsStartIndex]);
                    numClusters = Integer.parseInt(args[userArgsStartIndex + 1]);
                    maxCycles = Integer.parseInt(args[userArgsStartIndex + 2]);
                    jsonPath = args[userArgsStartIndex + 3];
                } else if (userArgsCount == 3) {
                    numFacilities = Integer.parseInt(args[userArgsStartIndex]);
                    numClusters = Integer.parseInt(args[userArgsStartIndex + 1]);
                    maxCycles = Integer.parseInt(args[userArgsStartIndex + 2]);
                } else if (userArgsCount == 1) {
                    jsonPath = args[userArgsStartIndex];
                } else {
                    System.out.println("Not enough user arguments, using defaults.");
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid number format in user arguments, using defaults.");
            }
        }

        // Now broadcast parameters to all ranks
        int[] intParams = new int[3];
        if (rank == 0) {
            intParams[0] = numFacilities;
            intParams[1] = numClusters;
            intParams[2] = maxCycles;
        }
        MPI.COMM_WORLD.Bcast(intParams, 0, 3, MPI.INT, 0);

        // For the string path, broadcast as bytes
        byte[] pathBytes = new byte[256]; // max path length, adjust if needed
        if (rank == 0) {
            byte[] jsonBytes = jsonPath.getBytes();
            System.arraycopy(jsonBytes, 0, pathBytes, 0, jsonBytes.length);
        }
        MPI.COMM_WORLD.Bcast(pathBytes, 0, pathBytes.length, MPI.BYTE, 0);

        // Decode path on all ranks
        jsonPath = new String(pathBytes).trim();

        // Update variables after broadcast
        numFacilities = intParams[0];
        numClusters = intParams[1];
        maxCycles = intParams[2];

        /*
        System.out.println("Rank " + rank + " using parameters:");
        System.out.println("numFacilities = " + numFacilities);
        System.out.println("numClusters = " + numClusters);
        System.out.println("maxCycles = " + maxCycles);
        System.out.println("jsonPath = " + jsonPath);
        */

        List<Facility> facilities = null;
        long startTime = 0;
        if (rank == 0) {
            startTime = System.currentTimeMillis();
            try {
                ObjectMapper mapper = new ObjectMapper();
                File file = new File(jsonPath);
                if (!file.exists()) {
                    System.err.println("File not found: " + file.getAbsolutePath());
                    MPI.Finalize();
                    return;
                }
                facilities = mapper.readValue(file, new TypeReference<List<Facility>>() {});

                if (facilities.size() < numFacilities) {
                    System.err.println("Warning: Only " + facilities.size() + " facilities found. Requested " + numFacilities);
                    // Either throw, or just proceed with fewer
                    numFacilities = facilities.size();
                }
                facilities = facilities.subList(0, numFacilities);


                System.out.println("Loaded " + facilities.size() + " facilities.");
            } catch (Exception e) {
                e.printStackTrace();
                MPI.Finalize();
                return;
            }
        }

        // Broadcast size of the list to all processes
        int[] sizes = new int[1];
        if (rank == 0) {
            sizes[0] = facilities.size();
        }
        MPI.COMM_WORLD.Bcast(sizes, 0, 1, MPI.INT, 0);
        int totalFacilities = sizes[0];

        // Broadcast facility data to all processes
        Facility[] facilityArray = new Facility[totalFacilities];
        if (rank == 0) {
            facilityArray = facilities.toArray(new Facility[0]);
        }
        MPI.COMM_WORLD.Bcast(facilityArray, 0, totalFacilities, MPI.OBJECT, 0);

        if (rank != 0) {
            facilities = Arrays.asList(facilityArray);
        }

        // Run distributed Kmeans clustering
        KmeansDistributed distributed = new KmeansDistributed(numClusters, maxCycles, facilities);
        distributed.runDistributed();

      //  System.out.println("Rank = " + rank);
        List<Cluster> finalClusters = distributed.getClusters();
     //   System.out.println("finalClusters is " + (finalClusters == null ? "null" : ("size = " + finalClusters.size())));

        if (rank == 0) {
            long endTime = System.currentTimeMillis();
            Logger.log("Distributed run: Time for " + maxCycles + " cycles, " + numClusters + " clusters is " + (endTime - startTime) + "ms", LogLevel.Success);

            long duration = endTime - startTime;
            // Write to time.txt
            try (PrintWriter out = new PrintWriter("jole/runtime.txt")) {
                out.println(duration);
            } catch (IOException e) {
                System.err.println("Failed to write runtime: " + e.getMessage());
            }

            ObjectMapper mapper = new ObjectMapper();

            System.out.println("Working directory: " + System.getProperty("user.dir"));

            File outputDir = new File("jole");
            if (!outputDir.exists()) {
                boolean created = outputDir.mkdirs();
                if (!created) {
                    System.err.println("Failed to create directory: " + outputDir.getAbsolutePath());
                }
            }

            File outFile = new File(outputDir, "clusters.json");
            System.out.println("Trying to write to: " + outFile.getAbsolutePath());

            // Test creating file manually
            try {
                if (!outFile.exists()) {
                    boolean fileCreated = outFile.createNewFile();
                    System.out.println("File created manually? " + fileCreated);
                    System.out.println("About to write clusters JSON...");
                    mapper.writeValue(outFile, finalClusters);
                    System.out.println("Clusters saved successfully to " + outFile.getAbsolutePath());

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Now write JSON
            try {
                mapper.writeValue(outFile, finalClusters);
                System.out.println("Clusters saved successfully");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        MPI.Finalize();
    }
}
