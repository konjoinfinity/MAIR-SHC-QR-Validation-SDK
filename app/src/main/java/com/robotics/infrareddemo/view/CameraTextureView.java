package com.robotics.infrareddemo.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;
import java.util.List;

public class CameraTextureView extends TextureView {

    private final static String TAG = "CameraTextureView";
    private final static boolean DEBUG = true;

    private Camera mCamera;
    private Matrix matrix = new Matrix();
    private Camera.PreviewCallback mPreviewCallback;

    private int mCameraFacing;

    public int mPreviewWidth;
    public int mPreviewHeight;
    public int mDegrees;
    public byte[] mBuffer;
    private float scaleX;
    private float scaleY;
    private SurfaceTexture mSurface;

    public CameraTextureView(Context context) {
        super(context);
        init(context, null);
    }

    public CameraTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        ((Activity) context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        ((Activity) context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        setKeepScreenOn(true);

        mCameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;

        this.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    private SurfaceTextureListener mSurfaceTextureListener = new SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture arg0) {
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture arg0, int width,
                                                int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture arg0) {
            releaseCamera();
            return false;
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.e(TAG, "onSurfaceTextureAvailable: " + width + "  " + height);
            mSurface = surface;
        }
    };

    public void startPreview() {
        Log.e(TAG, "start preview: ");
        try {
            mCameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
            openCamera(mCameraFacing);
            initParameters();
            mCamera.setPreviewTexture(mSurface);
            updateCamera();
            if (mCameraOpenCallBack != null) {
                mCameraOpenCallBack.openCameraSuccess();
            }
            Log.e(TAG, " openCameraSuccess");
        } catch (Exception e) {
            e.printStackTrace();
            if (mCameraOpenCallBack != null) {
                mCameraOpenCallBack.openCameraFail();
            }
            Log.e(TAG, "open camera failed:" + e.getMessage());
        }

        if (getWidth() > getHeight()) {
            scaleX = getWidth() / (float) mPreviewWidth;
            scaleY = getHeight() / (float) mPreviewHeight;
        } else {
            scaleX = getWidth() / (float) mPreviewHeight;
            scaleY = getHeight() / (float) mPreviewWidth;
        }
        matrix.setScale(scaleX, scaleY);
        matrix.postScale(-1, 1, getWidth() / 2, getHeight() / 2);
    }



    private void openCamera(int mCameraFacing) throws RuntimeException {
        releaseCamera();
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraFacing, info);
        mCamera = Camera.open(mCameraFacing);
        mCamera.setErrorCallback(mErrorCallback);
    }

    private void initParameters() {
        if (mCamera == null) {
            return;
        }
        try {
            Camera.Parameters parameters = mCamera.getParameters();

            List<String> supportedFlashModes = parameters.getSupportedFlashModes();
            if (supportedFlashModes != null && supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            List<String> supportedFocusModes = parameters.getSupportedFocusModes();
            if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            parameters.setPreviewFormat(ImageFormat.NV21);
            parameters.setPictureFormat(ImageFormat.JPEG);
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void updateCamera() {
        if (mCamera == null) {
            return;
        }
        mCamera.stopPreview();
        setCameraDisplayOrientation((Activity) getContext(), mCamera);
        initPreviewSize();
        initPreviewBuffer();
        mCamera.startPreview();
        Log.e(TAG, "updateCamera: ");
    }


    private void initPreviewSize() {
        if (mCamera == null) {
            return;
        }
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            mPreviewWidth = 1280;
            mPreviewHeight = 720;
            parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
            if (DEBUG) {
                Log.d(TAG, "initPreviewSize() mPreviewWidth: " + mPreviewWidth + ", mPreviewHeight: " + mPreviewHeight);
            }
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            Log.e(TAG, "initPreviewSize: " + e.getMessage());
        }
    }

    private void initPreviewBuffer() {
        if (mCamera == null) {
            return;
        }
        mBuffer = new byte[mPreviewWidth * mPreviewHeight * 3 / 2];
        if (DEBUG) {
            Log.d(TAG, "initPreviewBuffer() mBuffer.length: " + mBuffer.length);
        }
        mCamera.addCallbackBuffer(mBuffer);
        mCamera.setPreviewCallbackWithBuffer(mPreviewCallback);
    }


    private void setCameraDisplayOrientation(Activity activity, Camera camera) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0: // portrait
                mDegrees = 90;
                break;
            case Surface.ROTATION_90: // landscape
                mDegrees = 0;
                break;
            case Surface.ROTATION_180: // portrait-reverse
                mDegrees = 270;
                break;
            case Surface.ROTATION_270: // landscape-reverse
                mDegrees = 180;
                break;
            default:
                mDegrees = 90;
                break;
        }

        camera.setDisplayOrientation(mDegrees);
        Log.d(TAG, "current camera display orientation is: " + mDegrees);
    }



    public void releaseCamera() {
        if (null != mCamera) {
            if (DEBUG) {
                Log.d(TAG, "releaseCamera()");
            }
            mCameraFacing = 0;
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
    }


    public void addCallbackBuffer() {
        if (mCamera != null) {
            mCamera.addCallbackBuffer(mBuffer);
        }
    }


    @Override
    public Matrix getMatrix() {
        return matrix;
    }

    public boolean isFrontCamera() {
        return mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }


    private CameraOpenCallBack mCameraOpenCallBack;

    public void setCameraOpenCallBack(CameraOpenCallBack cb) {
        mCameraOpenCallBack = cb;
    }

    public interface CameraOpenCallBack {
        void openCameraSuccess();

        void openCameraFail();
    }

    private Camera.ErrorCallback mErrorCallback = new Camera.ErrorCallback() {
        @Override
        public void onError(int i, Camera camera) {
            Log.e(TAG, "onError: " + i);
        }
    };
}
