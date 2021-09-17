// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.robotics.infrareddemo.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;




/**
 * Created by Nguyen on 5/20/2016.
 */

/**
 * This class is a simple View to display the faces.
 */
public class FaceOverlayView extends View {

    private Paint mPaint;
    private Paint mTextPaint;
    private int previewWidth;
    private int previewHeight;
    private int cameraWidth;
    private int cameraHeight;

    public int getCameraWidth() {
        return cameraWidth;
    }

    public void setCameraWidth(int cameraWidth) {
        Log.e(TAG, "setCameraWidth: "+cameraWidth );
        this.cameraWidth = cameraWidth;
    }

    public int getCameraHeight() {
        return cameraHeight;
    }

    public void setCameraHeight(int cameraHeight) {
        Log.e(TAG, "setCameraHeight: "+cameraHeight );
        this.cameraHeight = cameraHeight;
    }

    Handler handler = new Handler();
    private String TAG ="FaceOverlayView";
    public static Rect faceRect = new Rect();
    private Rect emptyRect = new Rect(0, 0, 0, 0);

    public FaceOverlayView(Context context) {
        super(context);
        initialize();
    }

    public FaceOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public FaceOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    public FaceOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    private void initialize() {
        // We want a green box around the face:
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        int stroke = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, metrics);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.GREEN);
        mPaint.setStrokeWidth(stroke);
        mPaint.setStyle(Paint.Style.STROKE);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setDither(true);
        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, metrics);
        mTextPaint.setTextSize(14);
        mTextPaint.setColor(Color.GREEN);
        mTextPaint.setStyle(Paint.Style.FILL);
    }
    int type = 1;
    public void setFaceRect(int left,int top,int right,int bottom) {
        type = 1;
        if(this.faceRect.left == left &&
        this.faceRect.right == right &&
        this.faceRect.top == top &&
        this.faceRect.bottom == bottom){
            return;
        }
        this.faceRect.left=left;
        this.faceRect.right=right;
        this.faceRect.top=top;
        this.faceRect.bottom=bottom;
        handler.post(() -> invalidate());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try {
            canvas.save();
            if (faceRect != null) {
                Log.d(TAG, "onDraw: 0  " + cameraWidth + "  " + cameraHeight + "   " + previewWidth + "  " + previewHeight);
                Log.d(TAG, "onDraw: 1   " + faceRect.top + "  " + faceRect.bottom + "  " + faceRect.left + "  " + faceRect.right);
                faceRect.top = (int) (1.0 * previewHeight * faceRect.top / cameraHeight);
                faceRect.bottom = (int) (1.0 * previewHeight * faceRect.bottom / cameraHeight);

                faceRect.left = (int) (1.0 * previewWidth * faceRect.left / cameraWidth);
                faceRect.right = (int) (1.0 * previewWidth * faceRect.right / cameraWidth);

                Log.d(TAG, "onDraw: 2   " + faceRect.top + "  " + faceRect.bottom + "  " + faceRect.left + "  " + faceRect.right);
                canvas.drawRect(faceRect, mPaint);

            } else {
                canvas.drawRect(emptyRect, mPaint);
            }
            canvas.restore();
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    public void setPreviewWidth(int previewWidth) {
        this.previewWidth = previewWidth;
    }

    public void setPreviewHeight(int previewHeight) {
        this.previewHeight = previewHeight;
    }

}