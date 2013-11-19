/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package entities.cost;

import Utils.LinearCoordCalculator;
import entities.Coord2D;

/**
 * Represents a simple proof-of-concept cost model.
 * @author pla
 */
public class SimpleCostModel implements ICostModel
{
    private int numOfPassengers;
    private int aircraftCapacity;
    private Coord2D currentAircraftPosition;
    private Coord2D departureAirportPosition;
    private Coord2D destinationAirportPosition;
    private double aircraftSpeed;
    private double fuelBurnRate;
    
    private final double NUM_OF_PASSENGERS_FACTOR = -1;
    private final double NUM_OF_EMPTY_SEATS_FACTOR = 2;
    private final double DIST_TO_DEPARTURE_AIRPORT_FACTOR = 11;
    private final double TOTAL_EET_FACTOR = 12;
    private final double FUEL_CONSUMPTION_FACTOR = 9.8;
    
    public SimpleCostModel(int numOfPassengers, int acCapacity, Coord2D currentAcPos, Coord2D depPos, Coord2D destPos, double acSpeed, double fuelBurnRate)
    {
        this.numOfPassengers = numOfPassengers;
        this.aircraftCapacity = acCapacity;
        this.aircraftSpeed = acSpeed;
        this.fuelBurnRate = fuelBurnRate;
        this.departureAirportPosition = depPos;
        this.currentAircraftPosition = currentAcPos;
        this.destinationAirportPosition = destPos;
    }
    
    /**
     * Calculates the cost of using this aircraft, based on the number of passengers,
     * empty seats, distance to the departure airport, estimated time of flight and fuel consumption.
     * @return The cost of using this aircraft.
     */
    @Override
    public double calculateCost()
    {
        int numOfEmptySeats = aircraftCapacity - numOfPassengers;
        double distToDepAirport = LinearCoordCalculator.INSTANCE.calculateDistance(currentAircraftPosition, departureAirportPosition);
        double distToDestAirport = LinearCoordCalculator.INSTANCE.calculateDistance(departureAirportPosition, destinationAirportPosition);
        double totalEET = distToDepAirport/aircraftSpeed;
        double fuelConsumption = fuelBurnRate * (distToDepAirport + distToDestAirport);
        
        double weightedNumOfPassengers = numOfPassengers * NUM_OF_PASSENGERS_FACTOR;
        double weightedNumOfEmptySeats = numOfEmptySeats * NUM_OF_EMPTY_SEATS_FACTOR;
        double weightedDistToDepAirport = distToDepAirport * DIST_TO_DEPARTURE_AIRPORT_FACTOR;
        double weightedTotalEET = totalEET * TOTAL_EET_FACTOR;
        double weightedFuelConsumption = fuelConsumption * FUEL_CONSUMPTION_FACTOR;
        
        return weightedNumOfPassengers + weightedNumOfEmptySeats + weightedDistToDepAirport + weightedTotalEET + weightedFuelConsumption;
    }
}
