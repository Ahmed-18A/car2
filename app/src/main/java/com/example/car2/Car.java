package com.example.car2;

import java.util.ArrayList;

public class Car {
    private String type;
    private String price;
    private ArrayList<String> images;
    private ArrayList<String> details;

    // ===== الخصائص الجديدة =====
    private String region;
    private String gearType;
    private String fuelType;
    private String color;
    private int doors;
    private int seats;
    private boolean sunroof;
    private boolean disabledCar;
    private String testDate;
    private int year;
    private int horsePower;
    private int engineCapacity;

    public Car() { }

    public Car(String type, String price, ArrayList<String> images, ArrayList<String> details,
               String region, String gearType, String fuelType, String color,
               int doors, int seats, boolean sunroof, boolean disabledCar,
               String testDate, int year, int horsePower, int engineCapacity) {

        this.type = type;
        this.price = price;
        this.images = images;
        this.details = details;
        this.region = region;
        this.gearType = gearType;
        this.fuelType = fuelType;
        this.color = color;
        this.doors = doors;
        this.seats = seats;
        this.sunroof = sunroof;
        this.disabledCar = disabledCar;
        this.testDate = testDate;
        this.year = year;
        this.horsePower = horsePower;
        this.engineCapacity = engineCapacity;
    }

    // ===== getters & setters =====
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public ArrayList<String> getImages() { return images; }
    public void setImages(ArrayList<String> images) { this.images = images; }

    public ArrayList<String> getDetails() { return details; }
    public void setDetails(ArrayList<String> details) { this.details = details; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getGearType() { return gearType; }
    public void setGearType(String gearType) { this.gearType = gearType; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public int getDoors() { return doors; }
    public void setDoors(int doors) { this.doors = doors; }

    public int getSeats() { return seats; }
    public void setSeats(int seats) { this.seats = seats; }

    public boolean hasSunroof() { return sunroof; }
    public void setSunroof(boolean sunroof) { this.sunroof = sunroof; }

    public boolean isDisabledCar() { return disabledCar; }
    public void setDisabledCar(boolean disabledCar) { this.disabledCar = disabledCar; }

    public String getTestDate() { return testDate; }
    public void setTestDate(String testDate) { this.testDate = testDate; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getHorsePower() { return horsePower; }
    public void setHorsePower(int horsePower) { this.horsePower = horsePower; }

    public int getEngineCapacity() { return engineCapacity; }
    public void setEngineCapacity(int engineCapacity) { this.engineCapacity = engineCapacity; }
}
