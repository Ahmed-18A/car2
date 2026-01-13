package com.example.car2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MyCars extends AppCompatActivity {

    private RecyclerView rvMyCars;
    private CarAdapter carAdapter;
    private ArrayList<Car> myCars = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private FrameLayout progressOverlay;

    BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_cars);

        bottomNav=findViewById(R.id.bottom_navigation);

        rvMyCars = findViewById(R.id.rvMyCars);
        progressOverlay = findViewById(R.id.progressOverlay);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        carAdapter = new CarAdapter(this, myCars);
        rvMyCars.setLayoutManager(new LinearLayoutManager(this));
        rvMyCars.setAdapter(carAdapter);

        loadMyCars();

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.mnu_profile) {
                startActivity(new Intent(MyCars.this, profile.class));
                finish();
            }
            if(item.getItemId() == R.id.mnu_add) {
                startActivity(new Intent(MyCars.this, addCar.class));
                finish();
            }
            if(item.getItemId() == R.id.mnu_dash) {
                startActivity(new Intent(MyCars.this, dashboard.class));
                finish();
            }
            return true;
        });
    }
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // إلغاء أي تحديد موجود
        for (int i = 0; i < bottomNav.getMenu().size(); i++) {
            bottomNav.getMenu().getItem(i).setChecked(false);
        }
    }

    private void loadMyCars() {
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        progressOverlay.setVisibility(View.VISIBLE);

        Query query = db.collection("cars")
                .whereEqualTo("ownerId", currentUser.getUid());

        query.get().addOnCompleteListener(task -> {
            progressOverlay.setVisibility(View.GONE);

            if (task.isSuccessful()) {
                myCars.clear();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Car car = doc.toObject(Car.class);
                    car.setId(doc.getId());
                    myCars.add(car);
                }
                carAdapter.notifyDataSetChanged();

                if (myCars.isEmpty()) {
                    Toast.makeText(this, "No cars found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Failed to load cars", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
