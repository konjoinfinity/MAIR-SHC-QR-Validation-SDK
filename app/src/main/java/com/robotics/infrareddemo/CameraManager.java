package com.robotics.infrareddemo;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.*;
import org.json.*;
import com.android.volley.*;
import java.io.UnsupportedEncodingException;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import androidx.camera.core.ImageProxy;
import android.media.Image;
//import android.view.*;
//import android.hardware.camera2.CameraAccessException;
//import android.app.*;
//import android.util.SparseIntArray;
//import android.hardware.camera2.CameraCharacteristics;
//import android.os.Build;
//import androidx.annotation.RequiresApi;
//import android.content.Context.CAMERA_SERVICE;

import java.util.List;

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

//
    private Context context;

    public CameraManager(Context context) {
            this.context = context;
        }

    private void shcVerify(){
        try {
            RequestQueue requestQueue = Volley.newRequestQueue(context);
            String URL = "https://vaccineqr.herokuapp.com/";
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("data",
                    "shc:/567629595326546034602925407728043360287028656767542228092862372537602870286471674522280928647530563921034461336441062477562637675506320644623307566125655306630741274076326559284477294054635922342541254063246057360106453133537170742428375775555956294172405332434269414023332421522126613838002423394124424250072829264324225739724340677662363034624032353744114045035477275054455927333636343312090769253659044536090557550977594135676453727456287122102658683867757238695332116056232066275826570574082533505926105660256536655642630875534043413431407655366771297711263142415611573236413762540424772772297472305839386541523525247361284571213475065930505510585565356038712605247567367252206753774524103633277712033463301168227506296534326507507128316728676752206133386663717460202150442812442745031030370070373225677362593654236854325310236823585770253125377534685924365030292400735861257610030754210666065865122410441241717029103509370072212233727141652922444533230603556254082730346408575828652256007007333741406972565421000724762961367339394007322532297337116072093577432405695874503976307274312873205534252471422227377737587476386276374303057709225560665075306408083967625611623437445275775808436656761240303325387228626140643471600506715326300576243145551044442562290403665236230940650744636455046677090564547352454176290364454138523225641156761255687767503333075945303868594437122826043335083167065359230621123804530811634334094350283444003910220554697511657331562661360537066472587065112904356973582568505438273558736777774060523141570822312373577424012031696538204363303828683336390656544169622221774552455977210955260337266560607629056569343439685766300822424569672330052204684041113022052424631166113273230937302177043258"
                    );
            final String requestBody = jsonBody.toString();

            StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i("VOLLEY", response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("VOLLEY", error.toString());
                }
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() throws AuthFailureError {
                    try {
                        return requestBody == null ? null : requestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                        return null;
                    }
                }

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    String responseString = "";
                    if (response != null) {
                        responseString = String.valueOf(response.statusCode);
                        // can get more details such as response.headers
                    }
                    return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                }
            };

            requestQueue.add(stringRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

//    private void scanBarcodes(InputImage image) {
//        // [START set_detector_options]
//        BarcodeScannerOptions options =
//                new BarcodeScannerOptions.Builder()
//                        .setBarcodeFormats(
//                                Barcode.FORMAT_QR_CODE)
//                        .build();
//        // [END set_detector_options]
//
//        // [START get_detector]
//        BarcodeScanner scanner = BarcodeScanning.getClient();
//        // Or, to specify the formats to recognize:
//        // BarcodeScanner scanner = BarcodeScanning.getClient(options);
//        // [END get_detector]
//
//        // [START run_detector]
//        Task<List<Barcode>> result = scanner.process(image)
//                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
//                    @Override
//                    public void onSuccess(List<Barcode> barcodes) {
//                        // Task completed successfully
//                        // [START_EXCLUDE]
//                        // [START get_barcodes]
//                        for (Barcode barcode: barcodes) {
//                            Rect bounds = barcode.getBoundingBox();
//                            Point[] corners = barcode.getCornerPoints();
//
//                            String rawValue = barcode.getRawValue();
//                            ToastUtils.showShort("QR Code: " + rawValue);
////                            int valueType = barcode.getValueType();
////                            // See API reference for complete list of supported types
////                            switch (valueType) {
////                                case Barcode.TYPE_WIFI:
////                                    String ssid = barcode.getWifi().getSsid();
////                                    String password = barcode.getWifi().getPassword();
////                                    int type = barcode.getWifi().getEncryptionType();
////                                    break;
////                                case Barcode.TYPE_URL:
////                                    String title = barcode.getUrl().getTitle();
////                                    String url = barcode.getUrl().getUrl();
////                                    break;
////                            }
//                        }
//                        // [END get_barcodes]
//                        // [END_EXCLUDE]
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        // Task failed with an exception
//                        // ...
//                    }
//                });
//        // [END run_detector]
//    }
//
//
//    public void analyze(ImageProxy imageProxy) {
//            Image mediaImage = imageProxy.getImage();
//            if (mediaImage != null) {
//                ToastUtils.showShort("Output: " + mediaImage);
//                InputImage image =
//                        InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().hashCode());
//                // Pass image to an ML Kit Vision API
//                // ...
//            }
//        }


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
                    shcVerify();
                }
            } else {
                Log.d(TAG, "run: data exception");
            }
            isRun = false;
        }
    }

}
