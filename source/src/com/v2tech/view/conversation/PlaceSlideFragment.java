package com.v2tech.view.conversation;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.widget.RelativeLayout;

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.view.cus.GifView;
import com.v2tech.view.cus.TouchImageView;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageImageItem;

public class PlaceSlideFragment extends Fragment {
	

	private Bitmap mHoldPlaceBitmap;

	private VMessageImageItem vim;

	private RelativeLayout rlContainer;

	private Object mLock = new Object();

	private AsyncTask<Void, Void, Bitmap> at;
	
	private String filePath;

	private View v;

	public PlaceSlideFragment() {

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle arguments = getArguments();
		filePath = arguments.getString("filePath");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		v = inflater.inflate(R.layout.image_view, container, false);
		rlContainer = (RelativeLayout) v.findViewById(R.id.image_view_root);
		rlContainer.measure(MeasureSpec.UNSPECIFIED,
				MeasureSpec.UNSPECIFIED);
		if(vim == null)
			vim = new VMessageImageItem(new VMessage(0, 0, null, null), filePath);
		if(".gif".equals(vim.getExtension())){
			GifView view = (GifView) v.findViewById(R.id.imageview_smile);
			view.setGIFResource(filePath);
		}
		else{
			final TouchImageView iv  = new TouchImageView(this.getActivity());
			VMessageImageItem.Size si = vim.getFullBitmapSize();
			int width =si.width;
			int height =  si.height;
			mHoldPlaceBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
			iv.setImageBitmap(mHoldPlaceBitmap);
			
			at = new AsyncTask<Void, Void, Bitmap>() {

				@Override
				protected Bitmap doInBackground(Void... params) {
					synchronized (mLock) {
						if (vim != null) {
							return vim.getFullQuantityBitmap();
						}
						return null;
					}
				}

				@Override
				protected void onPostExecute(Bitmap result) {
						if(result == null){
							V2Log.e("ConversationView", "getFullQuantityBitmap is null");
							return;
						}
						iv.setImageBitmap(result);
//						iv.resetZoom();
//						iv.setImageDrawable(new BitmapDrawable(PlaceSlideFragment.this.getActivity().getResources(), vim.getFullQuantityBitmap()));
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
				protected void onCancelled(Bitmap result) {
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
		}
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
		if(at != null)
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
//		if (!isVisibleToUser && rlContainer != null && rlContainer.getChildCount() > 0)
//			((TouchImageView) rlContainer.getChildAt(rlContainer
//					.getChildCount() - 1)).resetZoom();
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
