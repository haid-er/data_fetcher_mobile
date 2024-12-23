package com.example.data_fetcher_mobile;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datafetchermobile.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SensorAdapter extends RecyclerView.Adapter<SensorAdapter.ViewHolder> {

    private List<ParcelableSensor> sensorList;
    private SparseBooleanArray selectedSensors; // Keep track of selected sensors

    private Set<String> previouslySelected;
    private Context context;

    public SensorAdapter(Context context, List<ParcelableSensor> sensorList, Set<String> previouslySelected) {
        this.context = context;
        this.sensorList = sensorList;
        this.previouslySelected = previouslySelected;
        selectedSensors = new SparseBooleanArray();

        for (int i = 0; i < sensorList.size(); i++) {
            if (previouslySelected.contains(sensorList.get(i).getName())) {
                selectedSensors.put(i, true);
            }
        }

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final ParcelableSensor sensor = sensorList.get(position);
        holder.checkBox.setText(sensor.getName());

        // Set the checkbox state based on the selectedSensors array
        holder.checkBox.setChecked(selectedSensors.get(position));

        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Update the selectedSensors array when a checkbox state changes
                selectedSensors.put(holder.getAdapterPosition(), isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sensorList.size();
    }

    // Add a method to get selected sensors
    public List<ParcelableSensor> getSelectedSensors() {
        List<ParcelableSensor> selected = new ArrayList<>();
        for (int i = 0; i < selectedSensors.size(); i++) {
            if (selectedSensors.valueAt(i)) {
                selected.add(sensorList.get(selectedSensors.keyAt(i)));
            }
        }
        return selected;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox_sensor);
        }
    }
}
