package camos;

import camos.modeexecution.Agent;
import camos.modeexecution.Coordinate;
import camos.modeexecution.Request;
import camos.modeexecution.carmodels.Vehicle;

public class AgentWithConstraints extends Agent {

    double willingToWalkInMeters;
    long willingToRideInMinutes;
    long timeIntervalInMinutes;


    public AgentWithConstraints(Agent agent){
        super(agent.getId(), agent.getHomePosition(), agent.getCar(), agent.getRequest(), agent.isWillingToUseAlternatives());
    }

    public AgentWithConstraints(Agent agent, long timeInterval, double willingToWalkInMeters){
        super(agent.getId(), agent.getHomePosition(), agent.getCar(), agent.getRequest(), agent.isWillingToUseAlternatives());
        this.timeIntervalInMinutes = timeInterval;
        this.willingToWalkInMeters = willingToWalkInMeters;
    }

    public AgentWithConstraints(Agent agent, long timeInterval, double willingToWalkInMeters, long willingToRideInMinutes){
        super(agent.getId(), agent.getHomePosition(), agent.getCar(), agent.getRequest(), agent.isWillingToUseAlternatives());
        this.timeIntervalInMinutes = timeInterval;
        this.willingToWalkInMeters = willingToWalkInMeters;
        this.willingToRideInMinutes = willingToRideInMinutes;
    }

    public AgentWithConstraints(long id, Coordinate homePosition, Vehicle car, Request request) {
        super(id,homePosition,car,request);
    }

    public AgentWithConstraints(long id, Coordinate homePosition, Vehicle car, Request request, boolean willingToUseAlternatives) {
        super(id,homePosition,car,request,willingToUseAlternatives);
    }

    public AgentWithConstraints(long id, Coordinate homePosition, Vehicle car, Request request, long timeInterval, double willingToWalkInMeters) {
        super(id,homePosition,car,request);
        this.timeIntervalInMinutes = timeInterval;
        this.willingToWalkInMeters = willingToWalkInMeters;
    }

    public AgentWithConstraints(long id, Coordinate homePosition, Vehicle car, Request request, long timeInterval, double willingToWalkInMeters, long willingToRideInMinutes) {
        super(id,homePosition,car,request);
        this.timeIntervalInMinutes = timeInterval;
        this.willingToWalkInMeters = willingToWalkInMeters;
        this.willingToRideInMinutes = willingToRideInMinutes;
    }


    public double getWillingToWalkInMeters() {
        return willingToWalkInMeters;
    }

    public void setWillingToWalkInMeters(double willingToWalkInMeters) {
        this.willingToWalkInMeters = willingToWalkInMeters;
    }

    public long getWillingToRideInMinutes() {
        return willingToRideInMinutes;
    }

    public void setWillingToRideInMinutes(long willingToRideInMinutes) {
        this.willingToRideInMinutes = willingToRideInMinutes;
    }

    public long getTimeIntervalInMinutes() {
        return timeIntervalInMinutes;
    }

    public void setTimeIntervalInMinutes(long timeIntervalInMinutes) {
        this.timeIntervalInMinutes = timeIntervalInMinutes;
    }

}
