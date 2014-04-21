package com.felicekarl.foragingtech.presenters;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.felicekarl.ardrone.ARDrone;
import com.felicekarl.ardrone.ARDroneConstants;
import com.felicekarl.ardrone.ARDroneInterface.ARDroneCameraMode;
import com.felicekarl.ardrone.managers.navdata.DroneState;
import com.felicekarl.ardrone.managers.navdata.listeners.AttitudeListener;
import com.felicekarl.ardrone.managers.navdata.listeners.GpsListener;
import com.felicekarl.ardrone.managers.navdata.listeners.StateListener;
import com.felicekarl.foragingtech.ForagingTechConstraint;
import com.felicekarl.foragingtech.activities.MainActivity;
import com.felicekarl.foragingtech.listeners.*;
import com.felicekarl.foragingtech.models.IModel;
import com.felicekarl.foragingtech.views.IView;
import com.felicekarl.foragingtech.views.IView.TypeView;
import com.felicekarl.foragingtech.views.fragments.ControllerNavigatingFragment.NAVIGATINGMODE;
import com.nutiteq.components.MapPos;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

public class MainPresenter implements Runnable {
	private static final String TAG = MainPresenter.class.getSimpleName();
	
	
	private static final int SPLASH_TIME = 2000;
	private int timeElapsed = 0;
	
	private Context context;
	private IView view;
	private IModel model;
	private Thread thread;
	private Thread navThread;
	
	private ARDrone mARDrone;
	private int droneSpeedX;
	private int droneSpeedY;
	private int droneSpeedZ;
	private int droneSpeedSpin;
	private double droneYaw;
	private int droneAlt;
	
	private List<MapPos> path;
	private int curPathIndex;
	

	public MainPresenter(Context context, IView view, IModel model){
		this.context = context;
		this.view = view;
		this.model = model;
		
		droneSpeedX = 0;
		droneSpeedY = 0;
		droneSpeedZ = 0;
		droneSpeedSpin = 0;
		
		initListeners();
		
		/* splash fragment timer */
		thread = new Thread(this);
        thread.start();
	}
	
	
	
