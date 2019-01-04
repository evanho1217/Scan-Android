package com.uc56.scan_android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.common.HybridBinarizer;
import com.uc56.scan_android.test2.PlanarYUVLuminanceSource;
import com.uc56.scancore.ScanView;
import com.uc56.scancore.ScanView2;
import com.uc56.scancore.zbar.ZBarScan;
import com.uc56.scancore.zxing.QRCodeDecoder;
import com.uc56.scancore.zxing.ZXingScan;

import java.io.ByteArrayOutputStream;

import cn.bingoogolapple.photopicker.activity.BGAPhotoPickerActivity;

public class TestScan2Activity extends AppCompatActivity {
    private static final String TAG = TestScan2Activity.class.getSimpleName();
    private static final int REQUEST_CODE_CHOOSE_QRCODE_FROM_GALLERY = 666;

    private ScanView2 scanView2;
    private ImageView imageView;
    private TextView scanResultTextView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_scan2);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        scanView2 = (ScanView2) findViewById(R.id.zxingview);
        imageView = (ImageView) findViewById(R.id.img_camera);
        scanResultTextView = (TextView) findViewById(R.id.tv_scan_code);
        test();
    }

    @Override
    protected void onStart() {
        super.onStart();
        scanView2.startCamera();
        scanView2.postDelayed(new Runnable() {
            @Override
            public void run() {
                scanView2.startSpotAndShowRect();
            }
        }, 300);
    }

    @Override
    protected void onStop() {
        scanView2.stopCamera();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        scanView2.onDestroy();
        super.onDestroy();
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }

    public void onScanQRCodeSuccess(final String result) {
        Log.i(TAG, "result:" + result);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
                vibrate();
                scanView2.startSpot();
                scanResultTextView.setText(result);
            }
        });
    }


    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_spot:
                scanView2.startSpot();
                break;
            case R.id.stop_spot:
                scanView2.stopSpot();
                break;
            case R.id.start_spot_showrect:
                scanView2.startSpotAndShowRect();
                break;
            case R.id.stop_spot_hiddenrect:
                scanView2.stopSpotAndHiddenRect();
                break;
            case R.id.show_rect:
                scanView2.showScanRect();
                break;
            case R.id.hidden_rect:
                scanView2.hiddenScanRect();
                break;
            case R.id.start_preview:
                scanView2.startCamera();
                break;
            case R.id.stop_preview:
                scanView2.stopCamera();
                break;
            case R.id.open_flashlight:
                scanView2.openFlashlight();
                break;
            case R.id.close_flashlight:
                scanView2.closeFlashlight();
                break;
            case R.id.scan_barcode:
                test();
                scanView2.addScanBoxView(View.inflate(this, R.layout.layout_scanbox_bar, null));
                scanView2.getScanBoxView().setTipText("将条形码放入框中");

                break;
            case R.id.scan_qrcode:
                test();
                scanView2.addScanBoxView(View.inflate(this, R.layout.layout_scanbox_qrcode, null));
                scanView2.getScanBoxView().setTipText("将二维码放入框中");
                break;
            case R.id.choose_qrcde_from_gallery:
                /*
                从相册选取二维码图片，这里为了方便演示，使用的是
                https://github.com/bingoogolapple/BGAPhotoPicker-Android
                这个库来从图库中选择二维码图片，这个库不是必须的，你也可以通过自己的方式从图库中选择图片
                 */
                startActivityForResult(BGAPhotoPickerActivity.newIntent(this, null, 1, null, false), REQUEST_CODE_CHOOSE_QRCODE_FROM_GALLERY);
                break;

            case R.id.tv_switch_camera:
                if (scanView2.isCameraNewView()) {
                    scanView2.showCameraByOld();
                } else {
                    scanView2.showCameraByNew();
                }
                scanView2.startCamera();
                scanView2.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanView2.startSpotAndShowRect();
                    }
                }, 300);
                break;
        }
    }

    static Bitmap bitmap = null;

    private void test() {
        try {
            scanView2.addScanBoxView(View.inflate(this, R.layout.layout_scanbox_qrcode, null));
            scanView2.getScanBoxView().setTipText("将证件放入框中");
            scanView2.getScanBoxView().setOnlyDecodeScanBoxArea(true);

        } catch (Exception e) {
            e.printStackTrace();
        }

        scanView2.removeHandleScanDataListenerAll();
        scanView2.addHandleScanDataListener(new ZXingScan(new ZXingScan.IZXingResultListener() {
            @Override
            public boolean onScanResult(BarcodeFormat codeFormat, String result) {
                onScanQRCodeSuccess("ZXingScan:" + result + "  " + codeFormat.name());
                return false;
            }
        }));

        scanView2.addHandleScanDataListener(new ZBarScan(new ZBarScan.IZbarResultListener() {
            @Override
            public boolean onScanResult(me.dm7.barcodescanner.zbar.BarcodeFormat codeFormat, String result) {
                onScanQRCodeSuccess("ZBarScan:" + result + "  " + codeFormat.getName());
                return false;
            }
        }));

        scanView2.addHandleScanDataListener(new IDCardScan(new IDCardScan.IIDCardResultListener() {//身份证
            @Override
            public void onScanResult(final String result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onScanQRCodeSuccess("IDCardScan:" + result);
                    }
                });
            }
        }) {
            @Override
            public Boolean onHandleScanData(final byte[] previewData, final byte[] desData, final int format, int width, int height, Rect rect) {
                super.onHandleScanData(previewData, desData, format, width, height, rect);
                if (format == ImageFormat.JPEG) {
                    try {
                        //将rawImage转换成bitmap
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.RGB_565;
                        Bitmap srcBitmap = BitmapFactory.decodeByteArray(desData, 0, desData.length, options);
                        final Bitmap bitmap1 = Bitmap.createBitmap(srcBitmap, rect.left, rect.top, rect.width(), rect.height());
                        srcBitmap.recycle();
                        srcBitmap = null;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(bitmap1);
                                if (bitmap != null) bitmap.recycle();
                                bitmap = bitmap1;
                                imageView.postInvalidate();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    byte[] data = new byte[previewData.length];
                    ScanView.rotateYUV240SP(previewData, data, height, width);//旋转

                    try {
                        ByteArrayOutputStream baos;
                        byte[] rawImage;
                        BitmapFactory.Options newOpts = new BitmapFactory.Options();
                        newOpts.inJustDecodeBounds = true;
                        YuvImage yuvimage = new YuvImage(
                                data,
                                ImageFormat.NV21,//YUV240SP
                                width,
                                height,
                                null);
                        baos = new ByteArrayOutputStream();
                        yuvimage.compressToJpeg(rect, 100, baos);// 80--JPG图片的质量[0-100],100最高
                        rawImage = baos.toByteArray();
                        //将rawImage转换成bitmap
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.RGB_565;
                        final Bitmap bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        scanView2.showScanRect();

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_CHOOSE_QRCODE_FROM_GALLERY) {
            final String picturePath = BGAPhotoPickerActivity.getSelectedImages(data).get(0);

            /*
            这里为了偷懒，就没有处理匿名 AsyncTask 内部类导致 Activity 泄漏的问题
            请开发在使用时自行处理匿名内部类导致Activity内存泄漏的问题，处理方式可参考 https://github.com/GeniusVJR/LearningNotes/blob/master/Part1/Android/Android%E5%86%85%E5%AD%98%E6%B3%84%E6%BC%8F%E6%80%BB%E7%BB%93.md
             */
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    return QRCodeDecoder.syncDecodeQRCode(picturePath);
                }

                @Override
                protected void onPostExecute(String result) {
                    if (TextUtils.isEmpty(result)) {
                        Toast.makeText(TestScan2Activity.this, "未发现二维码", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(TestScan2Activity.this, result, Toast.LENGTH_SHORT).show();
                    }
                }
            }.execute();
        }
    }
}