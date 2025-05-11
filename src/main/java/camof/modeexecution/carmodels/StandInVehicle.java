package camof.modeexecution.carmodels;

import camof.GeneralManager;

public class StandInVehicle extends Vehicle{

    public StandInVehicle(double co2EmissionPerLiter, double pricePerLiter, double consumptionPerKm) {
        this.withDriver = true;
        this.seatCount = 4;
        this.co2EmissionPerLiter = co2EmissionPerLiter;
        this.pricePerLiter = pricePerLiter; // studentCarPricePerLiter*2;
        this.consumptionPerKm = consumptionPerKm;
        idCounter++;
        this.id = idCounter;
    }

}
