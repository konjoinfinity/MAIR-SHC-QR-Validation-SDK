package com.robotics.infrareddemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.widget.FrameLayout;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.robotics.mairobotics.InfraredInterface;
import com.robotics.mairobotics.IrSurface;

import java.util.Arrays;


public class InfraredTemManager implements InfraredInterface.ImageCallBackInterface{

    private static InfraredTemManager singleton;
    private static final String TAG = "InfraredTemManager";
    private static InfraredInterface mInterface;
    private static short[] mY16Frame;
    private static final int SRC_WIDTH = 90;
    private static final int SRC_HEIGHT = 120;
    protected IrSurface mIrSurface;
    private int mLastOrientation = 0;
    private HandlerThread handlerThread;
    private Handler handler;
    private static float maxFaceTemp = 0f;
    private float density;
    private long mRefreshTime = 0L;
    private static boolean isInfraredImage = false;
    private int mRefreshNum = 0;
    private static final long MAX_REFRESH_TIME = 25000L;
    private static final int MAX_REFRESH_NUM = 3;
    private static final long REFRESH_INTERVAL = 5000L;
    private static final int HEIGHT_IR_VIEW_DEFAULT = 360;

    private Runnable healthCheckRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "healthCheckRunnable");
            if (System.currentTimeMillis() - mRefreshTime > MAX_REFRESH_TIME) {
                Log.e(TAG, "Restart the temperature measuring device");
                mRefreshTime = System.currentTimeMillis();
                mRefreshNum++;
                if (mRefreshNum > MAX_REFRESH_NUM) {
                    Log.e(TAG, "The temperature measuring device exception，restart app");
                    ToastUtils.showShort("The temperature function is abnormal and the application is restarting");
                    mRefreshNum = 0;
                    ThreadUtils.runOnUiThreadDelayed(new Runnable() {
                        @Override
                        public void run() {
                            AppUtils.relaunchApp(true);
                        }
                    }, 1500);
                }
                onPause();
                onResume();
            }else {
                try {
                    handler.postDelayed(this, REFRESH_INTERVAL);
                }catch (Exception e){
                    Log.e(TAG,"");
                }

            }
        }
    };

    public static InfraredTemManager getInstance() {
        if (singleton == null) {
            synchronized (InfraredTemManager.class) {
                if (singleton == null) {
                    singleton = new InfraredTemManager();
                }
            }
        }
        return singleton;
    }

    public void initInterface(Activity context, FrameLayout mIrSurfaceViewLayout) {

        Log.d(TAG, "init Infrared Interface");
        handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        //Show infrared image preview
        mIrSurface = new IrSurface(context);
        FrameLayout.LayoutParams ifrSurfaceViewLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        mIrSurface.setLayoutParams(ifrSurfaceViewLayoutParams);

        mIrSurface.setMatrix(getScale(context), 0, 0);
        mIrSurfaceViewLayout.addView(mIrSurface.getSurfaceView());


        if (mInterface == null) {
            mInterface = new InfraredInterface();
        }

        mInterface.infraredCoreInit(context, 2);


        //The resolution of the original infrared video is 90*120
        mY16Frame = new short[SRC_WIDTH * SRC_HEIGHT];

        density = context.getResources().getDisplayMetrics().density;
        Log.d(TAG, "density: " + density);
    }


    private float getScale(Context context) {
        int height = context.getResources().getDimensionPixelSize(R.dimen.height_ir_layout);
        float scale = (float) height / HEIGHT_IR_VIEW_DEFAULT;
        Log.d(TAG, "getScale: " + scale);
        return scale;
    }

    public void onResume() {
        Log.d(TAG, "onResume");
        if (mInterface == null) {
            Log.e(TAG, "mGuideInterface uninitialized！");
            return;
        }
        mInterface.registUsbPermissions();
        mInterface.startGetImage(this);
        mInterface.changePalette(2);
        mInterface.setDistance(0.5f);   // 0.5- 1.2
        mRefreshTime = System.currentTimeMillis();
        if (handler != null) {
            handler.removeCallbacks(healthCheckRunnable);
            handler.post(healthCheckRunnable);
        }
    }

    public void onPause() {
        Log.d(TAG, "onPause");
        if (mInterface == null) {
            Log.e(TAG, "mGuideInterface uninitialized！");
            return;
        }
        isInfraredImage = false;
        try {
            //Stop the parsing thread when exiting
            mInterface.stopGetImage();
            mInterface.unRegistUsbPermissions();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }



    /*The resolution of the original infrared video is 90*120, and the returned Bitmap is bilinear interpolation at the bottom layer.
    Amplified by 3 times, so the resolution of bitmap is 270*360 to ensure the clarity of display on mobile phones*/
    @Override
    public void onOneFrameBitmap(Bitmap bitmap, short[] y16Frame) {
        if (mInterface == null) {
            Log.e(TAG, "mGuideInterface uninitialized！");
            return;
        }
        mIrSurface.doDraw(bitmap, mInterface.getShutterStatus());

        mY16Frame = y16Frame;
        mRefreshTime = System.currentTimeMillis();
        isInfraredImage = true;
    }


    /**
     * Get the highest temperature in the face area
     *
     * @param faceRect face Rect
     * @return highest temperature
     */
    public static float getBodyTemByRect(Rect faceRect) {
        if (mInterface == null || !isInfraredImage) {
            Log.e(TAG, "mGuideInterface uninitialized！");
            return 0f;
        }

        /*
        *  width scale   10.67   -   1280  / 120
        *  height scale  8       -   720 / 90
        * */
        Rect originalRect = new Rect((int) (faceRect.left / 8), (int) (faceRect.top / 10.67),
                (int) (faceRect.right / 8), (int) (faceRect.bottom / 10.67));

        //Mapping width and height
        int width = originalRect.right - originalRect.left;
        int height = (originalRect.bottom - originalRect.top) / 2;
        Log.d(TAG, "Map w&h:" + width + "," + height);
        int left = originalRect.left;
        int top = originalRect.top;
        Log.d(TAG, "Map left-top:" + left + "," + top);

        //Map face position array
        short[] faceFrame = new short[width * height];
        int num = 0;
        int index;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                index = left + i + SRC_WIDTH * (top + j);
                if (index > mY16Frame.length - 1) {
                    index = mY16Frame.length - 1;
                }
                faceFrame[num] = mY16Frame[index];
                num++;
            }
        }

        Log.d(TAG, "Map matrix length：" + faceFrame.length);
        Arrays.sort(faceFrame);
        short max = faceFrame[faceFrame.length - 1];
        Log.d(TAG, "y16 max value：" + max);
        String maxY16Tem = mInterface.measureTemByY16(max);
        Log.d(TAG, "y16 max temperature: " + maxY16Tem);
        String maxTemp = mInterface.getHumanTemp(Float.valueOf(maxY16Tem));
        Log.d(TAG, "Map temperature: " + maxTemp);
        if (!TextUtils.isEmpty(maxTemp)) {
            maxFaceTemp = Float.parseFloat(maxTemp);
        }
        return maxFaceTemp;
    }

    public void release() {
        Log.d(TAG, "release");
        if (mInterface != null) {
            mInterface.infraredCoreDestory();
            mInterface = null;
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
        if (handlerThread != null) {
            handlerThread.quit();
        }
    }
}
