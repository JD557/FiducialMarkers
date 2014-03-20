CC=g++

CFLAGS= -Wall

OPENCV_LIBS = -lopencv_core -lopencv_imgproc -lopencv_highgui -lopencv_ml -lopencv_video -lopencv_features2d -lopencv_calib3d -lopencv_objdetect -lopencv_contrib -lopencv_legacy -lopencv_flann 

all:decoder encoder

decoder:markerDecoder.cpp
	$(CC) $(CFLAGS) markerDecoder.cpp -o decoder $(OPENCV_LIBS) -L/usr/local/lib

encoder:markerEncoder.cpp
	$(CC) $(CFLAGS) markerEncoder.cpp -o encoder 

clean: 
	rm -f encoder decoder