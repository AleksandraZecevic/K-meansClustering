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

public class MapFrameJustGermany extends JFrame {
    private JXMapKit mapKit;
    private static final int SEED = 17;

    private int numFacilities, numClusters, numCycles;
    public MapFrameJustGermany(int numSites, int numC, int numCy) {
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
        Logger.log("MapFrameJustGermany - making of map ");
        mapKit = new JXMapKit();

        OSMTileFactoryInfo osmInfo = new OSMTileFactoryInfo();
        mapKit.setTileFactory(new DefaultTileFactory(osmInfo));

        // Germany
        GeoPosition initialLocation = new GeoPosition(52.52, 13.4050);
        // GeoPosition icelandPosition = new GeoPosition(64.1355, -19.8211);
        mapKit.setCenterPosition(initialLocation);

        mapKit.setZoom(5); // smaller the number , more zoomed it is

        add(mapKit);
        Logger.log("MapFrameJustGermany - map made ", LogLevel.Success);
    }

    private void initializeClustering() {

        ObjectMapper objectMapper = new ObjectMapper(); // needed for JSON FILE
        // "I expect this JSON to be a list, and each item in the list should be a Facility."

        try {
            // must be in try and catch because of reading from JSON file "readValue"
            // PROVIDED GERMANY POINTS

            //path name should be relative and not absolute - changed to "germany/germany.json" Alja Eremic
            List<Facility> allFacilities = objectMapper.readValue(new File("germany/germany.json"), new TypeReference<List<Facility>>() {});

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
