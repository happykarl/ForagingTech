package com.felicekarl.foragingtech.views;

import com.felicekarl.foragingtech.R;
import com.felicekarl.foragingtech.activities.MainActivity;
import com.felicekarl.foragingtech.listeners.*;
import com.felicekarl.foragingtech.views.fragments.*;
import com.felicekarl.foragingtech.views.fragments.BaseFragment.DIRECTION;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.Surface;

public class MainView implements IView {
	private static final String TAG = MainView.class.getSimpleName();
	
	private Context context;
	private TypeView curTypeView;
	
	private FragmentManager mFragmentManager;
	private SplashFragment mSplashFragment;
	private PagerFragment mPagerFragment;
	private ContentFlyingFragment mContentFlyingFragment;
	private ContentNavigatingFragment mContentNavigatingFragment;
	private CameraFragment mCameraFragment;
	private ControllerFragment mControllerFragment;
	
	public MainView(Context context) {
		this.context = context;
		this.mFragmentManager = ((MainActivity) context).getFragmentManager();
		/* Initialize curTypeView as Splash Screen */
		curTypeView = TypeView.SPLASH;
		initFragments();
	}
	
	/**
	 * initialize fragments and add on the fragment manager.
	 */
	private void initFragments() {
		/* add pager screen fragment */
		mPagerFragment = new PagerFragment();
		mFragmentManager.beginTransaction().add(R.id.main, mPagerFragment).commit();
		/* add splash screen fragment */
		mSplashFragment = SplashFragment.create();
		mFragmentManager.beginTransaction().add(R.id.main, mSplashFragment).commit();
		/* add content flying mode fragment */
		mContentFlyingFragment = ContentFlyingFragment.create();
		mFragmentManager.beginTransaction().add(R.id.main, mContentFlyingFragment).commit();
		/* add content navigating mode fragment */
		mContentNavigatingFragment = ContentNavigatingFragment.create();
		mFragmentManager.beginTransaction().add(R.id.main, mContentNavigatingFragment).commit();
		/* add camera fragment */
		mCameraFragment = CameraFragment.create();
		mFragmentManager.beginTransaction().add(R.id.main, mCameraFragment).commit();
		/* add controller fragment */
		mControllerFragment = ControllerFragment.create();
		mFragmentManager.beginTransaction().add(R.id.main, mControllerFragment).commit();
	}

	@Override
	public void setView(TypeView type) {
		if(curTypeView.equals(TypeView.SPLASH) && type.equals(TypeView.MENU)) {
			curTypeView = TypeView.MENU;
			// pager (menu) fragment moves on the stage with slide up animation
			mPagerFragment.toggle(true, true, DIRECTION.TOP);
			// splash fragment moves off the stage with slide up animation
			mSplashFragment.toggle(false, true, DIRECTION.BOTTOM);
			mContentFlyingFragment.toggle(false, false, DIRECTION.TOP);
			mContentNavigatingFragment.toggle(false, false, DIRECTION.TOP);
			mCameraFragment.toggle(false, false, DIRECTION.TOP);
			mControllerFragment.toggle(false, false, DIRECTION.TOP);
		} else if(curTypeView.equals(TypeView.MENU) && type.equals(TypeView.FLYINGMODE)) {
			curTypeView = TypeView.FLYINGMODE;
			mCameraFragment.setCameraFullScreen();
			mCameraFragment.setFrameColor("#f0a30a");
			mCameraFragment.resetFragment();
			
			mPagerFragment.toggle(false, true, DIRECTION.BOTTOM);
			mSplashFragment.toggle(false, false, DIRECTION.TOP);
			mContentFlyingFragment.toggle(true, true, DIRECTION.TOP);
			mContentNavigatingFragment.toggle(false, false, DIRECTION.TOP);
			mCameraFragment.toggle(true, true, DIRECTION.TOP);
			mControllerFragment.toggle(true, true, DIRECTION.TOP);
			
			
		} else if(curTypeView.equals(TypeView.MENU) && type.equals(TypeView.NAVIGATINGMODE)) {
			curTypeView = TypeView.NAVIGATINGMODE;
			mCameraFragment.setCameraSmallScreen();
			mCameraFragment.setFrameColor("#6a00ff");
			
			mPagerFragment.toggle(false, true, DIRECTION.BOTTOM);
			mSplashFragment.toggle(false, false, DIRECTION.TOP);
			mContentFlyingFragment.toggle(false, false, DIRECTION.TOP);
			mContentNavigatingFragment.toggle(true, true, DIRECTION.TOP);
			mCameraFragment.toggle(true, true, DIRECTION.TOP);
			mControllerFragment.toggle(false, false, DIRECTION.TOP);
		} else if(curTypeView.equals(TypeView.FLYINGMODE) && type.equals(TypeView.MENU)) {
			curTypeView = TypeView.MENU;
			mPagerFragment.toggle(true, true, DIRECTION.BOTTOM);
			mSplashFragment.toggle(false, false, DIRECTION.TOP);
			mContentFlyingFragment.toggle(false, true, DIRECTION.TOP);
			mContentNavigatingFragment.toggle(false, false, DIRECTION.TOP);
			mCameraFragment.toggle(false, true, DIRECTION.TOP);
			mControllerFragment.toggle(false, false, DIRECTION.TOP);
		} else if(curTypeView.equals(TypeView.NAVIGATINGMODE) && type.equals(TypeView.MENU)) {
			curTypeView = TypeView.MENU;
			mPagerFragment.toggle(true, true, DIRECTION.BOTTOM);
			mSplashFragment.toggle(false, false, DIRECTION.TOP);
			mContentFlyingFragment.toggle(false, false, DIRECTION.TOP);
			mContentNavigatingFragment.toggle(false, true, DIRECTION.TOP);
			mCameraFragment.toggle(false, true, DIRECTION.TOP);
			mControllerFragment.toggle(false, false, DIRECTION.TOP);
		}
	}
	

	@Override
	public void updateFlipForwardButtonListener(FlipForwardButtonListener mFlipForwardButtonListener) {
		mPagerFragment.updateFlipForwardButtonListener(mFlipForwardButtonListener);
	}
	
	@Override
	public Surface getCameraSurface() {
		if (mCameraFragment != null)	return mCameraFragment.getSurface();
		return null;
	}

	@Override
	public Bitmap getCameraBitmap() {
		if (mCameraFragment != null)	return mCameraFragment.getTextureView().getBitmap();
		return null;
	}
	
	@Override
	public Bitmap getImageBitmap() {
		if (mCameraFragment != null)	return mCameraFragment.getProcessedImage();
		return null;
	}

	@Override
	public void updateControllerListener(ControllerListener mControllerListener) {
		mControllerFragment.updateControllerListener(mControllerListener);
	}

	@Override
	public void setIsFlying(boolean isFlying) {
		mControllerFragment.setIsFlying(isFlying);
	}

	@Override
	public void updateCameraFragmentButtonListener(CameraFragmentButtonListener mCameraFragmentButtonListener) {
		mCameraFragment.updateCameraFragmentButtonListener(mCameraFragmentButtonListener);
	}

	@Override
	public void updateContentActionBarFragmentButtonListener(
			ContentActionBarFragmentButtonListener mContentActionBarFragmentButtonListener) {
		mContentFlyingFragment.updateContentActionBarFragmentButtonListener(mContentActionBarFragmentButtonListener);
	}
}
