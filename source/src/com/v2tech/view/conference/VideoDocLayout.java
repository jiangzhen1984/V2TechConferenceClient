package com.v2tech.view.conference;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.view.cus.TouchImageView;
import com.v2tech.vo.V2Doc;
import com.v2tech.vo.V2ShapeMeta;

public class VideoDocLayout extends LinearLayout {

	private View rootView;

	private DocListener listener;

	private Map<String, V2Doc> mDocs;

	/**
	 * Use to draw image backgroud
	 */
	private Bitmap mBackgroundBitMap;

	/**
	 * Use to draw all shapes
	 */
	private Bitmap mShapeBitmap;

	/**
	 * Use to show in image view
	 */
	private Bitmap mImageViewBitmap;

	private Matrix matrix;

	private FrameLayout container;

	private TextView mDocPageTV;

	private ImageView mPrePageButton;

	private ImageView mNextPageButton;
	private PopupWindow mDocListWindow;
	private LinearLayout mDodListContainer;
	private View mShowDocListButton;
	private View mRequestFixedPosButton;
	private ImageView mRequestUpdateSizeButton;

	private ScrollView mDocListWindowScroller;

	private boolean mSyncStatus;

	private V2Doc mCurrentDoc;

	private V2Doc.Page mCurrentPage;

	private Handler mTimeHanlder = new Handler();

	public interface DocListener {
		public void updateDoc(V2Doc doc, V2Doc.Page p);

		public void requestDocViewFixedLayout(View v);

		public void requestDocViewFloatLayout(View v);

		public void requestDocViewFillParent(View v);

		public void requestDocViewRestore(View v);
	};

	public VideoDocLayout(Context context, Map<String, V2Doc> docs,
			String defaultDocId) {
		super(context);
		mDocs = new HashMap<String, V2Doc>();
		mDocs.putAll(docs);
		V2Doc defaultDoc = mDocs.get(defaultDocId);
		initLayout();
		if (defaultDoc != null) {
			updateCurrentDoc(defaultDoc);
		}
	}
	
	

	private void initLayout() {
		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.video_doc_layout, null, false);
		container = (FrameLayout) view.findViewById(R.id.video_doc_container);

		mDocPageTV = (TextView) view.findViewById(R.id.video_doc_navgator);
		mPrePageButton = (ImageView) view
				.findViewById(R.id.video_doc_pre_button);
		mNextPageButton = (ImageView) view
				.findViewById(R.id.video_doc_next_button);
		mPrePageButton.setOnClickListener(pageChangeListener);
		mNextPageButton.setOnClickListener(pageChangeListener);
		mShowDocListButton = view.findViewById(R.id.video_doc_list_button);
		mShowDocListButton.setOnClickListener(showDocListListener);
		mRequestFixedPosButton = view.findViewById(R.id.video_doc_pin_button);
		mRequestFixedPosButton.setOnClickListener(mRequestFixedListener);
		mRequestUpdateSizeButton = (ImageView) view
				.findViewById(R.id.video_doc_screen_button);
		mRequestUpdateSizeButton.setOnClickListener(mUpdateSizeListener);

