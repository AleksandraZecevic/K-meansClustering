package basics;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import modes.Kmeans;
import modes.KmeansParallel;
import org.jxmapviewer.JXMapKit;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import util.LogLevel;
import util.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MapPanelJustGermany extends JPanel {
    private JXMapKit mapKit;
    private static final int SEED = 17;

    private int numFacilities, numClusters, numCycles;
    private boolean isParallel;

    private boolean isDistributed;

    public MapPanelJustGermany(int numSites, int numC, int numCy, boolean p, boolean d) {
        this.numFacilities = numSites;
        this.numClusters = numC;
        this.numCycles = numCy;
        this.isParallel = p;
        this.isDistributed = d;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(800, 600));

        initializeMap();
        initializeClustering();

        setVisible(true);
    }

    private void initializeMap() {
        Logger.log("MapFrameJustGermany - making of map ");
        mapKit = new JXMapKit();

        OSMTileFactoryInfo osmInfo = new OSMTileFactoryInfo();
        mapKit.setTileFactory(new DefaultTileFactory(osmInfo));

        // Germany
        GeoPosition initialLocation = new GeoPosition(52.52, 13.4050);
        mapKit.setCenterPosition(initialLocation);

        mapKit.setZoom(5); // smaller the number , more zoomed it is

        add(mapKit, BorderLayout.CENTER);

        add(mapKit);
        Logger.log("MapFrameJustGermany - map made ", LogLevel.Success);
    }

    private void initializeClustering() {
        ObjectMapper objectMapper = new ObjectMapper(); // needed for JSON FILE

        try {
            // must be in try and catch because of reading from JSON file "readValue"
            // PROVIDED GERMANY POINTS
            // path name should be relative and not absolute - changed to "germany/germany.json"
            List<Facility> allFacilities = objectMapper.readValue(
                    new File("germany/germany.json"),
                    new TypeReference<List<Facility>>() {});

            List<Facility> facilities = getFacilities(allFacilities, numFacilities);

            if (isDistributed) {
                Logger.log("MapFrameJustGermany - Running in DISTRIBUTED mode");
                try {
                    String classpath = "C:\\Users\\PC-2\\Desktop\\K-meansClustering\\Student_89231025\\target\\classes;" +
                            "C:\\Users\\PC-2\\Downloads\\mpj-v0_44\\lib\\mpi.jar;" +
                            "C:\\Users\\PC-2\\Downloads\\mpj-v0_44\\lib\\mpj.jar;" +
                            "C:\\Users\\PC-2\\.m2\\repository\\com\\fasterxml\\jackson\\core\\jackson-annotations\\2.13.3\\jackson-annotations-2.13.3.jar;" +
                            "C:\\Users\\PC-2\\.m2\\repository\\com\\fasterxml\\jackson\\core\\jackson-core\\2.13.3\\jackson-core-2.13.3.jar;" +
                            "C:\\Users\\PC-2\\.m2\\repository\\com\\fasterxml\\jackson\\core\\jackson-databind\\2.13.3\\jackson-databind-2.13.3.jar";

                    String jsonPath = "C:/Users/PC-2/Desktop/K-meansClustering/germany/germany.json";

                    String command = "\"C:\\Users\\PC-2\\Downloads\\mpj-v0_44\\bin\\mpjrun.bat\" -np 4 -classpath \"" +
                            classpath + "\" modes.DistributedMain " +
                            numFacilities + " " + numClusters + " " + numCycles + " \"" + jsonPath + "\"";


                    System.out.println("Command to run: " + command);

                    Process process = Runtime.getRuntime().exec(command);

                    // Optionally read process output or errors
                  /*  new Thread(() -> {
                        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                Logger.log("[MPI] " + line, LogLevel.Status);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();*/
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            BufferedReader reader = null;
                            try {
                                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    Logger.log("[MPI] " + line, LogLevel.Status);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                if (reader != null) {
                                    try {
                                        reader.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }).start();


                    int exitCode = process.waitFor();
                    Logger.log("Distributed job finished with exit code " + exitCode, LogLevel.Success);

                    // Here you would load and display results (e.g. from a file generated by DistributedMain)
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Failed to run distributed job: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else if (isParallel) {
                Logger.log("MapFrameJustGermany - Running in PARALLEL mode");
                KmeansParallel kmeans = new KmeansParallel(numClusters, numCycles, facilities);
                kmeans.run();

                mapKit.getMainMap().setOverlayPainter(new Painter<JComponent>() {
                    @Override
                    public void paint(Graphics2D g, JComponent jComponent, int width, int height) {
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        List<Cluster> clusters = kmeans.getClusters();

                        for (int i = 0; i < clusters.size(); i++) {
                            Cluster cluster = clusters.get(i);
                            Color clusterColor = getColorForCluster(i);

                            for (Facility facility : cluster.getFacilities()) {
                                drawPoint(g, mapKit, facility, clusterColor);
                            }

                            drawCentroid(g, mapKit, cluster.getCentroid(), clusterColor);
                        }
                    }
                });
            } else {
                Logger.log("MapFrameJustGermany - Running in SEQUENTIAL mode");
                Kmeans kmeans = new Kmeans(numClusters, numCycles, facilities);
                kmeans.run();

                mapKit.getMainMap().setOverlayPainter(new Painter<JComponent>() {
                    @Override
                    public void paint(Graphics2D g, JComponent jComponent, int width, int height) {
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        List<Cluster> clusters = kmeans.getClusters();

                        for (int i = 0; i < clusters.size(); i++) {
                            Cluster cluster = clusters.get(i);
                            Color clusterColor = getColorForCluster(i);

                            for (Facility facility : cluster.getFacilities()) {
                                drawPoint(g, mapKit, facility, clusterColor);
                            }

                            drawCentroid(g, mapKit, cluster.getCentroid(), clusterColor);
                        }
                    }
                });
            }

        } catch (StreamReadException | DatabindException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


    private static List<Facility> getFacilities(List<Facility> allFacilities, int numFacilities) {
        List<Facility> facilities = new ArrayList<>();

        if(numFacilities<= allFacilities.size()){
            facilities.addAll(allFacilities.subList(0,numFacilities));
        }else{
            Logger.log("MapFrameJustGermany - taking random points in Germany " , LogLevel.Status);
            facilities.addAll(allFacilities);

            // generating random ones : (
            Random r = new Random(SEED);

            // Generate additional facilities within Germany
            for (int i = allFacilities.size(); i < numFacilities; i++) {
                // Pick two random facilities from the list
                Facility facility1 = allFacilities.get(r.nextInt(allFacilities.size()));
                Facility facility2 = allFacilities.get(r.nextInt(allFacilities.size()));

                // Calculate a random point between the two facilities
                double newLatitude = facility1.getLatitude() + (facility2.getLatitude() - facility1.getLatitude()) * r.nextDouble();
                double newLongitude = facility1.getLongitude() + (facility2.getLongitude() - facility1.getLongitude()) * r.nextDouble();
                int newCapacity = (facility1.getCapacity() + facility2.getCapacity()) / 2; // Average capacity

                // Create a new facility with the interpolated values
                String newName = "RandomFacility_" + (i - allFacilities.size());
                facilities.add(new Facility(newName, newLongitude, newLatitude, newCapacity));
            }

        }

        return facilities;
    }


    private static void drawCentroid(Graphics2D g, JXMapKit mapKit, Centroid centroid, Color color) {
        GeoPosition position = new GeoPosition(centroid.getLatitude(), centroid.getLongitude());
        Point2D point = mapKit.getMainMap().convertGeoPositionToPoint(position);

        int pointSize = 12;
        g.setColor(color.darker());
        g.fillOval((int) point.getX() - pointSize / 2, (int) point.getY() - pointSize / 2, pointSize, pointSize);
        g.setColor(Color.BLACK); // border
        g.drawOval((int) point.getX() - pointSize / 2, (int) point.getY() - pointSize / 2, pointSize, pointSize);
    }

    private static void drawPoint(Graphics2D g, JXMapKit mapKit, Facility facility, Color color){
        GeoPosition position = new GeoPosition(facility.getLatitude(), facility.getLongitude());
        Point2D point = mapKit.getMainMap().convertGeoPositionToPoint(position);

        int pointSize =  3; // Size of the point
        g.setColor(color);
        g.fillOval((int) point.getX() - pointSize / 2, (int) point.getY() - pointSize / 2, pointSize, pointSize);
    }

    private static Color getColorForCluster(int index) {
        Random r = new Random(SEED + index);

        int red = r.nextInt(256);
        int blue = r.nextInt(256);
        int green = r.nextInt(256);

        return new Color(red, green, blue);
    }
}
