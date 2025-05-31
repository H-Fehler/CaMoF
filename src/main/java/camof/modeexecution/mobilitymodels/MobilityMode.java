package camof.modeexecution.mobilitymodels;

import camof.modeexecution.DirectionType;
import com.graphhopper.GraphHopper;
import camof.modeexecution.Agent;
import camof.modeexecution.ModeExecutionManager;
import camof.modeexecution.groupings.Ride;
import org.springframework.lang.NonNull;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;


/**
 * An abstract class for defining mobility modes.
 */
public abstract class MobilityMode {

    GraphHopper graphHopper;
    boolean comparing = false;
    MobilityMode compareMode;
    List<Agent> agents;
    List<Agent> drivers;
    List<Agent> unwillingAgents;
    Map<Agent,Double> emissions;
    Map<Agent,Double> costs;
    Map<Agent,Double> kmTravelled;
    Map<Agent,Double> minutesTravelled;
    Map<Set<Object>,Double> oneWayEmissions;
    Map<Set<Object>,Double> oneWayCosts;
    Map<Set<Object>,Double> oneWayKmTravelled;
    Map<Set<Object>,Double> oneWayMinutesTravelled;
    double totalCosts;
    double totalEmissions;
    double totalKmTravelled;
    double totalMinutesTravelled;
    Map<Ride,Double> rideCosts;
    Map<Ride,Double> rideEmissions;
    Map<Ride,Double> rideKmTravelled;
    Map<Ride,Double> rideMinutesTravelled;
    Map<Agent,List<Ride>> agentToRides;
    List<Ride> rides;

    public MobilityMode(){
        this.graphHopper = ModeExecutionManager.graphHopper;
        drivers = new ArrayList<>();
        emissions = new HashMap<>();
        costs = new HashMap<>();
        kmTravelled = new HashMap<>();
        minutesTravelled = new HashMap<>();
        oneWayEmissions = new HashMap<>();
        oneWayCosts = new HashMap<>();
        oneWayKmTravelled = new HashMap<>();
        oneWayMinutesTravelled = new HashMap<>();
        agentToRides = new HashMap<>();
        rides = new ArrayList<>();
        unwillingAgents = new ArrayList<>();
    }

    /**
     * Executes necessary steps before the actual mode execution can start.
     *
     * @param agents the agents input into the mode
     */
    public abstract void prepareMode(List<Agent> agents);

    /**
     * Starts the actual execution of the mobility mode.
     */
    public abstract void startMode();

    public abstract String getName();

    /**
     * Checks if constraints of at least one agent were broken (return true if broken)
     */
    public abstract boolean checkIfConstraintsAreBroken(List<Agent> agents);

    public abstract Map<Agent, Double> getFinishedEmissions();

    public abstract Map<Agent, Double> getFinishedCosts();

    public abstract Map<Agent, Double> getFinishedKmTravelled();

    public abstract Map<Agent, Double> getFinishedMinutesTravelled();

    public abstract Map<Set<Object>, Double> getFinishedOneWayEmissions();

    public abstract Map<Set<Object>, Double> getFinishedOneWayCosts();

    public abstract Map<Set<Object>, Double> getFinishedOneWayKmTravelled();

    public abstract Map<Set<Object>, Double> getFinishedOneWayMinutesTravelled();

    public abstract Map<Agent, List<Ride>> getFinishedAgentToRides();

    public abstract List<Ride> getFinishedRides();
    public abstract List<Agent> getFinishedDrivers();
    public abstract double getFinishedTotalCosts();
    public abstract double getFinishedTotalEmissions();
    public abstract double getFinishedTotalKmTravelled();
    public abstract double getFinishedTotalMinutesTravelled();
    public abstract Map<Ride,Double> getFinishedRideCosts();
    public abstract Map<Ride,Double> getFinishedRideEmissions();
    public abstract Map<Ride,Double> getFinishedRideKmTravelled();
    public abstract Map<Ride,Double> getFinishedRideMinutesTravelled();

    public abstract void writeAdditionalResults(String resultsFolder);

    public Map<Agent, Double> getEmissions() {
        return this.emissions;
    }

    public Map<Agent, Double> getCosts() {
        return this.costs;
    }

    public Map<Agent, Double> getKmTravelled() {
        return this.kmTravelled;
    }

    public Map<Agent, Double> getMinutesTravelled() {
        return this.minutesTravelled;
    }

    public Map<Set<Object>, Double> getOneWayEmissions() {
        return this.oneWayEmissions;
    }

    public Map<Set<Object>, Double> getOneWayCosts() {
        return this.oneWayCosts;
    }

    public Map<Set<Object>, Double> getOneWayKmTravelled() {
        return this.oneWayKmTravelled;
    }

    public Map<Set<Object>, Double> getOneWayMinutesTravelled() {
        return this.oneWayMinutesTravelled;
    }

    public Map<Agent, List<Ride>> getAgentToRides() {
        return this.agentToRides;
    }