		this.addView(view, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT));

		rootView = this;

	}

	
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		cleanCache();
	}



	private void showPopUpWindow(View anchor) {
		if (mDocs.isEmpty()) {
			// TODO prompt doc list is empty
			return;
		}
		if (mDocListWindow == null) {
			LayoutInflater inflater = (LayoutInflater) this.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(R.layout.video_doc_list_layout, null);

			mDocListWindowScroller = (ScrollView) view
					.findViewById(R.id.video_doc_list_container_scroller);
			mDocListWindow = new PopupWindow(view, LayoutParams.WRAP_CONTENT,
					(int) (rootView.getHeight() * 0.5));
			mDocListWindow.setBackgroundDrawable(new ColorDrawable(
					Color.TRANSPARENT));
			mDocListWindow.setFocusable(true);
			mDocListWindow.setTouchable(true);
			mDocListWindow.setOutsideTouchable(true);
			mDocListWindow.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss() {
					mDocListWindow.dismiss();
				}

			});

			mDodListContainer = (LinearLayout) view
					.findViewById(R.id.video_doc_list_container);
			View currActivateView = null;
			for (Entry<String, V2Doc> e : mDocs.entrySet()) {
				View v = addViewToDoc(e.getValue());
				if (e.getValue() == mCurrentDoc) {
					currActivateView = v;
				}
			}

			view.measure(container.getMeasuredWidth(),
					container.getMeasuredHeight());
			mDocListWindowScroller.measure(View.MeasureSpec.UNSPECIFIED,
					View.MeasureSpec.UNSPECIFIED);
			mDodListContainer.measure(View.MeasureSpec.UNSPECIFIED,
					View.MeasureSpec.UNSPECIFIED);
			if (currActivateView != null) {
				currActivateView.measure(View.MeasureSpec.EXACTLY,
						View.MeasureSpec.EXACTLY);
				mDocListWindowScroller.computeScroll();
				mDocListWindowScroller
						.scrollTo(0, currActivateView.getBottom());
			}
		}
		if (mDocListWindow.isShowing()) {
			mDocListWindow.dismiss();
		} else {
			int[] l = new int[2];
			anchor.getLocationInWindow(l);

			mDocListWindow.getContentView()
					.measure(container.getMeasuredWidth(),
							container.getMeasuredHeight());

			mDocListWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, l[0],
					l[1] - mDocListWindow.getHeight());

			moveToShowedTab();
		}
	}

	private View addViewToDoc(V2Doc d) {
		if (mDodListContainer == null) {
			return null;
		}
		TextView content = new TextView(this.getContext());
		content.setText(d.getDocName());
		content.setPadding(10, 10, 10, 10);
		if (d == mCurrentDoc) {
			content.setBackgroundColor(this.getResources().getColor(
					R.color.in_meeting_doc_list_activited_doc_bg));

		}
		content.setTag(d);

		mDodListContainer.addView(content, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		LinearLayout separatedLine = new LinearLayout(this.getContext());
		separatedLine.setBackgroundColor(this.getResources().getColor(
				R.color.in_meeting_doc_list_separation));
		mDodListContainer.addView(separatedLine, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 1));

		content.setOnClickListener(updateDocListener);
		return content;

	}

	/**
	 * Close document and remove document from list<br>
	 * If document is current document, will destroy bitmap
	 * 
	 * @param d
	 */
	public void closeDoc(V2Doc d) {
		if (d == null) {
			V2Log.e(" closed Doc is null");
			return;
		}
		if (d == mCurrentDoc) {
			mCurrentDoc = null;
			mCurrentPage = null;
			updateLayoutPageInformation();
			updatePageButton();
			// recycle bitmap
			cleanCache();
		}

		for (int i = 0; mDodListContainer != null
				&& i < mDodListContainer.getChildCount(); i++) {
			View v = mDodListContainer.getChildAt(i);
			if (v instanceof TextView) {
				V2Doc va = (V2Doc) v.getTag();
				if (va != null && va.getId().equals(d.getId())) {
					mDodListContainer.removeView(v);
				}
			}
		}
		mDocs.remove(d.getId());
	}

	public void updateCurrentDoc(V2Doc d) {
		if (d == null) {
			return;
		}

		mCurrentDoc = d;

		mCurrentPage = mCurrentDoc.getActivatePage();

		// draw shape
		if (mCurrentPage != null) {
			updateCurrentDocPage(mCurrentPage);
			drawShape(mCurrentPage.getVsMeta());
		}
		updateLayoutPageInformation();
		updatePageButton();

		moveToShowedTab();
	}

	private void moveToShowedTab() {
		// Update selected doc
		for (int i = 0; mDodListContainer != null
				&& i < mDodListContainer.getChildCount(); i++) {
			final View child = mDodListContainer.getChildAt(i);
			if (mCurrentDoc != child.getTag()) {
				child.setBackgroundColor(Color.WHITE);
			} else {
				child.setBackgroundColor(getResources().getColor(
						R.color.in_meeting_doc_list_activited_doc_bg));
				mDocListWindowScroller.post(new Runnable() {

					@Override
					public void run() {
						if (child.getBottom() > mDocListWindowScroller
								.getMaxScrollAmount()) {
							mDocListWindowScroller.scrollTo(0,
									child.getBottom());
						} else {
							mDocListWindowScroller.scrollTo(0, child.getTop());
						}
					}

				});
			}
		}
	}

	public void updateCurrentDoc() {

		if (mDocs.size() <= 0) {
			return;
		}

		if (mCurrentDoc == null) {
			mCurrentDoc = mDocs.entrySet().iterator().next().getValue();
			if (mCurrentDoc == null) {
				V2Log.w(" No Doc");
				return;
			}
		}

		mCurrentPage = mCurrentDoc.getActivatePage();

		updateCurrentDocPage(mCurrentPage);
		drawShape(mCurrentPage.getVsMeta());
		updateLayoutPageInformation();
		updatePageButton();
	}

	int width;
	int height;
	int sampl;

	private void updateCurrentDocPage(V2Doc.Page p) {
		if (p == null) {
			return;
		}
		// recycle shape bitmap
		recycleBitmap(mShapeBitmap);
		// recycle image bitmap
		recycleBitmap(mBackgroundBitMap);

		// FIXME optimze code
		if (this.mCurrentDoc.getDocType() == V2Doc.DOC_TYPE_BLANK_BOARD) {
			mShapeBitmap = Bitmap.createBitmap(800, 600,
					Bitmap.Config.ARGB_8888);
			matrix = new Matrix();
			RectF src = new RectF();
			RectF dest = new RectF();
			src.left = 0;
			src.right = 800;
			src.top = 0;
			src.bottom = 600;

			dest.left = 0;
			dest.right = 800;
			dest.top = 0;
			dest.bottom = 600;
			matrix.mapRect(src, dest);
			container.removeAllViews();
			TouchImageView iv = new TouchImageView(this.getContext());
			// Merge bitmap
			mergeBitmapToImage(mBackgroundBitMap, mShapeBitmap);
			iv.setImageBitmap(mImageViewBitmap);
			container.addView(iv, new FrameLayout.LayoutParams(
					FrameLayout.LayoutParams.MATCH_PARENT,
					FrameLayout.LayoutParams.MATCH_PARENT));
			container.postInvalidate();

			return;
		}

		if (p.getFilePath() != null) {
			File f = new File(p.getFilePath());
			if (f.exists()) {
				matrix = new Matrix();
				RectF src = new RectF();
				RectF dest = new RectF();

				BitmapFactory.Options ops = new BitmapFactory.Options();
				ops.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(p.getFilePath(), ops);
				src.left = 0;
				src.right = ops.outWidth;
				src.top = 0;
				src.bottom = ops.outHeight;
				width = (int) src.right;
				height = (int) src.bottom;

				ops.inJustDecodeBounds = false;
				BitmapFactory.Options opsNew = new BitmapFactory.Options();
				opsNew.inPurgeable = true;
				opsNew.inInputShareable = true;
				opsNew.inMutable = true;

				if (ops.outHeight < 600 || ops.outWidth < 1080) {
					opsNew.inSampleSize = 1;
				} else if (ops.outHeight < 1080 || ops.outWidth < 1920) {
					opsNew.inSampleSize = 2;
					sampl = 2;
				} else if (ops.outHeight > 1080 || ops.outWidth > 1920) {
					opsNew.inSampleSize = 2;
					sampl = 2;
				} else {
					opsNew.inSampleSize = 2;
					sampl = 2;
				}

				mBackgroundBitMap = BitmapFactory.decodeFile(p.getFilePath(),
						opsNew);

				dest.left = (src.right - opsNew.outWidth) / 2;
				dest.right = opsNew.outWidth;
				dest.top = (src.bottom - opsNew.outHeight) / 2;
				dest.bottom = opsNew.outHeight;
				matrix.postScale(opsNew.outWidth / src.right, opsNew.outHeight
						/ src.bottom);
				// matrix.setScale(src.right /dest.right ,
				// src.bottom/dest.bottom , opsNew.outWidth /2 ,
				// opsNew.outHeight /2);
				matrix.mapRect(dest, src);
				// matrix.setRectToRect(src, dest, ScaleToFit.CENTER);

				container.removeAllViews();
				TouchImageView iv = new TouchImageView(this.getContext());
				// Merge bitmap
				mergeBitmapToImage(mBackgroundBitMap, mShapeBitmap);
				iv.setImageBitmap(mImageViewBitmap);
				container.addView(iv, new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.MATCH_PARENT,
						FrameLayout.LayoutParams.MATCH_PARENT));
				container.postInvalidate();
			} else {
				// Set interval timer for waiting page download
				mTimeHanlder.postDelayed(new Runnable() {

					@Override
					public void run() {
						updateCurrentDocPage(mCurrentPage);
					}

				}, 1000);
				V2Log.e(" doc file doesn't exist " + f.getAbsolutePath());
			}
		}
	}

	public void setListener(DocListener listener) {
		this.listener = listener;
	}

	/**
	 * Merge bitmaps to image view bitmap
	 * 
	 * @param shareDocBm
	 * @param shapesBm
	 */
	private void mergeBitmapToImage(Bitmap shareDocBm, Bitmap shapesBm) {
		if (mImageViewBitmap == null || mImageViewBitmap.isRecycled()) {
			if (shareDocBm != null && !shareDocBm.isRecycled()) {
				mImageViewBitmap = Bitmap.createBitmap(shareDocBm.getWidth(),
						shareDocBm.getHeight(), Config.ARGB_8888);
			} else if (shapesBm != null && !shapesBm.isRecycled()) {
				mImageViewBitmap = Bitmap.createBitmap(shapesBm.getWidth(),
						shapesBm.getHeight(), Config.ARGB_8888);
			} else {
				V2Log.e(" No available bitmap");
				return;
			}
		}

		Canvas target = new Canvas(mImageViewBitmap);
		Paint paint = new Paint();
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		target.drawRect(new Rect(0, 0, mImageViewBitmap.getWidth(),
				mImageViewBitmap.getHeight()), paint);

		Paint p = new Paint();
		if (shareDocBm != null && !shareDocBm.isRecycled()) {
			target.drawBitmap(shareDocBm, 0, 0, p);
		}

		if (shapesBm != null && !shapesBm.isRecycled()) {
			target.drawBitmap(shapesBm, 0, 0, p);
		}
	}

	private void recycleBitmap(Bitmap bm) {
		if (bm != null && !bm.isRecycled()) {
			bm.recycle();
			bm = null;
		}
	}

	/**
	 * Used to manually request FloatLayout, Because when this layout will hide,
	 * call this function to inform interface
	 */
	public void requestFloatLayout() {
		// Ignore same state
		if ("float".equals(mRequestFixedPosButton.getTag())) {
			return;
		}

		if (this.listener != null) {
			this.listener.requestDocViewFloatLayout(rootView);
		}

		mRequestFixedPosButton.setTag("float");
	}

	/**
	 * Used to manually fixed FloatLayout, Because when this layout will hide,
	 * call this function to inform interface
	 */
	public void requestFixedLayout() {
		// Ignore same state
		if ("fix".equals(mRequestFixedPosButton.getTag())) {
			return;
		}

		if (this.listener != null) {
			this.listener.requestDocViewFixedLayout(rootView);
		}

		mRequestFixedPosButton.setTag("fix");
	}

	/**
	 * Used to manually request requestRestore, Because when this layout will
	 * hide, call this function to inform interface
	 */
	public void requestRestore() {
		if (this.listener != null) {
			this.listener.requestDocViewRestore(rootView);
		}
		// Ignore same state
		if (mRequestUpdateSizeButton.equals("fullscreen")) {
			return;
		}
		// restore image
		mRequestUpdateSizeButton.setTag("fullscreen");
		mRequestUpdateSizeButton
				.setImageResource(R.drawable.video_doc_full_screen_button_selector);
	}

	/**
	 * Add new document to list
	 * 
	 * @param doc
	 */
	public void addDoc(V2Doc doc) {
		if (!mDocs.containsKey(doc.getId())) {
			mDocs.put(doc.getId(), doc);
			addViewToDoc(doc);
		}
	}

	/**
	 * Update page information according current document
	 */
	public void updateLayoutPageInformation() {
		if (mCurrentDoc == null) {
			mDocPageTV.setText("0/0");
		} else {
			mDocPageTV.setText(mCurrentDoc.getActivatePageNo() + "/"
					+ mCurrentDoc.getPageSize());
		}
	}

	/**
	 * Update page button enable/disable according to current document
	 */
	public void updatePageButton() {
		if (mCurrentDoc == null || mSyncStatus) {
			mPrePageButton
					.setImageResource(R.drawable.video_doc_left_arrow_gray);
			mNextPageButton
					.setImageResource(R.drawable.video_doc_right_arrow_gray);
			return;
		}

		if (mCurrentDoc.getActivatePageNo() == 1) {
			mPrePageButton
					.setImageResource(R.drawable.video_doc_left_arrow_gray);
		} else {
			mPrePageButton
					.setImageResource(R.drawable.video_doc_page_button_left_selector);
		}
		if (mCurrentDoc.getActivatePageNo() == mCurrentDoc.getPageSize()) {
			mNextPageButton
					.setImageResource(R.drawable.video_doc_right_arrow_gray);
		} else {
			mNextPageButton
					.setImageResource(R.drawable.video_doc_page_button_right_selector);
		}
	}

	/**
	 * Update sync status according to chairman's setting if Set true, user
	 * can't switch page any more. Otherwise user can
	 * 
	 * @param sync
	 */
	public void updateSyncStatus(boolean sync) {
		mSyncStatus = sync;
		if (sync) {
			mPrePageButton
					.setImageResource(R.drawable.video_doc_left_arrow_gray);
			mNextPageButton
					.setImageResource(R.drawable.video_doc_right_arrow_gray);
			mShowDocListButton.setEnabled(false);
			mNextPageButton.setEnabled(false);
			mPrePageButton.setEnabled(false);
		} else {
			updatePageButton();
			mShowDocListButton.setEnabled(true);
			mNextPageButton.setEnabled(true);
			mPrePageButton.setEnabled(true);
		}
	}

	/**
	 * According to docId and pageNo draw shape
	 * 
	 * @param docId
	 * @param pageNo
	 * @param list
	 */
	public void drawShape(String docId, int pageNo, V2ShapeMeta shape) {
		if (this.mCurrentDoc == null || this.mCurrentPage == null
				|| docId == null) {
			return;
		}
		if (!docId.equals(this.mCurrentDoc.getId())
				|| pageNo != this.mCurrentPage.getNo()) {
			return;
		}
		List<V2ShapeMeta> list = new ArrayList<V2ShapeMeta>();
		list.add(shape);
		drawShape(list);
	}

	/**
	 * According to docId and pageNo draw shape
	 * 
	 * @param docId
	 * @param pageNo
	 * @param list
	 */
	public void drawShape(String docId, int pageNo, List<V2ShapeMeta> list) {
		if (this.mCurrentDoc == null || this.mCurrentPage == null
				|| docId == null) {
			return;
		}
		if (!docId.equals(this.mCurrentDoc.getId())
				|| pageNo != this.mCurrentPage.getNo()) {
			return;
		}
		drawShape(list);
	}

	public void drawShape(List<V2ShapeMeta> list) {
		if (list == null) {
			V2Log.w(" shape list is null");
			return;
		}

		if (mShapeBitmap == null || mShapeBitmap.isRecycled()) {
			mShapeBitmap = Bitmap.createBitmap(mBackgroundBitMap.getWidth(),
					mBackgroundBitMap.getHeight(), Config.ARGB_8888);
		}

		Canvas cv = new Canvas(mShapeBitmap);
		// target.concat(matrix);
		for (V2ShapeMeta meta : list) {
			meta.draw(cv);
		}

		mergeBitmapToImage(mBackgroundBitMap, mShapeBitmap);

		container.postInvalidate();
	}

	
	/**
	 */
	public void cleanCache() {
		container.removeAllViews();

		if (this.mImageViewBitmap != null
				&& !this.mImageViewBitmap.isRecycled()) {
			Canvas ca = new Canvas(mImageViewBitmap);
			ca.drawColor(Color.WHITE);
			this.mImageViewBitmap.recycle();
		}

		// recycle shape bitmap
		recycleBitmap(mShapeBitmap);
		// recycle image bitmap
		recycleBitmap(mBackgroundBitMap);
	}

	public boolean isFullScreenSize() {
		return "fullscreen".equals(mRequestUpdateSizeButton.getTag());
	}

	private OnClickListener pageChangeListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (mCurrentDoc == null) {
				V2Log.w(" mCurrentDoc is null  doc is not selected yet");
				return;
			}
			if (view == mNextPageButton) {
				if (mCurrentDoc.getActivatePageNo() < mCurrentDoc.getPageSize()) {
					mCurrentDoc.setActivatePageNo(mCurrentDoc
							.getActivatePageNo() + 1);
					updateCurrentDoc();
				}
			} else if (view == mPrePageButton) {
				if (mCurrentDoc.getActivatePageNo() > 1) {
					mCurrentDoc.setActivatePageNo(mCurrentDoc
							.getActivatePageNo() - 1);
					updateCurrentDoc();
				}
			} else {
				V2Log.e(" Invalid click listener");
			}
		}

	};

	private OnClickListener showDocListListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			showPopUpWindow(view);
		}

	};

	private OnClickListener updateDocListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mSyncStatus) {
				Toast.makeText(getContext(), R.string.warning_under_sync,
						Toast.LENGTH_SHORT).show();
				return;
			}
			V2Doc d = (V2Doc) v.getTag();
			if (d != mCurrentDoc) {
				V2Doc.Page p = d.getActivatePage();
				if (p == null
						|| ((p.getFilePath() == null || p.getFilePath()
								.isEmpty()) && (d.getDocType() != V2Doc.DOC_TYPE_BLANK_BOARD))) {
					Toast.makeText(getContext(),
							R.string.warning_downloading_doc,
							Toast.LENGTH_SHORT).show();
					return;
				}
				for (int i = 0; i < mDodListContainer.getChildCount(); i++) {
					View child = mDodListContainer.getChildAt(i);
					if (child.getTag() != null) {
						child.setBackgroundColor(Color.WHITE);
					} else {
						continue;
					}
				}
				v.setBackgroundColor(getResources().getColor(
						R.color.in_meeting_doc_list_activited_doc_bg));
				mCurrentDoc = d;
				updateCurrentDoc();
				mDocListWindow.dismiss();
			}
		}

	};

	private OnClickListener mRequestFixedListener = new OnClickListener() {

		@Override
		public void onClick(View view) {

			if (view.getTag().equals("float")) {
				if (listener != null) {
					listener.requestDocViewFixedLayout(rootView);
				}
			} else {
				if (listener != null) {
					listener.requestDocViewFloatLayout(rootView);
				}
			}

			if (view.getTag().equals("float")) {
				view.setTag("fix");
			} else {
				view.setTag("float");
			}
		}

	};

	private OnClickListener mUpdateSizeListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (view.getTag().equals("fullscreen")) {
				view.setTag("restorescreen");
				mRequestUpdateSizeButton
						.setImageResource(R.drawable.video_doc_full_screen_button_selector);
			} else {
				view.setTag("fullscreen");
				mRequestUpdateSizeButton
						.setImageResource(R.drawable.video_doc_restore_screen_button_selector);
			}

			if (view.getTag().equals("fullscreen")) {
				if (listener != null) {
					listener.requestDocViewFillParent(rootView);
					if (container.getChildCount() > 0) {
						TouchImageView tiv = (TouchImageView) container
								.getChildAt(0);
						tiv.setZoom(2F);
					}
				}
			} else {
				if (listener != null) {
					listener.requestDocViewRestore(rootView);
					if (container.getChildCount() > 0) {
						TouchImageView tiv = (TouchImageView) container
								.getChildAt(0);
						tiv.setZoom(1F);
					}
				}
			}

		}

	};
}
