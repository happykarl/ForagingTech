package com.felicekarl.foragingtech.views.fragments;

import java.io.File;
import java.util.List;

import com.felicekarl.foragingtech.R;
import com.felicekarl.foragingtech.listeners.ConfiguringPathListener;
import com.felicekarl.foragingtech.listeners.ContentActionBarFragmentButtonListener;
import com.felicekarl.foragingtech.listeners.ControllerNavigatingFragmentButtonListener;
import com.felicekarl.foragingtech.listeners.UpdateConfiguringPathListener;
import com.felicekarl.foragingtech.listeners.UpdateContentActionBarFragmentButtonListener;
import com.felicekarl.foragingtech.listeners.UpdateControllerNavigatingFragmentButtonListener;
import com.felicekarl.foragingtech.views.fragments.ControllerNavigatingFragment.NAVIGATINGMODE;
import com.nutiteq.components.MapPos;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class ContentNavigatingFragment extends BaseFragment
		implements UpdateContentActionBarFragmentButtonListener,
		UpdateControllerNavigatingFragmentButtonListener {
	private static final String TAG = ContentNavigatingFragment.class.getSimpleName();
	public static final String ARG_MAP = "map";
	
	private ContentActionBarFragment mContentActionBarFragment;
	private ContentActionBarFragmentButtonListener mContentActionBarFragmentButtonListener;
	private ControllerNavigatingFragmentButtonListener mControllerNavigatingFragmentButtonListener;
	private ControllerNavigatingFragment mControllerNavigatingFragment;
	private MapFragment mapFragment;
	
	/**
	 * SharedPreferences.
	 */
    private SharedPreferences preferences;
    /**
	 * SharedPreferences.Editor.
	 */
    private SharedPreferences.Editor editor;
	
	private List<MapPos> path;
	
	
	public ContentNavigatingFragment() {
    	
    }
	
	public static ContentNavigatingFragment create() {
		return new ContentNavigatingFragment();
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	view = (ViewGroup) inflater.inflate(R.layout.fragment_content_navigating, container, false);
    	/* add map fragment */
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        editor = preferences.edit();
        String mapPath = preferences.getString(ARG_MAP, "");
        File file = new File(mapPath);
        if (file.exists()) {
        	mapFragment = MapFragment.create(mapPath);
        } else {
        	mapFragment = MapFragment.create();
        }
        getChildFragmentManager().beginTransaction().add(R.id.content_container, mapFragment).commit();
    	/* add content actionbar fragment */
    	mContentActionBarFragment = ContentActionBarFragment.create();
    	getChildFragmentManager().beginTransaction().add(R.id.content_container, mContentActionBarFragment).commit();
    	mContentActionBarFragment.updateContentActionBarFragmentButtonListener(mContentActionBarFragmentButtonListener);
    	/* add navigating controller */
    	mControllerNavigatingFragment = ControllerNavigatingFragment.create();
    	getChildFragmentManager().beginTransaction().add(R.id.content_container, mControllerNavigatingFragment).commit();
    	
    	mControllerNavigatingFragment.updateControllerNavigatingFragmentButtonListener(mControllerNavigatingFragmentButtonListener);
    	mControllerNavigatingFragment.updateConfiguringPathListener(new ConfiguringPathListener() {			
			@Override
			public void savePath() {
				if (mControllerNavigatingFragment.getNavigatingMode().equals(NAVIGATINGMODE.CONFIGURING)) {
					path = mapFragment.getPath();
					mControllerNavigatingFragment.setUISetPath();
					Toast.makeText(getActivity(), "Nav Path is configured.", Toast.LENGTH_SHORT).show();
	//				for(MapPos pos : path) {
	//					Log.d(TAG, pos.toString());
	//				}
				} else if (mControllerNavigatingFragment.getNavigatingMode().equals(NAVIGATINGMODE.NAVIGATING)) {
					Toast.makeText(getActivity(), "Stop Navigating to configuring path.", Toast.LENGTH_SHORT).show();
				}
			}
			@Override
			public void resetPath() {
				if (mControllerNavigatingFragment.getNavigatingMode().equals(NAVIGATINGMODE.CONFIGURING)) {
					mapFragment.resetPath();
					mControllerNavigatingFragment.setUIResetPath();
					Toast.makeText(getActivity(), "Nav Path is reset.", Toast.LENGTH_SHORT).show();
				} else if (mControllerNavigatingFragment.getNavigatingMode().equals(NAVIGATINGMODE.NAVIGATING)) {
					Toast.makeText(getActivity(), "Stop Navigating to reset path.", Toast.LENGTH_SHORT).show();
				}
				
			}
		});
    	
    	
    	slideUpFragment();
    	
    	return view;
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
	public void resetFragment() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateContentActionBarFragmentButtonListener(
			ContentActionBarFragmentButtonListener mContentActionBarFragmentButtonListener) {
		this.mContentActionBarFragmentButtonListener = mContentActionBarFragmentButtonListener;
	}

	@Override
	public void updateControllerNavigatingFragmentButtonListener(
			ControllerNavigatingFragmentButtonListener mControllerNavigatingFragmentButtonListener) {
		this.mControllerNavigatingFragmentButtonListener = mControllerNavigatingFragmentButtonListener;
	}

	public void setNavigatingMode(NAVIGATINGMODE mode) {
		mControllerNavigatingFragment.setNavigatingMode(mode);
	}

	public NAVIGATINGMODE getNavigatingMode() {
		return mControllerNavigatingFragment.getNavigatingMode();
	}
	
	public List<MapPos> getPath() {
		return path;
	}

	public void setDroneCurPos(double lat, double lon) {
		mapFragment.setDroneCurPos(lat, lon);
	}
	
	public void updateUserCurPos() {
		mapFragment.updateUserCurPos();
	}

	public MapPos getDroneCurPos() {
		return mapFragment.getDroneCurPos();
	}

	public void setIsFlying(boolean isFlying) {
		mControllerNavigatingFragment.setIsFlying(isFlying);
	}

	public void changeMap(String mapFile) {
		mapFragment.changeMap(mapFile);
	}

}
