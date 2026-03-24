package com.example.car2;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

public class addCar extends BaseActivity {

    private static final int REQUEST_GALLERY = 101;
    private static final String IMGBB_API_KEY = "3c6e38b46c0548e23b364cf83954877f";
    private static final int REQ_PAYMENT = 202;

    FrameLayout progressOverlay;

    private ArrayList<Uri> allImages = new ArrayList<>();

    private Spinner spLocation, spGearType, spFuelType, spColor, spDoors, spSeats;
    private EditText etTestDateyy, etTestDatemm, etPrice, etYear, etHorsePower, etEngineCapacity;
    private CheckBox cbSunroof, cbDisabled;
    private Button btnAddImages, btnAddCar, btnChooseCarType;
    private BottomNavigationView bottomNav;

    private TextView tvChosenMake, tvChosenModel, tvChosenTrim;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private final ArrayList<CarMake> carData = new ArrayList<>();
    private String selectedMake = "";
    private String selectedModel = "";
    private String selectedTrim = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_car);

        applySystemBars();

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.getMenu().getItem(0).setChecked(false);
        bottomNav.getMenu().setGroupCheckable(0, false, true);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        progressOverlay = findViewById(R.id.progressOverlay);

        spLocation = findViewById(R.id.spLocation);
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

        btnAddImages = findViewById(R.id.btnSearch);
        btnAddCar = findViewById(R.id.btnAddCar);

        btnChooseCarType = findViewById(R.id.btnChooseCarType);
        tvChosenMake = findViewById(R.id.tvChosenMake);
        tvChosenModel = findViewById(R.id.tvChosenModel);
        tvChosenTrim = findViewById(R.id.tvChosenTrim);

        loadCarsFromJson();

        btnChooseCarType.setOnClickListener(v -> {
            hideKeyboard(this);

            if (carData.isEmpty()) {
                Toast.makeText(this, "Car list not loaded", Toast.LENGTH_SHORT).show();
                return;
            }

            showMakeDialog();
        });

        btnAddImages.setOnClickListener(v -> {
            checkPermissionAndOpenGallery();
            hideKeyboard(this);
        });

        btnAddCar.setOnClickListener(v -> {

            hideKeyboard(this);

            if (selectedMake.isEmpty() || selectedModel.isEmpty() || selectedTrim.isEmpty()) {
                Toast.makeText(this, "Please choose manufacturer, model and trim", Toast.LENGTH_SHORT).show();
                return;
            }

            if (allImages.size() < 5) {
                Toast.makeText(this, "Please select at least 5 images", Toast.LENGTH_SHORT).show();
                return;
            }

            if (etEngineCapacity.getText().toString().trim().isEmpty() ||
                    etPrice.getText().toString().trim().isEmpty() ||
                    etHorsePower.getText().toString().trim().isEmpty() ||
                    etYear.getText().toString().trim().isEmpty() ||
                    etTestDatemm.getText().toString().trim().isEmpty() ||
                    etTestDateyy.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (Integer.parseInt(etTestDatemm.getText().toString()) == 0 ||
                    Integer.parseInt(etTestDatemm.getText().toString()) > 12) {
                Toast.makeText(this, "Rong test date", Toast.LENGTH_SHORT).show();
                return;
            }

            double price = Double.parseDouble(etPrice.getText().toString().trim());

            Intent i = new Intent(addCar.this, payment.class);
            i.putExtra(payment.EXTRA_PRICE, price);
            startActivityForResult(i, REQ_PAYMENT);

        });

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.mnu_profile) {
                startActivity(new Intent(addCar.this, profile.class));
                finish();
            }
            if (item.getItemId() == R.id.mnu_dash) {
                startActivity(new Intent(addCar.this, dashboard.class));
                finish();
            }
            if (item.getItemId() == R.id.mnu_myC) {
                startActivity(new Intent(addCar.this, MyCars.class));
                finish();
            }
            if (item.getItemId() == R.id.mnu_chats) {
                startActivity(new Intent(addCar.this, ChatsActivity.class));
                finish();
            }
            return true;
        });
    }

    // ===== JSON MODELS =====

    private static class CarMake {
        String name;
        ArrayList<CarModel> models = new ArrayList<>();
    }

    private static class CarModel {
        String name;
        ArrayList<String> trims = new ArrayList<>();
    }

    private interface OnValuePicked {
        void onPicked(String value);
    }

    private void loadCarsFromJson() {
        carData.clear();

        try {
            InputStream is = getAssets().open("israel_cars.json");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8)
            );

            StringBuilder builder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            reader.close();

            JSONArray makesArray = new JSONArray(builder.toString());

            for (int i = 0; i < makesArray.length(); i++) {
                JSONObject makeObj = makesArray.getJSONObject(i);

                CarMake make = new CarMake();
                make.name = makeObj.getString("make");

                JSONArray modelsArray = makeObj.getJSONArray("models");

                for (int j = 0; j < modelsArray.length(); j++) {
                    JSONObject modelObj = modelsArray.getJSONObject(j);

                    CarModel model = new CarModel();
                    model.name = modelObj.getString("name");

                    JSONArray trimsArray = modelObj.getJSONArray("trims");
                    for (int k = 0; k < trimsArray.length(); k++) {
                        model.trims.add(trimsArray.getString(k));
                    }

                    if (model.trims.isEmpty()) {
                        model.trims.add("Standard");
                    }

                    make.models.add(model);
                }

                carData.add(make);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load car list", Toast.LENGTH_LONG).show();
        }
    }

    private void showMakeDialog() {
        ArrayList<String> makes = new ArrayList<>();
        for (CarMake make : carData) {
            makes.add(make.name);
        }

        showSearchableDialog("Choose manufacturer", makes, value -> {
            selectedMake = value;
            selectedModel = "";
            selectedTrim = "";
            updateSelectedViews();

            CarMake make = getMakeByName(value);
            if (make != null) {
                showModelDialog(make);
            }
        });
    }

    private void showModelDialog(CarMake make) {
        ArrayList<String> models = new ArrayList<>();
        for (CarModel model : make.models) {
            models.add(model.name);
        }

        showSearchableDialog("Choose model", models, value -> {
            selectedModel = value;
            selectedTrim = "";
            updateSelectedViews();

            CarModel model = getModelByName(make, value);
            if (model != null) {
                showTrimDialog(model);
            }
        });
    }

    private void showTrimDialog(CarModel model) {
        String[] trimArray = model.trims.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Choose trim")
                .setItems(trimArray, (dialog, which) -> {
                    selectedTrim = model.trims.get(which);
                    updateSelectedViews();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSearchableDialog(String title, ArrayList<String> originalItems, OnValuePicked listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(16);
        root.setPadding(padding, padding, padding, padding);

        EditText etSearch = new EditText(this);
        etSearch.setHint("Search...");
        root.addView(etSearch, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        ListView listView = new ListView(this);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(400)
        );
        listParams.topMargin = dpToPx(10);
        root.addView(listView, listParams);

        ArrayList<String> filteredItems = new ArrayList<>(originalItems);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                filteredItems
        );
        listView.setAdapter(adapter);

        builder.setView(root);
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().trim().toLowerCase();

                filteredItems.clear();

                for (String item : originalItems) {
                    if (item.toLowerCase().contains(q)) {
                        filteredItems.add(item);
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            String picked = filteredItems.get(position);
            dialog.dismiss();
            listener.onPicked(picked);
        });

        dialog.show();
    }

    private CarMake getMakeByName(String name) {
        for (CarMake make : carData) {
            if (make.name.equals(name)) return make;
        }
        return null;
    }

    private CarModel getModelByName(CarMake make, String name) {
        for (CarModel model : make.models) {
            if (model.name.equals(name)) return model;
        }
        return null;
    }

    private void updateSelectedViews() {
        if (!selectedMake.isEmpty()) {
            tvChosenMake.setText("Manufacturer: " + selectedMake);
            tvChosenMake.setVisibility(View.VISIBLE);
        } else {
            tvChosenMake.setVisibility(View.GONE);
        }

        if (!selectedModel.isEmpty()) {
            tvChosenModel.setText("Model: " + selectedModel);
            tvChosenModel.setVisibility(View.VISIBLE);
        } else {
            tvChosenModel.setVisibility(View.GONE);
        }

        if (!selectedTrim.isEmpty()) {
            tvChosenTrim.setText("Trim: " + selectedTrim);
            tvChosenTrim.setVisibility(View.VISIBLE);
        } else {
            tvChosenTrim.setVisibility(View.GONE);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
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

    // ===== UPLOAD IMAGES =====
    private void uploadAllImagesAndSaveCar() {
        if (allImages.size() != 5) {
            Toast.makeText(this, "You must select exactly 5 images", Toast.LENGTH_SHORT).show();
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
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
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
                            });
                        }
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(addCar.this, "Image error", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // ===== FIREBASE =====
    private void saveCarToFirebase(ArrayList<String> imageUrls) {
        FirebaseUser user = auth.getCurrentUser();

        Map<String, Object> car = new HashMap<>();

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

        car.put("price", etPrice.getText().toString());
        car.put("type", selectedMake);
        car.put("model", selectedModel);
        car.put("trim", selectedTrim);
        car.put("fullType", selectedMake + " " + selectedModel + " " + selectedTrim);

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
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Car added successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(addCar.this, MyCars.class));
                })
                .addOnFailureListener(e -> {
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to add car", Toast.LENGTH_SHORT).show();
                });
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = activity.getWindow().getDecorView();
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        getWindow().getDecorView().clearFocus();
        hideKeyboard(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        // ====== Gallery ======
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
            return;
        }

        // ====== Payment OK ======
        if (requestCode == REQ_PAYMENT) {
            progressOverlay.setVisibility(View.VISIBLE);
            uploadAllImagesAndSaveCar();
        }
    }
}