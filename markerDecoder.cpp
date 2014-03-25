#include <opencv2/core/core.hpp> 
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/contrib/contrib.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include <iostream>
#include <vector>
#include <sstream>
using namespace std;
using namespace cv;

const int MAX_DIST = 1500;

Mat frame;
Mat imageSmooth;
Mat imageHsv;
Mat redImage;
Mat greenImage;
Mat blueImage;
Mat imageThr1;
Mat imageThr2;
Mat logo;
Mat marker;

// Filtra a imageSmooth
void improveImage(Mat& image) {
	medianBlur(image,imageSmooth,5);
}

// Extrai pontos vermelhos
void extractRed(Mat& imageHsv) {
	inRange(imageHsv,Scalar(165,120,50),Scalar(180,255,255),imageThr1);
	inRange(imageHsv,Scalar(0,120,50),Scalar(15,255,255),imageThr2);
	bitwise_or(imageThr1,imageThr2,redImage);
	morphologyEx(redImage,redImage,MORPH_CLOSE,Mat());
}

// Extrai pontos verdes
void extractGreen(Mat& imageHsv) {
	inRange(imageHsv,Scalar(45,80,50),Scalar(75,255,255),greenImage);
	morphologyEx(greenImage,greenImage,MORPH_CLOSE,Mat());
}

// Extrai pontos azuis
void extractBlue(Mat& imageHsv) {
	inRange(imageHsv,Scalar(105,80,50),Scalar(135,255,255),blueImage);
	morphologyEx(blueImage,blueImage,MORPH_CLOSE,Mat());
}

int distance(Point pt1,Point pt2) {
	return (pt1.x-pt2.x)*(pt1.x-pt2.x)+(pt1.y-pt2.y)*(pt1.y-pt2.y);
}

int main(int argc, char* argv[]) 
{ 

	logo = imread("clear.png");
	// Blob detector TODO falta afinar os parametros disto
	SimpleBlobDetector::Params pointDetectorParams;
	pointDetectorParams.filterByColor = false;
	pointDetectorParams.filterByArea = true;
	pointDetectorParams.minArea = 10;
	pointDetectorParams.maxArea = 1600;
	pointDetectorParams.filterByCircularity = false;
	pointDetectorParams.minCircularity = 0.5;
	pointDetectorParams.maxCircularity = 1.0;
	pointDetectorParams.filterByInertia = false;
	pointDetectorParams.filterByConvexity = false;
	cout << pointDetectorParams.filterByColor << endl;
	cout << pointDetectorParams.filterByArea << " " << pointDetectorParams.minArea << " " << pointDetectorParams.maxArea << endl;
	cout << pointDetectorParams.filterByCircularity << " " << pointDetectorParams.minCircularity << " " << pointDetectorParams.maxCircularity << endl;
	cout << pointDetectorParams.filterByInertia << endl;
	cout << pointDetectorParams.filterByConvexity << " " << pointDetectorParams.minConvexity << " " << pointDetectorParams.maxConvexity << endl;
	SimpleBlobDetector pointDetector(pointDetectorParams);

	// Camera (0=camera interna, 1=camera USB)
	VideoCapture cap(0);
	namedWindow("Camera",CV_WINDOW_AUTOSIZE);
	if(!cap.isOpened()) {return -1;}
	while (true) {
		if (cap.read(frame)) {
			// Melhora a imagem
			improveImage(frame);
			cvtColor(imageSmooth,imageHsv,CV_BGR2HSV);

			// Extrai pontos verdes e vermelhos
			extractRed(imageHsv);
			extractGreen(imageHsv);
			extractBlue(imageHsv);
			vector<KeyPoint> redKeypoints;
			vector<KeyPoint> greenKeypoints;
			vector<KeyPoint> blueKeypoints;
			pointDetector.detect(redImage,redKeypoints);
			pointDetector.detect(greenImage,greenKeypoints);
			pointDetector.detect(blueImage,blueKeypoints);

			Point pt1;
			Point pt2;
			Point pt3;
			Point pt4;

			// Desenha linhas
			for (int i=0;i<redKeypoints.size();++i) {
				KeyPoint redKP = redKeypoints[i]; 
				pt1 = redKP.pt;
				circle(imageSmooth,redKeypoints[i].pt,10,Scalar(0,0,255),2);
				for (int j=0;j<greenKeypoints.size();++j) {
					KeyPoint greenKP = greenKeypoints[j]; 
					pt3 = greenKP.pt;
					circle(imageSmooth,pt3,10,Scalar(0,255,0),2);
					Point center = Point2i((pt1.x+pt3.x)/2,(pt1.y+pt3.y)/2);
					//circle(imageSmooth,center,10,Scalar(255,0,0),2);
					int vectX=pt3.x-pt1.x;
					int vectY=pt3.y-pt1.y;
					int normX=vectY/2;
					int normY=-vectX/2;
					pt2 = Point2i(center.x+normX,center.y+normY);
					pt4 = Point2i(center.x-normX,center.y-normY);

					unsigned int distPt2 = -1;int bestPt2=-1;
					unsigned int distPt4 = -1;int bestPt4=-1;

					for (int k=0;k<blueKeypoints.size();++k) {
						KeyPoint blueKP = blueKeypoints[k]; 
						circle(imageSmooth,blueKP.pt,10,Scalar(255,0,0),2);
						int newDistPt2 = distance(pt2,blueKP.pt);
						int newDistPt4 = distance(pt4,blueKP.pt);
						if (newDistPt2<distPt2 && newDistPt2<MAX_DIST) {
							bestPt2 = k;
							distPt2 = newDistPt2;
						}
						if (newDistPt4<distPt4 && newDistPt4<MAX_DIST) {
							bestPt4 = k;
							distPt4 = newDistPt4;
						}
					}

					if (bestPt2>=0) {
						KeyPoint bestKP2 = blueKeypoints[bestPt2];
						pt2 = bestKP2.pt;
					}
					if (bestPt4>=0) {
						KeyPoint bestKP4 = blueKeypoints[bestPt4];
						pt4 = bestKP4.pt;
					}

					if (bestPt2!=-1 && bestPt4!=-1) {
						line(imageSmooth,pt1,pt2,Scalar(255,0,0),2);
						line(imageSmooth,pt2,pt3,Scalar(255,0,0),2);
						line(imageSmooth,pt3,pt4,Scalar(255,0,0),2);
						line(imageSmooth,pt4,pt1,Scalar(255,0,0),2);

						std::vector<Point2f> imagePoints(4);
						imagePoints[0]=Point2f(pt1.x,pt1.y);
						imagePoints[1]=Point2f(pt2.x,pt2.y);
						imagePoints[2]=Point2f(pt3.x,pt3.y);
						imagePoints[3]=Point2f(pt4.x,pt4.y);
						std::vector<Point2f> figurePoints(4);
						figurePoints[0]=Point2f(0.0,0.0);
						figurePoints[1]=Point2f(0.0,512.0);
						figurePoints[2]=Point2f(512.0,512.0);
						figurePoints[3]=Point2f(512.0,0.0);
						Mat homography = findHomography(figurePoints, imagePoints, 0);
						Mat invHomography = homography.inv();
						Mat overlay;
						warpPerspective(logo,overlay,homography,Size(imageSmooth.cols,imageSmooth.rows));
						warpPerspective(imageSmooth,marker,invHomography,Size(512,512));
						add(overlay,imageSmooth,imageSmooth);
						imshow("Marker", marker);

					}
				}
			}

			imshow("Camera", imageSmooth);
		}
		if(waitKey(30) == 27) {break;}
	}


	waitKey(0);
	return 0;
}


