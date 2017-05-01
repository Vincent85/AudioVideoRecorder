package com.example.vin19.audiovideo.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import com.example.vin19.audiovideo.util.VideoUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by vin19 on 2017/5/1.
 */

public class VideoEncoder implements Runnable{

    private static final String TAG = "VideoEncoder";
    private static final boolean DEBUG = true;

    private static final int DEQUE_TIME_OUT = 2000;

    private MediaCodec mEncoder;
    private String mime="video/avc";
    private int rate=256000;
    private int frameRate=24;
    private int frameInterval=1;

    private int fpsTime;

    private Thread mThread;
    private boolean isEncoding = false;
    private int mWidth;
    private int mHeight;
    private byte[] mHeadInfo=null;

    private byte[] nowFeedData;
    private long nowTimeStamp;
    private boolean hasNewData=false;

    private FileOutputStream mFos;
    private String mSavePath;

    public VideoEncoder(){
        fpsTime=1000/frameRate;
    }

    public void setMime(String mime){
        this.mime=mime;
    }

    public void setRate(int rate){
        this.rate=rate;
    }

    public void setFrameRate(int frameRate){
        this.frameRate=frameRate;
    }

    public void setFrameInterval(int frameInterval){
        this.frameInterval=frameInterval;
    }

    public void setSavePath(String path){
        this.mSavePath=path;
    }


    /**
     * 准备录制
     * @param width 视频宽度
     * @param height 视频高度
     * @throws IOException
     */
    public void prepare(int width,int height) {
        mHeadInfo=null;
        this.mWidth =width;
        this.mHeight =height;
        File file=new File(mSavePath);
        File folder=file.getParentFile();
        if(!folder.exists()){
            boolean b=folder.mkdirs();
            Log.e(TAG,"create "+folder.getAbsolutePath()+" "+b);
        }
        if(file.exists()){
            boolean b=file.delete();
        }
        try {
            mFos =new FileOutputStream(mSavePath);
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        MediaFormat format=MediaFormat.createVideoFormat(mime,width,height);
        format.setInteger(MediaFormat.KEY_BIT_RATE,rate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE,frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,frameInterval);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities
                .COLOR_FormatYUV420Planar);
        try {
            mEncoder = MediaCodec.createEncoderByType(mime);
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        mEncoder.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    public void start() {
        if (null != mThread && mThread.isAlive()) {
            isEncoding = false;
            try {
                mThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }

        mEncoder.start();
        isEncoding = true;
        mThread = new Thread(this);
        mThread.start();
    }

    public void stop() {
        isEncoding = false;
        try {
            mThread.join();
        } catch (InterruptedException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        mEncoder.release();
        mEncoder = null;
        try {
            mFos.flush();
            mFos.close();
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    /**
     * 由外部喂入一帧数据
     * @param data
     * @param timestamp
     */
    public void feedData(byte[] data, long timestamp) {
        nowFeedData = data;
        hasNewData = true;
        nowTimeStamp = timestamp;
    }

    private ByteBuffer getInputBuffer(int index) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mEncoder.getInputBuffer(index);
        }else {
            return mEncoder.getInputBuffers()[index];
        }
    }

    private ByteBuffer getOutputBuffer(int index) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mEncoder.getOutputBuffer(index);
        }else {
            return mEncoder.getOutputBuffers()[index];
        }
    }

    byte[] mFrameData;
    //定时调用，如果没有新数据，就用上一个数据
    public void readOutputData(byte[] data,long timestamp) {
        int index = mEncoder.dequeueInputBuffer(-1);
        if (index >= 0) {
            if (hasNewData) {
                if (null == mFrameData) {
                    mFrameData = new byte[mWidth * mHeight * 3 / 2];
                }
                VideoUtil.NV21ToYUV420P(data,mFrameData,mWidth,mHeight);
            }
            ByteBuffer buffer = getInputBuffer(index);
            buffer.clear();
            buffer.put(mFrameData);

            mEncoder.queueInputBuffer(index, 0, data.length, timestamp, 0);
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputIndex = mEncoder.dequeueOutputBuffer(bufferInfo, DEQUE_TIME_OUT);
        while (outputIndex >= 0) {
            ByteBuffer buffer = getOutputBuffer(outputIndex);
            byte[] temp = new byte[bufferInfo.size];
            buffer.get(temp);
            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                if (DEBUG) {
                    Log.d(TAG, "start frame");
                }
                mHeadInfo = new byte[temp.length];
                mHeadInfo = temp;
            } else if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                if (DEBUG) {
                    Log.e(TAG,"key frame");
                }
                byte[] keyframe = new byte[temp.length + mHeadInfo.length];
                System.arraycopy(mHeadInfo, 0, keyframe, 0, mHeadInfo.length);
                System.arraycopy(temp, 0, keyframe, mHeadInfo.length, temp.length);
                if (DEBUG) {
                    Log.e(TAG,"other->"+bufferInfo.flags);
                }
                try {
                    mFos.write(keyframe,0,keyframe.length);
                } catch (IOException e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
            } else if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                if (DEBUG) {
                    Log.d(TAG, "end of stream");
                }
            }else {
                try {
                    mFos.write(temp,0,temp.length);
                } catch (IOException e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
            }

            mEncoder.releaseOutputBuffer(outputIndex,false);
            outputIndex = mEncoder.dequeueOutputBuffer(bufferInfo, DEQUE_TIME_OUT);
        }

    }

    @Override
    public void run() {
        while (isEncoding){
            long time=System.currentTimeMillis();
            if(nowFeedData!=null){
                readOutputData(nowFeedData, nowTimeStamp);
            }
            long lt=System.currentTimeMillis()-time;
            if(fpsTime>lt){
                try {
                    Thread.sleep(fpsTime-lt);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