    public List<Ride> getRides() {
        return rides;
    }

    public boolean isComparing() {
        return this.comparing;
    }

    public void setComparing(boolean comparing) {
        this.comparing = comparing;
    }

    public MobilityMode getCompareMode() {
        return compareMode;
    }

    public void setCompareMode(MobilityMode compareMode) {
        this.compareMode = compareMode;
    }

    public GraphHopper getGraphHopper() {
        return graphHopper;
    }

    public void setGraphHopper(GraphHopper graphHopper) {
        this.graphHopper = graphHopper;
    }

    public List<Agent> getAgents() {
        return agents;
    }

    public void setAgents(List<Agent> agents) {
        this.agents = agents;
    }

    public List<Agent> getDrivers() {
        return drivers;
    }

    public void setDrivers(List<Agent> drivers) {
        this.drivers = drivers;
    }

    public void writeResults(List<Agent> agents, String modeName, String resultsFolder){
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

        List<String> dataLines = new ArrayList<>();

        List<String> dataLines3 = new ArrayList<>();
        dataLines.add("Agent Id,ToRide Id,BackRide Id,willing,wasDriver,droveAloneToUni,droveAloneBack,GesamtKilometer,GesamtMinuten,GesamtCO2,GesamtKosten,HinKilometer,HinMinuten,HinCO2,HinKosten,RueckKilometer,RueckMinuten,RueckCO2,RueckKosten");
        dataLines3.add("Ride Id,travelledKilometers,travelledMinutes,costs,emissions,AgentCount,unwillingRide,StopCount,StartTime,EndTime,StartPosition-Latitude,StartPosition-Longitude,EndPosition-Latitude,EndPosition-Longitude");
        if(getFinishedRides()!=null){
            for(Ride ride : getFinishedRides()){
                dataLines3.add(ride.getId() + "," + (getFinishedRideKmTravelled()!=null&&getFinishedRideKmTravelled().containsKey(ride)?getFinishedRideKmTravelled().get(ride):"")+ "," + (getFinishedRideMinutesTravelled()!=null&&getFinishedRideMinutesTravelled().containsKey(ride)?getFinishedRideMinutesTravelled().get(ride):"") + "," + (getFinishedRideCosts()!=null&&getFinishedRideCosts().containsKey(ride)?getFinishedRideCosts().get(ride):"") + "," + (getFinishedRideEmissions()!=null&&getFinishedRideEmissions().containsKey(ride)?getFinishedRideEmissions().get(ride):"") + "," + ride.getAgents().size() + "," + (ride.getDriver() != null && !ride.getDriver().isWillingToUseAlternatives()) + "," + ride.getExtraStops().size() + "," + ride.getStartTime() + "," + ride.getEndTime() + "," + ride.getStartPosition().getLatitude() + "," + ride.getStartPosition().getLongitude() + "," + ride.getEndPosition().getLatitude() + "," + ride.getEndPosition().getLongitude());
                averageSeatCount += ride.getAgents().size();
                if((ride.getDriver() != null && !ride.getDriver().isWillingToUseAlternatives())){
                    averageSeatCountForWilling += ride.getAgents().size();
                    countOfWilling++;
                }
                if(ride.getAgents().size()>1){
                    averageSeatCountForGroups += ride.getAgents().size();
                    countOfGroups++;
                }
                if(ride.getTypeOfGrouping()== DirectionType.DRIVETOUNI){
                    if((ride.getDriver() != null && !ride.getDriver().isWillingToUseAlternatives())){
                        averageSeatCountToUniForWilling += ride.getAgents().size();
                        countOfWillingToUni++;
                    }
                    if(ride.getAgents().size()>1){
                        averageSeatCountToUniForGroups += ride.getAgents().size();
                        countOfGroupsToUni++;
                    }
                    averageSeatCountToUni += ride.getAgents().size();
                    countOfToUni++;
                }else{
                    if((ride.getDriver() != null && !ride.getDriver().isWillingToUseAlternatives())){
                        averageSeatCountHomeForWilling += ride.getAgents().size();
                        countOfWillingHome++;
                    }
                    if(ride.getAgents().size()>1){
                        averageSeatCountHomeForGroups += ride.getAgents().size();
                        countOfGroupsHome++;
                    }
                    averageSeatCountHome += ride.getAgents().size();
                    countOfHome++;
                }
            }
        }

        averageSeatCount = getFinishedRides()!=null?averageSeatCount/getFinishedRides().size():0;
        averageSeatCountToUni = averageSeatCountToUni/countOfToUni;
        averageSeatCountHome = averageSeatCountHome/countOfHome;
        averageSeatCountForWilling = averageSeatCountForWilling/countOfWilling;
        averageSeatCountToUniForWilling = averageSeatCountToUniForWilling/countOfWillingToUni;
        averageSeatCountHomeForWilling = averageSeatCountHomeForWilling/countOfWillingHome;
        averageSeatCountForGroups = averageSeatCountForGroups/countOfGroups;
        averageSeatCountToUniForGroups = averageSeatCountToUniForGroups/countOfGroupsToUni;
        averageSeatCountHomeForGroups = averageSeatCountHomeForGroups/countOfGroupsHome;

        List<String> dataLines2 = new ArrayList<>();
        dataLines2.add("totalKilometers,totalMinutes,totalCosts,totalEmissions,averageSeatCount,averageSeatCountToUni,averageSeatCountHome,count of driving students,count of agents,count of rides,count of rides to uni,count of rides home,averageSeatCountForWilling,countOfWilling,averageSeatCountToUniForWilling,countOfWillingToUni,averageSeatCountHomeForWilling,countOfWillingHome,averageSeatCountForGroups,countOfGroups,averageSeatCountToUniForGroups,countOfGroupsToUni,averageSeatCountHomeForGroups,countOfGroupsHome");
        dataLines2.add(getFinishedTotalKmTravelled() +","+ getFinishedTotalMinutesTravelled() +","+ getFinishedTotalCosts() +","+ getFinishedTotalEmissions() +","+ averageSeatCount + "," + averageSeatCountToUni + "," + averageSeatCountHome + "," + (getFinishedDrivers()!=null ? getFinishedDrivers().size() : "") + "," + (agents!=null? agents.size():"") + "," + (getFinishedRides()!=null?getFinishedRides().size():"") + "," + countOfToUni + "," + countOfHome + "," + averageSeatCountForWilling + "," + countOfWilling + "," + averageSeatCountToUniForWilling + "," + countOfWillingToUni + "," + averageSeatCountHomeForWilling + "," + countOfWillingHome + "," + averageSeatCountForGroups + "," + countOfGroups + "," + averageSeatCountToUniForGroups + "," + countOfGroupsToUni + "," + averageSeatCountHomeForGroups + "," + countOfGroupsHome);

        if(agents!=null && getFinishedAgentToRides()!=null){
            for(Agent a : agents) {
                if(getFinishedAgentToRides().containsKey(a) && !getFinishedAgentToRides().get(a).isEmpty()){
                    Ride toUni = getFinishedAgentToRides().get(a).get(0);
                    Set<Object> set = Set.of(a,toUni);
                    double km;
                    double min;
                    double co2;
                    double cost;
                    Ride home = null;
                    if (getFinishedAgentToRides().get(a).size() > 1) {
                        home = getFinishedAgentToRides().get(a).get(1);
                        Set<Object> set2 = Set.of(a,home);
                        km = getFinishedOneWayKmTravelled().get(set2);
                        min = getFinishedOneWayMinutesTravelled().get(set2);
                        co2 = getFinishedOneWayEmissions().get(set2);
                        cost = getFinishedOneWayCosts().get(set2);
                    } else {
                        km = 0.0;
                        min = 0.0;
                        co2 = 0.0;
                        cost = 0.0;
                    }
                    dataLines.add(a.getId() + "," + (getFinishedAgentToRides() != null && getFinishedAgentToRides().containsKey(a) && !getFinishedAgentToRides().get(a).isEmpty() ? getFinishedAgentToRides().get(a).get(0).getId() + "," + (getFinishedAgentToRides().get(a).size() > 1 ? getFinishedAgentToRides().get(a).get(1).getId() : -1) : ",") + "," + a.isWillingToUseAlternatives() + "," + (toUni.getDriver()!=null && toUni.getDriver().equals(a)) + "," + (toUni.getAgents().size() == 1) + "," + (home != null && home.getAgents().size() == 1) + "," + getFinishedKmTravelled().get(a) + "," + getFinishedMinutesTravelled().get(a) + "," + getFinishedEmissions().get(a) + "," + getFinishedCosts().get(a) + "," + getFinishedOneWayKmTravelled().get(set) + "," + getFinishedOneWayMinutesTravelled().get(set) + "," + getFinishedOneWayEmissions().get(set) + "," + getFinishedOneWayCosts().get(set) + "," + km + "," + min + "," + co2 + "," + cost);
                }
            }
        }

        File csvOutputFile = new File(resultsFolder+"\\"+modeName+"AgentResults.csv");
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            for(String data : dataLines){
                pw.println(data);
            }
            pw.close();
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
        String newResultFileName = resultsFolder+"\\"+modeName+"SummedResults.csv";
        File csvOutputFile2 = new File(newResultFileName);
        try (PrintWriter pw = new PrintWriter(csvOutputFile2)) {
            for(String data : dataLines2){
                pw.println(data);
            }
            pw.close();
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }

        newResultFileName = resultsFolder+"\\"+modeName + "RideResults.csv";
        File csvOutputFile3 = new File(newResultFileName);
        try (PrintWriter pw = new PrintWriter(csvOutputFile3)) {
            for(String data : dataLines3){
                pw.println(data);
            }
            pw.close();
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }
}
