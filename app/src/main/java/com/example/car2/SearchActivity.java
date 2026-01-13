package com.example.car2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.RangeSlider;

public class SearchActivity extends AppCompatActivity {

    ImageButton btnBack;
    Spinner spRegion, spCarType, spGearType, spFuelType, spColor, spDoors, spSeats;
    EditText etYear, etHorsePower, etEngineCapacity;
    CheckBox cbSunroof, cbDisabled;
    RangeSlider sliderPrice;
    Button btnApplyFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        btnBack=findViewById(R.id.ImageButton);

        // Spinners
        spRegion = findViewById(R.id.spLocation);
        spCarType = findViewById(R.id.spCarType);
        spGearType = findViewById(R.id.spGearType);
        spFuelType = findViewById(R.id.spFuelType);
        spColor = findViewById(R.id.spColor);
        spDoors = findViewById(R.id.spDoors);
        spSeats = findViewById(R.id.spSeats);
        // EditTexts
        etYear = findViewById(R.id.etYear);
        etHorsePower = findViewById(R.id.etHorsePower);
        etEngineCapacity = findViewById(R.id.etEngineCapacity);

        // CheckBoxes
        cbSunroof = findViewById(R.id.cbSunroof);
        cbDisabled = findViewById(R.id.cbDisabled);

        // RangeSlider
        sliderPrice = findViewById(R.id.sliderPrice);

        // Button
        btnApplyFilter = findViewById(R.id.btnSearch);

        btnApplyFilter.setOnClickListener(v -> applyFilter());

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SearchActivity.this, dashboard.class));
                finish();
            }
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

        String year = etYear.getText().toString();
        String horsePower = etHorsePower.getText().toString();
        String engineCapacity = etEngineCapacity.getText().toString();

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

        // فقط إذا EditText فيها قيمة محددة نرسلها، إذا فاضية بنفع كل القيم
        if (!TextUtils.isEmpty(year)) intent.putExtra("year", year);
        if (!TextUtils.isEmpty(horsePower)) intent.putExtra("horsePower", horsePower);
        if (!TextUtils.isEmpty(engineCapacity)) intent.putExtra("engineCapacity", engineCapacity);

        setResult(RESULT_OK, intent);
        finish();
    }
}
