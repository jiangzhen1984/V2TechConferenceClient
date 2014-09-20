package com.v2tech.view.conversation;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.view.cus.TouchImageView;
import com.v2tech.vo.VMessageImageItem;

public class PlaceSlideFragment extends Fragment {
	
	private Bitmap mHoldPlaceBitmap;

	private VMessageImageItem vim;

	private RelativeLayout rlContainer;

	private Object mLock = new Object();

	private AsyncTask<Void, Void, Void> at;

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
		rlContainer = (RelativeLayout) v.findViewById(R.id.image_view_root);

		final TouchImageView iv = new TouchImageView(this.getActivity());
		
		VMessageImageItem.Size si = vim.getFullBitmapSize();
		int width =si.width;
		int height =  si.height;
		mHoldPlaceBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		iv.setImageBitmap(mHoldPlaceBitmap);
		
		
		RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		rl.addRule(RelativeLayout.CENTER_IN_PARENT);
		rlContainer.addView(iv, rl);
		
		at = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				synchronized (mLock) {
					if (vim != null) {
						vim.getFullQuantityBitmap();
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				if (vim != null) {
					if(vim.getFullQuantityBitmap() == null){
						V2Log.e("ConversationView", "getFullQuantityBitmap is null");
						return;
					}
					iv.setImageBitmap(vim.getFullQuantityBitmap());
//					iv.resetZoom();
//					iv.setImageDrawable(new BitmapDrawable(PlaceSlideFragment.this.getActivity().getResources(), vim.getFullQuantityBitmap()));
//					
				}
			}

			@Override
			protected void onCancelled() {
				super.onCancelled();
				synchronized (vim) {
					if (vim != null) {
						vim.recycleFull();
					}
				}
				iv.setImageBitmap(null);
			}

			@Override
			protected void onCancelled(Void result) {
				super.onCancelled(result);
				synchronized (vim) {
					if (vim != null) {
						vim.recycle();
					}
				}
				iv.setImageBitmap(null);
			}

		}.execute();

		return v;
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		at.cancel(true);
		if (vim != null) {
			vim.recycleFull();
		}
		if (mHoldPlaceBitmap != null) {
			mHoldPlaceBitmap.recycle();
		}
		rlContainer.removeAllViews();
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		if (!isVisibleToUser && rlContainer != null && rlContainer.getChildCount() > 0)
			((TouchImageView) rlContainer.getChildAt(rlContainer
					.getChildCount() - 1)).resetZoom();
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	public void setMessage(VMessageImageItem vim) {
		this.vim = vim;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (vim != null) {
			vim.recycleFull();
		}
		if (rlContainer != null) {
			rlContainer.removeAllViews();
		}
	}

}
