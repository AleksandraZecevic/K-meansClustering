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

        // Declare and initialize parameters with default values
        int numFacilities = 100;  // default number of facilities
        int numClusters = 10;     // default number of clusters
        int maxCycles = 100;      // default max cycles

        // Parse args if provided
        if (args.length >= 3) {
            try {
                numFacilities = Integer.parseInt(args[0]);
                numClusters = Integer.parseInt(args[1]);
                maxCycles = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                if (rank == 0) {
                    System.err.println("Invalid input arguments, using default parameters.");
                }
            }
        }

        List<Facility> facilities = null;

        if (rank == 0) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                facilities = mapper.readValue(
                        new File("germany/germany.json"),
                        new TypeReference<List<Facility>>() {});

                // Trim or extend facilities to requested number
                if (facilities.size() > numFacilities) {
                    facilities = facilities.subList(0, numFacilities);
                }

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
