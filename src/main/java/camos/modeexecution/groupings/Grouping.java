package camos.modeexecution.groupings;

import camos.modeexecution.Agent;
import camos.modeexecution.Coordinate;
import camos.modeexecution.DirectionType;
import camos.modeexecution.carmodels.Vehicle;

import java.util.ArrayList;
import java.util.List;

public abstract class Grouping {

    List<Agent> agents;
    Vehicle vehicle;
    Agent driver;
    int groupNumber;
    Coordinate startPosition;
    Coordinate endPosition;
    DirectionType typeOfGrouping;

    public Grouping(){
        agents = new ArrayList<>();
    }

    public List<Agent> getAgents() {
        return agents;
    }

    public void setAgents(List<Agent> agents) {
        this.agents = agents;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Agent getDriver() {
        return driver;
    }

    public void setDriver(Agent driver) {
        this.driver = driver;
    }

    public int getGroupNumber() {
        return groupNumber;
    }

    public void setGroupNumber(int groupNumber) {
        this.groupNumber = groupNumber;
    }

    public Coordinate getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(Coordinate startPosition) {
        this.startPosition = startPosition;
    }

    public Coordinate getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(Coordinate endPosition) {
        this.endPosition = endPosition;
    }

    public DirectionType getTypeOfGrouping() {
        return typeOfGrouping;
    }

    public void setTypeOfGrouping(DirectionType typeOfGrouping) {
        this.typeOfGrouping = typeOfGrouping;
    }
}
