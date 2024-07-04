package camos.modeexecution.mobilitymodels;

import camos.modeexecution.carmodels.StudentVehicle;
import com.graphhopper.ResponsePath;
import camos.modeexecution.*;
import camos.modeexecution.groupings.Ride;
import camos.modeexecution.mobilitymodels.modehelpers.CommonFunctionHelper;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.*;

/**
 * EverybodyDrives is a mobility mode in which every agent rides in their own car.
 */
public class EverybodyDrives extends MobilityMode {

    List<Event> sortedEvents;

    int seatCount;
    double co2EmissionPerLiter;
    double pricePerLiter;
    double consumptionPerKm;
    Map<Ride, Double> finishedRideMinutesTravelled;
    Map<Ride,Double> finishedRideKmTravelled;
    Map<Ride,Double> finishedRideEmissions;
    Map<Ride,Double> finishedRideCosts;

    public EverybodyDrives(){
        super();
        sortedEvents = new ArrayList<>();
    }

    public String getName(){
        return "everybodyDrives";
    }


    public void prepareMode(List<Agent> agents){

        try{
            seatCount = (int) ModeExecutionManager.configValues.get("student car seat count");
            co2EmissionPerLiter = Double.parseDouble(String.valueOf(ModeExecutionManager.configValues.get("studentCarCo2EmissionPerLiter")));
            pricePerLiter = Double.parseDouble(String.valueOf(ModeExecutionManager.configValues.get("studentCarPricePerLiter")));
            consumptionPerKm = Double.parseDouble(String.valueOf(ModeExecutionManager.configValues.get("studentCarConsumptionPerKm")));

            this.agents = agents;
            for(Agent agent : agents){
                agent.setCar(new StudentVehicle(seatCount, co2EmissionPerLiter, pricePerLiter, consumptionPerKm));
                Request agentRequest = agent.getRequest();
                LocalDateTime driveStartTime = CommonFunctionHelper.calculateNecessaryDriveStartTime(agent.getRequest());
                Ride toUniRide = new Ride(agentRequest.getHomePosition(),agentRequest.getDropOffPosition(),driveStartTime,agentRequest.getFavoredArrivalTime(),agent.getCar(),agent, DirectionType.DRIVETOUNI,List.of(agent));
                Ride fromUniRide = new Ride(agentRequest.getDropOffPosition(),agentRequest.getHomePosition(),agentRequest.getFavoredDepartureTime(),null,agent.getCar(),agent, DirectionType.DRIVEHOME,List.of(agent));
                this.sortedEvents.add(new Event("rideStart",driveStartTime,toUniRide));
                this.sortedEvents.add(new Event("rideStart",agentRequest.getFavoredDepartureTime(),fromUniRide));
            }
            Collections.sort(this.sortedEvents);
        }catch(Exception e){
            throw new RuntimeException("Something went wrong when setting input parameters for "+this.getName()+".");
        }
    }


    public void startMode(){
        if(this.sortedEvents.isEmpty()){
            throw new RuntimeException("Run 'prepareSimulation' first.");
        }

        while(!this.sortedEvents.isEmpty()){
            Event event = this.sortedEvents.get(0);
            if(event.getType().equals("rideStart")){
                Ride ride = (Ride) event.getEventObject();
                ride.setEndTime(calculateMetrics(ride));
                rides.add(ride);
            }
            this.sortedEvents.remove(0);
        }
        finishedRideKmTravelled = new HashMap<>();
        finishedRideMinutesTravelled = new HashMap<>();
        finishedRideEmissions = new HashMap<>();
        finishedRideCosts = new HashMap<>();
        for(Ride ride : this.rides){
            finishedRideKmTravelled.put(ride,getFinishedOneWayKmTravelled().get(Set.of(ride,ride.getAgents().get(0))));
            finishedRideMinutesTravelled.put(ride,getFinishedOneWayMinutesTravelled().get(Set.of(ride,ride.getAgents().get(0))));
            finishedRideEmissions.put(ride,getFinishedOneWayEmissions().get(Set.of(ride,ride.getAgents().get(0))));
            finishedRideCosts.put(ride,getFinishedOneWayCosts().get(Set.of(ride,ride.getAgents().get(0))));
        }
    }


    public boolean checkIfConstraintsAreBroken(List<Agent> agents){
        for(Agent agent : agents){
            if(agentToRides==null || !agentToRides.containsKey(agent) || agentToRides.get(agent).size()<2){
                return true;
            }
            Ride toRide = agentToRides.get(agent).get(0);
            Ride backRide = agentToRides.get(agent).get(1);
            if(toRide==null || backRide==null){
                return true;
            }
            if(!toRide.getEndTime().equals(agent.getRequest().getFavoredArrivalTime())){
                return true;
            }
            if(!backRide.getStartTime().equals(agent.getRequest().getFavoredDepartureTime())){
                return true;
            }
        }
        return false;
    }

