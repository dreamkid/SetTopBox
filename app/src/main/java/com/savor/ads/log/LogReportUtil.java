package com.savor.ads.log;

import android.content.Context;
import android.text.TextUtils;

import com.savor.ads.core.Session;
import com.savor.ads.utils.AppUtils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class LogReportUtil {
    private static BlockingQueue<LogReportParam> mLogQueue = null;
    private static BlockingQueue<QRCodeLogBean> mQRCodeLogQueue = null;
    Context mContext = null;
    Session session = null;
    private static LogReportUtil instance = null;

    private LogReportUtil(Context context) {
        mLogQueue = new ArrayBlockingQueue<>(15);
        mQRCodeLogQueue = new ArrayBlockingQueue<>(15);
        this.mContext = context;
        session = Session.get(mContext);
        initLog();
    }

    public static LogReportUtil get(Context context) {
        if (instance == null) {
            instance = new LogReportUtil(context);
        }
        return instance;
    }

    private void initLog() {
        String time = String.valueOf(System.currentTimeMillis());
        String boiteId = "";
        String roomId = "";

        LogReportParam mLogPowerOn = new LogReportParam();
        mLogPowerOn.setUUid(time);
        if (!TextUtils.isEmpty(session.getBoiteId())) {
            boiteId = session.getBoiteId();
        }
        if (!TextUtils.isEmpty(session.getRoomId())) {
            roomId = session.getRoomId();
        }
        mLogPowerOn.setUUid(time);
        mLogPowerOn.setHotel_id(boiteId);
        mLogPowerOn.setRoom_id(roomId);
        mLogPowerOn.setTime(time);
        mLogPowerOn.setAction("poweron");
        mLogPowerOn.setType("");
        mLogPowerOn.setMedia_id("");
        mLogPowerOn.setMobile_id("");
        mLogPowerOn.setApk_version(session.getVersionName());
        mLogPowerOn.setAdsPeriod(session.getAdsPeriod());
        mLogPowerOn.setVodPeriod(session.getVodPeriod());
        mLogPowerOn.setCustom(AppUtils.getInputType(session.getTvInputSource()));
        mLogPowerOn.setBoxId(session.getEthernetMac());
        mLogPowerOn.setLogHour("");
        offer(mLogPowerOn);

        /*******开机即生成一条信号源日志*******/
        LogReportParam mLogSignal = new LogReportParam();
        time = String.valueOf(System.currentTimeMillis());
        mLogSignal.setUUid(time);
        mLogSignal.setHotel_id(boiteId);
        mLogSignal.setRoom_id(roomId);
        mLogSignal.setTime(time);
        mLogSignal.setAction("Signal");
        mLogSignal.setType("system");
        mLogSignal.setMedia_id("");
        mLogSignal.setMobile_id("");
        mLogSignal.setApk_version(session.getVersionName());
        mLogSignal.setAdsPeriod(session.getAdsPeriod());
        mLogSignal.setVodPeriod(session.getVodPeriod());
        mLogSignal.setCustom(AppUtils.getInputType(session.getTvInputSource()));
        offer(mLogSignal);
    }


    private void offer(LogReportParam arg) {
        if (mLogQueue != null) {
            mLogQueue.offer(arg);
        }
    }

    private void offerQRCode(QRCodeLogBean arg) {
        if (mQRCodeLogQueue != null) {
            mQRCodeLogQueue.offer(arg);
        }
    }

    public LogReportParam poll() {
        return mLogQueue.poll();
    }

    public LogReportParam take() throws InterruptedException {
        if (mLogQueue != null)
            return mLogQueue.take();
        else
            return null;
    }

    public QRCodeLogBean takeCodeLog() throws InterruptedException {
        if (mQRCodeLogQueue != null)
            return mQRCodeLogQueue.take();
        else
            return null;
    }

    public static int getLogNum() {
        return mLogQueue.size();
    }

    public int getQRCodeLogNum() {
        return mQRCodeLogQueue.size();
    }

    /**
     * @param uuid        视频播放完整性
     * @param hotel_id    酒楼ID
     * @param room_id     包间ID
     * @param time        发生时间
     * @param action      动作
     * @param type        类型
     * @param media_id    视频ID
     * @param mobile_id   手机ID
     * @param apk_version apk版本
     * @param ads_period  广告期号
     * @param vod_period  点播期号
     * @param custom      通用参数，目前包括播放器音量，电视切换时间，电视信号源
     */
    public void sendAdsLog(String uuid,
                           String hotel_id,
                           String room_id,
                           String time,
                           String action,
                           String type,
                           String media_id,
                           String mobile_id,
                           String apk_version,
                           String ads_period,
                           String vod_period,
                           String custom) {
        LogReportParam mLogEn = new LogReportParam();

        mLogEn.setUUid(uuid);
        mLogEn.setHotel_id(hotel_id);
        mLogEn.setRoom_id(room_id);
        mLogEn.setTime(time);
        mLogEn.setAction(action);
        mLogEn.setType(type);
        mLogEn.setMedia_id(media_id);
        mLogEn.setMobile_id(mobile_id);
        mLogEn.setApk_version(apk_version);
        mLogEn.setAdsPeriod(ads_period);
        mLogEn.setVodPeriod(vod_period);
        mLogEn.setCustom(custom);
        offer(mLogEn);
    }

    /**
     * 写小程序码显示日志
     * @param boxId
     * @param hotel_id
     * @param room_id
     * @param time
     * @param action
     */
    public void sendQRCodeLog(String boxId,
                            String hotel_id,
                            String room_id,
                            String time,
                            String action){
        QRCodeLogBean codeLogBean = new QRCodeLogBean();
        codeLogBean.setHotel_id(hotel_id);
        codeLogBean.setRoom_id(room_id);
        codeLogBean.setBoxId(boxId);
        codeLogBean.setAction(action);
        codeLogBean.setTime(time);
        offerQRCode(codeLogBean);
    }
}
