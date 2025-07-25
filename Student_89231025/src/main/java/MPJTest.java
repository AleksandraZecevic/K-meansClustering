import mpi.*;

public class MPJTest {
    public static void main(String[] args) throws Exception {
        MPI.Init(args);

        int me = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        System.out.println("Hello from process " + me + " out of " + size);

        MPI.Finalize();
    }
}
