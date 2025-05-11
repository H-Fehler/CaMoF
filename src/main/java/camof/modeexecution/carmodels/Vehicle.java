package camof.modeexecution.carmodels;

public abstract class Vehicle {

    static long idCounter = 0;

    long id;

    int seatCount = 5;

    boolean withDriver = true;

    double consumptionPerKm;
    double co2EmissionPerLiter;
    double pricePerLiter;


    public int getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(int seatCount) {
        this.seatCount = seatCount;
    }

    public boolean isWithDriver() {
        return withDriver;
    }

    public void setWithDriver(boolean withDriver) {
        this.withDriver = withDriver;
    }

    public double getConsumptionPerKm() {
        return consumptionPerKm;
    }

    public void setConsumptionPerKm(double consumptionPerKm) {
        this.consumptionPerKm = consumptionPerKm;
    }

    public double getCo2EmissionPerLiter() {
        return co2EmissionPerLiter;
    }

    public void setCo2EmissionPerLiter(double co2EmissionPerLiter) {
        this.co2EmissionPerLiter = co2EmissionPerLiter;
    }

    public double getPricePerLiter() {
        return pricePerLiter;
    }

    public void setPricePerLiter(double pricePerLiter) {
        this.pricePerLiter = pricePerLiter;
    }

    public static long getIdCounter() {
        return idCounter;
    }

    public static void setIdCounter(long idCounter) {
        MiniBus.idCounter = idCounter;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
