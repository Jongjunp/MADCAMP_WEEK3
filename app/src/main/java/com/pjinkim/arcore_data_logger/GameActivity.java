package com.pjinkim.arcore_data_logger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameActivity extends AppCompatActivity {

    TextView tv;
    ImageButton shoot;

    // Accurate pose detector on static images, when depending on the pose-detection-accurate sdk
    AccuratePoseDetectorOptions options = new AccuratePoseDetectorOptions.Builder().setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE).build();
    PoseDetector poseDetector = PoseDetection.getClient(options);
    // properties
    private static final String LOG_TAG = GameActivity.class.getName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private final static int REQUEST_CODE_ANDROID = 1001;
    private static String[] REQUIRED_PERMISSIONS = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private ARCoreSession mARCoreSession;

    private Handler mHandler = new Handler();
    private AtomicBoolean mIsRecording = new AtomicBoolean(false);
    private PowerManager.WakeLock mWakeLock;

    private Timer mInterfaceTimer = new Timer();
    private int mSecondCounter = 0;


    Timer timer = new Timer();
    TimerTask locUpdater = new TimerTask() {
        @Override
        public void run() {
            // 위치 정보 보내기
            Log.d("상대 위치",UserInfo.instance.printLoc());
            tv.setText(UserInfo.instance.printLoc());
        }
    };

    // Android activity lifecycle states
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv=findViewById(R.id.tv);
        shoot = findViewById(R.id.shoot);
        shoot.setOnClickListener(new View.OnClickListener() { // 이미지 버튼 이벤트 정의
            @Override
            public void onClick(View v) { //클릭 했을경우
                shotTrigger();
            }
        });

        // check Android and OpenGL version
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        // setup sessions
        mARCoreSession = new ARCoreSession(this);

        // battery power setting
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sensors_data_logger:wakelocktag");
        mWakeLock.acquire();

        timer.schedule(locUpdater, 0, 500);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_ANDROID);
        }
    }


    @Override
    protected void onDestroy() {
        if (mIsRecording.get()) { }
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        super.onDestroy();
    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {

        // check Android version
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(LOG_TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }

        // get current OpenGL version
        String openGlVersionString = ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                .getDeviceConfigurationInfo()
                .getGlEsVersion();

        // check OpenGL version
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(LOG_TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        return true;
    }


    @Override
    public void onBackPressed() {

        // nullify back button when recording starts
        if (!mIsRecording.get()) {
            super.onBackPressed();
        }
    }

    public void shotTrigger()
    {
        Log.d("발사","타격 판정");
        mARCoreSession.hitCheck();
    }

    private static boolean hasPermissions(Context context, String... permissions) {

        // check Android hardware permissions
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
