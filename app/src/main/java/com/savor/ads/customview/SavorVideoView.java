package com.savor.ads.customview;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.savor.ads.R;
import com.savor.ads.bean.MediaPlayerError;
import com.savor.ads.bean.MediaPlayerState;
import com.savor.ads.core.Session;
import com.savor.ads.utils.GlideImageLoader;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


/**
 * 公用的播放器类
 * Created by zhanghq on 2016/12/8.
 */
public class SavorVideoView extends RelativeLayout {
    private static final String TAG = "SavorVideoView";

    /**
     * MediaPlayer最大Prepare时间
     */
    private static final int MAX_PREPARE_TIME = 1000 * 20;

    private SurfaceView mSurfaceTv;
    private SurfaceHolder mSurfaceHolder;
    private RelativeLayout mRootRl;
    private ImageView mLoadingIv;
    private CircleProgressBar mProgressBar;
    private ImageView mPlayVideoIv;

    private MediaPlayer mMediaPlayer;
    private MediaPlayerState mPlayState;
    private ArrayList<String> mMediaFiles;

    /**
     * 当前应该播放的源序号
     */
    private int mCurrentFileIndex;
    /**
     * 指定的播放起始位置
     */
    private int mAssignedPlayPosition = -1;
    /**
     * 强制从头播放标志位
     */
    private boolean mForcePlayFromStart;
    private boolean mIsPauseByOut;

    /**
     * 暂停时是否显示“播放”图片
     */
    private boolean mIfShowPauseBtn;
    /**
     * 是否显示加载loading
     */
    private boolean mIfShowLoading;
    /**
     * 是否自动播放下一个
     */
    private boolean mIsLooping = true;
    /**
     * 是否处理MediaPlayer的Prepare超时
     */
    private boolean mIfHandlePrepareTimeout;
    /**
     * Surface是否创建好了
     */
    private boolean mIsSurfaceCreated;

    /**最大缓冲加载时间*/
    private static final int MAX_BUFFER_TIME = 1000 * 10;
    private Runnable mBufferTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPlayStateCallback != null) {
                boolean isLast = false;
                if (mMediaFiles != null && mMediaFiles.size() > 0) {
                    isLast = mCurrentFileIndex == mMediaFiles.size() - 1;
                }
                // 回调某个视频播放出错
                mPlayStateCallback.onMediaError(mCurrentFileIndex, isLast);
            }
        }
    };

    private PlayStateCallback mPlayStateCallback;

    public SavorVideoView(Context context) {
        this(context, null);
    }

    public SavorVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
        initMediaPlayer();
    }

    private void initView() {
        View rootView = View.inflate(getContext(), R.layout.layout_savor_video_view, this);
        mRootRl = (RelativeLayout)  rootView;
        mSurfaceTv = (SurfaceView) rootView.findViewById(R.id.texture_view);
        mLoadingIv = (ImageView) findViewById(R.id.iv_loading);
        mProgressBar = (CircleProgressBar) rootView.findViewById(R.id.progress_bar);
        mPlayVideoIv = (ImageView) findViewById(R.id.iv_video_play);

        mSurfaceHolder = mSurfaceTv.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                LogUtils.w(TAG + "surfaceCreated mPlayState:" + mPlayState + " mMediaPlayer == null?" +(mMediaPlayer == null) + "  " + SavorVideoView.this.hashCode());
//                LogFileUtil.write(TAG + "surfaceCreated mPlayState:" + mPlayState + " mMediaPlayer == null?" +(mMediaPlayer == null) + "  " + SavorVideoView.this.hashCode());
                mIsSurfaceCreated = true;
                if (mMediaPlayer != null /*&&
                        (mPlayState != MediaPlayerState.ERROR &&
                                mPlayState != MediaPlayerState.IDLE &&
                                mPlayState != MediaPlayerState.END)*/) {

                    try {
                        LogUtils.w("Will setDisplay Current state:" + mPlayState + " " + SavorVideoView.this.hashCode());
//                        LogFileUtil.write("Will setDisplay Current state:" + mPlayState + " " + SavorVideoView.this.hashCode());
                        mMediaPlayer.setDisplay(mSurfaceHolder);
                        mMediaPlayer.setScreenOnWhilePlaying(true);
                    } catch (Exception e) {
                        LogUtils.e("setDisplay Exception, Current state:" + mPlayState + " " + SavorVideoView.this.hashCode());
//                        LogFileUtil.write("setDisplay Exception, Current state:" + mPlayState + " " + SavorVideoView.this.hashCode());
                        e.printStackTrace();
                    }
                    if (mPlayState == MediaPlayerState.INITIALIZED) {
                        prepareMediaPlayer();
                    } else if (mPlayState == MediaPlayerState.PREPARED) {
                        playInner();
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                LogUtils.w(TAG + "surfaceChanged width = " + width + " height = " + height + " " + SavorVideoView.this.hashCode());
//                LogFileUtil.write(TAG + "surfaceChanged width = " + width + " height = " + height + " " + SavorVideoView.this.hashCode());
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                LogUtils.w(TAG + "surfaceDestroyed" + " " + SavorVideoView.this.hashCode());
//                LogFileUtil.write(TAG + "surfaceDestroyed" + " " + SavorVideoView.this.hashCode());
                mIsSurfaceCreated = false;

                release();
            }
        });
