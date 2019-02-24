package com.audacious_software.pennyworth.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.audacious_software.passive_data_kit.activities.DiagnosticsActivity;
import com.audacious_software.passive_data_kit.generators.device.ForegroundApplication;
import com.audacious_software.pennyworth.PennyworthApplication;
import com.audacious_software.pennyworth.R;
import com.audacious_software.pennyworth.ScheduleManager;

import net.hockeyapp.android.UpdateManager;

import org.apache.commons.lang3.time.DurationFormatUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AppsUsedActivity extends AppCompatActivity {
    private RecyclerView mAppsList = null;
    private LinearLayoutManager mLayoutManager;

    private ArrayList<ForegroundApplication.ForegroundApplicationUsage> mAppsUsed = new ArrayList<>();
    private AppsAdapter mAdapter = null;
    private Menu mMenu = null;

    private Handler mHandler;
    private Runnable mRefreshRunnable = null;

    enum AppPeriod {
        DAY,
        WEEK,
        ALL
    }

    private AppPeriod mCurrentPeriod = AppPeriod.DAY;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_apps_used);
        this.getSupportActionBar().setTitle(R.string.app_name);

        this.mAppsList = this.findViewById(R.id.apps_list);
        this.mAppsList.setHasFixedSize(true);

        this.mLayoutManager = new LinearLayoutManager(this);
        this.mAppsList.setLayoutManager(mLayoutManager);

        this.mAdapter = new AppsAdapter(this.mAppsUsed);
        this.mAppsList.setAdapter(this.mAdapter);

        final AppsUsedActivity me = this;

        this.mRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (me.mMenu != null) {
                    DiagnosticsActivity.setUpDiagnosticsItem(me, me.mMenu, true, true);
                }

                me.refreshApps(me.mCurrentPeriod);

                me.mHandler.postDelayed(this, 10000);
            }
        };

        this.mHandler = new Handler(Looper.getMainLooper());
    }

    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (this.mMenu != null) {
            DiagnosticsActivity.setUpDiagnosticsItem(this, this.mMenu, true, true);
        }

        PennyworthApplication app = (PennyworthApplication) this.getApplication();
        final AppsUsedActivity me = this;

        String userId = app.getIdentifier();

        if (userId != null) {
            ScheduleManager.getInstance(this).setUserId(userId);
        } else {
            app.promptForIdentifier(this, new Runnable() {
                @Override
                public void run() {
                    Handler handler = new Handler(Looper.getMainLooper());

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (me.mMenu != null) {
                                DiagnosticsActivity.setUpDiagnosticsItem(me, me.mMenu, true, true);
                            }
                            me.refreshApps(me.mCurrentPeriod);
                        }
                    }, 1000);
                }
            });
        }

        this.mHandler.postDelayed(this.mRefreshRunnable, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();

        this.mHandler.removeCallbacks(this.mRefreshRunnable);
    }

    private void refreshApps(AppPeriod period) {
        this.mCurrentPeriod = period;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long when = cal.getTimeInMillis();

        if (this.mCurrentPeriod == AppPeriod.WEEK) {
            cal.add(Calendar.DATE, -6);

            when = cal.getTimeInMillis();

            this.getSupportActionBar().setSubtitle(R.string.subtitle_app_usage_week);
        } else if (this.mCurrentPeriod == AppPeriod.ALL){
            when = 0;
            this.getSupportActionBar().setSubtitle(R.string.subtitle_app_usage_all);
        } else {
            this.getSupportActionBar().setSubtitle(R.string.subtitle_app_usage_day);
        }

        HashMap<String, ForegroundApplication.ForegroundApplicationUsage> totals = new HashMap<>();

        this.mAppsUsed.clear();

        List<ForegroundApplication.ForegroundApplicationUsage> usages = ForegroundApplication.getInstance(this).fetchUsagesBetween(when, System.currentTimeMillis(), true);

        for (ForegroundApplication.ForegroundApplicationUsage usage : usages) {
            ForegroundApplication.ForegroundApplicationUsage total = null;

            if (totals.containsKey(usage.packageName)) {
                total = totals.get(usage.packageName);
            } else {
                total = new ForegroundApplication.ForegroundApplicationUsage();

                total.packageName = usage.packageName;
                total.duration = 0;
                total.start = usage.start;

                totals.put(usage.packageName, total);

                this.mAppsUsed.add(total);
            }

            if (total.start > usage.start) {
                total.start = usage.start;
            }

            total.duration += usage.duration;
        }

        Collections.sort(this.mAppsUsed, new Comparator<ForegroundApplication.ForegroundApplicationUsage>() {
            @Override
            public int compare(ForegroundApplication.ForegroundApplicationUsage one, ForegroundApplication.ForegroundApplicationUsage two) {
                if (one.duration < two.duration) {
                    return 1;
                } else if (one.duration > two.duration) {
                    return -1;
                }

                return 0;
            }
        });

        this.mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.menu_apps_used, menu);

        this.mMenu = menu;

        DiagnosticsActivity.setUpDiagnosticsItem(this, this.mMenu, true, true);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        final AppsUsedActivity me = this;

        if (DiagnosticsActivity.diagnosticItemSelected(this, item)) {

        } else if (id == R.id.action_change_filter) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.title_select_duration);

            String[] items = {
                    this.getString(R.string.duration_day),
                    this.getString(R.string.duration_week),
                    this.getString(R.string.duration_all),
            };

            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (i == 0) {
                        me.refreshApps(AppPeriod.DAY);
                    } else if (i == 1) {
                        me.refreshApps(AppPeriod.WEEK);
                    } else if (i == 2) {
                        me.refreshApps(AppPeriod.ALL);
                    }
                }
            });

            builder.create().show();
        } else if (id == R.id.action_transmit_data) {
            ScheduleManager.getInstance(this).transmitData();
        }

        return super.onOptionsItemSelected(item);
    }

    public static class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.AppViewHolder> {
        private ArrayList<ForegroundApplication.ForegroundApplicationUsage> mAppsList;

        public static class AppViewHolder extends RecyclerView.ViewHolder {
            public CardView mCardView;

            public AppViewHolder(CardView v) {
                super(v);

                this.mCardView = v;
            }
        }

        public AppsAdapter(ArrayList<ForegroundApplication.ForegroundApplicationUsage> list) {
            this.mAppsList = list;
        }

        @Override
        public AppsAdapter.AppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            CardView card = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.card_app_used, parent, false);

            return new AppViewHolder(card);
        }

        @Override
        public void onBindViewHolder(final AppViewHolder holder, int position) {
            ForegroundApplication.ForegroundApplicationUsage usage = this.mAppsList.get(position);

            Context context = holder.mCardView.getContext();

            TextView appName = holder.mCardView.findViewById(R.id.app_name);
            ImageView appIcon = holder.mCardView.findViewById(R.id.app_icon);

            try {
                PackageManager packageManager = context.getPackageManager();
                ApplicationInfo info = packageManager.getApplicationInfo(usage.packageName, PackageManager.GET_META_DATA);

                appName.setText(context.getString(R.string.numbered_list_app, holder.getAdapterPosition() + 1, packageManager.getApplicationLabel(info)));

                appIcon.setImageDrawable(packageManager.getApplicationIcon(usage.packageName));
            } catch (PackageManager.NameNotFoundException e) {
                appName.setText(usage.packageName);
            }

            TextView appDuration = holder.mCardView.findViewById(R.id.app_used_duration);
            appDuration.setText(DurationFormatUtils.formatDurationWords(usage.duration, true, true));
        }

        @Override
        public int getItemCount() {
            int size = this.mAppsList.size();

            if (size > 20) {
                size = 20;
            }

            return size;
        }
    }
}
