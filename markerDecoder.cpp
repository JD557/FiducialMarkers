#include <opencv2/core/core.hpp> 
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/contrib/contrib.hpp>
#include <iostream>
#include <vector>
#include <sstream>
using namespace std;
using namespace cv;

// Filtra a imageSmooth
Mat improveImage(Mat image) {
	Mat newImage;
	medianBlur(image,newImage,5);
	return newImage;
}

// Extrai pontos vermelhos
Mat extractRed(Mat imageHsv) {
	Mat imageThr1;
	Mat imageThr2;
	inRange(imageHsv,Scalar(165,180,50),Scalar(180,255,255),imageThr1);
	inRange(imageHsv,Scalar(0,180,50),Scalar(15,255,255),imageThr2);
	bitwise_or(imageThr1,imageThr2,imageThr1);
	//morphologyEx(imageThr1,imageThr1,MORPH_CLOSE,Mat());
	return imageThr1;
}

// Extrai pontos verdes
Mat extractGreen(Mat imageHsv) {
	Mat imageThr;
	inRange(imageHsv,Scalar(45,100,50),Scalar(75,255,255),imageThr);
	//morphologyEx(imageThr,imageThr,MORPH_CLOSE,Mat());
	return imageThr;
}

// Extrai pontos azuis
Mat extractBlue(Mat imageHsv) {
	Mat imageThr;
	inRange(imageHsv,Scalar(105,120,50),Scalar(135,255,255),imageThr);
	//morphologyEx(imageThr,imageThr,MORPH_CLOSE,Mat());
	return imageThr;
}

int distance(Point pt1,Point pt2) {
	return (pt1.x-pt2.x)*(pt1.x-pt2.x)+(pt1.y-pt2.y)*(pt1.y-pt2.y);
}

int main(int argc, char* argv[]) 
{ 
	// Blob detector TODO falta afinar os parametros disto
	SimpleBlobDetector::Params pointDetectorParams;
	pointDetectorParams.filterByColor = false;
	pointDetectorParams.filterByArea = true;
	pointDetectorParams.minArea = 10;
	pointDetectorParams.maxArea = 1600;
	pointDetectorParams.filterByCircularity = false;
	pointDetectorParams.minCircularity = 0;
	pointDetectorParams.maxCircularity = 0.5;
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
	Mat frame;
	while (true) {
		if (cap.read(frame)) {
			// Melhora a imagem
			Mat imageSmooth = improveImage(frame);	
			Mat imageHsv;
			cvtColor(imageSmooth,imageHsv,CV_BGR2HSV);

			// Extrai pontos verdes e vermelhos
			Mat redImage = extractRed(imageHsv);
			Mat greenImage = extractGreen(imageHsv);
			Mat blueImage = extractBlue(imageHsv);
			vector<KeyPoint> redKeypoints;
			vector<KeyPoint> greenKeypoints;
			vector<KeyPoint> blueKeypoints;
			pointDetector.detect(redImage,redKeypoints);
			pointDetector.detect(greenImage,greenKeypoints);
			pointDetector.detect(blueImage,blueKeypoints);

			// Desenha linhas
			for (int i=0;i<redKeypoints.size();++i) {
				circle(imageSmooth,redKeypoints[i].pt,10,Scalar(0,0,255),2);
				for (int j=0;j<greenKeypoints.size();++j) {
					circle(imageSmooth,greenKeypoints[j].pt,10,Scalar(0,255,0),2);
					Point pt1    = redKeypoints[i].pt;
					Point pt2;
					Point pt3    = greenKeypoints[j].pt;
					Point pt4;
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
						circle(imageSmooth,blueKeypoints[k].pt,10,Scalar(255,0,0),2);
						int newDistPt2 = distance(pt2,blueKeypoints[k].pt);
						int newDistPt4 = distance(pt4,blueKeypoints[k].pt);
						if (newDistPt2<distPt2) {
							bestPt2 = k;
							distPt2 = newDistPt2;
						}
						if (newDistPt4<distPt4) {
							bestPt4 = k;
							distPt4 = newDistPt4;
						}
					}

					if (bestPt2>=0) {pt2 = blueKeypoints[bestPt2].pt;}
					if (bestPt4>=0) {pt4 = blueKeypoints[bestPt4].pt;}

					line(imageSmooth,pt1,pt2,Scalar(255,0,0),2);
					line(imageSmooth,pt2,pt3,Scalar(255,0,0),2);
					line(imageSmooth,pt3,pt4,Scalar(255,0,0),2);
					line(imageSmooth,pt4,pt1,Scalar(255,0,0),2);
				}
			}

			imshow("Camera", imageSmooth);
		}
		if(waitKey(30) == 27) {break;}
	}


	waitKey(0);
	return 0;
}


