package com.example.car2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

public class Edit extends AppCompatActivity {
    ImageButton btnDel;
    Spinner location, type, gear, fuel, color, doors, seats,sunroof, disabledAccessible;
    EditText testMM, testYY, price, year, horsePower, engineCapacity;
    FrameLayout progressOverlay;
    Button apply;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private static final int REQUEST_PICK_5_IMAGES = 201;
    private static final String IMGBB_API_KEY = "3c6e38b46c0548e23b364cf83954877f";
    private ArrayList<String> uploadedImageUrls = new ArrayList<>();

    private ArrayList<Uri> selectedImages = new ArrayList<>();
    private Button btnPickImages;
    private boolean img=false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        btnDel=findViewById(R.id.btnDel);

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
        sunroof = findViewById(R.id.spSunroof);
        disabledAccessible = findViewById(R.id.spDisabled);

        testMM = findViewById(R.id.etTestDatemm);
        testYY = findViewById(R.id.etTestDateyy);
        price = findViewById(R.id.etPrice);
        year = findViewById(R.id.etYear);
        horsePower = findViewById(R.id.etHorsePower);
        engineCapacity = findViewById(R.id.etEngineCapacity);

        Car oldCar = (Car) getIntent().getSerializableExtra("car");

        type.setSelection(getSpinnerIndex(type, oldCar.getType()));
        price.setText(oldCar.getPrice());
        location.setSelection(getSpinnerIndex(location, oldCar.getLocation()));
        gear.setSelection(getSpinnerIndex(gear, oldCar.getGearType()));
        fuel.setSelection(getSpinnerIndex(fuel, oldCar.getFuelType()));
        color.setSelection(getSpinnerIndex(color, oldCar.getColor()));
        doors.setSelection(getSpinnerIndex(doors, oldCar.getDoors()));
        seats.setSelection(getSpinnerIndex(seats, oldCar.getSeats()));
        sunroof.setSelection(getSpinnerIndex(sunroof, oldCar.getSunroof()));
        disabledAccessible.setSelection(getSpinnerIndex(disabledAccessible, oldCar.getDisabledCar()));
        testMM.setText(oldCar.getTestDate().substring(0, oldCar.getTestDate().indexOf("/")));
        testYY.setText(oldCar.getTestDate().substring(oldCar.getTestDate().indexOf("/") + 1));
        year.setText(oldCar.getYear());
        horsePower.setText(oldCar.getHorsePower());
        engineCapacity.setText(oldCar.getEngineCapacity());

        btnPickImages = findViewById(R.id.btnchImg);

        btnPickImages.setOnClickListener(v -> {
            openGalleryForFiveImages();
            img=true;
            hideKeyboard(this);
        });

        btnDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new androidx.appcompat.app.AlertDialog.Builder(Edit.this)
                        .setTitle("Delete Car")
                        .setMessage("Are you sure you want to delete this car?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            db.collection("cars")
                                    .document(oldCar.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(Edit.this, "Car deleted successfully", Toast.LENGTH_SHORT).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(Edit.this, "Failed to delete car", Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .setCancelable(true)
                        .show();
            }
        });



        apply.setOnClickListener(v -> {
            progressOverlay.setVisibility(View.VISIBLE);
            hideKeyboard(this);

            if (img) {
                // المستخدم اختار صور جديدة → ارفعهم أولًا
                if (selectedImages.size() != 5) {
                    Toast.makeText(this, "لازم تختار 5 صور", Toast.LENGTH_SHORT).show();
                    progressOverlay.setVisibility(View.GONE);
                    return;
                }

                uploadedImageUrls.clear();
                uploadImageRecursive(0, oldCar);

            } else {
                // المستخدم ما غيّر الصور → احفظ مباشرة
                saveCar(oldCar, oldCar.getImages());
            }
        });

    }

    private int getSpinnerIndex(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) return i;
        }
        return 0;
    }

    private void openGalleryForFiveImages() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_PICK_5_IMAGES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK_5_IMAGES && resultCode == Activity.RESULT_OK && data != null) {

            selectedImages.clear();

            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();

                if (count != 5) {
                    Toast.makeText(this, "لازم تختار 5 صور بالضبط", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    selectedImages.add(imageUri);
                }

            } else if (data.getData() != null) {
                Toast.makeText(this, "اختيار صورة واحدة غير مسموح", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "تم اختيار 5 صور بنجاح ✅", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageRecursive(int index, Car oldCar) {

        if (index >= selectedImages.size()) {
            // ✔️ خلص الرفع
            saveCar(oldCar, uploadedImageUrls);
            return;
        }

        Uri imageUri = selectedImages.get(index);

        new Thread(() -> {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                bitmap = Utils.rotateImageIfRequired(this, bitmap, imageUri);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);

                String encodedImage = android.util.Base64.encodeToString(
                        baos.toByteArray(), android.util.Base64.NO_WRAP
                );

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
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(Edit.this, "فشل رفع الصور", Toast.LENGTH_SHORT).show();
                            progressOverlay.setVisibility(View.GONE);
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        try {
                            String url = Utils.parseImgBBUrl(response.body().string());
                            uploadedImageUrls.add(url);

                            runOnUiThread(() ->
                                    uploadImageRecursive(index + 1, oldCar)
                            );

                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                Toast.makeText(Edit.this, "خطأ بالرفع", Toast.LENGTH_SHORT).show();
                                progressOverlay.setVisibility(View.GONE);
                            });
                        }
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(Edit.this, "خطأ بالصورة", Toast.LENGTH_SHORT).show();
                    progressOverlay.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void saveCar(Car oldCar, ArrayList<String> images) {

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
        newCar.put("sunroof", sunroof.getSelectedItem().toString());
        newCar.put("disabledCar", disabledAccessible.getSelectedItem().toString());
        newCar.put("images", images);
        newCar.put("ownerId", user.getUid());

        db.collection("cars").document(oldCar.getId())
                .set(newCar)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Car updated successfully!", Toast.LENGTH_SHORT).show();
                    progressOverlay.setVisibility(View.GONE);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update car!", Toast.LENGTH_SHORT).show();
                    progressOverlay.setVisibility(View.GONE);
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
