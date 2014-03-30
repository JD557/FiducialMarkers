package org.opencv.samples.tutorial1;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.Objdetect;
import org.opencv.video.KalmanFilter;

import android.provider.MediaStore.Video;
import android.util.Log;

public class MarkerDetector {
	private final int MAX_DIST = 2000;
	private final int MIN_DIST = 20;
	private final int MIN_SIZE = 5;
	private final double SCALE_FACTOR = 4.0;
	private final int REGION_SIZE = 20;
	private final double UPDATE = 0.8;
	private Mat imageHSV;
	private Mat redImage;
	private Mat greenImage;
	private Mat imgThr1;
	private Mat imgThr2;
	
	private Point[] pt = new Point[4];
	private Mat[] regions = new Mat[4];
	
	private boolean detected = false;
	
	public MarkerDetector() {
		imageHSV = new Mat();
		redImage = new Mat();
		greenImage = new Mat();
		imgThr1 = new Mat();
		imgThr2 = new Mat();
	}
	
	private int clamp(int value, int min, int max) {
		if (value<min) {return min;}
		else if (value>max) {return max;}
		else return value;
	}

	private int distance(Point pt1,Point pt2) {
		return (int)((pt1.x-pt2.x)*(pt1.x-pt2.x)+(pt1.y-pt2.y)*(pt1.y-pt2.y));
	}

	
	private void extractRed(Mat imageHSV) {
		Core.inRange(imageHSV,new Scalar(165,60,80),new Scalar(180,255,255),imgThr1);
		Core.inRange(imageHSV,new Scalar(0,60,80),new Scalar(15,255,255),imgThr2);
		Core.bitwise_or(imgThr1, imgThr2, redImage);
		Imgproc.morphologyEx(redImage, redImage, Imgproc.MORPH_CLOSE, new Mat());
	}
	
	private void extractGreen(Mat imageHSV) {
		Core.inRange(imageHSV,new Scalar(25,60,60),new Scalar(85,255,255),greenImage);
		Imgproc.morphologyEx(greenImage, greenImage, Imgproc.MORPH_CLOSE, new Mat());
	}
	
	public Mat getHomography(Mat frame) {
		if (!detected) {
			return detect(frame);
		}
		else {
			return track(frame);
		}
	}
	
	public Mat track(Mat frame) {
		Imgproc.cvtColor(frame, imageHSV, Imgproc.COLOR_RGB2HSV);
		for (int i=0;i<4;++i) {
			regions[i] = imageHSV.submat(
				clamp((int)pt[i].y-REGION_SIZE,0,imageHSV.rows()),
				clamp((int)pt[i].y+REGION_SIZE,0,imageHSV.rows()),
				clamp((int)pt[i].x-REGION_SIZE,0,imageHSV.cols()),
				clamp((int)pt[i].x+REGION_SIZE,0,imageHSV.cols())
			);
		}
		
		extractRed(regions[0]);
		Point newPt = new Point(0.0,0.0);
		int values = 0;
		for (int y=0;y<redImage.rows();++y) {
			for (int x=0;x<redImage.cols();++x) {
				if (redImage.get(y, x)[0]==255) {
					newPt.x+=x;
					newPt.y+=y;
					values++;
				}
			}
		}
		if (values==0) {detected=false;return null;}
		pt[0].x = (1.0-UPDATE)*pt[0].x + UPDATE * (pt[0].x - REGION_SIZE + newPt.x/values);
		pt[0].y = (1.0-UPDATE)*pt[0].y + UPDATE * (pt[0].y - REGION_SIZE + newPt.y/values);
		
		for (int i=1;i<4;++i) {
			extractGreen(regions[i]);
			newPt = new Point(0.0,0.0);
			values = 0;
			for (int y=0;y<greenImage.rows();++y) {
				for (int x=0;x<greenImage.cols();++x) {
					if (greenImage.get(y, x)[0]==255) {
						newPt.x+=x;
						newPt.y+=y;
						values++;
					}
				}
			}
			if (values==0) {detected=false;return null;}
			pt[i].x = (1.0-UPDATE)*pt[i].x + UPDATE * (pt[i].x - REGION_SIZE + newPt.x/values);
			pt[i].y = (1.0-UPDATE)*pt[i].y + UPDATE * (pt[i].y - REGION_SIZE + newPt.y/values);
		}
		
		MatOfPoint2f camPoints = new MatOfPoint2f(pt[0],pt[1],pt[2],pt[3]);
		MatOfPoint2f logoPoints = new MatOfPoint2f(
				new Point(0.0,0.0),
				new Point(0.0,512.0),
				new Point(512.0,512.0),
				new Point(512.0,0.0));
		Mat homography = Calib3d.findHomography(logoPoints, camPoints);

		return homography;
		//return null;
		
		
	}
		
