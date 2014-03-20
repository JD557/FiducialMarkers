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
Mat extractRed(Mat image) {
	Mat imageHsv;
	cvtColor(image,imageHsv,CV_BGR2HSV);
	Mat imageThr1;
	Mat imageThr2;
	inRange(imageHsv,Scalar(165,200,50),Scalar(180,255,255),imageThr1);
	inRange(imageHsv,Scalar(0,200,50),Scalar(15,255,255),imageThr2);
	bitwise_or(imageThr1,imageThr2,imageThr1);
	morphologyEx(imageThr1,imageThr1,MORPH_CLOSE,Mat());
	return imageThr1;
}

// Extrai pontos verdes
Mat extractGreen(Mat image) {
	Mat imageHsv;
	cvtColor(image,imageHsv,CV_BGR2HSV);
	Mat imageThr;
	inRange(imageHsv,Scalar(45,100,50),Scalar(75,255,255),imageThr);
	morphologyEx(imageThr,imageThr,MORPH_CLOSE,Mat());
	return imageThr;
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

			// Extrai pontos verdes e vermelhos
			Mat redImage = extractRed(imageSmooth);
			Mat greenImage = extractGreen(imageSmooth);
			vector<KeyPoint> redKeypoints;
			vector<KeyPoint> greenKeypoints;
			pointDetector.detect(redImage,redKeypoints);
			pointDetector.detect(greenImage,greenKeypoints);

			// Desenha Pontos
			for (int i=0;i<redKeypoints.size();++i) {
				circle(imageSmooth,redKeypoints[i].pt,10,Scalar(0,0,255),5);
			}
			for (int i=0;i<greenKeypoints.size();++i) {
				circle(imageSmooth,greenKeypoints[i].pt,10,Scalar(0,255,0),5);
			}
			// Desenha linhas
			for (int i=0;i<redKeypoints.size();++i) {
				for (int j=0;j<greenKeypoints.size();++j) {
					Point pt1    = redKeypoints[i].pt;
					Point pt2    = greenKeypoints[j].pt;
					Point center = Point2i((pt1.x+pt2.x)/2,(pt1.y+pt2.y)/2);
					line(imageSmooth,pt1,pt2,Scalar(255,0,0),2);
					circle(imageSmooth,center,10,Scalar(255,0,0),2);
					int vectX=pt2.x-pt1.x;
					int vectY=pt2.y-pt1.y;
					int normX=vectY/2;
					int normY=-vectX/2;
					line(imageSmooth,Point2i(center.x+normX,center.y+normY),Point2i(center.x-normX,center.y-normY),Scalar(255,0,0),2);
				}
			}

			imshow("Camera", imageSmooth);
		}
		if(waitKey(30) == 27) {break;}
	}


	waitKey(0);
	return 0;
}


