package com.example.car2;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class Car implements Serializable {
    private String type;
    private String price;
    private String[] images = new String[5];
    private String[] details = new String[12];
    /*
    1-> المنطقة
    2-> نوع الغير
    3-> نوع الوقود
    4-> اللون
    5-> موعد التيست
    6-> عدد الابواب
    7-> عدد المقاعد
    8-> فتحة السقف
    9-> مخصصة للمقعدين
    10-> سنة الصعود الى الشارع
    11-> قوة الاحصنة
    12-> سعة المحرك
     */

    public Car() {}

    public Car( String type, String price, String[] details , String[] images) {
        this.type = type;
        this.price = price;
        this.details= Arrays.copyOf(details,details.length);
        this.images= Arrays.copyOf(images,images.length);
    }
    public String getType() { return type; }
    public String getPrice() { return price; }
    public String[] getImages() { return images; }
    public String[] getDetails() { return details; }

    // 1 -> المنطقة
    public String getRegion() {
        return details[0];
    }

    // 2 -> نوع الغير
    public String getGearType() {
        return details[1];
    }

    // 3 -> نوع الوقود
    public String getFuelType() {
        return details[2];
    }

    // 4 -> اللون
    public String getColor() {
        return details[3];
    }

    // 5 -> موعد التيست
    public String getTestDate() {
        return details[4];
    }

    // 6 -> عدد الابواب
    public int getDoors() {
        return Integer.parseInt(details[5]);
    }

    // 7 -> عدد المقاعد
    public int getSeats() {
        return Integer.parseInt(details[6]);
    }

    // 8 -> فتحة السقف
    public boolean hasSunroof() {
        return details[7].equalsIgnoreCase("true");
    }

    // 9 -> مخصصة للمعاقين
    public boolean isDisabledCar() {
        return details[8].equalsIgnoreCase("true");
    }

    // 10 -> سنة الصعود الى الشارع
    public int getYear() {
        return Integer.parseInt(details[9]);
    }

    // 11 -> قوة الاحصنة
    public int getHorsePower() {
        return Integer.parseInt(details[10]);
    }

    // 12 -> سعة المحرك
    public int getEngineCapacity() {
        return Integer.parseInt(details[11]);
    }

}
