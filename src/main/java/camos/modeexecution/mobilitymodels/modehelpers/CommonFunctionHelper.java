package camos.modeexecution.mobilitymodels.modehelpers;

import camos.AgentWithConstraints;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.util.shapes.GHPoint;
import camos.GeneralManager;
import camos.modeexecution.*;
import camos.modeexecution.groupings.Ride;
import camos.modeexecution.groupings.Stop;
import camos.modeexecution.groupings.Stopreason;
import camos.modeexecution.mobilitymodels.MobilityMode;
import camos.modeexecution.mobilitymodels.tsphelpers.ActivityOrderConstraint;
import camos.modeexecution.mobilitymodels.tsphelpers.ActivityWaitConstraintNoneAllowed;
import camos.modeexecution.mobilitymodels.tsphelpers.ActivityWaitConstraintOneAllowed;
import camos.modeexecution.mobilitymodels.tsphelpers.TransportCosts;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * A class for grouping helpful functions for the mobility modes.
 */
public class CommonFunctionHelper {

    /**
     * Get the best graphhopper path for a start and end
     *
     * @param  graphHopper     the GraphHopper instance for calculating the best path
     * @param  start           the start coordinate
     * @param  end             the end coordinate
     * @return                 the best path
     */
    public static ResponsePath getSimpleBestGraphhopperPath(GraphHopper graphHopper, Coordinate start, Coordinate end){
        GHPoint ghPointStart = new GHPoint(start.getLatitude(),start.getLongitude());
        GHPoint ghPointEnd = new GHPoint(end.getLatitude(),end.getLongitude());
        GHRequest ghRequest = new GHRequest(ghPointStart,ghPointEnd).setProfile("car").setLocale(Locale.GERMANY);
        ResponsePath path = graphHopper.route(ghRequest).getBest();
        /*if(!ModeExecutionManager.timeMap.containsKey(List.of(Coordinate.coordinateToLocation(start),Coordinate.coordinateToLocation(end)))){
            ModeExecutionManager.timeMap.put(List.of(Coordinate.coordinateToLocation(start),Coordinate.coordinateToLocation(end)),path.getTime());
            ModeExecutionManager.distanceMap.put(List.of(Coordinate.coordinateToLocation(start),Coordinate.coordinateToLocation(end)),path.getDistance());
        }*/

        return path;
    }


    /**
     * Calculates the arrival time for a ride without stops
     *
     * @param  graphHopper     the GraphHopper instance for calculating the arrival time
     * @param  startTime       the time the ride starts
     * @param  start           the start coordinate
     * @param  end             the end coordinate
     * @return                 the best path
     */
    public static LocalDateTime getSimpleArrivalTime(GraphHopper graphHopper, LocalDateTime startTime, Coordinate start, Coordinate end){
        ResponsePath path = CommonFunctionHelper.getSimpleBestGraphhopperPath(graphHopper,start,end);
        if(path==null){
            throw new RuntimeException("No path found with graphhopper!");
        }
        long timeInMinutes = path.getTime()/60000L;
        return startTime.plusMinutes(timeInMinutes);
    }



    /**
     * Calculate the time an agent needs to start driving to arrive at their ideal arrival time
     *
     * @param  request the (ride home) request from which the ideal arrival time of an agent is extracted
     * @return         the time the agent needs to start driving
     */
    public static LocalDateTime calculateNecessaryDriveStartTime(Request request){
        GHPoint ghPointStart = new GHPoint(request.getHomePosition().getLatitude(),request.getHomePosition().getLongitude());
        Coordinate uniCoordinate = request.getDropOffPosition();
        GHPoint ghPointEnd = new GHPoint(uniCoordinate.getLatitude(), uniCoordinate.getLongitude());
        GHRequest ghRequest = new GHRequest(ghPointStart,ghPointEnd).setProfile("car").setLocale(Locale.GERMANY);
        ResponsePath path = ModeExecutionManager.graphHopper.route(ghRequest).getBest();

        long timeInMinutes = path.getTime()/60000L;
        return request.getFavoredArrivalTime().minusMinutes(timeInMinutes);
    }


