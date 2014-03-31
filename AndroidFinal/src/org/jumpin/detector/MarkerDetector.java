package org.jumpin.detector;

import java.util.ArrayList;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;


public class MarkerDetector {
	private final int MAX_DIST = 2000; // Max. distance between estimated and found green corners
	private final int MIN_DIST = 20; // Min. distance between blobs
	private final int MIN_SIZE = 5; // Min. blob area
	private final double SCALE_FACTOR = 4.0; // Downsampling factor
	private final double UPDATE = 0.8; // Tracking update factor
	
	// Image buffers (to avoid memory reallocation)
	private Mat imageHSV;
	private Mat redImage;
	private Mat greenImage;
	private Mat imgThr1;
	private Mat imgThr2;
	private Mat[] regions = new Mat[4];
	
	// Marker points
	private Point[] pt = new Point[4];
	
	// Phase (Detection or Tracking)
	private boolean detected = false;
	
	public MarkerDetector() {
		imageHSV = new Mat();
		redImage = new Mat();
		greenImage = new Mat();
		imgThr1 = new Mat();
		imgThr2 = new Mat();
	}
	
	// Clamps a value between min and max
	private int clamp(int value, int min, int max) {
		if (value<min) {return min;}
		else if (value>max) {return max;}
		else return value;
	}

	// Calculates the square of the distance between P1 and P2
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
		// Estimates a neighbourhood size
		int REGION_SIZE = (int)(Math.sqrt(distance(pt[0],pt[3]))/4);
		
		// Extract only the regions in the neighbourhood of the detected points
		for (int i=0;i<4;++i) {
			regions[i] = imageHSV.submat(
				clamp((int)pt[i].y-REGION_SIZE,0,imageHSV.rows()),
				clamp((int)pt[i].y+REGION_SIZE,0,imageHSV.rows()),
				clamp((int)pt[i].x-REGION_SIZE,0,imageHSV.cols()),
				clamp((int)pt[i].x+REGION_SIZE,0,imageHSV.cols())
			);
		}
		
		// Updates the points positions
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
		
		// Calculates the homography
		MatOfPoint2f camPoints = new MatOfPoint2f(pt[0],pt[1],pt[2],pt[3]);
		MatOfPoint2f logoPoints = new MatOfPoint2f(
				new Point(85.0,85.0),
				new Point(85.0,427.0),
				new Point(427.0,427.0),
				new Point(427.0,85.0)
			);
		Mat homography = Calib3d.findHomography(logoPoints, camPoints);

		return homography;
		
		
	}
		
	public Mat detect(Mat frame) {
		// Downsamples the image
		Imgproc.resize(frame, imageHSV, new Size(),1/SCALE_FACTOR,1/SCALE_FACTOR,Imgproc.INTER_NEAREST);
		Imgproc.cvtColor(imageHSV, imageHSV, Imgproc.COLOR_RGB2HSV);

		extractRed(imageHSV);
		extractGreen(imageHSV);
		ArrayList<Point> redKeypoints = new ArrayList<Point>();
		ArrayList<Point> greenKeypoints = new ArrayList<Point>();
		// Extracts the blobs and estimates their center based on their area
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
		// If there are not enough keypoints, stop the search
		if (redKeypoints.size()<1 || greenKeypoints.size()<3) {
			detected=false;
			return null;
		}
		for (int i=0;i<4;++i) {pt[i]=new Point();}
		
		for (int i=0;i<redKeypoints.size();++i) {
			pt[0] = redKeypoints.get(i);
			for (int j=0;j<greenKeypoints.size();++j) {
				pt[2] = greenKeypoints.get(j);
				
				// Estimates the position of two corners, based on the other two
				Point center = new Point((pt[0].x+pt[2].x)/2,(pt[0].y+pt[2].y)/2);
				int vectX=(int)(pt[2].x-pt[0].x);
				int vectY=(int)(pt[2].y-pt[0].y);
				int normX=vectY/2;
				int normY=-vectX/2;
				pt[1] = new Point(center.x-normX,center.y-normY);
				pt[3] = new Point(center.x+normX,center.y+normY);
				
				// Get the closest green blobs to the estimated points
				int distPt1 = Integer.MAX_VALUE;int bestPt1=-1;
				int distPt3 = Integer.MAX_VALUE;int bestPt3=-1;
				for (int k=0;k<greenKeypoints.size();++k) {
					if (k!=j) {
						Point blueKP = greenKeypoints.get(k);
						int newDistPt0 = distance(pt[0],blueKP);
						int newDistPt1 = distance(pt[1],blueKP);
						int newDistPt3 = distance(pt[3],blueKP);
						if (newDistPt0>MIN_DIST) {
							if (newDistPt1<distPt1 && newDistPt1<MAX_DIST) {
								bestPt1 = k;
								distPt1 = newDistPt1;
							}
							if (newDistPt3<distPt3 && newDistPt3<MAX_DIST) {
								bestPt3 = k;
								distPt3 = newDistPt3;
							}
						}
					}
				}
				
				// Check if the points are valid
				if (bestPt1!=-1 && bestPt3!=-1 && bestPt1!=bestPt3) {
					pt[1] = greenKeypoints.get(bestPt1);
					pt[3] = greenKeypoints.get(bestPt3);
				
					MatOfPoint2f camPoints = new MatOfPoint2f(pt[0],pt[1],pt[2],pt[3]);
					MatOfPoint2f logoPoints = new MatOfPoint2f(
							new Point(85.0,85.0),
							new Point(85.0,427.0),
							new Point(427.0,427.0),
							new Point(427.0,85.0)
						);
					Mat homography = Calib3d.findHomography(logoPoints, camPoints);
					detected = true;
					return homography;
				}
			}
		}
		detected = false;
		return null;
	}
}
