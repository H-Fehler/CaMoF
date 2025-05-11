package camof.modeexecution.groupings;

import camof.GeneralManager;
import camof.modeexecution.*;
import camof.modeexecution.carmodels.Vehicle;
import camof.modeexecution.mobilitymodels.modehelpers.RouteCalculationHelper;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Match extends Grouping implements Comparable<Match>{

    long id;
    static long idCounter = 0;
    Map<Coordinate,String> differentStops;
    LocalDateTime timeIntervalStart;
    LocalDateTime timeIntervalEnd;


    public Match(List<Agent> agents, Map<Coordinate,String> differentStops, Agent driver, Vehicle vehicle, Coordinate startPosition, Coordinate endPosition, DirectionType typeOfGrouping, LocalDateTime timeIntervalStart, LocalDateTime timeIntervalEnd) {
        this.agents = agents;
        this.differentStops = differentStops;
        this.driver = driver;
        this.vehicle = vehicle;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.typeOfGrouping = typeOfGrouping;
        this.timeIntervalStart = timeIntervalStart;
        this.timeIntervalEnd = timeIntervalEnd;
        this.id = ++idCounter;
    }

    public Match(){

    }


    public Match(Set<Request> requests){
        this.agents = requests.stream().map(Request::getAgent).toList();
        this.differentStops = RouteCalculationHelper.getStopIntervalsFromRequests(requests);
    }

    public Match(Set<Request> requests, Agent driver, Vehicle vehicle, Coordinate startPosition, Coordinate endPosition, DirectionType typeOfGrouping, LocalDateTime timeIntervalStart, LocalDateTime timeIntervalEnd) {
        this.agents = requests.stream().map(Request::getAgent).toList();
        this.differentStops = RouteCalculationHelper.getStopIntervalsFromRequests(requests);
        this.driver = driver;
        this.vehicle = vehicle;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.typeOfGrouping = typeOfGrouping;
        this.timeIntervalStart = timeIntervalStart;
        this.timeIntervalEnd = timeIntervalEnd;
        this.id = ++idCounter;
    }

    public LocalDateTime getTimeIntervalStart() {
        return timeIntervalStart;
    }

    public void setTimeIntervalStart(LocalDateTime timeIntervalStart) {
        this.timeIntervalStart = timeIntervalStart;
    }

    public LocalDateTime getTimeIntervalEnd() {
        return timeIntervalEnd;
    }

    public void setTimeIntervalEnd(LocalDateTime timeIntervalEnd) {
        this.timeIntervalEnd = timeIntervalEnd;
    }

    public Map<Coordinate,String> getDifferentStops() {
        return differentStops;
    }

    public void setDifferentStops(Map<Coordinate,String> differentStops) {
        this.differentStops = differentStops;
    }


    //TODO
    @Override
    public int compareTo(@NotNull Match match) {
        if(GeneralManager.compareRequest!=null){
            Request request = GeneralManager.compareRequest;
            if(Match.similarToRequest(request,match)<Match.similarToRequest(request,this)){
                return -1;
            }else if(Match.similarToRequest(request,match)>Match.similarToRequest(request,this)){
                return 1;
            }
            return 0;
        }
        return 0;
    }

    //TODO
    public static double similarToRequest(Request request, Match match){
        double value = 0;
        Coordinate position;
        TimeInterval requestInterval;
        if(match.getTypeOfGrouping()== DirectionType.DRIVETOUNI){
            position = match.getStartPosition();
            requestInterval = request.getArrivalInterval();
        }else{
            position = match.getEndPosition();
            requestInterval = request.getDepartureInterval();
        }
        value = value - Math.abs(position.getLongitude()-request.getHomePosition().getLongitude());
        value = value - Math.abs(position.getLatitude()-request.getHomePosition().getLatitude());
        value = value - (double) Math.abs(ChronoUnit.HOURS.between(match.getTimeIntervalStart(), requestInterval.getStart())) /100;
        value = value - (double) Math.abs(ChronoUnit.HOURS.between(match.getTimeIntervalEnd(), requestInterval.getEnd())) /100;
        return value;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public static long getIdCounter() {
        return idCounter;
    }

    public static void setIdCounter(long idCounter) {
        Match.idCounter = idCounter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Match match = (Match) o;
        return id == match.id;
    }

}
