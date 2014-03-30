package org.opencv.tutorials.imgproc;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;


import org.util.MyThreadPool;
import org.util.ResultListener;
import org.util.Work;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;

public class BackProjectionActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private Mat over;
    private MyThreadPool executor;
	private long now;
	private long lastRecognitionTime;

	private boolean updated=false;
	private int WIDTH;
	private int HEIGHT;
	public Mat logo;
	private ResultListener cb;
	
	private Point[] lastPoints = new Point[4];
	
	

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    logo = new Mat(512,512,CvType.CV_8UC4);
                    executor = new MyThreadPool(Runtime.getRuntime().availableProcessors(),cb);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };



    public BackProjectionActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.tutorial1_surface_view);

        if (mIsJavaCamera)
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        else
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_native_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
        
        lastRecognitionTime = Calendar.getInstance().getTimeInMillis();
        
        cb = new ResultListener() {

			@Override
			public void finish(Point[] pt) {
				Log.i("my","cb");
				lastPoints=pt;
				
				if(pt!=null){
					Log.i("my","not null");
					

					MatOfPoint2f camPoints = new MatOfPoint2f(pt[0],pt[1],pt[2],pt[3]);
			        MatOfPoint2f logoPoints = new MatOfPoint2f(
			                        new Point(0.0,0.0),
			                        new Point(0.0,512.0),
			                        new Point(512.0,512.0),
			                        new Point(512.0,0.0));
			        Mat homography = Calib3d.findHomography(logoPoints, camPoints);
					
					over = homography;
					updated=true;
				}
					
			}

			@Override
			public void error(Exception ex) {
				Log.e("my", "exception", ex);
			}
		};
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        executor.stop();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    	
    	WIDTH = width;
    	HEIGHT = height;
    	
    	over = new  Mat();
       
    }

    public void onCameraViewStopped() {
    	 // Explicitly deallocate Mats
        if(over!= null)
        	over.release();
        over = null;
    }

    
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	Mat mCamera = inputFrame.rgba();
    	
    	now = Calendar.getInstance().getTimeInMillis();

        if (now - lastRecognitionTime > 100) {
            lastRecognitionTime = now;
            executor.submit(new Work(mCamera,lastPoints));
        }
    	
        
        if(updated){
        	 Log.i("my","update");
        	 Imgproc.warpPerspective(logo,mCamera,over,new Size(WIDTH,HEIGHT),
                     Imgproc.INTER_NEAREST,Imgproc.BORDER_TRANSPARENT,new Scalar(0));
        	//updated=false;
	    	//Mat mOutputROI = mCamera.submat(0, outputHeight, 0, outputWidth);
	    	//mOutputROI = over;//Imgproc.cvtColor(over, mOutputROI, Imgproc.COLOR_GRAY2BGRA);
        } 
        return mCamera;
    }

	
	
	
	
}