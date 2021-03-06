package org.worldbank.seg_2g.worldbankapp;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

public class GraphAdapter extends FragmentPagerAdapter {
	
	
	private static int pagePosition;
	private static final int TOTAL_PAGES = 6;

	public GraphAdapter(FragmentManager frame) {
		super(frame);
		pagePosition = 1;

	}

	@Override
	public Fragment getItem(int position) {
		return GraphFrameHolder.newInstance(position + 1);
	}
	
	@Override
	public int getCount() {
		// Show number of pages.
		return TOTAL_PAGES;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		switch (position) {
			case 1:
			case 2:
			case 3:
			case 4:	
			case 5:	
		}
		return null;
	}

	//not used yet
	public static class GraphFrameHolder extends Fragment {

		private static final String CATEGORY_PAGE = "frameID";
		
		static GraphFrameHolder newInstance(int sectionNumber) {
			GraphFrameHolder graphFrame = new GraphFrameHolder();
			Bundle fMap = new Bundle();
			fMap.putInt(CATEGORY_PAGE, sectionNumber);
			graphFrame.setArguments(fMap);
			return graphFrame;
		}

		public GraphFrameHolder() {
		}

		//TODO fix this method
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_graph,
					container, false);
			
			return rootView;
		}
	}
	
	public int getPosition() {
		return pagePosition;
	}
	
	public void setPosition() {
		if (pagePosition < 5) {
			++pagePosition;
		}
	}
	
	public void setBackPosition() {
		if (pagePosition > 0) {
			--pagePosition;
		}
	}

	public void restartPosition() {
		pagePosition = 1;
	}
	
	public void restartGraph() {
		pagePosition = 2;
	}
	
	public void setNewPosition(int newPosition) {
		pagePosition = newPosition;
	}
}



