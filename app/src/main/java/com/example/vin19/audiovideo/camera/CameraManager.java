package com.example.vin19.audiovideo.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.example.vin19.audiovideo.R;

import java.io.IOException;
import java.util.List;

/**
 * date: 2017/3/21 0021
 * author: cbs
 */

public class CameraManager implements Camera.PreviewCallback {

    public static final String TAG = "CameraManager";

    private static CameraManager sInstance;

    Context mContext;

    int mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    Camera mCamera;

    Camera.Size mPreviewSize;

    boolean isCameraConfig = false;
    boolean isPreviewing = false;

    float mRatioTolerance = 0.2f;
    //帧数据buffer
    byte[] mPreviewCallbackBuffer;

    /**
     * 帧数据回调接口
     */
    CameraFrameCallback mFrameCallback;

    private CameraManager(Context context) {
        this.mContext = context;
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

    public void setFrameCallback(CameraFrameCallback callback) {
        this.mFrameCallback = callback;
    }

    public Camera.Size getPreviewSize() {
        return mPreviewSize;
    }

    public int getCameraId() {
        return mCameraId;
    }

    public void setCameraId(int cameraId) {
        mCameraId = cameraId;
    }

    public Camera getCamera() {
        return mCamera;
    }
    /**
     * 打开摄像头
     */
    public void openCamera(int cameraId) {
        //防止摄像头没被释放
        stopPreview();
        releaseCamera();
        mCamera = Camera.open(cameraId);
        mCameraId = cameraId;
        if (null == mCamera) {
            throw new CameraNotFoundException(mContext.getString(R.string.av_open_camera_error));
        }
    }

    /**
     * 预览初始化
     * @param holder
     * @param width
     * @param height
     */
    public void initPreview(SurfaceHolder holder,int width, int height,int encodeWidth,int encodeHeight) {
        if (null == mCamera) {
            openCamera(mCameraId);
        }
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewFormat(ImageFormat.NV21);
        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);

        final List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        setCameraRotation(mCameraId);

        //计算合适的预览尺寸
        mPreviewSize = getBestPreviewSize(width, height, encodeWidth, encodeHeight, parameters);
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

        mCamera.setParameters(parameters);

        mPreviewCallbackBuffer = new byte[mPreviewSize.width * mPreviewSize.height * 3 / 2];
        mCamera.addCallbackBuffer(mPreviewCallbackBuffer);
        mCamera.setPreviewCallbackWithBuffer(this);
        isCameraConfig = true;
    }

    public void startPreview() {
        if (null != mCamera && isCameraConfig) {
            mCamera.startPreview();
            isPreviewing = true;
        }
    }

    public void stopPreview() {
        if (null != mCamera && isPreviewing) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            isPreviewing = false;
        }
    }

    public void releaseCamera() {
        if (null != mCamera) {
            mCamera.release();
            mCamera = null;
        }
    }

    private void setCameraRotation(int cameraId) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);

        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        int degree = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 0;
                break;
            case Surface.ROTATION_90:
                degree = 90;
                break;
            case Surface.ROTATION_180:
                degree = 180;
                break;
            case Surface.ROTATION_270:
                degree = 270;
                break;
        }
        int result;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degree) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (cameraInfo.orientation - degree + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }

    private Camera.Size getBestPreviewSize(int width, int height,int encodeWidth,int encodeHeight, Camera.Parameters parameters) {
        Camera.Size result = null;
//        int resultDiff = 0;
        float ratio = (float) Math.max(width, height) / Math.min(width, height);
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        for (Camera.Size size : sizes) {
            float sizeRatio = (float) Math.max(size.width, size.height) / Math.min(size.width, size.height);
            if (Math.abs(sizeRatio - ratio) <= mRatioTolerance) {
                if (null == result) {
                    result = size;
                }else {
                    int oldDiff = Math.abs(result.width + result.height - encodeWidth - encodeHeight);
                    int newDiff = Math.abs(size.width + size.height - encodeWidth - encodeHeight);
                    if (newDiff < oldDiff) {
                        result = size;
                    }
                }
            }
        }
        return result;
    }

    long time;
    boolean isTimeInit = false;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (null != mFrameCallback) {
            if (!isTimeInit) {
                time = System.nanoTime() / 1000;
                isTimeInit = true;
            }
            if(!isTimeInit) {
                mFrameCallback.onFrame(data, time);
            }else {
                mFrameCallback.onFrame(data, (System.nanoTime() - time) / 1000);
            }
        }
        camera.addCallbackBuffer(data);
    }

    public interface CameraFrameCallback {
        void onFrame(byte[] data, long timestamp);
    }
}
