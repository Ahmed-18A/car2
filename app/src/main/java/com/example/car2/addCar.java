package com.example.car2;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class addCar extends AppCompatActivity {

    private static final int REQUEST_GALLERY = 101;
    private static final String IMGBB_API_KEY = "3c6e38b46c0548e23b364cf83954877f";

    FrameLayout progressOverlay;

    private ArrayList<Uri> allImages = new ArrayList<>();

    private Spinner spRegion, spCarType, spGearType, spFuelType, spColor, spDoors, spSeats;
    private EditText etTestDate, etPrice, etYear, etHorsePower, etEngineCapacity;
    private CheckBox cbSunroof, cbDisabled;
    private Button btnAddImages, btnAddCar;
    private BottomNavigationView bottomNav;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private boolean isUploading = false; // لمنع الضغط المتكرر

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_car);

        bottomNav = findViewById(R.id.bottom_navigation);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        progressOverlay = findViewById(R.id.progressOverlay);

        // ===== INIT VIEWS =====
        spRegion = findViewById(R.id.spRegion);
        spCarType = findViewById(R.id.spCarType);
        spGearType = findViewById(R.id.spGearType);
        spFuelType = findViewById(R.id.spFuelType);
        spColor = findViewById(R.id.spColor);
        spDoors = findViewById(R.id.spDoors);
        spSeats = findViewById(R.id.spSeats);

        etTestDate = findViewById(R.id.etTestDate);
        etPrice = findViewById(R.id.etPrice);
        etYear = findViewById(R.id.etYear);
        etHorsePower = findViewById(R.id.etHorsePower);
        etEngineCapacity = findViewById(R.id.etEngineCapacity);

        cbSunroof = findViewById(R.id.cbSunroof);
        cbDisabled = findViewById(R.id.cbDisabled);

        btnAddImages = findViewById(R.id.btnApplyFilter); // زر Add 5 images
        btnAddCar = findViewById(R.id.btnAddCar); // زر Apply Filter كزر لإضافة السيارة

        // ===== SELECT IMAGES =====
        btnAddImages.setOnClickListener(v -> checkPermissionAndOpenGallery());

        // ===== ADD CAR =====
        btnAddCar.setOnClickListener(v -> {
            if (isUploading) return; // لمنع الضغط المتكرر
            isUploading = true;
            btnAddCar.setEnabled(false);

            progressOverlay.setVisibility(View.VISIBLE);

            uploadAllImagesAndSaveCar();
        });

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.mnu_profile) {
                startActivity(new Intent(addCar.this, profile.class));
                finish();
            }
            if(item.getItemId() == R.id.mnu_dash) {
                startActivity(new Intent(addCar.this, dashboard.class));
                finish();
            }
            if(item.getItemId() == R.id.mnu_add) { }
            if(item.getItemId() == R.id.mnu_search) {
                startActivity(new Intent(addCar.this, SearchActivity.class));
                finish();
            }
            return true;
        });
    }

    // ===== PERMISSION =====
    private void checkPermissionAndOpenGallery() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES :
                Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_GALLERY);
        } else {
            openGallery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_GALLERY && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) return;

        if (requestCode == REQUEST_GALLERY && data != null) {
            allImages.clear();
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                if (count < 5) {
                    Toast.makeText(this, "Please select at least 5 images", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (int i = 0; i < count; i++) {
                    allImages.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                allImages.add(data.getData());
            }
            Toast.makeText(this, allImages.size() + " images selected", Toast.LENGTH_SHORT).show();
        }
    }

    // ===== UPLOAD IMAGES =====
    private void uploadAllImagesAndSaveCar() {
        if (allImages.size() < 5) {
            Toast.makeText(this, "Please select at least 5 images", Toast.LENGTH_SHORT).show();
            btnAddCar.setEnabled(true);
            isUploading = false;
            return;
        }

        ArrayList<String> uploadedUrls = new ArrayList<>();
        uploadImageAsync(0, uploadedUrls);
    }

    private void uploadImageAsync(int index, ArrayList<String> uploadedUrls) {
        if (index >= allImages.size()) {
            saveCarToFirebase(uploadedUrls);
            return;
        }

        Uri imageUri = allImages.get(index);

        new Thread(() -> {
            try {
                // فتح الصورة وضغطها خارج Main Thread
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos); // ضغط لتخفيف الضغط
                String encodedImage = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP);

                OkHttpClient client = new OkHttpClient();
                RequestBody body = new FormBody.Builder()
                        .add("key", IMGBB_API_KEY)
                        .add("image", encodedImage)
                        .build();

                Request request = new Request.Builder()
                        .url("https://api.imgbb.com/1/upload")
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, java.io.IOException e) {
                        runOnUiThread(() -> {
                            progressOverlay.setVisibility(View.GONE);
                            Toast.makeText(addCar.this, "Upload failed", Toast.LENGTH_SHORT).show();
                            btnAddCar.setEnabled(true);
                            isUploading = false;
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        try {
                            String url = Utils.parseImgBBUrl(response.body().string());
                            uploadedUrls.add(url);

                            runOnUiThread(() -> uploadImageAsync(index + 1, uploadedUrls));

                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                progressOverlay.setVisibility(View.GONE);
                                Toast.makeText(addCar.this, "Upload error", Toast.LENGTH_SHORT).show();
                                btnAddCar.setEnabled(true);
                                isUploading = false;
                            });
                        }
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(addCar.this, "Image error", Toast.LENGTH_SHORT).show();
                    btnAddCar.setEnabled(true);
                    isUploading = false;
                });
            }
        }).start();
    }

    // ===== FIREBASE =====
    private void saveCarToFirebase(ArrayList<String> imageUrls) {
        FirebaseUser user = auth.getCurrentUser();

        Map<String, Object> car = new HashMap<>();
        car.put("region", spRegion.getSelectedItem().toString());
        car.put("type", spCarType.getSelectedItem().toString());
        car.put("gearType", spGearType.getSelectedItem().toString());
        car.put("fuelType", spFuelType.getSelectedItem().toString());
        car.put("color", spColor.getSelectedItem().toString());
        car.put("doors", spDoors.getSelectedItem().toString());
        car.put("seats", spSeats.getSelectedItem().toString());
        car.put("testDate", etTestDate.getText().toString());
        car.put("price", etPrice.getText().toString());
        car.put("year", etYear.getText().toString());
        car.put("horsePower", etHorsePower.getText().toString());
        car.put("engineCapacity", etEngineCapacity.getText().toString());
        car.put("sunroof", cbSunroof.isChecked() ? "Yes" : "No");
        car.put("disabled", cbDisabled.isChecked() ? "Yes" : "No");
        car.put("images", imageUrls);
        car.put("ownerId", user.getUid());

        db.collection("cars")
                .add(car)
                .addOnSuccessListener(docRef -> {
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Car added successfully!", Toast.LENGTH_SHORT).show();
                    btnAddCar.setEnabled(true);
                    isUploading = false;
                })
                .addOnFailureListener(e -> {
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to add car", Toast.LENGTH_SHORT).show();
                    btnAddCar.setEnabled(true);
                    isUploading = false;
                });
    }
}
