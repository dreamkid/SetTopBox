package com.savor.ads.projection.action;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.savor.ads.activity.ScreenProjectionActivity;
import com.savor.ads.projection.ProjectPriority;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.LogUtils;

import java.io.Serializable;

/**
 * Created by zhang.haiqiang on 2017/5/22.
 */
public class VodAction extends ProjectionActionBase implements Serializable {

    private transient Context mContext;

    private String vid;
    private String url;
    private int position;
    private boolean isFromWeb;

    public VodAction(Context context, String vid, String url, int position, boolean isFromWeb) {
        super();

        mPriority = ProjectPriority.HIGH;
        mContext = context;
        this.vid = vid;
        this.url = url;
        this.position = position;
        this.isFromWeb = isFromWeb;
    }

    @Override
    public void execute() {
        onActionBegin();

        // 跳转或将参数设置到ScreenProjectionActivity
        Bundle data = new Bundle();
        data.putString(ScreenProjectionActivity.EXTRA_URL, url);
        data.putString(ScreenProjectionActivity.EXTRA_TYPE, ConstantValues.PROJECT_TYPE_VIDEO_VOD);
        data.putString(ScreenProjectionActivity.EXTRA_MEDIA_ID, vid);
        data.putInt(ScreenProjectionActivity.EXTRA_VIDEO_POSITION, position);
        data.putBoolean(ScreenProjectionActivity.EXTRA_IS_FROM_WEB, isFromWeb);
        data.putSerializable(ScreenProjectionActivity.EXTRA_PROJECT_ACTION, this);

        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof ScreenProjectionActivity && !((ScreenProjectionActivity) activity).isBeenStopped()) {
            LogUtils.e("Listener will setNewProjection");
            ((ScreenProjectionActivity) activity).setNewProjection(data);
        } else {
            if (ActivitiesManager.getInstance().getCurrentActivity() == null) {
                LogUtils.e("Listener will startActivity in new task");
                Intent intent = new Intent(mContext, ScreenProjectionActivity.class);
                intent.putExtras(data);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            } else {
                LogUtils.e("Listener will startActivity in " + activity.toString());
                Intent intent = new Intent(activity, ScreenProjectionActivity.class);
                intent.putExtras(data);
                activity.startActivity(intent);
            }
        }
    }

    @Override
    public String toString() {
        return "VodAction{" +
                "vid='" + vid + '\'' +
                ", url='" + url + '\'' +
                ", position=" + position +
                ", isFromWeb=" + isFromWeb +
                '}';
    }
}
