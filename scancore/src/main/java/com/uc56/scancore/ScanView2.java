package com.uc56.scancore;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.uc56.scancore.Interface.IHandleScanDataListener;
import com.uc56.scancore.Interface.ICameraP;
import com.uc56.scancore.Interface.ICameraPreviewFrame;
import com.uc56.scancore.camera.OldCameraP;
import com.uc56.scancore.camera2.NewCameraP;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import me.dm7.barcodescanner.core.DisplayUtils;


public class ScanView2 extends RelativeLayout implements ICameraPreviewFrame {
    protected ScanBoxView mBoxView;

    private volatile Queue<IHandleScanDataListener> handleScanDataListenerQueue = new ConcurrentLinkedQueue<IHandleScanDataListener>();

    protected volatile boolean mSpotAble = false;
    protected Thread mProcessDataTask;
    LayoutParams layoutParams;
    private Context context;
    private ICameraP iCameraP;//新老 camera 版本
    private FrameLayout containerFrameLayout;
    FrameLayout.LayoutParams containerLayoutParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

    public ScanView2(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ScanView2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        this.context = context;
        layoutParams = new LayoutParams(context, attrs);
        containerFrameLayout = new FrameLayout(context);
        containerFrameLayout.setId(R.id.scan_camera_preview_frame);
        addView(containerFrameLayout);
        layoutParams.addRule(RelativeLayout.ALIGN_TOP, containerFrameLayout.getId());
        layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, containerFrameLayout.getId());
        showCameraByOld();
    }

    public void showCameraByOld() {
        addCameraView(false);
    }

    public void showCameraByNew() {
        addCameraView(true);
    }

    private void addCameraView(boolean newView) {
        boolean first = iCameraP == null;
        removeCameraView();
        iCameraP = newView ? new NewCameraP(context) : new OldCameraP(context);
        iCameraP.setCameraPreviewFrame(this);
        containerFrameLayout.addView(iCameraP.getCameraPreView(), containerLayoutParams);
    }

    public void removeCameraView() {
        if (iCameraP == null) return;
        containerFrameLayout.removeView(iCameraP.getCameraPreView());
        iCameraP.stopCamera();
        iCameraP.onDestroy();
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isCameraNewView() {
        return iCameraP != null && (iCameraP instanceof NewCameraP);
    }

    public void addScanBoxView(View view) {
        if (mBoxView != null) {
            removeView(mBoxView);
            mBoxView.setVisibility(GONE);
            mBoxView = null;
        }
        if (view instanceof ScanBoxView) {
        } else {
            return;
        }
        mBoxView = (ScanBoxView) view;
        addView(mBoxView, layoutParams);
        mBoxView.setVisibility(VISIBLE);
    }

    public void removeScanBoxView() {
        if (mBoxView != null) {
            removeView(mBoxView);
            mBoxView.setVisibility(GONE);
        }
        mBoxView = null;
    }

    private synchronized Queue<IHandleScanDataListener> getHandleScanDataListenerQueque() {
        return handleScanDataListenerQueue;
    }

    /**
     * 添加扫描二维码的处理
     *
     * @param listener 扫描二维码的处理
     */
    public void addHandleScanDataListener(IHandleScanDataListener listener) {
        if (listener == null)
            return;
        if (!getHandleScanDataListenerQueque().isEmpty()) {
            for (IHandleScanDataListener itemListener : getHandleScanDataListenerQueque()) {
                if (itemListener == listener)
                    return;
            }
        }
        getHandleScanDataListenerQueque().add(listener);
    }

    /**
     * 删除扫描二维码的处理
     *
     * @param listener 扫描二维码的处理
     */
    public void removeHandleScanDataListener(IHandleScanDataListener listener) {
        if (listener == null)
            return;
        if (!getHandleScanDataListenerQueque().isEmpty()) {
            for (IHandleScanDataListener itemListener : getHandleScanDataListenerQueque()) {
                if (itemListener == listener) {
                    getHandleScanDataListenerQueque().remove(listener);
                    listener.release();
                    return;
                }
            }
        }
    }

    public void removeHandleScanDataListenerAll() {
        if (!getHandleScanDataListenerQueque().isEmpty()) {
            for (IHandleScanDataListener itemListener : getHandleScanDataListenerQueque()) {
                try {
                    getHandleScanDataListenerQueque().remove(itemListener);
                    itemListener.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public ScanBoxView getScanBoxView() {
        return mBoxView;
    }

    /**
     * 显示扫描框
     */
    public void showScanRect() {
        if (mBoxView != null) {
            mBoxView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 隐藏扫描框
     */
    public void hiddenScanRect() {
        if (mBoxView != null) {
            mBoxView.setVisibility(View.GONE);
        }
    }

    public View getCameraPreView() {
        return iCameraP.getCameraPreView();
    }

    /**
     * 打开后置摄像头开始预览，但是并未开始识别
     */
    public void startCamera() {
        iCameraP.startCamera();
    }

    /**
     * 关闭摄像头预览，并且隐藏扫描框
     */
    public void stopCamera() {
        iCameraP.stopCamera();
    }

    /**
     * 延迟0.5秒后开始识别
     */
    public void startSpot() {
        mSpotAble = true;
        iCameraP.startSpot();
    }

    /**
     * 停止识别
     */
    public void stopSpot() {
        cancelProcessDataTask();
        mSpotAble = false;
        iCameraP.stopSpot();
    }

    /**
     * 停止识别，并且隐藏扫描框
     */
    public void stopSpotAndHiddenRect() {
        try {
            stopSpot();
            hiddenScanRect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示扫描框，并且延迟1.5秒后开始识别
     */
    public void startSpotAndShowRect() {
        try {
            startSpot();
            showScanRect();
        } catch (Exception e) {
        }
    }

    /**
     * 打开闪光灯
     */
    public void openFlashlight() {
        iCameraP.openFlashlight();
    }

    /**
     * 关闭散光灯
     */
    public void closeFlashlight() {
        iCameraP.closeFlashlight();
    }

    /**
     * 销毁二维码扫描控件
     */
    public void onDestroy() {
        stopSpot();
        iCameraP.onDestroy();
    }

    /**
     * 取消数据处理任务
     */
    protected void cancelProcessDataTask() {
        if (mProcessDataTask != null) {
            try {
                mProcessDataTask.stop();
            } catch (Exception e) {
            }
        }
        mProcessDataTask = null;
    }

    @Override
    public void onPreviewFrame(final ICameraP iCameraP, final byte[] previewData, final int format, final int previewWidth, final int previewHeight) {
        try {
            if (!mSpotAble || getHandleScanDataListenerQueque().isEmpty() || mProcessDataTask != null)
                return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            final Rect rect = mBoxView != null ? mBoxView.getScanBoxAreaRect(previewWidth, previewHeight) : null;
            mProcessDataTask = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!mSpotAble || getHandleScanDataListenerQueque().isEmpty())
                        return;

                    try { //数据处理
                        if (format == ImageFormat.JPEG) {
                            handleNewCameraPreviewData(iCameraP, previewData, format, previewWidth, previewHeight, rect);
                        } else {
                            handleOldCameraPreviewData(iCameraP, previewData, format, previewWidth, previewHeight, rect);
                        }
                    } catch (Exception e) {
                    }

                    try { //是否继续扫描
                        if (!mSpotAble || getHandleScanDataListenerQueque().isEmpty())
                            return;

                        boolean isContinuity = false;
                        for (IHandleScanDataListener listener : getHandleScanDataListenerQueque()) {
                            if (listener.isContinuity()) {
                                isContinuity = listener.isContinuity();
                                break;
                            }
                        }

                        if (!isContinuity)
                            return;
                    } catch (Exception e) {
                    }
                    try { //继续扫描
                        if (iCameraP instanceof OldCameraP) {
                            ((OldCameraP) iCameraP).continueShot();
                        }
                    } catch (Exception e) {
                    }
                    cancelProcessDataTask();
                }
            });
            mProcessDataTask.start();
        } catch (Exception e) {
        }
    }

    public void handleOldCameraPreviewData(final ICameraP iCameraP, final byte[] previewData, final int format, final int previewWidth, final int previewHeight, final Rect boxRect) throws Exception {
        int width = previewWidth;
        int height = previewHeight;
        byte[] data = previewData;
        if (DisplayUtils.getScreenOrientation(getContext()) == Configuration.ORIENTATION_PORTRAIT) {
            int rotationCount = getRotationCount();
            if (rotationCount == 1 || rotationCount == 3) {
                data = getRotatedData(rotationCount, data, width, height);

                int tmp = width;
                width = height;
                height = tmp;
            }
        }

        if (!mSpotAble || getHandleScanDataListenerQueque().isEmpty())
            return;

        for (IHandleScanDataListener listener : getHandleScanDataListenerQueque()) {
            if (listener.onHandleScanData(previewData, data, format, width, height, boxRect) && mSpotAble && !getHandleScanDataListenerQueque().isEmpty())
                break;
        }
    }

    public void handleNewCameraPreviewData(final ICameraP iCameraP, final byte[] previewData, final int format, final int previewWidth, final int previewHeight, Rect boxRect) throws Exception {
        if (!mSpotAble || getHandleScanDataListenerQueque().isEmpty())
            return;

        int width = previewWidth;
        int height = previewHeight;
        byte[] data = previewData;

        if (DisplayUtils.getScreenOrientation(getContext()) == Configuration.ORIENTATION_PORTRAIT) {
            int rotationCount = getRotationCount();
            if (rotationCount == 1 || rotationCount == 3) {
                int tmp = width;
                width = height;
                height = tmp;
            }
        }

//        boxRect = ScanUtil.getRectInPreview(context, boxRect, new Point(width, height));
        for (IHandleScanDataListener listener : getHandleScanDataListenerQueque()) {
            if (listener.onHandleScanData(previewData, data, format, width, height, boxRect) && mSpotAble && !getHandleScanDataListenerQueque().isEmpty())
                break;
        }
    }

    public int getRotationCount() {
        int displayOrientation = ScanUtil.getDisplayOrientation(context, Camera.CameraInfo.CAMERA_FACING_BACK);
        return displayOrientation / 90;
    }

    public static void rotateYUV240SP(byte[] src, byte[] des, int width, int height) {
        int wh = width * height;
        //旋转Y
        int k = 0;
//        for (int i = 0; i < width; i++) { //左右互调了
//            for (int j = 0; j < height; j++) {
//                des[k] = src[width * j + i];
//                k++;
//            }
//        }
//
//        for (int i = 0; i < width; i += 2) {
//            for (int j = 0; j < height / 2; j++) {
//                des[k] = src[wh + width * j + i];
//                des[k + 1] = src[wh + width * j + i + 1];
//                k += 2;
//            }
//        }
        for (int i = 0; i < width; i++) { //正解
            for (int j = height - 1; j >= 0; j--) {
                des[k] = src[width * j + i];
                k++;
            }
        }
        for (int i = 0; i < width; i += 2) {
            for (int j = height / 2 - 1; j >= 0; j--) {
                des[k] = src[wh + width * j + i];
                des[k + 1] = src[wh + width * j + i + 1];
                k += 2;
            }
        }
    }

    public static byte[] getRotatedData(int rotationCount, byte[] data, int width, int height) {
        if (rotationCount == 1 || rotationCount == 3) {
            for (int i = 0; i < rotationCount; ++i) {
                byte[] rotatedData = new byte[data.length];

                int y;
                for (y = 0; y < height; ++y) {
                    for (int x = 0; x < width; ++x) {
                        rotatedData[x * height + height - y - 1] = data[x + y * width];
                    }
                }

                data = rotatedData;
                y = width;
                width = height;
                height = y;
            }
        }

        return data;
    }
}