package com.example.car2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

public class ChatsActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;

    RecyclerView rvChats;
    ArrayList<ChatItem> chats = new ArrayList<>();
    ChatsAdapter adapter;

    FirebaseFirestore db;
    String myId;

    private ListenerRegistration chatsListener; // ✅ مهم

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats);

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
        // ✅ لو كان في listener قديم، سكّره
        if (chatsListener != null) {
            chatsListener.remove();
            chatsListener = null;
        }

        chatsListener = db.collection("chats")
                .whereArrayContains("users", myId)
                .addSnapshotListener((snap, e) -> {

                    if (e != null) {
                        // لو عمل sign out أو صار auth null، ممكن يظهر Permission Denied
                        // ما نعملش توست مزعج
                        return;
                    }
                    if (snap == null) return;

                    chats.clear();
                    adapter.notifyDataSetChanged();

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

                        ChatItem item = new ChatItem();
                        item.chatId = doc.getId();
                        item.otherUserId = otherId;

                        loadUserData(item);
                    }
                });
    }

    private void loadUserData(ChatItem item) {
        db.collection("users").document(item.otherUserId)
                .get()
                .addOnSuccessListener(d -> {

                    String name = d.getString("name");
                    if (name == null || name.trim().isEmpty()) name = "User";

                    item.otherUserName = name;

                    // ✅ عندك اسم الحقل profileImage
                    item.otherUserImage = d.getString("profileImage");

                    chats.add(item);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(err -> {
                    // إذا users rules مانعة القراءة رح يفشل هون
                    // خليه صامت أو اعرض رسالة لو بدك:
                    // Toast.makeText(this, "User read denied", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onStop() {
        super.onStop();

        // ✅ سكّر الليستنر عشان ما يطلع Permission Denied بعد Sign out
        if (chatsListener != null) {
            chatsListener.remove();
            chatsListener = null;
        }
    }
}
