package camof.modeexecution.mobilitymodels.modehelpers;

import camof.GeneralManager;
import camof.modeexecution.Coordinate;
import camof.modeexecution.Request;
import camof.modeexecution.DirectionType;
import camof.modeexecution.groupings.Match;
import camof.modeexecution.groupings.Ride;
import camof.modeexecution.mobilitymodels.MobilityMode;
import camof.modeexecution.mobilitymodels.tsphelpers.ActivityOrderConstraint;
import camof.modeexecution.mobilitymodels.tsphelpers.ActivityWaitConstraintNoneAllowed;
import camof.modeexecution.mobilitymodels.tsphelpers.ActivityWaitConstraintOneAllowed;
import camof.modeexecution.mobilitymodels.tsphelpers.TransportCosts;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Solutions;

import java.time.LocalDateTime;
import java.util.*;

public class RouteCalculationHelper {


    public static Ride turnSolutionIntoFinishedRide(VehicleRoutingProblemSolution solution, Match match){
        return null;
    }


    public static VehicleRoutingProblemSolution turnRequestSequenceIntoSolution(List<Request> requests, Set<HardActivityConstraint> constraints){
        return null;
    }


    public static VehicleRoutingProblemSolution turnMatchIntoSolution(Match match, LocalDateTime earliestStartTime, LocalDateTime latestEndTime, Set<HardActivityConstraint> constraints, long stopTime){
        final int WEIGHT_INDEX = 0;
        VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("vehicleType").addCapacityDimension(WEIGHT_INDEX, 5).setCostPerServiceTime(1);
        VehicleType vehicleType = vehicleTypeBuilder.build();

        VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance("vehicle");
        vehicleBuilder.setStartLocation(Location.newInstance(match.getStartPosition().getLongitude(), match.getStartPosition().getLatitude()));
        vehicleBuilder.setEndLocation(Location.newInstance(match.getEndPosition().getLongitude(), match.getEndPosition().getLatitude()));
        vehicleBuilder.setType(vehicleType);
        vehicleBuilder.setEarliestStart(turnDateTimeIntoMinutes(earliestStartTime)).setLatestArrival(turnDateTimeIntoMinutes(latestEndTime));

        VehicleImpl vehicle = vehicleBuilder.build();
        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();

        VehicleRoutingTransportCosts transportCosts = new TransportCosts();
        vrpBuilder.setRoutingCost(transportCosts);
        vrpBuilder.setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);
        vrpBuilder.addVehicle(vehicle);
        int counter = 0;

        for(Coordinate coordinate : match.getDifferentStops().keySet()){
            String intervals = match.getDifferentStops().get(coordinate);
            for(String interval : intervals.split(",")){
                Service.Builder servicebuilder = Service.Builder.newInstance(String.valueOf(++counter)).addSizeDimension(WEIGHT_INDEX, 1).setLocation(Location.newInstance(coordinate.getLongitude(), coordinate.getLatitude())).setServiceTime(stopTime);
                if(!interval.isEmpty()){
                    servicebuilder.addTimeWindow(TimeWindow.newInstance(turnDateTimeIntoMinutes(LocalDateTime.parse(interval.split("-")[0],GeneralManager.dateTimeFormatter)),turnDateTimeIntoMinutes(LocalDateTime.parse(interval.split("-")[1],GeneralManager.dateTimeFormatter))));
                }
                vrpBuilder.addJob(servicebuilder.build());
            }
        }

        VehicleRoutingProblem problem = vrpBuilder.build();

