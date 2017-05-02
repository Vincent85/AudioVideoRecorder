package com.example.vin19.audiovideo.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;


import com.example.vin19.audiovideo.util.VideoUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Description:
 */
public class VideoEncoder implements Runnable {

    private static final String TAG="VideoEncoder";
    private MediaCodec mEnc;
    private String mime="video/avc";
    private int rate=4000000;
    private int frameRate=20;
    private int frameInterval=10;
    private int mColorFormat;

    private int fpsTime;

    private Thread mThread;
    private boolean mStartFlag=false;
    private int width;
    private int height;
    private byte[] mHeadInfo=null;

    private byte[] nowFeedData;
    private long nowTimeStep;
    private boolean hasNewData=false;

    private FileOutputStream fos;
    private String mSavePath;

    byte[] temp;

    int mRotation;

    public VideoEncoder(){
        fpsTime=1000/frameRate;
    }

    public void setMime(String mime){
        this.mime=mime;
    }

    public void setRate(int rate){
        this.rate=rate;
    }

    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
    public int getBitRate() {
        return rate;
    }

    public void setFrameRate(int frameRate){
        this.frameRate=frameRate;
        this.fpsTime=1000/frameRate;
    }

    public void setRotation(int rotation) {
        this.mRotation = rotation;
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
    public void prepare(int width,int height,int rotation) throws IOException {
        mRotation = rotation;
        mHeadInfo=null;
        this.width=width;
        this.height=height;
        File file=new File(mSavePath);
        File folder=file.getParentFile();
        if(!folder.exists()){
            boolean b=folder.mkdirs();
            Log.e("wuwang","create "+folder.getAbsolutePath()+" "+b);
        }
        if(file.exists()){
            boolean b=file.delete();
        }
        fos=new FileOutputStream(mSavePath);

        MediaFormat format;
        format = MediaFormat.createVideoFormat(mime, width, height);
        format.setInteger(MediaFormat.KEY_BIT_RATE,rate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE,frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,frameInterval);
        //选择支持的颜色空间
        MediaCodecInfo codecInfo = selectCodec(mime);
        mColorFormat = selectColorFormat(codecInfo, mime);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);
        mEnc= MediaCodec.createEncoderByType(mime);
        mEnc.configure(format,null,null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    /**
     * 开始录制
     * @throws InterruptedException
     */
    public void start() throws InterruptedException {
        if(mThread!=null&&mThread.isAlive()){
            mStartFlag=false;
            mThread.join();
        }
        mEnc.start();
        mStartFlag=true;
        mThread=new Thread(this);
        mThread.start();
    }

    /**
     * 停止录制
     */
    public void stop(){
        try {
            mStartFlag=false;
            mThread.join();
            mEnc.stop();
            mEnc.release();
            fos.flush();
            fos.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    class VideoSourceData {
        VideoSourceData(byte[] data, long timeStep){
            this.data = data;
            this.timeStep = timeStep;
        }
        byte[] data;
        long timeStep;
    }
    BlockingQueue<VideoSourceData> videoSourceData = new LinkedBlockingQueue<>();

    /**
     * 由外部喂入一帧数据
     * 同时处理角度旋转，颜色空间转换
     * @param data RGBA数据
     * @param timeStep camera附带时间戳
     */
    public void feedData(final byte[] data, final long timeStep){
        hasNewData=true;
        nowFeedData=data;
        nowTimeStep=timeStep;


        if(temp==null){
            temp=new byte[width*height*3/2];
        }
        byte[] rotated;
        if (mRotation == 90) {
            rotated = new byte[data.length];
            VideoUtil.NV21Rotate90CW(data,rotated,height,width);
        }else if(mRotation == 270) {
            rotated = new byte[data.length];
            VideoUtil.NV21Rotate90CCW(data,rotated,height,width);
        } else if (mRotation == 180) {
            rotated = new byte[data.length];
            VideoUtil.NV21Rotate180CW(data, rotated, width, height);
        } else {
            rotated = data;
        }
        if(mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
            VideoUtil.NV21toI420Planar(rotated,temp,width,height);
        }else if(mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
            VideoUtil.NV21toYUV420SemiPlanar(rotated,temp,width,height);
        }
        videoSourceData.add(new VideoSourceData(temp, timeStep));
    }

    private ByteBuffer getInputBuffer(int index){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mEnc.getInputBuffer(index);
        }else{
            return mEnc.getInputBuffers()[index];
        }
    }

    private ByteBuffer getOutputBuffer(int index){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mEnc.getOutputBuffer(index);
        }else{
            return mEnc.getOutputBuffers()[index];
        }
    }

    byte[] colors;
    long lastsec = 0;
    int framecount = 0;

    //定时调用，如果没有新数据，就用上一个数据
    private void readOutputData(byte[] data,long timeStep) throws IOException {
        int index=mEnc.dequeueInputBuffer(-1);
        if(index>=0){
            ByteBuffer buffer=getInputBuffer(index);
            buffer.clear();
            buffer.put(data);
            mEnc.queueInputBuffer(index,0,data.length,System.nanoTime()/1000,0);
        }
        MediaCodec.BufferInfo mInfo=new MediaCodec.BufferInfo();
        int outIndex=mEnc.dequeueOutputBuffer(mInfo,0);

        while (outIndex >= 0) {
            ByteBuffer outBuf = getOutputBuffer(outIndex);
            byte[] temp = new byte[mInfo.size];
            outBuf.get(temp);
            if (mInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                Log.e(TAG, "start frame");
                mHeadInfo = new byte[temp.length];
                mHeadInfo = temp;
            } else if (mInfo.flags % 8 == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                byte[] keyframe = new byte[temp.length + mHeadInfo.length];
                System.arraycopy(mHeadInfo, 0, keyframe, 0, mHeadInfo.length);
                System.arraycopy(temp, 0, keyframe, mHeadInfo.length, temp.length);

                fos.write(keyframe, 0, keyframe.length);
            } else if (mInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                Log.e(TAG, "end frame");
            } else {

                fos.write(temp, 0, temp.length);
            }
            mEnc.releaseOutputBuffer(outIndex, false);
            outIndex = mEnc.dequeueOutputBuffer(mInfo, 0);
            Log.e(TAG, "outIndex-->" + outIndex);
        }

    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    private static int selectColorFormat(MediaCodecInfo codecInfo,
                                         String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo
                .getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        Log.e(TAG,
                "couldn't find a good color format for " + codecInfo.getName()
                        + " / " + mimeType);
        return 0; // not reached
    }

    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            // these are the formats we know how to handle for this test
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
//            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
//            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
//            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }





    @Override
    public void run() {
        while (mStartFlag){

            try {
                VideoSourceData data = videoSourceData.poll(50, TimeUnit.MILLISECONDS);
                if (null==data) {
                    continue;
                }
                try {
                    readOutputData(data.data,data.timeStep);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }


//            long lt= System.currentTimeMillis()-time;
//            if(fpsTime>lt){
//                try {
//                    Thread.sleep(fpsTime-lt);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
        }
    }

}
