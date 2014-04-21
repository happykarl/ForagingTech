package com.felicekarl.foragingtech.views.fragments;

import com.felicekarl.foragingtech.R;
import com.felicekarl.foragingtech.listeners.FlipForwardButtonListener;
import com.felicekarl.foragingtech.listeners.UpdateFlipForwardButtonListener;

import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class PagerFragment extends BaseFragment implements OnClickListener, UpdateFlipForwardButtonListener{
	private static final String TAG = PagerFragment.class.getSimpleName();
	private static final int NUM_PAGES = 3;
	
	private PagerAdapter mPagerAdapter;
	private ViewPager mPager;
	private Button[] btnFooterPagers;
	private PagerSlidePageFragment[] slideFragments;
	
	private FlipForwardButtonListener mFlipForwardButtonListener;
	
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = (ViewGroup) inflater.inflate(R.layout.fragment_pager, container, false);
		
		mPager = (ViewPager) view.findViewById(R.id.pager);
		mPagerAdapter = new SlidePagerAdapter(getActivity(), getChildFragmentManager(), mFlipForwardButtonListener);
		mPager.setAdapter(mPagerAdapter);
		mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
            	Log.d(TAG, "position: " + position);
            	switch(position) {
            	case 0:
            		btnFooterPagers[0].setText(R.string.btn_footer_flying);
            		btnFooterPagers[1].setText("");
            		btnFooterPagers[2].setText("");
            		break;
            	case 1:
            		btnFooterPagers[0].setText("");
            		btnFooterPagers[1].setText(R.string.btn_footer_navigating);
            		btnFooterPagers[2].setText("");
            		break;
            	case 2:
            		btnFooterPagers[0].setText("");
            		btnFooterPagers[1].setText("");
            		btnFooterPagers[2].setText(R.string.btn_footer_setting);
            		break;
            	}
                // When changing pages, reset the action bar actions since they are dependent
                // on which page is currently active. An alternative approach is to have each
                // fragment expose actions itself (rather than the activity exposing actions),
                // but for simplicity, the activity provides the actions in this sample.
                //invalidateOptionsMenu();
            }
        });
		
		// Get Button Resources
		btnFooterPagers = new Button[NUM_PAGES];
		btnFooterPagers[0] = (Button) view.findViewById(R.id.footer_pager1);
		btnFooterPagers[1] = (Button) view.findViewById(R.id.footer_pager2);
		btnFooterPagers[2] = (Button) view.findViewById(R.id.footer_pager3);
        for (int i=0; i<NUM_PAGES; i++) {
        	btnFooterPagers[i].setOnClickListener(this);
        }
        
        slideUpFragment();
        
		return view;
		
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.footer_pager1:
			mPager.setCurrentItem(0);
			break;
		case R.id.footer_pager2:
			mPager.setCurrentItem(1);
			slideFragments[1].initMapSelector();
			break;
		case R.id.footer_pager3:
			mPager.setCurrentItem(2);
			break;
		}
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
	public void updateFlipForwardButtonListener(FlipForwardButtonListener mFlipForwardButtonListener) {
		this.mFlipForwardButtonListener = mFlipForwardButtonListener;
		if(mPagerAdapter != null)
			((SlidePagerAdapter) mPagerAdapter).updateFlipForwardButtonListener(mFlipForwardButtonListener);
	}
	
	private class SlidePagerAdapter extends FragmentStatePagerAdapter implements UpdateFlipForwardButtonListener {
		private Context context = null;
		private FlipForwardButtonListener mFlipForwardButtonListener;
		
        public SlidePagerAdapter(Context context, FragmentManager mFragmentManager, FlipForwardButtonListener mFlipForwardButtonListener) {
            super(mFragmentManager);
            this.context = context;
            this.mFlipForwardButtonListener = mFlipForwardButtonListener;
            slideFragments = new PagerSlidePageFragment[NUM_PAGES];
        }

        @Override
        public PagerSlidePageFragment getItem(int position) {
        	slideFragments[position] = PagerSlidePageFragment.create(position);
        	slideFragments[position].updateFlipForwardButtonListener(mFlipForwardButtonListener);
        	return slideFragments[position];
        }
        
        

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

		@Override
		public void updateFlipForwardButtonListener(FlipForwardButtonListener mFlipForwardButtonListener) {
			this.mFlipForwardButtonListener = mFlipForwardButtonListener;
    		for(int i=0; i<NUM_PAGES; i++) {
    			slideFragments[i].updateFlipForwardButtonListener(mFlipForwardButtonListener);
    		}
		}
    }

	@Override
	public void resetFragment() {
		// TODO Auto-generated method stub
		
	}
	
	public String getCurMapFile() {
		return slideFragments[1].getCurMapFile();
	}
	
}
