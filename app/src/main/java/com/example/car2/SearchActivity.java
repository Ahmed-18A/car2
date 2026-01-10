package com.example.car2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.slider.RangeSlider;

import java.util.ArrayList;

public class SearchActivity extends AppCompatActivity {

    Spinner spRegion, spCarType, spGearType, spFuelType, spColor, spDoors, spSeats;
    CheckBox cbSunroof, cbDisabled;
    RangeSlider sliderPrice;
    Button btnApplyFilter;
    BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        bottomNav = findViewById(R.id.bottom_navigation);

        // Spinners
        spRegion = findViewById(R.id.spRegion);
        spCarType = findViewById(R.id.spCarType);
        spGearType = findViewById(R.id.spGearType);
        spFuelType = findViewById(R.id.spFuelType);
        spColor = findViewById(R.id.spColor);
        spDoors = findViewById(R.id.spDoors);
        spSeats = findViewById(R.id.spSeats);

        // CheckBoxes
        cbSunroof = findViewById(R.id.cbSunroof);
        cbDisabled = findViewById(R.id.cbDisabled);

        // Price Slider
        sliderPrice = findViewById(R.id.sliderPrice);

        // Button
        btnApplyFilter = findViewById(R.id.btnApplyFilter);

        btnApplyFilter.setOnClickListener(v -> applyFilter());

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.mnu_profile) {
                startActivity(new Intent(SearchActivity.this, profile.class));
                finish();
            }
            if(item.getItemId() == R.id.mnu_dash) {
                startActivity(new Intent(SearchActivity.this, dashboard.class));
                finish();
            }
            if(item.getItemId() == R.id.mnu_add) {
                startActivity(new Intent(SearchActivity.this, addCar.class));
                finish();
            }
            if(item.getItemId() == R.id.mnu_search) {

            }
            return true;
        });
    }

    private void applyFilter() {

        String region = spRegion.getSelectedItem().toString();
        String carType = spCarType.getSelectedItem().toString();
        String gearType = spGearType.getSelectedItem().toString();
        String fuelType = spFuelType.getSelectedItem().toString();
        String color = spColor.getSelectedItem().toString();
        String doors = spDoors.getSelectedItem().toString();
        String seats = spSeats.getSelectedItem().toString();

        boolean sunroof = cbSunroof.isChecked();
        boolean disabledAccessible = cbDisabled.isChecked();

        float minPrice = sliderPrice.getValues().get(0);
        float maxPrice = sliderPrice.getValues().get(1);

        Intent intent = new Intent();
        intent.putExtra("region", region);
        intent.putExtra("carType", carType);
        intent.putExtra("gearType", gearType);
        intent.putExtra("fuelType", fuelType);
        intent.putExtra("color", color);
        intent.putExtra("doors", doors);
        intent.putExtra("seats", seats);
        intent.putExtra("sunroof", sunroof);
        intent.putExtra("disabled", disabledAccessible);
        intent.putExtra("minPrice", minPrice);
        intent.putExtra("maxPrice", maxPrice);

        setResult(RESULT_OK, intent);
        finish();
    }
}