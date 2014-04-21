package com.felicekarl.foragingtech.views.fragments;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import com.felicekarl.foragingtech.R;
import com.felicekarl.foragingtech.listeners.CameraFragmentButtonListener;
import com.felicekarl.foragingtech.listeners.UpdateCameraFragmentButtonListener;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class CameraFragment extends BaseFragment implements TextureView.SurfaceTextureListener,
		OnClickListener, UpdateCameraFragmentButtonListener {
	private static final String TAG = CameraFragment.class.getSimpleName();
	
	private TextureView mTextureView;
	private TextureView mImageView;
	private SurfaceTexture mSurfaceTexture;
	private Surface mSurface;
	private RelativeLayout camera_texture_view_wrappter;
	private RelativeLayout camera_image_view_wrappter;
	private RelativeLayout camera_photo_view_wrappter;
	private Button btn_image_mode1;
	private Button btn_image_mode2;
	private Button btn_image_mode3;
	private Button btn_image_mode4;
	private Button btn_image_save;
	private Button btn_photo_save;
	private LinearLayout ll_image_mode;
	
	private CameraFragmentButtonListener mCameraFragmentButtonListener;
	
	private RenderThread mThread;
	
	private Bitmap video;
	private long startMs;
	private int imageWidth;
	private int imageHeight;
	// opencv variables
//	private Mat tmp;
//	private Mat mIntermediateMat, mIntermediateMat2;
//	private Mat thresholdImage;
//	private Mat circles;
	private Point pt;
	
	private Object mFrameSyncObject = new Object();     // guards mFrameAvailable
	private boolean mFrameAvailable = true;
	 
	public enum IMAGEMODE{
		CANNY, HOUGHCIRCLES, REDHOUGHCIRCLES, REDTHRESHOLD
	}
	
	public enum CAMERAMODE {
		FLYING, NAVIGATING
	}
	
	private IMAGEMODE imgMode;
	private CAMERAMODE camMode;
	
	public void setImageMode(IMAGEMODE imgMode) {
		this.imgMode = imgMode;
	}
	
	public IMAGEMODE getImageMode() {
		return imgMode;
	}
	
	public void setCameraMode(CAMERAMODE camMode) {
		this.camMode = camMode;
		if (camMode.equals(CAMERAMODE.FLYING)) {
			setCameraFullScreen();
			camera_image_view_wrappter.setVisibility(View.VISIBLE);
			ll_image_mode.setVisibility(View.VISIBLE);
			ViewGroup.MarginLayoutParams params = (MarginLayoutParams) camera_photo_view_wrappter.getLayoutParams();
			params.setMargins(0, 0, 0, 0);
			camera_photo_view_wrappter.requestLayout();
		} else if (camMode.equals(CAMERAMODE.NAVIGATING)) {
			setCameraSmallScreen();
			camera_image_view_wrappter.setVisibility(View.INVISIBLE);
			ll_image_mode.setVisibility(View.INVISIBLE);
			ViewGroup.MarginLayoutParams params = (MarginLayoutParams) camera_photo_view_wrappter.getLayoutParams();
			params.setMargins(0, 190, 0, 0);
			camera_photo_view_wrappter.requestLayout();
		}
	}
	
	public CAMERAMODE getCameraMode() {
		return camMode;
	}
	
	public CameraFragment() {
		
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	view = (ViewGroup) inflater.inflate(R.layout.fragment_camera, container, false);
    	mTextureView = (TextureView) view.findViewById(R.id.camera_texture_view);
    	mTextureView.setSurfaceTextureListener(this);
    	mImageView = (TextureView) view.findViewById(R.id.camera_image_view);
    	mImageView.setSurfaceTextureListener(new CanvasListener());
    	camera_texture_view_wrappter = (RelativeLayout) view.findViewById(R.id.camera_texture_view_wrappter);
    	camera_image_view_wrappter = (RelativeLayout) view.findViewById(R.id.camera_image_view_wrappter);
    	camera_photo_view_wrappter = (RelativeLayout) view.findViewById(R.id.camera_photo_view_wrappter);
    	btn_image_mode1 = (Button) view.findViewById(R.id.btn_image_mode1);
    	btn_image_mode1.setOnClickListener(this);
    	btn_image_mode2 = (Button) view.findViewById(R.id.btn_image_mode2);
    	btn_image_mode2.setOnClickListener(this);
    	btn_image_mode3 = (Button) view.findViewById(R.id.btn_image_mode3);
    	btn_image_mode3.setOnClickListener(this);
    	btn_image_mode4 = (Button) view.findViewById(R.id.btn_image_mode4);
    	btn_image_mode4.setOnClickListener(this);
    	btn_image_save = (Button) view.findViewById(R.id.btn_image_save);
    	btn_image_save.setOnClickListener(this);
    	btn_photo_save = (Button) view.findViewById(R.id.btn_photo_save);
    	btn_photo_save.setOnClickListener(this);
    	ll_image_mode = (LinearLayout) view.findViewById(R.id.ll_image_mode);
    	slideUpFragment();
    	
    	return view;
    }
    
    public Surface getSurface() {
    	return mSurface;
    }
    
    public TextureView getTextureView() {
    	return mTextureView;
    }
    
    public Bitmap getProcessedImage() {
    	return mImageView.getBitmap();
    }
    
	public static CameraFragment create() {
		return new CameraFragment();
	}

	@Override
	protected void enableEditText() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void disableEditText() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
		mSurfaceTexture = surface;
    	mSurface = new Surface(mSurfaceTexture);
    	startMs = System.currentTimeMillis();
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
		
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
		if (camMode != null && camMode.equals(CAMERAMODE.FLYING)) {
			if ( (System.currentTimeMillis() - startMs) > 50 ) {
				video = mTextureView.getBitmap(imageWidth, imageHeight);
				startMs = System.currentTimeMillis();
			}
		}
	}
	
	public void setCameraFullScreen() {
		ViewGroup.LayoutParams params = camera_texture_view_wrappter.getLayoutParams();
		params.width = 1040;
		params.height = 500;
		camera_texture_view_wrappter.requestLayout();
	}
	
	public void setCameraMidScreen() {
		ViewGroup.LayoutParams params = camera_texture_view_wrappter.getLayoutParams();
		params.width = 500;
		params.height = 290;
		camera_texture_view_wrappter.requestLayout();
	}
	
	public void setCameraSmallScreen() {
		ViewGroup.LayoutParams params = camera_texture_view_wrappter.getLayoutParams();
		params.width = 340;
		params.height = 200;
		camera_texture_view_wrappter.requestLayout();
	}
	
	public void setFrameColor(String color) {
		//ViewGroup.LayoutParams params = camera_texture_view_wrappter.getLayoutParams();
		camera_texture_view_wrappter.setBackgroundColor(Color.parseColor(color));
		camera_image_view_wrappter.setBackgroundColor(Color.parseColor(color));
		camera_photo_view_wrappter.setBackgroundColor(Color.parseColor(color));
	}

	@Override
	public void onClick(View v) {
		Drawable mode_on= getActivity().getResources().getDrawable(R.drawable.btn_mode_on);
		mode_on.setBounds(0, 0, 40, 40);
		Drawable mode_off= getActivity().getResources().getDrawable(R.drawable.btn_mode_off);
		mode_off.setBounds(0, 0, 40, 40);
		switch (v.getId()) {
		case R.id.btn_image_mode1:
			imgMode = IMAGEMODE.CANNY;
			btn_image_mode1.setCompoundDrawables(mode_on, null, null, null);
			btn_image_mode2.setCompoundDrawables(mode_off, null, null, null);
			btn_image_mode3.setCompoundDrawables(mode_off, null, null, null);
			btn_image_mode4.setCompoundDrawables(mode_off, null, null, null);
			break;
		case R.id.btn_image_mode2:
			imgMode = IMAGEMODE.HOUGHCIRCLES;
			btn_image_mode1.setCompoundDrawables(mode_off, null, null, null);
			btn_image_mode2.setCompoundDrawables(mode_on, null, null, null);
			btn_image_mode3.setCompoundDrawables(mode_off, null, null, null);
			btn_image_mode4.setCompoundDrawables(mode_off, null, null, null);
			break;
		case R.id.btn_image_mode3:
			imgMode = IMAGEMODE.REDHOUGHCIRCLES;
			btn_image_mode1.setCompoundDrawables(mode_off, null, null, null);
			btn_image_mode2.setCompoundDrawables(mode_off, null, null, null);
			btn_image_mode3.setCompoundDrawables(mode_on, null, null, null);
			btn_image_mode4.setCompoundDrawables(mode_off, null, null, null);
			break;
		case R.id.btn_image_mode4:
			imgMode = IMAGEMODE.REDTHRESHOLD;
			btn_image_mode1.setCompoundDrawables(mode_off, null, null, null);
			btn_image_mode2.setCompoundDrawables(mode_off, null, null, null);
			btn_image_mode3.setCompoundDrawables(mode_off, null, null, null);
			btn_image_mode4.setCompoundDrawables(mode_on, null, null, null);
			break;
		case R.id.btn_image_save:
			if (mCameraFragmentButtonListener != null)
				mCameraFragmentButtonListener.saveProcessedImage();
			break;
		case R.id.btn_photo_save:
			if (mCameraFragmentButtonListener != null)
				mCameraFragmentButtonListener.saveOriginalImage();
				
		}
	}
	
	
	private class CanvasListener implements SurfaceTextureListener {
		@Override
		public void onSurfaceTextureAvailable(SurfaceTexture surface,
				int width, int height) {
			imageWidth = width;
			imageHeight = height;
			Log.d(TAG, "onSurfaceTextureAvailable");
			mThread = new RenderThread();
			mThread.start();
		}

		@Override
		public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
			Log.d(TAG, "onSurfaceTextureDestroyed");
			if (mThread != null) {
				mThread.stopRendering();
			}
			return true;
		}

		@Override
		public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
				int width, int height) {
			imageWidth = width;
			imageHeight = height;
			Log.d(TAG, "onSurfaceTextureSizeChanged");
		}

		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture surface) {
			//Log.d(TAG, "onSurfaceTextureUpdated");
		}
		
	}
	
	
	
	private class RenderThread extends Thread {
		private volatile boolean mRunning = true;
		private long prevMS = 0;

		@Override
		public void run() {
			while (mRunning && !Thread.interrupted()) {
				if ( (System.currentTimeMillis() - prevMS) > 50 ) {
					if (video != null && !video.isRecycled()) {
						final Canvas canvas = mImageView.lockCanvas(null);
						try {
							if (imgMode.equals(IMAGEMODE.CANNY)) {
								// initailize matrix
								Mat sourceMat = new Mat(imageWidth, imageHeight, CvType.CV_8UC1);
								Mat cannyMat = new Mat(imageWidth, imageHeight, CvType.CV_8UC4);
								
								Utils.bitmapToMat(video, sourceMat);
								
								Imgproc.cvtColor(sourceMat, sourceMat, Imgproc.COLOR_RGB2GRAY);
							    Imgproc.Canny(sourceMat, cannyMat, 80, 100);
							    Imgproc.cvtColor(cannyMat, sourceMat, Imgproc.COLOR_GRAY2RGBA, 4);
							    Utils.matToBitmap(sourceMat, video);
							    
							    sourceMat.release();
							    cannyMat.release();
							    
							} else if (imgMode.equals(IMAGEMODE.HOUGHCIRCLES)) {
								Mat sourceMat = new Mat(imageWidth, imageHeight, CvType.CV_8UC1);
								Mat thresholdMat = new Mat(imageWidth, imageHeight, CvType.CV_8UC1);
								
								
								Utils.bitmapToMat(video, sourceMat);
								Imgproc.cvtColor(sourceMat, thresholdMat, Imgproc.COLOR_RGB2GRAY, 4);
					            Imgproc.GaussianBlur(thresholdMat, thresholdMat, new Size(9, 9), 2, 2);
					            
					            Mat circles = new Mat();
								
					            int iCannyUpperThreshold = 40;
					            int iMinRadius = 5;
					            int iMaxRadius = 400;
					            int iAccumulator = 100;
					            int iLineThickness = 2;
					            
					            Imgproc.HoughCircles(thresholdMat, circles, Imgproc.CV_HOUGH_GRADIENT, 
					            		2.0, thresholdMat.rows() / 4, iCannyUpperThreshold, iAccumulator, 
					            		iMinRadius, iMaxRadius);
					            
					            if (circles.cols() > 0){
					            	for(int i=0; i<circles.cols(); i++){
					            		double vCircle[] = circles.get(0,i);
					            		if (vCircle == null)	break;
					
										pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
										int radius = (int)Math.round(vCircle[2]);
										
										// draw the found circle
										Core.circle(thresholdMat, pt, radius, new Scalar(255,0,0), iLineThickness);
										Core.circle(thresholdMat, pt, 2, new Scalar(255,0,0), iLineThickness);
					            	}
					            }
					            Utils.matToBitmap(thresholdMat, video);
					            sourceMat.release();
					            thresholdMat.release();
					            circles.release();
								
							} else if (imgMode.equals(IMAGEMODE.REDHOUGHCIRCLES)) {
								Mat sourceMat = new Mat(imageWidth, imageHeight, CvType.CV_8UC1);
								Mat thresholdMat = new Mat(imageWidth, imageHeight, CvType.CV_8UC1);
								Mat hsvMat = new Mat(width, height, CvType.CV_8UC1);
								Utils.bitmapToMat(video, sourceMat);
								Imgproc.cvtColor(sourceMat, hsvMat, Imgproc.COLOR_RGB2HSV, 4);
								
								Core.inRange(hsvMat, new Scalar(-2, 80, 80), new Scalar(3, 255, 255), thresholdMat); // for blue color
					            Imgproc.GaussianBlur(thresholdMat, thresholdMat, new Size(5, 5), 2, 2);
					            
					            Mat circles = new Mat();
								
					            int iCannyUpperThreshold = 30;
					            int iMinRadius = 5;
					            int iMaxRadius = 400;
					            int iAccumulator = 50;
					            int iLineThickness = 2;
					            
					            Imgproc.HoughCircles(thresholdMat, circles, Imgproc.CV_HOUGH_GRADIENT, 
					            		2.0, thresholdMat.rows() / 8, iCannyUpperThreshold, iAccumulator, 
					            		iMinRadius, iMaxRadius);
					            
					            
					            
					            if (circles.cols() > 0){
					            	for(int i=0; i<circles.cols(); i++){
					            		double vCircle[] = circles.get(0,i);
					            		if (vCircle == null)	break;
					
										pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
										int radius = (int)Math.round(vCircle[2]);
										
										// draw the found circle
										Core.circle(hsvMat, pt, radius, new Scalar(0,255,255), iLineThickness);
										Core.circle(hsvMat, pt, 2, new Scalar(0,255,255), iLineThickness);
					            	}
					            }
					            Imgproc.cvtColor(hsvMat, sourceMat, Imgproc.COLOR_HSV2RGB, 0);
								Utils.matToBitmap(sourceMat, video);
								
								sourceMat.release();
								thresholdMat.release();
								hsvMat.release();
								circles.release();
								
							} else if (imgMode.equals(IMAGEMODE.REDTHRESHOLD)) {
								Mat sourceMat = new Mat(imageWidth, imageHeight, CvType.CV_8UC1);
								Mat thresholdMat = new Mat(imageWidth, imageHeight, CvType.CV_8UC1);
								Mat hsvMat = new Mat(width, height, CvType.CV_8UC1);
								
								Utils.bitmapToMat(video, sourceMat);
								Imgproc.cvtColor(sourceMat, hsvMat, Imgproc.COLOR_RGB2HSV, 4);
								
								Core.inRange(hsvMat, new Scalar(-2, 80, 80), new Scalar(3, 255, 255), thresholdMat); // for blue color
					            Imgproc.GaussianBlur(thresholdMat, thresholdMat, new Size(5, 5), 2, 2);
					            
					            Mat circles = new Mat();
								
					            int iCannyUpperThreshold = 30;
					            int iMinRadius = 5;
					            int iMaxRadius = 400;
					            int iAccumulator = 50;
					            int iLineThickness = 2;
					            
					            Imgproc.HoughCircles(thresholdMat, circles, Imgproc.CV_HOUGH_GRADIENT, 
					            		2.0, thresholdMat.rows() / 8, iCannyUpperThreshold, iAccumulator, 
					            		iMinRadius, iMaxRadius);
					            
					            
					            
					            if (circles.cols() > 0){
					            	for(int i=0; i<circles.cols(); i++){
					            		double vCircle[] = circles.get(0,i);
					            		if (vCircle == null)	break;
					
										pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
										int radius = (int)Math.round(vCircle[2]);
										
										// draw the found circle
										Core.circle(thresholdMat, pt, radius, new Scalar(255,0,0), iLineThickness);
										Core.circle(thresholdMat, pt, 2, new Scalar(255,0,0), iLineThickness);
					            	}
					            }
					            Imgproc.cvtColor(thresholdMat, sourceMat, Imgproc.COLOR_GRAY2BGR, 0);
								Utils.matToBitmap(sourceMat, video);
								
								sourceMat.release();
								thresholdMat.release();
								hsvMat.release();
								circles.release();
							}
							canvas.drawBitmap(video, 0, 0, null);
						} finally {
							if (video != null)	video.recycle();
							mImageView.unlockCanvasAndPost(canvas);
						}
					}
					
					prevMS = System.currentTimeMillis();
				} else {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		public void stopRendering() {
			interrupt();
			mRunning = false;
		}

	}



	@Override
	public void resetFragment() {
		// reset Image Processing Mode as Canny
		imgMode = IMAGEMODE.CANNY;
		Drawable mode_on= getActivity().getResources().getDrawable(R.drawable.btn_mode_on);
		mode_on.setBounds(0, 0, 40, 40);
		Drawable mode_off= getActivity().getResources().getDrawable(R.drawable.btn_mode_off);
		mode_off.setBounds(0, 0, 40, 40);
		btn_image_mode1.setCompoundDrawables(mode_on, null, null, null);
		btn_image_mode2.setCompoundDrawables(mode_off, null, null, null);
		btn_image_mode3.setCompoundDrawables(mode_off, null, null, null);
		btn_image_mode4.setCompoundDrawables(mode_off, null, null, null);
	}

	@Override
	public void updateCameraFragmentButtonListener(
			CameraFragmentButtonListener mCameraFragmentButtonListener) {
		this.mCameraFragmentButtonListener = mCameraFragmentButtonListener; 
	}
}
