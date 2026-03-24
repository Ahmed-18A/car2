package com.example.car2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

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

public class Edit extends BaseActivity {
    ImageButton btnDel, btnback;
    Spinner location, gear, fuel, color, doors, seats, sunroof, disabledAccessible;
    EditText testMM, testYY, price, year, horsePower, engineCapacity;
    FrameLayout progressOverlay;
    Button apply, btnPickImages, btnChooseCarType;
    TextView tvChosenMake, tvChosenModel, tvChosenTrim;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private static final int REQUEST_PICK_5_IMAGES = 201;
    private static final String IMGBB_API_KEY = "3c6e38b46c0548e23b364cf83954877f";
    private ArrayList<String> uploadedImageUrls = new ArrayList<>();
    private ArrayList<Uri> selectedImages = new ArrayList<>();
    private boolean img = false;

    private final ArrayList<CarMake> carData = new ArrayList<>();
    private String selectedMake = "";
    private String selectedModel = "";
    private String selectedTrim = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        applySystemBars();

        btnDel = findViewById(R.id.btnDel);
        btnback = findViewById(R.id.ImageButton);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        progressOverlay = findViewById(R.id.progressOverlay);
        apply = findViewById(R.id.btnchinfo);

        location = findViewById(R.id.spLocation);
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

        btnPickImages = findViewById(R.id.btnchImg);
        btnChooseCarType = findViewById(R.id.btnChooseCarType);
        tvChosenMake = findViewById(R.id.tvChosenMake);
        tvChosenModel = findViewById(R.id.tvChosenModel);
        tvChosenTrim = findViewById(R.id.tvChosenTrim);

        loadCarsFromJson();

        Car oldCar = (Car) getIntent().getSerializableExtra("car");

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

        selectedMake = oldCar.getType() != null ? oldCar.getType() : "";
        selectedModel = oldCar.getModel() != null ? oldCar.getModel() : "";
        selectedTrim = oldCar.getTrim() != null ? oldCar.getTrim() : "";
        updateSelectedViews();

        btnChooseCarType.setOnClickListener(v -> {
            hideKeyboard(this);

            if (carData.isEmpty()) {
                Toast.makeText(this, "Car list not loaded", Toast.LENGTH_SHORT).show();
                return;
            }

            showMakeDialog();
        });

        btnPickImages.setOnClickListener(v -> {
            openGalleryForFiveImages();
            img = true;
            hideKeyboard(this);
        });

        btnback.setOnClickListener(v -> finish());

        btnDel.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(Edit.this)
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
                            .addOnFailureListener(e ->
                                    Toast.makeText(Edit.this, "Failed to delete car", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show());

        apply.setOnClickListener(v -> {
            progressOverlay.setVisibility(View.VISIBLE);
            hideKeyboard(this);

            if (selectedMake.isEmpty() || selectedModel.isEmpty() || selectedTrim.isEmpty()) {
                Toast.makeText(this, "Please choose manufacturer, model and trim", Toast.LENGTH_SHORT).show();
                progressOverlay.setVisibility(View.GONE);
                return;
            }

            if (img) {
                if (selectedImages.size() != 5) {
                    Toast.makeText(this, "لازم تختار 5 صور", Toast.LENGTH_SHORT).show();
                    progressOverlay.setVisibility(View.GONE);
                    return;
                }

                uploadedImageUrls.clear();
                uploadImageRecursive(0, oldCar);

            } else {
                saveCar(oldCar, oldCar.getImages());
            }
        });
    }

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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filteredItems);
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
                    public void onFailure(Call call, java.io.IOException e) {
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

                            runOnUiThread(() -> uploadImageRecursive(index + 1, oldCar));

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

        newCar.put("type", selectedMake);
        newCar.put("model", selectedModel);
        newCar.put("trim", selectedTrim);
        newCar.put("fullType", selectedMake + " " + selectedModel + " " + selectedTrim);

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