        StateManager stateManager = new StateManager(problem);
        ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);

        for(HardActivityConstraint constraint : constraints){
            constraintManager.addConstraint(constraint,ConstraintManager.Priority.CRITICAL);
        }
        VehicleRoutingAlgorithm algorithm = Jsprit.Builder.newInstance(problem).setStateAndConstraintManager(stateManager, constraintManager).buildAlgorithm();
        Jsprit.createAlgorithm(problem);
        algorithm.setMaxIterations(100);
        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();


        return Solutions.bestOf(solutions);
    }



    public static Map<String, Map<Set<Object>,Double>> getMetricsForRideSolution(Ride ride, VehicleRoutingProblemSolution solution){
        return null;
    }



    public static Map<Coordinate,String> getStopIntervalsFromRequests(Set<Request> requests){
        Map<Coordinate,String> differentStops = new HashMap<>();
        for(Request request : requests){
            Coordinate coordinate;
            LocalDateTime start;
            LocalDateTime end;
            if(request.getRequesttype()== DirectionType.DRIVEHOME){
                coordinate = request.getHomePosition();
                start = request.getDepartureInterval().getStart();
                end = request.getArrivalInterval().getEnd();
            }else{
                coordinate = request.getDropOffPosition();
                start = request.getDepartureInterval().getStart();
                end = request.getDepartureInterval().getEnd();
            }

            if(differentStops.containsKey(coordinate)){
                String[] intervals = differentStops.get(coordinate).split(",");
                for(String oldInterval : intervals){ //TODO: wenn overlap mit mehreren, dann schauen welches Interval größeren Overlap
                    if(CommonFunctionHelper.isOverlapping(start,end,LocalDateTime.parse(oldInterval.split("-")[0],GeneralManager.dateTimeFormatter),LocalDateTime.parse(oldInterval.split("-")[1],GeneralManager.dateTimeFormatter))){
                        differentStops.put(coordinate,CommonFunctionHelper.getNewStopInterval(oldInterval,start,end));
                    }else{
                        differentStops.put(coordinate,oldInterval + "," + CommonFunctionHelper.getIntervalString(start,end));
                    }
                }
            }else{
                differentStops.put(coordinate,CommonFunctionHelper.getIntervalString(start,end));
            }
        }
        return differentStops;
    }


    public static long turnDateTimeIntoMinutes(LocalDateTime time){
        return time.getHour() * 60L + time.getMinute(); //TODO was wenn tage unterschiedlich?
    }


    public static VehicleRoutingProblemSolution turnRequestsIntoSolution(Set<Request> requests, LocalDateTime earliestStartTime, LocalDateTime latestEndTime, Coordinate startLocation, Coordinate endLocation, Set<HardActivityConstraint> constraints, long stopTime){
        VehicleRoutingProblem.Builder vrpBuilder = getRoutingProblemBuilderWithOneVehicle(earliestStartTime,latestEndTime,startLocation,endLocation);

        int counter = 0;
        for(Request request : requests){
            Coordinate pickup;
            Coordinate dropoff;
            LocalDateTime intervalstart;
            LocalDateTime intervalend;
            if(request.getRequesttype()== DirectionType.DRIVETOUNI){
                pickup = request.getHomePosition();
                dropoff = request.getDropOffPosition();
                intervalstart = request.getArrivalInterval().getStart();
                intervalend = request.getArrivalInterval().getEnd();
            }else if(request.getRequesttype()== DirectionType.DRIVEHOME){
                pickup = request.getDropOffPosition();
                dropoff = request.getHomePosition();
                intervalstart = request.getDepartureInterval().getStart();
                intervalend = request.getDepartureInterval().getEnd();
            }else return null;

            Service.Builder pickupService = Service.Builder.newInstance(String.valueOf(++counter)).addSizeDimension(1, 1).setLocation(Location.newInstance(pickup.getLongitude(), pickup.getLatitude())).setServiceTime(stopTime);
            Service.Builder dropoffService = Service.Builder.newInstance(String.valueOf(++counter)).addSizeDimension(1, 1).setLocation(Location.newInstance(dropoff.getLongitude(), dropoff.getLatitude())).setServiceTime(stopTime);
            Service.Builder timedService = request.getRequesttype()== DirectionType.DRIVEHOME ? pickupService : dropoffService;
            timedService.addTimeWindow(new TimeWindow(turnDateTimeIntoMinutes(intervalstart),turnDateTimeIntoMinutes(intervalend)));
            vrpBuilder.addJob(pickupService.build());
            vrpBuilder.addJob(dropoffService.build());
        }

        VehicleRoutingProblem problem = vrpBuilder.build();
        StateManager stateManager = new StateManager(problem);
        ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);
        for(HardActivityConstraint constraint : constraints){
            constraintManager.addConstraint(constraint,ConstraintManager.Priority.CRITICAL);
        }
        VehicleRoutingAlgorithm algorithm = Jsprit.Builder.newInstance(problem).setStateAndConstraintManager(stateManager, constraintManager).buildAlgorithm();
        Jsprit.createAlgorithm(problem);
        algorithm.setMaxIterations(100);
        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();


        return Solutions.bestOf(solutions);
    }


    public static VehicleRoutingProblem.Builder getRoutingProblemBuilderWithOneVehicle(LocalDateTime earliestStartTime, LocalDateTime latestEndTime, Coordinate startLocation, Coordinate endLocation){
        final int WEIGHT_INDEX = 0;
        VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("vehicleType").addCapacityDimension(WEIGHT_INDEX, 5).setCostPerServiceTime(1);
        VehicleType vehicleType = vehicleTypeBuilder.build();

        VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance("vehicle");
        if(startLocation!=null){
            vehicleBuilder.setStartLocation(Location.newInstance(startLocation.getLongitude(), startLocation.getLatitude()));
        }
        if(endLocation!=null){
            vehicleBuilder.setEndLocation(Location.newInstance(endLocation.getLongitude(), endLocation.getLatitude()));
        }
        vehicleBuilder.setType(vehicleType);
        vehicleBuilder.setEarliestStart(turnDateTimeIntoMinutes(earliestStartTime)).setLatestArrival(turnDateTimeIntoMinutes(latestEndTime));
        VehicleImpl vehicle = vehicleBuilder.build();

        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
        VehicleRoutingTransportCosts transportCosts = new TransportCosts();
        vrpBuilder.setRoutingCost(transportCosts);
        vrpBuilder.setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);
        vrpBuilder.addVehicle(vehicle);

        return vrpBuilder;
    }


    /**
     * Try to find another routing solution without waiting times at the stops //TODO besser säubern
     * @param  mobilitymode   current mobility mode which the solution is for
     * @param  stopTime       the time it takes to fit in a stop
     * @param  directionType  direction the ride is going in
     * @param  solution       the old solution that is to be corrected
     * @param  vrpBuilder     the builder for building the routing problem
     * @return                the corrected solution without waiting times or null if none exists
     */
    public static VehicleRoutingProblemSolution correctSolution(MobilityMode mobilitymode, long stopTime, DirectionType directionType, VehicleRoutingProblemSolution solution, VehicleRoutingProblem.Builder vrpBuilder) {
        List<TourActivity> activities = ((List<VehicleRoute>)solution.getRoutes()).get(0).getActivities();
        VehicleRoutingProblem problem = vrpBuilder.build();
        Vehicle vehicle = vrpBuilder.getAddedVehicles().iterator().next();

        TourActivity firstActivity = activities.get(0);
        int index = firstActivity.getIndex();
        Job firstJob = null;
        for (Job job : problem.getJobsWithLocation()) {
            if (job.getIndex() == index) {
                firstJob = job;
                break;
            }
        }
        if(firstJob == null){
            throw new RuntimeException("No first job found for 'correctSolution()'!");
        }

        Collection<Job> jobs = new HashSet<>(vrpBuilder.getAddedJobs());
        jobs.remove(firstJob);

        vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
        vrpBuilder.addAllJobs(jobs);
        double referenceTime = mobilitymode.getName().equals("ridepooling") ? activities.get(activities.size() - 1).getEndTime() : ((List<VehicleRoute>) solution.getRoutes()).get(0).getEnd().getArrTime();
        referenceTime = (referenceTime - solution.getCost()) + firstActivity.getArrTime();
        if (referenceTime < ((Service) firstJob).getTimeWindow().getStart() || referenceTime > ((Service) firstJob).getTimeWindow().getEnd()) {
            return null;
        }
        vrpBuilder.addJob(Service.Builder.newInstance(firstJob.getId()).setLocation(firstActivity.getLocation()).setServiceTime(firstActivity.getOperationTime()).addTimeWindow(referenceTime, referenceTime).setPriority(1).build());
        vrpBuilder.setRoutingCost(new TransportCosts());
        vrpBuilder.setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);
        vrpBuilder.addVehicle(vehicle);

        problem = vrpBuilder.build();
        StateManager stateManager = new StateManager(problem);
        ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);
        constraintManager.addConstraint(new ActivityOrderConstraint(), ConstraintManager.Priority.CRITICAL);
        HardActivityConstraint constraint = mobilitymode.getName().equals("ridesharing") && directionType == DirectionType.DRIVEHOME ? new ActivityWaitConstraintOneAllowed() : new ActivityWaitConstraintNoneAllowed();
        constraintManager.addConstraint(constraint, ConstraintManager.Priority.CRITICAL);
        VehicleRoutingAlgorithm algorithm = Jsprit.Builder.newInstance(problem).setStateAndConstraintManager(stateManager, constraintManager).buildAlgorithm();
        Jsprit.createAlgorithm(problem);
        algorithm.setMaxIterations(50);
        if (mobilitymode.getName().equals("ridesharing") && directionType == DirectionType.DRIVEHOME) {
            int counter = 0;
            List<TourActivity> activities2 = ((List<VehicleRoute>) Solutions.bestOf(algorithm.searchSolutions()).getRoutes()).get(0).getActivities();
            int activityCounter = 0;
            if (Solutions.bestOf(algorithm.searchSolutions()).getUnassignedJobs().isEmpty()) {
                for (TourActivity a : activities2) {
                    long timeForStop = activityCounter == 0 ? 0 : stopTime;
                    if (activityCounter > 0 && a.getEndTime() - a.getArrTime() > timeForStop) {
                        counter++;
                    }
                    activityCounter++;
                }
            }
            if (counter > 0) {
                return null;
            }
        }
        return Solutions.bestOf(algorithm.searchSolutions());
    }


}
