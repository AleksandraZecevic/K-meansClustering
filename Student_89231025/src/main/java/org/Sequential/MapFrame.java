package org.Sequential;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class MapFrame extends JFrame {
    private JXMapKit mapKit;
    private static final int SEED = 69;

    private int numFacilities, numClusters, numCycles;
    public MapFrame(int numSites, int numC, int numCy) {
        super("K-means clustering");
        setSize(800, 600);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        numFacilities = numSites;
        numClusters = numC;
        numCycles = numCy;

        initializeMap();
        initializeClustering();

        setVisible(true);
    }

    private void initializeMap() {
        Logger.log("MapFrame - making of map ");
        mapKit = new JXMapKit();

        OSMTileFactoryInfo osmInfo = new OSMTileFactoryInfo();
        mapKit.setTileFactory(new DefaultTileFactory(osmInfo));

        // Germany
        GeoPosition initialLocation = new GeoPosition(52.52, 13.4050);
        // GeoPosition icelandPosition = new GeoPosition(64.1355, -19.8211);
        mapKit.setCenterPosition(initialLocation);

        mapKit.setZoom(5); // smaller the number , more zoomed it is

        add(mapKit);
        Logger.log("MapFrame - map made ", LogLevel.Success);
    }

    private void initializeClustering() {

        ObjectMapper objectMapper = new ObjectMapper(); // needed for JSON FILE

        // File file = new File("C:\\Users\\PC-2\\Desktop\\Points.json");
        // "I expect this JSON to be a list, and each item in the list should be a Facility."

        try {
            // must be in try and catch because of reading from JSON file "readValue"
            // PROVIDED GERMANY POINTS

            //File path should be relative and not absolute - "germany/germany.json" Alja Eremic
            List<Facility> allFacilities = objectMapper.readValue(new File("germany/germany.json"), new TypeReference<List<Facility>>() {});

            // SMALLER NUM OF FACILITIES FOR TESTING -
            // NOT NEEDED ANYMORE, 11 000> MORE GERMANY IS BROKEN
            //List<Facility> allFacilities = objectMapper.readValue(new File("C:\\Users\\PC-2\\Desktop\\89231025_K-meansClustering\\germany\\SmallNumOfFacilitiesForTesting.json"), new TypeReference<List<Facility>>() {});
            //List<Facility> facilities = objectMapper.readValue(file, objectMapper.getTypeFactory().constructCollectionType(List.class, Facility.class));

            /* Scanner sc = new Scanner(System.in);
            System.out.println("Number of accumulation sites: ");
            int numFacilities = sc.nextInt();

            /*int numClusters = 15;
            int maxCycles = 10;*/

            /*System.out.println("Number of clusters: " );
            numClusters = sc.nextInt();
            System.out.println("Number of cycles: ");
            numCycles = sc.nextInt(); */


            List<Facility> facilities = getFacilities(allFacilities, numFacilities);

            Kmeans kmeans = new Kmeans(numClusters, numCycles, facilities);
            kmeans.run();


            mapKit.getMainMap().setOverlayPainter(new Painter<JComponent>() { // retrieves the main map component from the JXMapkit
                // automatically got it with JComponent
                @Override
                public void paint(Graphics2D g, JComponent jComponent, int width, int height) {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // for smoother resolution - tbh chat gave me this line

                    List<Cluster> clusters = kmeans.getClusters();

                    for(int i=0; i<clusters.size(); i++){
                        Cluster cluster = clusters.get(i);
                        Color clusterColor = getColorForCluster(i);

                        for(int j=0; j<cluster.getFacilities().size(); j++){
                            Facility facility = cluster.getFacilities().get(j);
                            drawPoint(g, mapKit, facility, clusterColor);
                        }

                        // same color as cluster color

                        drawCentroid(g, mapKit, cluster.getCentroid(), clusterColor);
                    }

                }
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static List<Facility> getFacilities(List<Facility> allFacilities, int numFacilities) {
        List<Facility> facilities = new ArrayList<>();

        if(numFacilities<= allFacilities.size()){
            facilities.addAll(allFacilities.subList(0,numFacilities));
        }else{
            Logger.log("MapFrame - taking random points in Europe ");
            facilities.addAll(allFacilities);

            // generating random ones : (
            Random r = new Random(SEED);

            facilities.addAll(allFacilities);

            Random random = new Random(SEED);

            int minCapacity = allFacilities.stream().mapToInt(Facility::getCapacity).min().orElse(1); //
            int maxCapacity = allFacilities.stream().mapToInt(Facility::getCapacity).max().orElse(1000);

            int k = 0;

            for (int i = allFacilities.size(); i<numFacilities; i++){ // starting from end of json file and keep going
                double longitude = randomLongitude();
                double latitude = randomLatitude();
                while (isInEurope(latitude,longitude)==false){
                    longitude = randomLongitude();
                    latitude = randomLatitude();
                }
                int capacity = minCapacity + r.nextInt() * (maxCapacity-minCapacity);
                String name = "RandomFacility_" + k;
                k++;
                facilities.add(new Facility(name,longitude, latitude, capacity));
            }

            k = 0; // just in case to restart
        }

        return facilities;
    }

    // Europe is a continent on Earth at
    // latitude 60°00′00.00″ North
    // longitude 15°00′00.00″ East

    // Europe spans roughly these coordinates:
    //Latitude: Between 35.0° and 71.0° (southernmost Crete, Greece, to northern Norway).
    //Longitude: Between -25.0° and 60.0° (Azores in Portugal to the Ural Mountains in Russia).
    private static double randomLatitude() {
        Random r = new Random();
        double latitude = 35.0 + (71.0 - 35.0) * r.nextDouble();
        return latitude;
    }

    private static double randomLongitude() {
        Random r = new Random();
        double longitude = -25.0 + (60.0 - (-25.0)) * r.nextDouble();
        return longitude;
    }

    public static boolean isInEurope(double latitude, double longitude) {

        // Logger.log("MapFrame - taking random points in Europe ");
        // Adjust the point if it’s outside land boundaries

        // latitude is x
        // longitude is y

        // Westernmost point of Europe (Portugal, Spain)
        if (longitude < -25.0 || longitude > 60.0) return false;  // Outside the western and eastern boundary

        // Southernmost point of Europe (Greece, Spain)
        if (latitude < 35.0 || latitude > 71.0) return false;  // Outside the southern and northern boundary


        // Iberian Peninsula (Spain and Portugal)
        //              35.0                                    -10.0                 4.0
        if (latitude >= 30.0 && latitude <= 44.0 && longitude >= -9.0 && longitude <= 0.0) return true;

        // France and Benelux (Belgium, Netherlands, Luxembourg) -5.0
        if (latitude > 43.0 && latitude <= 51.0 && longitude >= 0.0 && longitude <= 7.0) return true;

        // Italy (including Sicily and Sardinia)
        // if (latitude >= 36.0 && latitude <= 47.0 && longitude >= 6.0 && longitude <= 18.0) return true;

        // Spain and Southern France (including the Pyrenees)     -10.0             5.0
        //if (latitude >= 42.0 && latitude <= 44.0 && longitude >= -5.0 && longitude <= 0.0) return true;

        // Balkans (including Croatia, Bosnia, Serbia, etc.)
        if (latitude >= 41.0 && latitude <= 48.0 && longitude >= 13.0 && longitude <= 30.0) return true;

        // Scandinavia (Norway, Sweden, Denmark)
        //                                  71.0
        if (latitude >= 55.0 && latitude <= 60.0 && longitude >= 10.0 && longitude <= 30.0) return true;

        // Eastern Europe (Ukraine, Moldova, Belarus, Western Russia)
        if (latitude >= 48.0 && latitude <= 60.0 && longitude >= 23.0 && longitude <= 40.0) return true;

        // Germany, Poland, Czech Republic, Slovakia, etc.
        if (latitude >= 47.0 && latitude <= 55.0 && longitude >= 10.0 && longitude <= 20.0) return true;

        // Britain and Ireland
        //              50.0                                     -10.0
        //if (latitude >= 50.0 && latitude <= 58.0 && longitude >= -5.0 && longitude <= 2.0) return true;
        if (latitude >= 50.0 && latitude <= 58.0 && longitude >= -10.0 && longitude <= 2.0) {
            return isInUK(latitude,longitude);
        }

        // Finland and the Baltic States (Estonia, Latvia, Lithuania)
       // if (latitude >= 55.0 && latitude <= 60.0 && longitude >= 20.0 && longitude <= 30.0) return true;
        // Finland (mainland only)
        if (latitude >= 60.0 && latitude <= 70.0 && longitude >= 20.0 && longitude <= 32.0) return true;

        // Switzerland, Austria, and parts of Central Europe
        if (latitude >= 45.0 && latitude <= 50.0 && longitude >= 5.0 && longitude <= 15.0) return true;

        // The Alps (France, Italy, Switzerland, Austria)
        if (latitude >= 44.0 && latitude <= 47.0 && longitude >= 5.0 && longitude <= 12.0) return true;

        // Greece and the Aegean Sea
        //if (latitude >= 36.0 && latitude <= 42.0 && longitude >= 20.0 && longitude <= 30.0) return true;

        // Eastern Europe (Ukraine, Belarus, etc.)
        if (latitude >= 48.0 && latitude <= 60.0 && longitude >= 23.0 && longitude <= 40.0) return true;

        // Italy (mainland only, excluding Sicily and Sardinia)
        if (latitude >= 37.0 && latitude <= 46.0 && longitude >= 6.0 && longitude <= 15.0) return true;

        // Greece (mainland only, excluding islands)
        if (latitude >= 38.0 && latitude <= 41.0 && longitude >= 20.0 && longitude <= 25.0) return true;

        // Iceland (mainland)
         if (latitude >= 63.0 && latitude <= 67.0 && longitude >= -25.0 && longitude <= -13.0) return true;

        return false;

    }

    private static boolean isInUK(double latitude, double longitude) {
        double[][] ukPolygon = {
                // Great Britain - outline (clockwise)
                {49.9, -7.6}, {50.1, 1.8}, {55.3, 1.7}, {58.6, -4.0},
                {57.8, -5.7}, {55.8, -6.4}, {54.6, -5.6}, {51.6, -3.0},
                {50.0, -5.3}, {49.9, -7.6}
        };

        return isPointInPolygon(latitude, longitude, ukPolygon);
    }

    // Utility to check if a point is inside a polygon (Ray-Casting algorithm)
    private static boolean isPointInPolygon(double latitude, double longitude, double[][] polygon) {
        int intersectCount = 0;
        for (int i = 0; i < polygon.length - 1; i++) {
            double[] v1 = polygon[i];
            double[] v2 = polygon[i + 1];

            // Make sure to compare latitude to y (longitude to x in your context)
            if (((v1[1] > longitude) != (v2[1] > longitude)) &&
                    (latitude < (v2[0] - v1[0]) * (longitude - v1[1]) / (v2[1] - v1[1]) + v1[0])) {
                intersectCount++;
            }
        }
        return (intersectCount % 2) == 1; // Odd intersections mean the point is inside
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
        int red = (index * 50 + 100) % 255;
        int green = (index * 100 + 150) % 255;
        int blue = (index * 150 + 200) % 255;
        return new Color(red, green, blue);
    }
}
