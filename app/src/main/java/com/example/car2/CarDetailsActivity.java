package com.example.car2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class CarDetailsActivity extends AppCompatActivity {

    ViewPager2 viewPagerImages;
    TextView txtType, txtPrice;
    TableLayout tableDetails;
    ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_details);

        viewPagerImages = findViewById(R.id.viewPagerImages);
        txtType = findViewById(R.id.txtType);
        txtPrice = findViewById(R.id.txtPrice);
        tableDetails = findViewById(R.id.tableDetails);
        btnBack=findViewById(R.id.ImageButton);

        Car car = (Car) getIntent().getSerializableExtra("car");

        txtType.setText(car.getType() != null ? car.getType() : "");
        txtPrice.setText(car.getPrice() != null ? car.getPrice() : "");

        // ===== ViewPager للصور =====
        if (car.getImages() != null && !car.getImages().isEmpty()) {
            ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(this, car.getImages());
            viewPagerImages.setAdapter(sliderAdapter);
        }

        // ===== تعبئة جدول التفاصيل =====
        String[] labels = {
                "Location", "Gear Type", "Fuel Type", "Color",
                "Doors", "Seats","Test Date", "Year", "Horsepower",
                "Engine Capacity", "Sunroof", "Disabled Accessible"
        };

        if (car.getDetails() != null) {
            for (int i = 0; i < labels.length && i < car.getDetails().size(); i++) {
                TableRow row = new TableRow(this);

                TextView label = new TextView(this);
                label.setText(labels[i]);
                label.setPadding(16,16,16,16);

                TextView value = new TextView(this);
                value.setText(car.getDetails().get(i));
                value.setPadding(16,16,16,16);

                row.addView(label);
                row.addView(value);

                tableDetails.addView(row);
            }
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CarDetailsActivity.this, dashboard.class));
                finish();
            }
        });
    }

}
