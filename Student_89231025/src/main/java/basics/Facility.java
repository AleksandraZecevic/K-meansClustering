package basics;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Facility {
    // {"name":"Lindenau","capacity":13.94121244509,"la":"51.4","lo":"13.7333"}
    private String name;
    private int capacity;

    @JsonProperty("lo")       // in JSON file it is like lo
    private double longitude; // vertical lines, y osa

    @JsonProperty("la")
    private double latitude; // horizontal lines, x osa

    public Facility() {
        // empty because we are reading info from JSON file
    }

    // dunno if I will need it in future
    // needed for random facilities lol

    public Facility(String n, double lg, double lt, int c){
        this.name = n;
        this.longitude=lg;
        this.latitude=lt;
        this.capacity=c;
    }


    // GETTERS
    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getName() {
        return name;
    }

}
