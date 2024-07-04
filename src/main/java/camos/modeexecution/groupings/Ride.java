package camos.modeexecution.groupings;

import camos.modeexecution.Agent;
import camos.modeexecution.Coordinate;
import camos.modeexecution.DirectionType;
import camos.modeexecution.carmodels.Vehicle;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Ride extends Grouping {

    long id;

    static long idcounter;

    List<Stop> extraStops;

    LocalDateTime startTime;

    LocalDateTime endTime;


    public Ride(){
        super();
        this.extraStops = new ArrayList<>();
    }

    public Ride(Coordinate startPosition, Coordinate endPosition, LocalDateTime startTime, LocalDateTime endTime, Vehicle vehicle, Agent driver, DirectionType typeOfRide, List<Agent> agents) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.startTime = startTime;
        this.endTime = endTime;
        this.vehicle = vehicle;
        this.driver = driver;
        this.typeOfGrouping = typeOfRide;
        this.agents = agents;
        this.extraStops = new ArrayList<>();
        this.id = ++idcounter;
    }


    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<Stop> getExtraStops() {
        return extraStops;
    }

    public void setExtraStops(List<Stop> extraStops) {
        this.extraStops = extraStops;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public static long getIdcounter() {
        return idcounter;
    }

    public static void setIdcounter(long idcounter) {
        Ride.idcounter = idcounter;
    }

}
