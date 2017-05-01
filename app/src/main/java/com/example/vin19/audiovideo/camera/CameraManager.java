package com.example.vin19.audiovideo.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.example.vin19.audiovideo.R;

import java.io.IOException;
import java.util.List;

/**
 * Created by vin19 on 2017/4/30.
 */

public class CameraManager {

    public static final String TAG = "CameraManager";

    private static CameraManager sInstance;

    private Context mContext;

    private Camera mCamera;
    private int mCameraId;

    private CameraManager(Context context) {
        mContext = context;
    }

    public static CameraManager getInstance(Context context) {
        synchronized (CameraManager.class) {
            if (null == sInstance) {
                synchronized (CameraManager.class) {
                    sInstance = new CameraManager(context);
                }
            }
        }
        return sInstance;
    }

    public void openCamera() {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        if (null == mCamera) {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        if (null == mCamera) {
            mCameraId = -1;
            throw new CameraException(mContext.getString(R.string.av_open_camera_error));
        }
    }

    public void initCamera(int width, int height, SurfaceHolder holder) {


        initCameraParameter(width, height);

        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            Log.e(TAG, "setPreviewDisplay error " + e.getLocalizedMessage());
        }
    }

    public void startPreview() {
        if (null != mCamera) {
            mCamera.startPreview();
        }
    }
    public void close() {
        if (null != mCamera) {
            mCamera.stopPreview();
            mCamera = null;
        }
    }

    private void initCameraParameter(int width,int height) {
        Camera.Parameters parameters = mCamera.getParameters();

        Camera.Size previewSize = getPreviewSize(parameters, width, height);
        parameters.setPreviewSize(previewSize.width, previewSize.height);

        setCameraPreviewRotation();
        mCamera.setParameters(parameters);
    }

    private Camera.Size getPreviewSize(Camera.Parameters parameters,int width, int height) {
        List<Camera.Size> supportedSizes =  parameters.getSupportedPreviewSizes();
        Camera.Size result = null;
        for (Camera.Size size : supportedSizes) {
            if (null == result) {
                result = size;
            }else {
                int diff1 = Math.abs(size.width - width) + Math.abs(size.height - height);
                int diff2 = Math.abs(size.width - result.width) + Math.abs(size.height - result.height);
                if (diff1 < diff2) {
                    result = size;
                }
            }
        }
        return result;
    }

    private void setCameraPreviewRotation() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, cameraInfo);

        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();

        int degree = 0;
        switch (rotation) {
            case Surface.ROTATION_0 : degree = 0; break;
            case Surface.ROTATION_90 : degree = 90; break;
            case Surface.ROTATION_180: degree = 180; break;
            case Surface.ROTATION_270: degree = 270; break;
        }

        int result = 0;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degree) % 360;
            result = (360 - result) % 360;
        }else {
            result = (cameraInfo.orientation - degree + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }

}
