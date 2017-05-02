package com.example.vin19.audiovideo.codec;

import android.content.Context;
import android.os.Environment;
import android.util.Log;


import com.example.vin19.audiovideo.camera.CameraManager;

import java.io.IOException;

/**
 * date: 2017/3/23 0023
 * author: cbs
 * 音视频编码器管理类
 */

public class MediaCodecManager {

    public static final String TAG = "MediaCodecManager";

    private static MediaCodecManager sInstance;

    CameraManager mCameraManager;

    AudioEncoder mAudioEncoder;
    VideoEncoder mVideoEncoder;

    int mEncodeWidth;
    int mEncodeHeight;
    int mEncodeFrameRate;
    int mEncodeBitRate;
    int mCameraOrientation;

    boolean isRecording;

    /**
     * 默认构造函数
     */
    private MediaCodecManager() {

    }

    public void init(Context context,int encodeWidth, int encodeHeight, int frameRate, int bitRate, int cameraOrientation) {
        mCameraManager = CameraManager.getInstance(context);
        mEncodeWidth = encodeWidth;
        mEncodeHeight = encodeHeight;
        mEncodeFrameRate = frameRate;
        mEncodeBitRate = bitRate;
        mCameraOrientation = cameraOrientation;
    }

    public static MediaCodecManager getInstance(Context context,int encodeWidth,int encodeHeight,int frameRate,int bitRate,int cameraOrientation) {
        synchronized (MediaCodecManager.class) {
            if (null == sInstance) {
                synchronized (MediaCodecManager.class) {
                    sInstance = new MediaCodecManager();
                }
            }
        }
        sInstance.init(context,encodeWidth,encodeHeight,frameRate,bitRate,cameraOrientation);
        return sInstance;
    }

    /**
     * 初始化音视频编码器
     */
    public void prepare() {
        initAudioEncoder();
        initVideoEncoder();
    }

    public void start() {
        isRecording = true;
        if (null != mVideoEncoder) {
            try {
                mVideoEncoder.start();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (null != mAudioEncoder) {
            try {
                mAudioEncoder.start();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void feedData(byte[] data, long timestamp) {
        if (null != mVideoEncoder) {
            mVideoEncoder.feedData(data,timestamp);
        }
    }

    public void setVideoEncoderRotation(int rotation) {
        if (null != mVideoEncoder) {
            mVideoEncoder.setRotation(rotation);
        }
    }

    public void stop() {
//        if (null != mCameraManager.getCamera()) {
//            mCameraManager.stopPreview();
//            mCameraManager.releaseCamera();
//        }
        isRecording = false;
        if (null != mAudioEncoder) {
            mAudioEncoder.stop();
        }
        if (null != mVideoEncoder) {
            mVideoEncoder.stop();
        }
    }

    private void initAudioEncoder() {
        if (null == mAudioEncoder) {
            mAudioEncoder = new AudioEncoder();
            mAudioEncoder.setSavePath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "/test.m4a");
        }
        try {
            mAudioEncoder.prepare();
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    private void initVideoEncoder() {
        if (null == mVideoEncoder) {
            mVideoEncoder = new VideoEncoder();
            mVideoEncoder.setSavePath(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath() + "/test.mp4");
            mVideoEncoder.setFrameRate(mEncodeFrameRate);
        }
        try {
            if(mCameraOrientation == 90 || mCameraOrientation == 270) {
                mVideoEncoder.prepare(mCameraManager.getPreviewSize().height, mCameraManager.getPreviewSize().width, mCameraOrientation);
            }else {
                mVideoEncoder.prepare(mCameraManager.getPreviewSize().width, mCameraManager.getPreviewSize().height, mCameraOrientation);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getEncodeWidth() {
        return mVideoEncoder.getWidth();
    }

    public int getEncodeHeight() {
        return mVideoEncoder.getHeight();
    }

    public int getBitRate() {
        return mEncodeBitRate;
    }

    public int getSampleRate() {
        return mAudioEncoder.getSampleRate();
    }
}

