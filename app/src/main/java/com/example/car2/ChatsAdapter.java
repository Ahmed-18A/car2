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

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.VH> {

    private final Context context;
    private final ArrayList<ChatItem> list;

    public ChatsAdapter(Context context, ArrayList<ChatItem> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.chat_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChatItem c = list.get(position);

        // ===== الاسم =====
        h.txtName.setText(
                c.otherUserName != null ? c.otherUserName : "User"
        );

        if (c.otherUserImage != null && !c.otherUserImage.trim().isEmpty()) {
            Glide.with(context)
                    .load(c.otherUserImage)
                    .circleCrop()
                    .placeholder(R.drawable.user2)
                    .error(R.drawable.user2)
                    .into(h.imgUser);
        } else {
            h.imgUser.setImageResource(R.drawable.user2);
        }

        h.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("sellerId", c.otherUserId);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ================= ViewHolder =================
    static class VH extends RecyclerView.ViewHolder {
        ImageView imgUser;
        TextView txtName;

        VH(@NonNull View itemView) {
            super(itemView);
            imgUser = itemView.findViewById(R.id.imgUser);
            txtName = itemView.findViewById(R.id.txtName);
        }
    }
}