    /**
     * Calculate the time an agent needs to start driving to arrive at the specified arrival time
     *
     * @param  startPosition the start coordinate
     * @param  endPosition   the end coordinate
     * @param  arrivalTime   the wished for arrival time
     * @return               the time the agent needs to start driving
     */
    public static LocalDateTime calculateNecessaryDriveStartTime(Coordinate startPosition, Coordinate endPosition, LocalDateTime arrivalTime){
        GHPoint ghPointStart = new GHPoint(startPosition.getLatitude(),startPosition.getLongitude());
        GHPoint ghPointEnd = new GHPoint(endPosition.getLatitude(), endPosition.getLongitude());
        GHRequest ghRequest = new GHRequest(ghPointStart,ghPointEnd).setProfile("car").setLocale(Locale.GERMANY);
        ResponsePath path = ModeExecutionManager.graphHopper.route(ghRequest).getBest();

        long timeInMinutes = path.getTime()/60000L;
        return arrivalTime.minusMinutes(timeInMinutes);
    }


    /**
     * Filter two lists of agents into willing and unwilling agents
     *
     * @param  percentOfWillingStudents  the percentage of students who are willing to try an alternative mode of travel
     * @param  willingAgents             the list which should only contain willing agents
     * @param  unWillingAgents           the list which should only contain unwilling agents
     */
    public static void filterWilling(double percentOfWillingStudents, List<Agent> willingAgents, List<Agent> unWillingAgents){
        if (percentOfWillingStudents < 100.0) {
            willingAgents.removeIf(a -> !a.isWillingToUseAlternatives());
            unWillingAgents.removeIf(Agent::isWillingToUseAlternatives);
        }
    }


    /**
     * Calculates for each agent how long he is willing to ride in a vehicle, based on a compared mode and the function contained in the given String
     *
     * @param  agents                 constraint having agents
     * @param  compareMode            the mode which gives the basis for calculating the accepted ride times
     * @param  functionForDrivingTime contains the function of x for calculating the time an agent is willing to ride in a vehicle (x being the time the agent travels to or from campus on average in a compareMode)
     */ //TODO
    public static void calculateAcceptedDrivingTimes(List<AgentWithConstraints> agents, MobilityMode compareMode, String functionForDrivingTime) {
        if (compareMode.getMinutesTravelled() == null || !compareMode.getMinutesTravelled().isEmpty()) {
            if(functionForDrivingTime.contains("log")){
                if (functionForDrivingTime.contains("+")) {
                    for (AgentWithConstraints agent : agents) {
                        double oneWayMinutesTravelled = compareMode.getMinutesTravelled().get(agent) / 2;
                        agent.setWillingToRideInMinutes((long) Math.max(2, oneWayMinutesTravelled + customLog(Double.parseDouble(StringUtils.substringBetween(functionForDrivingTime, "log", "(")), oneWayMinutesTravelled)));
                    }
                }
            }
        } else {
            throw new RuntimeException("At first, the compare mode simulation has to run.");
        }
    }


