import mpi.*;

public class MPJTest {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting MPI Init");
        MPI.Init(args);
        System.out.println("MPI Initialized");

        int me = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        System.out.println("Hello from process " + me + " out of " + size);
        System.out.flush();

        System.out.println("Finalizing MPI");
        MPI.Finalize();
        System.out.println("MPI Finalized");
    }
}
