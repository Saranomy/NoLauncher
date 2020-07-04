/**
 * NoLauncher
 * Created by Saranomy on 2020-04-29.
 * Under Apache License Version 2.0, http://www.apache.org/licenses/
 */
package com.saranomy.nolauncher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ListView;

public class MainActivity extends Activity {
    @SuppressLint("StaticFieldLeak")
    public static MainActivity self;

    private EditText activity_home_search;
    private ListView activity_home_list;
    private static ArrayList<AppItem> apps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity_home_search = findViewById(R.id.activity_home_search);
        activity_home_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                search(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        activity_home_list = findViewById(R.id.activity_home_list);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_INSTALL);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");
        registerReceiver(new AppChangeReceiver(), intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        self = this;
        if (apps == null || apps.size() == 0)
            loadApps();
    }

    @Override
    protected void onStop() {
        super.onStop();
        self = null;
    }

    @Override
    public void onBackPressed() {
        try {
            if (activity_home_search.getText().toString().length() > 0)
                activity_home_search.setText("");
        } catch (Exception ignored) {
        }
    }

    public void loadApps() {
        try {
            apps = new ArrayList<>();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> pkgAppsList = getPackageManager().queryIntentActivities(mainIntent, 0);
            for (ResolveInfo info : pkgAppsList) {
                AppItem app = new AppItem();
                app.name = info.activityInfo.applicationInfo.loadLabel(getPackageManager()).toString();
                app.packageName = info.activityInfo.applicationInfo.packageName;
                app.icon = info.activityInfo.applicationInfo.loadIcon(getPackageManager());
                apps.add(app);
            }
            Collections.sort(apps);
            AppAdapter adapter = new AppAdapter(MainActivity.this, apps);
            activity_home_list.setAdapter(adapter);
        } catch (Exception ignored) {
        }
    }

    private void search(String text) {
        try {
            ArrayList<AppItem> resultApps = new ArrayList<>();
            for (AppItem app : apps) {
                if (app.name.toLowerCase(Locale.getDefault()).contains(text.toLowerCase(Locale.getDefault()))) {
                    resultApps.add(app);
                }
            }
            AppAdapter adapter = new AppAdapter(MainActivity.this, resultApps);
            activity_home_list.setAdapter(adapter);
        } catch (Exception ignored) {
        }
    }
}