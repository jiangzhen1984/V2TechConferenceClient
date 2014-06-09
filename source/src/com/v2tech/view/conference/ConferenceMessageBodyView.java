package com.v2tech.view.conference;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.V2Log;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageFaceItem;
import com.v2tech.vo.VMessageImageItem;
import com.v2tech.vo.VMessageTextItem;

public class ConferenceMessageBodyView extends LinearLayout {

	private VMessage mMsg;

	private LinearLayout mContentContainer;

	private View rootView;

	private TextView senderTV;
	private ImageView mImageIV;

	private ClickListener callback;

	public interface ClickListener {
		public void onMessageClicked(VMessage v);
	}

	public ConferenceMessageBodyView(Context context, VMessage m) {
		super(context);
		this.mMsg = m;

		rootView = LayoutInflater.from(context).inflate(
				R.layout.conference_message_body, null, false);
		initView();
		initData();
	}

	private void initView() {
		if (rootView == null) {
			V2Log.e(" root view is Null can not initialize");
			return;
		}

		senderTV = (TextView) rootView
				.findViewById(R.id.conference_message_sender);
		mContentContainer = (LinearLayout) rootView
				.findViewById(R.id.conference_message_body_ly);
		LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		this.addView(rootView, ll);
	}

	private void initData() {
		senderTV.setText(this.mMsg.getFromUser().getName() + "  "
				+ mMsg.getDateTimeStr());
		
		
		mContentContainer.removeAllViews();
		LinearLayout line = new LinearLayout(this.getContext());
		LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,  LinearLayout.LayoutParams.WRAP_CONTENT);
		line.setOrientation(LinearLayout.HORIZONTAL);
		mContentContainer.addView(line, ll);
		List<VMessageAbstractItem>  items = mMsg.getItems();
		for (VMessageAbstractItem item : items) {
			//Add new layout for new line
			if (item.isNewLine()) {
				line = new LinearLayout(this.getContext());
				line.setOrientation(LinearLayout.HORIZONTAL);
				mContentContainer.addView(line, ll);
			}
			View child = null;
			if (item.getType() == VMessageAbstractItem.ITEM_TYPE_TEXT) {
				TextView contentTV = new TextView(this.getContext());
				contentTV.setText(((VMessageTextItem)item).getText());
				contentTV.setTextColor(Color.GRAY);
				contentTV.setHighlightColor(Color.GRAY);
				child = contentTV;
				line.addView(child, ll);
			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FACE) {
				ImageView iv = new ImageView(this.getContext());
				iv.setImageResource(GlobalConfig.GLOBAL_FACE_ARRAY[((VMessageFaceItem)item).getIndex()]);
				child = iv;
				line.addView(child, ll);
			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_IMAGE) {
				ImageView iv = new ImageView(this.getContext());
				VMessageImageItem.Size si = ((VMessageImageItem)item).getCompressedBitmapSize();
				line.addView(iv, new LinearLayout.LayoutParams(
						si.width,
						si.height));
				iv.setTag(item);
				child = iv;
				iv.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					if (callback != null) {
						callback.onMessageClicked(mMsg);
					}
				}

			});
				new LoadTask().execute(new ImageView[]{(ImageView)iv});
			}
			
		}
		
		mContentContainer.setTag(this.mMsg);

	}

	public VMessage getItem() {
		return this.mMsg;
	}

	public void setCallback(ClickListener cl) {
		this.callback = cl;
	}

	public void recycle() {
		mMsg.recycleAllImageMessage();
	}

	public void updateView(VMessage vm) {
		if (vm == null) {
			V2Log.e("Can't not update data vm is null");
			return;
		}
		if (this.mMsg == vm) {
			return;
		}
		if (mContentContainer != null) {
			mContentContainer.removeAllViews();
		}
		this.mMsg = vm;
		initData();
	}

	class LoadTask extends AsyncTask<ImageView, Void , ImageView[]> {

		

		@Override
		protected ImageView[] doInBackground(ImageView... vms) {
			//Only has one element
			for (ImageView vm : vms) {
				((VMessageImageItem)vm.getTag()).getCompressedBitmap();
			}
			return vms;
		}

		@Override
		protected void onPostExecute(ImageView[] result) {
			//Only has one element
			ImageView vm = result[0];
			//If loaded vm is not same member's, means current view has changed message, ignore this result
			VMessageImageItem item  =((VMessageImageItem)vm.getTag());
			if (item.getVm() != mMsg) {
				return;
			}
			Bitmap bm = item.getCompressedBitmap();
			vm.setImageBitmap(bm);
		}
		
	}

}
