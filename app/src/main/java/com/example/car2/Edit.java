package com.example.car2;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Edit extends AppCompatActivity {

    Spinner location, type, gear, fuel, color, doors, seats;
    EditText testMM, testYY, price, year, horsePower, engineCapacity;
    CheckBox sunroof, disabledAccessible;
    FrameLayout progressOverlay;
    Button apply;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        progressOverlay = findViewById(R.id.progressOverlay);
        apply = findViewById(R.id.btnchinfo);

        location = findViewById(R.id.spLocation);
        type = findViewById(R.id.spCarType);
        gear = findViewById(R.id.spGearType);
        fuel = findViewById(R.id.spFuelType);
        color = findViewById(R.id.spColor);
        doors = findViewById(R.id.spDoors);
        seats = findViewById(R.id.spSeats);

        testMM = findViewById(R.id.etTestDatemm);
        testYY = findViewById(R.id.etTestDateyy);
        price = findViewById(R.id.etPrice);
        year = findViewById(R.id.etYear);
        horsePower = findViewById(R.id.etHorsePower);
        engineCapacity = findViewById(R.id.etEngineCapacity);

        sunroof = findViewById(R.id.cbSunroof);
        disabledAccessible = findViewById(R.id.cbDisabled);

        Car oldCar = (Car) getIntent().getSerializableExtra("car");

        // ==== تعبئة الحقول بالسيارة القديمة ====
        type.setSelection(getSpinnerIndex(type, oldCar.getType()));
        price.setText(oldCar.getPrice());
        location.setSelection(getSpinnerIndex(location, oldCar.getLocation()));
        gear.setSelection(getSpinnerIndex(gear, oldCar.getGearType()));
        fuel.setSelection(getSpinnerIndex(fuel, oldCar.getFuelType()));
        color.setSelection(getSpinnerIndex(color, oldCar.getColor()));
        doors.setSelection(getSpinnerIndex(doors, oldCar.getDoors()));
        seats.setSelection(getSpinnerIndex(seats, oldCar.getSeats()));
        testMM.setText(oldCar.getTestDate().substring(0, oldCar.getTestDate().indexOf("/")));
        testYY.setText(oldCar.getTestDate().substring(oldCar.getTestDate().indexOf("/") + 1));
        year.setText(oldCar.getYear());
        horsePower.setText(oldCar.getHorsePower());
        engineCapacity.setText(oldCar.getEngineCapacity());
        sunroof.setChecked(oldCar.hasSunroof());
        disabledAccessible.setChecked(oldCar.isDisabledCarBool());

        apply.setOnClickListener(v -> {
            progressOverlay.setVisibility(View.VISIBLE);

            FirebaseUser user = auth.getCurrentUser();

            Map<String, Object> newCar = new HashMap<>();
            newCar.put("type", type.getSelectedItem().toString());
            newCar.put("price", price.getText().toString());
            newCar.put("location", location.getSelectedItem().toString());
            newCar.put("gearType", gear.getSelectedItem().toString());
            newCar.put("fuelType", fuel.getSelectedItem().toString());
            newCar.put("color", color.getSelectedItem().toString());
            newCar.put("doors", doors.getSelectedItem().toString());
            newCar.put("seats", seats.getSelectedItem().toString());
            newCar.put("testDate", testMM.getText().toString() + "/" + testYY.getText().toString());
            newCar.put("year", year.getText().toString());
            newCar.put("horsePower", horsePower.getText().toString());
            newCar.put("engineCapacity", engineCapacity.getText().toString());
            newCar.put("sunroof", sunroof.isChecked() ? "Yes" : "No");
            newCar.put("disabledCar", disabledAccessible.isChecked() ? "Yes" : "No");
            newCar.put("images", oldCar.getImages());

            // ✅ استخدم uid الحالي
            newCar.put("ownerId", user.getUid());

            db.collection("cars")
                    .add(newCar)
                    .addOnSuccessListener(docRef -> {
                        db.collection("cars").document(oldCar.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(Edit.this, "Car updated successfully!", Toast.LENGTH_SHORT).show();
                                    progressOverlay.setVisibility(View.GONE);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(Edit.this, "Failed to delete old car!", Toast.LENGTH_SHORT).show();
                                    progressOverlay.setVisibility(View.GONE);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(Edit.this, "Failed to add new car!", Toast.LENGTH_SHORT).show();
                        progressOverlay.setVisibility(View.GONE);
                    });
        });

    }

    private int getSpinnerIndex(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) return i;
        }
        return 0;
    }
}
