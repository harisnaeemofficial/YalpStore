package com.github.yeriomin.yalpstore.fragment.details;

import android.content.pm.PackageManager;
import android.view.View;

import com.github.yeriomin.yalpstore.YalpStoreActivity;
import com.github.yeriomin.yalpstore.model.App;

public abstract class Button extends Abstract {

    protected View button;

    public Button(YalpStoreActivity activity, App app) {
        super(activity, app);
        this.button = getButton();
    }

    abstract protected View getButton();

    abstract protected boolean shouldBeVisible();

    abstract protected void onButtonClick(View v);

    @Override
    public void draw() {
        if (null == button) {
            return;
        }
        button.setEnabled(true);
        button.setVisibility(shouldBeVisible() ? View.VISIBLE : View.GONE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonClick(v);
            }
        });
    }

    protected void disable(int stringId) {
        if (null == button) {
            return;
        }
        if (button instanceof android.widget.Button) {
            ((android.widget.Button) button).setText(stringId);
        }
        button.setEnabled(false);
    }

    protected boolean isInstalled() {
        try {
            activity.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