    /**
     * Calculates the seconds needed for driving between postcodes
     *
     * @param  secondsBetweenDropOffs the map with "postcodeX-postcodeY" keys to be filled with the needed seconds
     * @param  postcodeToCoordinate mapping of a postcode String to a pickup/drop-off coordinate
     */
    public static void calculateSecondsBetweenDropOffs(Map<String, Long> secondsBetweenDropOffs, Map<String, Coordinate> postcodeToCoordinate) {
        for (String postcode : postcodeToCoordinate.keySet()) {
            for (String postcode2 : postcodeToCoordinate.keySet()) {
                if (!postcode.equals(postcode2) && !secondsBetweenDropOffs.containsKey(postcode + "-" + postcode2)) {
                    GHPoint ghPointStart = new GHPoint(postcodeToCoordinate.get(postcode).getLatitude(), ModeExecutionManager.postcodeToCoordinate.get(postcode).getLongitude());
                    GHPoint ghPointEnd = new GHPoint(postcodeToCoordinate.get(postcode2).getLatitude(), ModeExecutionManager.postcodeToCoordinate.get(postcode2).getLongitude());
                    GHRequest ghRequest = new GHRequest(ghPointStart, ghPointEnd).setProfile("car").setLocale(Locale.GERMANY);
                    GHResponse rsp = ModeExecutionManager.graphHopper.route(ghRequest);
                    ResponsePath path = rsp.getBest();
                    secondsBetweenDropOffs.put(postcode + "-" + postcode2, path.getTime() / 1000);
                }
            }
        }
    }


    /**
     * Calculates the seconds needed for driving between postcodes
     *
     * @param  start1 the start time of the first interval
     * @param  end1   the end time of the first interval
     * @param  start2 the start time of the second interval
     * @param  end2   the end time of the second interval
     * @return        whether the two intervals are overlapping
     */
    public static boolean isOverlapping(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        return !start1.isAfter(end2) && !start2.isAfter(end1);
    }


    /**
     * Calculates the seconds needed for driving between postcodes
     *
     * @param  interval1 the first interval
     * @param  interval2 the second interval
     * @return           whether the two intervals are overlapping
     */
    public static boolean isOverlapping(TimeInterval interval1, TimeInterval interval2) {
        return !interval1.getStart().isAfter(interval2.getEnd()) && !interval2.getStart().isAfter(interval1.getEnd());
    }


    /**
     * Get the most common integer from an array of integer values
     *
     * @param   a array of integer values
     * @return  most common integer of a
     */
    public static int getPopularElement(int[] a){
        int count = 1, tempCount;
        int popular = a[0];
        int temp;
        for (int i = 0; i < (a.length - 1); i++){
            temp = a[i];
            tempCount = 0;
            for (int j = 1; j < a.length; j++){
                if (temp == a[j])
                    tempCount++;
            }
            if (tempCount > count){
                popular = temp;
                count = tempCount;
            }
        }
        return popular;
    }


    public static String getIntervalString(LocalDateTime start, LocalDateTime end) {
        return start.format(GeneralManager.dateTimeFormatter) + "-" + end.format(GeneralManager.dateTimeFormatter);
    }


    /**
     * Get a logarithm with custom base and argument
     *
     * @param  base the logarithm base
     * @param  logNumber the argument of the logarithm
     * @return the custom logarithm result
     */
    public static double customLog(double base, double logNumber) {
        return Math.log(logNumber) / Math.log(base);
    }


    /**
     * Get the stops of an agent for a specified ride of his
     *
     * @param  ride the ride containing the agent's stops
     * @param  agent the agent whose stops are wanted
     * @return a list of the agent's stops
     */
    public static List<Stop> getAgentStops(Ride ride, Agent agent){
        Coordinate pickupLocation = ride.getTypeOfGrouping()== DirectionType.DRIVETOUNI ? agent.getHomePosition() : agent.getRequest().getDropOffPosition();
        Coordinate dropoffLocation = ride.getTypeOfGrouping()== DirectionType.DRIVETOUNI ? agent.getRequest().getDropOffPosition() : agent.getHomePosition();
        List<Stop> stops = new ArrayList<>();
        for(Stop stop : ride.getExtraStops()){
            if(stop.getReasonForStopping()== Stopreason.PICKUP && stop.getStopCoordinate().equals(pickupLocation)){
                stops.add(stop);
            }else if(stop.getReasonForStopping()==Stopreason.DROPOFF && stop.getStopCoordinate().equals(dropoffLocation)){
                stops.add(stop);
            }
        }
        if(stops.isEmpty()){
            if(ride.getTypeOfGrouping()== DirectionType.DRIVETOUNI){
                stops.add(new Stop(ride.getEndTime(),ride.getEndTime(),ride.getEndPosition(),Stopreason.DROPOFF,null));
            }else{
                stops.add(new Stop(ride.getStartTime(),ride.getStartTime(),ride.getStartPosition(),Stopreason.PICKUP,null));
            }
        }
        return stops;
    }


