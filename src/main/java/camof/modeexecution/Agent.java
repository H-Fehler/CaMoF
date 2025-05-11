package camof.modeexecution;

import camof.GeneralManager;
import camof.modeexecution.carmodels.Vehicle;

import java.util.Objects;

public class Agent {

    long id;
    boolean willingToUseAlternatives;
    Coordinate homePosition;
    Request request;
    Vehicle car;


    public Agent(long id, Coordinate homePosition, Vehicle car, Request request) {
        this.id = id;
        this.homePosition = homePosition;
        this.car = car;
        this.request = request;
        this.willingToUseAlternatives = true;
    }

    public Agent(long id, Coordinate homePosition, Vehicle car, Request request, boolean willingToUseAlternatives) {
        this.id = id;
        this.homePosition = homePosition;
        this.car = car;
        this.request = request;
        this.willingToUseAlternatives = willingToUseAlternatives;
    }


    public Agent(){

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Coordinate getHomePosition() {
        return homePosition;
    }

    public void setHomePosition(Coordinate homePosition) {
        this.homePosition = homePosition;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public Vehicle getCar() {
        return car;
    }

    public void setCar(Vehicle car) {
        this.car = car;
    }

    public boolean isWillingToUseAlternatives() {
        return willingToUseAlternatives;
    }

    public void setWillingToUseAlternatives(boolean willingToUseAlternatives) {
        this.willingToUseAlternatives = willingToUseAlternatives;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !Agent.class.isAssignableFrom(o.getClass())) return false;
        Agent agent = (Agent) o;
        return id == agent.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
