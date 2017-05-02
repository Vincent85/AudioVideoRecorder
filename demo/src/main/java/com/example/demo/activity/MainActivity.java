package com.example.demo.activity;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.example.demo.R;
import com.example.vin19.audiovideo.camera.CameraManager;
import com.example.vin19.audiovideo.codec.AudioEncoder;
import com.example.vin19.audiovideo.codec.MediaCodecManager;
import com.example.vin19.audiovideo.codec.VideoEncoder;

import java.io.File;

/**
 * Created by vin19 on 2017/4/30.
 */

public class MainActivity extends Activity implements SurfaceHolder.Callback,View.OnClickListener{

    public static final String TAG = "MainActivity";
    public static final boolean DEBUG = true;

    SurfaceView mPreviewSfv;
    SurfaceHolder mHolder;

    Button mSwitchBtn;
    Button mRecordBtn;

    boolean isRecording = false;

    AudioEncoder mAudioEncoder;
    VideoEncoder mVideoEncoder;

    int mSurfaceWidth;
    int mSurfaceHeight;

    int mEncodeWidth = 1280;
    int mEncodeHeight = 720;
    int mEncodeFrameRate = 30;
    int mEncodeBitRate = 2000000;


    boolean isOrientationInit = false;
    int mCameraOrientation = 0;

    //屏幕朝向监听器
    ScreenOrientationListener mOrientationListener;
    //帧数据回调
    OnFrameCallback mCallback;
    //摄像头管理
    CameraManager mCameraManager;
    //编码器管理
    MediaCodecManager mMediaCodecManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 去掉标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main_layout);

        initView();

        mOrientationListener = new ScreenOrientationListener(this);

        mCameraManager = CameraManager.getInstance(getApplicationContext());
        mCallback = new OnFrameCallback();
        mCameraManager.setFrameCallback(mCallback);


    }

    private void initView() {
        mPreviewSfv = (SurfaceView) findViewById(R.id.prv_sfv);
        mHolder = mPreviewSfv.getHolder();
        mHolder.addCallback(this);
        mHolder.setKeepScreenOn(true);

        mSwitchBtn = (Button) findViewById(R.id.switch_btn);
        mSwitchBtn.setOnClickListener(this);

        mRecordBtn = (Button) findViewById(R.id.record_btn);
        mRecordBtn.setOnClickListener(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (DEBUG) {
            Log.d(TAG, "surfaceCreated");
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "surfaceChanged width = " + width + ",height = " + height);
        }
        mSurfaceWidth = width;
        mSurfaceHeight = height;

        mCameraManager.openCamera(mCameraManager.getCameraId());
        mOrientationListener.enable();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (DEBUG) {
            Log.d(TAG, "surfaceDestroy");
        }
        mOrientationListener.disable();
    }

    @Override
    public void onDestroy() {
        mHolder.removeCallback(this);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.switch_btn) {
            switchCamera();
        } else if (v.getId() == R.id.record_btn) {
            if (!isRecording) {
                startRecord();
                isRecording = true;
                mRecordBtn.setText(getString(R.string.stop_record));
            }else {
                stopRecord();
                isRecording = false;
                mRecordBtn.setText(getString(R.string.start_record));
            }
        }
    }

    boolean hasSwitch = false;
    private void switchCamera() {
        mCameraManager.setCameraId(mCameraManager.getCameraId() == Camera.CameraInfo.CAMERA_FACING_FRONT ?
                Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT);
        mCameraManager.stopPreview();
        mCameraManager.releaseCamera();

        isOrientationInit = false;
        hasSwitch = true;
        mOrientationListener.enable();
    }

    private void startRecord() {
//        if (null == mAudioEncoder) {
//            mAudioEncoder = new AudioEncoder();
//        }
//        mAudioEncoder.setSavedPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath()
//                + File.separator + "testAudio.m4a");
//
//        mAudioEncoder.prepare();
//
//        mAudioEncoder.start();
//
//        if (null == mVideoEncoder) {
//            mVideoEncoder = new VideoEncoder();
//        }
//        mVideoEncoder.setSavePath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath()
//                + File.separator + "testVideo.mp4");
//
//        Camera.Size previewSize = CameraManager.getInstance(getApplicationContext()).getPreviewSize();
//        mVideoEncoder.prepare(previewSize.width,previewSize.height);
//        mVideoEncoder.start();

        if (null == mMediaCodecManager) {
            mMediaCodecManager = MediaCodecManager.getInstance(getApplicationContext(),
                    mEncodeWidth, mEncodeHeight, mEncodeFrameRate, mEncodeBitRate, mCameraOrientation);
        }
        mMediaCodecManager.prepare();
        mMediaCodecManager.start();

    }

    private void stopRecord() {
        if (null != mMediaCodecManager) {
            mMediaCodecManager.stop();
        }
    }

    private void stopPreview() {
        if (null != mCameraManager) {
            mCameraManager.stopPreview();
            mCameraManager.releaseCamera();
        }
    }
    @Override
    public void onPause() {
        stopRecord();
        stopPreview();
        super.onPause();
    }


    class OnFrameCallback implements CameraManager.CameraFrameCallback {

        @Override
        public void onFrame(byte[] data, long timestamp) {
            mMediaCodecManager.feedData(data,timestamp);
        }
    }

    class ScreenOrientationListener extends OrientationEventListener {

        public ScreenOrientationListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation == ORIENTATION_UNKNOWN) {
                return;
            }

            if (!isOrientationInit) {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(mCameraManager.getCameraId(), cameraInfo);

                orientation = (orientation + 45) / 90 * 90;
                int rotation = 0;
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    rotation = (cameraInfo.orientation - orientation + 360) % 360;
                } else {  // back-facing camera
                    rotation = (cameraInfo.orientation + orientation) % 360;
                }

                mCameraOrientation = rotation;

                isOrientationInit = true;
            }

            //启动预览
            mCameraManager.initPreview(mHolder,mSurfaceWidth,mSurfaceHeight,mEncodeWidth,mEncodeHeight);
            mCameraManager.startPreview();

            if (null == mMediaCodecManager) {
                mMediaCodecManager = MediaCodecManager.getInstance(getApplicationContext(),
                        mEncodeWidth, mEncodeHeight, mEncodeFrameRate, mEncodeBitRate, mCameraOrientation);
            }else {
                mMediaCodecManager.init(getApplicationContext(),
                        mEncodeWidth, mEncodeHeight, mEncodeFrameRate, mEncodeBitRate, mCameraOrientation);
            }

            if (hasSwitch) {
                mMediaCodecManager.setVideoEncoderRotation(mCameraOrientation);
            }
            mOrientationListener.disable();
        }
    }
}
