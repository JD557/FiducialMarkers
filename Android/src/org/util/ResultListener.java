package org.util;

import org.opencv.core.Point;

public interface ResultListener  {
 
 public void finish(Point[] obj);
 public void error(Exception ex);
 
}