	private void initListeners() {
		/* add flip page from menu to content */
		view.updateFlipForwardButtonListener(new FlipForwardButtonListener() {
			@Override
			public void flip(int pageNumber) {
				if (pageNumber == 0) {
					initDrone();
					if(mARDrone.isConnected()) {
						Toast.makeText(context, "Drone is successfully connected.", Toast.LENGTH_SHORT).show();
						view.setView(TypeView.FLYINGMODE);
					} else {
						Toast.makeText(context, "Fail to connect Drone. Check the Wi-Fi again.", Toast.LENGTH_SHORT).show();
					}
				} else if (pageNumber == 1) {
					if (view.getCurMapFile() != null) {
						Log.d(TAG, "view.getCurMapFile(): " + view.getCurMapFile());
						//view.changeMap(view.getCurMapFile());
						initDrone();
						if(mARDrone.isConnected()) {
							Toast.makeText(context, "Drone is successfully connected.", Toast.LENGTH_SHORT).show();
							view.setView(TypeView.NAVIGATINGMODE);
						} else {
							Toast.makeText(context, "Fail to connect Drone. Check the Wi-Fi again.", Toast.LENGTH_SHORT).show();
						}
					} else {
						Toast.makeText(context, "Map is not selected. Please select the .map file.", Toast.LENGTH_SHORT).show();
					}
					
				} else if (pageNumber == 2) {
					
				}
			}
		});
		
		/* add flying mode actionbar framgment button listener */
		view.updateContentActionBarFragmentButtonListener(new ContentActionBarFragmentButtonListener() {
			@Override
			public void takePhoto() {
				Log.d(TAG, "takePhoto()");
				BufferedOutputStream bos = null;
				FileOutputStream fos = null;
				try {
					File dir = new File(ForagingTechConstraint.defaultPhotoPath);
					if(!dir.isDirectory()){
						dir.mkdirs();
					}
					String filename = ForagingTechConstraint.PT + "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
					fos = new FileOutputStream(ForagingTechConstraint.defaultPhotoPath + filename + ".jpg");
		        	bos = new BufferedOutputStream(fos);	
					Bitmap bmp = view.getCameraBitmap();
					bmp.compress(Bitmap.CompressFormat.JPEG, 90, bos);
		            bmp.recycle();
		            if (bos != null) {
		        		bos.close();
		        	}
		        	if (fos != null) {
		        		fos.flush();
						fos.close();
		        	}
		        	Toast.makeText(context, filename + ".jpg" + " is successfully saved.", Toast.LENGTH_SHORT).show();
				} catch (IOException e) {
					Toast.makeText(context, "Error occurs while saving Photo.", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			}
			
			@Override
			public void backToMenu() {
				if(mARDrone.isConnected())
					mARDrone.disconnect();
				view.setView(TypeView.MENU);
			}

			@Override
			public void emergency() {
				if(mARDrone.isConnected() && !mARDrone.isEmergency()) {
					Log.d(TAG, "mode 1");
					mARDrone.reset();
					mARDrone.setIsEmergency(true);
					view.setIsEmergency(true);
				} else if(mARDrone.isConnected() && mARDrone.isEmergency()) {
					Log.d(TAG, "mode 2");
					mARDrone.reset();
					mARDrone.setIsEmergency(false);
					view.setIsEmergency(false);
				} 
					
			}

			@Override
			public void toggleCameraMode() {
				Log.d(TAG, "toggleCameraMode()");
				if (mARDrone.getCameraMode().equals(ARDroneCameraMode.FRONT_CAMERA)) {
					Log.d(TAG, "toggleCameraMode1()");
					mARDrone.setCameraMode(ARDroneCameraMode.BOTTOM_CAMERA);
					mARDrone.toggleCamera();
				} else if (mARDrone.getCameraMode().equals(ARDroneCameraMode.BOTTOM_CAMERA)) {
					Log.d(TAG, "toggleCameraMode2()");
					mARDrone.setCameraMode(ARDroneCameraMode.FRONT_CAMERA);
					mARDrone.setHorizontalCamera();
				}
			}
		});
		
		/* joystick listener */
		view.updateControllerListener(new ControllerListener() {
			@Override
			public void setSpeedZ(int speedZ) {
				droneSpeedZ = speedZ;
				if(mARDrone.isConnected() && mARDrone.isFlying()) {
					//Log.d(TAG, "droneSpeedZ: " + droneSpeedZ);
					mARDrone.move3D(droneSpeedX, droneSpeedY, droneSpeedZ, droneSpeedSpin);
				}
			}
			
			@Override
			public void setSpeedY(int speedY) {
				droneSpeedX = speedY;
				if(mARDrone.isConnected() && mARDrone.isFlying()) {
					//Log.d(TAG, "droneSpeedX: " + droneSpeedX);
					mARDrone.move3D(droneSpeedX, droneSpeedY, droneSpeedZ, droneSpeedSpin);
				}
			}
			
			@Override
			public void setSpeedX(int speedX) {
				droneSpeedY = speedX;
				if(mARDrone.isConnected() && mARDrone.isFlying()) {
					//Log.d(TAG, "droneSpeedY: " + droneSpeedY);
					mARDrone.move3D(droneSpeedX, droneSpeedY, droneSpeedZ, droneSpeedSpin);
				}
			}
			
			@Override
			public void setSpeedSpin(int speedSpin) {
				droneSpeedSpin = speedSpin;
				if(mARDrone.isConnected() && mARDrone.isFlying()) {
					//Log.d(TAG, "droneSpeedSpin: " + droneSpeedSpin);
					mARDrone.move3D(droneSpeedX, droneSpeedY, droneSpeedZ, droneSpeedSpin);
				}
			}

			@Override
			public void takeOff() {
				if (mARDrone.isConnected() && !mARDrone.isEmergency()) {
					mARDrone.takeOff();
				} else {
					Toast.makeText(context, "Calibration is needed.", Toast.LENGTH_SHORT).show();
				}
			}

			@Override
			public void landing() {
				if (mARDrone.isConnected()) {
					mARDrone.landing();
				}
			}
		});
		
		/* camera fragment button listener */
		view.updateCameraFragmentButtonListener(new CameraFragmentButtonListener() {
			@Override
			public void saveProcessedImage() {
				Log.d(TAG, "saveImage()");
				BufferedOutputStream bos = null;
				FileOutputStream fos = null;
				try {
					File dir = new File(ForagingTechConstraint.defaultPhotoPath);
					if(!dir.isDirectory()){
						dir.mkdirs();
					}
					String filename = ForagingTechConstraint.IG + "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
					fos = new FileOutputStream(ForagingTechConstraint.defaultPhotoPath + filename + ".jpg");
		        	bos = new BufferedOutputStream(fos);	
					Bitmap bmp = view.getImageBitmap();
					bmp.compress(Bitmap.CompressFormat.JPEG, 90, bos);
		            bmp.recycle();
		            if (bos != null) {
		        		bos.close();
		        	}
		        	if (fos != null) {
		        		fos.flush();
						fos.close();
		        	}
		        	Toast.makeText(context, filename + ".jpg" + " is successfully saved.", Toast.LENGTH_SHORT).show();
				} catch (IOException e) {
					Toast.makeText(context, "Error occurs while saving Image.", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			}

			@Override
			public void saveOriginalImage() {
				Log.d(TAG, "takePhoto()");
				BufferedOutputStream bos = null;
				FileOutputStream fos = null;
				try {
					File dir = new File(ForagingTechConstraint.defaultPhotoPath);
					if(!dir.isDirectory()){
						dir.mkdirs();
					}
					String filename = ForagingTechConstraint.PT + "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
					fos = new FileOutputStream(ForagingTechConstraint.defaultPhotoPath + filename + ".jpg");
		        	bos = new BufferedOutputStream(fos);	
					Bitmap bmp = view.getCameraBitmap();
					bmp.compress(Bitmap.CompressFormat.JPEG, 90, bos);
		            bmp.recycle();
		            if (bos != null) {
		        		bos.close();
		        	}
		        	if (fos != null) {
		        		fos.flush();
						fos.close();
		        	}
		        	Toast.makeText(context, filename + ".jpg" + " is successfully saved.", Toast.LENGTH_SHORT).show();
				} catch (IOException e) {
					Toast.makeText(context, "Error occurs while saving Photo.", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			}
		});
		
		/* add controller navigating fragment */
		view.updateControllerNavigatingFragmentButtonListener(new ControllerNavigatingFragmentButtonListener() {
			@Override
			public void toggleTakeOffLanding() {
				if (mARDrone.isConnected() && !mARDrone.isFlying()) {
					mARDrone.takeOff();
					mARDrone.setIsFlying(true);
					view.setIsFlying(true);
				} else if (mARDrone.isConnected() && mARDrone.isFlying()) {
					mARDrone.landing();
					mARDrone.setIsFlying(false);
					view.setIsFlying(false);
					// stop navigating
					view.setNavigatingMode(NAVIGATINGMODE.CONFIGURING);
					mARDrone.setIsNavigating(false);
					if (navThread != null) {
						navThread.interrupt();
					}
				}
			}

			@Override
			public void toggleStartStopNavigating() {
				if (mARDrone.isConnected() && !mARDrone.isFlying()) {
					Toast.makeText(context, "Take Off before start navigating", Toast.LENGTH_SHORT).show();
				} else if (mARDrone.isConnected() && mARDrone.isFlying() && mARDrone.isNavigating()) {
					view.setNavigatingMode(NAVIGATINGMODE.CONFIGURING);
					mARDrone.setIsNavigating(false);
					if (navThread != null) {
						navThread.interrupt();
					}
					
				} else if (mARDrone.isConnected() && mARDrone.isFlying() && !mARDrone.isNavigating()) {
					if (view.getNavigatingMode().equals(NAVIGATINGMODE.CONFIGURED)) {
						path = view.getPath();
						if (path != null) {
//							for(MapPos pos : path) {
//								Log.d(TAG, pos.toString());
//							}
							view.setNavigatingMode(NAVIGATINGMODE.NAVIGATING);
							mARDrone.setIsNavigating(true);
							
							// set first target as index 1
							curPathIndex = 0;
							Log.d("Karl", path.get(curPathIndex).toString());
							
							navThread = new Thread() {
								@Override
								public void run() {
									while (!navThread.isInterrupted()) {
										try {
											if (mARDrone.isConnected() && mARDrone.isFlying() && mARDrone.isNavigating()) {
												MapPos cur = view.getDroneCurPos();
												MapPos target = path.get(curPathIndex);
												double xDiff = target.x - cur.x;
												double yDiff = target.y - cur.y;
												double distance = Math.sqrt(xDiff * xDiff + yDiff * yDiff);
												Log.d("Karl", "distance: " + distance + " | curPathIndex: " + curPathIndex);
												if (distance < 10d) {
													curPathIndex++;
												}
												if (curPathIndex > path.size() - 1) {
													curPathIndex = path.size() - 1;
													mARDrone.move3DNav(0, 0, 0, 0);
													navThread.interrupt();
												}
												double theta = 0;
												if (xDiff > 0 && yDiff > 0) {
													theta = Math.atan2(xDiff, yDiff);
												} else if (xDiff > 0 && yDiff <0) {
													theta = Math.atan2(-yDiff, xDiff) + (Math.PI / 2);
												} else if (xDiff < 0 && yDiff > 0) {
													theta = -1 * Math.atan2(-xDiff, yDiff);
												} else if (xDiff < 0 && yDiff < 0) {
													theta = -1 * (Math.atan2(-yDiff, -xDiff) + (Math.PI / 2));
												}
												theta = Math.toDegrees(theta);
												Log.d(TAG, String.format("xDiff: %3.5f, yDiff: %3.5f, theta: %3.5f", xDiff, yDiff, theta));
												double vLength = Math.sqrt(xDiff*xDiff + yDiff * yDiff);
												float speedXY = 1.5f;
												int speedSpin = 0;
												int MIN_HEIGHT = 2500;
												int speedZ = 0;
												
												if (droneYaw > 0 && theta > 0) {
													if (droneYaw < theta) {
														speedSpin = (int) (-1 * speedXY * Math.abs(droneYaw - theta));
														//mDebugFragment.setDebugMsg(14, String.format("turn right"));
													} else if (droneYaw > theta) {
														speedSpin = (int) (speedXY * Math.abs(droneYaw - theta));
														//mDebugFragment.setDebugMsg(14, String.format("turn left"));
													}
												} else if (droneYaw < 0 && theta < 0) {
													if (droneYaw < theta) {
														speedSpin = (int) (-1 * speedXY * Math.abs(droneYaw - theta));
														//mDebugFragment.setDebugMsg(14, String.format("turn right"));
													} else if (droneYaw > theta) {
														speedSpin = (int) (speedXY * Math.abs(droneYaw - theta));
														//mDebugFragment.setDebugMsg(14, String.format("turn left"));
													}
												} else if (droneYaw < 0 && theta > 0) {
													speedSpin = (int) (-1 * speedXY * Math.abs(droneYaw - theta));
													//mDebugFragment.setDebugMsg(14, String.format("turn right"));
												} else if (droneYaw > 0 && theta < 0) {
													speedSpin = (int) (speedXY * Math.abs(droneYaw - theta));
													//mDebugFragment.setDebugMsg(14, String.format("turn left"));
												}
												
												if (droneAlt < MIN_HEIGHT) {
													speedZ = (int) (-1 * (MIN_HEIGHT - droneAlt) * 0.8);
												} else {
													speedZ = (int) (-1 * (MIN_HEIGHT - droneAlt) * 0.8);
												}
												if (Math.abs(droneYaw - theta) > 15) {
													mARDrone.move3DNav(0, 0, speedZ, (int) (speedSpin * 1) );
												} else {
													mARDrone.move3DNav((int) (speedXY * vLength * 0.7), 0, speedZ, (int) (speedSpin * 1));
												}
											}
											sleep(100);
										} catch (InterruptedException e) {
											Thread.currentThread().interrupt();
											e.printStackTrace();
										}
									}
								}
							};
							
							navThread.start();
						}
						
					} else if (view.getNavigatingMode().equals(NAVIGATINGMODE.CONFIGURING)) {
						Toast.makeText(context, "Press Configuring Button before start navigating", Toast.LENGTH_SHORT).show();
					}
				}
				
			}
		});
		
		
		
	}
	
	
	@Override
	public void run() {
		try {
			while (timeElapsed < SPLASH_TIME) {
				Thread.sleep(100);
				timeElapsed += 100;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			view.setView(TypeView.MENU);
		}
	}
	
	public boolean initDrone() {
		// TODO: Change the thread as Async Thread
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		/* connect to drone and initialize it. */
		mARDrone = new ARDrone(ARDroneConstants.IP_ADDRESS);
		boolean isConnected = mARDrone.connect();
		if (isConnected) Log.d(TAG, "CommandManager is created.");
		if (!isConnected) return false;
		boolean isConnectedNav = mARDrone.connectNav();
		if (isConnectedNav) Log.d(TAG, "NavDataManager is created.");
		if (!isConnectedNav) return false;
		boolean isConnectedVideo = mARDrone.connectVideo(view.getCameraSurface());
		if (isConnectedVideo) Log.d(TAG, "VideoManager is created.");
		if (!isConnectedVideo) return false;
		
		
		/* Add Drone State Listener */
		mARDrone.updateStateListener(new StateListener() {
			@Override
			public void stateChanged(DroneState state) {
				if(state.isTagOn(ARDroneConstants.FLY_MASK)) {
					//Log.d(TAG, "isFlying");
					view.setIsFlying(true);
					mARDrone.setIsFlying(true);
					//mGLView.setIsFlying(true);
//					if (mActionBarFragment != null) {
//						mActionBarFragment.setIsFlying(true);
//						isFlying = true;
//					}
				} else {
					//mGLView.setIsFlying(false);
					//Log.d(TAG, "isNotFlying");
					view.setIsFlying(false);
					mARDrone.setIsFlying(false);
//					if (mActionBarFragment != null) {
//						mActionBarFragment.setIsFlying(false);
//						isFlying = false;
//					}
				}
				
				if(state.isTagOn(ARDroneConstants.NAVDATA_DEMO)) {
					//Log.d(TAG, "ARDroneConstants.NAVDATA_DEMO");
				}
				
				if(state.isTagOn(ARDroneConstants.NAVDATA_BOOTSTRAP)) {
					//Log.d(TAG, "ARDroneConstants.NAVDATA_DEMO");
				}
				
				if(state.isTagOn(ARDroneConstants.MAGNETOMETER)) {
					Log.d(TAG, "MAGNETO CALIBRATION IS NEEDED");
				}
			}
		});
		
		/* Add Drone Attitude Listener */
		mARDrone.updateAttitudeListener(new AttitudeListener() {
			@Override
			public void attitudeUpdated(float pitch, float roll, float yaw, int altitude) {
				droneYaw = yaw;
				droneAlt = altitude;
			}
		});
		
		/* add GPS Listener */
		mARDrone.updateGpsListener(new GpsListener() {
			@Override
			public void gpsLocChanged(double lat, double lon, double elevation,
					double hdop, int data_available, double lat0, double lon0,
					double lat_fuse, double lon_fuse, long gps_state, double vdop,
					double pdop, float speed, long last_frame_timestamp, float degree,
					float degree_mag) {
				//Log.d(TAG, "lat: " + lat + " | lon: " + lon);
				view.setDroneCurPos(lat, lon);
				view.updateUserCurPos();
			}
		});
		
		mARDrone.start();
		Log.d(TAG, "ARDrone listeners are registered.");
		return true;
	}

}
