package com.audacious_software.pennyworth;

import android.app.Activity;
import android.app.Application;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.audacious_software.passive_data_kit.Toolbox;
import com.github.anrwatchdog.ANRError;
import com.github.anrwatchdog.ANRWatchDog;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;
import net.hockeyapp.android.ExceptionHandler;

import androidx.appcompat.app.AlertDialog;

public class PennyworthApplication extends Application {
    private static final String IDENTIFIER = "com.audacious_software.pennyworth.IDENTIFIER";

    @Override
    public void onCreate() {
        super.onCreate();

        HandlerThread thread = new HandlerThread("app-background-tasks");
        thread.start();

        CrashManager.register(this, this.getString(R.string.hockeyapp_api_key), new CrashManagerListener() {
            @Override
            public boolean shouldAutoUploadCrashes() {
                return true;
            }
        });

        new ANRWatchDog().setIgnoreDebugger(true).setANRListener(new ANRWatchDog.ANRListener() {
            @Override
            public void onAppNotResponding(final ANRError error) {
                // Handle the error. For example, log it to HockeyApp:
                ExceptionHandler.saveException(error, Thread.currentThread(), new CrashManagerListener() {
                    public boolean shouldAutoUploadCrashes() {
                        return true;
                    }

                    public void onCrashesSent() {

                    }
                });
            }
        }).start();
    }

    public String getIdentifier() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        return prefs.getString(PennyworthApplication.IDENTIFIER, null);
    }

    public void setIdentifier(String identifier) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor e = prefs.edit();
        e.putString(PennyworthApplication.IDENTIFIER, identifier);
        e.apply();
    }

    public void promptForIdentifier(final Activity activity, final Runnable next) {
        final PennyworthApplication me = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);

        builder.setTitle(R.string.dialog_title_prompt_identifier);

        View content = LayoutInflater.from(this).inflate(R.layout.dialog_prompt_identifier, null, false);

        final EditText promptValue = content.findViewById(R.id.prompt_identifier);
        promptValue.setText(this.getIdentifier());

        builder.setView(content);

        builder.setPositiveButton(R.string.action_continue, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                final String value = Toolbox.toSlug(promptValue.getText().toString().trim());

                if (value.length() > 0) {
                    me.setIdentifier(value);

                    ScheduleManager.getInstance(me).setUserId(value);

                    if (next != null) {
                        next.run();
                    }
                } else {
                    me.promptForIdentifier(activity, next);
                }
            }
        });

        builder.create().show();
    }
}
