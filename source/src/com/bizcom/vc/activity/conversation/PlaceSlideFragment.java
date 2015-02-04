package com.bizcom.vc.activity.conversation;

import java.util.UUID;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.V2.jni.util.V2Log;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.widget.cus.SubsamplingScaleImageView;
import com.bizcom.vc.widget.cus.TouchImageView;
import com.bizcom.vo.VMessage;
import com.bizcom.vo.VMessageImageItem;
import com.bizcom.vo.VMessageImageItem.Size;
import com.v2tech.R;

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
		rlContainer.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
		if (vim == null)
			vim = new VMessageImageItem(new VMessage(0, 0, null, null), UUID
					.randomUUID().toString(), filePath, 0);
		// if(".gif".equals(vim.getExtension())){
		// GifView view = (GifView) v.findViewById(R.id.imageview_smile);
		// view.setGIFResource(filePath);
		// }
		// else{

		View cusView;
		Size fullBitmapSize = vim.getFullBitmapSize();
		if (fullBitmapSize.width > GlobalConfig.BITMAP_MAX_SIZE
				|| fullBitmapSize.height > GlobalConfig.BITMAP_MAX_SIZE) {
			cusView = new SubsamplingScaleImageView(this.getActivity());
			SubsamplingScaleImageView subImage = (SubsamplingScaleImageView) cusView;
			subImage.setFitScreen(true);
			subImage.setMaxScale(3F);
			subImage.setImageFile(filePath);
		} else {
			cusView = new TouchImageView(getActivity());
			final TouchImageView iv = (TouchImageView) cusView;
			mHoldPlaceBitmap = Bitmap.createBitmap(50, 50,
					Bitmap.Config.RGB_565);
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
					if (result == null) {
						V2Log.e("ConversationView",
								"getFullQuantityBitmap is null");
						return;
					}
					iv.setImageBitmap(result);
					iv.setFilePath(filePath);
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
		}

		RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		rl.addRule(RelativeLayout.CENTER_IN_PARENT);
		rlContainer.addView(cusView, rl);
		// }
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
		if (at != null)
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
		// if (!isVisibleToUser && rlContainer != null &&
		// rlContainer.getChildCount() > 0)
		// ((TouchImageView) rlContainer.getChildAt(rlContainer
		// .getChildCount() - 1)).resetZoom();
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
