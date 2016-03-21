package com.audacious_software.pennyworth;

import android.os.Bundle;

import com.audacious_software.passive_data_kit.activities.PdkActivity;

public class MainActivity extends PdkActivity
{
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_main);
    }
}
