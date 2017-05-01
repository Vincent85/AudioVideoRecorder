package com.example.vin19.audiovideo.util;

/**
 * Created by vin19 on 2017/5/1.
 * 视屏处理类
 */

public class VideoUtil {

    /**
     * NV21 转为YUV420p
     * NV21     YYYYYYYY VUVU
     * YUV420P  YYYYYYYY VV UU
     * @param origin
     * @param dest
     * @param width
     * @param height
     */
    public static void NV21ToYUV420P(byte[] origin, byte[] dest, int width, int height) {
        System.arraycopy(origin, 0, dest, 0, width * height);

        int offset = width * height;
        for (int i = offset,j=0 ; i < origin.length; i += 2,j++) {
            dest[offset + j] = origin[offset + i];
            dest[offset + offset / 4 + j] = origin[offset + i + 1];
        }
    }
}
