package com.audacious_software.pennyworth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;

import com.audacious_software.passive_data_kit.activities.DiagnosticsActivity;
import com.audacious_software.passive_data_kit.activities.DataStreamActivity;

public class MainActivity extends DataStreamActivity {
    private Menu mMenu = null;

    protected int layoutResource() {
        return R.layout.activity_main;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        this.setTitle(R.string.app_name);

        ScheduleManager.getInstance(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final PennyworthApplication app = (PennyworthApplication) this.getApplication();
        final MainActivity me = this;

        String userId = app.getIdentifier();

        if (userId != null) {
            ScheduleManager.getInstance(this).updateSchedule(false, null, false);
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
                        }
                    }, 1000);

                    ScheduleManager.getInstance(me).updateSchedule(true, null, false);
                }
            });
        }

        if (this.mMenu != null) {
            DiagnosticsActivity.setUpDiagnosticsItem(this, this.mMenu, true, true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.menu_main, menu);

        DiagnosticsActivity.setUpDiagnosticsItem(this, menu, true, true);

        this.mMenu = menu;

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_diagnostics) {
            Intent diagnosticsIntent = new Intent(this, DiagnosticsActivity.class);
            this.startActivity(diagnosticsIntent);
        }
        else if (id == R.id.action_data_stream) {
            Intent dataIntent = new Intent(this, DataStreamActivity.class);
            this.startActivity(dataIntent);
        }
        else if (id == R.id.action_transmit_data) {
            ScheduleManager.getInstance(this).transmitData();
        }

        return super.onOptionsItemSelected(item);
    }
}
