package com.personalai.apps;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppLauncher {

    private final Context context;
    private final PackageManager packageManager;

    public AppLauncher(Context context) {
        this.context = context;
        this.packageManager = context.getPackageManager();
    }

    public boolean openApp(String appName) {

        if (appName == null || appName.trim().isEmpty()) {
            return false;
        }

        String target =
                appName.trim().toLowerCase(Locale.ROOT);

        List<android.content.pm.ResolveInfo> apps =
                packageManager.queryIntentActivities(
                        new Intent(Intent.ACTION_MAIN)
                                .addCategory(Intent.CATEGORY_LAUNCHER),
                        PackageManager.MATCH_ALL
                );

        for (android.content.pm.ResolveInfo info : apps) {

            String label =
                    info.loadLabel(packageManager)
                            .toString()
                            .toLowerCase(Locale.ROOT);

            if (label.equals(target) ||
                    label.contains(target) ||
                    target.contains(label)) {

                Intent launchIntent =
                        packageManager.getLaunchIntentForPackage(
                                info.activityInfo.packageName
                        );

                if (launchIntent != null) {

                    launchIntent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                    );

                    context.startActivity(
                            launchIntent
                    );

                    return true;
                }
            }
        }

        return false;
    }

    public List<String> getInstalledLaunchableApps() {

        List<String> result =
                new ArrayList<>();

        List<android.content.pm.ResolveInfo> apps =
                packageManager.queryIntentActivities(
                        new Intent(Intent.ACTION_MAIN)
                                .addCategory(Intent.CATEGORY_LAUNCHER),
                        PackageManager.MATCH_ALL
                );

        for (android.content.pm.ResolveInfo info : apps) {

            String label =
                    info.loadLabel(packageManager)
                            .toString();

            if (!label.isEmpty()) {
                result.add(label);
            }
        }

        return result;
    }
}
