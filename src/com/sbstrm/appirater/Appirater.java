package com.sbstrm.appirater;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

/*	
 * @source https://github.com/sbstrm/appirater-android
 * @license MIT/X11
 * 
 * Copyright (c) 2011-2013 sbstrm Y.K.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

public class Appirater {

    private static final String PREF_LAUNCH_COUNT = "launch_count";
    private static final String PREF_EVENT_COUNT = "event_count";
    private static final String PREF_RATE_CLICKED = "rateclicked";
    private static final String PREF_DONT_SHOW = "dontshow";
    private static final String PREF_DATE_REMINDER_PRESSED = "date_reminder_pressed";
    private static final String PREF_DATE_FIRST_LAUNCHED = "date_firstlaunch";
    private static final String PREF_APP_VERSION_CODE = "versioncode";

    public static void appLaunched(Context context) {
        appLaunched(context, -1, -1, -1, -1);
    }

    public static void appLaunched(Context context,
                                   int daysUntilPrompt,
                                   int launchesUntilPrompt,
                                   int eventsUntilPrompt,
                                   int daysBeforeReminding) {
        int appiratorDaysUntilPrompt = daysUntilPrompt;
        int appiratorLaunchesUntilPrompt = launchesUntilPrompt;
        int appiratorEventsUntilPrompt = eventsUntilPrompt;
        int appiratorDaysBeforeReminding = daysBeforeReminding;
        if (appiratorDaysUntilPrompt < 0) {
            appiratorDaysUntilPrompt = context.getResources().getInteger(R.integer.appirator_days_until_prompt);
        }
        if (appiratorLaunchesUntilPrompt < 0) {
            appiratorLaunchesUntilPrompt = context.getResources().getInteger(R.integer.appirator_launches_until_prompt);
        }
        if (appiratorEventsUntilPrompt < 0) {
            appiratorEventsUntilPrompt = context.getResources().getInteger(R.integer.appirator_events_until_prompt);
        }
        if (appiratorDaysBeforeReminding < 0) {
            appiratorDaysBeforeReminding = context.getResources().getInteger(R.integer.appirator_days_before_reminding);
        }
        boolean testMode = context.getResources().getBoolean(R.bool.appirator_test_mode);
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + ".appirater", 0);
        if (!testMode && (prefs.getBoolean(PREF_DONT_SHOW, false) || prefs.getBoolean(PREF_RATE_CLICKED, false))) {
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();

        if (testMode) {
            showRateDialog(context, editor);
            return;
        }

        // Increment launch counter
        long launch_count = prefs.getLong(PREF_LAUNCH_COUNT, 0);

        // Get events counter
        long event_count = prefs.getLong(PREF_EVENT_COUNT, 0);

        // Get date of first launch
        long date_firstLaunch = prefs.getLong(PREF_DATE_FIRST_LAUNCHED, 0);

        // Get reminder date pressed
        long date_reminder_pressed = prefs.getLong(PREF_DATE_REMINDER_PRESSED, 0);

        try {
            int appVersionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            if (prefs.getInt(PREF_APP_VERSION_CODE, 0) != appVersionCode) {
                //Reset the launch and event counters to help assure users are rating based on the latest version.
                launch_count = 0;
                event_count = 0;
                editor.putLong(PREF_EVENT_COUNT, event_count);
            }
            editor.putInt(PREF_APP_VERSION_CODE, appVersionCode);
        } catch (Exception e) {
            //do nothing
        }

        launch_count++;
        editor.putLong(PREF_LAUNCH_COUNT, launch_count);

        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            editor.putLong(PREF_DATE_FIRST_LAUNCHED, date_firstLaunch);
        }

        // Wait at least n days or m events before opening
        if (launch_count >= appiratorLaunchesUntilPrompt) {
            long millisecondsToWait = appiratorDaysUntilPrompt * 24 * 60 * 60 * 1000L;
            if (System.currentTimeMillis() >= (date_firstLaunch + millisecondsToWait) || event_count >= appiratorEventsUntilPrompt) {
                if (date_reminder_pressed == 0) {
                    showRateDialog(context, editor);
                } else {
                    long remindMillisecondsToWait = appiratorDaysBeforeReminding * 24 * 60 * 60 * 1000L;
                    if (System.currentTimeMillis() >= (remindMillisecondsToWait + date_reminder_pressed)) {
                        showRateDialog(context, editor);
                    }
                }
            }
        }

        editor.commit();
    }

    public static void rateApp(Context mContext) {
        SharedPreferences prefs = mContext.getSharedPreferences(mContext.getPackageName() + ".appirater", 0);
        SharedPreferences.Editor editor = prefs.edit();
        rateApp(mContext, editor);
    }

    public static void significantEvent(Context mContext) {
        boolean testMode = mContext.getResources().getBoolean(R.bool.appirator_test_mode);
        SharedPreferences prefs = mContext.getSharedPreferences(mContext.getPackageName() + ".appirater", 0);
        if (!testMode && (prefs.getBoolean(PREF_DONT_SHOW, false) || prefs.getBoolean(PREF_RATE_CLICKED, false))) {
            return;
        }

        long event_count = prefs.getLong(PREF_EVENT_COUNT, 0);
        event_count++;
        prefs.edit().putLong(PREF_EVENT_COUNT, event_count).apply();
    }

    private static void rateApp(Context mContext, final SharedPreferences.Editor editor) {
        mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(mContext.getString(R.string.appirator_market_url), mContext.getPackageName()))));
        if (editor != null) {
            editor.putBoolean(PREF_RATE_CLICKED, true);
            editor.commit();
        }
    }

    @SuppressLint("NewApi")
    private static void showRateDialog(final Context mContext, final SharedPreferences.Editor editor) {
        String appName = mContext.getString(R.string.appirator_app_title);

        AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();

        alertDialog.setTitle(String.format(mContext.getString(R.string.rate_title), appName));

        alertDialog.setMessage(String.format(mContext.getString(R.string.rate_message), appName));

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, String.format(mContext.getString(R.string.rate), appName), new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                rateApp(mContext, editor);
                dialog.dismiss();
            }
        });

//        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, mContext.getString(R.string.rate_later), new DialogInterface.OnClickListener() {
//
//            public void onClick(DialogInterface dialog, int id) {
//                if (editor != null) {
//                    editor.putLong(PREF_DATE_REMINDER_PRESSED, System.currentTimeMillis());
//                    editor.commit();
//                }
//                dialog.dismiss();
//            }
//        });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, mContext.getString(R.string.rate_cancel), new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                if (editor != null) {
                    editor.putBoolean(PREF_DONT_SHOW, true);
                    editor.commit();
                }
                dialog.dismiss();
            }
        });

        alertDialog.show();
    }
}