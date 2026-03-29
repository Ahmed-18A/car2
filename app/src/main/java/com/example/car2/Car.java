package com.example.car2;

import java.io.Serializable;
import java.util.ArrayList;

public class Car implements Serializable {

    private String type;
    private String model;
    private String trim;
    private String fullType;

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

    private String ownerId;

    private ArrayList<String> images;

    public Car() { }

    public Car(String type, String model, String trim, String fullType,
               String price, ArrayList<String> images,
               String location, String gearType, String fuelType, String color,
               String doors, String seats, String sunroof, String disabledCar,
               String testDate, String year, String horsePower,
               String engineCapacity, String ownerId) {

        this.type = type;
        this.model = model;
        this.trim = trim;
        this.fullType = fullType;
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
        this.ownerId = ownerId;
    }

    public String getType() { return type; }

    public String getModel() { return model; }

    public String getTrim() { return trim; }

    public String getFullType() { return fullType; }

    public String getPrice() { return price; }

    public ArrayList<String> getImages() { return images; }

    public String getId() { return id; }

    public void setId(String id) {
        this.id = id;
    }

    public String getLocation() { return location; }

    public String getGearType() { return gearType; }

    public String getFuelType() { return fuelType; }

    public String getColor() { return color; }

    public String getDoors() { return doors; }

    public String getSeats() { return seats; }

    public String getTestDate() { return testDate; }

    public String getYear() { return year; }

    public String getHorsePower() { return horsePower; }

    public String getEngineCapacity() { return engineCapacity; }

    public String getSunroof() { return sunroof; }

    public String getDisabledCar() { return disabledCar; }

    public String getOwnerId() { return ownerId; }


    public ArrayList<String> getDetails() {
        ArrayList<String> list = new ArrayList<>();
        list.add(model);
        list.add(trim);
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
}