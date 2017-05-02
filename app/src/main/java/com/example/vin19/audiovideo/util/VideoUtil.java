package com.example.vin19.audiovideo.util;

/**
 * Created by vin19 on 2017/5/1.
 * 视屏处理类
 */

public class VideoUtil {

    /**
     * 顺时针旋转90度
     * NV21格式 YYYYYYYY VUVU
     * @param src
     * @param dest
     * @param originWidth  旋转前的宽
     * @param originHeight 旋转前的高
     */
    public static void NV21Rotate90CW(byte[] src, byte[] dest, int originWidth, int originHeight) {
        int wh = originWidth * originHeight;

        //旋转Y分量
        int k = 0;
        for(int i=0; i<originWidth; ++i) {
            for(int j=originHeight-1; j>=0; --j) {
                dest[k] = src[i + j * originWidth];
                k++;
            }
        }

        //旋转VU分量
        for(int i=0; i<originWidth; i+=2) {
            for(int j=originHeight/2-1; j>=0; --j) {
                dest[k] = src[wh + i + j * originWidth];
                dest[k + 1] = src[wh + i + 1 + j * originWidth];
                k +=2;
            }
        }
    }

    /**
     * 逆时针旋转90度
     * 当初始角度为270度时，需执行此操作
     * NV21格式 YYYYYYYY VUVU
     * @param src
     * @param dest
     * @param originWidth
     * @param originHeight
     */
    public static void NV21Rotate90CCW(byte[] src, byte[] dest, int originWidth, int originHeight) {
        int wh = originWidth * originHeight;
        //旋转Y分量
        int k = 0;
        for (int i=originWidth-1; i>=0; --i) {
            for(int j=0; j<originHeight; ++j) {
                dest[k] = src[i + j * originWidth];
                k++;
            }
        }
        //旋转UV分量
        for(int i=originWidth-2; i>=0; i-=2) {
            for(int j=0; j<originHeight/2; ++j) {
                dest[k] = src[wh + i + j * originWidth];
                dest[k + 1] = src[wh + i + 1 + j * originWidth];
                k += 2;
            }
        }
    }

    /**
     * 顺时针旋转180度
     * @param src
     * @param dest
     * @param width
     * @param height
     */
    public static void NV21Rotate180CW(byte[] src, byte[] dest, int width, int height) {
        int wh = width * height;
        int k = 0;
        //旋转Y分量
        for(int i=height-1; i>=0; --i) {
            for(int j=width-1; j>=0; --j) {
                dest[k] = src[j + i * width];
                k++;
            }
        }

        //旋转UV分量
        for(int i=height/2-1; i>=0; --i) {
            for(int j=width-2; j>=0; j-=2) {
                dest[k] = src[wh + j + i * width];
                dest[k + 1] = src[wh + j + 1 + i * width];
                k += 2;
            }
        }
    }

    /**
     * NV21 is a 4:2:0 YCbCr, For 1 NV21 pixel: YYYYYYYY VUVU I420YUVSemiPlanar
     * is a 4:2:0 YUV, For a single I420 pixel: YYYYYYYY UU VV
     */
    public static void NV21toI420Planar(byte[] nv21bytes, byte[] i420bytes,
                                  int width, int height) {
        System.arraycopy(nv21bytes, 0, i420bytes, 0, width * height);
        /**
         * VUVU -- UU VV 转化
         * startU 目标数组u分量开始下标
         * startV 目标数组v分量开始下标
         */
        int startU = width * height;
        int startV = width * height + width * height / 4;
        for(int i=width * height; i<(nv21bytes.length-1); i += 2) {
            i420bytes[startU] = nv21bytes[i + 1];
            i420bytes[startV] = nv21bytes[i];
            startU++;
            startV++;
        }
    }

    public static void  NV21toYUV420SemiPlanar(byte[] nv21bytes, byte[] i420bytes,
                                        int width, int height) {
        System.arraycopy(nv21bytes, 0, i420bytes, 0, width * height);

        int start = width * height;
        for(int i=start; i<(nv21bytes.length-1); i+=2) {
            i420bytes[i + 1] = nv21bytes[i];
            i420bytes[i] = nv21bytes[i + 1];
        }
    }
}