	public Mat detect(Mat frame) {
		Imgproc.cvtColor(frame, imageHSV, Imgproc.COLOR_RGB2HSV);
		Imgproc.resize(imageHSV, imageHSV, new Size(),1/SCALE_FACTOR,1/SCALE_FACTOR,Imgproc.INTER_NEAREST);
		extractRed(imageHSV);
		extractGreen(imageHSV);
		//extractBlue();
		ArrayList<Point> redKeypoints = new ArrayList<Point>();
		ArrayList<Point> greenKeypoints = new ArrayList<Point>();
		for (int y=0;y<imageHSV.rows();++y) {
			for (int x=0;x<imageHSV.cols();++x) {
				if (redImage.get(y, x)[0]==255) {
					int size = Imgproc.floodFill(redImage, new Mat(), new Point(x,y),new Scalar(0));
					int offset = (int)Math.sqrt((double)size)/2;
					if (size>MIN_SIZE) {redKeypoints.add(new Point(SCALE_FACTOR*(x+offset),SCALE_FACTOR*(y+offset)));}
				}
				if (greenImage.get(y, x)[0]==255) {
					int size = Imgproc.floodFill(greenImage, new Mat(), new Point(x,y),new Scalar(0));
					int offset = (int)Math.sqrt((double)size)/2;
					if (size>MIN_SIZE) {greenKeypoints.add(new Point(SCALE_FACTOR*(x+offset),SCALE_FACTOR*(y+offset)));}
				}
			}
		}
		Log.i("KEYPOINTS",redKeypoints.size()+" "+greenKeypoints.size());
		if (redKeypoints.size()<1 || greenKeypoints.size()<3) {
			detected=false;
			return null;
		}
		for (int i=0;i<4;++i) {pt[i]=new Point();}
		
		for (int i=0;i<redKeypoints.size();++i) {
			pt[0] = redKeypoints.get(i);
			//Core.circle(frame, pt[0], 5, new Scalar(255,0,0),3);
			for (int j=0;j<greenKeypoints.size();++j) {
				pt[2] = greenKeypoints.get(j);
				//Core.circle(frame, pt[2], 5, new Scalar(0,255,0),3);
				
				Point center = new Point((pt[0].x+pt[2].x)/2,(pt[0].y+pt[2].y)/2);
				int vectX=(int)(pt[2].x-pt[0].x);
				int vectY=(int)(pt[2].y-pt[0].y);
				int normX=vectY/2;
				int normY=-vectX/2;
				pt[1] = new Point(center.x-normX,center.y-normY);
				pt[3] = new Point(center.x+normX,center.y+normY);
				
				int distPt1 = Integer.MAX_VALUE;int bestPt1=-1;
				int distPt3 = Integer.MAX_VALUE;int bestPt3=-1;
				for (int k=0;k<greenKeypoints.size();++k) {
					if (k!=j) {
						Point blueKP = greenKeypoints.get(k);
						//Core.circle(frame, blueKP, 5, new Scalar(0,0,255),3);
						int newDistPt0 = distance(pt[0],blueKP);
						int newDistPt1 = distance(pt[1],blueKP);
						int newDistPt3 = distance(pt[3],blueKP);
						if (newDistPt0>MIN_DIST) {
							if (newDistPt1<distPt1 && newDistPt1<MAX_DIST/*newDistPt3*/) {
								bestPt1 = k;
								distPt1 = newDistPt1;
							}
							if (newDistPt3<distPt3 && newDistPt3<MAX_DIST/*newDistPt1*/) {
								bestPt3 = k;
								distPt3 = newDistPt3;
							}
						}
					}
				}
				Log.i("Results",bestPt1+" "+bestPt3);
				if (bestPt1!=-1 && bestPt3!=-1 && bestPt1!=bestPt3) {
					pt[1] = greenKeypoints.get(bestPt1);
					pt[3] = greenKeypoints.get(bestPt3);
				
					MatOfPoint2f camPoints = new MatOfPoint2f(pt[0],pt[1],pt[2],pt[3]);
					MatOfPoint2f logoPoints = new MatOfPoint2f(
							new Point(0.0,0.0),
							new Point(0.0,512.0),
							new Point(512.0,512.0),
							new Point(512.0,0.0));
					Mat homography = Calib3d.findHomography(logoPoints, camPoints);
					//warpPerspective(frame,marker,homography,new Size(512,512),WARP_INVERSE_MAP);
					detected = true;
					return homography;
				}
			}
		}
		detected = false;
		return null;
	}
}
