package com.v2tech.view.conversation;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.v2tech.R;

public class PlaceSlideFragment extends Fragment {

	private Bitmap bmp;

	public PlaceSlideFragment() {

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}
	
	
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.image_view, container, false);
		RelativeLayout rlContainer = (RelativeLayout)v.findViewById(R.id.image_view_root);
		ImageView iv = new ImageView(this.getActivity());
		iv.setImageBitmap(bmp);
		RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		rl.addRule(RelativeLayout.CENTER_IN_PARENT);
		rlContainer.addView(iv, rl);
		return v;
	}

	public void setBitmap(Bitmap bmp) {
		this.bmp = bmp;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		bmp.recycle();
	}
	
	
}
