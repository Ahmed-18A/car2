package com.example.car2;

import java.util.ArrayList;

public class Car {

    // ===== BASIC =====
    private String type;
    private String price;

    // ===== DISPLAY =====
    private ArrayList<String> images;
    private ArrayList<String> details;

    // ===== FILTERABLE FIELDS =====
    private String region;
    private String gearType;
    private String fuelType;
    private String color;

    private String doors;
    private String seats;

    private String sunroof;
    private String disabledCar;

    private String testDate;
    private String year;
    private String horsePower;
    private String engineCapacity;

    // ===== REQUIRED EMPTY CONSTRUCTOR =====
    public Car() { }

    // ===== FULL CONSTRUCTOR =====
    public Car(String type, String price,
               ArrayList<String> images, ArrayList<String> details,
               String region, String gearType, String fuelType, String color,
               String doors, String seats,
               String sunroof, String disabledCar,
               String testDate, String year,
               String horsePower, String engineCapacity) {

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

    // ===== GETTERS & SETTERS =====
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

    public String getDoors() { return doors; }
    public void setDoors(String doors) { this.doors = doors; }

    public String getSeats() { return seats; }
    public void setSeats(String seats) { this.seats = seats; }

    public String getTestDate() { return testDate; }
    public void setTestDate(String testDate) { this.testDate = testDate; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getHorsePower() { return horsePower; }
    public void setHorsePower(String horsePower) { this.horsePower = horsePower; }

    public String getEngineCapacity() { return engineCapacity; }
    public void setEngineCapacity(String engineCapacity) { this.engineCapacity = engineCapacity; }

    // ===== CHECK BOOLEAN FIELDS =====
    public String getDisabledCar() {
        return disabledCar;
    }
    public void setDisabledCar(String disabledCar) {
        this.disabledCar = disabledCar;
    }


    public String getSunroof() {
        return sunroof;
    }
    public void setSunroof(String sunroof) {
        this.sunroof = sunroof;
    }

    public boolean hasSunroof() {
        return "true".equalsIgnoreCase(sunroof);
    }

    public boolean isDisabledCarBool() {
        return "true".equalsIgnoreCase(disabledCar);
    }



}
