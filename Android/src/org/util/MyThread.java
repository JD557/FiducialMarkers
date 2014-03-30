package org.util;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import android.util.Log;

public class MyThread extends Thread {
	
	/**
	 * memory allocation for each thread
	 * to stop the overhead of creating it
	 * at each iteration
	 */
    private Mat imageHSV;
    private Mat redImage;
    private Mat greenImage;
    private Mat imgThr1;
    private Mat imgThr2;
    private Mat[] regions;
	
    private MyThreadPool pool;
    private boolean active = true;
    
    public MyThread() {
    	imageHSV   = new Mat();
        redImage   = new Mat();
        greenImage = new Mat();
        imgThr1    = new Mat();
        imgThr2    = new Mat();
        regions    = new Mat[4];
	}
    
    public boolean isActive() {
        return active;
    }
    
    public void setPool(MyThreadPool p) {
        pool = p;
    }
    
    public void run() {
        ResultListener result = pool.getResultListener();
        Work task;
        while (true)
        {
            task = pool.removeFromQueue();
            if (task != null)
            {
                try
                {
                	Log.i("my", "before call");
                    Point[] output = task.call(imageHSV,redImage,greenImage,imgThr1,imgThr2,regions);
                    Log.i("my", "after call");
                    result.finish(output);
                } catch (Exception e)
                {
                    result.error(e);
                }
            } else
            {
                if (!isActive())
                break;
                else
                {
                    synchronized (pool.getWaitLock())
                    {
                        try
                        {
                            pool.getWaitLock().wait();
                        } catch (InterruptedException e)
                        {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
    
    void shutdown() {
        active = false;
        if (imageHSV != null)
        	imageHSV.release();
        imageHSV = null;
        
        if (redImage != null)
        	redImage.release();
        redImage = null;
        
        if (greenImage != null)
        	greenImage.release();
        greenImage = null;
        
        if (imgThr1 != null)
        	imgThr1.release();
        imgThr1 = null;
        
        if (imgThr2 != null)
        	imgThr2.release();
        redImage = null;
    }
}