#include <iostream>
#include <stdlib.h>
#include "bitmap_image.hpp"

//NUMBER OF CELLS
const unsigned int WIDTH = 5;
const unsigned int HEIGHT = 5;
const unsigned int TOTAL_CELLS = WIDTH*HEIGHT;

//CELL SIZE IN PIXELS
const unsigned int CELL_WIDTH  = 30;
const unsigned int CELL_HEIGHT = 30;

//CORNER POSITIONS
const unsigned int TOP_LEFT_CORNER = 0;
const unsigned int TOP_RIGHT_CORNER = WIDTH-1;
const unsigned int BOTTOM_LEFT_CORNER = WIDTH*HEIGHT-WIDTH;
const unsigned int BOTTOM_RIGHT_CORNER = WIDTH*HEIGHT-1;

//RGB COLORS
unsigned int YES_COLOR[]          = {0,0,0}; //BLACK
unsigned int NO_COLOR[]           = {255,255,255}; //WHITE
unsigned int TOP_LEFT_COLOR[]     = {0,255,0}; //GREEN
unsigned int TOP_RIGHT_COLOR[]    = {0,0,255}; //BLUE
unsigned int BOTTOM_LEFT_COLOR[]  = {0,0,255}; //BLUE
unsigned int BOTTOM_RIGHT_COLOR[] = {255,0,0}; //RED


void readme(){
	std::cout << "USAGE: encoder: <id> [output]";
}

std::string dec2bin(unsigned n){
    const int size=sizeof(n)*8;
    std::string res;
    bool s=0;
    for (int a=0;a<size;a++){
        bool bit=n>>(size-1);
        if (bit)
            s=1;
        if (s)
            res.push_back(bit+'0');
        n<<=1;
    }
    if (!res.size())
        res.push_back('0');
    return res;
}

void create_image(std::string filename, std::string id){
	   bitmap_image image(CELL_WIDTH*5,CELL_HEIGHT*5);
	   image.set_all_channels(NO_COLOR[0],NO_COLOR[1],NO_COLOR[2]);
	   unsigned int *color;
	   for(unsigned int i=0; i<TOTAL_CELLS; ++i){
		   switch(i){
		   	   case TOP_LEFT_CORNER:
		   		 color=TOP_LEFT_COLOR;
		   		 break;
		   	   case TOP_RIGHT_CORNER:
		   		 color=TOP_RIGHT_COLOR;
				 break;
		   	   case BOTTOM_LEFT_CORNER:
		   		 color=BOTTOM_LEFT_COLOR;
				 break;
		   	   case BOTTOM_RIGHT_CORNER:
		   		 color=BOTTOM_RIGHT_COLOR;
				 break;
		   	   default:
		   		color=  (i<id.size() && id[i]=='1') ? YES_COLOR : NO_COLOR;
		   		break;
		   }
		   image.set_region(i%5*CELL_WIDTH,i/5*CELL_HEIGHT,CELL_WIDTH,CELL_HEIGHT,color[0],color[1],color[2]);
	   }
	   image.save_image(filename);
}

int main(int argc, char **argv)
{
	if(argc < 2){
		readme();
		return 1;
	}

	std::string id          = dec2bin(atoi(argv[1]));
	std::string output_file = "output.bmp";

	if(argc > 2){
		output_file = argv[2];
	}

	create_image(output_file,id);

	std::cout << id[0] << " " << output_file;
	return 0;
}