//        mSurfaceTv.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
//            @Override
//            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//                LogUtils.d(TAG, "onSurfaceTextureAvailable");
//                mMediaPlayer.setSurface(new Surface(surface));
//                if (mPlayState == MediaPlayerState.PREPARED) {
//                    playInner();
//                }
//            }
//
//            @Override
//            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//                LogUtils.d(TAG, "onSurfaceTextureSizeChanged width = " + width + " height = " + height);
//            }
//
//            @Override
//            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//                LogUtils.d(TAG, "onSurfaceTextureDestroyed");
//                release();
//                return true;
//            }
//
//            @Override
//            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//            }
//        });
    }

    private void initMediaPlayer() {
        LogUtils.w(TAG + "initMediaPlayer mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
//        LogFileUtil.write(TAG + "initMediaPlayer mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mPlayState = MediaPlayerState.IDLE;
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    LogUtils.w(TAG + "MediaPlayer onCompletion" + " " + SavorVideoView.this.hashCode());
//                    LogFileUtil.write(TAG + "MediaPlayer onCompletion" + " " + SavorVideoView.this.hashCode());
                    mPlayState = MediaPlayerState.COMPLETED;

                    boolean beenResetSource = false;
                    if (mPlayStateCallback != null) {
                        boolean isLast = false;
                        if (mMediaFiles != null && mMediaFiles.size() > 0) {
                            isLast = mCurrentFileIndex == mMediaFiles.size() - 1;
                        }
                        // 回调某个视频播放完毕
                        beenResetSource = mPlayStateCallback.onMediaComplete(mCurrentFileIndex, isLast);
                    }

                    if (!beenResetSource) {
                        if (mForcePlayFromStart) {
                            // 强制从头播放
                            mForcePlayFromStart = false;
                            mCurrentFileIndex = 0;
                            mAssignedPlayPosition = 0;
                        } else {
                            // 播放下一个
                            mCurrentFileIndex = (mCurrentFileIndex + 1) % mMediaFiles.size();
                            mAssignedPlayPosition = 0;
                        }

                        if (mIsLooping) {
                            // 重置播放器状态，以备下次播放
                            resetAndPreparePlayer();
                        }
                    }
                }
            });
            mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    LogUtils.v(TAG + "onBufferingUpdate percent = " + percent + " " + SavorVideoView.this.hashCode());
//                    LogFileUtil.write(TAG + "onBufferingUpdate percent = " + percent + " " + SavorVideoView.this.hashCode());

                    // 播放器状态不对时getDuration报错，也没必要处理播放、暂停了，这里直接return
                    if (mPlayState == MediaPlayerState.IDLE || mPlayState == MediaPlayerState.INITIALIZED ||
                            mPlayState == MediaPlayerState.PREPARING || mPlayState == MediaPlayerState.ERROR)
                        return;

                    int currentPercent = mp.getCurrentPosition() * 100 / mp.getDuration();
                    LogUtils.v(TAG + "onBufferingUpdate currentPercent = " + currentPercent + " position = " + mp.getCurrentPosition() + " duration = " + mp.getDuration() + " " + SavorVideoView.this.hashCode());
