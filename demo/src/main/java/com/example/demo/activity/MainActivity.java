package com.example.demo.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.demo.R;
import com.example.vin19.audiovideo.camera.CameraManager;

/**
 * Created by vin19 on 2017/4/30.
 */

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    public static final String TAG = "MainActivity";
    public static final boolean DEBUG = true;

    SurfaceView mPreviewSfv;
    SurfaceHolder mHolder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        mPreviewSfv = (SurfaceView) findViewById(R.id.prv_sfv);
        mHolder = mPreviewSfv.getHolder();
        mHolder.addCallback(this);
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
        CameraManager.getInstance(getApplicationContext()).initCamera(width,height,mHolder);
        CameraManager.getInstance(getApplicationContext()).startPreview();
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
}
