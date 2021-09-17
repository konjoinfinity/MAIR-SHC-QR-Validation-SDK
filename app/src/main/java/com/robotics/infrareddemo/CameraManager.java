package com.robotics.infrareddemo;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.blankj.utilcode.util.ToastUtils;
import com.robotics.infrareddemo.utils.CFaceDetector;
import com.robotics.infrareddemo.view.CameraTextureView;
import com.robotics.infrareddemo.view.FaceOverlayView;

public class CameraManager implements CameraTextureView.CameraOpenCallBack {

    private static final String TAG = "CameraManager";

    private static final int STATE_INIT_Failed = -1;
    private static final int STATE_NOINIT = 0;
    private static final int STATE_INITING = 1;
    private static final int STATE_INITED = 2;
    private static final int STATE_PREVIEW = 3;
    private static final int STATE_PAUSE = 4;
    private static final int STATE_DESTROY = -2;
    private static int cameraState = STATE_NOINIT;
    private static CameraManager mCameraManager;

    static ImageDataBean bufferImageDataBean = new ImageDataBean();

    private Handler mainHandler;

    private int prevSettingWidth;
    private int prevSettingHeight;

    private static FaceOverlayView mFaceView;
    private Handler faceDetectHandler;
    private static int rotateAngle = 270;
    private CameraTextureView textureView;

    private static final int INTERVAL_CHECK_FACE = 200;

    private MainActivity mainActivity = null;

    private static CFaceDetector cFaceDetector = new CFaceDetector();
    public static long detectSuccessTime = 0L;

    private CameraManager() {
        Log.d(TAG, "CameraManager: create");

    }


    private long cameraPerviewTime = 0L;
    private long checkFaceTime = 0L;

    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {

            try {
                textureView.addCallbackBuffer();
                cameraPerviewTime = System.currentTimeMillis();
                Camera.Size size = camera.getParameters().getPreviewSize();

                bufferImageDataBean.setdata(data, size.width, size.height, cameraPerviewTime);

                if (System.currentTimeMillis() - checkFaceTime >= INTERVAL_CHECK_FACE) { //人脸框150ms追踪一次
                    if (!checkFaceRunnable.isRun) {
                        faceDetectHandler.post(checkFaceRunnable);
                    }
                    checkFaceTime = System.currentTimeMillis();
                }

            }catch (Exception e){
                Log.e(TAG,"handle preview frame error:"+e.toString());
            }

        }
    };

    public void init(Context context, MainActivity mainActivity) {

        this.mainActivity = mainActivity;

        HandlerThread thread = new HandlerThread("ImageHandler");
        thread.start();

        Log.d(TAG, "init: CameraManager");
        thread = new HandlerThread("CameraManager");
        thread.start();

        faceDetectHandler = new Handler(thread.getLooper());

    }

    public static CameraManager getInstance() {
        if (mCameraManager == null) {
            synchronized (CameraManager.class) {
                if (null == mCameraManager) {
                    mCameraManager = new CameraManager();
                }
            }
        }
        return mCameraManager;
    }


    public void destroy() {
        Log.d(TAG, "destroy: ");
        if (mCameraManager != null) {
            stop1();
            textureView = null;
            mFaceView = null;
            mCameraManager = null;
        }
    }

    public void setTextureView(CameraTextureView textureView, FaceOverlayView faceView) {
        Log.d(TAG, "setTextureView: " + cameraState);
        if (cameraState == STATE_INITING) {
            Log.d(TAG, "setTextureView: init ing");
            return;
        }

        // when activity pause - > resume
        if (cameraState >= STATE_INITED) {
            startPreview();
            return;
        }

        cameraState = STATE_INITING;
        this.textureView = textureView;
        this.mFaceView = faceView;
        prevSettingWidth = textureView.getWidth();
        prevSettingHeight = textureView.getHeight();
        faceView.setPreviewHeight(prevSettingHeight);
        faceView.setPreviewWidth(prevSettingWidth);
        faceView.setCameraWidth(720);
        faceView.setCameraHeight(1280);
        Log.d(TAG, "setTextureView: ThreadId = " + Thread.currentThread().getId());
        mainHandler = new Handler(Looper.getMainLooper());
        textureView.setPreviewCallback(previewCallback);
        textureView.setCameraOpenCallBack(this);

        cameraState = STATE_INITED;

    }

    @Override
    public void openCameraSuccess() {
        Log.i(TAG, "openCameraSuccess: ");
    }

    @Override
    public void openCameraFail() {
        ToastUtils.showShort("Camera open failed, reset.");
        resetCamera();
    }

    public void startPreview() {

        if (cameraState >= STATE_NOINIT && cameraState < STATE_INITED) {
            Log.e(TAG, "startPreview: init ing " + cameraState);
            return;
        } else if (cameraState == STATE_INIT_Failed) {
            return;
        }
        try {
            if (cameraState != STATE_PREVIEW) {

                textureView.releaseCamera();
                textureView.startPreview();
                cameraPerviewTime = System.currentTimeMillis() + 2000;
                cameraState = STATE_PREVIEW;
            } else {
                Log.d(TAG, "startPreview: " + cameraState);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    public void stopPreview() {
        if (cameraState == STATE_PREVIEW) {
            cameraState = STATE_PAUSE;
            try {
                textureView.releaseCamera();
            } catch (Exception e) {
                Log.d(TAG, "stopPerview: " + e.getMessage());
            }
            Log.d(TAG, "stopPerview: ");
        }
    }

    public void stop1() {
        Log.d(TAG, "stop: ");
        if (textureView != null) {
            textureView.releaseCamera();
        }

        cameraState = STATE_DESTROY;
    }

    public void release1() {
        Log.d(TAG, "release: ");
        if (textureView != null) {
            textureView.releaseCamera();
        }

        cameraState = STATE_INITED;
    }

    boolean isReseting = false;

    private void resetCamera() {
        release1();
        if (!isReseting) {
            isReseting = true;
            Log.d(TAG, "setTextureView: reset camera");
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    Log.d(TAG, "run: ThreadId = " + Thread.currentThread().getId());
                    isReseting = false;
                    mainHandler.post(() -> {
                        startPreview();
                    });
                }
            }.start();

        }
    }

    CheckFaceRunnable checkFaceRunnable = new CheckFaceRunnable();

    //check face from image
    public class CheckFaceRunnable implements Runnable {

        boolean isRun = false;

        @Override
        public void run() {
            isRun = true;

            final byte[] data;
            int width;
            int height;

            data = bufferImageDataBean.getData();
            width = bufferImageDataBean.getWidth();
            height = bufferImageDataBean.getHeight();

            if (cFaceDetector != null && data != null && data.length > 0) {

                Rect faceRect = cFaceDetector.faceDetect(data, width, height, rotateAngle);

                if (faceRect != null) {

                    detectSuccessTime = System.currentTimeMillis();
                    Rect rect = faceRect;

                    if (rect != null) {

                        Log.d(TAG, "Face has been detected");

                        int temp = width;

                        if (rotateAngle != 0) {
                            temp = height;
                        }
                        rect.left = temp - rect.left;
                        rect.right = temp - rect.right;
                        temp = rect.left;
                        rect.left = rect.right;
                        rect.right = temp;

                        mFaceView.setFaceRect(rect.left, rect.top, rect.right, rect.bottom); //show face rect view

                        mainActivity.showFaceTemperature(rect);

                    }

                } else {

                    mFaceView.setFaceRect(0, 0, 0, 0); //no face , hide face rect view

                }
            } else {
                Log.d(TAG, "run: data exception");
            }
            isRun = false;
        }
    }

}