//                    LogFileUtil.write(TAG + "onBufferingUpdate currentPercent = " + currentPercent + " position = " + mp.getCurrentPosition() + " duration = " + mp.getDuration() + " " + SavorVideoView.this.hashCode());
//                    if (mp.getCurrentPosition() + 400 < mp.getDuration()) {
                        if (percent < 99 && currentPercent >= percent - 1) {
                            // 缓冲部分不足时，暂停播放并显示进度圈
                            if (mIfShowLoading) {
                                mProgressBar.setVisibility(VISIBLE);
                            }
                            if (mPlayState == MediaPlayerState.STARTED) {
                                pauseInner();
                            }
                        } else {
                            // 缓冲好时，继续播放并隐藏进度圈
                            if (mIfShowLoading) {
                                mProgressBar.setVisibility(GONE);
                            }
                            if (mPlayState == MediaPlayerState.PAUSED && !mIsPauseByOut) {
                                playInner();
                            }
                        }
//                    } else {
//                        // 缓冲好时，继续播放并隐藏进度圈
//                        if (mIfShowLoading) {
//                            mProgressBar.setVisibility(GONE);
//                        }
//                        if (mPlayState == MediaPlayerState.PAUSED && !mIsPauseByOut) {
//                            playInner();
//                        }
//                    }
                }
            });
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    LogUtils.w(TAG + "MediaPlayer onMediaPrepared " +  " mIsSurfaceCreated:" + mIsSurfaceCreated +
                            " mIsPauseByOut:" + mIsPauseByOut +" mAssignedPlayPosition:" + mAssignedPlayPosition
                            + " " + SavorVideoView.this.hashCode());
//                    LogFileUtil.write(TAG + "MediaPlayer onMediaPrepared " +  " mIsSurfaceCreated:" + mIsSurfaceCreated +
//                            " mIsPauseByOut:" + mIsPauseByOut +" mAssignedPlayPosition:" + mAssignedPlayPosition
//                            + " " + SavorVideoView.this.hashCode());
                    mPlayState = MediaPlayerState.PREPARED;

                    if (mIfHandlePrepareTimeout) {
                        // 准备开始播放移除Runnable
                        removeCallbacks(mPrepareTimeoutRunnable);
                    }

                    boolean beenAborted = false;
                    // 回调准备完毕
                    if (mPlayStateCallback != null) {
                        beenAborted = mPlayStateCallback.onMediaPrepared(mCurrentFileIndex);
                    }

                    if (!beenAborted) {
                        if (mIfShowLoading) {
                            mLoadingIv.setVisibility(GONE);
                            mProgressBar.setVisibility(GONE);
                        }

                        // 如果Surface创建完毕且没被外部强行停止时，开始播放
                        if (mIsSurfaceCreated && !mIsPauseByOut) {
                            LogUtils.d("Will setDisplay in onPrepared() when surface is created");
                            mp.setDisplay(mSurfaceHolder);

                            if (mAssignedPlayPosition > 0 && mAssignedPlayPosition < mp.getDuration()) {
                                mp.seekTo(mAssignedPlayPosition);
                                mAssignedPlayPosition = -1;
                            } else {
                                playInner();
                            }
                        }
                    }
                }
            });
            mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                    ViewGroup.LayoutParams layoutParams = mSurfaceTv.getLayoutParams();
                    int viewWidth = getWidth();
                    int viewHeight = getHeight();
                    LogUtils.w(TAG + "MediaPlayer onVideoSizeChanged width = " + width + " height = " + height +
                            " viewWidth = " + viewWidth + " viewHeight = " + viewHeight + " " + SavorVideoView.this.hashCode());
//                    LogFileUtil.write(TAG + "MediaPlayer onVideoSizeChanged width = " + width + " height = " + height +
//                            " viewWidth = " + viewWidth + " viewHeight = " + viewHeight + " " + SavorVideoView.this.hashCode());
                    if (((double) viewWidth / width) * height > viewHeight) {
                        layoutParams.width = (int) (((double) viewHeight / height) * width);
                        layoutParams.height = viewHeight;
                    } else {
                        layoutParams.width = viewWidth;
                        layoutParams.height = (int) (((double) viewWidth / width) * height);
                    }
//                    adjustAspectRatio(width, height);
                }
            });
            mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    LogUtils.w(TAG + "MediaPlayer onSeekComplete" + " " + SavorVideoView.this.hashCode());
