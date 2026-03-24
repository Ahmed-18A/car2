package com.example.car2;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.slider.RangeSlider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class SearchActivity extends BaseActivity {

    ImageButton btnBack;
    Spinner spRegion, spGearType, spFuelType, spColor, spDoors, spSeats, spSunroof, spDisabled;
    EditText etYear, etHorsePower, etEngineCapacity;
    RangeSlider sliderPrice;
    Button btnApplyFilter, btnChooseCarType;
    TextView tvChosenMake, tvChosenModel, tvChosenTrim;

    private final ArrayList<CarMake> carData = new ArrayList<>();
    private String selectedMake = "Any";
    private String selectedModel = "Any";
    private String selectedTrim = "Any";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        applySystemBars();

        btnBack = findViewById(R.id.ImageButton);

        spRegion = findViewById(R.id.spLocation);
        spGearType = findViewById(R.id.spGearType);
        spFuelType = findViewById(R.id.spFuelType);
        spColor = findViewById(R.id.spColor);
        spDoors = findViewById(R.id.spDoors);
        spSeats = findViewById(R.id.spSeats);
        spSunroof = findViewById(R.id.spSunroof);
        spDisabled = findViewById(R.id.spDisabled);

        etYear = findViewById(R.id.etYear);
        etHorsePower = findViewById(R.id.etHorsePower);
        etEngineCapacity = findViewById(R.id.etEngineCapacity);

        sliderPrice = findViewById(R.id.sliderPrice);
        btnApplyFilter = findViewById(R.id.btnSearch);

        btnChooseCarType = findViewById(R.id.btnChooseCarType);
        tvChosenMake = findViewById(R.id.tvChosenMake);
        tvChosenModel = findViewById(R.id.tvChosenModel);
        tvChosenTrim = findViewById(R.id.tvChosenTrim);

        loadCarsFromJson();
        updateSelectedViews();

        btnChooseCarType.setOnClickListener(v -> {
            if (carData.isEmpty()) {
                Toast.makeText(this, "Car list not loaded", Toast.LENGTH_SHORT).show();
                return;
            }
            showMakeDialog();
        });

        btnApplyFilter.setOnClickListener(v -> applyFilter());

        btnBack.setOnClickListener(v -> finish());

        sliderPrice.setLabelFormatter(value -> {
            if (value == sliderPrice.getValueTo()) {
                return "+" + String.format("%.0f", value);
            } else {
                return String.format("%.0f", value);
            }
        });
    }

    private void applyFilter() {

        String region = spRegion.getSelectedItem().toString();
        String carType = selectedMake;
        String model = selectedModel;
        String trim = selectedTrim;
        String gearType = spGearType.getSelectedItem().toString();
        String fuelType = spFuelType.getSelectedItem().toString();
        String color = spColor.getSelectedItem().toString();
        String doors = spDoors.getSelectedItem().toString();
        String seats = spSeats.getSelectedItem().toString();
        String sunroof = spSunroof.getSelectedItem().toString();
        String disabledAccessible = spDisabled.getSelectedItem().toString();

        float minPrice = sliderPrice.getValues().get(0);
        float maxPrice = sliderPrice.getValues().get(1);

        String year = etYear.getText().toString().trim();
        String horsePower = etHorsePower.getText().toString().trim();
        String engineCapacity = etEngineCapacity.getText().toString().trim();

        Intent intent = new Intent();
        intent.putExtra("region", region);
        intent.putExtra("carType", carType);
        intent.putExtra("model", model);
        intent.putExtra("trim", trim);
        intent.putExtra("gearType", gearType);
        intent.putExtra("fuelType", fuelType);
        intent.putExtra("color", color);
        intent.putExtra("doors", doors);
        intent.putExtra("seats", seats);
        intent.putExtra("sunroof", sunroof);
        intent.putExtra("disabled", disabledAccessible);
        intent.putExtra("minPrice", minPrice);
        intent.putExtra("maxPrice", maxPrice);

        if (!TextUtils.isEmpty(year)) intent.putExtra("year", year);
        if (!TextUtils.isEmpty(horsePower)) intent.putExtra("horsePower", horsePower);
        if (!TextUtils.isEmpty(engineCapacity)) intent.putExtra("engineCapacity", engineCapacity);

        setResult(RESULT_OK, intent);
        finish();
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
        makes.add("Any");
        for (CarMake make : carData) {
            makes.add(make.name);
        }

        showSearchableDialog("Choose manufacturer", makes, value -> {
            selectedMake = value;
            selectedModel = "Any";
            selectedTrim = "Any";
            updateSelectedViews();

            if ("Any".equals(value)) {
                return;
            }

            CarMake make = getMakeByName(value);
            if (make != null) {
                showModelDialog(make);
            }
        });
    }

    private void showModelDialog(CarMake make) {
        ArrayList<String> models = new ArrayList<>();
        models.add("Any");
        for (CarModel model : make.models) {
            models.add(model.name);
        }

        showSearchableDialog("Choose model", models, value -> {
            selectedModel = value;
            selectedTrim = "Any";
            updateSelectedViews();

            if ("Any".equals(value)) {
                return;
            }

            CarModel model = getModelByName(make, value);
            if (model != null) {
                showTrimDialog(model);
            }
        });
    }

    private void showTrimDialog(CarModel model) {
        ArrayList<String> trims = new ArrayList<>();
        trims.add("Any");
        trims.addAll(model.trims);

        String[] trimArray = trims.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Choose trim")
                .setItems(trimArray, (dialog, which) -> {
                    selectedTrim = trims.get(which);
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
        if (!"Any".equals(selectedMake) && !selectedMake.isEmpty()) {
            tvChosenMake.setText("Manufacturer: " + selectedMake);
            tvChosenMake.setVisibility(View.VISIBLE);
        } else {
            tvChosenMake.setVisibility(View.GONE);
        }

        if (!"Any".equals(selectedModel) && !selectedModel.isEmpty()) {
            tvChosenModel.setText("Model: " + selectedModel);
            tvChosenModel.setVisibility(View.VISIBLE);
        } else {
            tvChosenModel.setVisibility(View.GONE);
        }

        if (!"Any".equals(selectedTrim) && !selectedTrim.isEmpty()) {
            tvChosenTrim.setText("Trim: " + selectedTrim);
            tvChosenTrim.setVisibility(View.VISIBLE);
        } else {
            tvChosenTrim.setVisibility(View.GONE);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}