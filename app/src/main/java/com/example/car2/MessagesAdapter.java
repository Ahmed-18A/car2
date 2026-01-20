package com.example.car2;

import android.graphics.Color;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.VH> {

    private final ArrayList<Message> list;
    private final String myId;

    public MessagesAdapter(ArrayList<Message> list, String myId) {
        this.list = list;
        this.myId = myId;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Message m = list.get(position);

        String text = (m.getText() == null) ? "" : m.getText();
        h.txtMessage.setText(text);

        // خلي الروابط قابلة للضغط (خصوصاً روابط الخرائط)
        h.txtMessage.setAutoLinkMask(Linkify.WEB_URLS);
        h.txtMessage.setLinksClickable(true);
        h.txtMessage.setMovementMethod(LinkMovementMethod.getInstance());

        boolean isMe = m.getSenderId() != null && m.getSenderId().equals(myId);

        // يمين / يسار
        h.container.setGravity(isMe ? Gravity.END : Gravity.START);

        // لون الفقاعة
        h.cardBubble.setCardBackgroundColor(
                isMe ? Color.parseColor("#dbf2ff") : Color.WHITE
        );
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout container;
        CardView cardBubble;
        TextView txtMessage;

        VH(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.container);
            cardBubble = itemView.findViewById(R.id.cardBubble);
            txtMessage = itemView.findViewById(R.id.txtMessage);
        }
    }
}