    /**
     * Combine a former interval and new times to a new interval String in the form of "dd.MM.yyyy HH:mm:ss-dd.MM.yyyy HH:mm:ss"
     *
     * @param  oldInterval the former interval String
     * @param  newStart the start time of the new interval
     * @param  newEnd the end time of the new interval
     * @return the new time interval String
     */
    public static String getNewStopInterval(String oldInterval, LocalDateTime newStart, LocalDateTime newEnd) {
        LocalDateTime oldStart = LocalDateTime.parse(oldInterval.split("-")[0], GeneralManager.dateTimeFormatter);
        LocalDateTime oldEnd = LocalDateTime.parse(oldInterval.split("-")[1], GeneralManager.dateTimeFormatter);
        String newInterval = "";
        if (oldStart.isBefore(newStart)) {
            newInterval = newInterval + newStart.format(GeneralManager.dateTimeFormatter);
        } else {
            newInterval = newInterval + oldStart.format(GeneralManager.dateTimeFormatter);
        }
        newInterval = newInterval + "-";
        if (oldEnd.isAfter(newEnd)) {
            newInterval = newInterval + newEnd.format(GeneralManager.dateTimeFormatter);
        } else {
            newInterval = newInterval + oldEnd.format(GeneralManager.dateTimeFormatter);
        }
        return newInterval;
    }


    /**
     * Set flexible time intervals in the specified requests, based on the minutes given
     *
     * @param  requests     the requests that should be augmented
     * @param  timeInterval the minutes for flexing the arrival and departure times
     */
    public static void setRequestTimeIntervals(List<Request> requests, long timeInterval){
        for(Request request : requests){
            request.setDepartureInterval(new TimeInterval(request.getFavoredDepartureTime(),request.getFavoredDepartureTime().plusMinutes(timeInterval*2)));
            request.setArrivalInterval(new TimeInterval(request.getFavoredArrivalTime().minusMinutes(timeInterval),request.getFavoredArrivalTime().plusMinutes(timeInterval)));
            if(!request.getArrivalInterval().getEnd().isBefore(request.getDepartureInterval().getStart())){
                request.getArrivalInterval().setEnd(request.getDepartureInterval().getStart().minusMinutes(5L));
            }
        }
    }


    /**
     * Set flexible time intervals for the specified agent's request, based on the minutes given
     *
     * @param  agent        the agent whose request should be augmented
     * @param  timeInterval the minutes for flexing the arrival and departure times
     */
    public static void setRequestTimeIntervalsForAgent(Agent agent, long timeInterval){
        agent.getRequest().setArrivalInterval(new TimeInterval(agent.getRequest().getFavoredArrivalTime().minusMinutes(timeInterval),agent.getRequest().getFavoredArrivalTime().plusMinutes(timeInterval)));
        agent.getRequest().setDepartureInterval(new TimeInterval(agent.getRequest().getFavoredDepartureTime(),agent.getRequest().getFavoredDepartureTime().plusMinutes(timeInterval*2)));
    }


    /**
     * Get the total sum of agents' metric values
     *
     * @param  agents the agents
     * @param  metricValues each agent's metric value
     * @return the total sum of agents' metric values
     */
    public static double sumOverAgents(List<Agent> agents,Map<Agent,Double> metricValues){
        double sum = 0.0;
        for(Agent agent : agents){
            sum+=metricValues.get(agent);
        }
        return sum;
    }

}


