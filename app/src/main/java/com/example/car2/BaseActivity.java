package com.example.car2;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import java.util.ArrayList;
import java.util.Map;

public class BaseActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "car2_chat_foreground";
    private static final int REQ_NOTIF = 900;

    private static final String PREFS = "car2_prefs";
    private static final String KEY_LAST_NOTIF_PREFIX = "last_notif_";

    private static ListenerRegistration globalChatsListener = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // ✅ قفل كامل بدون إنترنت
        if (!isInternetAvailable() && !getClass().equals(NoInternet.class)) {
            Intent i = new Intent(this, NoInternet.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            return;
        }

        createNotifChannel();
        requestNotifPermissionIfNeeded();
        startGlobalChatListener();
    }
    private void startGlobalChatListener() {
        String myId = FirebaseAuth.getInstance().getUid();
        if (myId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        stopGlobalChatListener();

        globalChatsListener = db.collection("chats")
                .whereArrayContains("users", myId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    for (var doc : snap.getDocuments()) {
                        String chatId = doc.getId();

                        String lastMessage = doc.getString("lastMessage");
                        String lastSenderId = doc.getString("lastSenderId");
                        Timestamp lastMessageTime = doc.getTimestamp("lastMessageTime");

                        if (lastMessageTime == null) continue;
                        if (lastSenderId == null) continue;

                        // ✅ إذا أنا آخر واحد بعت -> لا إشعار
                        if (lastSenderId.equals(myId)) continue;

                        // ✅ إذا أنا داخل نفس الشات -> لا إشعار
                        if (ChatActivity.OPEN_CHAT_ID != null && ChatActivity.OPEN_CHAT_ID.equals(chatId)) continue;

                        Timestamp myLastRead = null;
                        Object lrObj = doc.get("lastRead");
                        if (lrObj instanceof Map) {
                            Map<String, Object> lr = (Map<String, Object>) lrObj;
                            Object t = lr.get(myId);
                            if (t instanceof Timestamp) myLastRead = (Timestamp) t;
                        }

                        // ✅ إذا مقروء -> لا إشعار
                        if (myLastRead != null && lastMessageTime.compareTo(myLastRead) <= 0) continue;

                        // ✅ منع تكرار نفس الإشعار (حتى لو فتحت/سكرت شاشات)
                        long msgTimeMs = lastMessageTime.toDate().getTime();
                        long lastShown = getSharedPreferences(PREFS, MODE_PRIVATE)
                                .getLong(KEY_LAST_NOTIF_PREFIX + chatId, 0L);

                        if (msgTimeMs <= lastShown) continue;

                        // خزّن قبل العرض
                        getSharedPreferences(PREFS, MODE_PRIVATE)
                                .edit()
                                .putLong(KEY_LAST_NOTIF_PREFIX + chatId, msgTimeMs)
                                .apply();

                        // ✅ احسب otherId عشان نفتح ChatActivity صح
                        ArrayList<String> users = (ArrayList<String>) doc.get("users");
                        String otherId = null;
                        if (users != null) {
                            for (String u : users) {
                                if (u != null && !u.equals(myId)) {
                                    otherId = u;
                                    break;
                                }
                            }
                        }
                        if (otherId == null) continue;

                        String body = (lastMessage != null && !lastMessage.trim().isEmpty())
                                ? lastMessage
                                : "You have a new message";

                        final String finalOtherId = otherId;
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(otherId)
                                .get()
                                .addOnSuccessListener(uDoc -> {
                                    final String senderName=uDoc.getString("name");
                                    showChatNotification(chatId, finalOtherId, senderName, body);
                                });
                    }
                });
    }

    private void stopGlobalChatListener() {
        if (globalChatsListener != null) {
            globalChatsListener.remove();
            globalChatsListener = null;
        }
    }

    private void showChatNotification(String chatId, String otherUserId, String title, String body) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("sellerId", otherUserId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                this,
                7000 + chatId.hashCode(),
                intent,
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(7000 + chatId.hashCode(), b.build());
    }

    private void createNotifChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Chat messages (foreground)",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
    }

    private void requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_NOTIF
                );
            }
        }
    }

    // ================== Your System Bars Code (unchanged) ==================
    protected void applySystemBars() {

        Window window = getWindow();

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        WindowCompat.setDecorFitsSystemWindows(window, false);

        int statusColor = ContextCompat.getColor(this, R.color.my_status_bar);
        window.setStatusBarColor(statusColor);

        window.setNavigationBarColor(Color.TRANSPARENT);

        View decor = window.getDecorView();
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, decor);

        controller.hide(WindowInsetsCompat.Type.navigationBars());
        controller.show(WindowInsetsCompat.Type.statusBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        controller.setAppearanceLightStatusBars(true);

        View content = findViewById(android.R.id.content);
        if (content != null) {
            ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                v.setPadding(0, top, 0, imeBottom);
                return insets;
            });

            ViewCompat.requestApplyInsets(content);
        }

        View bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), 0);
                return WindowInsetsCompat.CONSUMED;
            });

            ViewCompat.requestApplyInsets(bottomNav);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applySystemBars();
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;

            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            if (caps == null) return false;

            boolean hasTransport =
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);

            // NOTE: هذا يعني “في اتصال شبكة”، مش ضمان 100% أنه في إنترنت فعلي
            return hasTransport;
        } else {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }

}
