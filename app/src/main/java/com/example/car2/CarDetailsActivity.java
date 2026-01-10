package com.example.car2;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
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

        // Get data from Intent
        Intent intent = getIntent();
        String type = intent.getStringExtra("type");
        String price = intent.getStringExtra("price");
        String[] details = intent.getStringArrayExtra("details");
        String[] images = intent.getStringArrayExtra("images");

        // Set type & price
        txtType.setText(type);
        txtPrice.setText(price);

        // Setup Image Slider
        ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(this, images);
        viewPagerImages.setAdapter(sliderAdapter);

        // Fill table with details
        String[] labels = {
                "Region", "Gear Type", "Fuel Type", "Color", "Test Date",
                "Doors", "Seats", "Sunroof", "Disabled Accessible", "Year", "Horsepower", "Engine Capacity"
        };

        for (int i = 0; i < labels.length && i < details.length; i++) {
            TableRow row = new TableRow(this);
            TextView label = new TextView(this);
            TextView value = new TextView(this);

            label.setText(labels[i]);
            label.setPadding(8,8,8,8);
            value.setText(details[i]);
            value.setPadding(8,8,8,8);

            row.addView(label);
            row.addView(value);
            tableDetails.addView(row);
        }
    }
}