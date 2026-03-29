package com.example.car2;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String REMINDER_CHANNEL_ID = "car2_reminder_channel";
    private static final String REMINDER_CHANNEL_NAME = "Car2 reminders";

    private static final String PREFS = "car2_prefs";
    private static final String KEY_LAST_APP_OPEN = "last_app_open";

    private static final String REMINDER_ACTION = "com.example.car2.REMINDER_72_HOURS";
    private static final int REMINDER_REQUEST_CODE = 3333;

    private static final long REMINDER_DELAY_MS = 72L * 60L * 60L * 1000L;

    @Override
    public void onReceive(Context context, Intent intent) {
        createReminderChannel(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long lastOpen = sp.getLong(KEY_LAST_APP_OPEN, 0L);
        long now = System.currentTimeMillis();

        // إذا المستخدم فتح التطبيق مؤخرًا، لا ترسل
        if (lastOpen > 0 && now - lastOpen < REMINDER_DELAY_MS) {
            scheduleNextReminder(context, REMINDER_DELAY_MS - (now - lastOpen));
            return;
        }

        Class<?> targetActivity;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            targetActivity = dashboard.class;
        } else {
            targetActivity = log_in.class;
        }

        Intent openIntent = new Intent(context, targetActivity);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                9001,
                openIntent,
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Reminder")
                .setContentText("Open Car2 and check the new cars")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Open Car2 and check the new cars"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (nm != null) {
            nm.notify(9001, builder.build());
        }

        // مهم: بعد أول إشعار، احجز الإشعار القادم كمان
        scheduleNextReminder(context, REMINDER_DELAY_MS);
    }

    private void scheduleNextReminder(Context context, long delayMs) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(REMINDER_ACTION);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REMINDER_REQUEST_CODE,
                intent,
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        alarmManager.cancel(pendingIntent);

        long triggerAtMillis = System.currentTimeMillis() + delayMs;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        }
    }

    private void createReminderChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    REMINDER_CHANNEL_ID,
                    REMINDER_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }
}