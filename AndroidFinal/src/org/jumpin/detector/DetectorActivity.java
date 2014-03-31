package org.jumpin.detector;

import java.util.Timer;
import java.util.TimerTask;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

public class DetectorActivity extends Activity implements CvCameraViewListener2 {

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;
    private Mat                  homography = null;
    private Mat                  logo = null;
    private Mat                  outputLogo = null;
    private Mat                  frame = null;
    private Mat                  outputFrame = null;
	private Mat                  marker = null;
	private int                  markerValue = -1;
	private int                  oldMarkerValue = -1;
    private Timer                timer;
	private int WIDTH;
	private int HEIGHT;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                    outputFrame = new Mat();
                    marker = new Mat();
                	logo = new Mat();
                	Bitmap logoBmp = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
                	Utils.bitmapToMat(logoBmp, logo);
                	Imgproc.resize(logo, logo, new Size(512,512));
                	outputLogo = logo.clone();
                	timer = new Timer();
                	
                	// Detection Task (runs in background)
                	timer.schedule(new TimerTask() {
                		
                		// Hamming codes
                		private final int[][] hamming_matrix = {
                				{0,0,1,1,1},
                				{0,1,0,1,1},
                				{0,1,1,0,1},
                				{0,1,1,1,0},
                				{1,0,0,1,1},
                				{1,0,1,0,1},
                				{1,0,1,1,0},
                				{1,1,0,0,1},
                				{1,1,0,1,0},
                				{1,1,1,0,0},
                				{1,1,1,1,1},
                				{1,1,1,1,0},
                				{1,1,1,0,1},
                				{1,1,0,1,1},
                				{1,1,0,0,0},
                				{1,0,1,1,1}
                			};
                		
                		// Marker Detector object
                		private MarkerDetector detector = new MarkerDetector();
                		
                		/* Decodes a marker
                		 * Uses the planar marker image to try to read it's value
                		 * Returns -1 if the value was incorrect
                		 */
                		public int decodeMarker() {
                			if (frame == null) {return -1;}
                			else {
                				// Inverse homography to get the planar marker image
					    		Imgproc.warpPerspective(frame,marker,homography,new Size(512,512),
					    				Imgproc.WARP_INVERSE_MAP);
	    						int accum = 0; // Accumulator (marker value)
	    						int check = 0; // Check (Hamming codes of the image)
	    						int correctCheck = 0; // Hamming codes og the Accumulator
	    						int counter = 0; // Bit counter (Current bit being read)
	    						for (int y=0;y<6;++y) {
	    							for (int x=0;x<6;++x) {
	    								// Ignore the colored corners
	    								if (!(y<=1 && (x<=1 || x>=4)) &&
	    									!(y>=4 && (x<=1 || x>=4))) {
	    									// Read the value from the middle of the square
	    									double[] val = marker.get(41+470*y/5, 41+470*x/5);
	    									if (val!=null) {
		    									if (val[0]<64 && val[1]<64 && val[2]<64) {
		    										if(counter<15) {
		    											accum|=(1<<counter);
		    											for (int i=0;i<5;++i) {
			    											correctCheck^=(hamming_matrix[counter][i])<<i;
		    											}
		    										}
		    										else {check|=(1<<counter-15);}
		    									}
		    									counter++;
	    									}
	    								}
	    							}
	    						}
	    						if (check==correctCheck) {
	    							return accum;
	    						}
	    						else {
	    							return -1;
	    						}
                			}
                		}
                		
						@Override
						public void run() {
							// If there's a frame, try to find the marker and get the homography
							if (frame!=null && frame.cols()>0 && frame.rows()>0) {
								Mat newHomography=detector.getHomography(frame);
								homography = newHomography;
								// If the marker was found, decode it's value
								if (homography!=null) {
						    		int newMarkerValue = decodeMarker();
						    		// If a valid value was found, update the current value
						    		if (newMarkerValue!=-1) {
						    			markerValue=newMarkerValue;
						    		}
								}
							}
							
						}
                	},1000,20);
                	
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public DetectorActivity() {
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.surface_view);

        if (mIsJavaCamera)
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_surface_view);
        else
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.native_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mItemSwitchCamera = menu.add("Toggle Native/Java camera");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String toastMesage = new String();

        if (item == mItemSwitchCamera) {
            mOpenCvCameraView.setVisibility(SurfaceView.GONE);
            mIsJavaCamera = !mIsJavaCamera;

            if (mIsJavaCamera) {
                mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_surface_view);
                toastMesage = "Java Camera";
            } else {
                mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.native_surface_view);
                toastMesage = "Native Camera";
            }

            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView.setCvCameraViewListener(this);
            mOpenCvCameraView.enableView();
            Toast toast = Toast.makeText(this, toastMesage, Toast.LENGTH_LONG);
            toast.show();
        }

        return true;
    }

    public void onCameraViewStarted(int width, int height) {
    	WIDTH = width;
    	HEIGHT = height;
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	frame = inputFrame.rgba();
    	outputFrame = frame.clone();
    	if (homography != null) {
    		if (markerValue!=-1 && markerValue!=oldMarkerValue) {
    			oldMarkerValue = markerValue;
        		outputLogo = logo.clone();
    			Core.putText(outputLogo, Integer.toString(markerValue), new Point(5,505), Core.FONT_HERSHEY_SIMPLEX, 5,new Scalar(255,0,0),5);
    		}
    		Imgproc.warpPerspective(outputLogo,outputFrame,homography,new Size(WIDTH,HEIGHT),
					Imgproc.INTER_NEAREST,Imgproc.BORDER_TRANSPARENT,new Scalar(0));
    	}
    	return outputFrame;
    }
}
