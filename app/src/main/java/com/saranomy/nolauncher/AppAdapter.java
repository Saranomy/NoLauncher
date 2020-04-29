/**
 * NoLauncher
 * Created by Saranomy on 2020-04-29.
 * Under Apache License Version 2.0, http://www.apache.org/licenses/
 * saranomy@gmail.com
 */
package com.saranomy.nolauncher;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AppAdapter extends BaseAdapter {
    private Activity activity;
    private ArrayList<AppItem> apps;

    AppAdapter(Activity activity, ArrayList<AppItem> apps) {
        this.activity = activity;
        this.apps = apps;
    }

    @Override
    public int getCount() {
        return apps.size();
    }

    @Override
    public Object getItem(int position) {
        return apps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        final ViewHolder holder;
        if (convertView == null) {
            view = activity.getLayoutInflater().inflate(R.layout.item_app, null);
            holder = new ViewHolder();
            holder.item_app_name = view.findViewById(R.id.item_app_name);
            holder.item_app_icon = view.findViewById(R.id.item_app_icon);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        final AppItem app = (AppItem) getItem(position);
        holder.item_app_icon.setImageDrawable(app.icon);
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                try {
                    activity.startActivity(activity.getPackageManager().getLaunchIntentForPackage(app.packageName));
                } catch (Exception e) {
                }
            }
        });
        view.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                try {
                    activity.startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.packageName)));
                } catch (Exception ignored) {
                }
                return false;
            }
        });
        holder.item_app_name.setText(app.name);
        return view;
    }

    public class ViewHolder {
        ImageView item_app_icon;
        TextView item_app_name;
    }
}
