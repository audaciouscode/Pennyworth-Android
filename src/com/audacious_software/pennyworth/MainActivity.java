package com.audacious_software.pennyworth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.audacious_software.passive_data_kit.PassiveDataKit;
import com.audacious_software.passive_data_kit.activities.DiagnosticsActivity;
import com.audacious_software.passive_data_kit.activities.DataStreamActivity;
import com.audacious_software.passive_data_kit.activities.PdkActivity;
import com.audacious_software.passive_data_kit.generators.Generators;

public class MainActivity extends PdkActivity
{
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_main);

        PassiveDataKit.getInstance(this).start();

        Generators.getInstance(this).addNewDataPointListener(new Generators.NewDataPointListener() {
            @Override
            public void onNewDataPoint(String identifier, Bundle data) {
                Log.e("Pennyworth", "DATA[" + identifier + "] = " + data.toString());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        this.getMenuInflater().inflate(R.menu.menu_main, menu);

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

        return super.onOptionsItemSelected(item);
    }
}
