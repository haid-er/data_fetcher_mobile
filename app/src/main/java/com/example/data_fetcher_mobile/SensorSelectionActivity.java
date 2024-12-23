package com.example.data_fetcher_mobile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;// New
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datafetchermobile.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SensorSelectionActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_SENSORS = "selected_sensors";
    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_SELECTED_SENSORS = "selected_sensors";

    private SensorAdapter sensorAdapter;
    private List<ParcelableSensor> sensorList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_selection);

        RecyclerView recyclerView = findViewById(R.id.recycler_view_sensors);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Get the list of available sensors
        sensorList = getAvailableSensors();

        // Load previously selected sensors
        Set<String> previouslySelected = loadSelectedSensors(); // Ensure saved sensors are loaded

        // Initialize the adapter with previously selected sensors
        sensorAdapter = new SensorAdapter(this, sensorList, previouslySelected);
        recyclerView.setAdapter(sensorAdapter);

        findViewById(R.id.button_ok).setOnClickListener(v -> {
            List<ParcelableSensor> selectedSensors = sensorAdapter.getSelectedSensors();
            if (selectedSensors.isEmpty()) {
                Toast.makeText(SensorSelectionActivity.this, "Please select at least one sensor", Toast.LENGTH_SHORT).show();
            } else {
                saveSelectedSensors(selectedSensors); // Persist selected sensors immediately
                Intent resultIntent = new Intent();
                resultIntent.putParcelableArrayListExtra(EXTRA_SELECTED_SENSORS, new ArrayList<>(selectedSensors)); // Pass sensors back
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    private List<ParcelableSensor> getAvailableSensors() {
        List<ParcelableSensor> sensors = new ArrayList<>();
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);

        for (Sensor sensor : sensorList) {
            ParcelableSensor parcelableSensor = new ParcelableSensor(sensor.getType(), sensor.getName());
            sensors.add(parcelableSensor);
        }
        return sensors;
    }

    private Set<String> loadSelectedSensors() { // Ensure persistence of selected sensors
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return preferences.getStringSet(KEY_SELECTED_SENSORS, new HashSet<>());
    }

    private void saveSelectedSensors(List<ParcelableSensor> sensors) { // Save selected sensors persistently
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        Set<String> sensorNames = new HashSet<>();
        for (ParcelableSensor sensor : sensors) {
            sensorNames.add(sensor.getName());
        }

        editor.putStringSet(KEY_SELECTED_SENSORS, sensorNames);
        editor.apply();
    }
}