    @Override
    public Map<Agent, Double> getFinishedEmissions() {
        return this.emissions;
    }

    @Override
    public Map<Agent, Double> getFinishedCosts() {
        return this.costs;
    }

    @Override
    public Map<Agent, Double> getFinishedKmTravelled() {
        return this.kmTravelled;
    }

    @Override
    public Map<Agent, Double> getFinishedMinutesTravelled() {
        return this.minutesTravelled;
    }

    @Override
    public Map<Set<Object>, Double> getFinishedOneWayEmissions() {
        return this.oneWayEmissions;
    }

    @Override
    public Map<Set<Object>, Double> getFinishedOneWayCosts() {
        return this.oneWayCosts;
    }

    @Override
    public Map<Set<Object>, Double> getFinishedOneWayKmTravelled() {
        return this.oneWayKmTravelled;
    }

    @Override
    public Map<Set<Object>, Double> getFinishedOneWayMinutesTravelled() {
        return this.oneWayMinutesTravelled;
    }

    @Override
    public Map<Agent, List<Ride>> getFinishedAgentToRides() {
        return this.agentToRides;
    }

    @Override
    public List<Ride> getFinishedRides() {
        return this.rides;
    }

    @Override
    public List<Agent> getFinishedDrivers() {
        return this.drivers;
    }

    @Override
    public double getFinishedTotalCosts() {
        return CommonFunctionHelper.sumOverAgents(agents,costs);
    }

    @Override
    public double getFinishedTotalEmissions() {
        return CommonFunctionHelper.sumOverAgents(agents,emissions);
    }

    @Override
    public double getFinishedTotalKmTravelled() {
        return CommonFunctionHelper.sumOverAgents(agents,kmTravelled);
    }

    @Override
    public double getFinishedTotalMinutesTravelled() {
        return CommonFunctionHelper.sumOverAgents(agents,minutesTravelled);
    }

    @Override
    public Map<Ride, Double> getFinishedRideCosts() {
        return finishedRideCosts;
    }

    @Override
    public Map<Ride, Double> getFinishedRideEmissions() {
        return finishedRideEmissions;
    }

    @Override
    public Map<Ride, Double> getFinishedRideKmTravelled() {
        return finishedRideKmTravelled;
    }

    @Override
    public Map<Ride, Double> getFinishedRideMinutesTravelled() {
        return finishedRideMinutesTravelled;
    }


    @Override
    public void writeAdditionalResults(String resultsFolder) {}


    /**
     * Calculate and save the output metrics of one ride and return the ride end time.
     *
     * @param  ride the ride for which the metrics are to be calculated
     * @return      the ride end time
     */
    public LocalDateTime calculateMetrics(Ride ride){
        Agent agent = ride.getAgents().get(0);
        ResponsePath path = CommonFunctionHelper.getSimpleBestGraphhopperPath(this.graphHopper,ride.getStartPosition(),ride.getEndPosition());
        double distance = path.getDistance()/1000.0;
        long timeInMinutes = path.getTime()/60000L;
        double time = (double) timeInMinutes;
        double pathCosts = distance*agent.getCar().getConsumptionPerKm()*agent.getCar().getPricePerLiter();
        double pathEmissions = distance*agent.getCar().getConsumptionPerKm()*agent.getCar().getCo2EmissionPerLiter();

        List<Ride> rideList = new ArrayList<>();
        if(agentToRides.containsKey(agent)){
            rideList = agentToRides.get(agent);
        }
        rideList.add(ride);
        agentToRides.put(agent,rideList);

        oneWayCosts.put(Set.of(agent,ride),pathCosts);
        oneWayEmissions.put(Set.of(agent,ride),pathEmissions);
        oneWayKmTravelled.put(Set.of(agent,ride),distance);
        oneWayMinutesTravelled.put(Set.of(agent,ride),time);

        if(minutesTravelled.containsKey(agent)){
            time = minutesTravelled.get(agent)+time;
            distance = kmTravelled.get(agent)+distance;
            pathEmissions = emissions.get(agent)+pathEmissions;
            pathCosts = costs.get(agent)+pathCosts;
        }
        minutesTravelled.put(agent, time);
        kmTravelled.put(agent, distance);
        emissions.put(agent,pathEmissions);
        costs.put(agent,pathCosts);

        return ride.getTypeOfGrouping()== DirectionType.DRIVETOUNI ? ride.getEndTime() : ride.getStartTime().plusMinutes(timeInMinutes);
    }




}
