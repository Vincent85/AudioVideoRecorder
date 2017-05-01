package com.example.demo.activity;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.demo.R;
import com.example.vin19.audiovideo.camera.CameraManager;
import com.example.vin19.audiovideo.codec.AudioEncoder;
import com.example.vin19.audiovideo.codec.VideoEncoder;

import java.io.File;
import java.io.IOException;

/**
 * Created by vin19 on 2017/4/30.
 */

public class MainActivity extends Activity implements SurfaceHolder.Callback,View.OnClickListener,CameraManager.OnPreviewFrameCallback {

    public static final String TAG = "MainActivity";
    public static final boolean DEBUG = true;

    SurfaceView mPreviewSfv;
    SurfaceHolder mHolder;

    Button mSwitchBtn;
    Button mRecordBtn;

    boolean isRecording = false;

    AudioEncoder mAudioEncoder;
    VideoEncoder mVideoEncoder;

    int mWidth;
    int mHeight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        initView();
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
        CameraManager.getInstance(getApplicationContext()).openCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "surfaceChanged width = " + width + ",height = " + height);
        }
        mWidth = width;
        mHeight = height;
        CameraManager.getInstance(getApplicationContext()).initCamera(width,height,mHolder);
        CameraManager.getInstance(getApplicationContext()).startPreview(this);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (DEBUG) {
            Log.d(TAG, "surfaceDestroy");
        }
        CameraManager.getInstance(getApplicationContext()).close();
    }

    @Override
    public void onDestroy() {
        mHolder.removeCallback(this);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.switch_btn) {
            CameraManager.getInstance(getApplicationContext()).switchCamera();
        } else if (v.getId() == R.id.record_btn) {
            if (!isRecording) {
                startRecord();
                isRecording = true;
                mRecordBtn.setText(getString(R.string.stop_record));
            }else {
                stopRecord();
                Toast.makeText(this,"stopRecording",Toast.LENGTH_LONG).show();
                isRecording = false;
                mRecordBtn.setText(getString(R.string.start_record));
            }
        }
    }

    private void startRecord() {
        if (null == mAudioEncoder) {
            mAudioEncoder = new AudioEncoder();
        }
        mAudioEncoder.setSavedPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath()
                + File.separator + "testAudio.m4a");

        mAudioEncoder.prepare();

        mAudioEncoder.start();

        if (null == mVideoEncoder) {
            mVideoEncoder = new VideoEncoder();
        }
        mVideoEncoder.setSavePath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath()
                + File.separator + "testVideo.mp4");

        Camera.Size previewSize = CameraManager.getInstance(getApplicationContext()).getPreviewSize();
        mVideoEncoder.prepare(previewSize.width,previewSize.height);
        mVideoEncoder.start();
    }

    private void stopRecord() {
        if (null != mAudioEncoder) {
            mAudioEncoder.stop();
            mAudioEncoder = null;
        }

        if (null != mVideoEncoder) {
            mVideoEncoder.stop();
            mVideoEncoder = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopRecord();
    }

//    boolean isFirst = true;
//    long startTime;
//    @Override
//    public void onPreviewFrame(byte[] data, Camera camera) {
//        if (null != mVideoEncoder) {
//            if (isFirst) {
//                mVideoEncoder.feedData(data,0);
//                startTime = System.nanoTime();
//            }else {
//                mVideoEncoder.feedData(data,System.nanoTime() - startTime);
//            }
//        }
//    }

    @Override
    public void onFrame(byte[] data, long timestamp) {
        if (null != mVideoEncoder) {
            mVideoEncoder.feedData(data,timestamp);
        }
    }
}
