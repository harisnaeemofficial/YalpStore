package com.github.yeriomin.yalpstore.fragment.details;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;

import com.github.yeriomin.yalpstore.BlackWhiteListManager;
import com.github.yeriomin.yalpstore.BuildConfig;
import com.github.yeriomin.yalpstore.ContextUtil;
import com.github.yeriomin.yalpstore.InstalledApkCopier;
import com.github.yeriomin.yalpstore.ManualDownloadActivity;
import com.github.yeriomin.yalpstore.R;
import com.github.yeriomin.yalpstore.YalpStoreActivity;
import com.github.yeriomin.yalpstore.model.App;
import com.github.yeriomin.yalpstore.task.CheckShellTask;
import com.github.yeriomin.yalpstore.task.ConvertToNormalTask;
import com.github.yeriomin.yalpstore.task.ConvertToSystemTask;
import com.github.yeriomin.yalpstore.task.SystemRemountTask;
import com.github.yeriomin.yalpstore.view.FlagDialogBuilder;

public class DownloadOptions extends Abstract {

    public DownloadOptions(YalpStoreActivity activity, App app) {
        super(activity, app);
    }

    @Override
    public void draw() {
        final View more = activity.findViewById(R.id.more);
        if (null == more || more instanceof ViewStub) {
            return;
        }
        activity.registerForContextMenu(more);
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                more.showContextMenu();
            }
        });
    }

    public void inflate(Menu menu) {
        MenuInflater inflater = activity.getMenuInflater();
        inflater.inflate(R.menu.menu_download, menu);
        if (isInstalled(app)) {
            onCreateOptionsMenu(menu);
        }
    }

    public void onCreateOptionsMenu(Menu menu) {
        show(menu, R.id.action_manual, true);
        show(menu, R.id.action_get_local_apk, app.isInstalled());
        BlackWhiteListManager manager = new BlackWhiteListManager(activity);
        boolean isContained = manager.contains(app.getPackageName());
        if (manager.isBlack()) {
            show(menu, R.id.action_unignore, isContained);
            show(menu, R.id.action_ignore, !isContained);
        } else {
            show(menu, R.id.action_unwhitelist, isContained);
            show(menu, R.id.action_whitelist, !isContained);
        }
        if (isConvertible(app)) {
            show(menu, R.id.action_make_system, !app.isSystem());
            show(menu, R.id.action_make_normal, app.isSystem());
        }
        show(menu, R.id.action_flag, app.isInPlayStore());
    }

    private void show(Menu menu, int itemId, boolean show) {
        MenuItem item = menu.findItem(itemId);
        if (null != item) {
            item.setVisible(show);
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_ignore:
            case R.id.action_whitelist:
                new BlackWhiteListManager(activity).add(app.getPackageName());
                draw();
                return true;
            case R.id.action_unignore:
            case R.id.action_unwhitelist:
                new BlackWhiteListManager(activity).remove(app.getPackageName());
                draw();
                return true;
            case R.id.action_manual:
                activity.startActivity(new Intent(activity, ManualDownloadActivity.class));
                return true;
            case R.id.action_get_local_apk:
                new CopyTask(activity).execute(app);
                return true;
            case R.id.action_make_system:
                checkAndExecute(new ConvertToSystemTask(activity, app));
                return true;
            case R.id.action_make_normal:
                checkAndExecute(new ConvertToNormalTask(activity, app));
                return true;
            case R.id.action_flag:
                new FlagDialogBuilder().setActivity(activity).setApp(app).build().show();
                return true;
        }
        return false;
    }

    private void checkAndExecute(SystemRemountTask primaryTask) {
        CheckShellTask checkShellTask = new CheckShellTask(activity);
        checkShellTask.setPrimaryTask(primaryTask);
        checkShellTask.execute();
    }

    private boolean isConvertible(App app) {
        return isInstalled(app)
            && !app.getPackageName().equals(BuildConfig.APPLICATION_ID)
            && null != app.getPackageInfo().applicationInfo
            && null != app.getPackageInfo().applicationInfo.sourceDir
            && !app.getPackageInfo().applicationInfo.sourceDir.endsWith("pkg.apk")
        ;
    }

    private boolean isInstalled(App app) {
        try {
            activity.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    static class CopyTask extends AsyncTask<App, Void, Boolean> {

        private Activity activity;
        private App app;

        public CopyTask(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            ContextUtil.toastLong(
                activity.getApplicationContext(),
                activity.getString(result
                    ? R.string.details_saved_in_downloads
                    : R.string.details_could_not_copy_apk
                )
            );
        }

        @Override
        protected Boolean doInBackground(App... apps) {
            app = apps[0];
            return InstalledApkCopier.copy(activity, app);
        }
    }
}
