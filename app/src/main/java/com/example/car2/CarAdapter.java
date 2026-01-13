package com.example.car2;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> {

    private Context context;
    private ArrayList<Car> cars;

    public CarAdapter(Context context, ArrayList<Car> cars) {
        this.context = context;
        this.cars = cars;
    }

    @NonNull
    @Override
    public CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.car_item, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarViewHolder holder, int position) {
        Car car = cars.get(position);

        // Load first image safely
        if (car.getImages() != null && !car.getImages().isEmpty() && car.getImages().get(0) != null) {
            Glide.with(context).load(car.getImages().get(0)).into(holder.imgCar);
        }

        holder.txtType.setText(car.getType() != null ? car.getType() : "");
        holder.txtPrice.setText(car.getPrice() != null ? car.getPrice() : "");

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CarDetailsActivity.class);
            intent.putExtra("type", car.getType());
            intent.putExtra("price", car.getPrice()+"$");
            intent.putStringArrayListExtra("images", new ArrayList<>(car.getImages()));
            intent.putStringArrayListExtra("details", new ArrayList<>(car.getDetails()));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return cars.size();
    }

    public static class CarViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCar;
        TextView txtType, txtPrice;

        public CarViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCar = itemView.findViewById(R.id.imgCar);
            txtType = itemView.findViewById(R.id.txtType);
            txtPrice = itemView.findViewById(R.id.txtPrice);
        }
    }
}
