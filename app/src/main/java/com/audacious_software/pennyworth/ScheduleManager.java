package com.audacious_software.pennyworth;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.audacious_software.passive_data_kit.PassiveDataKit;
import com.audacious_software.passive_data_kit.generators.Generators;
import com.audacious_software.passive_data_kit.transmitters.Transmitter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.core.content.ContextCompat;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ScheduleManager implements Generators.GeneratorUpdatedListener {
    private static final String LAST_TRANSMISSION = "com.audacious_software.pennyworth.ScheduleManager.LAST_TRANSMISSION";

    public static final String NOTIFICATION_CHANNEL_ID = "pennyworth";
    private static final String SAVED_CONFIGURATION = "com.audacious_software.pennyworth.ScheduleManager.SAVED_CONFIGURATION";

    @SuppressLint("StaticFieldLeak")
    private static ScheduleManager sInstance = null;
    private PennyworthApplication mApplication = null;

    private Context mContext = null;
    private List<Transmitter> mTransmitters = new ArrayList<>();
    private boolean mFetchingConfig = false;

    public static ScheduleManager getInstance(Context context) {
        if (ScheduleManager.sInstance == null) {
            ScheduleManager.sInstance = new ScheduleManager(context.getApplicationContext());

            Intent fireIntent = new Intent(KeepAliveService.ACTION_KEEP_ALIVE, null, context, KeepAliveService.class);

            fireIntent.putExtra(PassiveDataKit.NOTIFICATION_CHANNEL_ID, ScheduleManager.NOTIFICATION_CHANNEL_ID);

            KeepAliveService.enqueueWork(context, KeepAliveService.class, KeepAliveService.JOB_ID, fireIntent);
        }

        return ScheduleManager.sInstance;
    }

    private ScheduleManager(final Context context) {
        this.mContext  = context.getApplicationContext();
        this.mApplication = (PennyworthApplication) context.getApplicationContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager noteManager = (NotificationManager) this.mContext.getSystemService(Context.NOTIFICATION_SERVICE);

            if (noteManager.getNotificationChannel(ScheduleManager.NOTIFICATION_CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(ScheduleManager.NOTIFICATION_CHANNEL_ID, this.mContext.getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
                channel.setShowBadge(false);

                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

                noteManager.createNotificationChannel(channel);
            }
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);

        SharedPreferences.Editor e = prefs.edit();
        e.remove(ScheduleManager.LAST_TRANSMISSION);
        e.apply();
    }

    public void updateSchedule(boolean force, final Runnable next, final boolean isService) {
        final ScheduleManager me = this;

        final long now = System.currentTimeMillis();

        String userId = this.mApplication.getIdentifier();

        if (userId != null) {
            this.setUserId(userId);
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);

        long lastTransmission = prefs.getLong(ScheduleManager.LAST_TRANSMISSION, 0);

        if (force) {
            lastTransmission = 0;
        }

        long transmissionInterval = Long.parseLong(prefs.getString(SettingsActivity.TRANSMISSION_INTERVAL, SettingsActivity.TRANSMISSION_INTERVAL_DEFAULT));

        if (transmissionInterval > 0 && now - lastTransmission > transmissionInterval) {
            SharedPreferences.Editor e = prefs.edit();
            e.putLong(ScheduleManager.LAST_TRANSMISSION, now);
            e.apply();

            if (isService) {
                me.transmitData();

                AppLogger.getInstance(me.mContext).log("schedule_manager_transmit_data_via_service");
            } else {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(3000);

                            me.transmitData();

                            AppLogger.getInstance(me.mContext).log("schedule_manager_transmit_data");

                            Handler mainHandler = new Handler(Looper.getMainLooper());

                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (next != null) {
                                        next.run();
                                    }
                                }
                            });
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                });

                t.start();
            }
        }
    }

    public void transmitData() {
        for (Transmitter transmitter : this.mTransmitters) {
            transmitter.transmit(true);
        }
    }

    private void start(final String userId) {
        PassiveDataKit pdkInstance = PassiveDataKit.getInstance(this.mContext);
        pdkInstance.setAlwaysNotify(true);
        pdkInstance.setStartForegroundService(true);
        pdkInstance.setForegroundServiceChannelId(ScheduleManager.NOTIFICATION_CHANNEL_ID);
        pdkInstance.setForegroundServiceIcon(R.drawable.ic_notification);
        pdkInstance.setForegroundServiceColor(ContextCompat.getColor(this.mContext, R.color.colorNotification));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);

        try {
            JSONObject config = new JSONObject(prefs.getString(ScheduleManager.SAVED_CONFIGURATION, null));

            this.mTransmitters.addAll(pdkInstance.fetchTransmitters(userId, "Pennyworth", config));

            pdkInstance.updateGenerators(config);

            pdkInstance.start();

            Generators.getInstance(this.mContext).addNewGeneratorUpdatedListener(this);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setUserId(final String userId) {
        final ScheduleManager me = this;

        if (userId != null && this.mTransmitters.size() == 0 && this.mFetchingConfig == false) {
            this.mFetchingConfig = true;

            Log.e("Pennyworth", "Fetching online config and initializing.");

            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(me.mContext.getString(R.string.url_pennyworth_configuration, userId))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    me.mFetchingConfig = false;
                    Log.e("Pennyworth", "Unable to fetch online config. Falling back to last fetched config.");
                    me.start(userId);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        me.mFetchingConfig = false;

                        Log.e("Pennyworth", "Fetched online config.");
                        JSONObject config = new JSONObject(response.body().string());

                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(me.mContext);
                        SharedPreferences.Editor e = prefs.edit();
                        e.putString(ScheduleManager.SAVED_CONFIGURATION, config.toString(2));
                        e.apply();

                        me.start(userId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

            AppLogger.getInstance(me.mContext).log("schedule_manager_inited");
        }
    }

    @Override
    public void onGeneratorUpdated(String identifier, long timestamp, Bundle data) {
        Log.e("Pennyworth", "DATA[" + identifier + "] = " + data.toString());

        this.updateSchedule(false, null, false);
    }
}
