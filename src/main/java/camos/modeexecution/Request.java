package camos.modeexecution;

import camos.GeneralManager;
import camos.modeexecution.groupings.Match;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

public class Request implements Comparable{

    long id;
    Agent agent;
    DirectionType directionType;
    LocalDateTime requestTime;
    LocalDateTime favoredArrivalTime;
    TimeInterval arrivalInterval;
    LocalDateTime favoredDepartureTime;
    TimeInterval departureInterval;
    String homePLZ;
    String uniPLZ;
    Coordinate homePosition;
    Coordinate dropOffPosition;


    public Request(Agent agent, DirectionType directionType, String homePLZ, String uniPLZ, Coordinate homePosition, Coordinate dropOffPosition) {
        this.agent = agent;
        this.directionType = directionType;
        this.homePLZ = homePLZ;
        this.uniPLZ = uniPLZ;
        this.homePosition = homePosition;
        this.dropOffPosition = dropOffPosition;
    }


    public Request(Request request) {
        this.agent = request.agent;
        this.directionType = request.directionType;
        this.requestTime = request.requestTime;
        this.favoredArrivalTime = request.favoredArrivalTime;
        this.arrivalInterval = request.getArrivalInterval();
        this.favoredDepartureTime = request.favoredDepartureTime;
        this.departureInterval = request.getDepartureInterval();
        this.homePLZ = request.homePLZ;
        this.uniPLZ = request.uniPLZ;
        this.homePosition = request.homePosition;
        this.dropOffPosition = request.dropOffPosition;
    }

    public Request() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public DirectionType getRequesttype() {
        return directionType;
    }

    public void setRequesttype(DirectionType directionType) {
        this.directionType = directionType;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }

    public LocalDateTime getFavoredArrivalTime() {
        return favoredArrivalTime;
    }

    public void setFavoredArrivalTime(LocalDateTime favoredArrivalTime) {
        this.favoredArrivalTime = favoredArrivalTime;
    }

    public LocalDateTime getFavoredDepartureTime() {
        return favoredDepartureTime;
    }

    public void setFavoredDepartureTime(LocalDateTime favoredDepartureTime) {
        this.favoredDepartureTime = favoredDepartureTime;
    }

    public String getHomePLZ() {
        return homePLZ;
    }

    public void setHomePLZ(String homePLZ) {
        this.homePLZ = homePLZ;
    }

    public String getUniPLZ() {
        return uniPLZ;
    }

    public void setUniPLZ(String uniPLZ) {
        this.uniPLZ = uniPLZ;
    }

    public Coordinate getHomePosition() {
        return homePosition;
    }

    public void setHomePosition(Coordinate homePosition) {
        this.homePosition = homePosition;
    }

    public Coordinate getDropOffPosition() {
        return dropOffPosition;
    }

    public void setDropOffPosition(Coordinate dropOffPosition) {
        this.dropOffPosition = dropOffPosition;
    }

    public TimeInterval getArrivalInterval() {
        return arrivalInterval;
    }

    public void setArrivalInterval(TimeInterval arrivalInterval) {
        this.arrivalInterval = arrivalInterval;
    }

    public TimeInterval getDepartureInterval() {
        return departureInterval;
    }

    public void setDepartureInterval(TimeInterval departureInterval) {
        this.departureInterval = departureInterval;
    }

    //TODO
    @Override
    public int compareTo(@NotNull Object o) {
        if(GeneralManager.compareMatch!=null){
            Request request = (Request) o;
            if(Match.similarToRequest(request, GeneralManager.compareMatch)<Match.similarToRequest(this, GeneralManager.compareMatch)){
                return -1;
            }else if(Match.similarToRequest(request, GeneralManager.compareMatch)>Match.similarToRequest(this, GeneralManager.compareMatch)){
                return 1;
            }
            return 0;
        }
        return 0;
    }
}
