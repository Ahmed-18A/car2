package com.example.car2;

import java.io.Serializable;
import java.util.ArrayList;

public class Car implements Serializable {

    private String type;
    private String price;
    private String location;
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
    private String id;
    private String userId;
    private ArrayList<String> images;

    public Car() { }

    public Car(String type, String price, ArrayList<String> images,
               String location, String gearType, String fuelType, String color,
               String doors, String seats, String sunroof, String disabledCar,
               String testDate, String year, String horsePower, String engineCapacity,
               String userId) {

        this.type = type;
        this.price = price;
        this.images = images;
        this.location = location;
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
        this.userId = userId;
    }

    public String getType() { return type; }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public ArrayList<String> getImages() { return images; }
    public void setImages(ArrayList<String> images) { this.images = images; }

    public ArrayList<String> getDetails() {
        ArrayList<String> list = new ArrayList<>();
        list.add(location);
        list.add(gearType);
        list.add(fuelType);
        list.add(color);
        list.add(doors);
        list.add(seats);
        list.add(testDate);
        list.add(year);
        list.add(horsePower);
        list.add(engineCapacity);
        list.add(sunroof);
        list.add(disabledCar);
        return list;
    }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

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
        return "Yes".equalsIgnoreCase(sunroof);
    }

    public boolean isDisabledCarBool() {
        return "Yes".equalsIgnoreCase(disabledCar);
    }

}
