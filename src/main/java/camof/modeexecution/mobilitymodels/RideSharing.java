package camof.modeexecution.mobilitymodels;

import camof.AgentWithConstraints;
import camof.modeexecution.mobilitymodels.modehelpers.RouteCalculationHelper;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.ResponsePath;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.util.shapes.GHPoint;
import camof.GeneralManager;
import camof.modeexecution.*;
import camof.modeexecution.carmodels.StandInVehicle;
import camof.modeexecution.groupings.Match;
import camof.modeexecution.groupings.Ride;
import camof.modeexecution.groupings.Stop;
import camof.modeexecution.groupings.Stopreason;
import camof.modeexecution.mobilitymodels.modehelpers.CommonFunctionHelper;
import camof.modeexecution.mobilitymodels.tsphelpers.ActivityOrderConstraint;
import camof.modeexecution.mobilitymodels.tsphelpers.ActivityWaitConstraintOneAllowed;
import camof.modeexecution.mobilitymodels.tsphelpers.TransportCosts;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class RideSharing extends MobilityMode {

    public static final String name = "ridesharing";
    double co2EmissionPerLiter;
    double pricePerLiter;
    double consumptionPerKm;
    int seatCount;
    long timeInterval;
    double acceptedWalkingDistance;
    String acceptedRideTimeFunction;
    List<Event> sortedEvents;
    int requestSum;
    List<Match> activeMatches;
    List<Match> finishedMatches;
    Map<AgentWithConstraints, Request> lost;
    boolean handleLost;
    long stopTime;
    List<Request> pendingHomeRequests;
    Map<String, Long> secondsBetweenDropOffs;
    Map<Match, LocalDateTime> matchToStartTime;
    Map<Match, VehicleRoutingProblemSolution> matchToSolution;
    Map<Match, VehicleRoutingProblemSolution> temporaryMatchToSolution;
    List<AgentWithConstraints> agents;


    public RideSharing(){
        super();
        comparing = true;
        lost = new HashMap<>();
        matchToStartTime = new HashMap<>();
        matchToSolution = new HashMap<>();
        temporaryMatchToSolution = new HashMap<>();
        activeMatches = new ArrayList<>();
        finishedMatches = new ArrayList<>();
        pendingHomeRequests = new ArrayList<>();
        secondsBetweenDropOffs = new HashMap<>();
        sortedEvents = new ArrayList<>();
        this.compareMode = ModeExecutionManager.finishedModes.get(ModeExecutionManager.compareModes.get(this.getName()));
    }


    public String getName(){
        return "ridesharing";
    }


    public void prepareMode(List<Agent> agents) {

        seatCount = (int) ModeExecutionManager.configValues.get("student car seat count");
        co2EmissionPerLiter = (double) ModeExecutionManager.configValues.get("studentCarCo2EmissionPerLiter");
        pricePerLiter = (double) ModeExecutionManager.configValues.get("studentCarPricePerLiter");
        consumptionPerKm = (double) ModeExecutionManager.configValues.get("studentCarConsumptionPerKm");
        timeInterval = (long) ModeExecutionManager.configValues.get("time interval");
        stopTime = (long) ModeExecutionManager.configValues.get("stop time");
        acceptedWalkingDistance = (double) ModeExecutionManager.configValues.get("accepted walking distance");
        acceptedRideTimeFunction = (String) ModeExecutionManager.configValues.get("accepted ridesharing time");
        handleLost = (boolean) ModeExecutionManager.configValues.get("handleLost");

        this.agents = new ArrayList<>();
        for(Agent agent : agents){
            AgentWithConstraints agentWithConstraints = new AgentWithConstraints(agent);
            agentWithConstraints.setTimeIntervalInMinutes((Long) ModeExecutionManager.configValues.get("time interval"));
            agentWithConstraints.setWillingToWalkInMeters((Double) ModeExecutionManager.configValues.get("accepted walking distance"));
            Request agentRequest = agentWithConstraints.getRequest();
            agentRequest.setAgent(agentWithConstraints);
            agentRequest.setArrivalInterval(new TimeInterval(agentRequest.getFavoredArrivalTime().minusMinutes(timeInterval),agentRequest.getFavoredArrivalTime().plusMinutes(timeInterval)));
            agentRequest.setDepartureInterval(new TimeInterval(agentRequest.getFavoredDepartureTime(),agentRequest.getFavoredDepartureTime().plusMinutes(2*timeInterval)));
            this.agents.add(agentWithConstraints); //todo
        }
        List<Agent> willingAgents = new ArrayList<>(this.agents);
        CommonFunctionHelper.filterWilling(ModeExecutionManager.percentOfWillingStudents,willingAgents,unwillingAgents);
        for(Agent agent : this.unwillingAgents){
            CommonFunctionHelper.letAgentDriveNormally(agent,sortedEvents,matchToStartTime);
        }

        for (Request request : splitRequestsInTwo(willingAgents)) {
            this.sortedEvents.add(new Event("requestArrival",request.getRequestTime(),request));
        }
        Collections.sort(this.sortedEvents);

        CommonFunctionHelper.calculateSecondsBetweenDropOffs(this.secondsBetweenDropOffs,ModeExecutionManager.postcodeToCoordinate);
        CommonFunctionHelper.calculateAcceptedDrivingTimes(this.agents, this.compareMode, "x + log1.4(x)"); //TODO
    }


    public List<Request> splitRequestsInTwo(List<Agent> agents) {
        List<Request> splitRequests = new ArrayList<>();
        for (Agent agent : agents) {
            Request sourceRequest = agent.getRequest();
            Request driveToUniRequest = new Request(sourceRequest);
            driveToUniRequest.setRequesttype(DirectionType.DRIVETOUNI);

            Request driveHomeRequest = new Request(sourceRequest);
            driveHomeRequest.setRequesttype(DirectionType.DRIVEHOME);
            int low = 30;
            int high = 121;
            int result = GeneralManager.random.nextInt(high - low) + low;
            driveHomeRequest.setRequestTime(driveHomeRequest.getFavoredDepartureTime().minusMinutes(result));

            splitRequests.add(driveToUniRequest);
            splitRequests.add(driveHomeRequest);
        }
        requestSum = splitRequests.size();
        return splitRequests;
    }


    public void startMode() {

        if(this.sortedEvents.isEmpty()){
            throw new RuntimeException("Run 'prepareSimulation' first.");
        }

        String progressBar;
        int requestSum = this.sortedEvents.size();
        int requestCounter = 0;
        int countOf5percentSteps=0;
        int countOf5percentStepsForRequests=0;
        String lastProgress = "";

        while(!this.sortedEvents.isEmpty()){
            Event event = this.sortedEvents.get(0);
            if(event.getType().equals("requestArrival")){
                requestCounter++;
                Request request = (Request) event.getEventObject();
                findMatch(request);
                countOf5percentStepsForRequests = (int) Math.floor(((double) requestCounter / requestSum) * 20);
            }else if(event.getType().equals("rideStart")) {
                Match match = (Match) event.getEventObject();
                calculateMetrics(matchToRide(match));
            }
            this.sortedEvents.remove(0);

            countOf5percentSteps = (int) Math.floor(((requestSum-(sortedEvents.size()-1))/(double)requestSum) * 20);
            progressBar = "Request progress: |" + "=".repeat(countOf5percentStepsForRequests) + " ".repeat(20 - countOf5percentStepsForRequests) + "|\t\tTotal progress: |" + "=".repeat(countOf5percentSteps) + " ".repeat(20 - countOf5percentSteps) + "|\r";
            if (!progressBar.equals(lastProgress)) {
                System.out.print(progressBar);
                lastProgress = progressBar;
            }
        }

        for (Request pendingRequest : this.pendingHomeRequests) {
            lost.put((AgentWithConstraints) pendingRequest.getAgent(), pendingRequest);
            handleLostPerson(pendingRequest);
        }
        this.pendingHomeRequests = new ArrayList<>();
    }


    public boolean checkIfConstraintsAreBroken(List<Agent> agents){
        for(Agent agent : agents){
            if(agentToRides==null || !agentToRides.containsKey(agent) || agentToRides.get(agent).isEmpty()){
                return true;
            }
            Ride toRide = agentToRides.get(agent).get(0);
            Ride backRide = agentToRides.get(agent).size()>1 ? agentToRides.get(agent).get(1) : null;
            if(toRide==null){
                return true;
            }

            Stop stop = CommonFunctionHelper.getAgentStops(toRide,agent).get(0);
            if(!CommonFunctionHelper.isOverlapping(stop.getStartTime(),stop.getStartTime(),agent.getRequest().getArrivalInterval().getStart(),agent.getRequest().getArrivalInterval().getEnd())){
                return true;
            }
            if(backRide!=null){
                stop = CommonFunctionHelper.getAgentStops(backRide,agent).get(0);
                if(!CommonFunctionHelper.isOverlapping(stop.getStartTime(),stop.getStartTime(),agent.getRequest().getDepartureInterval().getStart(),agent.getRequest().getDepartureInterval().getEnd())){
                    return true;
                }
            }else{
                if(!lost.containsKey(agent)){
                    return true;
                }
            }
            if(agent instanceof AgentWithConstraints){
                AgentWithConstraints agent2 = (AgentWithConstraints) agent;
                Set<Object> set = new HashSet<>();
                set.add(toRide);
                set.add(agent);
                if(oneWayMinutesTravelled.get(set)>agent2.getWillingToRideInMinutes()){
                    return true;
                }
                if(backRide!=null){
                    set.remove(toRide);
                    set.add(backRide);
                    if(oneWayMinutesTravelled.get(set)>agent2.getWillingToRideInMinutes()){
                        return true;
                    }
                }

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
        double totalCosts = 0.0;
        for(Agent agent : this.agents){
            totalCosts += getCosts().get(agent);
        }
        return totalCosts;
    }

    @Override
    public double getFinishedTotalEmissions() {
        double totalEmissions = 0.0;
        for(Agent agent : this.agents){
            totalEmissions += getEmissions().get(agent);
        }
        return totalEmissions;
    }

    @Override
    public double getFinishedTotalKmTravelled() {
        double totalKm = 0.0;
        for(Agent agent : this.agents){
            totalKm += getKmTravelled().get(agent);
        }
        return totalKm;
    }

    @Override
    public double getFinishedTotalMinutesTravelled() {
        double totalMinutes = 0.0;
        for(Agent agent : this.agents){
            totalMinutes += getMinutesTravelled().get(agent);
        }
        return totalMinutes;
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


    @Override
    public void writeAdditionalResults(String resultsFolder) {
        try {
            File agentFile = new File(resultsFolder+"\\"+getName()+"AgentResults.csv");
            List<Map<String, String>> agentResponse = new LinkedList<>();
            File rideFile = new File(resultsFolder+"\\"+getName()+"RideResults.csv");
            List<Map<String, String>> rideResponse = new LinkedList<>();
            File summedFile = new File(resultsFolder+"\\"+getName()+"SummedResults.csv");
            List<Map<String, String>> summedResponse = new LinkedList<>();
            CsvMapper mapper = new CsvMapper();
            CsvSchema schema = CsvSchema.emptySchema().withHeader().withColumnSeparator(',');
            MappingIterator<Map<String, String>> iterator = mapper.reader(Map.class)
                    .with(schema)
                    .readValues(agentFile);
            while (iterator.hasNext()) {
                agentResponse.add(iterator.next());
            }
            iterator = mapper.reader(Map.class)
                    .with(schema)
                    .readValues(rideFile);
            while (iterator.hasNext()) {
                rideResponse.add(iterator.next());
            }
            iterator = mapper.reader(Map.class)
                    .with(schema)
                    .readValues(summedFile);
            while (iterator.hasNext()) {
                summedResponse.add(iterator.next());
            }
            for(Map<String,String> map : rideResponse){
                map.put("lostRide", String.valueOf(lost.containsKey(getFinishedRides().stream().reduce((a, b) -> (a.getId()==Long.parseLong(map.get("Ride Id"))?a:b)).get().getAgents().get(0))));

            }
            ObjectWriter csvWriter = new CsvMapper().writer(schema);
            Stream<String> csvStream = rideResponse.stream().map(m -> {
                        try {
                            return csvWriter.writeValueAsString(m);
                        } catch (Exception ex) {
                            return null;
                        }
                    }
            );
            try{
                PrintWriter pw = new PrintWriter(resultsFolder+"\\"+getName() + "RideResults.csv");
                csvStream.forEach(pw::println);
            }catch (Exception e){
                throw new RuntimeException(e.getMessage());
            }
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }


    public void findMatch(Request request) {
        boolean matchFound = false;
        List<Match> eligibleMatches = new ArrayList<>();
        LocalDateTime requestIntervalStart;
        LocalDateTime requestIntervalEnd;

        if (!(request.getRequesttype() == DirectionType.DRIVEHOME && drivers.contains(request.getAgent()))) {
            if (request.getRequesttype() == DirectionType.DRIVETOUNI) {
                requestIntervalStart = request.getArrivalInterval().getStart();
                requestIntervalEnd = request.getArrivalInterval().getEnd();
            } else {
                requestIntervalStart = request.getDepartureInterval().getStart();
                requestIntervalEnd = request.getDepartureInterval().getEnd();
            }

            GeneralManager.compareRequest = request;
            List<Match> matchList;
            matchList = this.activeMatches.stream()
                    .filter(m -> m.getTypeOfGrouping() == request.getRequesttype() && m.getAgents().size() < m.getDriver().getCar().getSeatCount())
                    .collect(Collectors.toList());
            matchList.sort(null);

            List<Match> truncatedList = matchList.subList(0, Math.min(matchList.size(), 20));
            for (Match match : truncatedList) {
                if (checkIfRequestFitsMatch(request, match, requestIntervalStart, requestIntervalEnd)) {
                    eligibleMatches.add(match);
                }
            }

            if (!eligibleMatches.isEmpty()) {
                Match bestMatch = getBestMatch(eligibleMatches, request);
                matchFound = true;
                if (temporaryMatchToSolution.containsKey(bestMatch)) {
                    matchToSolution.put(bestMatch, temporaryMatchToSolution.get(bestMatch));
                    temporaryMatchToSolution.remove(bestMatch);
                }
                setNewMatchPartner(bestMatch, request, requestIntervalStart, requestIntervalEnd, false);
            }

        }

        if (!matchFound) {
            if (request.getRequesttype() == DirectionType.DRIVETOUNI) {
                Match match = new Match(new ArrayList<>(), new HashMap<>(), request.getAgent(), request.getAgent().getCar(), request.getHomePosition(), request.getDropOffPosition(), request.getRequesttype(), request.getArrivalInterval().getStart(), request.getArrivalInterval().getEnd());
                setNewMatchPartner(match, request, request.getArrivalInterval().getStart(), request.getArrivalInterval().getEnd(), true);
            } else {
                if (drivers.contains(request.getAgent())) {
                    Match match = new Match(new ArrayList<>(), new HashMap<>(), request.getAgent(), request.getAgent().getCar(), request.getDropOffPosition(), request.getHomePosition(), request.getRequesttype(), request.getDepartureInterval().getStart(), request.getDepartureInterval().getEnd());
                    setNewMatchPartner(match, request, request.getDepartureInterval().getStart(), request.getDepartureInterval().getEnd(), false);
                    GeneralManager.compareMatch = match;
                    List<Request> pendingList = new ArrayList<>(this.pendingHomeRequests);
                    pendingList.sort(null);
                    List<Request> trunList;
                    trunList = pendingList.subList(0, Math.min(pendingList.size(), 20));
                    GeneralManager.compareMatch = null;
                    for (Request pendingRequest : trunList) {
                        if (match.getAgents().size() < match.getDriver().getCar().getSeatCount()) {
                            requestIntervalStart = pendingRequest.getDepartureInterval().getStart();
                            requestIntervalEnd = pendingRequest.getDepartureInterval().getEnd();
                            if (request.getRequestTime().isAfter(requestIntervalEnd)) {
                                lost.put((AgentWithConstraints) pendingRequest.getAgent(), pendingRequest);
                                handleLostPerson(pendingRequest);
                                this.pendingHomeRequests.remove(pendingRequest);
                                continue;
                            }

                            if (checkIfRequestFitsMatch(pendingRequest, match, requestIntervalStart, requestIntervalEnd)) {
                                if (temporaryMatchToSolution.containsKey(match)) {
                                    matchToSolution.put(match, temporaryMatchToSolution.get(match));
                                    temporaryMatchToSolution.remove(match);
                                }
                                setNewMatchPartner(match, pendingRequest, requestIntervalStart, requestIntervalEnd, false);
                                this.pendingHomeRequests.remove(pendingRequest);
                            }
                        }
                    }
                } else {
                    pendingHomeRequests.add(request);
                }
            }
        }
        GeneralManager.compareRequest = null;
    }


    public void calculateMetrics(Ride ride) {
        Coordinate firstCoordinate = ride.getStartPosition();
        Coordinate secondCoordinate;
        Map<Agent, Double> agentToDistance = new HashMap<>();
        Map<Agent, Double> agentToTime = new HashMap<>();
        for (Agent agent : ride.getAgents()) {
            agentToDistance.put(agent, 0.0);
            agentToTime.put(agent, 0.0);
        }
        for (Stop stop : ride.getExtraStops()) {
            secondCoordinate = stop.getStopCoordinate();
            List<Location> list = new ArrayList<>();
            list.add(Coordinate.coordinateToLocation(firstCoordinate));
            list.add(Coordinate.coordinateToLocation(secondCoordinate));
            double distance = ModeExecutionManager.distanceMap.get(list);
            double time = ModeExecutionManager.timeMap.get(list) + this.stopTime;
            for (Agent agent : ride.getAgents()) {
                if (agentToDistance.containsKey(agent)) {
                    agentToDistance.put(agent, agentToDistance.get(agent) + distance);
                    agentToTime.put(agent, agentToTime.get(agent) + time);
                }
                if (stop.getPersonsInQuestion().contains(agent)) {
                    if (ride.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
                        agentToTime.put(agent, agentToTime.get(agent) - this.stopTime);
                        setMetrics(agent, ride, agentToDistance, agentToTime);
                        agentToDistance.remove(agent);
                        agentToTime.remove(agent);
                    } else {
                        agentToDistance.put(agent, 0.0);
                        agentToTime.put(agent, 0.0);
                    }
                }
            }
            firstCoordinate = secondCoordinate;
        }
        if (!ride.getExtraStops().isEmpty()) {
            List<Location> list = new ArrayList<>();
            list.add(Coordinate.coordinateToLocation(firstCoordinate));
            list.add(Coordinate.coordinateToLocation(ride.getEndPosition()));
            double time = ModeExecutionManager.timeMap.get(list);
            double distance = ModeExecutionManager.distanceMap.get(list);
            for (Agent agent : agentToDistance.keySet()) {
                agentToDistance.put(agent, agentToDistance.get(agent) + distance);
                agentToTime.put(agent, agentToTime.get(agent) + time);
                setMetrics(agent, ride, agentToDistance, agentToTime);
            }
        } else {
            ResponsePath path = CommonFunctionHelper.getSimpleBestGraphhopperPath(graphHopper,ride.getStartPosition(),ride.getEndPosition());
            for (Agent a : ride.getAgents()) {
                agentToDistance.put(a,path.getDistance());
                agentToTime.put(a, (double)(path.getTime()/60000L));
                setMetrics(a, ride, agentToDistance, agentToTime);
            }
        }
    }


    public void setMetrics(Agent agent, Ride ride, Map<Agent, Double> agentToDistance, Map<Agent, Double> agentToTime) {
        Set<Object> set;
        if (agent.equals(ride.getDriver()) || this.handleLost && this.lost.containsKey(agent)) {
            double co2 = (agentToDistance.get(agent) / 1000) * agent.getCar().getConsumptionPerKm() * agent.getCar().getCo2EmissionPerLiter();
            double costPerKm = this.handleLost && this.lost.containsKey(agent) ? 0.3 : agent.getCar().getConsumptionPerKm() * agent.getCar().getPricePerLiter();
            double cost = (agentToDistance.get(agent) / 1000) * costPerKm;
            for (Agent a : ride.getAgents()) {
                set = new HashSet<>();
                set.add(a);
                set.add(ride);
                oneWayCosts.put(set, cost / ride.getAgents().size());
                oneWayEmissions.put(set, co2 / ride.getAgents().size());

                if (emissions.containsKey(a)) {
                    emissions.put(a, emissions.get(a) + co2 / ride.getAgents().size());
                    costs.put(a, costs.get(a) + cost / ride.getAgents().size());
                } else {
                    emissions.put(a, co2 / ride.getAgents().size());
                    costs.put(a, cost / ride.getAgents().size());
                }
            }
        }

        set = new HashSet<>();
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


    public Ride matchToRide(Match match) {
        VehicleRoutingProblemSolution solution = matchToSolution.get(match);
        Ride ride;

        if (solution == null || match.getDifferentStops().isEmpty()) {
            LocalDateTime rideEndTime ;
            if (match.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
                rideEndTime = match.getDriver().getRequest().getFavoredArrivalTime();
                long minutesBetweenAgentPreferredArrivalAndMatchInterval = ChronoUnit.MINUTES.between(match.getDriver().getRequest().getFavoredArrivalTime(), getStartTimeForNoStops(match, match.getDriver().getRequest().getFavoredArrivalTime()));
                rideEndTime = minutesBetweenAgentPreferredArrivalAndMatchInterval < 0 ? rideEndTime.minusMinutes(Math.abs(minutesBetweenAgentPreferredArrivalAndMatchInterval)) : rideEndTime.plusMinutes(minutesBetweenAgentPreferredArrivalAndMatchInterval);
            } else {
                ResponsePath path = CommonFunctionHelper.getSimpleBestGraphhopperPath(this.graphHopper,match.getStartPosition(),match.getEndPosition());
                long timeInMinutes = path.getTime()/60000L;

                rideEndTime = match.getDriver().getRequest().getFavoredDepartureTime().plusMinutes(timeInMinutes);
                long minutesBetweenAgentPreferredArrivalAndMatchInterval = ChronoUnit.MINUTES.between(match.getDriver().getRequest().getFavoredDepartureTime(), getStartTimeForNoStops(match, match.getDriver().getRequest().getFavoredDepartureTime()));
                rideEndTime = minutesBetweenAgentPreferredArrivalAndMatchInterval < 0 ? rideEndTime.minusMinutes(Math.abs(minutesBetweenAgentPreferredArrivalAndMatchInterval)) : rideEndTime.plusMinutes(minutesBetweenAgentPreferredArrivalAndMatchInterval);
            }
            ride = new Ride(match.getStartPosition(), match.getEndPosition(), matchToStartTime.get(match), rideEndTime, match.getDriver().getCar(), match.getDriver(), match.getTypeOfGrouping(), match.getAgents());
            if (ride.getEndTime() == null) {
                ride.setEndTime(CommonFunctionHelper.getSimpleArrivalTime(graphHopper,ride.getStartTime(),ride.getStartPosition(),ride.getEndPosition()));
            }
        } else {
            List<VehicleRoute> routes = (List<VehicleRoute>) solution.getRoutes();
            VehicleRoute route = routes.get(0);
            Stopreason reasonForStopping;
            LocalDateTime rideEndTime = matchToStartTime.get(match).plusMinutes((long) (solution.getCost()));
            ride = new Ride(match.getStartPosition(), match.getEndPosition(), matchToStartTime.get(match), rideEndTime, match.getDriver().getCar(), match.getDriver(), match.getTypeOfGrouping(), match.getAgents());
            if (match.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
                reasonForStopping = Stopreason.DROPOFF;
            } else {
                reasonForStopping = Stopreason.PICKUP;
            }
            List<Agent> stopAgents;
            List<Stop> stops = new ArrayList<>();
            double lastArrivalTime = 1000;
            double departureTime;
            LocalDateTime lastArrivalLocalTime = ride.getEndTime();
            TourActivity service;
            if (match.getTypeOfGrouping() == DirectionType.DRIVEHOME) {
                lastArrivalTime = route.getEnd().getArrTime();
            }
            for (int j = route.getActivities().size() - 1; j > 0; j--) {
                service = route.getActivities().get(j);
                if (j != route.getActivities().size() - 1 || match.getTypeOfGrouping() != DirectionType.DRIVETOUNI) {
                    departureTime = service.getEndTime();
                    stopAgents = new ArrayList<>();
                    for (Agent agent : match.getAgents()) {
                        if (Coordinate.coordinateTheSameAsLocation(agent.getRequest().getDropOffPosition(), service.getLocation())) {
                            stopAgents.add(agent);
                        }
                    }
                    stops.add(new Stop(lastArrivalLocalTime.minusMinutes((long) (lastArrivalTime - departureTime)).minusMinutes(this.stopTime), lastArrivalLocalTime.minusMinutes((long) (lastArrivalTime - departureTime)), Coordinate.locationToCoordinate(service.getLocation()), reasonForStopping, stopAgents));
                    lastArrivalLocalTime = lastArrivalLocalTime.minusMinutes((long) (lastArrivalTime - departureTime)).minusMinutes(this.stopTime);
                }
                lastArrivalTime = service.getArrTime();
            }
            if (match.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
                service = route.getActivities().get(0);
                departureTime = service.getEndTime();
                stopAgents = new ArrayList<>();
                for (Agent agent : match.getAgents()) {
                    if (Coordinate.coordinateTheSameAsLocation(agent.getRequest().getDropOffPosition(), service.getLocation())) {
                        stopAgents.add(agent);
                    }
                }
                stops.add(new Stop(lastArrivalLocalTime.minusMinutes((long) (lastArrivalTime - departureTime)).minusMinutes(this.stopTime), lastArrivalLocalTime.minusMinutes((long) (lastArrivalTime - departureTime)), Coordinate.locationToCoordinate(service.getLocation()), reasonForStopping, stopAgents));
            }
            stops.sort(Comparator.comparing(Stop::getStartTime));
            ride.setExtraStops(stops);
        }

        activeMatches.remove(match);
        finishedMatches.add(match);
        rides.add(ride);
        return ride;
    }


    public boolean checkIfRequestFitsMatch(Request request, Match match, LocalDateTime requestIntervalStart, LocalDateTime requestIntervalEnd) {
        AgentWithConstraints agent = (AgentWithConstraints) request.getAgent();
        if (match.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
            if (request.getDropOffPosition().equals(match.getEndPosition())) {
                return CommonFunctionHelper.isOverlapping(match.getTimeIntervalStart(), match.getTimeIntervalEnd(), requestIntervalStart, requestIntervalEnd) && getDistanceToDriverInMeters(request, match) <= agent.getWillingToWalkInMeters() && wouldDriveBy(request.getDropOffPosition(), match, requestIntervalStart, requestIntervalEnd, (AgentWithConstraints) request.getAgent());
            } else {
                return CommonFunctionHelper.isOverlapping(match.getTimeIntervalStart(), match.getTimeIntervalEnd(), requestIntervalStart.plusSeconds(this.secondsBetweenDropOffs.get(request.getUniPLZ() + "-" + match.getDriver().getRequest().getUniPLZ())), requestIntervalEnd.plusSeconds(this.secondsBetweenDropOffs.get(request.getUniPLZ() + "-" + match.getDriver().getRequest().getUniPLZ()))) && getDistanceToDriverInMeters(request, match) <= agent.getWillingToWalkInMeters() && wouldDriveBy(request.getDropOffPosition(), match, requestIntervalStart, requestIntervalEnd, agent);
            }
        } else {
            if (request.getDropOffPosition().equals(match.getStartPosition())) {
                return CommonFunctionHelper.isOverlapping(match.getTimeIntervalStart(), match.getTimeIntervalEnd(), requestIntervalStart, requestIntervalEnd) && getDistanceToDriverInMeters(request, match) <= agent.getWillingToWalkInMeters() && wouldDriveBy(request.getDropOffPosition(), match, requestIntervalStart, requestIntervalEnd, agent);
            } else {
                return CommonFunctionHelper.isOverlapping(match.getTimeIntervalStart().plusSeconds(this.secondsBetweenDropOffs.get(match.getDriver().getRequest().getUniPLZ() + "-" + request.getUniPLZ())), match.getTimeIntervalEnd(), requestIntervalStart.minusSeconds(this.secondsBetweenDropOffs.get(match.getDriver().getRequest().getUniPLZ() + "-" + request.getUniPLZ())), requestIntervalEnd) && getDistanceToDriverInMeters(request, match) <= agent.getWillingToWalkInMeters() && wouldDriveBy(request.getDropOffPosition(), match, requestIntervalStart, requestIntervalEnd, agent);
            }
        }
    }


    public void setNewMatchPartner(Match match, Request request, LocalDateTime requestIntervalStart, LocalDateTime requestIntervalEnd, boolean newDriver) {
        Coordinate position;

        boolean sameAsBefore = true;
        boolean sameDropOff = false;
        VehicleRoutingProblemSolution solution = this.matchToSolution.remove(match);
        LocalDateTime startTime = this.matchToStartTime.remove(match);

        if (startTime != null) {
            this.sortedEvents.remove(new Event("rideStart",startTime,match));
        }

        if (match.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
            position = match.getEndPosition();
        } else {
            position = match.getStartPosition();
        }
        int index = this.activeMatches.indexOf(match);
        List<Agent> matchPeople = match.getAgents();
        matchPeople.add(request.getAgent());
        match.setAgents(matchPeople);
        String oldInterval = CommonFunctionHelper.getIntervalString(match.getTimeIntervalStart(), match.getTimeIntervalEnd());
        setNewInterval(match, requestIntervalStart, requestIntervalEnd, request.getDropOffPosition());
        if (!request.getDropOffPosition().equals(position)) {
            Map<Coordinate, String> map = match.getDifferentStops();
            if (match.getDifferentStops().containsKey(request.getDropOffPosition())) {
                if (!match.getDifferentStops().get(request.getDropOffPosition()).equals(getNewStopInterval(match.getDifferentStops().get(request.getDropOffPosition()), requestIntervalStart, requestIntervalEnd))) {
                    map.put(request.getDropOffPosition(), getNewStopInterval(match.getDifferentStops().get(request.getDropOffPosition()), requestIntervalStart, requestIntervalEnd));
                    sameAsBefore = false;
                }
            } else {
                map.put(request.getDropOffPosition(), CommonFunctionHelper.getIntervalString(requestIntervalStart, requestIntervalEnd));
                sameAsBefore = false;
            }
            match.setDifferentStops(map);
        } else {
            sameDropOff = true;
            if (!oldInterval.equals(CommonFunctionHelper.getIntervalString(match.getTimeIntervalStart(), match.getTimeIntervalEnd()))) {
                sameAsBefore = false;
            }
        }
        if (newDriver) {
            drivers.add(request.getAgent());
        }
        if (index == -1) {
            activeMatches.add(match);
        } else {
            activeMatches.set(index, match);
        }
        this.setDriveStartTime(match, solution, startTime, sameAsBefore, sameDropOff);
    }


    public void setNewInterval(Match match, LocalDateTime start, LocalDateTime end, Coordinate dropOffPosition) {
        Coordinate referencePosition = match.getTypeOfGrouping() == DirectionType.DRIVETOUNI ? match.getEndPosition() : match.getStartPosition();
        if (referencePosition.equals(dropOffPosition)) {
            if (match.getTimeIntervalStart().isBefore(start)) {
                match.setTimeIntervalStart(start);
            }
            if (match.getTimeIntervalEnd().isAfter(end)) {
                match.setTimeIntervalEnd(end);
            }
        }
    }


    public String getNewStopInterval(String oldInterval, LocalDateTime newStart, LocalDateTime newEnd) {
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


    public Match getBestMatch(List<Match> eligibleMatches, Request request) {
        Match bestMatch = null;
        double bestDistanceToDriver = Double.MAX_VALUE;
        for (Match match : eligibleMatches) {
            double distance = getDistanceToDriverInMeters(request, match);
            if (distance < bestDistanceToDriver) {
                bestMatch = match;
                bestDistanceToDriver = distance;
            }
        }
        return bestMatch;
    }

    public boolean wouldDriveBy(Coordinate dropOffCoordinate, Match match, LocalDateTime start, LocalDateTime end, AgentWithConstraints newAgent) {
        temporaryMatchToSolution.remove(match);
        boolean sameStopAsDriver = false;

        LocalDateTime oldTimeIntervalStart = match.getTimeIntervalStart();
        LocalDateTime oldTimeIntervalEnd = match.getTimeIntervalEnd();
        if ((match.getTypeOfGrouping() == DirectionType.DRIVEHOME && match.getStartPosition().equals(dropOffCoordinate)) || (match.getTypeOfGrouping() == DirectionType.DRIVETOUNI && match.getEndPosition().equals(dropOffCoordinate))) {
            if (match.getDifferentStops().isEmpty() && !match.getTimeIntervalStart().isAfter(end) && !match.getTimeIntervalEnd().isBefore(start)) {
                return CommonFunctionHelper.getSimpleBestGraphhopperPath(this.graphHopper,match.getStartPosition(),match.getEndPosition()).getTime()/60000 <= newAgent.getWillingToRideInMinutes();
            }
            sameStopAsDriver = true;
            setNewInterval(match, start, end, dropOffCoordinate);
        }

        final int WEIGHT_INDEX = 0;
        VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("vehicleType").addCapacityDimension(WEIGHT_INDEX, 5).setCostPerServiceTime(1);
        VehicleType vehicleType = vehicleTypeBuilder.build();

        VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance("vehicle");
        vehicleBuilder.setStartLocation(Location.newInstance(match.getStartPosition().getLongitude(), match.getStartPosition().getLatitude()));
        vehicleBuilder.setEndLocation(Location.newInstance(match.getEndPosition().getLongitude(), match.getEndPosition().getLatitude()));
        vehicleBuilder.setType(vehicleType);
        vehicleBuilder.setEarliestStart(0).setLatestArrival(1000);

        VehicleImpl vehicle = vehicleBuilder.build();

        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();

        VehicleRoutingTransportCosts transportCosts = new TransportCosts();
        vrpBuilder.setRoutingCost(transportCosts);
        vrpBuilder.setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);
        vrpBuilder.addVehicle(vehicle);

        int i = 0;
        double intervalStart;
        double intervalEnd;
        for (Coordinate coordinate : match.getDifferentStops().keySet()) {
            String stopInterval = match.getDifferentStops().get(coordinate);
            if (coordinate.equals(dropOffCoordinate)) {
                if (!CommonFunctionHelper.isOverlapping(LocalDateTime.parse(stopInterval.split("-")[0], GeneralManager.dateTimeFormatter), LocalDateTime.parse(stopInterval.split("-")[1], GeneralManager.dateTimeFormatter), start, end)) {
                    return false;
                }
                stopInterval = getNewStopInterval(stopInterval, start, end);
            }
            if (match.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
                intervalStart = 1000 - (Math.max(ChronoUnit.MINUTES.between(LocalDateTime.parse(stopInterval.split("-")[0], GeneralManager.dateTimeFormatter), match.getTimeIntervalEnd()), 0));
                intervalEnd = 1000 - (Math.max(0, ChronoUnit.MINUTES.between(LocalDateTime.parse(stopInterval.split("-")[1], GeneralManager.dateTimeFormatter), match.getTimeIntervalEnd())));
            } else {
                intervalStart = Math.max(ChronoUnit.MINUTES.between(match.getTimeIntervalStart(), LocalDateTime.parse(stopInterval.split("-")[0], GeneralManager.dateTimeFormatter)), 0);
                intervalEnd = Math.max(ChronoUnit.MINUTES.between(match.getTimeIntervalStart(), LocalDateTime.parse(stopInterval.split("-")[1], GeneralManager.dateTimeFormatter)), 0);
            }
            Service service = Service.Builder.newInstance(String.valueOf(++i)).addSizeDimension(WEIGHT_INDEX, 1).setLocation(Location.newInstance(coordinate.getLongitude(), coordinate.getLatitude())).setServiceTime(this.stopTime).addTimeWindow(intervalStart, intervalEnd).build();
            vrpBuilder.addJob(service);
        }
        if (!sameStopAsDriver && !match.getDifferentStops().containsKey(dropOffCoordinate)) {
            if (match.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
                intervalStart = 1000 - (ChronoUnit.MINUTES.between(start, match.getTimeIntervalEnd()));
                intervalEnd = 1000 - (Math.max(0, ChronoUnit.MINUTES.between(end, match.getTimeIntervalEnd())));
            } else {
                intervalStart = Math.max(ChronoUnit.MINUTES.between(match.getTimeIntervalStart(), start), 0);
                intervalEnd = ChronoUnit.MINUTES.between(match.getTimeIntervalStart(), end);
            }
            Service service = Service.Builder.newInstance(String.valueOf(++i)).addSizeDimension(WEIGHT_INDEX, 1).setLocation(Location.newInstance(dropOffCoordinate.getLongitude(), dropOffCoordinate.getLatitude())).setServiceTime(this.stopTime).addTimeWindow(intervalStart, intervalEnd).build();
            vrpBuilder.addJob(service);
        }
        Coordinate position;
        int priority;
        if (match.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
            intervalStart = 1000 - (ChronoUnit.MINUTES.between(match.getTimeIntervalStart(), match.getTimeIntervalEnd()));
            intervalEnd = 1000;
            priority = 10;
            position = match.getEndPosition();
        } else {
            intervalStart = 0;
            intervalEnd = ChronoUnit.MINUTES.between(match.getTimeIntervalStart(), match.getTimeIntervalEnd());
            priority = 1;
            position = match.getStartPosition();
        }
        Service service = Service.Builder.newInstance(String.valueOf(++i)).addSizeDimension(WEIGHT_INDEX, 1).setLocation(Location.newInstance(position.getLongitude(), position.getLatitude())).addTimeWindow(intervalStart, intervalEnd).setPriority(priority).build();
        vrpBuilder.addJob(service);

        VehicleRoutingProblem problem = vrpBuilder.build();

        StateManager stateManager = new StateManager(problem);
        ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);
        constraintManager.addConstraint(new ActivityOrderConstraint(), ConstraintManager.Priority.CRITICAL);
        HardActivityConstraint constraint = new ActivityWaitConstraintOneAllowed();
        constraintManager.addConstraint(constraint, ConstraintManager.Priority.CRITICAL);
        VehicleRoutingAlgorithm algorithm = Jsprit.Builder.newInstance(problem).setStateAndConstraintManager(stateManager, constraintManager).buildAlgorithm();
        Jsprit.createAlgorithm(problem);
        algorithm.setMaxIterations(100);
        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
        VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

        int counter = 0;
        List<TourActivity> activities = ((List<VehicleRoute>) bestSolution.getRoutes()).get(0).getActivities();
        int activityCounter = 0;
        if (bestSolution.getUnassignedJobs().isEmpty()) {
            for (TourActivity a : activities) {
                long timeForStop = match.getTypeOfGrouping() == DirectionType.DRIVETOUNI && activityCounter == activities.size() - 1 || match.getTypeOfGrouping() == DirectionType.DRIVEHOME && activityCounter == 0 ? 0 : this.stopTime;
                if (activityCounter > 0 && a.getEndTime() - a.getArrTime() > timeForStop) {
                    counter++;
                }
                activityCounter++;
            }
        }
        if (counter > 0) {
            bestSolution = RouteCalculationHelper.correctSolution(this, stopTime, match.getTypeOfGrouping(), bestSolution, vrpBuilder);
        }

        match.setTimeIntervalStart(oldTimeIntervalStart);
        match.setTimeIntervalEnd(oldTimeIntervalEnd);
        List<AgentWithConstraints> agents = new ArrayList<>(match.getAgents().stream().filter(a -> a instanceof AgentWithConstraints).map(a -> (AgentWithConstraints) a).toList());
        agents.add(newAgent);

        if (bestSolution != null && bestSolution.getUnassignedJobs().isEmpty() && rideTimeIsAcceptedByAllAgents(bestSolution, agents, match.getTypeOfGrouping())) {
            this.temporaryMatchToSolution.put(match, bestSolution);
            return true;
        }

        return false;
    }


    public void setDriveStartTime(Match match, VehicleRoutingProblemSolution solution, LocalDateTime oldStartTime, boolean sameAsBefore, boolean sameDropOff) {
        LocalDateTime startTime;

        if (oldStartTime == null) {
            if (match.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
                startTime = CommonFunctionHelper.calculateNecessaryDriveStartTime(match.getDriver().getRequest());
            } else {
                startTime = match.getTimeIntervalStart();
            }
        } else {
            this.matchToSolution.put(match, solution);
            if (!sameAsBefore) {
                if (sameDropOff && match.getDifferentStops().isEmpty()) {
                    LocalDateTime referenceTime = match.getTypeOfGrouping() == DirectionType.DRIVETOUNI ? match.getDriver().getRequest().getFavoredArrivalTime() : match.getDriver().getRequest().getFavoredDepartureTime();
                    long minutesBetweenAgentPreferredTimeAndMatchInterval = ChronoUnit.MINUTES.between(referenceTime, getStartTimeForNoStops(match, referenceTime));
                    if (match.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
                        referenceTime = CommonFunctionHelper.calculateNecessaryDriveStartTime(match.getDriver().getRequest());
                    }
                    startTime = minutesBetweenAgentPreferredTimeAndMatchInterval < 0 ? referenceTime.minusMinutes(Math.abs(minutesBetweenAgentPreferredTimeAndMatchInterval)) : referenceTime.plusMinutes(minutesBetweenAgentPreferredTimeAndMatchInterval);
                } else {
                    VehicleRoute route = ((List<VehicleRoute>) solution.getRoutes()).get(0);
                    if (match.getTypeOfGrouping() == DirectionType.DRIVETOUNI) {
                        startTime = match.getTimeIntervalEnd().minusMinutes((long) (1000 - route.getEnd().getArrTime())).minusMinutes((long) solution.getCost());
                    } else {
                        startTime = match.getTimeIntervalStart().plusMinutes(((long) route.getActivities().get(0).getEndTime()));
                    }
                }
            } else {
                startTime = oldStartTime;
            }
        }

        this.matchToStartTime.put(match, startTime);
        this.sortedEvents.add(new Event("rideStart",startTime,match));
        Collections.sort(this.sortedEvents);
    }


    public double getDistanceToDriverInMeters(Request request, Match match) {
        GHPoint ghPointStart = new GHPoint(request.getHomePosition().getLatitude(), request.getHomePosition().getLongitude());
        GHPoint ghPointEnd;
        if (request.getRequesttype() == DirectionType.DRIVETOUNI) {
            ghPointEnd = new GHPoint(match.getStartPosition().getLatitude(), match.getStartPosition().getLongitude());
        } else {
            ghPointEnd = new GHPoint(match.getEndPosition().getLatitude(), match.getEndPosition().getLongitude());
        }
        GHRequest ghRequest = new GHRequest(ghPointStart, ghPointEnd).setProfile("foot").setLocale(Locale.GERMANY);

        GHResponse rsp = ModeExecutionManager.graphHopper.route(ghRequest);
        ResponsePath path = rsp.getBest();
        return path.getDistance();
    }

    public LocalDateTime getStartTimeForNoStops(Match match, LocalDateTime referenceTime) {
        LocalDateTime fittingTime;
        if (CommonFunctionHelper.isOverlapping(referenceTime, referenceTime, match.getTimeIntervalStart(), match.getTimeIntervalEnd())) {
            fittingTime = referenceTime;
        } else {
            fittingTime = Math.abs(ChronoUnit.MINUTES.between(match.getTimeIntervalStart(), referenceTime)) < Math.abs(ChronoUnit.MINUTES.between(referenceTime, match.getTimeIntervalEnd())) ? match.getTimeIntervalStart() : match.getTimeIntervalEnd();
        }
        return fittingTime;
    }


    public boolean rideTimeIsAcceptedByAllAgents(VehicleRoutingProblemSolution solution, List<AgentWithConstraints> agents, DirectionType directionType) {
        Map<AgentWithConstraints, Double> agentToTime = new HashMap<>();
        for (AgentWithConstraints agent : agents) {
            agentToTime.put(agent, 0.0);
        }

        VehicleRoute route = ((List<VehicleRoute>) solution.getRoutes()).get(0);
        List<Location> locations = new ArrayList<>();
        Map<Location, Double> locationToStopTime = new HashMap<>();
        for (TourActivity activity : route.getActivities()) {
            locations.add(activity.getLocation());
            locationToStopTime.put(activity.getLocation(), activity.getOperationTime());
        }
        locations.add(route.getEnd().getLocation());
        Location firstLocation = route.getStart().getLocation();
        Location secondLocation;
        for (Location location : locations) {
            secondLocation = location;
            List<Location> list = new ArrayList<>();
            list.add(firstLocation);
            list.add(secondLocation);
            double time = ModeExecutionManager.timeMap.get(list);
            if (locationToStopTime.containsKey(secondLocation)) {
                time += locationToStopTime.get(secondLocation);
            }
            for (AgentWithConstraints agent : agents) {
                if (agentToTime.containsKey(agent)) {
                    agentToTime.put(agent, agentToTime.get(agent) + time);
                    if (Coordinate.coordinateTheSameAsLocation(agent.getRequest().getDropOffPosition(), secondLocation)) {
                        if (directionType == DirectionType.DRIVETOUNI) {
                            long stopTime = 0;
                            if (!secondLocation.equals(route.getEnd().getLocation())) {
                                stopTime = this.stopTime;
                            }
                            if (agentToTime.get(agent) - stopTime > agent.getWillingToRideInMinutes()) {
                                return false;
                            }
                            agentToTime.remove(agent);
                        } else {
                            agentToTime.put(agent, 0.0);
                        }
                    }
                }
            }
            firstLocation = secondLocation;
        }
        for (AgentWithConstraints agent : agentToTime.keySet()) {
            if (agentToTime.get(agent) > agent.getWillingToRideInMinutes()) {
                return false;
            }
        }
        return true;
    }


    public void handleLostPerson(Request request) {
        if(this.handleLost){
            ResponsePath path = CommonFunctionHelper.getSimpleBestGraphhopperPath(this.graphHopper,request.getDropOffPosition(),request.getHomePosition());
            long timeInMinutes = path.getTime()/60000L;

            Ride ride = new Ride(request.getDropOffPosition(),request.getHomePosition(),request.getDepartureInterval().getEnd(),request.getDepartureInterval().getEnd().plusMinutes(timeInMinutes),new StandInVehicle(co2EmissionPerLiter, pricePerLiter, consumptionPerKm),null, DirectionType.DRIVEHOME,List.of(request.getAgent()));
            this.rides.add(ride);
            calculateMetrics(ride);
        }
    }


    public List<Match> getFinishedMatches() {
        return finishedMatches;
    }

    public Map<AgentWithConstraints, Request> getLost() {
        return lost;
    }


}