package com.robotics.infrareddemo.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;

import java.nio.ByteBuffer;

public class CFaceDetector {

    private static final String TAG = "CFaceDetector";

    private static final int MAX_FACE = 1;

    private int prevSettingWidth = 360;

    private byte[] grayBuff;
    private int bufflen;
    private int[] rgbs;
    private android.media.FaceDetector amDetctor;


    public CFaceDetector(){
        bufflen = 1280 * 720;
        grayBuff = new byte[bufflen];
        rgbs = new int[bufflen];
    }


    public Rect faceDetect(byte[] data, int width, int height, int orientation){

        float aspect = (float) height / (float) width;
        int w = prevSettingWidth;
        int h = (int) (prevSettingWidth * aspect);

        ByteBuffer bbuffer = ByteBuffer.wrap(data);
        bbuffer.get(grayBuff, 0, bufflen);


        gray8toRGB32(grayBuff, width, height, rgbs);

        Bitmap bitmap = Bitmap.createBitmap(rgbs, width, height, Bitmap.Config.RGB_565);

        Bitmap bmp = Bitmap.createScaledBitmap(bitmap, w, h, false);

        bmp = rotate(bmp, 270);
        float xScale = (float) height / (float) h;
        float yScale = (float) width / (float) prevSettingWidth;

        if (null == amDetctor)
            amDetctor = new android.media.FaceDetector(bmp.getWidth(), bmp.getHeight(), MAX_FACE);


        android.media.FaceDetector.Face[] fullResults = new android.media.FaceDetector.Face[MAX_FACE];
        amDetctor.findFaces(bmp, fullResults);

        for (int i = 0; i < MAX_FACE; i++) {
            if (fullResults[i] == null) {

            } else {
                PointF mid = new PointF();
                fullResults[i].getMidPoint(mid);

                mid.x *= xScale;
                mid.y *= yScale;

                float eyesDis = fullResults[i].eyesDistance() * xScale;

                Rect rect = new Rect(
                        (int) (mid.x - eyesDis * 1.20f),
                        (int) (mid.y - eyesDis * 0.55f),
                        (int) (mid.x + eyesDis * 1.20f),
                        (int) (mid.y + eyesDis * 1.85f));


                long et = System.currentTimeMillis();

                return rect;
            }
        }

        return null;
    }

    private void gray8toRGB32(byte[] gray8, int width, int height, int[] rgb_32s) {
        final int endPtr = width * height;
        int ptr = 0;
        while (true) {
            if (ptr == endPtr)
                break;

            final int Y = gray8[ptr] & 0xff;
            rgb_32s[ptr] = 0xff000000 + (Y << 16) + (Y << 8) + Y;
            ptr++;
        }
    }

    //Rotate Bitmap
    public  Bitmap rotate(Bitmap originalBp, float rotation) {
        if (rotation != 0 && originalBp != null) {
            Matrix m = new Matrix();
            m.postRotate(rotation);

            Bitmap b2 = Bitmap.createBitmap(originalBp, 0, 0, originalBp.getWidth(),
                    originalBp.getHeight(), m, true);

            if (originalBp != b2) {
                originalBp.recycle();
                originalBp = b2;
            }

        }
        return originalBp;
    }
}
