package camos.modeexecution.mobilitymodels.tsphelpers;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.ResponsePath;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.cost.AbstractForwardVehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.driver.Driver;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.util.shapes.GHPoint;
import camos.modeexecution.ModeExecutionManager;

import java.util.*;

public class TransportCosts extends AbstractForwardVehicleRoutingTransportCosts {

    @Override
    public double getDistance(Location from, Location to, double departureTime, Vehicle vehicle) {
        List<Location> list = new ArrayList<>();
        list.add(from);
        list.add(to);
        if(ModeExecutionManager.timeMap.containsKey(list)){
            return ModeExecutionManager.timeMap.get(list);
        }else{
            GHPoint ghPointStart = new GHPoint(from.getCoordinate().getY(),from.getCoordinate().getX());
            GHPoint ghPointEnd = new GHPoint(to.getCoordinate().getY(),to.getCoordinate().getX());
            GHRequest ghRequest = new GHRequest(ghPointStart,ghPointEnd).setProfile("car").setLocale(Locale.GERMANY);

            GHResponse rsp = ModeExecutionManager.graphHopper.route(ghRequest);
            ResponsePath path = rsp.getBest();
            long time = path.getTime()/60000L;
            ModeExecutionManager.timeMap.put(list,time);
            ModeExecutionManager.distanceMap.put(list,path.getDistance());
            return path.getDistance();
        }
    }

    @Override
    public double getTransportTime(Location from, Location to, double departureTime, Driver driver, Vehicle vehicle) {
        List<Location> list = new ArrayList<>();
        list.add(from);
        list.add(to);
        if(ModeExecutionManager.timeMap.containsKey(list)){
            return ModeExecutionManager.timeMap.get(list);
        }else{
            GHPoint ghPointStart = new GHPoint(from.getCoordinate().getY(),from.getCoordinate().getX());
            GHPoint ghPointEnd = new GHPoint(to.getCoordinate().getY(),to.getCoordinate().getX());
            GHRequest ghRequest = new GHRequest(ghPointStart,ghPointEnd).setProfile("car").setLocale(Locale.GERMANY);

            GHResponse rsp = ModeExecutionManager.graphHopper.route(ghRequest);
            ResponsePath path = rsp.getBest();
            long time = path.getTime()/60000L;
            ModeExecutionManager.timeMap.put(list,time);
            ModeExecutionManager.distanceMap.put(list,path.getDistance());
            return time;
        }
    }

    @Override
    public double getTransportCost(Location from, Location to, double departureTime, Driver driver, Vehicle vehicle) {
        return getTransportTime(from,to,departureTime,driver,vehicle);
    }
}
