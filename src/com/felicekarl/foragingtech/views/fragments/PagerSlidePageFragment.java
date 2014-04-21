/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.felicekarl.foragingtech.views.fragments;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.felicekarl.foragingtech.R;
import com.felicekarl.foragingtech.activities.MainActivity;
import com.felicekarl.foragingtech.listeners.FlipForwardButtonListener;
import com.felicekarl.foragingtech.listeners.UpdateFlipForwardButtonListener;

import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * A fragment representing a single step in a wizard. The fragment shows a dummy title indicating
 * the page number, along with some dummy text.
 *
 * <p>This class is used by the {@link CardFlipActivity} and {@link
 * ScreenSlideActivity} samples.</p>
 */
public class PagerSlidePageFragment extends Fragment implements OnClickListener, UpdateFlipForwardButtonListener{
	private static final String TAG = PagerSlidePageFragment.class.getSimpleName();
    /**
     * The argument key for the page number this fragment represents.
     */
    public static final String ARG_PAGE = "page";
    public static final String ARG_MAP = "map";

    /**
     * The fragment's page number, which is set to the argument value for {@link #ARG_PAGE}.
     */
    private int mPageNumber;
    
    private Button btnPlay;
    private String mapFile;
    private Spinner mapSelector;
    private boolean isMapSelectorInitialized = false;
    private List<String> mapList;
    
    private FlipForwardButtonListener mFlipForwardButtonListener;
    
    /**
	 * SharedPreferences.
	 */
    private SharedPreferences preferences;
    /**
	 * SharedPreferences.Editor.
	 */
    private SharedPreferences.Editor editor;

    /**
     * Factory method for this fragment class. Constructs a new fragment for the given page number.
     */
    public static PagerSlidePageFragment create(int pageNumber) {
        PagerSlidePageFragment fragment = new PagerSlidePageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, pageNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public PagerSlidePageFragment() {
    	
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageNumber = getArguments().getInt(ARG_PAGE);
        /* shared preference */
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        editor = preferences.edit();
        mapFile = preferences.getString(ARG_MAP, "");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	ViewGroup view = (ViewGroup) inflater
                .inflate(R.layout.fragment_pager_slide_page, container, false);
    	
        switch(mPageNumber){
    	case 0:
    		// Inflate the layout containing a title and body text.
    		view = (ViewGroup) inflater
                    .inflate(R.layout.fragment_pager_slide_page1, container, false);
            // Set the title view to show the page number.
            //((TextView) view.findViewById(R.id.pageNumber)).setText(String.valueOf(mPageNumber + 1));
    		break;
    	case 1:
    		// Inflate the layout containing a title and body text.
    		view = (ViewGroup) inflater
                    .inflate(R.layout.fragment_pager_slide_page2, container, false);
            // Set the title view to show the page number.
            //((TextView) view.findViewById(R.id.pageNumber)).setText(String.valueOf(mPageNumber + 1));
//    		btnSelectMapFile = (Button) view.findViewById(R.id.btn_select_map);
//            btnSelectMapFile.setOnClickListener(this);
//            
//            tvCurMapFile = (TextView) view.findViewById(R.id.cur_map_file);
    		
    		
    		////////////////////////
    		mapList = new ArrayList<String>();
    	    File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/maps");
    	    File list[] = file.listFiles();

    	    for( int i=0; i< list.length; i++) {
    	    	mapList.add( list[i].getAbsolutePath() );
    	    	Log.d(TAG, "list[i].getAbsolutePath(): " + list[i].getAbsolutePath());
    	    }
    	    
    	    // Selection of the spinner
    	    mapSelector = (Spinner) view.findViewById(R.id.sp_map_select);

    	    // Application of the Array to the Spinner
    	    ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, mapList);
    	    spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
    	    mapSelector.setAdapter(spinnerArrayAdapter);
    	    mapSelector.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                	if (isMapSelectorInitialized) {
                		mapFile = ((TextView) selectedItemView).getText().toString();
                    	editor.putString(ARG_MAP, mapFile);
                        editor.commit();
                        Log.d(TAG, "mapFile: " + mapFile);
                        Log.d(TAG, "preferences.getString(ARG_MAP, ): " + preferences.getString(ARG_MAP, ""));
                	}
                }
                
                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // TODO Auto-generated method stub
                }
            });
    	    
    		break;
    	case 2:
    		// Inflate the layout containing a title and body text.
    		view = (ViewGroup) inflater
                    .inflate(R.layout.fragment_pager_slide_page3, container, false);
            // Set the title view to show the page number.
            //((TextView) view.findViewById(R.id.pageNumber)).setText(String.valueOf(mPageNumber + 1));
    		break;
        }
        
        btnPlay = (Button) view.findViewById(R.id.btn_play);
        btnPlay.setOnClickListener(this);
        
        
        
        return view;
    }

    /**
     * Returns the page number represented by this fragment object.
     */
    public int getPageNumber() {
        return mPageNumber;
    }
    

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_play:
			mFlipForwardButtonListener.flip(mPageNumber);
			break;
		}
	}
	
	@Override
	public void onDestroyView () {
		super.onDestroyView();
	}
	
	@Override
	public void onDestroy () {
		super.onDestroy();
	}

	@Override
	public void updateFlipForwardButtonListener(FlipForwardButtonListener mFlipForwardButtonListener) {
		this.mFlipForwardButtonListener = mFlipForwardButtonListener;
	}
	
//	private void showFileChooser() {
//		Intent intent = new Intent( "com.sec.android.app.myfiles.PICK_DATA" );
//		intent.putExtra("CONTENT_TYPE", "application/octet-stream");
//        try {
//            ((MainActivity) getActivity()).startActivityForResult( intent, MainActivity.PICK_MAP_REQUEST);
//        } catch ( ActivityNotFoundException e ) {
//            e.printStackTrace( );
//            Log.d(TAG, "MyFiles is not installed !!" );
//        }
//	}

//	public void setCurMapFile(final String mapFile) {
//		this.mapFile = mapFile;
//		if (getActivity() != null && getView() != null) {
//            getActivity().runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
////                	tvCurMapFile.setText("| Current Map: " + mapFile);
//                }
//            });
//        }
//		
//		editor.putString(ARG_MAP, mapFile);
//        editor.commit();
//	}
	
	public String getCurMapFile() {
		return mapFile;
	}
	
	public void initMapSelector() {
		final String filePath = preferences.getString(ARG_MAP, "");
        if (getActivity() != null && getView() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	int position = 0;
                    while (!mapSelector.getItemAtPosition(position).toString().equals(filePath) && (mapSelector.getAdapter().getCount() > position + 1) ) {
                    	Log.d(TAG, "mapSelector.getItemAtPosition(position).toString(): " + mapSelector.getItemAtPosition(position).toString());
                    	Log.d(TAG, "filePath: " + filePath);
                        position++;
                    }
                    Log.d(TAG, "mapSelector.getItemAtPosition(position).toString(): " + mapSelector.getItemAtPosition(position).toString());
                	Log.d(TAG, "filePath: " + filePath);
                	isMapSelectorInitialized = true;
                    mapSelector.setSelection(position);
                    
                }
            });
        }
    }
}
