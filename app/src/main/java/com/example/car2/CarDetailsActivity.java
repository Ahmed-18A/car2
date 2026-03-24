package com.example.car2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class CarDetailsActivity extends BaseActivity {

    Button btnChat;
    ViewPager2 viewPagerImages;
    TextView txtType, txtPrice;
    TableLayout tableDetails;
    ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_details);

        applySystemBars();

        viewPagerImages = findViewById(R.id.viewPagerImages);
        txtType = findViewById(R.id.txtType);
        txtPrice = findViewById(R.id.txtPrice);
        tableDetails = findViewById(R.id.tableDetails);
        btnBack = findViewById(R.id.ImageButton);
        btnChat = findViewById(R.id.btnChat);

        Car car = (Car) getIntent().getSerializableExtra("car");
        if (car == null) {
            finish();
            return;
        }

        txtType.setText(car.getFullType() != null ? car.getFullType() : "");
        txtPrice.setText(car.getPrice() != null ? car.getPrice() : "");

        if (car.getImages() != null && !car.getImages().isEmpty()) {
            ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(this, car.getImages());
            viewPagerImages.setAdapter(sliderAdapter);
        }

        String[] labels = {
                "Type", "Model", "Trim", "Location", "Gear Type", "Fuel Type", "Color",
                "Doors", "Seats", "Test Date", "Year", "Horsepower",
                "Engine Capacity", "Sunroof", "Disabled Accessible"
        };

        ArrayList<String> values = new ArrayList<>();
        values.add(car.getType() != null ? car.getType() : "");
        values.add(car.getModel() != null ? car.getModel() : "");
        values.add(car.getTrim() != null ? car.getTrim() : "");
        values.add(car.getLocation() != null ? car.getLocation() : "");
        values.add(car.getGearType() != null ? car.getGearType() : "");
        values.add(car.getFuelType() != null ? car.getFuelType() : "");
        values.add(car.getColor() != null ? car.getColor() : "");
        values.add(car.getDoors() != null ? car.getDoors() : "");
        values.add(car.getSeats() != null ? car.getSeats() : "");
        values.add(car.getTestDate() != null ? car.getTestDate() : "");
        values.add(car.getYear() != null ? car.getYear() : "");
        values.add(car.getHorsePower() != null ? car.getHorsePower() : "");
        values.add(car.getEngineCapacity() != null ? car.getEngineCapacity() : "");
        values.add(car.getSunroof() != null ? car.getSunroof() : "");
        values.add(car.getDisabledCar() != null ? car.getDisabledCar() : "");

        for (int i = 0; i < labels.length; i++) {
            TableRow row = new TableRow(this);

            TextView label = new TextView(this);
            label.setText(labels[i]);
            label.setPadding(16, 16, 16, 16);

            TextView value = new TextView(this);
            value.setText(values.get(i));
            value.setPadding(16, 16, 16, 16);

            row.addView(label);
            row.addView(value);

            tableDetails.addView(row);
        }

        btnBack.setOnClickListener(v -> finish());

        btnChat.setOnClickListener(v -> {
            String myId = FirebaseAuth.getInstance().getUid();
            String carId = getIntent().getStringExtra("carId");

            if (myId == null || carId == null) {
                return;
            }

            FirebaseFirestore.getInstance()
                    .collection("cars")
                    .document(carId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String sellerId = doc.getString("ownerId");

                        if (sellerId == null) {
                            return;
                        }

                        if (myId.equals(sellerId)) {
                            return;
                        }

                        Intent intent = new Intent(CarDetailsActivity.this, ChatActivity.class);
                        intent.putExtra("sellerId", sellerId);
                        startActivity(intent);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to load ownerId", Toast.LENGTH_SHORT).show()
                    );
        });
    }
}