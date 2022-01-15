package com.pjinkim.arcore_data_logger;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingFailureReason;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import uk.co.appoly.arcorelocation.LocationScene;

public class ARCoreSession {

    // properties
    private static final String LOG_TAG = ARCoreSession.class.getName();
    private static final long mulSecondToNanoSecond = 1000000000;
    private long previousTimestamp = 0;

    private MainActivity mContext;
    private ArFragment mArFragment;
    private PointCloudNode mPointCloudNode;
    private AccumulatedPointCloud mAccumulatedPointCloud;
    private WorldToScreenTranslator mWorldToScreenTranslator;

    private AtomicBoolean mIsRecording = new AtomicBoolean(false);
    private AtomicBoolean mIsWritingFile = new AtomicBoolean(false);

    private int mNumberOfFeatures = 0;
    private TrackingState mTrackingState;
    private TrackingFailureReason mTrackingFailureReason;
    private double mUpdateRate = 0;

    private LocationScene locationScene;

    AccuratePoseDetectorOptions options;
    PoseDetector poseDetector;

    // constructor
    public ARCoreSession(@NonNull MainActivity context) {

        mContext = context;
        mArFragment = (ArFragment) mContext.getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        mArFragment.getArSceneView().getPlaneRenderer().setVisible(false);
        mArFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);

        mPointCloudNode = new PointCloudNode(mContext);
        mArFragment.getArSceneView().getScene().addChild(mPointCloudNode);
        mAccumulatedPointCloud = new AccumulatedPointCloud();
        mWorldToScreenTranslator = new WorldToScreenTranslator();

        options =new AccuratePoseDetectorOptions.Builder().setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE).build();
        poseDetector = PoseDetection.getClient(options);
    }

    private void onUpdateFrame(FrameTime frameTime) {

        // set some variables
        boolean isFileSaved = (mIsRecording.get() && mIsWritingFile.get());

        // obtain current ARCore information
        mArFragment.onUpdate(frameTime);
        Frame frame = mArFragment.getArSceneView().getArFrame();
        Camera camera = frame.getCamera();

        // update ARCore measurements
        long timestamp = frame.getTimestamp();
        double updateRate = (double) mulSecondToNanoSecond / (double) (timestamp - previousTimestamp);
        previousTimestamp = timestamp;

        TrackingState trackingState = camera.getTrackingState();
        TrackingFailureReason trackingFailureReason = camera.getTrackingFailureReason();
        Pose T_gc = frame.getAndroidSensorPose();

        float qx = T_gc.qx();
        float qy = T_gc.qy();
        float qz = T_gc.qz();
        float qw = T_gc.qw();

        float tx = T_gc.tx();
        float ty = T_gc.ty();
        float tz = T_gc.tz();

        UserInfo.instance.setrelateLoc(tx, ty, tz);

        // update 3D point cloud from ARCore
        PointCloud pointCloud = frame.acquirePointCloud();
        IntBuffer bufferPointID = pointCloud.getIds();
        FloatBuffer bufferPoint3D = pointCloud.getPoints();
        mPointCloudNode.visualize(pointCloud);
        int numberOfFeatures = mAccumulatedPointCloud.getNumberOfFeatures();
        pointCloud.release();
    }

    void hitCheck() {
        //이미지 추출
        Image view = null;
        try {
            view = mArFragment.getArSceneView().getArFrame().acquireCameraImage();
        } catch (NotYetAvailableException e) {
            e.printStackTrace();
        }

        final Bitmap bitmap = imageToBitmap(view);

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        Task<com.google.mlkit.vision.pose.Pose> task;
        Image finalView = view;
        task = poseDetector.process(image).addOnSuccessListener(new OnSuccessListener<com.google.mlkit.vision.pose.Pose>() {
            @Override
            public void onSuccess(@NonNull com.google.mlkit.vision.pose.Pose pose) {
                if(pose.getAllPoseLandmarks().size()>0) {
                    PointF headPos = pose.getPoseLandmark(0).getPosition();
                    Log.d("머리 위치",headPos.x/image.getWidth()+", "+headPos.y/image.getHeight());
                }
            }
        });
        task.addOnCompleteListener(new OnCompleteListener<com.google.mlkit.vision.pose.Pose>() {

            @Override
            public void onComplete(@NonNull Task<com.google.mlkit.vision.pose.Pose> task) {
                finalView.close();
            }
        });
    }
    private Bitmap imageToBitmap (Image image) {
        int width = image.getWidth();
        int height = image.getHeight();

        byte[] nv21;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        // U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, os);
        byte[] jpegByteArray = os.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);

        Matrix matrix = new Matrix();
        matrix.setRotate(90);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
