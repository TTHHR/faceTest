package cn.qingyuyu.videoserver;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.media.FaceDetector;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import androidx.appcompat.app.AppCompatActivity;

class MainActivity extends AppCompatActivity implements View.OnClickListener
    {
        private SurfaceView preview;
        private Camera camera;
        private Camera.Parameters parameters;
        private int orientionOfCamera;// 前置摄像头的安装角度
        private int faceNumber;// 识别的人脸数
        private FaceDetector.Face[] faces;
        private FindFaceView mFindFaceView;
        private ImageView iv_photo;
        private Button bt_camera;
        TextView mTV;

        /**
         * Called when the activity is first created.
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
        }

        @Override
        protected void onStart() {
            super.onStart();
            iv_photo = (ImageView) findViewById(R.id.iv_photo);
            bt_camera = (Button) findViewById(R.id.bt_camera);
            mTV = (TextView) findViewById(R.id.show_count);
            bt_camera.setOnClickListener(this);

            mFindFaceView = (FindFaceView) findViewById(R.id.my_preview);

            preview = (SurfaceView) findViewById(R.id.preview);
            // 设置缓冲类型（必不可少）
            preview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            // 设置surface的分辨率
            preview.getHolder().setFixedSize(176, 144);
            // 设置屏幕常亮（必不可少）
            preview.getHolder().setKeepScreenOn(true);

            preview.getHolder().addCallback(new SurfaceCallback());
        }

        private final class MyPictureCallback implements PictureCallback {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0,
                            data.length);
                    Matrix matrix = new Matrix();
                    matrix.setRotate(-90);
                    Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap
                            .getWidth(), bitmap.getHeight(), matrix, true);
                    bitmap.recycle();
                    iv_photo.setImageBitmap(bmp);
                    camera.startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

        private final class SurfaceCallback implements Callback {

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                       int height) {
                if (camera != null) {
                    parameters = camera.getParameters();
                    parameters.setPictureFormat(PixelFormat.JPEG);
                    // 设置预览区域的大小
                    parameters.setPreviewSize(width, height);
                    // 设置每秒钟预览帧数
                    parameters.setPreviewFrameRate(20);
                    // 设置预览图片的大小
                    parameters.setPictureSize(width, height);
                    parameters.setJpegQuality(80);
                }
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                int cameraCount = 0;
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                cameraCount = Camera.getNumberOfCameras();
                //设置相机的参数
                for (int i = 0; i < cameraCount; i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        try {
                            camera = Camera.open(i);
                            camera.setPreviewDisplay(holder);
                            setCameraDisplayOrientation(i, camera);
                            //最重要的设置 帧图的回调
                            camera.setPreviewCallback(new MyPreviewCallback());
                            camera.startPreview();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                //记得释放，避免OOM和占用
                if (camera != null) {
                    camera.setPreviewCallback(null);
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }
            }

        }

        private class MyPreviewCallback implements PreviewCallback {

            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                //这里需要注意，回调出来的data不是我们直接意义上的RGB图 而是YUV图，因此我们需要
                //将YUV转化为bitmap再进行相应的人脸检测，同时注意必须使用RGB_565，才能进行人脸检测，其余无效
                Camera.Size size = camera.getParameters().getPreviewSize();
                YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21,
                        size.width, size.height, null);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, size.width, size.height),
                        80, baos);
                byte[] byteArray = baos.toByteArray();
                detectionFaces(byteArray);
            }
        }

        /**
         * 检测人脸
         *
         * @param data 预览的图像数据
         */
        private void detectionFaces(byte[] data) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap bitmap1 = BitmapFactory.decodeByteArray(data, 0, data.length,
                    options);
            int width = bitmap1.getWidth();
            int height = bitmap1.getHeight();
            Matrix matrix = new Matrix();
            Bitmap bitmap2 = null;
            FaceDetector detector = null;
            //设置各个角度的相机，这样我们的检测效果才是最好
            switch (orientionOfCamera) {
                case 0:
                    //初始化人脸检测（下同）
                    detector = new FaceDetector(width, height, 10);
                    matrix.postRotate(0.0f, width / 2, height / 2);
                    // 以指定的宽度和高度创建一张可变的bitmap（图片格式必须是RGB_565，不然检测不到人脸）
                    bitmap2 = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                    break;
                case 90:
                    detector = new FaceDetector(height, width, 1);
                    matrix.postRotate(-270.0f, height / 2, width / 2);
                    bitmap2 = Bitmap.createBitmap(height, width, Bitmap.Config.RGB_565);
                    break;
                case 180:
                    detector = new FaceDetector(width, height, 1);
                    matrix.postRotate(-180.0f, width / 2, height / 2);
                    bitmap2 = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                    break;
                case 270:
                    detector = new FaceDetector(height, width, 1);
                    matrix.postRotate(-90.0f, height / 2, width / 2);
                    bitmap2 = Bitmap.createBitmap(height, width, Bitmap.Config.RGB_565);
                    break;
            }
            //设置支持的面数（最大支持检测多少人的脸 ，可以根据需要调整，不过需要与findFaces中的参数数值相同，否则会抛出异常）
            faces = new FaceDetector.Face[10];
            Paint paint = new Paint();
            paint.setDither(true);
            Canvas canvas = new Canvas();
            canvas.setBitmap(bitmap2);
            canvas.setMatrix(matrix);
            // 将bitmap1画到bitmap2上（这里的偏移参数根据实际情况可能要修改）
            canvas.drawBitmap(bitmap1, 0, 0, paint);
            //这里通过向findFaces中传递帧图转化后的bitmap和最大检测的人脸数face，返回检测后的人脸数
            faceNumber = detector.findFaces(bitmap2, faces);
            mTV.setText("facnumber----" + faceNumber);
            mTV.setTextColor(Color.RED);
            //这里就是我们的人脸识别，绘制识别后的人脸区域的类
            if (faceNumber != 0) {
                mFindFaceView.setVisibility(View.VISIBLE);
                mFindFaceView.drawRect(faces, faceNumber);
            } else {
                mFindFaceView.setVisibility(View.GONE);
            }
            bitmap2.recycle();
            bitmap1.recycle();
        }

        /**
         * 设置相机的显示方向（这里必须这么设置，不然检测不到人脸）
         *
         * @param cameraId 相机ID(0是后置摄像头，1是前置摄像头）
         * @param camera   相机对象
         */
        private void setCameraDisplayOrientation(int cameraId, Camera camera) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int degree = 0;
            switch (rotation) {
                case Surface.ROTATION_0:
                    degree = 0;
                    break;
                case Surface.ROTATION_90:
                    degree = 90;
                    break;
                case Surface.ROTATION_180:
                    degree = 180;
                    break;
                case Surface.ROTATION_270:
                    degree = 270;
                    break;
            }

            orientionOfCamera = info.orientation;
            int result;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degree) % 360;
                result = (360 - result) % 360;
            } else {
                result = (info.orientation - degree + 360) % 360;
            }
            camera.setDisplayOrientation(result);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.bt_camera:
                    if (camera != null) {
                        try {
                            camera.takePicture(null, null, new MyPictureCallback());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        }
    }