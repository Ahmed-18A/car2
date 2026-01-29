package com.example.car2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;

public class ChatsActivity extends BaseActivity {

    BottomNavigationView bottomNav;

    RecyclerView rvChats;
    ArrayList<ChatItem> chats = new ArrayList<>();
    ChatsAdapter adapter;

    FirebaseFirestore db;
    String myId;

    private ListenerRegistration chatsListener;

    private final HashMap<String, ChatItem> chatsMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats);

        applySystemBars();

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.getMenu().getItem(0).setChecked(false);
        bottomNav.getMenu().setGroupCheckable(0, false, true);

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.mnu_profile) {
                startActivity(new Intent(ChatsActivity.this, profile.class));
                finish();
            } else if (item.getItemId() == R.id.mnu_add) {
                startActivity(new Intent(ChatsActivity.this, addCar.class));
                finish();
            } else if (item.getItemId() == R.id.mnu_myC) {
                startActivity(new Intent(ChatsActivity.this, MyCars.class));
                finish();
            } else if (item.getItemId() == R.id.mnu_dash) {
                startActivity(new Intent(ChatsActivity.this, dashboard.class));
                finish();
            }
            return true;
        });

        rvChats = findViewById(R.id.rvChats);
        rvChats.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatsAdapter(this, chats);
        rvChats.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();

        myId = FirebaseAuth.getInstance().getUid();
        if (myId == null) {
            Toast.makeText(this, "مش مسجّل دخول", Toast.LENGTH_SHORT).show();
            return;
        }

        startChatsListener();
    }

    private void startChatsListener() {
        if (chatsListener != null) {
            chatsListener.remove();
            chatsListener = null;
        }

        chatsMap.clear();
        chats.clear();
        adapter.notifyDataSetChanged();

        chatsListener = db.collection("chats")
                .whereArrayContains("users", myId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) return;
                    if (snap == null) return;

                    for (var doc : snap.getDocuments()) {

                        ArrayList<String> users = (ArrayList<String>) doc.get("users");
                        if (users == null) continue;

                        String otherId = null;
                        for (String u : users) {
                            if (u != null && !u.equals(myId)) {
                                otherId = u;
                                break;
                            }
                        }
                        if (otherId == null) continue;

                        String chatId = doc.getId();

                        ChatItem item = chatsMap.get(chatId);
                        if (item == null) item = new ChatItem();

                        item.chatId = chatId;
                        item.otherUserId = otherId;

                        chatsMap.put(chatId, item);

                        if (item.otherUserName == null || item.otherUserName.trim().isEmpty()) {
                            loadUserDataIntoMap(chatId, otherId);
                        }
                    }

                    refreshChatsListFromMap();
                });
    }

    private void loadUserDataIntoMap(String chatId, String otherUserId) {
        db.collection("users").document(otherUserId)
                .get()
                .addOnSuccessListener(d -> {
                    ChatItem item = chatsMap.get(chatId);
                    if (item == null) return;

                    String name = d.getString("name");
                    if (name == null || name.trim().isEmpty()) name = "User";

                    item.otherUserName = name;
                    item.otherUserImage = d.getString("profileImage");

                    chatsMap.put(chatId, item);
                    refreshChatsListFromMap();
                })
                .addOnFailureListener(err -> {});
    }

    private void refreshChatsListFromMap() {
        chats.clear();
        chats.addAll(chatsMap.values());
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (chatsListener != null) {
            chatsListener.remove();
            chatsListener = null;
        }

        chatsMap.clear();
        chats.clear();
    }
}
