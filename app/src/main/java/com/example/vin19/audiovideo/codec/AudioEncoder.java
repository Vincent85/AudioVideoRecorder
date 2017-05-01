package com.example.vin19.audiovideo.codec;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by vin19 on 2017/5/1.
 * 音频编码类
 */

public class AudioEncoder implements Runnable {

    public static final String TAG = "AudioEncoder";
    public static final boolean DEBUG = true;

    private static final int DEQUE_TIME_OUT = 2000;
    private MediaCodec mEncoder;
    private AudioRecord mAudioRecorder;

    private String mMime = "audio/mp4a-latm";
    private int mRate = 25600;
    private int mSampleRate = 44100;
    private int mChannelCount = 2;
    private int mChannelConfig = AudioFormat.CHANNEL_IN_STEREO;
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;

    private byte[] mBuffers;
    private int mBufferSize;

    private FileOutputStream mFos;
    private boolean isRecording;

    private Thread mThread;
    private String mSavedPath;

    public AudioEncoder() {

    }

    public void setMime(String mime) {
        this.mMime = mime;
    }

    public void setSampleRate(int sampleRate) {
        this.mSampleRate = sampleRate;
    }

    public void setSavedPath(String savedPath) {
        this.mSavedPath = savedPath;
    }

    public void prepare() {
        try {
            mFos = new FileOutputStream(mSavedPath);
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

        //音频编码格式相关参数设定
        MediaFormat format = MediaFormat.createAudioFormat(mMime, mSampleRate, mChannelCount);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mRate);

        try {
            mEncoder = MediaCodec.createEncoderByType(mMime);
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        //音频录制相关参数设定
        mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, mAudioFormat) * 2;
        mBuffers = new byte[mBufferSize];
        mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, mSampleRate,
                mChannelConfig, mAudioFormat, mBufferSize);
    }

    public void start() {
        mEncoder.start();
        mAudioRecorder.startRecording();
        if (null != mThread && mThread.isAlive()) {
            isRecording = false;
            try {
                mThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
        isRecording = true;
        mThread = new Thread(this);
        mThread.start();
    }

    public ByteBuffer getInputBuffer(int index) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mEncoder.getInputBuffer(index);
        }else {
            return mEncoder.getInputBuffers()[index];
        }
    }

    public ByteBuffer getOutputBuffer(int index) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mEncoder.getOutputBuffer(index);
        }else {
            return mEncoder.getOutputBuffers()[index];
        }
    }

    private void readOutputData() throws IOException {
        int index = mEncoder.dequeueInputBuffer(-1);
        if (index >= 0) {
            if (DEBUG) {
                Log.d(TAG, "inputbuffer index = " + index);
            }
            final ByteBuffer buffer = getInputBuffer(index);
            buffer.clear();
            int length = mAudioRecorder.read(buffer, mBufferSize);
            if (length > 0) {
                mEncoder.queueInputBuffer(index, 0, length, System.nanoTime() / 1000, 0);
                if (DEBUG) {
                    Log.d(TAG, "queueinputBuffer data length = " + length);
                }
            }else {
                if (DEBUG) {
                    Log.e(TAG, "read data length = " + length);
                }
            }
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputIndex;
        do {
            outputIndex = mEncoder.dequeueOutputBuffer(bufferInfo, DEQUE_TIME_OUT);
            if (outputIndex >= 0) {
                ByteBuffer buffer = getOutputBuffer(outputIndex);
                buffer.position(bufferInfo.offset);
                byte[] temp = new byte[bufferInfo.size + 7];
                buffer.get(temp, 7, bufferInfo.size);
                addADTStoPacket(temp, temp.length);
                mFos.write(temp);
                mEncoder.releaseOutputBuffer(outputIndex, false);
                if (DEBUG) {
                    Log.d(TAG, "mEncoder releaseOutputBuffer");
                }
            } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {

            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                //todo init mediaMuxer
                if (DEBUG) {
                    Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
                }
            }
        } while (outputIndex >= 0);
    }

    /**
     * 给编码出的aac裸流添加adts头字段
     * @param packet 要空出前7个字节，否则会搞乱数据
     * @param packetLen
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;  //AAC LC
        int freqIdx = 4;  //44.1KHz
        int chanCfg = 2;  //CPE
        packet[0] = (byte)0xFF;
        packet[1] = (byte)0xF9;
        packet[2] = (byte)(((profile-1)<<6) + (freqIdx<<2) +(chanCfg>>2));
        packet[3] = (byte)(((chanCfg&3)<<6) + (packetLen>>11));
        packet[4] = (byte)((packetLen&0x7FF) >> 3);
        packet[5] = (byte)(((packetLen&7)<<5) + 0x1F);
        packet[6] = (byte)0xFC;
    }

    public void stop() {
        isRecording= false;
        try {
            mThread.join();
            mAudioRecorder.stop();
            mEncoder.stop();
            mEncoder.release();
            mFos.flush();
            mFos.close();
        } catch (InterruptedException e) {
            Log.e(TAG, e.getLocalizedMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (isRecording) {
            try {
                readOutputData();
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    }
}
