package basics;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

// Serializable is a marker interface in Java.
// It doesn’t have any methods you need to implement — it just marks a class as being able to be converted into a stream of bytes**, which can then be saved to a file, sent over a network, or used for inter-process communication.

public class Facility implements Serializable{

    private static final long serialVersionUID = 1L;
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
