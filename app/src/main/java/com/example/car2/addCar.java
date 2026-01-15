package com.example.car2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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

    private Spinner spLocation, spCarType, spGearType, spFuelType, spColor, spDoors, spSeats;
    private EditText etTestDateyy,etTestDatemm, etPrice, etYear, etHorsePower, etEngineCapacity;
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
        spLocation = findViewById(R.id.spLocation);
        spCarType = findViewById(R.id.spCarType);
        spGearType = findViewById(R.id.spGearType);
        spFuelType = findViewById(R.id.spFuelType);
        spColor = findViewById(R.id.spColor);
        spDoors = findViewById(R.id.spDoors);
        spSeats = findViewById(R.id.spSeats);

        etTestDatemm = findViewById(R.id.etTestDatemm);
        etTestDateyy = findViewById(R.id.etTestDateyy);
        etPrice = findViewById(R.id.etPrice);
        etYear = findViewById(R.id.etYear);
        etHorsePower = findViewById(R.id.etHorsePower);
        etEngineCapacity = findViewById(R.id.etEngineCapacity);

        cbSunroof = findViewById(R.id.spSunroof);
        cbDisabled = findViewById(R.id.spDisabled);

        btnAddImages = findViewById(R.id.btnSearch); // زر Add 5 images
        btnAddCar = findViewById(R.id.btnAddCar); // زر Apply Filter كزر لإضافة السيارة

        // ===== SELECT IMAGES =====
        btnAddImages.setOnClickListener(v -> {
            checkPermissionAndOpenGallery();
            hideKeyboard(this);
        });

        // ===== ADD CAR =====
        btnAddCar.setOnClickListener(v -> {

            hideKeyboard(this);

            if (allImages.size() < 5) {
                Toast.makeText(this, "Please select at least 5 images", Toast.LENGTH_SHORT).show();
                return;
            }

            if (etEngineCapacity.getText().toString().trim().isEmpty() ||
                    etPrice.getText().toString().trim().isEmpty() ||
                    etHorsePower.getText().toString().trim().isEmpty() ||
                    etYear.getText().toString().trim().isEmpty() ||
                    etTestDatemm.getText().toString().trim().isEmpty()||
                    etTestDateyy.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (Integer.parseInt(etTestDatemm.getText().toString())==0||Integer.parseInt(etTestDatemm.getText().toString())>12){
                Toast.makeText(this, "Rong test date", Toast.LENGTH_SHORT).show();
                return;
            }

            isUploading = true;
            btnAddCar.setEnabled(false);

            progressOverlay.setVisibility(View.VISIBLE);
            bottomNav.setVisibility(View.GONE);

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
            if(item.getItemId() == R.id.mnu_myC) {
                startActivity(new Intent(addCar.this, MyCars.class));
                finish();
            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // إلغاء أي تحديد موجود
        for (int i = 0; i < bottomNav.getMenu().size(); i++) {
            bottomNav.getMenu().getItem(i).setChecked(false);
        }
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
                if (count != 5) {
                    Toast.makeText(this, "You must select exactly 5 images", Toast.LENGTH_SHORT).show();
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
        if (allImages.size() != 5) {
            Toast.makeText(this, "You must select exactly 5 images", Toast.LENGTH_SHORT).show();
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
                            bottomNav.setVisibility(View.VISIBLE);
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
                                bottomNav.setVisibility(View.VISIBLE);
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

        // ===== متغيرات مستقلة لكل صفة =====
        String location = spLocation.getSelectedItem().toString();
        String gearType = spGearType.getSelectedItem().toString();
        String fuelType = spFuelType.getSelectedItem().toString();
        String color = spColor.getSelectedItem().toString();
        String doors = spDoors.getSelectedItem().toString();
        String seats = spSeats.getSelectedItem().toString();
        String testDate = etTestDatemm.getText().toString().trim() + "/" + etTestDateyy.getText().toString().trim();
        String year = etYear.getText().toString().trim();
        String horsePower = etHorsePower.getText().toString().trim();
        String engineCapacity = etEngineCapacity.getText().toString().trim();
        String sunroof = cbSunroof.isChecked() ? "Yes" : "No";
        String disabledCar = cbDisabled.isChecked() ? "Yes" : "No";

        // ===== حفظ الحقول المستقلة =====
        car.put("price", etPrice.getText().toString());
        car.put("type", spCarType.getSelectedItem().toString());
        car.put("location", location);
        car.put("gearType", gearType);
        car.put("fuelType", fuelType);
        car.put("color", color);
        car.put("doors", doors);
        car.put("seats", seats);
        car.put("testDate", testDate);
        car.put("year", year);
        car.put("horsePower", horsePower);
        car.put("engineCapacity", engineCapacity);
        car.put("sunroof", sunroof);
        car.put("disabledCar", disabledCar);

        car.put("images", imageUrls);
        car.put("ownerId", user.getUid());

        db.collection("cars")
                .add(car)
                .addOnSuccessListener(docRef -> {
                    bottomNav.setVisibility(View.VISIBLE);
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Car added successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(addCar.this, dashboard.class));
                })
                .addOnFailureListener(e -> {
                    bottomNav.setVisibility(View.VISIBLE);
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to add car", Toast.LENGTH_SHORT).show();
                    btnAddCar.setEnabled(true);
                    isUploading = false;
                });
    }
    public static void hideKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
