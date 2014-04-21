package com.felicekarl.foragingtech.views;

import java.util.List;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.view.Surface;

import com.felicekarl.foragingtech.listeners.*;
import com.felicekarl.foragingtech.views.fragments.ControllerNavigatingFragment.NAVIGATINGMODE;
import com.nutiteq.components.MapPos;

public interface IView extends UpdateFlipForwardButtonListener, UpdateControllerListener, 
		UpdateCameraFragmentButtonListener, UpdateContentActionBarFragmentButtonListener,
		UpdateControllerNavigatingFragmentButtonListener {
	public void setView(TypeView type);
	public Bitmap getCameraBitmap();
	public Bitmap getImageBitmap();
	public Surface getCameraSurface();
	public void setIsFlying(boolean isFlying);
	public void setIsEmergency(boolean isEmergency);
	
	public NAVIGATINGMODE getNavigatingMode();
	public void setNavigatingMode(NAVIGATINGMODE mode);
	public List<MapPos> getPath();
	
	public String getCurMapFile();
	public void changeMap(String mapFile);
	public void setDroneCurPos(double lat, double lon);
	public MapPos getDroneCurPos();
	public void updateUserCurPos();
	
	public enum TypeView {
		SPLASH, MENU, FLYINGMODE, NAVIGATINGMODE
	}
}
