package com.example.car2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;


import java.util.ArrayList;

public class dashboard extends BaseActivity {

    SwipeRefreshLayout swipeRefreshLayout;

    private static final int SEARCH_REQUEST = 100;

    private RecyclerView rvCars;
    private CarAdapter carAdapter;

    private ArrayList<Car> allCarsList = new ArrayList<>();
    private ArrayList<Car> shownCarsList = new ArrayList<>();

    private CardView cardSearch;
    private FirebaseFirestore db;

    BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        applySystemBars();

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.getMenu().getItem(0).setChecked(false);
        bottomNav.getMenu().setGroupCheckable(0, false, true);

        rvCars = findViewById(R.id.rvCars);
        cardSearch = findViewById(R.id.cardSearch);

        carAdapter = new CarAdapter(this, shownCarsList);
        rvCars.setLayoutManager(new LinearLayoutManager(this));
        rvCars.setAdapter(carAdapter);

        db = FirebaseFirestore.getInstance();
        loadCarsFromFirebase();

        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(this::recreate);

        cardSearch.setOnClickListener(v -> {
            Intent intent = new Intent(dashboard.this, SearchActivity.class);
            startActivityForResult(intent, SEARCH_REQUEST);
        });

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.mnu_profile) {
                startActivity(new Intent(dashboard.this, profile.class));
                finish();
            }
            if (item.getItemId() == R.id.mnu_add) {
                startActivity(new Intent(dashboard.this, addCar.class));
                finish();
            }
            if (item.getItemId() == R.id.mnu_myC) {
                startActivity(new Intent(dashboard.this, MyCars.class));
                finish();
            }
            if(item.getItemId() == R.id.mnu_chats) {
                startActivity(new Intent(dashboard.this, ChatsActivity.class));
                finish();
            }
            return true;
        });
    }

    // ================= FIREBASE =================
    private void loadCarsFromFirebase() {
        String myId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("cars").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                allCarsList.clear();
                shownCarsList.clear();

                for (var doc : task.getResult()) {
                    String ownerId = doc.getString("ownerId");
                    if (ownerId != null && ownerId.equals(myId)) {
                        continue;
                    }
                    Car car = doc.toObject(Car.class);
                    car.setId(doc.getId());
                    allCarsList.add(car);
                }

                shownCarsList.addAll(allCarsList);
                carAdapter.notifyDataSetChanged();

            } else {
                Toast.makeText(this, "Failed to load cars", Toast.LENGTH_SHORT).show();
            }
        });
    }


    protected void onResume() {
        super.onResume();
        swipeRefreshLayout.setRefreshing(false);
    }

    // ================= RECEIVE SEARCH =================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SEARCH_REQUEST && resultCode == RESULT_OK && data != null) {

            String region = data.getStringExtra("region");
            String carType = data.getStringExtra("carType");
            String gearType = data.getStringExtra("gearType");
            String fuelType = data.getStringExtra("fuelType");
            String color = data.getStringExtra("color");
            String doors = data.getStringExtra("doors");
            String seats = data.getStringExtra("seats");
            String sunroof = data.getStringExtra("sunroof");
            String disabled = data.getStringExtra("disabled");

            float minPrice = data.getFloatExtra("minPrice", 0);
            float maxPrice = data.getFloatExtra("maxPrice", Float.MAX_VALUE);

            String year = data.getStringExtra("year");
            String horsePower = data.getStringExtra("horsePower");
            String engineCapacity = data.getStringExtra("engineCapacity");

            applyFilter(
                    region, carType, gearType, fuelType, color,
                    doors, seats, sunroof, disabled,
                    minPrice, maxPrice, year, horsePower, engineCapacity
            );
        }
    }

    // ================= FILTER =================
    private void applyFilter(String region, String carType, String gearType,
                             String fuelType, String color, String doors,
                             String seats, String sunroof, String disabled,
                             float minPrice, float maxPrice, String year,
                             String horsePower, String engineCapacity) {

        shownCarsList.clear();


        for (Car car : allCarsList) {
            // ===== Spinners =====
            if (!"Any".equals(region) && (car.getLocation() == null || !car.getLocation().equals(region)))
                continue;

            if (!"Any".equals(carType) && (car.getType() == null || !car.getType().equals(carType)))
                continue;

            if (!"Any".equals(gearType) && (car.getGearType() == null || !car.getGearType().equals(gearType)))
                continue;

            if (!"Any".equals(fuelType) && (car.getFuelType() == null || !car.getFuelType().equals(fuelType)))
                continue;

            if (!"Any".equals(color) && (car.getColor() == null || !car.getColor().equals(color)))
                continue;

            if (!"Any".equals(doors) && !String.valueOf(car.getDoors()).equals(doors))
                continue;

            if (!"Any".equals(seats) && !String.valueOf(car.getSeats()).equals(seats))
                continue;

            if (!"Any".equals(sunroof) && !String.valueOf(car.getSunroof()).equals(sunroof))
                continue;

            if (!"Any".equals(disabled) && !String.valueOf(car.getDisabledCar()).equals(disabled))
                continue;

            // ===== EditTexts =====
            if (!TextUtils.isEmpty(year) && !year.equals(car.getYear()))
                continue;

            if (!TextUtils.isEmpty(horsePower) && !horsePower.equals(car.getHorsePower()))
                continue;

            if (!TextUtils.isEmpty(engineCapacity) && !engineCapacity.equals(car.getEngineCapacity()))
                continue;

            // ===== Slider Price =====
            if (maxPrice == 500000) {
                if (Float.parseFloat(car.getPrice()) < minPrice) continue;
            }
            else {
                if (Float.parseFloat(car.getPrice()) < minPrice || Float.parseFloat(car.getPrice()) > maxPrice) continue;
            }
            String myId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (car.getOwnerId() != null && car.getOwnerId().equals(myId)) continue;


            // ===== إضافة السيارة للقائمة المعروضة =====
            shownCarsList.add(car);
        }

        carAdapter.notifyDataSetChanged();
    }
}
