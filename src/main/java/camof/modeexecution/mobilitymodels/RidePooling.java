package camof.modeexecution.mobilitymodels;

import camof.AgentWithConstraints;
import camof.modeexecution.mobilitymodels.modehelpers.RouteCalculationHelper;
import com.graphhopper.ResponsePath;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Solutions;
import camof.GeneralManager;
import camof.modeexecution.*;
import camof.modeexecution.carmodels.MiniBus;
import camof.modeexecution.carmodels.Vehicle;
import camof.modeexecution.groupings.Match;
import camof.modeexecution.groupings.Ride;
import camof.modeexecution.groupings.Stop;
import camof.modeexecution.groupings.Stopreason;
import camof.modeexecution.mobilitymodels.modehelpers.CommonFunctionHelper;
import camof.modeexecution.mobilitymodels.tsphelpers.ActivityOrderConstraint;
import camof.modeexecution.mobilitymodels.tsphelpers.ActivityWaitConstraintOneAllowed;
import camof.modeexecution.mobilitymodels.tsphelpers.TransportCosts;
import org.apache.commons.lang3.StringUtils;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.CoordinateXY;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class RidePooling extends MobilityMode {

    int seatCount;
    double co2EmissionPerLiter;
    double pricePerLiter;
    double consumptionPerKm;

    MobilityMode compareMode;

    public Map<Vehicle, Double> vehicleEmptyDistances;
    public Map<Ride, Double> emptyDistances;

    public Map<Vehicle, Coordinate> vehicleToDepot;

    public Map<Coordinate, Integer> depotCapacity;

    public List<Coordinate> depots;

    public List<Agent> unwillingAgents;

    Map<String, Coordinate> postcodeToCoordinate;

    public Map<List<Long>, LocalDateTime> matchToPossibleNewIntervalStartTime;

    LocalDateTime timeCounter;

    public Map<Request, Match> assignedRequests;

    public Coordinate centralCoordinate;

    public Coordinate compareVector;

    public int countOfGroups;

    public double compareAngle;

    public double radiusToExclude;

    public Map<Agent, Double> emissions;

    public Map<Agent, Double> costs;

    public Map<Agent, Double> kmTravelled;

    public Map<Agent, Double> minutesTravelled;

    public Map<Vehicle, Double> vehicleEmissions;

    public Map<Vehicle, Double> vehicleCosts;

    public Map<Vehicle, Double> vehicleKmTravelled;

    public Map<Vehicle, Double> vehicleMinutesTravelled;

    public Map<LocalDateTime, List<Request>> requestFinder;
    public int requestSum;
    public int requestCounter;
    public Map<Agent, List<Match>> agentToMatches;

    public Map<Agent, List<Ride>> agentToRides;

    public List<Ride> rides;

    public List<Match> activeMatches;

    public List<Match> finishedMatches;

    public List<Agent> drivingAgents;

    public List<Request> pendingHomeRequests;

    public Map<String, Long> secondsBetweenDropOffs;

    public Map<Match, LocalDateTime> matchToStartTime;

    public Map<Match, LocalDateTime> matchToEndTime;

    public Map<LocalDateTime, List<Match>> timeToMatch;

    public Map<Match, VehicleRoutingProblemSolution> matchToSolution;


    public Map<Match, VehicleRoutingProblemSolution> temporaryMatchToSolution;

    public Map<Integer, List<Request>> groupToRequests;

    public Map<Integer, List<MiniBus>> groupToMiniBusses;

    public List<Vehicle> vehicles;

    public Map<Vehicle, Map<LocalDateTime, Coordinate>> vehiclePositions;

    public Map<Vehicle, List<Match>> vehicleToMatches;

    public Map<Vehicle, List<Ride>> vehicleToRides;

    public Map<Vehicle, Ride> lastVehicleRide;

    public Map<Vehicle, Long> rideTimeBackToDepot;

    public long stopTime;

    public int busCount;

    public String acceptedRideTimeString;


    public RidePooling(Map<String, Coordinate> postcodeToCoordinate, MobilityMode compareMode){
        this.graphHopper= ModeExecutionManager.graphHopper;
        this.postcodeToCoordinate = postcodeToCoordinate;
        this.compareMode = compareMode;
    }

    public String getName(){
        return "ridepooling";
    }


    public void prepareMode(List<Agent> agents) {
        this.agents = new ArrayList<>(agents);

        if(this.centralCoordinate==null || this.countOfGroups == 0 || this.radiusToExclude == 0){//TODO
            throw new RuntimeException("Run 'prepareMore()' first!");
        }
        unwillingAgents = new ArrayList<>();
        this.rides = new ArrayList<>();
        assignedRequests = new HashMap<>();
        this.compareAngle = (double) 360 / countOfGroups;
        compareVector = new Coordinate(0.0, 1.0);
        emissions = new HashMap<>();
        costs = new HashMap<>();
        kmTravelled = new HashMap<>();
        minutesTravelled = new HashMap<>();
        agentToMatches = new HashMap<>();
        agentToRides = new HashMap<>();
        matchToStartTime = new HashMap<>();
        matchToEndTime = new HashMap<>();
        matchToSolution = new HashMap<>();
        timeToMatch = new HashMap<>();
        activeMatches = new ArrayList<>();
        finishedMatches = new ArrayList<>();
        drivingAgents = new ArrayList<>();
        pendingHomeRequests = new ArrayList<>();
        oneWayCosts = new HashMap<>();
        oneWayEmissions = new HashMap<>();
        oneWayKmTravelled = new HashMap<>();
        oneWayMinutesTravelled = new HashMap<>();
        groupToRequests = new HashMap<>();
        groupToMiniBusses = new HashMap<>();
        vehicles = new ArrayList<>();
        vehiclePositions = new HashMap<>();
        vehicleToMatches = new HashMap<>();
        vehicleToRides = new HashMap<>();
        temporaryMatchToSolution = new HashMap<>();
        vehicleEmissions = new HashMap<>();
        vehicleCosts = new HashMap<>();
        vehicleKmTravelled = new HashMap<>();
        vehicleMinutesTravelled = new HashMap<>();
        rideTimeBackToDepot = new HashMap<>();
        lastVehicleRide = new HashMap<>();
        matchToPossibleNewIntervalStartTime = new HashMap<>();
        vehicleToDepot = new HashMap<>();
        vehicleEmptyDistances = new HashMap<>();
        emptyDistances = new HashMap<>();

        String timeString = "02.02.2023 04:00:00"; //TODO
        LocalDateTime startTime = LocalDateTime.parse(timeString, GeneralManager.dateTimeFormatter);

        List<Coordinate> depots = new ArrayList<>();
        depots.add(this.centralCoordinate);
        depots.add(new Coordinate(9.77714538574219, 49.79057843998488));
        depots.add(new Coordinate(10.1253122091293, 49.79057843998488));
        depots.add(new Coordinate(9.951222228079288, 49.6779658273723));
        depots.add(new Coordinate(9.951222228079288, 49.9031910525975));
        this.depots = depots;
        int depotCount = 5; //TODO
        Map<Coordinate, Integer> depotVehicles = new HashMap<>();
        for (Coordinate d : depots) {
            depotVehicles.put(d, 0);
        }

        this.depotCapacity = new HashMap<>();
        int maxDepotCapacity = 0;
        int centralCapacity = busCount;
        if (depotCount > 1) {
            centralCapacity = (int) Math.floor(0.4 * busCount);
            maxDepotCapacity = (int) Math.ceil((busCount - centralCapacity) / (double) (depotCount - 1));
        }
        for (Coordinate d : depots) {
            if (d.equals(centralCoordinate)) {
                this.depotCapacity.put(d, centralCapacity);
            } else {
                this.depotCapacity.put(d, maxDepotCapacity);
            }
        }

        for (int i = 0; i < busCount; i++) {
            Vehicle vehicle = new MiniBus(seatCount, co2EmissionPerLiter, pricePerLiter, consumptionPerKm);
            vehicleEmptyDistances.put(vehicle, 0.0);
            vehicles.add(vehicle);
            vehicleToMatches.put(vehicle, new ArrayList<>());
            vehicleToRides.put(vehicle, new ArrayList<>());
            Map<LocalDateTime, Coordinate> positions = new HashMap<>();
            if (centralCapacity > 0) {
                positions.put(startTime, centralCoordinate);
                centralCapacity--;
                depotVehicles.put(centralCoordinate, depotVehicles.get(centralCoordinate) + 1);
            } else if (depotCount > 1) {
                int depotCat = i % (depotCount - 1);
                positions.put(startTime, depots.get(depotCat + 1));
                depotVehicles.put(depots.get(depotCat + 1), depotVehicles.get(depots.get(depotCat + 1)) + 1);
            }
            vehiclePositions.put(vehicle, positions);
        }

        for (Coordinate d : depots) {
            if (depotVehicles.get(d) < maxDepotCapacity) {
                this.depotCapacity.put(d, depotVehicles.get(d));
            }
        }

        CommonFunctionHelper.calculateSecondsBetweenDropOffs(this.secondsBetweenDropOffs,this.postcodeToCoordinate);

        List<Agent> willingAgents = new ArrayList<>(agents);
        if (ModeExecutionManager.percentOfWillingStudents < 100.0) {
            List<Agent> unWillingAgents = new ArrayList<>(agents);
            willingAgents.removeIf(a -> !a.isWillingToUseAlternatives());
            unWillingAgents.removeIf(Agent::isWillingToUseAlternatives);
            unwillingAgents = unWillingAgents;
            for (Agent agent : unWillingAgents) {
                handleDrivingAgent(agent.getRequest());
            }
        }

        Map<LocalDateTime, List<Request>> requestFinder = new HashMap<>();
        List<Request> finderRequests;
        for (Agent agent : willingAgents) {
            Request request = agent.getRequest();
            if (requestFinder.containsKey(request.getRequestTime())) {
                finderRequests = requestFinder.get(request.getRequestTime());
            } else {
                finderRequests = new ArrayList<>();
            }
            finderRequests.add(request);
            requestFinder.put(request.getRequestTime(), finderRequests);
            requestSum++;
        }
        this.requestFinder = requestFinder;

        calculateAcceptedDrivingTimes(new ArrayList<>(agents.stream().filter(a -> a instanceof AgentWithConstraints).map(a -> (AgentWithConstraints) a).toList()),this.compareMode);
    }


    public void prepareMore(Coordinate centralCoordinate, int countOfGroups, double radiusToExclude){
        this.centralCoordinate = centralCoordinate;
        this.countOfGroups = countOfGroups;
        this.radiusToExclude = radiusToExclude;
    }


    public void calculateAcceptedDrivingTimes(List<AgentWithConstraints> agents, MobilityMode compareMode) {
        if (!compareMode.getMinutesTravelled().isEmpty()) {
            for (AgentWithConstraints agent : agents) {
                double oneWayMinutesTravelled = compareMode.getMinutesTravelled().get(agent) / 2;
                if (acceptedRideTimeString.contains("log")) {
                    if (acceptedRideTimeString.contains("+")) {
                        agent.setWillingToRideInMinutes((long) Math.max(2, oneWayMinutesTravelled + CommonFunctionHelper.customLog(Double.parseDouble(StringUtils.substringBetween(acceptedRideTimeString, "log", "(")), oneWayMinutesTravelled)));
                    }
                }
            }
        } else {
            throw new RuntimeException("At first, the compare mode simulation has to run.");
        }
    }


    public int assignRequestToGroup(Request request) {
        int groupNumber = -1;
        Coordinate homePosition = request.getHomePosition();
        Coordinate homeVector = addCoordinatesToOneAnother(homePosition, new Coordinate(centralCoordinate.getLongitude() * (-1.0), centralCoordinate.getLatitude() * (-1.0)));
        org.locationtech.jts.geom.Coordinate homeCoordinate = new CoordinateXY(homePosition.getLongitude(), homePosition.getLatitude());
        org.locationtech.jts.geom.Coordinate referenceCoordinate = new CoordinateXY(request.getDropOffPosition().getLongitude(), request.getDropOffPosition().getLatitude());

        try {
            if (JTS.orthodromicDistance(homeCoordinate, referenceCoordinate, DefaultGeographicCRS.WGS84) > this.radiusToExclude) {
                double angle = Math.acos(getVectorScalarProduct(homeVector, this.compareVector) / getVectorAbs(homeVector));
                angle = (angle / Math.PI) * 180;

                if (homeVector.getLongitude() < 0.0) {
                    angle = 360 - angle;
                }
                groupNumber = (int) Math.floor(angle / compareAngle) + 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (groupToRequests != null) {
            List<Request> requestsInGroup = new ArrayList<>();
            if (groupToRequests.containsKey(groupNumber)) {
                requestsInGroup = groupToRequests.get(groupNumber);
            }
            requestsInGroup.add(request);
            groupToRequests.put(groupNumber, requestsInGroup);
        }
        return groupNumber;
    }


    public double getVectorScalarProduct(Coordinate coordinate1, Coordinate coordinate2) {
        return coordinate1.getLongitude() * coordinate2.getLongitude() + coordinate1.getLatitude() * coordinate2.getLatitude();
    }


    public double getVectorAbs(Coordinate coordinate) {
        return Math.sqrt(Math.pow(coordinate.getLongitude(), 2) + Math.pow(coordinate.getLatitude(), 2));
    }


    public Coordinate addCoordinatesToOneAnother(Coordinate coordinate1, Coordinate coordinate2) {
        return new Coordinate(coordinate1.getLongitude() + coordinate2.getLongitude(), coordinate1.getLatitude() + coordinate2.getLatitude());
    }


    public void startMode() {
        String timeString = "01.02.2023 18:00:00";
        LocalDateTime startTime = LocalDateTime.parse(timeString, GeneralManager.dateTimeFormatter);
        timeString = "03.02.2023 22:00:00";
        LocalDateTime endTime = LocalDateTime.parse(timeString, GeneralManager.dateTimeFormatter);
        List<LocalDateTime> times = GeneralManager.generateTimeScale(startTime, endTime).toList();
        requestCounter = 0;
        int lastPercent = -1;
        for (LocalDateTime time : times) {
            this.timeCounter = time;
            if (timeToMatch.containsKey(timeCounter)) {
                for (Match match : timeToMatch.get(timeCounter)) {
                    calculateMetrics(matchToRide(match));
                }
            }
            if (requestFinder.containsKey(timeCounter)) {
                //System.out.println(timeCounter);
                for (Request request : requestFinder.get(timeCounter)) {
                    requestCounter++;
                    int countOf5percentSteps = (int) Math.floor(((double) requestCounter / requestSum) * 20);
                    String progressBar = "|" + "=".repeat(countOf5percentSteps) + " ".repeat(20 - countOf5percentSteps) + "|" + "\r";
                    findMatch(request);
                    if (countOf5percentSteps > lastPercent) {
                        System.out.print(progressBar);
                    }
                    lastPercent = countOf5percentSteps;
                    //System.out.println(requestCounter + ": " + woulddrivebyCounter + " and " + againCounter);
                    //System.out.println("match false because of acceptedDriveTime: " + acceptedDriveTimeCounter + ", noSolutionFound: " + noSolutionFoundCounter + ", timeConstraints: " + timeConstraintsCounter);
                }
            }

        }
        for (Vehicle v : lastVehicleRide.keySet()) {
            Map<LocalDateTime, Coordinate> positions = vehiclePositions.get(v);
            positions.put(lastVehicleRide.get(v).getEndTime().plusMinutes(1L).plusMinutes(rideTimeBackToDepot.get(v)), vehicleToDepot.get(v));
            vehiclePositions.put(v, positions);
            Ride r = new Ride(lastVehicleRide.get(v).getEndPosition(), vehicleToDepot.get(v), lastVehicleRide.get(v).getEndTime().plusMinutes(1L), lastVehicleRide.get(v).getEndTime().plusMinutes(1L).plusMinutes(rideTimeBackToDepot.get(v)), v, null, null, new ArrayList<>());
            List<Stop> stops = new ArrayList<>();
            stops.add(new Stop(r.getEndTime(), r.getEndTime(), vehicleToDepot.get(v), Stopreason.PARKING, new ArrayList<>()));
            r.setExtraStops(stops);
            calculateMetrics(r);
        }
        int f = 2;
    }

    public boolean checkIfConstraintsAreBroken(List<Agent> agents){
        for(Agent agent : agents) {
            if (agentToRides==null || !agentToRides.containsKey(agent) || agentToRides.get(agent).size()<2) {
                return true;
            }
            Ride toRide = agentToRides.get(agent).get(0);
            Ride backRide = agentToRides.get(agent).get(1);
            if(backRide==null || toRide==null || backRide.getTypeOfGrouping()!= DirectionType.DRIVEHOME || toRide.getTypeOfGrouping()!= DirectionType.DRIVETOUNI){
                return true;
            }else if(!toRide.getAgents().get(0).getCar().equals(toRide.getVehicle()) && (toRide.getStartTime().isBefore(LocalDateTime.parse("02.02.2023 04:00:00", GeneralManager.dateTimeFormatter)) || toRide.getEndTime().isAfter(LocalDateTime.parse("03.02.2023 02:00:00", GeneralManager.dateTimeFormatter)) || backRide.getStartTime().isBefore(LocalDateTime.parse("02.02.2023 04:00:00", GeneralManager.dateTimeFormatter)) || backRide.getEndTime().isAfter(LocalDateTime.parse("03.02.2023 02:00:00", GeneralManager.dateTimeFormatter)))){
                return true;
            }else if(toRide.getAgents().get(0).getCar().equals(toRide.getVehicle()) && !toRide.getEndTime().isBefore(backRide.getStartTime())){
                return true;
            }else if(toRide.getVehicle().equals(agent.getCar())){
                if(!CommonFunctionHelper.isOverlapping(toRide.getEndTime(),toRide.getEndTime(),agent.getRequest().getArrivalInterval().getStart(),agent.getRequest().getArrivalInterval().getEnd())){
                    return true;
                }
                if(!CommonFunctionHelper.isOverlapping(backRide.getStartTime(),backRide.getStartTime(),agent.getRequest().getDepartureInterval().getStart(),agent.getRequest().getDepartureInterval().getEnd())){
                    return true;
                }
            }
            Stop dropOffStop = null;
            List<Stop> stops = CommonFunctionHelper.getAgentStops(toRide,agent);
            for(Stop st : stops){
                if(st.getReasonForStopping()== Stopreason.DROPOFF){
                    dropOffStop = st;
                }
            }
            if(dropOffStop!=null && !CommonFunctionHelper.isOverlapping(dropOffStop.getStartTime(),dropOffStop.getStartTime(),agent.getRequest().getArrivalInterval().getStart(),agent.getRequest().getArrivalInterval().getEnd())){
                return true;
            }

            Stop pickUpStop = null;
            stops = CommonFunctionHelper.getAgentStops(backRide,agent);
            for(Stop st : stops){
                if(st.getReasonForStopping()==Stopreason.PICKUP){
                    pickUpStop = st;
                }
            }
            if(pickUpStop!=null && !CommonFunctionHelper.isOverlapping(pickUpStop.getStartTime(),pickUpStop.getStartTime(),agent.getRequest().getDepartureInterval().getStart(),agent.getRequest().getDepartureInterval().getEnd())){
                return true;
            }
            if(agent instanceof AgentWithConstraints){
                AgentWithConstraints agent2 = (AgentWithConstraints) agent;
                Set<Object> set = new HashSet<>();
                set.add(toRide);
                set.add(agent);
                if(oneWayMinutesTravelled.get(set)>agent2.getWillingToRideInMinutes()){
                    return true;
                }
                set = new HashSet<>();
                set.add(backRide);
                set.add(agent);
                if(oneWayMinutesTravelled.get(set)>agent2.getWillingToRideInMinutes()){
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void writeAdditionalResults(String resultsFolder) {
        List<Vehicle> vehicles = this.vehicles;
        Map<Agent,Double> kmTravelled = this.getKmTravelled();
        Map<Agent,Double> minutesTravelled = this.getMinutesTravelled();
        Map<Agent,Double> emissions = this.getEmissions();
        Map<Agent,Double> costs = this.getCosts();
        Map<Vehicle,Double> vehicleKmTravelled = this.vehicleKmTravelled;
        Map<Vehicle,Double> vehicleMinutesTravelled = this.vehicleMinutesTravelled;
        Map<Vehicle,Double> vehicleEmissions = this.vehicleEmissions;
        Map<Vehicle,Double> vehicleCosts = this.vehicleCosts;
        Map<Vehicle,List<Ride>> vehicleToRides = this.vehicleToRides;
        Map<Vehicle,Map<LocalDateTime,Coordinate>> vehiclePositions = this.vehiclePositions;
        Map<Set<Object>,Double> oneWayEmissions = this.getOneWayEmissions();
        Map<Set<Object>,Double> oneWayCosts = this.getOneWayCosts();
        Map<Set<Object>,Double> oneWayKmTravelled = this.getOneWayKmTravelled();
        Map<Set<Object>,Double> oneWayMinutesTravelled = this.getOneWayMinutesTravelled();
        Map<Agent, List<Ride>> agentToRides = this.getAgentToRides();
        List<Match> matches = this.finishedMatches;
        List<Agent> drivingAgents = this.drivingAgents;
        List<Ride> rides = this.rides;
        Map<Vehicle,Double> vehicleEmptyDistances = this.vehicleEmptyDistances;
        Map<Ride,Double> emptyDistances = this.emptyDistances;

        double averageSeatCount = 0;
        double averageSeatCountToUni = 0;
        int countOfToUni = 0;
        int countOfHome = 0;
        double averageSeatCountHome = 0;
        double averageSeatCountForWilling = 0;
        int countOfWilling=0;
        double averageSeatCountToUniForWilling = 0;
        int countOfWillingToUni=0;
        double averageSeatCountHomeForWilling = 0;
        int countOfWillingHome=0;
        double averageSeatCountForGroups = 0;
        int countOfGroups=0;
        double averageSeatCountToUniForGroups = 0;
        int countOfGroupsToUni=0;
        double averageSeatCountHomeForGroups = 0;
        int countOfGroupsHome=0;
        double averageSeatCountForNonDrivers = 0;
        int countOfNonDrivers=0;
        double averageSeatCountToUniForNonDrivers = 0;
        int countOfNonDriversToUni=0;
        double averageSeatCountHomeForNonDrivers = 0;
        int countOfNonDriversHome=0;

        if(this.checkIfConstraintsAreBroken(agents)){
            throw new RuntimeException("ridepooling constraints.");
        }

        List<String> dataLines = new ArrayList<>();
        List<String> dataLines2 = new ArrayList<>();
        List<String> dataLines3 = new ArrayList<>();
        List<String> dataLines4 = new ArrayList<>();

        dataLines.add("Agent Id,ToRide Id,BackRide Id,willing,droveTheirCar,GesamtKilometer,GesamtMinuten,GesamtCO2,GesamtKosten,HinKilometer,HinMinuten,HinCO2,HinKosten,RueckKilometer,RueckMinuten,RueckCO2,RueckKosten");
        dataLines4.add("Ride Id,AgentCount,unwillingRide,privateCarRide,StopCount,StartTime,EndTime,empty_Distance,StartPosition-Latitude,StartPosition-Longitude,EndPosition-Latitude,EndPosition-Longitude");

        for(Ride ride : rides){
            dataLines4.add(ride.getId() + "," + ride.getAgents().size() + "," + (ride.getDriver()!=null && !ride.getDriver().isWillingToUseAlternatives()) + "," + (ride.getDriver()!=null) + "," + ride.getExtraStops().size() + "," + ride.getStartTime() + "," + ride.getEndTime() + "," + (emptyDistances.containsKey(ride) ? emptyDistances.get(ride)/1000 : 0.0) + "," + ride.getStartPosition().getLatitude() + "," + ride.getStartPosition().getLongitude() + "," + ride.getEndPosition().getLatitude() + "," + ride.getEndPosition().getLongitude());
            averageSeatCount += ride.getAgents().size();
            if(ride.getDriver()!=null && ride.getDriver().isWillingToUseAlternatives()){
                averageSeatCountForWilling += ride.getAgents().size();
                countOfWilling++;
            }
            if(ride.getAgents().size()>1){
                averageSeatCountForGroups += ride.getAgents().size();
                countOfGroups++;
            }
            if(ride.getDriver()==null || !ride.getVehicle().equals(ride.getDriver().getCar())){
                averageSeatCountForNonDrivers += ride.getAgents().size();
                countOfNonDrivers++;
            }
            if(ride.getTypeOfGrouping()== DirectionType.DRIVETOUNI){
                if(ride.getDriver()!=null && ride.getDriver().isWillingToUseAlternatives()){
                    averageSeatCountToUniForWilling += ride.getAgents().size();
                    countOfWillingToUni++;
                }
                if(ride.getAgents().size()>1){
                    averageSeatCountToUniForGroups += ride.getAgents().size();
                    countOfGroupsToUni++;
                }
                if(ride.getDriver()==null || !ride.getVehicle().equals(ride.getDriver().getCar())){
                    averageSeatCountToUniForNonDrivers += ride.getAgents().size();
                    countOfNonDriversToUni++;
                }
                averageSeatCountToUni += ride.getAgents().size();
                countOfToUni++;
            }else{
                if(ride.getDriver()!=null && ride.getDriver().isWillingToUseAlternatives()){
                    averageSeatCountHomeForWilling += ride.getAgents().size();
                    countOfWillingHome++;
                }
                if(ride.getAgents().size()>1){
                    averageSeatCountHomeForGroups += ride.getAgents().size();
                    countOfGroupsHome++;
                }
                if(ride.getDriver()==null || !ride.getVehicle().equals(ride.getDriver().getCar())){
                    averageSeatCountHomeForNonDrivers += ride.getAgents().size();
                    countOfNonDriversHome++;
                }
                averageSeatCountHome += ride.getAgents().size();
                countOfHome++;
            }
        }
        averageSeatCount = averageSeatCount/rides.size();
        averageSeatCountToUni = averageSeatCountToUni/countOfToUni;
        averageSeatCountHome = averageSeatCountHome/countOfHome;
        averageSeatCountForWilling = averageSeatCountForWilling/countOfWilling;
        averageSeatCountToUniForWilling = averageSeatCountToUniForWilling/countOfWillingToUni;
        averageSeatCountHomeForWilling = averageSeatCountHomeForWilling/countOfWillingHome;
        averageSeatCountForGroups = averageSeatCountForGroups/countOfGroups;
        averageSeatCountToUniForGroups = averageSeatCountToUniForGroups/countOfGroupsToUni;
        averageSeatCountHomeForGroups = averageSeatCountHomeForGroups/countOfGroupsHome;
        averageSeatCountForNonDrivers = averageSeatCountForNonDrivers/countOfNonDrivers;
        averageSeatCountToUniForNonDrivers = averageSeatCountToUniForNonDrivers/countOfNonDriversToUni;
        averageSeatCountHomeForNonDrivers = averageSeatCountHomeForNonDrivers/countOfNonDriversHome;

        dataLines2.add("averageSeatCount,averageSeatCountToUni,averageSeatCountHome,count of driving students,count of agents,count of rides,count of rides to uni,count of rides home,averageSeatCountForWilling,countOfWilling,averageSeatCountToUniForWilling,countOfWillingToUni,averageSeatCountHomeForWilling,countOfWillingHome,averageSeatCountForGroups,countOfGroups,averageSeatCountToUniForGroups,countOfGroupsToUni,averageSeatCountHomeForGroups,countOfGroupsHome,averageSeatCountForNonDrivers,countOfNonDrivers,averageSeatCountToUniForNonDrivers,countOfNonDriversToUni,averageSeatCountHomeForNonDrivers,countOfNonDriversHome");
        dataLines2.add(averageSeatCount + "," + averageSeatCountToUni + "," + averageSeatCountHome + "," + drivingAgents.size() + "," + agents.size() + "," + rides.size() + "," + countOfToUni + "," + countOfHome + "," + averageSeatCountForWilling + "," + countOfWilling + "," + averageSeatCountToUniForWilling + "," + countOfWillingToUni + "," + averageSeatCountHomeForWilling + "," + countOfWillingHome + "," + averageSeatCountForGroups + "," + countOfGroups + "," + averageSeatCountToUniForGroups + "," + countOfGroupsToUni + "," + averageSeatCountHomeForGroups + "," + countOfGroupsHome + "," + averageSeatCountForNonDrivers + "," + countOfNonDrivers + "," + averageSeatCountToUniForNonDrivers + "," + countOfNonDriversToUni + "," + averageSeatCountHomeForNonDrivers + "," + countOfNonDriversHome);
        dataLines3.add("Vehicle Id,GesamtKilometer,GesamtMinuten,GesamtCO2,GesamtKosten,countOfRides,standingTime,mostDrivenGroup,averageSeatCount,emptyDistance");
        for(Agent a : kmTravelled.keySet()){
            Ride toUni = agentToRides.get(a).get(0);
            Set<Object> set = new HashSet<>();
            set.add(a);
            set.add(toUni);
            double km;
            double min;
            double co2;
            double cost;
            if(agentToRides.get(a).size()>1){
                Ride home = agentToRides.get(a).get(1);
                Set<Object> set2 = new HashSet<>();
                set2.add(a);
                set2.add(home);
                km = oneWayKmTravelled.get(set2);
                min = oneWayMinutesTravelled.get(set2);
                co2 = oneWayEmissions.get(set2);
                cost = oneWayCosts.get(set2);
            }else{
                km = 0.0;
                min = 0.0;
                co2 = 0.0;
                cost = 0.0;
            }
            dataLines.add(a.getId() + "," + agentToRides.get(a).get(0).getId() + "," + agentToRides.get(a).get(1).getId() + "," + a.isWillingToUseAlternatives() + "," + toUni.getVehicle().equals(a.getCar()) + "," + kmTravelled.get(a) + "," + minutesTravelled.get(a) + "," + emissions.get(a) + "," + costs.get(a) + "," + oneWayKmTravelled.get(set) + "," + oneWayMinutesTravelled.get(set) + "," + oneWayEmissions.get(set) + "," + oneWayCosts.get(set) + "," + km + "," + min + "," + co2 + "," + cost);
        }

        for(Vehicle v : vehicleKmTravelled.keySet()){
            double averageSeatCountForVehicle = 0.0;
            int[] groupNumbers = new int[vehicleToRides.get(v).size()];
            int counter = 0;
            for(Ride ride : vehicleToRides.get(v)){
                groupNumbers[counter] = ride.getGroupNumber();
                averageSeatCountForVehicle += ride.getAgents().size();
                counter++;
            }
            int mostGroupNumber = CommonFunctionHelper.getPopularElement(groupNumbers);
            averageSeatCountForVehicle = averageSeatCountForVehicle/vehicleToRides.get(v).size();

            List<LocalDateTime> vehicleTimes = new ArrayList<>(vehiclePositions.get(v).keySet());
            Collections.sort(vehicleTimes);
            long standingTime = 0L;
            for(counter=vehicleTimes.size()%2==0?1:2;counter<vehicleTimes.size()-2; counter+=2){
                standingTime += ChronoUnit.MINUTES.between(vehicleTimes.get(counter-1),vehicleTimes.get(counter));
            }

            dataLines3.add(v.getId() + "," + vehicleKmTravelled.get(v) + "," + vehicleMinutesTravelled.get(v) + "," + vehicleEmissions.get(v) + "," + vehicleCosts.get(v) + "," + vehicleToRides.get(v).size() + "," + standingTime + "," + mostGroupNumber + "," + averageSeatCountForVehicle + "," + vehicleEmptyDistances.get(v)/1000);
        }

        File csvOutputFile = new File("rpResults.csv");
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            for(String data : dataLines){
                pw.println(data);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        String[] results = "rpResults.csv".split("\\.");
        String newResultFileName = results[0] + "AdditionalData.csv";
        File csvOutputFile2 = new File(newResultFileName);
        try (PrintWriter pw = new PrintWriter(csvOutputFile2)) {
            for(String data : dataLines2){
                pw.println(data);
            }
        }catch (Exception e){
            e.printStackTrace();
        }


        newResultFileName = results[0] + "ForVehicles.csv";
        File csvOutputFile3 = new File(newResultFileName);
        try (PrintWriter pw = new PrintWriter(csvOutputFile3)) {
            for(String data : dataLines3){
                pw.println(data);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        newResultFileName = results[0] + "ForRides.csv";
        File csvOutputFile4 = new File(newResultFileName);
        try (PrintWriter pw = new PrintWriter(csvOutputFile4)) {
            for(String data : dataLines4){
                pw.println(data);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public Map<Agent, Double> getEmissions() {
        return null;
    }

    @Override
    public Map<Agent, Double> getCosts() {
        return null;
    }

    @Override
    public Map<Agent, Double> getKmTravelled() {
        return null;
    }

    @Override
    public Map<Agent, Double> getMinutesTravelled() {
        return null;
    }

    @Override
    public Map<Set<Object>, Double> getOneWayEmissions() {
        return null;
    }

    @Override
    public Map<Set<Object>, Double> getOneWayCosts() {
        return null;
    }

    @Override
    public Map<Set<Object>, Double> getOneWayKmTravelled() {
        return null;
    }

    @Override
    public Map<Set<Object>, Double> getOneWayMinutesTravelled() {
        return null;
    }

    @Override
    public Map<Agent, List<Ride>> getAgentToRides() {
        return null;
    }


    public void findMatch(Request request) {
        LocalDateTime requestIntervalStart;
        LocalDateTime requestIntervalEnd;


        Match toUniMatch = getCorrectBusMatch(request, DirectionType.DRIVETOUNI, null);
        Match homeMatch = null;

        if (toUniMatch != null) {
            homeMatch = getCorrectBusMatch(request, DirectionType.DRIVEHOME, toUniMatch);
        }
        if (toUniMatch != null && homeMatch != null) {
            requestIntervalStart = request.getArrivalInterval().getStart();
            requestIntervalEnd = request.getArrivalInterval().getEnd();
            setNewMatchPartner(toUniMatch, request, requestIntervalStart, requestIntervalEnd);

            requestIntervalStart = request.getDepartureInterval().getStart();
            requestIntervalEnd = request.getDepartureInterval().getEnd();
            setNewMatchPartner(homeMatch, request, requestIntervalStart, requestIntervalEnd);
        } else {
            handleDrivingAgent(request);
        }
    }


    public void handleDrivingAgent(Request request) {
        Agent agent = request.getAgent();
        drivingAgents.add(agent);
        List<Ride> agentRides = new ArrayList<>();
        List<Agent> agentList = new ArrayList<>();
        agentList.add(agent);
        Ride toUniRide, fromUniRide;
        LocalDateTime driveStartTime = CommonFunctionHelper.calculateNecessaryDriveStartTime(agent.getRequest());
        toUniRide = new Ride(request.getHomePosition(), request.getDropOffPosition(), driveStartTime, request.getFavoredArrivalTime(), agent.getCar(), agent, DirectionType.DRIVETOUNI, agentList);
        fromUniRide = new Ride(request.getDropOffPosition(), request.getHomePosition(), request.getDepartureInterval().getStart(), null, agent.getCar(), agent, DirectionType.DRIVEHOME, agentList);
        fromUniRide.setEndTime(CommonFunctionHelper.getSimpleArrivalTime(this.graphHopper,fromUniRide.getStartTime(),fromUniRide.getStartPosition(),fromUniRide.getEndPosition()));

        agentRides.add(toUniRide);
        agentRides.add(fromUniRide);
        agentToRides.put(agent, agentRides);
        rides.add(toUniRide);
        rides.add(fromUniRide);
        Set<Object> set = new HashSet<>();
        set.add(toUniRide);
        set.add(agent);

        ResponsePath path = CommonFunctionHelper.getSimpleBestGraphhopperPath(this.graphHopper,toUniRide.getStartPosition(),toUniRide.getEndPosition());
        double distance = path.getDistance()/1000.0;
        long timeInMinutes = path.getTime()/60000L;
        double time = (double) timeInMinutes;
        double pathCosts = distance*agent.getCar().getConsumptionPerKm()*agent.getCar().getPricePerLiter();
        double pathEmissions = distance*agent.getCar().getConsumptionPerKm()*agent.getCar().getCo2EmissionPerLiter();


        oneWayMinutesTravelled.put(set, time);
        oneWayKmTravelled.put(set, distance);
        oneWayEmissions.put(set, pathEmissions);
        oneWayCosts.put(set, pathCosts);

        Set<Object> set2 = new HashSet<>();
        set2.add(fromUniRide);
        set2.add(agent);
        path = CommonFunctionHelper.getSimpleBestGraphhopperPath(this.graphHopper,toUniRide.getStartPosition(),toUniRide.getEndPosition());
        distance = path.getDistance()/1000.0;
        timeInMinutes = path.getTime()/60000L;
        time = (double) timeInMinutes;
        pathCosts = distance*agent.getCar().getConsumptionPerKm()*agent.getCar().getPricePerLiter();
        pathEmissions = distance*agent.getCar().getConsumptionPerKm()*agent.getCar().getCo2EmissionPerLiter();


        oneWayMinutesTravelled.put(set2, time);
        oneWayKmTravelled.put(set2, distance);
        oneWayEmissions.put(set2, pathEmissions);
        oneWayCosts.put(set2, pathCosts);

        emissions.put(agent, oneWayEmissions.get(set) + oneWayEmissions.get(set2));
        costs.put(agent, oneWayCosts.get(set) + oneWayCosts.get(set2));
        kmTravelled.put(agent, oneWayKmTravelled.get(set) + oneWayKmTravelled.get(set2));
        minutesTravelled.put(agent, oneWayMinutesTravelled.get(set) + oneWayMinutesTravelled.get(set2));
    }


    public void setNewMatchPartner(Match match, Request request, LocalDateTime requestIntervalStart, LocalDateTime requestIntervalEnd) {
        vehiclePositions.get(match.getVehicle()).remove(matchToStartTime.get(match));
        vehiclePositions.get(match.getVehicle()).remove(matchToEndTime.get(match));

        LocalDateTime startTime = this.matchToStartTime.get(match);
        if (startTime != null && this.timeToMatch.containsKey(startTime)) {
            List<Match> matchList = this.timeToMatch.get(startTime);
            matchList.remove(match);
            if (matchList.isEmpty()) {
                this.timeToMatch.remove(startTime);
            } else {
                this.timeToMatch.put(startTime, matchList);
            }
        }

        List<Agent> matchPeople = match.getAgents();
        matchPeople.add(request.getAgent());
        match.setAgents(matchPeople);

        Map<Coordinate, String> map = match.getDifferentStops();
        if (match.getDifferentStops().containsKey(request.getDropOffPosition())) {
            map.put(request.getDropOffPosition(), CommonFunctionHelper.getNewStopInterval(match.getDifferentStops().get(request.getDropOffPosition()), requestIntervalStart, requestIntervalEnd));
        } else {
            map.put(request.getDropOffPosition(), CommonFunctionHelper.getIntervalString(requestIntervalStart, requestIntervalEnd));
        }
        map.put(request.getHomePosition(), "");
        match.setDifferentStops(map);

        VehicleRoutingProblemSolution solution = temporaryMatchToSolution.get(match);
        VehicleRoute route = ((List<VehicleRoute>) solution.getRoutes()).get(0);
        TourActivity startActivity = route.getActivities().get(0);
        TourActivity endActivity = route.getActivities().get(route.getActivities().size() - 1);

        if (match.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
            match.setTimeIntervalStart(LocalDateTime.parse(match.getDifferentStops().get(Coordinate.locationToCoordinate(endActivity.getLocation())).split("-")[0], GeneralManager.dateTimeFormatter));
            match.setTimeIntervalEnd(LocalDateTime.parse(match.getDifferentStops().get(Coordinate.locationToCoordinate(endActivity.getLocation())).split("-")[1], GeneralManager.dateTimeFormatter));
        } else if (match.getTypeOfGrouping() == DirectionType.DRIVEHOME) {
            match.setTimeIntervalStart(LocalDateTime.parse(match.getDifferentStops().get(Coordinate.locationToCoordinate(startActivity.getLocation())).split("-")[0], GeneralManager.dateTimeFormatter));
            match.setTimeIntervalEnd(LocalDateTime.parse(match.getDifferentStops().get(Coordinate.locationToCoordinate(startActivity.getLocation())).split("-")[1], GeneralManager.dateTimeFormatter));
        }
        List<Long> list = new ArrayList<>();
        list.add(match.getId());
        list.add(request.getAgent().getId());
        if (matchToPossibleNewIntervalStartTime.containsKey(list)) {
            match.setTimeIntervalStart(matchToPossibleNewIntervalStartTime.get(list));
            matchToPossibleNewIntervalStartTime.remove(list);
        }

        int requestGroup = assignRequestToGroup(request);
        if (match.getGroupNumber() == -1 && requestGroup != -1) {
            match.setGroupNumber(requestGroup);
        }

        if (!activeMatches.contains(match)) {
            activeMatches.add(match);
        }
        List<Match> agentMatches = new ArrayList<>();
        if (agentToMatches.containsKey(request.getAgent())) {
            agentMatches = agentToMatches.get(request.getAgent());
        }
        agentMatches.add(match);
        agentToMatches.put(request.getAgent(), agentMatches);

        if (!vehicleToMatches.get(match.getVehicle()).contains(match)) {
            List<Match> vehicleMatches = vehicleToMatches.get(match.getVehicle());
            vehicleMatches.add(match);
            vehicleMatches.sort(Comparator.comparing(Match::getTimeIntervalEnd));
            vehicleToMatches.put(match.getVehicle(), vehicleMatches);
        }
        assignedRequests.put(request, match);
        this.setDriveStartAndEndTime(match, solution);
    }


    public void setDriveStartAndEndTime(Match match, VehicleRoutingProblemSolution solution) {

        LocalDateTime startTime = getStartTime(match.getTimeIntervalStart(), match.getTypeOfGrouping(), solution);
        LocalDateTime endTime = getEndTime(solution, startTime);

        this.matchToStartTime.put(match, startTime);
        this.matchToEndTime.put(match, endTime);
        Map<LocalDateTime, Coordinate> map = vehiclePositions.get(match.getVehicle());
        List<LocalDateTime> times = new ArrayList<>(map.keySet());
        if (times.contains(startTime) || times.contains(endTime) || startTime.equals(endTime)) {
            Collections.sort(times);
        }
        map.put(startTime, match.getStartPosition());
        map.put(endTime, match.getEndPosition());

        if (times.size() % 2 == 0) {
            Collections.sort(times);
        }
        vehiclePositions.put(match.getVehicle(), map);

        List<Match> matchList = new ArrayList<>();
        if (this.timeToMatch.containsKey(startTime)) {
            matchList = this.timeToMatch.get(startTime);
        }
        matchList.add(match);
        this.timeToMatch.put(startTime, matchList);
        matchToSolution.put(match, solution);
    }


    public LocalDateTime getStartTime(LocalDateTime timeIntervalStart, DirectionType directionType, VehicleRoutingProblemSolution solution) {
        List<VehicleRoute> routes = (List<VehicleRoute>) solution.getRoutes();
        VehicleRoute route = routes.get(0);
        LocalDateTime startTime;
        if (directionType == DirectionType.DRIVETOUNI) {
            startTime = timeIntervalStart.plusMinutes((long) ((route.getActivities().get(route.getActivities().size() - 1).getEndTime() - stopTime) - route.getActivities().get(route.getActivities().size() - 1).getTheoreticalEarliestOperationStartTime())).minusMinutes((long) (solution.getCost() - stopTime));
        } else {
            startTime = timeIntervalStart.plusMinutes(((long) ((route.getActivities().get(0).getEndTime() - stopTime) - route.getActivities().get(0).getTheoreticalEarliestOperationStartTime()))).minusMinutes((long) (route.getActivities().get(0).getArrTime() - route.getStart().getEndTime()));
        }
        return startTime;
    }


    public LocalDateTime getEndTime(VehicleRoutingProblemSolution solution, LocalDateTime startTime) {
        return startTime.plusMinutes((long) solution.getCost());
    }


    public Match getBestMatch(List<Match> eligibleMatches) {
        return eligibleMatches.stream().reduce((a, b) -> temporaryMatchToSolution.get(a).getCost() < temporaryMatchToSolution.get(b).getCost() ? a : b).get();
    }


    public Match getSmallerMatchDistance(Match m1, Match m2, CoordinateXY firstCoordinate) {
        try {
            double m1Distance = JTS.orthodromicDistance(new CoordinateXY(m1.getStartPosition().getLongitude(), m1.getStartPosition().getLatitude()), firstCoordinate, DefaultGeographicCRS.WGS84);
            double m2Distance = JTS.orthodromicDistance(new CoordinateXY(m2.getStartPosition().getLongitude(), m2.getStartPosition().getLatitude()), firstCoordinate, DefaultGeographicCRS.WGS84);
            if (m1Distance < m2Distance) {
                return m1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return m2;
    }


    public Match getCorrectBusMatch(Request request, DirectionType directionType, Match firstMatch) {
        LocalDateTime start;
        LocalDateTime end;
        Coordinate endPosition;
        Coordinate firstPosition;

        if (directionType == DirectionType.DRIVETOUNI) {
            start = request.getArrivalInterval().getStart();
            end = request.getArrivalInterval().getEnd();
            endPosition = request.getDropOffPosition();
            firstPosition = request.getHomePosition();
        } else {
            start = request.getDepartureInterval().getStart();
            end = request.getDepartureInterval().getEnd();
            endPosition = request.getHomePosition();
            firstPosition = request.getDropOffPosition();
        }
        org.locationtech.jts.geom.CoordinateXY firstCoordinate = new CoordinateXY(firstPosition.getLongitude(), firstPosition.getLatitude());
        int groupNumber = assignRequestToGroup(request);

        GeneralManager.compareRequest = request;
        List<Match> matchList = this.activeMatches.stream()
                .filter(m -> m.getTypeOfGrouping() == directionType && (groupNumber == -1 || m.getGroupNumber() == groupNumber || m.getGroupNumber() == -1 || m.getGroupNumber() == (groupNumber == 1 ? countOfGroups : groupNumber - 1) || m.getGroupNumber() == (groupNumber == countOfGroups ? 1 : groupNumber + 1)) && m.getAgents().size() < m.getVehicle().getSeatCount() && (m.getDifferentStops().containsKey(request.getDropOffPosition()) && CommonFunctionHelper.isOverlapping(LocalDateTime.parse(m.getDifferentStops().get(request.getDropOffPosition()).split("-")[0], GeneralManager.dateTimeFormatter), LocalDateTime.parse(m.getDifferentStops().get(request.getDropOffPosition()).split("-")[1], GeneralManager.dateTimeFormatter), start, end) || !m.getDifferentStops().containsKey(request.getDropOffPosition()) && stopsAreOverlapping(m, request, start, end)))
                .collect(Collectors.toList());
        //List<Request> compareRequests = new ArrayList<>(assignedRequests.keySet().stream().filter(r -> (r.getDropOffPosition().equals(request.getDropOffPosition() && CommonFunctionHelper.isOverlapping(r.)))));
        //compareRequests.sort(Comparator.comparing(r -> Math.abs(r.getHomePosition().getLatitude()-request.getHomePosition().getLatitude()) + Math.abs(r.getHomePosition().getLongitude()-request.getHomePosition().getLongitude())));


        if (!matchList.isEmpty()) {
            //matchList.sort(Comparator.comparing(m -> matchToSolution.get(m).getCost()));

            matchList.sort(Comparator.comparing(m -> {
                double difference = Double.MAX_VALUE;
                Set<Coordinate> coos = new HashSet<>(m.getDifferentStops().keySet());
                coos.removeIf(c -> !m.getDifferentStops().get(c).isEmpty());
                for (Coordinate coordinate : coos) {
                    double newDif = 0;
                    newDif += Math.abs(coordinate.getLatitude() - request.getHomePosition().getLatitude());
                    newDif += Math.abs(coordinate.getLongitude() - request.getHomePosition().getLongitude());
                    if (newDif < difference) {
                        difference = newDif;
                    }
                }
                int intervalValue = m.getDifferentStops().containsKey(request.getDropOffPosition()) ? 0 : 100000;
                int groupValue = m.getGroupNumber() == groupNumber ? 0 : 100000000;
                return difference + intervalValue + groupValue;
            }));
            //Collections.reverse(matchList);
        }

        List<Vehicle> possibleFreeVehicles = new ArrayList<>(vehicles);
        Match bestMatch = null;
        List<Match> bestMatches = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            if (i == matchList.size()) break;
            Match match = matchList.get(i);
            if (isMatchDoable(match, firstMatch, directionType, request)) {
                bestMatches.add(match);
            }
            if (CommonFunctionHelper.isOverlapping(this.matchToStartTime.get(match), this.matchToStartTime.get(match).plusMinutes((long) matchToSolution.get(match).getCost()), start, end)) {
                possibleFreeVehicles.remove(match.getVehicle());
            }
        }
        if (!bestMatches.isEmpty()) {
            bestMatch = getBestMatch(bestMatches);
        } else {
            for (int i = 5; i < matchList.size(); i++) {
                Match match = matchList.get(i);
                if (isMatchDoable(match, firstMatch, directionType, request)) {
                    bestMatch = match;
                    break;
                }
                if (CommonFunctionHelper.isOverlapping(this.matchToStartTime.get(match), this.matchToStartTime.get(match).plusMinutes((long) matchToSolution.get(match).getCost()), start, end)) {
                    possibleFreeVehicles.remove(match.getVehicle());
                }
            }
        }

        List<Vehicle> possibleFreeStartVehicles = new ArrayList<>();
        List<Vehicle> possibleFreeEndVehicles = new ArrayList<>();
        for (Vehicle v : possibleFreeVehicles) {
            List<Object> result = vehicleIsFree(v, start, end, firstPosition, endPosition, directionType);
            if ((boolean) result.get(0)) {
                if (result.get(1).equals("start")) {
                    possibleFreeStartVehicles.add(v);
                } else {
                    possibleFreeEndVehicles.add(v);
                }
            }
        }

        possibleFreeVehicles.removeIf(v -> !(possibleFreeStartVehicles.contains(v) || possibleFreeEndVehicles.contains(v)));

        TransportCosts tc = new TransportCosts();
        possibleFreeVehicles.sort(Comparator.comparing(v -> {
            try {
                LocalDateTime referenceTime = possibleFreeStartVehicles.contains(v) ? start : end;
                if (referenceTime.isBefore(LocalDateTime.parse("02.02.2023 04:00:00", GeneralManager.dateTimeFormatter)) && possibleFreeStartVehicles.contains(v)) {
                    referenceTime = end;
                }
                if (directionType == DirectionType.DRIVETOUNI) {
                    referenceTime = referenceTime.minusMinutes((long) tc.getTransportTime(Coordinate.coordinateToLocation(request.getHomePosition()), Coordinate.coordinateToLocation(request.getDropOffPosition()), 0, null, null));
                }
                LocalDateTime lastStandingTime = getLastVehiclePositionTime(v, referenceTime);
                if (lastStandingTime != null) {
                    //double driveTime = tc.getTransportTime(Coordinate.coordinateToLocation(vehiclePositions.get(v).get(lastStandingTime)),Coordinate.coordinateToLocation(firstPosition),0,null,null);
                    //long minutes = ChronoUnit.MINUTES.between(lastStandingTime,referenceTime.minusMinutes((long) driveTime));;
                    //return minutes;
                    Coordinate lastStandingPosition = vehiclePositions.get(v).get(getLastVehiclePositionTime(v, lastStandingTime));
                    return JTS.orthodromicDistance(new CoordinateXY(lastStandingPosition.getLongitude(), lastStandingPosition.getLatitude()), firstCoordinate, DefaultGeographicCRS.WGS84);
                } else {
                    return Double.MAX_VALUE;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Double.MAX_VALUE;
            }
        }));

        for (Vehicle vehicle : possibleFreeVehicles) {
            if (vehiclePositions.get(vehicle).get(getLastVehiclePositionTime(vehicle, end)) == null) {
                break;
            }
            Match m = new Match(new ArrayList<>(), new HashMap<>(), null, vehicle, vehiclePositions.get(vehicle).get(getLastVehiclePositionTime(vehicle, end)), endPosition, directionType, start, end);
            m.setGroupNumber(assignRequestToGroup(request));
            if (bestMatch != null) {
                Set<Coordinate> coos = new HashSet<>(bestMatch.getDifferentStops().keySet());
                Match finalBestMatch = bestMatch;
                if (bestMatch.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
                    coos.removeIf(c -> !finalBestMatch.getDifferentStops().get(c).isEmpty());
                } else {
                    coos.removeIf(c -> finalBestMatch.getDifferentStops().get(c).isEmpty());
                }
                try {
                    for (Coordinate c : coos) {
                        if (JTS.orthodromicDistance(new CoordinateXY(c.getLongitude(), c.getLatitude()), firstCoordinate, DefaultGeographicCRS.WGS84) < JTS.orthodromicDistance(new CoordinateXY(m.getStartPosition().getLongitude(), m.getStartPosition().getLatitude()), firstCoordinate, DefaultGeographicCRS.WGS84)) {
                            return bestMatch;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            }
            if (wouldDriveBy(request, m, false, LocalDateTime.parse("02.02.2023 04:00:00", GeneralManager.dateTimeFormatter))) {
                return m;
            }
        }
        return bestMatch;
    }


    public List<Object> vehicleIsFree(Vehicle vehicle, LocalDateTime start, LocalDateTime end, Coordinate firstPosition, Coordinate endPosition, DirectionType directionType) {
        List<Object> result = new ArrayList<>();
        List<LocalDateTime> vehicleTimes = new ArrayList<>(vehiclePositions.get(vehicle).keySet());
        TransportCosts tc = new TransportCosts();
        boolean vehicleFreeForStart = true;
        boolean vehicleFreeForEnd = true;
        boolean vehicleFreeForBetween = true;

        if (vehicleTimes.size() == 1) {
            if (!vehicleTimes.get(0).isAfter(start)) {
                result.add(true);
                result.add("end");
            } else {
                result.add(false);
            }
            return result;
        }

        LocalDateTime firstTimeWithStart;
        LocalDateTime endTimeWithStart;
        LocalDateTime firstTimeWithEnd;
        LocalDateTime endTimeWithEnd;
        double drivetime = tc.getTransportTime(Coordinate.coordinateToLocation(firstPosition), Coordinate.coordinateToLocation(endPosition), 0, null, null);
        if (directionType == DirectionType.DRIVETOUNI) {
            firstTimeWithStart = start.minusMinutes((long) drivetime);
            endTimeWithStart = start;
            firstTimeWithEnd = end.minusMinutes((long) drivetime);
            endTimeWithEnd = end;
        } else {
            firstTimeWithStart = start;
            endTimeWithStart = start.plusMinutes((long) drivetime);
            firstTimeWithEnd = end;
            endTimeWithEnd = end.plusMinutes((long) drivetime);
        }

        /*if(vehicleTimes.contains(start) || vehicleTimes.contains(end)){
            return false;
        }*/
        Collections.sort(vehicleTimes);
        List<LocalDateTime> vehicleReferences = new ArrayList<>(vehicleTimes);
        vehicleReferences.add(firstTimeWithStart);
        vehicleReferences.add(endTimeWithStart);
        Collections.sort(vehicleReferences);

        int indexStart = vehicleReferences.indexOf(firstTimeWithStart);
        int indexEnd = vehicleReferences.indexOf(endTimeWithStart);
        if (indexEnd - indexStart > 1) {
            vehicleFreeForStart = false;
        }
        int indexOfBefore = indexStart - 1;
        int indexOfAfter = indexEnd - 1;
        if (vehicleFreeForStart && (vehicleTimes.size() % 2 == 0 && indexOfBefore % 2 == 1 && indexOfAfter % 2 == 0 || vehicleTimes.size() % 2 == 1 && indexOfBefore % 2 == 0 && indexOfAfter % 2 == 1)) {
            result.add(true);
            result.add("start");
            return result;
        }

        vehicleReferences = new ArrayList<>(vehicleTimes);
        vehicleReferences.add(firstTimeWithEnd);
        vehicleReferences.add(endTimeWithEnd);
        Collections.sort(vehicleReferences);

        indexStart = vehicleReferences.indexOf(firstTimeWithEnd);
        indexEnd = vehicleReferences.indexOf(endTimeWithEnd);
        if (indexEnd - indexStart > 1) {
            vehicleFreeForEnd = false;
        }
        indexOfBefore = indexStart - 1;
        indexOfAfter = indexEnd - 1;
        if (vehicleFreeForEnd && (vehicleTimes.size() % 2 == 0 && indexOfBefore % 2 == 1 && indexOfAfter % 2 == 0 || vehicleTimes.size() % 2 == 1 && indexOfBefore % 2 == 0 && indexOfAfter % 2 == 1)) {
            result.add(true);
            result.add("end");
            return result;
        }

        if (drivetime < ChronoUnit.MINUTES.between(endTimeWithStart, firstTimeWithEnd)) {
            vehicleReferences = new ArrayList<>(vehicleTimes);
            vehicleReferences.add(endTimeWithStart);
            vehicleReferences.add(firstTimeWithEnd);
            Collections.sort(vehicleReferences);

            indexStart = vehicleReferences.indexOf(endTimeWithStart);
            indexEnd = vehicleReferences.indexOf(firstTimeWithEnd);
            if (indexEnd - indexStart > 1) {
                vehicleFreeForBetween = false;
            }
            indexOfBefore = indexStart - 1;
            indexOfAfter = indexEnd - 1;
            if (vehicleFreeForBetween && (vehicleTimes.size() % 2 == 0 && indexOfBefore % 2 == 1 && indexOfAfter % 2 == 0 || vehicleTimes.size() % 2 == 1 && indexOfBefore % 2 == 0 && indexOfAfter % 2 == 1)) {
                result.add(true);
                result.add("end");
                return result;
            }
        }

        result.add(false);
        return result;
    }


    public boolean stopsAreOverlapping(Match match, Request request, LocalDateTime start, LocalDateTime end) {
        List<Coordinate> coordinates = new ArrayList<>(match.getDifferentStops().keySet());
        coordinates.removeIf(c -> match.getDifferentStops().get(c).isEmpty());
        if (match.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
            for (Coordinate coordinate : coordinates) {
                LocalDateTime stopStart = LocalDateTime.parse(match.getDifferentStops().get(coordinate).split("-")[0], GeneralManager.dateTimeFormatter);
                LocalDateTime stopEnd = LocalDateTime.parse(match.getDifferentStops().get(coordinate).split("-")[1], GeneralManager.dateTimeFormatter);
                if (CommonFunctionHelper.isOverlapping(stopStart, stopEnd, start.plusSeconds(this.secondsBetweenDropOffs.get(request.getUniPLZ() + "-" + ModeExecutionManager.turnUniCoordinateIntoPostcode(coordinate))), end.plusSeconds(this.secondsBetweenDropOffs.get(request.getUniPLZ() + "-" + ModeExecutionManager.turnUniCoordinateIntoPostcode(coordinate)))) || CommonFunctionHelper.isOverlapping(stopStart.plusSeconds(this.secondsBetweenDropOffs.get(request.getUniPLZ() + "-" + ModeExecutionManager.turnUniCoordinateIntoPostcode(coordinate))), stopEnd.plusSeconds(this.secondsBetweenDropOffs.get(request.getUniPLZ() + "-" + ModeExecutionManager.turnUniCoordinateIntoPostcode(coordinate))), start, end)) {
                    return true;
                }
            }
        } else {
            for (Coordinate coordinate : coordinates) {
                if (CommonFunctionHelper.isOverlapping(match.getTimeIntervalStart().minusSeconds(this.secondsBetweenDropOffs.get(ModeExecutionManager.turnUniCoordinateIntoPostcode(coordinate) + "-" + request.getUniPLZ())), match.getTimeIntervalEnd(), start.plusSeconds(this.secondsBetweenDropOffs.get(ModeExecutionManager.turnUniCoordinateIntoPostcode(coordinate) + "-" + request.getUniPLZ())), end) || CommonFunctionHelper.isOverlapping(match.getTimeIntervalStart().plusSeconds(this.secondsBetweenDropOffs.get(ModeExecutionManager.turnUniCoordinateIntoPostcode(coordinate) + "-" + request.getUniPLZ())), match.getTimeIntervalEnd(), start.minusSeconds(this.secondsBetweenDropOffs.get(ModeExecutionManager.turnUniCoordinateIntoPostcode(coordinate) + "-" + request.getUniPLZ())), end)) {
                    return true;
                }
            }
        }

        return false;
    }


    public boolean wouldDriveBy(Request request, Match match, boolean justGetSolution, LocalDateTime earliestDriveStart) {

        final int WEIGHT_INDEX = 0;
        VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("vehicleType").addCapacityDimension(WEIGHT_INDEX, match.getVehicle().getSeatCount()).setCostPerServiceTime(1);
        VehicleType vehicleType = vehicleTypeBuilder.build();

        VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance("vehicle");
        LocalDateTime startTime = matchToStartTime.get(match);
        Coordinate startPosition;
        startPosition = match.getStartPosition();
        if (startTime == null) {
            startTime = match.getTimeIntervalEnd();
        }
        vehicleBuilder.setStartLocation(Location.newInstance(startPosition.getLongitude(), startPosition.getLatitude()));
        vehicleBuilder.setType(vehicleType);
        vehicleBuilder.setEarliestStart(0);

        VehicleImpl vehicle = vehicleBuilder.setReturnToDepot(false).build();

        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();

        VehicleRoutingTransportCosts transportCosts = new TransportCosts();
        vrpBuilder.setRoutingCost(transportCosts);
        vrpBuilder.setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);
        vrpBuilder.addVehicle(vehicle);

        int i = 0;
        double intervalStart;
        double intervalEnd;
        LocalDateTime requestIntervalStart = null;
        LocalDateTime requestIntervalEnd = null;
        LocalDateTime referenceTime = getLastVehiclePositionTime(match.getVehicle(), startTime);

        if (!justGetSolution) {
            if (match.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
                requestIntervalStart = request.getArrivalInterval().getStart();
                requestIntervalEnd = request.getArrivalInterval().getEnd();
            } else {
                requestIntervalStart = request.getDepartureInterval().getStart();
                requestIntervalEnd = request.getDepartureInterval().getEnd();
            }
        }

        int priority;

        for (Coordinate coordinate : match.getDifferentStops().keySet()) {
            String stopInterval = match.getDifferentStops().get(coordinate);
            if (!justGetSolution) {
                if (coordinate.equals(request.getDropOffPosition())) {
                    if (!CommonFunctionHelper.isOverlapping(LocalDateTime.parse(stopInterval.split("-")[0], GeneralManager.dateTimeFormatter), LocalDateTime.parse(stopInterval.split("-")[1], GeneralManager.dateTimeFormatter), requestIntervalStart, requestIntervalEnd)) {
                        return false;
                    }
                    stopInterval = CommonFunctionHelper.getNewStopInterval(stopInterval, requestIntervalStart, requestIntervalEnd);
                }
            }

            if (match.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
                if (match.getDifferentStops().get(coordinate).isEmpty()) {
                    priority = 2;
                    intervalStart = 1000;
                    intervalEnd = 1000;
                } else {
                    priority = 10;
                    intervalStart = 1000 + ChronoUnit.MINUTES.between(referenceTime, LocalDateTime.parse(stopInterval.split("-")[0], GeneralManager.dateTimeFormatter));
                    intervalEnd = 1000 + ChronoUnit.MINUTES.between(referenceTime, LocalDateTime.parse(stopInterval.split("-")[1], GeneralManager.dateTimeFormatter));
                }
            } else {
                if (match.getDifferentStops().get(coordinate).isEmpty()) {
                    priority = 10;
                    intervalStart = 1000;
                    intervalEnd = 1000;
                } else {
                    priority = 2;
                    intervalStart = 1000 + ChronoUnit.MINUTES.between(referenceTime, LocalDateTime.parse(stopInterval.split("-")[0], GeneralManager.dateTimeFormatter));
                    intervalEnd = 1000 + ChronoUnit.MINUTES.between(referenceTime, LocalDateTime.parse(stopInterval.split("-")[1], GeneralManager.dateTimeFormatter));
                }
            }
            Service service;
            if (match.getDifferentStops().get(coordinate).isEmpty()) {
                service = Service.Builder.newInstance(String.valueOf(++i)).addSizeDimension(WEIGHT_INDEX, 1).setLocation(Location.newInstance(coordinate.getLongitude(), coordinate.getLatitude())).setServiceTime(stopTime).setPriority(priority).build();
            } else {
                service = Service.Builder.newInstance(String.valueOf(++i)).addSizeDimension(WEIGHT_INDEX, 1).setLocation(Location.newInstance(coordinate.getLongitude(), coordinate.getLatitude())).setServiceTime(stopTime).addTimeWindow(intervalStart, intervalEnd).setPriority(priority).build();
            }
            vrpBuilder.addJob(service);
        }

        if (!justGetSolution) {
            if (!match.getDifferentStops().containsKey(request.getDropOffPosition())) {
                if (match.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
                    priority = 10;
                    intervalStart = 1000 + ChronoUnit.MINUTES.between(referenceTime, requestIntervalStart);
                    intervalEnd = 1000 + ChronoUnit.MINUTES.between(referenceTime, requestIntervalEnd);
                } else {
                    priority = 2;
                    intervalStart = 1000 + ChronoUnit.MINUTES.between(referenceTime, requestIntervalStart);
                    intervalEnd = 1000 + ChronoUnit.MINUTES.between(referenceTime, requestIntervalEnd);
                }
                Service service = Service.Builder.newInstance(String.valueOf(++i)).addSizeDimension(WEIGHT_INDEX, 1).setLocation(Location.newInstance(request.getDropOffPosition().getLongitude(), request.getDropOffPosition().getLatitude())).setServiceTime(stopTime).addTimeWindow(intervalStart, intervalEnd).setPriority(priority).build();
                vrpBuilder.addJob(service);
            }

            if (!match.getDifferentStops().containsKey(request.getHomePosition())) {
                if (match.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
                    priority = 2;
                } else {
                    priority = 10;
                }
                Service service = Service.Builder.newInstance(String.valueOf(++i)).addSizeDimension(WEIGHT_INDEX, 1).setLocation(Location.newInstance(request.getHomePosition().getLongitude(), request.getHomePosition().getLatitude())).setServiceTime(stopTime).setPriority(priority).build();
                vrpBuilder.addJob(service);
            }
        }

        VehicleRoutingProblem problem = vrpBuilder.build();
        StateManager stateManager = new StateManager(problem);
        ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);
        constraintManager.addConstraint(new ActivityOrderConstraint(), ConstraintManager.Priority.CRITICAL);
        constraintManager.addConstraint(new ActivityWaitConstraintOneAllowed(), ConstraintManager.Priority.CRITICAL);

        VehicleRoutingAlgorithm algorithm = Jsprit.Builder.newInstance(problem).setStateAndConstraintManager(stateManager, constraintManager).buildAlgorithm();

        algorithm.setMaxIterations(50);
        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
        VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

        if (bestSolution != null) {
            VehicleRoute route = ((List<VehicleRoute>) bestSolution.getRoutes()).get(0);

            List<AgentWithConstraints> agentsOfMatch = new ArrayList<>(agents.stream().filter(a -> a instanceof AgentWithConstraints).map(a -> (AgentWithConstraints) a).toList());
            if (!justGetSolution) {
                agentsOfMatch.add((AgentWithConstraints) request.getAgent());
            }
            List<TourActivity> activities = route.getActivities();
            int counter = 0;
            if (!bestSolution.getUnassignedJobs().isEmpty()) {
                return false;
            }
            TourActivity activity = match.getTypeOfGrouping() == DirectionType.DRIVETOUNI ? route.getActivities().get(route.getActivities().size() - 1) : route.getActivities().get(0);
            String intervalStartTimeString = match.getDifferentStops().get(Coordinate.locationToCoordinate(activity.getLocation()));
            LocalDateTime intervalStartTime;
            if (intervalStartTimeString == null) {
                intervalStartTime = match.getTypeOfGrouping() == DirectionType.DRIVETOUNI ? request.getArrivalInterval().getStart() : request.getDepartureInterval().getStart();
            } else {
                if (!justGetSolution && Coordinate.coordinateTheSameAsLocation(request.getDropOffPosition(), activity.getLocation())) {
                    intervalStartTimeString = CommonFunctionHelper.getNewStopInterval(intervalStartTimeString, requestIntervalStart, requestIntervalEnd);
                }
                intervalStartTime = LocalDateTime.parse(intervalStartTimeString.split("-")[0], GeneralManager.dateTimeFormatter);
            }
            LocalDateTime newStartTime = getStartTime(intervalStartTime, match.getTypeOfGrouping(), bestSolution);
            LocalDateTime newEndTime = matchToEndTime.get(match) != null ? matchToEndTime.get(match) : match.getTimeIntervalStart();
            Match nextMatch = getNextMatchForVehicle(match.getVehicle(), newEndTime);
            newEndTime = getEndTime(bestSolution, newStartTime);
            if (newStartTime.isBefore(earliestDriveStart) || (nextMatch != null && !matchToStartTime.get(nextMatch).isAfter(newEndTime))) {
                LocalDateTime latestDriveEnd = nextMatch != null ? matchToStartTime.get(nextMatch).plusMinutes(1) : null;
                bestSolution = otherSolutionPossible(newStartTime, earliestDriveStart, vehicleBuilder, activities, problem, vrpBuilder, latestDriveEnd, newEndTime);
                if (bestSolution == null || !bestSolution.getUnassignedJobs().isEmpty()) {
                    return false;
                }
                route = ((List<VehicleRoute>) bestSolution.getRoutes()).get(0);
                activity = match.getTypeOfGrouping() == DirectionType.DRIVETOUNI ? route.getActivities().get(route.getActivities().size() - 1) : route.getActivities().get(0);
                intervalStartTimeString = match.getDifferentStops().get(Coordinate.locationToCoordinate(activity.getLocation()));
                if (intervalStartTimeString == null) {
                    intervalStartTime = match.getTypeOfGrouping() == DirectionType.DRIVETOUNI ? request.getArrivalInterval().getStart() : request.getDepartureInterval().getStart();
                } else {
                    if (!justGetSolution && Coordinate.coordinateTheSameAsLocation(request.getDropOffPosition(), activity.getLocation())) {
                        intervalStartTimeString = CommonFunctionHelper.getNewStopInterval(intervalStartTimeString, requestIntervalStart, requestIntervalEnd);
                    }
                    intervalStartTime = LocalDateTime.parse(intervalStartTimeString.split("-")[0], GeneralManager.dateTimeFormatter);
                }
                activities = ((List<VehicleRoute>) bestSolution.getRoutes()).get(0).getActivities();
                newStartTime = getStartTime(intervalStartTime, match.getTypeOfGrouping(), bestSolution);
                newEndTime = getEndTime(bestSolution, newStartTime);
            }
            if (!match.getDifferentStops().keySet().isEmpty() && bestSolution.getUnassignedJobs().isEmpty()) {
                for (TourActivity a : activities) {
                    if (a.getEndTime() - a.getArrTime() > stopTime) {
                        counter++;
                    }
                }
            }
            if (counter > 1) {
                double earliestStartTimeBefore = activity.getTheoreticalEarliestOperationStartTime();
                Location earlierStartLocation = activity.getLocation();
                bestSolution = RouteCalculationHelper.correctSolution(this, stopTime, match.getTypeOfGrouping(), bestSolution, vrpBuilder);
                if (bestSolution == null || !bestSolution.getUnassignedJobs().isEmpty()) {
                    return false;
                }
                route = ((List<VehicleRoute>) bestSolution.getRoutes()).get(0);
                activity = match.getTypeOfGrouping() == DirectionType.DRIVETOUNI ? route.getActivities().get(route.getActivities().size() - 1) : route.getActivities().get(0);
                intervalStartTimeString = match.getDifferentStops().get(Coordinate.locationToCoordinate(activity.getLocation()));
                if (intervalStartTimeString == null) {
                    intervalStartTime = match.getTypeOfGrouping() == DirectionType.DRIVETOUNI ? request.getArrivalInterval().getStart() : request.getDepartureInterval().getStart();
                } else {
                    if (!justGetSolution && Coordinate.coordinateTheSameAsLocation(request.getDropOffPosition(), activity.getLocation())) {
                        intervalStartTimeString = CommonFunctionHelper.getNewStopInterval(intervalStartTimeString, requestIntervalStart, requestIntervalEnd);
                    }
                    intervalStartTime = LocalDateTime.parse(intervalStartTimeString.split("-")[0], GeneralManager.dateTimeFormatter);
                }
                if (earlierStartLocation.equals(activity.getLocation()) && activity.getTheoreticalEarliestOperationStartTime() != earliestStartTimeBefore) {
                    intervalStartTime = activity.getTheoreticalEarliestOperationStartTime() > earliestStartTimeBefore ? intervalStartTime.plusMinutes((long) (activity.getTheoreticalEarliestOperationStartTime() - earliestStartTimeBefore)) : intervalStartTime.minusMinutes((long) (earliestStartTimeBefore - activity.getTheoreticalEarliestOperationStartTime()));
                }
                newStartTime = getStartTime(intervalStartTime, match.getTypeOfGrouping(), bestSolution);
                newEndTime = getEndTime(bestSolution, newStartTime);
            }

            if (bestSolution.getUnassignedJobs().isEmpty() && rideTimeIsAcceptedByAgents(bestSolution, agentsOfMatch, match.getTypeOfGrouping()) && !newStartTime.isBefore(earliestDriveStart) && newStartTime.isAfter(this.timeCounter) && !newEndTime.isAfter(LocalDateTime.parse("03.02.2023 02:00:00", GeneralManager.dateTimeFormatter)) && fitsTimeConstraints(route, match, newStartTime, newEndTime) && fitVehiclePositions(match, newEndTime, bestSolution) && !timeWindowsAreBroken(bestSolution)) {
                if ((matchToStartTime.containsKey(match) && !matchToStartTime.get(match).equals(newStartTime) && vehiclePositions.get(match.getVehicle()).containsKey(newStartTime)) || (matchToEndTime.containsKey(match) && !matchToEndTime.get(match).equals(newEndTime) && vehiclePositions.get(match.getVehicle()).containsKey(newEndTime)) || newStartTime.equals(newEndTime)) {
                    //System.out.println("overlap");
                    return false;
                }
                if (justGetSolution) {
                    this.matchToSolution.put(match, bestSolution);
                } else {
                    temporaryMatchToSolution.put(match, bestSolution);
                }
                List<Long> list = new ArrayList<>();
                list.add(match.getId());
                list.add(request == null ? null : request.getAgent().getId());
                if (counter > 1) {
                    matchToPossibleNewIntervalStartTime.put(list, intervalStartTime);
                } else {
                    matchToPossibleNewIntervalStartTime.remove(list);
                }
                return true;
            }
        }

        return false;
    }


    public VehicleRoutingProblemSolution otherSolutionPossible(LocalDateTime newStartTime, LocalDateTime earliestDriveStart, VehicleImpl.Builder vehicleBuilder, List<TourActivity> activities, VehicleRoutingProblem problem, VehicleRoutingProblem.Builder vrpBuilder, LocalDateTime latestDriveEnd, LocalDateTime newEndTime) {
        Collection<Job> jobs = new HashSet<>(vrpBuilder.getAddedJobs());

        vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
        vrpBuilder.addAllJobs(jobs);
        vrpBuilder.setRoutingCost(new TransportCosts());
        vrpBuilder.setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);

        long minutesBetweenEarliestAndEnd = ChronoUnit.MINUTES.between(earliestDriveStart, newEndTime);

        double earliestStart = Math.max(0, activities.get(activities.size() - 1).getEndTime() - minutesBetweenEarliestAndEnd);
        if (newStartTime.isBefore(earliestDriveStart)) {
            earliestStart += ChronoUnit.MINUTES.between(newStartTime, earliestDriveStart);
        }
        if (latestDriveEnd != null) {
            long minutesBetweenEarliestAndLatest = ChronoUnit.MINUTES.between(earliestDriveStart, latestDriveEnd);
            double latestEnd;
            if (latestDriveEnd.isBefore(newEndTime)) {
                latestEnd = activities.get(activities.size() - 1).getEndTime() - ChronoUnit.MINUTES.between(latestDriveEnd, newEndTime);
                earliestStart = Math.max(0, latestEnd - minutesBetweenEarliestAndLatest);
            } else {
                latestEnd = earliestStart + minutesBetweenEarliestAndLatest;
            }
            try {
                vehicleBuilder.setLatestArrival(latestEnd);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        vehicleBuilder.setEarliestStart(earliestStart);

        VehicleImpl vehicle = vehicleBuilder.setReturnToDepot(false).build();
        vrpBuilder.addVehicle(vehicle);

        problem = vrpBuilder.build();
        StateManager stateManager = new StateManager(problem);
        ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);
        constraintManager.addConstraint(new ActivityOrderConstraint(), ConstraintManager.Priority.CRITICAL);
        constraintManager.addConstraint(new ActivityWaitConstraintOneAllowed(), ConstraintManager.Priority.CRITICAL);
        VehicleRoutingAlgorithm algorithm = Jsprit.Builder.newInstance(problem).setStateAndConstraintManager(stateManager, constraintManager).buildAlgorithm();
        Jsprit.createAlgorithm(problem);
        algorithm.setMaxIterations(50);
        VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(algorithm.searchSolutions());
        if (!bestSolution.getUnassignedJobs().isEmpty()) {
            return null;
        }
        return bestSolution;
    }


    public boolean rideTimeIsAcceptedByAgents(VehicleRoutingProblemSolution solution, List<AgentWithConstraints> agents, DirectionType directionType) {
        Map<Agent, Double> agentToTime = new HashMap<>();
        for (AgentWithConstraints agent : agents) {
            agentToTime.put(agent, 0.0);
        }
        boolean firstNotDone = true;
        Location firstLocation = null;
        Location secondLocation;
        for (TourActivity activity : ((List<VehicleRoute>) solution.getRoutes()).get(0).getActivities()) {
            if (firstNotDone) {
                firstLocation = activity.getLocation();
                firstNotDone = false;
                continue;
            }
            secondLocation = activity.getLocation();
            List<Location> list = new ArrayList<>();
            list.add(firstLocation);
            list.add(secondLocation);
            double time = ModeExecutionManager.timeMap.get(list) + activity.getOperationTime();
            for (AgentWithConstraints agent : agents) {
                if (agentToTime.containsKey(agent)) {
                    agentToTime.put(agent, agentToTime.get(agent) + time);
                }
                if (Coordinate.coordinateTheSameAsLocation(agent.getRequest().getDropOffPosition(), secondLocation)) {
                    if (directionType == DirectionType.DRIVETOUNI) {
                        if (agentToTime.get(agent) - activity.getOperationTime() > agent.getWillingToRideInMinutes()) {
                            return false;
                        }
                        agentToTime.remove(agent);
                    } else {
                        agentToTime.put(agent, 0.0);
                    }
                } else if (Coordinate.coordinateTheSameAsLocation(agent.getRequest().getHomePosition(), secondLocation)) {
                    if (directionType == DirectionType.DRIVEHOME) {
                        if (agentToTime.get(agent) > agent.getWillingToRideInMinutes()) {
                            return false;
                        }
                        agentToTime.remove(agent);
                    } else {
                        agentToTime.put(agent, 0.0);
                    }
                }
            }
            firstLocation = secondLocation;
        }
        return true;
    }


    public boolean fitVehiclePositions(Match match, LocalDateTime end, VehicleRoutingProblemSolution solution) {
        VehicleRoute newRoute = ((List<VehicleRoute>) solution.getRoutes()).get(0);
        Coordinate newEndPosition = Coordinate.locationToCoordinate(newRoute.getActivities().get(newRoute.getActivities().size() - 1).getLocation());
        TransportCosts tc = new TransportCosts();
        Match nextMatch = getNextMatchForVehicle(match.getVehicle(), end);
        if (nextMatch != null && !(nextMatch.getStartPosition().equals(newEndPosition) && end.isAfter(matchToStartTime.get(nextMatch)))) {
            VehicleRoute route = ((List<VehicleRoute>) matchToSolution.get(nextMatch).getRoutes()).get(0);
            Coordinate oldStartPosition = nextMatch.getStartPosition();
            nextMatch.setStartPosition(newEndPosition);
            //if(tc.getTransportTime(Coordinate.coordinateToLocation(newEndPosition),route.getActivities().get(0).getLocation(),0,null,null) < ChronoUnit.MINUTES.between(end,matchToStartTime.get(nextMatch))){
            VehicleRoute oldRoute = null;
            if (matchToSolution.get(match) != null) {
                oldRoute = ((List<VehicleRoute>) matchToSolution.get(match).getRoutes()).get(0);
            }
            if (oldRoute != null && Coordinate.coordinateTheSameAsLocation(newEndPosition, oldRoute.getActivities().get(oldRoute.getActivities().size() - 1).getLocation()) && end.isBefore(matchToStartTime.get(nextMatch))) {
                return true;
            } else if (wouldDriveBy(null, nextMatch, true, end.plusMinutes(1))) {
                LocalDateTime startTime = matchToStartTime.get(nextMatch);
                vehiclePositions.get(match.getVehicle()).remove(startTime);
                LocalDateTime endTime = matchToEndTime.get(nextMatch);
                List<Match> timeMatches = timeToMatch.get(startTime);
                vehiclePositions.get(match.getVehicle()).remove(endTime);
                timeMatches.remove(nextMatch);
                if (timeMatches.isEmpty()) {
                    timeToMatch.remove(startTime);
                } else {
                    timeToMatch.put(startTime, timeMatches);
                }
                route = ((List<VehicleRoute>) matchToSolution.get(nextMatch).getRoutes()).get(0);
                nextMatch.setEndPosition(Coordinate.locationToCoordinate(route.getActivities().get(route.getActivities().size() - 1).getLocation()));
                TourActivity startActivity = route.getActivities().get(0);
                TourActivity endActivity = route.getActivities().get(route.getActivities().size() - 1);
                if (nextMatch.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
                    nextMatch.setTimeIntervalStart(LocalDateTime.parse(nextMatch.getDifferentStops().get(Coordinate.locationToCoordinate(endActivity.getLocation())).split("-")[0], GeneralManager.dateTimeFormatter));
                    nextMatch.setTimeIntervalEnd(LocalDateTime.parse(nextMatch.getDifferentStops().get(Coordinate.locationToCoordinate(endActivity.getLocation())).split("-")[1], GeneralManager.dateTimeFormatter));
                } else if (nextMatch.getTypeOfGrouping() == DirectionType.DRIVEHOME) {
                    nextMatch.setTimeIntervalStart(LocalDateTime.parse(nextMatch.getDifferentStops().get(Coordinate.locationToCoordinate(startActivity.getLocation())).split("-")[0], GeneralManager.dateTimeFormatter));
                    nextMatch.setTimeIntervalEnd(LocalDateTime.parse(nextMatch.getDifferentStops().get(Coordinate.locationToCoordinate(startActivity.getLocation())).split("-")[1], GeneralManager.dateTimeFormatter));
                }
                List<Long> list = new ArrayList<>();
                list.add(nextMatch.getId());
                list.add(null);
                if (matchToPossibleNewIntervalStartTime.containsKey(list)) {
                    nextMatch.setTimeIntervalStart(matchToPossibleNewIntervalStartTime.get(list));
                    matchToPossibleNewIntervalStartTime.remove(list);
                }
                LocalDateTime newStartTime = getStartTime(nextMatch.getTimeIntervalStart(), nextMatch.getTypeOfGrouping(), matchToSolution.get(nextMatch));
                LocalDateTime newEndTime = getEndTime(matchToSolution.get(nextMatch), newStartTime);
                matchToEndTime.put(nextMatch, newEndTime);
                matchToStartTime.put(nextMatch, newStartTime);
                timeMatches = new ArrayList<>();
                if (timeToMatch.containsKey(newStartTime)) {
                    timeMatches = timeToMatch.get(newStartTime);
                }
                timeMatches.add(nextMatch);
                timeToMatch.put(newStartTime, timeMatches);
                vehiclePositions.get(nextMatch.getVehicle()).put(newStartTime, nextMatch.getStartPosition());
                vehiclePositions.get(nextMatch.getVehicle()).put(newEndTime, nextMatch.getEndPosition());
                return true;
            } else {
                nextMatch.setStartPosition(oldStartPosition);
                return false;
            }
            //}else{
            //    nextMatch.setStartPosition(oldStartPosition);
            //    return false;
            //}
        } else {
            return true;
        }
    }


    public boolean fitsTimeConstraints(VehicleRoute route, Match match, LocalDateTime startTime, LocalDateTime endTime) {
        List<LocalDateTime> vehicleTimes = new ArrayList<>(vehiclePositions.get(match.getVehicle()).keySet());
        vehicleTimes.remove(matchToStartTime.get(match));
        vehicleTimes.remove(matchToEndTime.get(match));
        if (vehicleTimes.size() == 1 && !vehicleTimes.get(0).isAfter(startTime)) {
            return true;
        }
        if (vehicleTimes.contains(startTime) || vehicleTimes.contains(endTime)) {
            return false;
        }
        Collections.sort(vehicleTimes);
        List<LocalDateTime> vehicleReferences = new ArrayList<>(vehicleTimes);
        vehicleReferences.add(startTime);
        vehicleReferences.add(endTime);
        Collections.sort(vehicleReferences);

        int indexStart = vehicleReferences.indexOf(startTime);
        int indexEnd = vehicleReferences.indexOf(endTime);
        if (indexEnd - indexStart > 1) {
            return false;
        }
        int indexOfBefore = indexStart - 1;
        int indexOfAfter = indexEnd - 1;

        Match nextMatch = getNextMatchForVehicle(match.getVehicle(), endTime);
        Coordinate firstPosition = Coordinate.locationToCoordinate(route.getActivities().get(route.getActivities().size() - 1).getLocation());
        if (nextMatch != null && !firstPosition.equals(nextMatch.getStartPosition()) && !driveFitsTimewise(firstPosition, endTime, Coordinate.locationToCoordinate(route.getActivities().get(0).getLocation()), matchToStartTime.get(nextMatch), route)) {
            return false;
        }
        if (nextMatch != null && firstPosition.equals(nextMatch.getStartPosition()) && !endTime.isBefore(matchToStartTime.get(nextMatch))) {
            return false;
        }

        if (vehicleTimes.size() % 2 == 0 && indexOfBefore % 2 == 1 && indexOfAfter % 2 == 0 || vehicleTimes.size() % 2 == 1 && indexOfBefore % 2 == 0 && indexOfAfter % 2 == 1) {
            return true;
        } else {
            return false;
        }

        /*
        for(Match m : vehicleToMatches.get(match.getVehicle())){
            if(CommonFunctionHelper.isOverlapping(matchToStartTime.get(m),matchToEndTime.get(m),startTime,endTime)){
                return false;
            }
        }

        for(Ride r : vehicleToRides.get(match.getVehicle())){
            if(CommonFunctionHelper.isOverlapping(r.getStartTime(),r.getEndTime(),startTime,endTime)){
                return false;
            }
        }*/

    }


    public boolean driveFitsTimewise(Coordinate newMatchEnd, LocalDateTime newMatchEndTime, Coordinate firstStopOfNextMatch, LocalDateTime nextStartPositionStartTime, VehicleRoute route) {
        TransportCosts transportCosts = new TransportCosts();
        LocalDateTime arrivalAtFirstStopOfNextMatch = nextStartPositionStartTime.plusMinutes((long) route.getActivities().get(0).getArrTime());

        return transportCosts.getTransportTime(Coordinate.coordinateToLocation(newMatchEnd), Coordinate.coordinateToLocation(firstStopOfNextMatch), 0, null, null) <= ChronoUnit.MINUTES.between(newMatchEndTime, arrivalAtFirstStopOfNextMatch);
    }


    public Match getNextMatchForVehicle(Vehicle vehicle, LocalDateTime endTime) {
        List<LocalDateTime> vehicleTimes = new ArrayList<>(vehiclePositions.get(vehicle).keySet());
        Collections.sort(vehicleTimes);
        int index;
        if (vehicleTimes.contains(endTime)) {
            index = vehicleTimes.indexOf(endTime) + 1;
        } else {
            List<LocalDateTime> vehicleReferences = new ArrayList<>(vehicleTimes);
            vehicleReferences.add(endTime);
            Collections.sort(vehicleReferences);
            index = vehicleReferences.indexOf(endTime);
        }
        if (index >= vehicleTimes.size()) {
            return null;
        }
        if (!timeToMatch.containsKey(vehicleTimes.get(index))) {
            return null;
        }
        for (Match match : timeToMatch.get(vehicleTimes.get(index))) {
            if (match.getVehicle().equals(vehicle)) {
                return match;
            }
        }
        return null;

        /*Map<LocalDateTime,Coordinate> positions = this.vehiclePositions.get(vehicle);
        List<LocalDateTime> times = new ArrayList<>(positions.keySet());
        Collections.sort(times);
        Collections.reverse(times);
        LocalDateTime nextPositionTime = null;
        for(LocalDateTime time : times){
            if(time.isAfter(endTime)){
                nextPositionTime = time;
            }else{
                break;
            }
        }
        for(Match m : vehicleToMatches.get(vehicle)){
            if(matchToStartTime.get(m).equals(nextPositionTime)){
                return m;
            }
        }
        return null;*/
    }


    public LocalDateTime getLastVehiclePositionTime(Vehicle vehicle, LocalDateTime startTime) {
        List<LocalDateTime> vehicleTimes = new ArrayList<>(vehiclePositions.get(vehicle).keySet());
        Collections.sort(vehicleTimes);
        if (vehicleTimes.get(0).isAfter(startTime)) {
            return null;
        }
        if (vehicleTimes.size() == 1) {
            return vehicleTimes.get(0);
        }
        if (vehicleTimes.contains(startTime)) {
            if (startTime.isEqual(LocalDateTime.parse("02.02.2023 04:00:00", GeneralManager.dateTimeFormatter))) {
                return vehicleTimes.get(0);
            }
            return vehicleTimes.get(vehicleTimes.indexOf(startTime) - 1);
        }
        List<LocalDateTime> vehicleReferences = new ArrayList<>(vehicleTimes);
        vehicleReferences.add(startTime);
        Collections.sort(vehicleReferences);

        return vehicleTimes.get(vehicleReferences.indexOf(startTime) - 1);
    }



    public Ride matchToRide(Match match) {
        for (Agent a : match.getAgents()) {
            if (a.getId() == 9078) {
                int f = 2;
            }
        }
        VehicleRoutingProblemSolution solution = matchToSolution.get(match);
        Ride ride;
        VehicleRoute route = ((List<VehicleRoute>) solution.getRoutes()).get(0);
        Stopreason reasonForStopping = Stopreason.PARKING;
        ride = new Ride(match.getStartPosition(), match.getEndPosition(), matchToStartTime.get(match), matchToEndTime.get(match), match.getVehicle(), null, match.getTypeOfGrouping(), match.getAgents());
        ride.setGroupNumber(match.getGroupNumber());
        if (getNextMatchForVehicle(match.getVehicle(), ride.getEndTime()) == null) {
            getLastRideBackToDepotTime(ride);
        }

        List<Agent> stopAgents;
        List<Stop> stops = new ArrayList<>();
        double departureTime;
        LocalDateTime lastArrivalLocalTime = ride.getEndTime().minusMinutes(stopTime);
        TourActivity service;
        double lastArrivalTime = route.getActivities().get(route.getActivities().size() - 1).getArrTime();
        for (int j = route.getActivities().size() - 1; j >= 0; j--) {
            service = route.getActivities().get(j);
            departureTime = service.getEndTime();
            stopAgents = new ArrayList<>();
            for (Agent agent : match.getAgents()) {
                if (Coordinate.coordinateTheSameAsLocation(agent.getRequest().getDropOffPosition(), service.getLocation())) {
                    stopAgents.add(agent);
                    reasonForStopping = match.getTypeOfGrouping() == DirectionType.DRIVETOUNI ? Stopreason.DROPOFF : Stopreason.PICKUP;
                } else if (Coordinate.coordinateTheSameAsLocation(agent.getRequest().getHomePosition(), service.getLocation())) {
                    stopAgents.add(agent);
                    reasonForStopping = match.getTypeOfGrouping() == DirectionType.DRIVETOUNI ? Stopreason.PICKUP : Stopreason.DROPOFF;
                }
            }
            Stop newStop;
            if (j == route.getActivities().size() - 1) {
                newStop = new Stop(lastArrivalLocalTime, lastArrivalLocalTime.plusMinutes(stopTime), Coordinate.locationToCoordinate(service.getLocation()), reasonForStopping, stopAgents);
            } else {
                newStop = new Stop(lastArrivalLocalTime.minusMinutes((long) (lastArrivalTime - departureTime)).minusMinutes(stopTime), lastArrivalLocalTime.minusMinutes((long) (lastArrivalTime - departureTime)), Coordinate.locationToCoordinate(service.getLocation()), reasonForStopping, stopAgents);
                lastArrivalLocalTime = lastArrivalLocalTime.minusMinutes((long) (lastArrivalTime - departureTime)).minusMinutes(stopTime);
                lastArrivalTime = service.getArrTime();
            }
            stops.add(newStop);
        }
        stops.sort(Comparator.comparing(Stop::getStartTime));
        ride.setExtraStops(stops);
        activeMatches.remove(match);
        finishedMatches.add(match);
        this.rides.add(ride);
        return ride;
    }


    public List<Coordinate> bestInsertionPlaceForHomePositionIsBefore(Match match, Request request, VehicleRoutingProblemSolution solution) {
        VehicleRoute route = ((List<VehicleRoute>) solution.getRoutes()).get(0);
        org.locationtech.jts.geom.Coordinate homeCoordinate = new CoordinateXY(request.getHomePosition().getLongitude(), request.getHomePosition().getLatitude());
        org.locationtech.jts.geom.Coordinate dropCoordinate = new CoordinateXY(request.getDropOffPosition().getLongitude(), request.getDropOffPosition().getLatitude());
        double distance = 0;
        try {
            distance = JTS.orthodromicDistance(homeCoordinate, dropCoordinate, DefaultGeographicCRS.WGS84);
        } catch (Exception e) {
            e.printStackTrace();
        }
        double finalDistance = distance;
        List<Coordinate> coordinates = new ArrayList<>(match.getDifferentStops().keySet());
        coordinates.removeIf(c -> !match.getDifferentStops().get(c).isEmpty());
        coordinates.sort(Comparator.comparing(c -> {
            org.locationtech.jts.geom.Coordinate neighbourCoordinate = new CoordinateXY(c.getLongitude(), c.getLatitude());
            try {
                return Math.abs(finalDistance - JTS.orthodromicDistance(neighbourCoordinate, dropCoordinate, DefaultGeographicCRS.WGS84));
            } catch (Exception e) {
                e.printStackTrace();
                return Double.MAX_VALUE;
            }
        }));

        Coordinate nearestNeighbour = coordinates.get(0);
        Coordinate neighbourBefore = null;
        Coordinate neighbourAfter = null;
        org.locationtech.jts.geom.Coordinate neighbourCoordinate = new CoordinateXY(nearestNeighbour.getLongitude(), nearestNeighbour.getLatitude());
        try {
            double neighbourDistance = JTS.orthodromicDistance(neighbourCoordinate, dropCoordinate, DefaultGeographicCRS.WGS84);
            if (neighbourDistance < distance) {
                neighbourAfter = nearestNeighbour;
                if (!Coordinate.coordinateToLocation(neighbourAfter).equals(route.getActivities().get(0).getLocation())) {
                    int counter = 0;
                    for (TourActivity activity : route.getActivities()) {
                        if (Coordinate.coordinateToLocation(neighbourAfter).equals(activity.getLocation())) {
                            neighbourBefore = Coordinate.locationToCoordinate(route.getActivities().get(counter - 1).getLocation());
                        }
                        counter++;
                    }
                }
            } else {
                neighbourBefore = nearestNeighbour;
                int counter = 0;
                for (TourActivity activity : route.getActivities()) {
                    if (Coordinate.coordinateToLocation(neighbourBefore).equals(activity.getLocation())) {
                        neighbourAfter = Coordinate.locationToCoordinate(route.getActivities().get(counter + 1).getLocation());
                    }
                    counter++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<Coordinate> result = new ArrayList<>();
        result.add(neighbourBefore);
        result.add(neighbourAfter);
        return result;

    }


    public List<Double> getPufferTime(VehicleRoutingProblemSolution solution, Match match) {
        VehicleRoute route = ((List<VehicleRoute>) solution.getRoutes()).get(0);
        double pufferBack = Double.MAX_VALUE;
        double pufferFurther = Double.MAX_VALUE;
        for (TourActivity activity : route.getActivities()) {
            try {
                if (!match.getDifferentStops().get(Coordinate.locationToCoordinate(activity.getLocation())).isEmpty()) {
                    double newPuffer = activity.getTheoreticalLatestOperationStartTime() - (activity.getEndTime() - stopTime);
                    if (newPuffer < pufferFurther) {
                        pufferFurther = newPuffer;
                    }
                    newPuffer = (activity.getEndTime() - stopTime) - activity.getTheoreticalEarliestOperationStartTime();
                    if (newPuffer < pufferBack) {
                        pufferBack = newPuffer;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        List<Double> puffers = new ArrayList<>();
        puffers.add(pufferBack);
        puffers.add(pufferFurther);
        return puffers;
    }


    public double getExtraCostForRequest(Coordinate beforeCoordinate, Coordinate afterCoordinate, Coordinate newCoordinate, Agent beforeNeighbourWithHomePosition, Agent afterNeighbourWithHomePosition, Agent newAgent) {
        TransportCosts tc = new TransportCosts();
        String indicesString = beforeNeighbourWithHomePosition.getId() + "." + newAgent.getId();
        double indices1 = Double.parseDouble(indicesString);
        indicesString = newAgent.getId() + "." + afterNeighbourWithHomePosition.getId();
        double indices2 = Double.parseDouble(indicesString);
        return tc.getTransportTime(Coordinate.coordinateToLocation(beforeCoordinate), Coordinate.coordinateToLocation(newCoordinate), indices1, null, null) + stopTime + tc.getTransportTime(Coordinate.coordinateToLocation(newCoordinate), Coordinate.coordinateToLocation(afterCoordinate), indices2, null, null);
    }


    public void calculateMetrics(Ride ride) {
        Coordinate firstCoordinate = ride.getStartPosition();
        Coordinate secondCoordinate;
        Map<Agent, Double> agentToDistance = new HashMap<>();
        Map<Agent, Double> agentToTime = new HashMap<>();

        double vehicleDistance = 0.0;
        double vehicleTime = 0.0;
        for (Agent agent : ride.getAgents()) {
            agentToDistance.put(agent, 0.0);
            agentToTime.put(agent, 0.0);
        }
        int i = 0;
        for (Stop stop : ride.getExtraStops()) {
            secondCoordinate = stop.getStopCoordinate();
            List<Location> list = new ArrayList<>();
            list.add(Coordinate.coordinateToLocation(firstCoordinate));
            list.add(Coordinate.coordinateToLocation(secondCoordinate));
            if (ModeExecutionManager.distanceMap.get(list) == null) {
                new TransportCosts().getTransportCost(Coordinate.coordinateToLocation(firstCoordinate), Coordinate.coordinateToLocation(secondCoordinate), 0, null, null);
            }
            double distance = ModeExecutionManager.distanceMap.get(list);
            double time = ModeExecutionManager.timeMap.get(list) + stopTime;
            for (Agent agent : ride.getAgents()) {
                if (agentToTime.containsKey(agent)) {
                    agentToDistance.put(agent, agentToDistance.get(agent) + distance);
                    agentToTime.put(agent, agentToTime.get(agent) + time);
                }
                if (stop.getPersonsInQuestion().contains(agent)) {
                    if (stop.getReasonForStopping() == Stopreason.DROPOFF) {
                        agentToTime.put(agent, agentToTime.get(agent) - stopTime);
                        setMetrics(agent, ride, agentToDistance, agentToTime);
                        agentToTime.remove(agent);
                    } else if (stop.getReasonForStopping() == Stopreason.PICKUP) {
                        agentToDistance.put(agent, 0.0);
                        agentToTime.put(agent, 0.0);
                    }
                }
            }
            if (i == 0 && ride.getDriver() == null) {
                vehicleEmptyDistances.put(ride.getVehicle(), vehicleEmptyDistances.get(ride.getVehicle()) + distance);
                emptyDistances.put(ride, distance);
            }
            vehicleDistance += distance;
            vehicleTime += time;
            firstCoordinate = secondCoordinate;
            i++;
        }
        double distanceSum = 0;
        Map<Agent, Double> agentToLuftDistance = new HashMap<>();
        for (Agent agent : ride.getAgents()) {
            org.locationtech.jts.geom.Coordinate homeCoordinate = new CoordinateXY(agent.getHomePosition().getLongitude(), agent.getHomePosition().getLatitude());
            org.locationtech.jts.geom.Coordinate dropoffCoordinate = new CoordinateXY(agent.getRequest().getDropOffPosition().getLongitude(), agent.getRequest().getDropOffPosition().getLatitude());
            try {
                double luftDistance = JTS.orthodromicDistance(homeCoordinate, dropoffCoordinate, DefaultGeographicCRS.WGS84);

                distanceSum += luftDistance;
                agentToLuftDistance.put(agent, luftDistance);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (Agent agent : ride.getAgents()) {
            setAgentCostAndCo2Metrics(ride, agent, agentToLuftDistance.get(agent) / distanceSum, vehicleDistance / 1000);
        }
        setVehicleMetrics(ride.getVehicle(), ride, vehicleDistance, vehicleTime);
    }


    public void setMetrics(Agent agent, Ride ride, Map<Agent, Double> agentToDistance, Map<Agent, Double> agentToTime) {

        Set<Object> set = new HashSet<>();
        set.add(agent);
        set.add(ride);

        oneWayKmTravelled.put(set, agentToDistance.get(agent) / 1000);
        oneWayMinutesTravelled.put(set, agentToTime.get(agent));
        List<Ride> rideList = new ArrayList<>();
        if (agentToRides.containsKey(agent)) {
            rideList = agentToRides.get(agent);
        }
        rideList.add(ride);
        agentToRides.put(agent, rideList);

        if (minutesTravelled.containsKey(agent)) {
            minutesTravelled.put(agent, minutesTravelled.get(agent) + agentToTime.get(agent));
            kmTravelled.put(agent, kmTravelled.get(agent) + agentToDistance.get(agent) / 1000);
        } else {
            minutesTravelled.put(agent, agentToTime.get(agent));
            kmTravelled.put(agent, agentToDistance.get(agent) / 1000);
        }
    }


    public void setVehicleMetrics(Vehicle vehicle, Ride ride, double vehicleDistance, double vehicleTime) {
        Set<Object> set = new HashSet<>();
        set.add(vehicle);
        set.add(ride);

        double vehicleCost = (vehicleDistance / 1000) * vehicle.getConsumptionPerKm() * vehicle.getPricePerLiter();
        double vehicleEmission = (vehicleDistance / 1000) * vehicle.getConsumptionPerKm() * vehicle.getCo2EmissionPerLiter();

        oneWayCosts.put(set, vehicleCost);
        oneWayEmissions.put(set, vehicleEmission);
        oneWayKmTravelled.put(set, vehicleDistance / 1000);
        oneWayMinutesTravelled.put(set, vehicleTime);

        List<Ride> vehicleRides = new ArrayList<>();
        if (vehicleToRides.containsKey(vehicle)) {
            vehicleRides = vehicleToRides.get(vehicle);
        }
        vehicleRides.add(ride);
        vehicleToRides.put(vehicle, vehicleRides);

        if (vehicleMinutesTravelled.containsKey(vehicle)) {
            vehicleMinutesTravelled.put(vehicle, vehicleMinutesTravelled.get(vehicle) + vehicleTime);
            vehicleKmTravelled.put(vehicle, vehicleKmTravelled.get(vehicle) + (vehicleDistance / 1000));
            vehicleCosts.put(vehicle, vehicleCosts.get(vehicle) + vehicleCost);
            vehicleEmissions.put(vehicle, vehicleEmissions.get(vehicle) + vehicleEmission);
        } else {
            vehicleMinutesTravelled.put(vehicle, vehicleTime);
            vehicleKmTravelled.put(vehicle, (vehicleDistance / 1000));
            vehicleCosts.put(vehicle, vehicleCost);
            vehicleEmissions.put(vehicle, vehicleEmission);
        }
        if (!checkConstraints(ride)) {
            int f = 2;
        }
    }


    public void setAgentCostAndCo2Metrics(Ride ride, Agent agent, double percent, double vehicleDistance) {
        Set<Object> set = new HashSet<>();
        set.add(agent);
        set.add(ride);
        oneWayCosts.put(set, ((percent * vehicleDistance) * ride.getVehicle().getConsumptionPerKm() * ride.getVehicle().getPricePerLiter()));
        oneWayEmissions.put(set, ((percent * vehicleDistance) * ride.getVehicle().getConsumptionPerKm() * ride.getVehicle().getCo2EmissionPerLiter()));

        if (emissions.containsKey(agent)) {
            emissions.put(agent, emissions.get(agent) + ((percent * vehicleDistance) * ride.getVehicle().getConsumptionPerKm() * ride.getVehicle().getCo2EmissionPerLiter()));
            costs.put(agent, costs.get(agent) + (((percent * vehicleDistance) * ride.getVehicle().getConsumptionPerKm() * ride.getVehicle().getPricePerLiter())));
        } else {
            emissions.put(agent, ((percent * vehicleDistance) * ride.getVehicle().getConsumptionPerKm() * ride.getVehicle().getCo2EmissionPerLiter()));
            costs.put(agent, ((percent * vehicleDistance) * ride.getVehicle().getConsumptionPerKm() * ride.getVehicle().getPricePerLiter()));
        }
    }


    public boolean checkConstraints(Ride newRide) {
        if (newRide.getStartTime().isBefore(LocalDateTime.parse("02.02.2023 04:00:00", GeneralManager.dateTimeFormatter)) || newRide.getEndTime().isAfter(LocalDateTime.parse("03.02.2023 02:00:00", GeneralManager.dateTimeFormatter))) {
            return false;
        }
        if (newRide.getAgents().size() == 1 && newRide.getVehicle().equals(newRide.getAgents().get(0).getCar())) {
            LocalDateTime rideReferenceTime;
            LocalDateTime requestIntervalStart;
            LocalDateTime requestIntervalEnd;
            if (newRide.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
                rideReferenceTime = newRide.getEndTime();
                requestIntervalStart = newRide.getAgents().get(0).getRequest().getArrivalInterval().getStart();
                requestIntervalEnd = newRide.getAgents().get(0).getRequest().getArrivalInterval().getEnd();
            } else {
                rideReferenceTime = newRide.getStartTime();
                requestIntervalStart = newRide.getAgents().get(0).getRequest().getDepartureInterval().getStart();
                requestIntervalEnd = newRide.getAgents().get(0).getRequest().getDepartureInterval().getEnd();
            }
            return CommonFunctionHelper.isOverlapping(rideReferenceTime, rideReferenceTime, requestIntervalStart, requestIntervalEnd);
        } else {
            for (AgentWithConstraints agent : newRide.getAgents().stream().filter(a -> a instanceof AgentWithConstraints).map(a -> (AgentWithConstraints) a).toList()) {
                Set<Object> set = new HashSet<>();
                set.add(newRide);
                set.add(agent);
                if (oneWayMinutesTravelled.get(set) > agent.getWillingToRideInMinutes()) {
                    return false;
                }

                Stop dropOffStop = null;
                Stop pickUpStop = null;
                List<Stop> stops = CommonFunctionHelper.getAgentStops(newRide, agent);
                for (Stop st : stops) {
                    if (st.getReasonForStopping() == Stopreason.DROPOFF) {
                        dropOffStop = st;
                    } else if (st.getReasonForStopping() == Stopreason.PICKUP) {
                        pickUpStop = st;
                    }
                }
                if (newRide.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
                    if (dropOffStop != null && !CommonFunctionHelper.isOverlapping(dropOffStop.getStartTime(), dropOffStop.getStartTime(), agent.getRequest().getArrivalInterval().getStart(), agent.getRequest().getArrivalInterval().getEnd())) {
                        return false;
                    }
                } else {
                    if (pickUpStop != null && !CommonFunctionHelper.isOverlapping(pickUpStop.getStartTime(), pickUpStop.getStartTime(), agent.getRequest().getDepartureInterval().getStart(), agent.getRequest().getDepartureInterval().getEnd())) {
                        return false;
                    }
                }
            }

            List<Ride> vehicleRides = vehicleToRides.get(newRide.getVehicle());
            for (Ride otherRide : vehicleRides) {
                if (!otherRide.equals(newRide)) {
                    if (CommonFunctionHelper.isOverlapping(otherRide.getStartTime(), otherRide.getEndTime(), newRide.getStartTime(), newRide.getEndTime())) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public void getLastRideBackToDepotTime(Ride ride) {
        List<Coordinate> deps = new ArrayList<>(depots);
        deps.sort(Comparator.comparing(d -> {
            org.locationtech.jts.geom.Coordinate firstCoordinate = new CoordinateXY(ride.getEndPosition().getLongitude(), ride.getEndPosition().getLatitude());
            org.locationtech.jts.geom.Coordinate depotCoordinate = new CoordinateXY(d.getLongitude(), d.getLatitude());
            try {
                return JTS.orthodromicDistance(firstCoordinate, depotCoordinate, DefaultGeographicCRS.WGS84);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            }
        }));
        Coordinate dep = null;
        if (vehicleToDepot.containsKey(ride.getVehicle())) {
            if (vehicleToDepot.get(ride.getVehicle()).equals(deps.get(0))) {
                dep = deps.get(0);
            } else {
                depotCapacity.put(vehicleToDepot.get(ride.getVehicle()), depotCapacity.get(vehicleToDepot.get(ride.getVehicle())) + 1);
            }
        }
        if (dep == null) {
            for (Coordinate d : deps) {
                if (depotCapacity.get(d) > 0) {
                    dep = d;
                    vehicleToDepot.put(ride.getVehicle(), d);
                    depotCapacity.put(d, depotCapacity.get(d) - 1);
                    break;
                }
            }
        }
        if (dep == null) {
            throw new RuntimeException("no more free depot!");
        }

        rideTimeBackToDepot.put(ride.getVehicle(), (long) new TransportCosts().getTransportTime(Coordinate.coordinateToLocation(ride.getEndPosition()), Coordinate.coordinateToLocation(dep), 0, null, null));
        lastVehicleRide.put(ride.getVehicle(), ride);
    }


    public boolean isMatchDoable(Match match, Match firstMatch, DirectionType directionType, Request request) {
        LocalDateTime starttime = null;
        LocalDateTime endtime = null;
        LocalDateTime oldstarttime = null;
        LocalDateTime oldendtime = null;
        Coordinate oldstartposition = null;
        Coordinate oldendposition = null;

        if (vehiclePositions.get(match.getVehicle()).keySet().size() % 2 == 0) {
            int f = 2;
        }

        boolean result = false;
        if (directionType == DirectionType.DRIVEHOME && firstMatch.getVehicle().equals(match.getVehicle())) {
            Map<LocalDateTime, Coordinate> positions = vehiclePositions.get(match.getVehicle());
            VehicleRoutingProblemSolution solution = temporaryMatchToSolution.get(firstMatch);
            VehicleRoute route = ((List<VehicleRoute>) solution.getRoutes()).get(0);
            String stopIntervalForRequest = firstMatch.getDifferentStops().containsKey(request.getDropOffPosition()) ? CommonFunctionHelper.getNewStopInterval(firstMatch.getDifferentStops().get(request.getDropOffPosition()), request.getArrivalInterval().getStart(), request.getArrivalInterval().getEnd()) : CommonFunctionHelper.getIntervalString(request.getArrivalInterval().getStart(), request.getArrivalInterval().getEnd());
            LocalDateTime timeIntervalStart;
            if (Coordinate.coordinateTheSameAsLocation(request.getDropOffPosition(), route.getEnd().getLocation())) {
                timeIntervalStart = LocalDateTime.parse(stopIntervalForRequest.split("-")[0], GeneralManager.dateTimeFormatter);
            } else {
                timeIntervalStart = LocalDateTime.parse(firstMatch.getDifferentStops().get(Coordinate.locationToCoordinate(route.getEnd().getLocation())).split("-")[0], GeneralManager.dateTimeFormatter);
            }
            List<Long> list = new ArrayList<>();
            list.add(firstMatch.getId());
            list.add(request.getAgent().getId());
            if (matchToPossibleNewIntervalStartTime.containsKey(list)) {
                timeIntervalStart = matchToPossibleNewIntervalStartTime.get(list);
            }
            oldstarttime = matchToStartTime.get(firstMatch);
            oldendtime = matchToEndTime.get(firstMatch);
            if (oldstarttime != null) {
                oldstartposition = positions.remove(oldstarttime);
                oldendposition = positions.remove(oldendtime);
            }
            starttime = getStartTime(timeIntervalStart, firstMatch.getTypeOfGrouping(), solution);
            positions.put(starttime, Coordinate.locationToCoordinate(route.getStart().getLocation()));
            endtime = getEndTime(solution, starttime);
            positions.put(endtime, Coordinate.locationToCoordinate(route.getEnd().getLocation()));
            vehiclePositions.put(match.getVehicle(), positions);
        }//(requesttype==Requesttype.DRIVEHOME || requestMightBeInsertable(match,matchToSolution.get(match),request)) &&
        if (wouldDriveBy(request, match, false, LocalDateTime.parse("02.02.2023 04:00:00", GeneralManager.dateTimeFormatter))) {
            result = true;
        }
        if (directionType == DirectionType.DRIVEHOME && firstMatch.getVehicle().equals(match.getVehicle())) {
            Map<LocalDateTime, Coordinate> positions = vehiclePositions.get(match.getVehicle());
            positions.remove(starttime);
            positions.remove(endtime);
            if (oldstarttime != null) {
                positions.put(oldstarttime, oldstartposition);
                positions.put(oldendtime, oldendposition);
            }
            vehiclePositions.put(match.getVehicle(), positions);
        }
        if (vehiclePositions.get(match.getVehicle()).keySet().size() % 2 == 0) {
            throw new RuntimeException("");
        }
        return result;
    }


    public boolean timeWindowsAreBroken(VehicleRoutingProblemSolution solution) {
        VehicleRoute route = ((List<VehicleRoute>) solution.getRoutes()).get(0);
        int counter = 0;
        for (TourActivity activity : route.getActivities()) {
            if (activity.getEndTime() - activity.getTheoreticalLatestOperationStartTime() > stopTime) {
                return true;
            } else if (activity.getArrTime() < activity.getTheoreticalEarliestOperationStartTime()) {
                counter++;
            }
        }
        return counter > 1;
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
        return null;
    }

    @Override
    public double getFinishedTotalCosts() {
        return 0;
    }

    @Override
    public double getFinishedTotalEmissions() {
        return 0;
    }

    @Override
    public double getFinishedTotalKmTravelled() {
        return 0;
    }

    @Override
    public double getFinishedTotalMinutesTravelled() {
        return 0;
    }

    @Override
    public Map<Ride, Double> getFinishedRideCosts() {
        Map<Ride, Double> map = new HashMap<>();
        for(Ride ride : this.rides){
            map.put(ride,0.0);
        }
        return map;
    }

    @Override
    public Map<Ride, Double> getFinishedRideEmissions() {
        Map<Ride, Double> map = new HashMap<>();
        for(Ride ride : this.rides){
            map.put(ride,0.0);
        }
        return map;
    }

    @Override
    public Map<Ride, Double> getFinishedRideKmTravelled() {
        Map<Ride, Double> map = new HashMap<>();
        for(Ride ride : this.rides){
            map.put(ride,0.0);
        }
        return map;
    }

    @Override
    public Map<Ride, Double> getFinishedRideMinutesTravelled() {
        Map<Ride, Double> map = new HashMap<>();
        for(Ride ride : this.rides){
            map.put(ride,0.0);
        }
        return map;
    }

}