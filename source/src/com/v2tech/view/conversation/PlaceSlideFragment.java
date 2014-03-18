package com.v2tech.view.conversation;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.v2tech.R;
import com.v2tech.logic.VImageMessage;
import com.v2tech.view.cus.TouchImageView;

public class PlaceSlideFragment extends Fragment {

	private VImageMessage vim;

	
	private RelativeLayout rlContainer;
	
	private Object mLock = new Object();

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
			rlContainer = (RelativeLayout) v
					.findViewById(R.id.image_view_root);
		
		final TouchImageView iv = new TouchImageView(this.getActivity());

		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				synchronized(mLock) {
					if (vim != null) {
						vim.getFullQuantityBitmap();
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				if (vim != null) {
					iv.setImageBitmap(vim.getFullQuantityBitmap());
				}
			}

			
		}.execute();
		
		RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		rl.addRule(RelativeLayout.CENTER_IN_PARENT);
		rlContainer.addView(iv, rl);
		return v;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (vim != null) {
			vim.recycle();
		}
		rlContainer.removeAllViews();
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	public void setMessage(VImageMessage vim) {
		this.vim = vim;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (vim != null) {
			vim.recycle();
		}
		rlContainer.removeAllViews();
	}

}
