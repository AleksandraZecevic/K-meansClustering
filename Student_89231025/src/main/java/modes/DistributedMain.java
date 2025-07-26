package modes;

import basics.Facility;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mpi.MPI;

import java.io.File;
import java.util.List;
import java.util.Arrays;

public class DistributedMain {
    public static void main(String[] args) {
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();

        int numFacilities = 100;  // default
        int numClusters = 10;
        int maxCycles = 100;
        String jsonPath = "germany/germany.json"; // default relative path

        if (args.length >= 3) {
            try {
                numFacilities = Integer.parseInt(args[0]);
                numClusters = Integer.parseInt(args[1]);
                maxCycles = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                if (MPI.COMM_WORLD.Rank() == 0) {
                    System.err.println("Invalid numeric input arguments, using defaults.");
                }
            }
        }

        if (args.length >= 4) {
            jsonPath = args[3];
        }


        List<Facility> facilities = null;

        if (rank == 0) {
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

        MPI.Finalize();
    }
}