//                    LogFileUtil.write(TAG + "MediaPlayer onSeekComplete" + " " + SavorVideoView.this.hashCode());
                    if (mMediaPlayer != null && mPlayState == MediaPlayerState.PREPARED) {
                        playInner();
                    }
                }
            });
            mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(MediaPlayer mp, int what, int extra) {
                    LogUtils.w(TAG + "MediaPlayer setOnInfoListener" + " what=" + what + " extra=" + extra + " " + SavorVideoView.this.hashCode());
//                    LogFileUtil.write(TAG + "MediaPlayer setOnInfoListener" + " what=" + what + " extra=" + extra + " " + SavorVideoView.this.hashCode());
                    if (MediaPlayer.MEDIA_INFO_BUFFERING_START == what) {
                        mRootRl.removeCallbacks(mBufferTimeoutRunnable);
                        mRootRl.postDelayed(mBufferTimeoutRunnable, MAX_BUFFER_TIME);
                    } else if (MediaPlayer.MEDIA_INFO_BUFFERING_END == what) {
                        mRootRl.removeCallbacks(mBufferTimeoutRunnable);
                    }
                    return false;
                }
            });
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    LogUtils.e(TAG + "MediaPlayer onError what = " + what + " extra = " + extra + " " + SavorVideoView.this.hashCode());
//                    LogFileUtil.write(TAG + "MediaPlayer onError what = " + what + " extra = " + extra + " " + SavorVideoView.this.hashCode());
                    if (!mp.equals(mMediaPlayer)) {
                        return true;
                    }
                    mPlayState = MediaPlayerState.ERROR;

                    int position = mp.getCurrentPosition();

                    // 根据出错类型，判断是否可继续尝试播放此视频源
                    if (extra == MediaPlayerError.ERROR_NOT_CONNECTED) {
                        // 网络连接错误的时候会进到ERROR_NOT_CONNECTED这个错误类型，这时reset MediaPlayer并记录播放进度
                        resetAndPreparePlayer(position);
                    } else {
                        boolean beenResetSource = false;
                        if (mPlayStateCallback != null) {
                            boolean isLast = false;
                            if (mMediaFiles != null && mMediaFiles.size() > 0) {
                                isLast = mCurrentFileIndex == mMediaFiles.size() - 1;
                            }
                            // 回调某个视频播放出错
                            beenResetSource = mPlayStateCallback.onMediaError(mCurrentFileIndex, isLast);
                        }

                        if (!beenResetSource) {
                            if (mForcePlayFromStart) {
                                // 强制从头播放
                                mForcePlayFromStart = false;
                                mCurrentFileIndex = 0;
                                mAssignedPlayPosition = 0;
                            } else /*if (position == mMediaPlayer.getDuration()) */{
                                // 播放下一个
                                if (mMediaFiles != null && mMediaFiles.size() > 0){
                                    mCurrentFileIndex = (mCurrentFileIndex + 1) % mMediaFiles.size();
                                    mAssignedPlayPosition = 0;
                                }
                            }
                            if (mIsLooping) {
                                resetAndPreparePlayer();
                            }
                        }
                    }
                    return true;
                }
            });

            try {
                if (mIsSurfaceCreated) {
                    LogUtils.d("Will setDisplay in initMediaPlayer() when surface is created");
//                    LogFileUtil.write("Will setDisplay in initMediaPlayer() when surface is created");
                    mMediaPlayer.setDisplay(mSurfaceHolder);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

//    private void adjustAspectRatio(int videoWidth, int videoHeight) {
//        int viewWidth = mSurfaceTv.getWidth();
//        int viewHeight = mSurfaceTv.getHeight();
//        double aspectRatio = (double) videoHeight / videoWidth;
//
//        int newWidth, newHeight;
//        if (viewHeight > (int) (viewWidth * aspectRatio)) {
//            // limited by narrow width; restrict height
//            newWidth = viewWidth;
//            newHeight = (int) (viewWidth * aspectRatio);
//        } else {
//            // limited by short height; restrict width
//            newWidth = (int) (viewHeight / aspectRatio);
//            newHeight = viewHeight;
//        }
//        int xoff = (viewWidth - newWidth) / 2;
//        int yoff = (viewHeight - newHeight) / 2;
//        LogUtils.v(TAG, "video=" + videoWidth + "x" + videoHeight +
//                " view=" + viewWidth + "x" + viewHeight +
//                " newView=" + newWidth + "x" + newHeight +
//                " off=" + xoff + "," + yoff);
//
//        Matrix txform = new Matrix();
//        mSurfaceTv.getTransform(txform);
//        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
//        //txform.postRotate(10);          // just for fun
//        txform.postTranslate(xoff, yoff);
//        mSurfaceTv.setTransform(txform);
//    }

    /**
     * 设置播放数据源
     */
    private boolean setMediaPlayerSource() {
        LogUtils.w(TAG + " setMediaPlayerSource mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
//        LogFileUtil.write(TAG + "setMediaPlayerSource mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
        if (mMediaPlayer == null) {
            LogUtils.e(TAG + " setMediaPlayerSource mMediaPlayer == null " + " " + SavorVideoView.this.hashCode());
//            LogFileUtil.write(TAG + " setMediaPlayerSource mMediaPlayer == null " + " " + SavorVideoView.this.hashCode());
            return false;
        }
        if (mPlayState != MediaPlayerState.IDLE) {
            LogUtils.e(TAG + " setMediaPlayerSource in illegal state: " + mPlayState + " " + SavorVideoView.this.hashCode());
//            LogFileUtil.write(TAG + " setMediaPlayerSource in illegal state: " + mPlayState + " " + SavorVideoView.this.hashCode());
            return false;
        }
        if (mMediaFiles == null || mMediaFiles.isEmpty() || mCurrentFileIndex >= mMediaFiles.size() || TextUtils.isEmpty(mMediaFiles.get(mCurrentFileIndex))) {
            LogUtils.e(TAG + " setMediaPlayerSource in garbled source, mCurrentFileIndex =  " + mCurrentFileIndex + " " + SavorVideoView.this.hashCode());
//            LogFileUtil.write(TAG + " setMediaPlayerSource in garbled source, mCurrentFileIndex =  " + mCurrentFileIndex + " " + SavorVideoView.this.hashCode());
            return false;
        }
        try {
            LogUtils.w("开始播放：" + mMediaFiles.get(mCurrentFileIndex) + " " + SavorVideoView.this.hashCode());
//            LogFileUtil.write("开始播放：" + mMediaFiles.get(mCurrentFileIndex) + " " + SavorVideoView.this.hashCode());
            mMediaPlayer.setDataSource(mMediaFiles.get(mCurrentFileIndex));
            mPlayState = MediaPlayerState.INITIALIZED;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 准备播放
     */
    private void prepareMediaPlayer() {
        LogUtils.w(TAG + " prepareMediaPlayer mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
//        LogFileUtil.write(TAG + " prepareMediaPlayer mPlayState:" + mPlayState + " mIsSurfaceCreated:" +
//                mIsSurfaceCreated + " " + SavorVideoView.this.hashCode());
        if (mIsSurfaceCreated) {
            if (mPlayState != MediaPlayerState.INITIALIZED) {
                LogUtils.e(TAG + " prepareMediaPlayer in illegal state: " + mPlayState + " " + SavorVideoView.this.hashCode());
//                LogFileUtil.write(TAG + "prepareMediaPlayer in illegal state: " + mPlayState + " " + SavorVideoView.this.hashCode());
                return;
            }
            mMediaPlayer.prepareAsync();
            mPlayState = MediaPlayerState.PREPARING;

            if (mIfShowLoading) {
                GlideImageLoader.loadImage(getContext(), Environment.getExternalStorageDirectory().getAbsolutePath() +
                        Session.get(getContext()).getLoadingPath(), mLoadingIv, 0, R.mipmap.ads);

                mLoadingIv.setVisibility(VISIBLE);
                mProgressBar.setVisibility(VISIBLE);
            }

            if (mIfHandlePrepareTimeout) {
                removeCallbacks(mPrepareTimeoutRunnable);
                postDelayed(mPrepareTimeoutRunnable, MAX_PREPARE_TIME);
            }
        }
    }

    /**
     * 准备超时，播放下一个
     */
    private Runnable mPrepareTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsLooping) {
                playNext();
            } else {
                if (mPlayStateCallback != null) {
                    boolean isLast = false;
                    if (mMediaFiles != null && mMediaFiles.size() > 0) {
                        isLast = mCurrentFileIndex == mMediaFiles.size() - 1;
                    }
                    // 回调某个视频播放出错
                    mPlayStateCallback.onMediaError(mCurrentFileIndex, isLast);
                }
            }
        }
    };

    /**
     * 重置播放器状态，并准备下次播放
     */
    private void resetAndPreparePlayer() {
        resetAndPreparePlayer(0);
    }

    /**
     * 重置播放器状态，并准备下次播放
     *
     * @param assignedPlayPosition
     */
    private void resetAndPreparePlayer(int assignedPlayPosition) {
        LogUtils.w(TAG + " resetAndPreparePlayer assignedPlayPosition = " + assignedPlayPosition + " " + SavorVideoView.this.hashCode());
//        LogFileUtil.write(TAG + "resetAndPreparePlayer assignedPlayPosition = " + assignedPlayPosition + " " + SavorVideoView.this.hashCode());
        mMediaPlayer.reset();
        mPlayState = MediaPlayerState.IDLE;

        mAssignedPlayPosition = assignedPlayPosition;

        if (mMediaFiles != null && mMediaFiles.size() > 0) {
            setAndPrepare();
        }
    }

    private void setAndPrepare() {
        if (setMediaPlayerSource()) {
            prepareMediaPlayer();
        } else {
            if (mForcePlayFromStart) {
                // 强制从头播放
                mForcePlayFromStart = false;
                mCurrentFileIndex = 0;
                mAssignedPlayPosition = 0;
            } else {
                // 播放下一个
                mCurrentFileIndex = (mCurrentFileIndex + 1) % mMediaFiles.size();
                mAssignedPlayPosition = 0;
            }

            if (mIsLooping) {
                // 重置播放器状态，以备下次播放
                resetAndPreparePlayer();
            }
        }
    }

    public boolean tryPlay() {
        LogUtils.w(TAG + " tryPlay mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
//        LogFileUtil.write(TAG + "tryPlay mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
        if (MediaPlayerState.PAUSED == mPlayState) {
            mIsPauseByOut = false;
            playInner();

            if (mIfShowPauseBtn) {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    mPlayVideoIv.setVisibility(GONE);
                } else {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            mPlayVideoIv.setVisibility(GONE);
                        }
                    });
                }
            }

            // 回调某个视频播放恢复播放
            if (mPlayStateCallback != null) {
                mPlayStateCallback.onMediaResume(mCurrentFileIndex);
            }

            return true;
        } else if (MediaPlayerState.STARTED == mPlayState)  {
            return true;
        } else {
            return false;
        }
    }

    private void playInner() {
        LogUtils.w(TAG + " playInner mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
//        LogFileUtil.write(TAG + "playInner mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
        try {
            mMediaPlayer.start();
            mPlayState = MediaPlayerState.STARTED;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            LogUtils.e(TAG + " Exception when playInner");
            LogFileUtil.write(TAG + " Exception when playInner");
        }
    }

    /**
     * 暂停播放
     */
    public boolean tryPause() {
        LogUtils.w(TAG + " tryPause mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
//        LogFileUtil.write(TAG + "tryPause mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
        if (MediaPlayerState.STARTED == mPlayState) {
            mIsPauseByOut = true;
            pauseInner();

            if (mIfShowPauseBtn) {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    mPlayVideoIv.setVisibility(VISIBLE);
                } else {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            mPlayVideoIv.setVisibility(VISIBLE);
                        }
                    });
                }
            }

            // 回调某个视频暂停
            if (mPlayStateCallback != null) {
                mPlayStateCallback.onMediaPause(mCurrentFileIndex);
            }
            return true;
        } else if (MediaPlayerState.PAUSED == mPlayState)  {
            return true;
        } else {
            return false;
        }
    }

    private void pauseInner() {
        LogUtils.w(TAG + " pauseInner mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
//        LogFileUtil.write(TAG + "pauseInner mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
        mMediaPlayer.pause();
        mPlayState = MediaPlayerState.PAUSED;
    }

    /**
     * 停止播放
     */
    public void stop() {
        LogUtils.w(TAG + " stop mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
//        LogFileUtil.write(TAG + "stop mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
        stopInner();

        mIsPauseByOut = true;
    }

    /**
     * 停止播放
     */
    private void stopInner() {
        LogUtils.w(TAG + " stopInner mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
//        LogFileUtil.write(TAG + "stopInner mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
        if (isInPlaybackState() && mMediaPlayer != null) {
            mMediaPlayer.stop();
            mPlayState = MediaPlayerState.STOPPED;
        }
    }

    /**
     * 所在页面onPause时请调用此方法处理，类似的还有{@link #onResume()}
     */
    public void onPause() {
        if (mMediaPlayer != null) {
            try {
                // 记录播放进度
                mAssignedPlayPosition = mMediaPlayer.getCurrentPosition();
            } catch (Exception e) {
                e.printStackTrace();
            }
            release();
        }
    }

    public void onStop() {
        LogUtils.w(TAG + " onPause mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
//        LogFileUtil.write(TAG + "onPause mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());

        removeCallbacks(mPrepareTimeoutRunnable);

        if (mPlayStateCallback != null) {
            mPlayStateCallback.onMediaPause(mCurrentFileIndex);
        }
//        mMediaPlayer.reset();
//        mPlayState = MediaPlayerState.IDLE;
    }

    /**
     * 所在页面onResume时请调用此方法处理，类似的还有{@link #onPause()}
     */
    public void onResume() {
        LogUtils.w(TAG + " onResume mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
//        LogFileUtil.write(TAG + "onResume mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
        if (mMediaPlayer == null) {
            initMediaPlayer();
        }

        mIsPauseByOut = false;
        if (mPlayState == MediaPlayerState.IDLE && mMediaFiles != null && mMediaFiles.size() > 0) {
            setAndPrepare();
        }
    }

    /**
     * 停止播放并释放MediaPlayer
     */
    public void release() {
        LogUtils.w(TAG + " release mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
//        LogFileUtil.write(TAG + "release mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
                if (mMediaPlayer != null) {
                    stopInner();

                    if (mMediaPlayer != null) {
                        mMediaPlayer.release();
                    }
                    mPlayState = MediaPlayerState.END;
                    mMediaPlayer = null;
                }
//            }
//        }).start();

        if (mIfShowPauseBtn) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                mPlayVideoIv.setVisibility(GONE);
            } else {
                post(new Runnable() {
                    @Override
                    public void run() {
                        mPlayVideoIv.setVisibility(GONE);
                    }
                });
            }
        }
    }

    /**
     * 设置播放源
     *
     * @param mediaFiles
     */
    public void setMediaFiles(ArrayList<String> mediaFiles) {
        setMediaFiles(mediaFiles, 0, 0);
    }

    /**
     * 设置播放源
     *
     * @param mediaFiles       文件路径集合
     * @param currentFileIndex 要播放的文件序号
     * @param playPosition     文件播放进度
     */
    public void setMediaFiles(ArrayList<String> mediaFiles, int currentFileIndex, int playPosition) {
        LogUtils.w(TAG + "setMediaFiles mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
//        LogFileUtil.write(TAG + "setMediaFiles mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
        mIsPauseByOut = false;
        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            mCurrentFileIndex = currentFileIndex;
            mAssignedPlayPosition = playPosition;
            mMediaFiles = mediaFiles;

//            if (mPlayState == MediaPlayerState.STARTED || mPlayState == MediaPlayerState.PAUSED) {
//                // 重设播放源集合后，将 mForcePlayFromStart置true强制从头播放
//                mForcePlayFromStart = true;
//            }

            if (mMediaPlayer == null) {
                initMediaPlayer();
                if (mIsSurfaceCreated) {
                    LogUtils.d("Will setDisplay in setMediaFiles() when surface is created");
                    mMediaPlayer.setDisplay(mSurfaceHolder);
                }
            }

            // 非IDLE状态时先重置再设置
            if (mPlayState != MediaPlayerState.IDLE) {
                mMediaPlayer.reset();
                mPlayState = MediaPlayerState.IDLE;
            }
            mPlayVideoIv.setVisibility(GONE);

            setAndPrepare();
        }
    }

    /**
     * 继续、暂停 播放
     */
    public void togglePlay() {
        LogUtils.w(TAG + "togglePlay mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
//        LogFileUtil.write(TAG + "togglePlay mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
        if (MediaPlayerState.PAUSED == mPlayState) {
            tryPlay();
        } else if (MediaPlayerState.STARTED == mPlayState) {
            tryPause();
        }
    }

    /**
     * 播放下一条
     */
    public void playNext() {
        LogUtils.w(TAG + " playNext mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
//        LogFileUtil.write(TAG + "playNext mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
        stopInner();

        mIsPauseByOut = false;
        mAssignedPlayPosition = 0;
        mCurrentFileIndex = (mCurrentFileIndex + 1) % mMediaFiles.size();
        LogUtils.w(TAG + " mCurrentFileIndex:" + mCurrentFileIndex + " size = " + mMediaFiles.size() + " " + SavorVideoView.this.hashCode());
        resetAndPreparePlayer();
    }

    /**
     * 播放上一条
     */
    public void playPrevious() {
        LogUtils.w(TAG + " playPrevious mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
//        LogFileUtil.write(TAG + " playPrevious mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
        stopInner();

        mIsPauseByOut = false;
        mAssignedPlayPosition = 0;
        mCurrentFileIndex =(mCurrentFileIndex - 1 + mMediaFiles.size()) % mMediaFiles.size();
        LogUtils.w(TAG + " mCurrentFileIndex:" + mCurrentFileIndex + " size = " + mMediaFiles.size() + " " + SavorVideoView.this.hashCode());
        resetAndPreparePlayer();
    }

    /**
     * 调整播放进度
     *
     * @param position
     */
    public void seekTo(int position) {
        LogUtils.w(TAG + " seek mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
//        LogFileUtil.write(TAG + "seek mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(position);
        } else {
            mAssignedPlayPosition = position;
        }
    }

    /**
     * 播放器是否处于可播放的状态
     *
     * @return
     */
    public boolean isInPlaybackState() {
        return mPlayState == MediaPlayerState.PAUSED ||
                mPlayState == MediaPlayerState.STARTED ||
                mPlayState == MediaPlayerState.COMPLETED ||
                mPlayState == MediaPlayerState.PREPARED;
    }

    /**
     * 是否显示开始、暂停按钮，默认不显示
     *
     * @param ifShowPauseBtn
     */
    public void setIfShowPauseBtn(boolean ifShowPauseBtn) {
        mIfShowPauseBtn = ifShowPauseBtn;
    }

    /**
     * 获取播放进度，非播放状态调用时返回-1
     *
     * @return
     */
    public int getCurrentPosition() {
        LogUtils.w(TAG + " getCurrentPosition mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
//        LogFileUtil.write(TAG + "getCurrentPosition mPlayState:" + mPlayState + " " + SavorVideoView.this.hashCode());
        if (isInPlaybackState())
            return mMediaPlayer.getCurrentPosition();
        else
            return -1;
    }

    /**
     * 设置播放状态监听事件回调
     *
     * @param playStateCallback
     */
    public void setPlayStateCallback(PlayStateCallback playStateCallback) {
        mPlayStateCallback = playStateCallback;
    }

    /**
     * 设置是否处理Prepare超时回调，目前只有投屏会处理
     *
     * @param ifHandlePrepareTimeout
     */
    public void setIfHandlePrepareTimeout(boolean ifHandlePrepareTimeout) {
        mIfHandlePrepareTimeout = ifHandlePrepareTimeout;
    }

    /**
     * 是否自动播放下一个
     * 默认是自动播放
     * @param looping
     */
    public void setLooping(boolean looping) {
        mIsLooping = looping;
    }

    public void setIfShowLoading(boolean ifShowLoading) {
        mIfShowLoading = ifShowLoading;
    }


    public interface PlayStateCallback {
        /**
         * 某个视频播放完毕
         * @param index 当前视频序号
         * @param isLast 是否是最后一个
         * @return true: 播放源被设置新的； false: otherwise
         */
        boolean onMediaComplete(int index, boolean isLast);
        /**
         * 某个视频播放出错
         * @param index 当前视频序号
         * @param isLast 是否是最后一个
         * @return true: 播放源被设置新的； false: otherwise
         */
        boolean onMediaError(int index, boolean isLast);
        /**
         * 视频准备完毕
         * @param index 当前视频序号
         * @return true: 播放被中止； false: otherwise
         */
        boolean onMediaPrepared(int index);

        void onMediaPause(int index);

        void onMediaResume(int index);
    }
}
