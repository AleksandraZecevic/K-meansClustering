package tryEurope;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.*;
import util.LogLevel;
import util.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EuropeChecker {

    // MORE RESEARCH NEEDED, IGNORE IT DOESN'T WORK
    // honestly had a fight with AI cause of this one

    private static final String countryLinesPath = "C:\\Users\\PC-2\\Desktop\\89231025_K-meansClustering\\tryEurope\\countryLines.json";
    private static final String landPath = "C:\\Users\\PC-2\\Desktop\\89231025_K-meansClustering\\tryEurope\\land.json";

    private static final List<List<double[]>> europePolygons = new ArrayList<>();
    private static final GeometryFactory geometryFactory = new GeometryFactory();


    public EuropeChecker(){
        Logger.log("EuropeChecker");
        createMainlanPolygon(countryLinesPath);
    }

    //{"type":"FeatureCollection","features":
    // [{"type":"Feature","properties":{"FID_ne_10m":39,"scalerank":1,"featurecla":

    private static void createMainlanPolygon(String countryPath) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode countryLines = objectMapper.readTree(new File(countryPath));

            if (countryLines != null && countryLines.has("features")) {
                JsonNode features = countryLines.get("features");
                Logger.log("Number of features in JSON: " + features.size(), LogLevel.Debug);

                for (int i = 0; i < features.size(); i++) {
                    JsonNode feature = features.get(i);

                    if (isMainlandEurope(feature)) {
                        List<double[]> polygon = extractPolygon(feature);

                        if (!polygon.isEmpty()) {
                            europePolygons.add(polygon);
                            Logger.log("Added polygon with " + polygon.size() + " points.", LogLevel.Debug);
                        } else {
                            Logger.log("Extracted polygon is empty.", LogLevel.Warn);
                        }
                    } else {
                        Logger.log("Feature skipped: does not match mainland Europe criteria.", LogLevel.Debug);
                    }
                }

                Logger.log("Loaded " + europePolygons.size() + " mainland Europe polygons.", LogLevel.Debug);
            } else {
                Logger.log("Invalid countryLines JSON structure: missing 'features'", LogLevel.Error);
            }

        } catch (IOException e) {
            Logger.log("Error reading JSON: " + e.getMessage(), LogLevel.Error);
            throw new RuntimeException("Error reading JSON: " + e.getMessage(), e);
        }
    }

    // "International boundary (verify)"
    // ,"note_":null,"name":null,"comment":null,"adm0_usa":1,"adm0_left":"Sweden","adm0_right":"Norway","adm0_a3_l":"SWE","adm0_a3_r":"NOR","sov_a3_l":"SWE","sov_a3_r":"NOR","type":"Water Indicator","labelrank":2},
    private static boolean isMainlandEurope(JsonNode feature) {
        JsonNode properties = feature.get("properties");

        if (properties != null) {
            String countryCodeLeft = properties.has("adm0_a3_l") ? properties.get("adm0_a3_l").asText() : null;
            String countryCodeRight = properties.has("adm0_a3_r") ? properties.get("adm0_a3_r").asText() : null;

            Set<String> mainlandEuropeCountries = Set.of(
                    "FRA", "DEU", "POL", "ITA", "ESP", "SWE", "FIN", "NOR", "NLD", "BEL",
                    "AUT", "CHE", "CZE", "HUN", "DNK", "ROM", "BGR", "GRC", "SVK", "SVN",
                    "PRT", "LUX", "LTU", "LVA", "EST", "HRV", "BIH", "SRB", "MNE", "ALB", "MKD"
            );

            // Return true if either country code is in the list of mainland Europe countries
            if ((countryCodeLeft != null && mainlandEuropeCountries.contains(countryCodeLeft)) ||
                    (countryCodeRight != null && mainlandEuropeCountries.contains(countryCodeRight))) {
                return true;
            }
        }

        Logger.log("Missing or invalid 'adm0_a3_l' or 'adm0_a3_r' in properties for feature: " + feature, LogLevel.Warn);
        return false;
    }





    // "geometry":{"type":"LineString",
    // "coordinates":[[11.437510613506703,58.99172086270566],[11.400936726000083,59.02590728800003],[11.376752156000094,59.07882395500009],[11.358251994000085,59.10026967400002],[11.31680749500012,59.10647084600002],[11.259343302000104,59.10409373000006],[11.20353275600013,59.099132793000095],[11.165395548000077,59.08864247700005],[11.132012573000111,59.07277781200004],[11.117233113000083,59.05272735699999],[11.10193688900003,59.00254954000006],[11.089741251000106,58.981310527000105],[11.054291219000078,58.96126007100004],[10.907013387000063,58.93692047200004]]}},
    private static List<double[]> extractPolygon(JsonNode geometry) {
        List<double[]> polygon = new ArrayList<>();

        if (geometry != null && geometry.has("geometry") && geometry.get("geometry").has("coordinates")) {
            JsonNode coordinates = geometry.get("geometry").get("coordinates");

            // Handle MultiLineString or LineString geometries
            if (geometry.get("geometry").get("type").asText().equals("MultiLineString")) {
                for (JsonNode ring : coordinates) {
                    extractCoordinates(ring, polygon);
                }
            } else if (geometry.get("geometry").get("type").asText().equals("LineString")) {
                extractCoordinates(coordinates, polygon);
            } else {
                Logger.log("Unsupported geometry type: " + geometry.get("geometry").get("type").asText(), LogLevel.Warn);
            }
        } else {
            Logger.log("Invalid or missing geometry in JSON: " + geometry, LogLevel.Warn);
        }

        Logger.log("Extracted " + polygon.size() + " points from polygon.", LogLevel.Debug);
        return polygon;
    }

    private static void extractCoordinates(JsonNode node, List<double[]> polygon) {
        if (node.isArray()) {
            for (JsonNode point : node) {
                if (point.isArray() && point.size() == 2) {
                    double longitude = point.get(0).asDouble();
                    double latitude = point.get(1).asDouble();
                    polygon.add(new double[]{latitude, longitude});
                }
            }
        }
    }



    public boolean NewIsInEurope(double latitude, double longitude) {
        org.locationtech.jts.geom.Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));

        for (List<double[]> polygon : europePolygons) {
            Coordinate[] coordinates = new Coordinate[polygon.size() + 1];

            for (int i = 0; i < polygon.size(); i++) {
                double[] coo = polygon.get(i);
                coordinates[i] = new Coordinate(coo[1], coo[0]); // Ensure order is [longitude, latitude]
            }

            // Close the polygon by adding the first point as the last point
            coordinates[polygon.size()] = coordinates[0];

            LinearRing ring;
            try {
                ring = geometryFactory.createLinearRing(coordinates);
            } catch (IllegalArgumentException e) {
                Logger.log("Invalid LinearRing for polygon: " + polygon, LogLevel.Error);
                continue; // Skip invalid polygons
            }

            Polygon justPolygon = geometryFactory.createPolygon(ring);

            if (justPolygon.contains(point)) {
                return true; // Point is inside this polygon
            }
        }

        return false; // Point is not inside any polygon
    }

}
