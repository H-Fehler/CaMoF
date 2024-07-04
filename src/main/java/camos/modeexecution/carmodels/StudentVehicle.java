package camos.modeexecution.carmodels;

import camos.GeneralManager;

public class StudentVehicle extends Vehicle {


    public StudentVehicle(int seatCount, double co2EmissionPerLiter, double pricePerLiter, double consumptionPerKm) {
        this.seatCount = seatCount;
        this.withDriver = true;
        this.co2EmissionPerLiter = co2EmissionPerLiter;
        this.pricePerLiter = pricePerLiter;
        this.consumptionPerKm = consumptionPerKm;
        idCounter++;
        this.id = idCounter;
    }

}
