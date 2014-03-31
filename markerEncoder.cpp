#include <iostream>
#include <cstdlib>
#include <vector>
#include <bitset>
#include "bitmap_image.hpp"

//NUMBER OF CELLS (redundancy bits only implemented for 5x5 matrix)
const unsigned int WIDTH       = 6;
const unsigned int HEIGHT      = 6;
const unsigned int TOTAL_CELLS = WIDTH*HEIGHT;

//CELL SIZE IN PIXELS
const unsigned int CELL_WIDTH  = 30;
const unsigned int CELL_HEIGHT = 30;

//CORNER POSITIONS
const unsigned int TOP_LEFT_CORNER_1     = 0;
const unsigned int TOP_LEFT_CORNER_2     = 1;
const unsigned int TOP_LEFT_CORNER_3     = WIDTH;
const unsigned int TOP_LEFT_CORNER_4     = WIDTH+1;

const unsigned int TOP_RIGHT_CORNER_1    = WIDTH-2;
const unsigned int TOP_RIGHT_CORNER_2    = WIDTH-1;
const unsigned int TOP_RIGHT_CORNER_3    = WIDTH+WIDTH-2;
const unsigned int TOP_RIGHT_CORNER_4    = WIDTH+WIDTH-1;


const unsigned int BOTTOM_LEFT_CORNER_1  = TOTAL_CELLS-WIDTH;
const unsigned int BOTTOM_LEFT_CORNER_2  = TOTAL_CELLS-WIDTH+1;
const unsigned int BOTTOM_LEFT_CORNER_3  = TOTAL_CELLS-WIDTH-WIDTH;
const unsigned int BOTTOM_LEFT_CORNER_4  = TOTAL_CELLS-WIDTH-WIDTH+1;


const unsigned int BOTTOM_RIGHT_CORNER_1 = TOTAL_CELLS-2;
const unsigned int BOTTOM_RIGHT_CORNER_2 = TOTAL_CELLS-1;
const unsigned int BOTTOM_RIGHT_CORNER_3 = TOTAL_CELLS-WIDTH-2;
const unsigned int BOTTOM_RIGHT_CORNER_4 = TOTAL_CELLS-WIDTH-1;

//RGB COLORS
unsigned int YES_COLOR[]          = {0,0,0};       //BLACK
unsigned int NO_COLOR[]           = {255,255,255}; //WHITE
unsigned int TOP_LEFT_COLOR[]     = {255,0,0};     //RED
unsigned int TOP_RIGHT_COLOR[]    = {0,255,0};     //GREEN
unsigned int BOTTOM_LEFT_COLOR[]  = {0,255,0};     //GREEN
unsigned int BOTTOM_RIGHT_COLOR[] = {0,255,0};     //GREEN

const std::string DEFAULT_FILE_NAME = "output.bmp";

const int hamming_matrix[16][5] = {
	0,0,1,1,1,
	0,1,0,1,1,
	0,1,1,0,1,
	0,1,1,1,0,
	1,0,0,1,1,
	1,0,1,0,1,
	1,0,1,1,0,
	1,1,0,0,1,
	1,1,0,1,0,
	1,1,1,0,0,
	1,1,1,1,1,
	1,1,1,1,0,
	1,1,1,0,1,
	1,1,0,1,1,
	1,1,0,0,0,
	1,0,1,1,1
};

void readme(){
	std::cout << "USAGE: encoder: <id> [output]";
}

std::bitset<20> dec2bin(unsigned n){
	// convert to binary
	std::bitset<20> bin(n);

	// blank out the 5 most significant
	// to use for redundancy bits
	// if number was over 2^15
	for(unsigned int i=15; i<20; ++i)
		bin[i] = 0;

	return bin;
}

std::bitset<20> add_redundancy(std::bitset<20> id){
	unsigned int shift = 15;
	for(unsigned int i=0; i<5; ++i){
		for(unsigned int j=0; j<shift; ++j){
			id[i+shift] = id[i+shift]^(id[j] & hamming_matrix[j][i]);			
		}
	}
	return id;
}

void create_image(std::string &filename, std::bitset<20> id){
	   bitmap_image image(CELL_WIDTH*WIDTH,CELL_HEIGHT*HEIGHT);

	   // white out the image
	   image.set_all_channels(NO_COLOR[0],NO_COLOR[1],NO_COLOR[2]);

	   // choose color for each cell
	   unsigned int *color;

	   for(unsigned int i=0, id_pos=0; i<TOTAL_CELLS; ++i){
		   switch(i){
		   	   case TOP_LEFT_CORNER_1:
		   	   case TOP_LEFT_CORNER_2:
		   	   case TOP_LEFT_CORNER_3:
		   	   case TOP_LEFT_CORNER_4:
		   		 color = TOP_LEFT_COLOR;
		   		 break;

		   	   case TOP_RIGHT_CORNER_1:
		   	   case TOP_RIGHT_CORNER_2:
		   	   case TOP_RIGHT_CORNER_3:
		   	   case TOP_RIGHT_CORNER_4:
		   		 color = TOP_RIGHT_COLOR;
				 break;

		   	   case BOTTOM_LEFT_CORNER_1:
		   	   case BOTTOM_LEFT_CORNER_2:
		   	   case BOTTOM_LEFT_CORNER_3:
		   	   case BOTTOM_LEFT_CORNER_4:
		   		 color = BOTTOM_LEFT_COLOR;
				 break;

		   	   case BOTTOM_RIGHT_CORNER_1:
		   	   case BOTTOM_RIGHT_CORNER_2:
		   	   case BOTTOM_RIGHT_CORNER_3:
		   	   case BOTTOM_RIGHT_CORNER_4:
		   		 color = BOTTOM_RIGHT_COLOR;
				 break;

		   	   default:
		   		// check if we finished printing
		   		// if not, paint the next bit
		   		if(id_pos>=0 && id[id_pos++]){
		   			color = YES_COLOR;
		   		} else {
		   			continue;
		   		}
		   		break;
		   }
		   
		   unsigned int x = (i%WIDTH)*CELL_WIDTH;
		   unsigned int y = (i/WIDTH)*CELL_HEIGHT;
		   image.set_region(x,y,CELL_WIDTH,CELL_HEIGHT,color[0],color[1],color[2]);

	   }
	   image.save_image(filename);
}

int main(int argc, char **argv)
{
	if(argc < 2){
		readme();
		return 1;
	}

	unsigned int id_decimal = atoi(argv[1]);

	// 20 bits total -> 15 bits for id encoding ; 5 bits for redundancy
	// |__redundancy__|_______________________id________________________|
	// [P5,P4,P3,P2,P1,B14,B13,B12,B11,B10,B9,B8,B7,B6,B5,B4,B3,B2,B1,B0]
	std::bitset<20> id = add_redundancy(dec2bin(id_decimal));

	//use name from user or default for the output image
	std::string output_file = argc > 2 ? argv[2] : DEFAULT_FILE_NAME;

	//create representation
	create_image(output_file,id);

	std::cout << "id:" << argv[1] << " encoded:" << id << " output:"<< output_file;
	return 0;
}
