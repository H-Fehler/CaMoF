package camos.modeexecution.carmodels;

import camos.GeneralManager;

public class MiniBus extends Vehicle{

    public MiniBus(int seatCount, double co2EmissionPerLiter, double pricePerLiter, double consumptionPerKm) {
        this.seatCount = seatCount;
        this.withDriver = false;
        this.co2EmissionPerLiter = co2EmissionPerLiter;
        this.pricePerLiter = pricePerLiter;
        this.consumptionPerKm = consumptionPerKm;
        idCounter++;
        id = idCounter;
    }

}
