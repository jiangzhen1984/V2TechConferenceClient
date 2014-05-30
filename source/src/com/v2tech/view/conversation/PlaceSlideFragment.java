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
import com.v2tech.view.cus.TouchImageView;
import com.v2tech.vo.VImageMessage;

public class PlaceSlideFragment extends Fragment {

	private VImageMessage vim;

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
					iv.setImageBitmap(vim.getFullQuantityBitmap());
				}
			}

			@Override
			protected void onCancelled() {
				super.onCancelled();
				synchronized (vim) {
					if (vim != null) {
						vim.recycle();
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

		RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		rl.addRule(RelativeLayout.CENTER_IN_PARENT);
		rlContainer.addView(iv, rl);
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
			vim.recycle();
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

	public void setMessage(VImageMessage vim) {
		this.vim = vim;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (vim != null) {
			vim.recycle();
		}
		if (rlContainer != null) {
			rlContainer.removeAllViews();
		}
	}

}
