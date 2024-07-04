package camos.modeexecution;

import java.util.Objects;

import com.graphhopper.jsprit.core.problem.Location;

public class Coordinate {

    double longitude;
    double latitude;
    String code;

    public Coordinate(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinate that = (Coordinate) o;
        return Double.compare(longitude, that.longitude) == 0 && Double.compare(latitude, that.latitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(longitude, latitude);
    }


    public static boolean coordinateTheSameAsLocation(Coordinate coordinate, Location location){
        return coordinate.getLongitude()==location.getCoordinate().getX() && coordinate.getLatitude()==location.getCoordinate().getY();
    }

    public static Coordinate locationToCoordinate(Location location){
        return new Coordinate(location.getCoordinate().getX(),location.getCoordinate().getY());
    }

    public static Location coordinateToLocation(Coordinate coordinate){
        return Location.newInstance(coordinate.getLongitude(),coordinate.getLatitude());
    }


}
