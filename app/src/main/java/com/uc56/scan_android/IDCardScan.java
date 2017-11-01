package com.uc56.scan_android;

import android.graphics.Rect;

import com.uc56.scancore.ScanView;


/**
 * Created by banketree on 2017/11/1.
 */

public class IDCardScan implements ScanView.IHandleScanDataListener {
    private IIDCardResultListener listener;

    public IDCardScan(IIDCardResultListener listener) {
        this.listener = listener;
    }

    @Override
    public Boolean onHandleScanData(byte[] data, int width, int height, Rect rect) {
        return false;
    }

    public interface IIDCardResultListener {
        void onScanResult(String result);
    }
}
