package com.mediatek.rcs.pam.activities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Rect;
import android.util.Log;

public class PaUtils {
    private static final String TAG = "PaUtils";

	public static final int UNCONSTRAINED = -1;

	public static Bitmap getBitmapByPath(String path, Options options, int width, int height) {
	    Log.d(TAG, "getBitmapByPath. path=[" + path + "]. w=" + width + ". height=" + height);

	    if (path == null || path.isEmpty() || width < 0 || height <=0) {
	        Log.e(TAG, "getBitmapByPath para error");
	    }

	    File file = new File(path);
	    FileInputStream in = null;
	    try {
	        in = new FileInputStream(file);
	    } catch (FileNotFoundException e) {
	        Log.e(TAG, "file not exist!");
	        e.printStackTrace();
			return null;
		}
		if (options != null) {
		    Rect r = new Rect(0, 0, width, height);
		    int w = r.width();
		    int h = r.height();
		    int maxSize = w > h ? w : h;
		    int inSimpleSize = computeSampleSize(options, maxSize, w * h);
		    options.inSampleSize = inSimpleSize;
		    options.inJustDecodeBounds = false;
		}
		Bitmap bm = null;
		try {
		    bm = BitmapFactory.decodeStream(in, null, options);
		} catch (OutOfMemoryError e) {
		    Log.e(TAG, "bitmap decode fail due to out of memory");
		    e.printStackTrace();
	    	return bm;
	    }
	    try {
	        in.close();
	    } catch (IOException e) {
	        Log.e(TAG, "getBitmapByPath IOException");
	        e.printStackTrace();
	    }
	    return bm;
	}

	public static int computeSampleSize(BitmapFactory.Options options, 
			int minSideLength, int maxNumOfPixels) {
		Log.d(TAG, "computeSampleSize min=" + minSideLength + ". max=" + maxNumOfPixels);
		int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);
		
		int roundedSize;
		if (initialSize <= 8) {
			roundedSize = 1;
			while (roundedSize < initialSize) {
				roundedSize <<= 1;
			}
		} else {
			roundedSize = (initialSize + 7) / 8 * 8;
		}
		Log.d(TAG, "computeSampleSize result=" + roundedSize);
		return roundedSize;
	}
	
	private static int computeInitialSampleSize(BitmapFactory.Options options, 
			int minSideLength, int maxNumOfPixels) {
		double w = options.outWidth;
		double h = options.outHeight;
		int bound;
		
		int lowerBound = (maxNumOfPixels == UNCONSTRAINED) ? 1 : 
						(int)Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
		int upperBound = (minSideLength == UNCONSTRAINED) ? 128 : (int)Math.min(
						Math.floor(w / minSideLength), Math.floor(h / minSideLength));
		
		Log.d(TAG, "computeInitialSampleSize. upperBound=" + upperBound + "lowerBound=" + lowerBound);
		
		if (upperBound < lowerBound) {
			bound = lowerBound;
		}else if ((maxNumOfPixels == UNCONSTRAINED) && (minSideLength == UNCONSTRAINED)) {
			bound = 1;
		} else if (minSideLength == UNCONSTRAINED) {
			bound = lowerBound;
		} else {
			bound = upperBound;
		}
		Log.d(TAG, "computeInitialSampleSize. result=" + bound);
		return bound;
	}
	
	
	
}
