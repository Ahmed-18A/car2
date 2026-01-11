package com.example.car2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;

public class CarDetailsActivity extends AppCompatActivity {

    ViewPager2 viewPagerImages;
    TextView txtType, txtPrice;
    TableLayout tableDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_details);

        viewPagerImages = findViewById(R.id.viewPagerImages);
        txtType = findViewById(R.id.txtType);
        txtPrice = findViewById(R.id.txtPrice);
        tableDetails = findViewById(R.id.tableDetails);

        // ===== جلب البيانات من Intent =====
        Intent intent = getIntent();
        String type = intent.getStringExtra("type");
        String price = intent.getStringExtra("price");
        ArrayList<String> images = intent.getStringArrayListExtra("images");
        ArrayList<String> details = intent.getStringArrayListExtra("details");

        // ===== تعيين النوع والسعر =====
        txtType.setText(type != null ? type : "نوع غير محدد");
        txtPrice.setText(price != null ? price : "سعر غير محدد");

        // ===== ViewPager للصور =====
        if (images != null && !images.isEmpty()) {
            ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(this, images);
            viewPagerImages.setAdapter(sliderAdapter);
        }

        // ===== تعبئة جدول التفاصيل =====
        String[] labels = {
                "Region", "Gear Type", "Fuel Type", "Color", "Test Date",
                "Doors", "Seats", "Sunroof", "Disabled Accessible", "Year",
                "Horsepower", "Engine Capacity"
        };

        if (details != null) {
            for (int i = 0; i < labels.length && i < details.size(); i++) {
                TableRow row = new TableRow(this);

                TextView label = new TextView(this);
                label.setText(labels[i]);
                label.setPadding(16,16,16,16);

                TextView value = new TextView(this);
                value.setText(details.get(i));
                value.setPadding(16,16,16,16);

                row.addView(label);
                row.addView(value);

                tableDetails.addView(row);
            }
        }
    }
}
