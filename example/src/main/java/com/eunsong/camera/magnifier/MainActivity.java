package com.eunsong.camera.magnifier;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


import com.eunsong.camera.PhotoViewAttacher;
import com.eunsong.camera.RxCamera;
import com.eunsong.camera.RxCameraData;
import com.eunsong.camera.config.CameraUtil;
import com.eunsong.camera.config.RxCameraConfig;
import com.eunsong.camera.request.Func;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "Magnifier";

    private static final String[] REQUEST_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int REQUEST_PERMISSION_CODE = 233;

    private PhotoViewAttacher mAttacher;
    private TextureView textureView;
    private ToggleButton captureBtn;
    private ToggleButton flashBtn;
    private ToggleButton helpBtn;
    private Button lockBtn;
    private Button takePhotoBtn;
    private Button saveBtn;
    private TextView logTextView;
    private ScrollView logArea;
    private ImageView imageView;
    private ImageView helpView;
    private SensorManager sm;
    private Sensor accSensor;
    private GregorianCalendar today;
    private RxCamera camera;
    private SeekBar adjustBrightnessBar;
    private SeekBar adjustMagnifyBar;
    private Bitmap bitmap;

    private int brightnessLv;
    private int zoomLv;
    private long lastTimeBackPressed;
    float y1 = 0.0f;
    float y2 = 0.0f;
    float x1 = 0.0f;
    float x2 = 0.0f;
    float cal[] = new float[3];
    int numCheck;
    int startSensor;
    int checkCapture;
    int checkOpenCamera;
    int checkScreenLock;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = (TextureView) findViewById(R.id.preview_surface);
        flashBtn = (ToggleButton) findViewById(R.id.flashBtn);
        imageView = (ImageView)findViewById(R.id.imageView);
        helpView = (ImageView)findViewById(R.id.helpScreen);
        captureBtn = (ToggleButton) findViewById(R.id.capture);
        helpBtn = (ToggleButton) findViewById(R.id.help);
        lockBtn = (Button) findViewById(R.id.lock);
        logTextView = (TextView) findViewById(R.id.log_textview);
        logArea = (ScrollView) findViewById(R.id.log_area);
        takePhotoBtn = (Button) findViewById(R.id.takePhoto);
        saveBtn = (Button) findViewById(R.id.save);
        mAttacher = new PhotoViewAttacher(imageView);
        adjustBrightnessBar = (SeekBar) findViewById(R.id.seekbar);
        adjustMagnifyBar = (SeekBar) findViewById(R.id.seekbar2);
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        accSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        startCamera();

        ViewTreeObserver vto = adjustBrightnessBar.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                Resources res = getResources();
                Drawable thumb = res.getDrawable(R.drawable.brightness2);
                int h = (int) (adjustBrightnessBar.getMeasuredHeight() * 0.22);
                int w = h;
                Bitmap bmpOrg = ((BitmapDrawable)thumb).getBitmap();
                Bitmap bmpScaled = Bitmap.createScaledBitmap(bmpOrg, w, h, true);
                Drawable newThumb = new BitmapDrawable(res, bmpScaled);
                newThumb.setBounds(0, 0, newThumb.getIntrinsicWidth(), newThumb.getIntrinsicHeight());
                adjustBrightnessBar.setThumb(newThumb);

                adjustBrightnessBar.getViewTreeObserver().removeOnPreDrawListener(this);

                return true;
            }
        });
        ViewTreeObserver vto2 = adjustMagnifyBar.getViewTreeObserver();
        vto2.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                Resources res = getResources();
                Drawable thumb = res.getDrawable(R.drawable.magthumb);
                int h = (int) (adjustMagnifyBar.getMeasuredHeight() * 0.11);
                int w = h;
                Bitmap bmpOrg = ((BitmapDrawable)thumb).getBitmap();
                Bitmap bmpScaled = Bitmap.createScaledBitmap(bmpOrg, w, h, true);
                Drawable newThumb = new BitmapDrawable(res, bmpScaled);
                newThumb.setBounds(0, 0, newThumb.getIntrinsicWidth(), newThumb.getIntrinsicHeight());
                adjustMagnifyBar.setThumb(newThumb);

                adjustMagnifyBar.getViewTreeObserver().removeOnPreDrawListener(this);

                return true;
            }
        });

        flashBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(flashBtn.isChecked()){
                    if (!checkCamera()) {return;}
                    flashBtn.setBackgroundResource(R.drawable.flash);
                    camera.action().flashAction(true).subscribe(new Subscriber<RxCamera>() {
                        @Override
                        public void onCompleted() {
                        }
                        @Override
                        public void onError(Throwable e) {
                            showLog("open flash error: " + e.getMessage());
                        }
                        @Override
                        public void onNext(RxCamera rxCamera) {
                            showLog("open flash");
                        }
                    });
                }else{
                    if (!checkCamera()) {return;}
                    flashBtn.setBackgroundResource(R.drawable.lightoff);
                    camera.action().flashAction(false).subscribe(new Subscriber<RxCamera>() {
                        @Override
                        public void onCompleted() {
                        }
                        @Override
                        public void onError(Throwable e) {
                            showLog("open flash error: " + e.getMessage());
                        }
                        @Override
                        public void onNext(RxCamera rxCamera) {
                            showLog("open flash");
                        }
                    });
                }
            }
        });

        captureBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(captureBtn.isChecked()){
                    if (!checkCamera()) {return;}
                    checkCapture = 1;
                    requestTakePicture();
                }else{
                    if (!checkCamera()) {return;}
                    camera.getNativeCamera().startPreview();
                    imageView.setVisibility(View.INVISIBLE);
                    saveBtn.setVisibility(View.INVISIBLE);
                    adjustBrightnessBar.setVisibility(View.VISIBLE);
                    adjustMagnifyBar.setVisibility(View.VISIBLE);
                    flashBtn.setVisibility(View.VISIBLE);
                    helpBtn.setVisibility(View.VISIBLE);
                    takePhotoBtn.setVisibility(View.VISIBLE);
                    lockBtn.setVisibility(View.VISIBLE);
                    captureBtn.setBackgroundResource(R.drawable.pause);
                    autoFocus();
                }
            }
        });
        takePhotoBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                    if (!checkCamera()) {return;}
                    requestTakePicture();
                }
        });
        saveBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (!checkCamera()) {return;}
                saveFile();
            }
        });
        lockBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                    Toast.makeText(MainActivity.this, " 터치 잠금모드입니다. \n 취소하려면 뒤로가기 버튼을 누르세요.", Toast.LENGTH_SHORT).show();
                    checkScreenLock = 1;
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                    adjustBrightnessBar.setVisibility(View.INVISIBLE);
                    adjustMagnifyBar.setVisibility(View.INVISIBLE);
                    flashBtn.setVisibility(View.INVISIBLE);
                    helpBtn.setVisibility(View.INVISIBLE);
                    lockBtn.setVisibility(View.INVISIBLE);
                    captureBtn.setVisibility(View.INVISIBLE);
                    takePhotoBtn.setVisibility(View.INVISIBLE);
            }
        });
        helpBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(helpBtn.isChecked() ){
                    helpView.setVisibility(View.VISIBLE);
                }else{
                    helpView.setVisibility(View.INVISIBLE);
                }
            }
        });
        changeSeekbar2();
        adjustBrightnessBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub

                Camera.Parameters parameters = camera.getNativeCamera().getParameters();
                int maxExpo = parameters.getMaxExposureCompensation();
                int minExpo = parameters.getMinExposureCompensation();
                if (progress >= 50) {
                    float val = (float) ((progress - 50.0) / 50.0) * maxExpo;
                    int value = (int) val;
                    setBrightness(value);
                } else {
                    float val = (float) ((50.0 - progress) / 50.0) * minExpo;
                    int value = (int) val;
                    setBrightness(value);
                }
                seekBar.setProgress(progress);
            }
        });
        textureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!checkCamera()) {
                    return false;
                }
                int action = event.getAction();
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    y1 = event.getY();
                    x1 = event.getX();
                    final float x = event.getX();
                    final float y = event.getY();
                    final Rect rect = CameraUtil.transferCameraAreaFromOuterSize(new Point((int) x, (int) y),
                            new Point(textureView.getWidth(), textureView.getHeight()), 100);
                    List<Camera.Area> areaList = Collections.singletonList(new Camera.Area(rect, 1000));
                    Observable.zip(camera.action().areaFocusAction(areaList),
                            camera.action().areaMeterAction(areaList),
                            new Func2<RxCamera, RxCamera, Object>() {
                                @Override
                                public Object call(RxCamera rxCamera, RxCamera rxCamera2) {
                                    return rxCamera;
                                }
                            }).subscribe(new Subscriber<Object>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            showLog("area focus and metering failed: " + e.getMessage());
                        }

                        @Override
                        public void onNext(Object o) {
                            showLog(String.format("area focus and metering success, x: %s, y: %s, area: %s", x, y, rect.toShortString()));
                        }
                    });
                } else if (action == MotionEvent.ACTION_MOVE) {
                    Log.d("action", "" + action);
                    x2 = event.getX();
                    y2 = event.getY();
                    float xx = Math.abs(x1 - x2);
                    float yy = Math.abs(y1 - y2);

                    if (yy > xx && xx <= 100) {
                        actionZoom(y1, y2);
                    } else if (yy < xx && yy <= 100) {
                        actionBrightness(x1, x2);
                    }
                    y1 = y2;
                    x1 = x2;
                }
                return true;
            }
        });
    }
    @Override
    public void onBackPressed(){
        if ( checkScreenLock == 1 ){
            Toast.makeText(MainActivity.this, "터치 잠금모드가 해제되었습니다.", Toast.LENGTH_SHORT).show();
            checkScreenLock = 0;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            adjustBrightnessBar.setVisibility(View.VISIBLE);
            adjustMagnifyBar.setVisibility(View.VISIBLE);
            flashBtn.setVisibility(View.VISIBLE);
            helpBtn.setVisibility(View.VISIBLE);
            lockBtn.setVisibility(View.VISIBLE);
            captureBtn.setVisibility(View.VISIBLE);
            takePhotoBtn.setVisibility(View.VISIBLE);
            return;
        }else {
            if ((System.currentTimeMillis() - lastTimeBackPressed) < 1500) {
                finish();
                return;
            }
            Toast.makeText(MainActivity.this, "뒤로가기 버튼을 한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
            lastTimeBackPressed = System.currentTimeMillis();
        }
    }

    private void setBrightness(int value) {
        Camera.Parameters parameters = camera.getNativeCamera().getParameters();
        parameters.setExposureCompensation(value);
        camera.getNativeCamera().setParameters(parameters);
    }

    private void actionBrightness(float x1, float x2) {
        if (!checkCamera()) {
            return;
        }
        int r = (int) (x1 - x2);
        Camera.Parameters parameters = camera.getNativeCamera().getParameters();
        int maxExpo = parameters.getMaxExposureCompensation();
        int minExpo = parameters.getMinExposureCompensation();

        if (r <= 0) {
            if (brightnessLv != maxExpo) { brightnessLv += (-r) / 20; }
            if (brightnessLv >= maxExpo) { brightnessLv = maxExpo; }
        } else {
            if (minExpo < brightnessLv) { brightnessLv += (-r) / 20; }
            if(brightnessLv <= minExpo) { brightnessLv = minExpo; }
        }
        setBrightness(brightnessLv);
        adjustBrightnessBar.setProgress(adjustBrightnessBar.getProgress()+brightnessLv);
    }

    @Override
    public void onResume() {
        super.onResume();
        sm.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(checkOpenCamera == 1){
           camera.getNativeCamera().stopPreview();
        }
        sm.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && startSensor == 1) {
            if (numCheck == 1) {
                cal[0] = event.values[0];
                cal[1] = event.values[1];
                cal[2] = event.values[2];
                numCheck = 0;
            }
            if ((Math.abs(cal[0] - event.values[0]) > 1)
                    || (Math.abs(cal[1] - event.values[1]) > 1)
                    || (Math.abs(cal[2] - event.values[2]) > 1)) {
                autoFocus();
            }
        }
    }

    public void autoFocus() {
        final float x = textureView.getWidth() / 2;
        final float y = textureView.getHeight() / 2;

        final Rect rect = CameraUtil.transferCameraAreaFromOuterSize(new Point((int) x, (int) y),
                new Point(textureView.getWidth(), textureView.getHeight()), 100);
        List<Camera.Area> areaList = Collections.singletonList(new Camera.Area(rect, 1000));
        Observable.zip(camera.action().areaFocusAction(areaList),
                camera.action().areaMeterAction(areaList),
                new Func2<RxCamera, RxCamera, Object>() {
                    @Override
                    public Object call(RxCamera rxCamera, RxCamera rxCamera2) {
                        return rxCamera;
                    }
                }).subscribe(new Subscriber<Object>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                showLog("area focus and metering failed: " + e.getMessage());
            }

            @Override
            public void onNext(Object o) {
                showLog(String.format("area focus and metering success, x: %s, y: %s, area: %s", x, y, rect.toShortString()));
            }
        });
        numCheck = 1;
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        for (String permission : REQUEST_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, REQUEST_PERMISSIONS, REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "화면을 터치하면 포커스가 적용됩니다.", Toast.LENGTH_LONG).show();
                openCamera();
            }
        }
    }

    private void openCamera() {
        startSensor = 1;
        checkOpenCamera = 1;
        RxCameraConfig config = new RxCameraConfig.Builder()
                .useBackCamera()
                .setMuteShutterSound(true)
                .setAutoFocus(true)
                .setPreferPreviewFrameRate(15, 30)
                .setPreferPreviewSize(new Point(4032, 3024), false)//해상도 화질 설정
                .setHandleSurfaceEvent(true)
                .build();
        Log.d(TAG, "config: " + config);
        RxCamera.open(this, config).flatMap(new Func1<RxCamera, Observable<RxCamera>>() {
            @Override
            public Observable<RxCamera> call(RxCamera rxCamera) {
                showLog("isopen: " + rxCamera.isOpenCamera() + ", thread: " + Thread.currentThread());
                camera = rxCamera;
                return rxCamera.bindTexture(textureView);
            }
        }).flatMap(new Func1<RxCamera, Observable<RxCamera>>() {
            @Override
            public Observable<RxCamera> call(RxCamera rxCamera) {
                showLog("isbindsurface: " + rxCamera.isBindSurface() + ", thread: " + Thread.currentThread());
                return rxCamera.startPreview();
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<RxCamera>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                showLog("open camera error: " + e.getMessage());
            }

            @Override
            public void onNext(final RxCamera rxCamera) {
                camera = rxCamera;
                showLog("open camera success: " + camera);
            }
        });
        autoFocus();
    }

    private void showLog(String s) {
        Log.d(TAG, s);
        logTextView.append(s + "\n");
        logTextView.post(new Runnable() {
            @Override
            public void run() {
                logArea.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (camera != null) {
            camera.closeCamera();
        }
        mAttacher.cleanup();
    }

    private void requestTakePicture() {
        if (!checkCamera()) {
            return;
        }
        camera.request().takePictureRequest(true, new Func() {
            @Override
            public void call() {
                showLog("Captured!");
            }
        }, 3024, 4032, ImageFormat.JPEG, false).subscribe(new Action1<RxCameraData>() {

            @Override
            public void call(RxCameraData rxCameraData) {
                today = new GregorianCalendar();
                String path = Environment.getExternalStorageDirectory() + File.separator + "Magnifier" + File.separator
                        + today.get(Calendar.YEAR) + "-"
                        + (today.get(Calendar.MONTH) + 1) + "-"
                        + today.get(Calendar.DAY_OF_MONTH) + " "
                        + today.get(Calendar.HOUR_OF_DAY) + ":"
                        + today.get(Calendar.MINUTE) + ":"
                        + today.get(Calendar.SECOND) + ".jpeg";
                File file = new File(path);
               if ( !file.getParentFile().exists() ) {
                    file.getParentFile().mkdirs();
                }
                bitmap = BitmapFactory.decodeByteArray(rxCameraData.cameraData, 0, rxCameraData.cameraData.length);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                        rxCameraData.rotateMatrix, false);

                if ( checkCapture == 0 ) {
                    try {
                        file.createNewFile();
                        FileOutputStream fos = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    showLog("Save file on " + path);
                    Toast.makeText(MainActivity.this, "Save file on " + path, Toast.LENGTH_SHORT).show();
                }
                else {
                    camera.getNativeCamera().stopPreview();
                    imageView.setImageBitmap(bitmap);
                    mAttacher.setScaleType(ImageView.ScaleType.FIT_XY);
                    imageView.setVisibility(View.VISIBLE);
                    adjustBrightnessBar.setVisibility(View.INVISIBLE);
                    adjustMagnifyBar.setVisibility(View.INVISIBLE);
                    flashBtn.setVisibility(View.INVISIBLE);
                    helpBtn.setVisibility(View.INVISIBLE);
                    lockBtn.setVisibility(View.INVISIBLE);
                    takePhotoBtn.setVisibility(View.INVISIBLE);
                    saveBtn.setVisibility(View.VISIBLE);
                    captureBtn.setBackgroundResource(R.drawable.play);
                    checkCapture = 0;
                }
            }
        });
    }

    private void actionZoom(float y1, float y2) {
        if (!checkCamera()) {
            return;
        }
        int r = (int) (y1 - y2);
        Camera.Parameters parameters = camera.getNativeCamera().getParameters();
        int maxZoom = parameters.getMaxZoom();
        adjustMagnifyBar.setMax(maxZoom);
        if (r >= 0) {
            if (zoomLv != maxZoom) { zoomLv += (r) / 20; }
            if (zoomLv >= maxZoom) { zoomLv = maxZoom; }
        } else {
            if (zoomLv > 0) { zoomLv += (r) / 20; }
            if (zoomLv < 0) { zoomLv = 0; }
        }
        adjustMagnifyBar.setProgress(zoomLv);

        camera.action().zoom(zoomLv).subscribe(new Subscriber<RxCamera>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                showLog("zoom error: " + e.getMessage());
            }

            @Override
            public void onNext(RxCamera rxCamera) {
                showLog("zoom success: " + rxCamera);
            }
        });
    }

    private void changeSeekbar2() {
        adjustMagnifyBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                camera.action().zoom(progress).subscribe(new Subscriber<RxCamera>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        showLog("zoom error: " + e.getMessage());
                    }

                    @Override
                    public void onNext(RxCamera rxCamera) {
                        showLog("zoom success: " + rxCamera);
                    }
                });
            }
        });
    }
    private boolean checkCamera() {
        if (camera == null || !camera.isOpenCamera()) {
            return false;
        }
        return true;
    }

    private void startCamera() {
        if (!checkPermission()) {
            requestPermission();
        } else {
            openCamera();
        }
    }
    private void saveFile(){
        today = new GregorianCalendar();
        String path = Environment.getExternalStorageDirectory() + File.separator + "Magnifier" + File.separator
                + today.get(Calendar.YEAR) + "-"
                + (today.get(Calendar.MONTH) + 1) + "-"
                + today.get(Calendar.DAY_OF_MONTH) + " "
                + today.get(Calendar.HOUR_OF_DAY) + ":"
                + today.get(Calendar.MINUTE) + ":"
                + today.get(Calendar.SECOND) + ".jpeg";
        File file = new File(path);
        if ( !file.getParentFile().exists() ) {
            file.getParentFile().mkdirs();
        }
        try {
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        showLog("Save file on " + path);
        Toast.makeText(MainActivity.this, "Save file on " + path, Toast.LENGTH_SHORT).show();
    }
}