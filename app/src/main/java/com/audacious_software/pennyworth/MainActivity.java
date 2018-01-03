package com.audacious_software.pennyworth;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.audacious_software.passive_data_kit.PassiveDataKit;
import com.audacious_software.passive_data_kit.activities.DiagnosticsActivity;
import com.audacious_software.passive_data_kit.activities.DataStreamActivity;
import com.audacious_software.passive_data_kit.activities.PdkActivity;
import com.audacious_software.passive_data_kit.generators.wearables.WithingsDevice;
import com.audacious_software.passive_data_kit.transmitters.HttpTransmitter;

import net.hockeyapp.android.CrashManager;

import java.util.HashMap;

public class MainActivity extends PdkActivity
{
    private HttpTransmitter mTransmitter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_main);

        WithingsDevice.getInstance(this).setProperty(WithingsDevice.OPTION_OAUTH_CONSUMER_KEY, this.getString(R.string.withings_api_key));
        WithingsDevice.getInstance(this).setProperty(WithingsDevice.OPTION_OAUTH_CONSUMER_SECRET, this.getString(R.string.withings_api_secret));
        WithingsDevice.getInstance(this).setProperty(WithingsDevice.OPTION_OAUTH_CALLBACK_URL, "pdk://pennyworth/oauth/withings");
//        WithingsDevice.getInstance(this).setProperty(WithingsDevice.OPTION_OAUTH_CALLBACK_URL, "https://pennyworthproject.org/foo");

        PassiveDataKit.getInstance(this).start();

//        Generators.getInstance(this).addNewDataPointListener(new Generators.NewDataPointListener() {
//            @Override
//            public void onNewDataPoint(String identifier, Bundle data) {
//                Log.e("Pennyworth", "DATA[" + identifier + "] = " + data.toString());
//            }
//        });

        CrashManager.register(this, this.getString(R.string.hockeyapp_api_key));

        this.mTransmitter = new HttpTransmitter();

        HashMap<String, String> options = new HashMap<>();
        options.put(HttpTransmitter.UPLOAD_URI, "http://pdk.audacious-software.com/data/add-bundle.json");
        options.put(HttpTransmitter.USER_ID, "pennyworth-user");
        options.put(HttpTransmitter.WIFI_ONLY, "false");
        options.put(HttpTransmitter.CHARGING_ONLY, "false");
        options.put(HttpTransmitter.USE_EXTERNAL_STORAGE, "true");

        this.mTransmitter.initialize(this, options);
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
        else if (id == R.id.action_transmit_data) {
            this.mTransmitter.transmit(true);
        }

        return super.onOptionsItemSelected(item);
    }
}
