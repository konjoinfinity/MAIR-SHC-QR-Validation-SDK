package com.robotics.infrareddemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.robotics.infrareddemo.utils.DangerousPermissions;
import com.robotics.infrareddemo.view.CameraTextureView;
import com.robotics.infrareddemo.view.FaceOverlayView;
import com.robotics.mairobotics.InfraredInterface;

import java.text.DecimalFormat;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = "MainActivity";

    public static final float DEFAULT_TEMPERATURE_THRESHOLD_C = 37.3f; //Default alarm threshold of body temperature (Celsius)
    public static final float DEFAULT_TEMPERATURE_THRESHOLD_F = 99.2f; //Default alarm threshold of body temperature (Fahrenheit)
    public static final float DEFAULT_ELIMINATE_TEMPERATURE_C = 34.0f; //The lowest value of the default invalid temperature measurement (Celsius)
    public static final float DEFAULT_ELIMINATE_TEMPERATURE_F = 93.2f; //The lowest value of the default invalid temperature measurement (Fahrenheit)
    public static final float TEMPERATURE_NO_DATA = 0f;

    private ConstraintLayout mConstraintLayout;
    private ImageView mSettingMenu;
    private Drawable mTempNormalDrawable;
    private Drawable mTempHighDrawable;
    private CameraTextureView mCameraView;
    private FaceOverlayView mFaceOverlayView;
    protected FrameLayout mIrSurfaceViewLayout;
    private ConstraintLayout clDetectResult;
    private TextView tvTemperature;
    private TextView tvMask;
    private ImageView ivAvatar;
    private TextView tvName;

    private Handler mBackgroundHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        initViews();

        initInfraredManager();

        CameraManager.getInstance().init(MainActivity.this.getApplicationContext(), MainActivity.this);

        checkPermissions();
    }

    protected void setFullScreen() {

        ScreenUtils.setFullScreen(this);
        BarUtils.setNavBarVisibility(this, false);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "on start");
        getHandler().postDelayed(startPreviewRunnable, 1500);

        InfraredTemManager.getInstance().onResume();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setFullScreen();
    }

    @Override
    protected void onPause() {
        super.onPause();

        CameraManager.getInstance().stopPreview();

        InfraredTemManager.getInstance().onPause();
    }

    private Runnable  startPreviewRunnable = new Runnable() {
        @Override
        public void run() {
            CameraManager.getInstance().setTextureView(mCameraView, mFaceOverlayView);
            CameraManager.getInstance().startPreview();
        }
    };

    private void initViews(){
        mConstraintLayout = findViewById(R.id.cl_main_content);
        mSettingMenu = findViewById(R.id.iv_setting);
        mCameraView = findViewById(R.id.textureView);

        mFaceOverlayView = findViewById(R.id.faceView);

        clDetectResult = findViewById(R.id.cl_detect_result);
        tvTemperature = findViewById(R.id.tv_temperature);
        tvMask = findViewById(R.id.tv_mask);
        ivAvatar = findViewById(R.id.iv_avatar);
        tvName = findViewById(R.id.tv_name);

        mTempNormalDrawable = getDrawable(R.drawable.main_temperature_normal);
        mTempHighDrawable = getDrawable(R.drawable.main_temperature_high);
        mTempNormalDrawable.setBounds(0, 0, 48, 88);
        mTempHighDrawable.setBounds(0, 0, 48, 88);

        Log.d(TAG, "End of init view");
    }

    private void initInfraredManager(){
        mIrSurfaceViewLayout = findViewById(R.id.fl_ir_layout);
        mIrSurfaceViewLayout.setVisibility(View.VISIBLE);
        InfraredTemManager.getInstance().initInterface(this, mIrSurfaceViewLayout);
    }

    public void showFaceTemperature(Rect rect){

        Log.d(TAG, "Map original rect w&h：" + rect.width() + "," + rect.height());
        Log.d(TAG, "Map original rect-left-top：" + rect.left + "," + rect.top);
        float temperature = InfraredTemManager.getBodyTemByRect(rect);

        if (temperature <= TEMPERATURE_NO_DATA) {
            Log.e(TAG, "Please wait for the temperature measurement device to initialize..." + temperature);
            ToastUtils.showShort("Please wait for the temperature measurement device to initialize...");
            return;
        }
        // Invalid temperature
        if (temperature <= DEFAULT_ELIMINATE_TEMPERATURE_C) {
            Log.e(TAG, "Please center your face..." + temperature);
            ToastUtils.showShort("Please center your face and show your forehead");
            return;
        }

        Log.d(TAG, "Temperature : "+ temperature);

        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (temperature < DEFAULT_ELIMINATE_TEMPERATURE_C) {
                    Log.d(TAG, "Please measure the temperature again");
                    return;
                }
                clDetectResult.setVisibility(View.VISIBLE);
                DecimalFormat df = new DecimalFormat("0.0");
                float convertTemp, thresholdTemp;

                convertTemp = parseC2F(temperature);
                thresholdTemp = DEFAULT_TEMPERATURE_THRESHOLD_F;
                tvTemperature.setText(String.format("%s℉",df.format(convertTemp)));

                tvName.setText("");
                if (convertTemp <= thresholdTemp) {
                    tvTemperature.setCompoundDrawables(mTempNormalDrawable, null, null, null);
                } else {
                    tvTemperature.setCompoundDrawables(mTempHighDrawable, null, null, null);
                }

                getHandler().removeCallbacks(hideTemperatureRunnable);
                getHandler().postDelayed(hideTemperatureRunnable, 3000);
            }
        });

    }

    public  float parseC2F(float c) {
        return c * 1.8f + 32;
    }

    private Runnable hideTemperatureRunnable = new Runnable() {
        @Override
        public void run() {
            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvTemperature.setText("");
                    clDetectResult.setVisibility(View.GONE);
                }
            });
        }
    };

    private Handler getHandler(){

        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }

        return mBackgroundHandler;
    }


    private void checkPermissions(){
        PackageInfo pack  = null;
        try {
            pack = getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        String[] pers = pack.requestedPermissions;
        ArrayList<String> permissionList = new ArrayList<>();
        StringBuilder sbPers = new StringBuilder();
        for (String per : pers) {
            if(DangerousPermissions.needrequest.contains(per)) {
                if (!EasyPermissions.hasPermissions(getApplicationContext(), per)) {
                    permissionList.add(per);
                    sbPers.append("\n");
                    sbPers.append(per);
                }
            }
        }
        pers =  permissionList.toArray(new String[permissionList.size()]);
        if(pers.length > 0){
            EasyPermissions.requestPermissions(this, getResources().getString(R.string.open_permission) +sbPers.toString(), 100, pers);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


    /**     Easy permission Callback   **/
    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        finish();
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "camera permission granted");
        AppUtils.relaunchApp(true);
    }

}