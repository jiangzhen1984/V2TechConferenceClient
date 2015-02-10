package com.bizcom.vc.activity.conference;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import com.bizcom.util.BitmapUtil;
import com.bizcom.util.OrderedHashMap;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.widget.cus.SubsamplingScaleImageView;
import com.bizcom.vc.widget.cus.TouchImageView;
import com.bizcom.vo.V2Doc;
import com.bizcom.vo.V2ShapeMeta;
import com.v2tech.R;

public class LeftShareDocLayout extends LinearLayout {

	private static final String TAG = "LeftShareDocLayout";
	private FrameLayout mDocDisplayContainer;
	private TextView mDocPageNumberTV;
	private TextView mDocTitleView;
	private ImageView mPrePageButton;
	private ImageView mNextPageButton;
	private PopupWindow mDocListWindow;
	private LinearLayout mDocListView;
	private View mDocListButton;
	private View mFixedPosButton;
	private ImageView mUpdateSizeButton;
	private View mShareDocButton;
	private ScrollView mDocListWindowScroller;
	private ImageView mShareDocCloseButton;
	private ShareDocCloseButtonOnClickListener mShareDocCloseButtonOnClickListener = new ShareDocCloseButtonOnClickListener();
	private OnClickListener mTurnPageButtonOnClickListener = new TurnPageButtonOnClickListener();
	private OnClickListener mDocListButtonOnClickListener = new DocListButtonOnClickListener();
	private OnClickListener mFixedPosButtonOnClickListener = new FixedPosButtonOnClickListener();
	private OnClickListener mShareDocButtonOnClickListener = new ShareDocButtonOnClickListener();
	private OnClickListener mUpdateSizeButtonOnClickListener = new UpdateSizeButtonOnClickListener();
	private OnClickListener mDocListItemOnClickListener = new DocListItemOnClickListener();
	private GestureDetector.OnDoubleTapListener mTouchImageViewGestureDetectorListener = new TouchImageViewGestureDetectorListener();

	private View rootView;
	private DocListener listener;
	private OrderedHashMap<String, V2Doc> mDocs;

	// Use to draw image backgroud
	private Bitmap mBackgroundBitMap;
	// Use to draw all shapes
	private Bitmap mShapeBitmap;
	// Use to show in image view
	private Bitmap mImageViewBitmap;
	private Matrix matrix;
	private boolean mSyncStatus;
	private boolean isLectureStateGranted = false;
	private V2Doc mCurrentDoc;
	private V2Doc.Page mCurrentPage;
	private Handler mTimeHanlder = new Handler();
	private Context mContext = null;

	public interface DocListener {
		public void updateDoc(V2Doc doc, V2Doc.Page p);

		public void requestDocViewFixedLayout(View v);

		public void requestDocViewFloatLayout(View v);

		public void requestDocViewFillParent(View v);

		public void requestDocViewRestore(View v);

		public void requestShareImageDoc(View v);

		public void requestShareDocClose(View v);

	};

	public LeftShareDocLayout(Context context, Map<String, V2Doc> docs,
			String currentLecturerActivateDocId) {
		super(context);
		this.mContext = context;
		mDocs = new OrderedHashMap<String, V2Doc>();
		mDocs.putAll(docs);
		V2Doc currentLecturerActivateDoc = mDocs
				.get(currentLecturerActivateDocId);
		initLayout();
		if (currentLecturerActivateDoc != null) {
			updateCurrentDoc(currentLecturerActivateDoc);
		}

		if (mDocs.size() <= 0) {
			mDocTitleView.setText(R.string.confs_doc);
		} else {
			if (mCurrentDoc != null) {
				mDocTitleView.setText(mCurrentDoc.getDocName());
			} else
				mDocTitleView.setText(R.string.confs_doc);
		}

	}

	private void initLayout() {
		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.video_doc_layout, null, false);

		mDocDisplayContainer = (FrameLayout) view
				.findViewById(R.id.video_doc_container);

		mDocPageNumberTV = (TextView) view
				.findViewById(R.id.video_doc_navgator);
		mDocTitleView = (TextView) view.findViewById(R.id.video_doc_title);
		mShareDocCloseButton = (ImageView) view
				.findViewById(R.id.share_doc_close_button);
		mShareDocCloseButton
				.setOnClickListener(mShareDocCloseButtonOnClickListener);

		mPrePageButton = (ImageView) view
				.findViewById(R.id.video_doc_pre_button);
		mPrePageButton.setOnClickListener(mTurnPageButtonOnClickListener);
		mNextPageButton = (ImageView) view
				.findViewById(R.id.video_doc_next_button);
		mNextPageButton.setOnClickListener(mTurnPageButtonOnClickListener);
		mDocListButton = view.findViewById(R.id.video_doc_list_button);
		mDocListButton.setOnClickListener(mDocListButtonOnClickListener);
		mFixedPosButton = view.findViewById(R.id.video_doc_pin_button);
		mFixedPosButton.setOnClickListener(mFixedPosButtonOnClickListener);
		mUpdateSizeButton = (ImageView) view
				.findViewById(R.id.video_doc_screen_button);
		mUpdateSizeButton.setOnClickListener(mUpdateSizeButtonOnClickListener);

		mShareDocButton = view.findViewById(R.id.video_doc_share_button);
		mShareDocButton.setOnClickListener(mShareDocButtonOnClickListener);

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

	private View addDocNameViewToDocListView(V2Doc v2Doc) {
		if (mDocListView == null || v2Doc == null) {
			return null;
		}
		TextView docNameView = new TextView(this.getContext());
		docNameView.setSingleLine(true);
		docNameView.setEllipsize(TruncateAt.END);
		docNameView.setText(v2Doc.getDocName());
		docNameView.setPadding(10, 10, 10, 10);
		if (v2Doc == mCurrentDoc) {
			docNameView.setBackgroundColor(this.getResources().getColor(
					R.color.in_meeting_doc_list_activited_doc_bg));

		}
		docNameView.setTag(v2Doc);

		mDocListView.addView(docNameView, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		LinearLayout separatedLine = new LinearLayout(this.getContext());
		separatedLine.setBackgroundColor(this.getResources().getColor(
				R.color.in_meeting_doc_list_separation));

		mDocListView.addView(separatedLine, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 1));

		docNameView.setOnClickListener(mDocListItemOnClickListener);
		return docNameView;

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
			mDocTitleView.setText(R.string.confs_doc);
		}

		for (int i = 0; mDocListView != null
				&& i < mDocListView.getChildCount(); i++) {
			View v = mDocListView.getChildAt(i);
			if (v instanceof TextView) {
				V2Doc va = (V2Doc) v.getTag();
				if (va != null && va.getId().equals(d.getId())) {
					mDocListView.removeView(v);
				}
			}
		}

		mDocs.remove(d.getId());
	}

	public void updateCurrentDoc(V2Doc d) {
		if (d == null) {
			V2Log.e("VideoDocLayout updateCurrentDoc --> Given V2Doc Object is null!");
			return;
		}

		mCurrentDoc = d;
		mDocTitleView.setText(mCurrentDoc.getDocName());

		mCurrentPage = mCurrentDoc.getActivatePage();

		// draw shape
		if (mCurrentPage != null) {
			updateCurrentDocPage(mCurrentPage);
			// drawShape(mCurrentPage.getVsMeta());
		}
		updateLayoutPageInformation();
		updatePageButton();

		moveToShowedTab();
		mDocDisplayContainer.postInvalidate();
	}

	private void moveToShowedTab() {
		// Update selected doc
		for (int i = 0; mDocListView != null
				&& i < mDocListView.getChildCount(); i++) {
			final View child = mDocListView.getChildAt(i);
			if (child instanceof TextView) {
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
								mDocListWindowScroller.scrollTo(0,
										child.getTop());
							}
						}

					});
				}
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
		mDocTitleView.setText(mCurrentDoc.getDocName());
		mCurrentPage = mCurrentDoc.getActivatePage();
		updateCurrentDocPage(mCurrentPage);
		updateLayoutPageInformation();
		updatePageButton();
	}

	private void updateCurrentDocPage(V2Doc.Page p) {

		if (p == null) {
			Log.i("20141229 2", "updateCurrentDocPage() p = null");
			for (int i = 0; i < mDocDisplayContainer.getChildCount(); i++) {
				View v = mDocDisplayContainer.getChildAt(i);
				if (v instanceof TouchImageView) {
					((TouchImageView) v).setImageBitmap(null);
				}
			}
			mDocDisplayContainer.removeAllViews();
			TouchImageView iv = new TouchImageView(this.getContext());
			iv.setOnDoubleTapListener(mTouchImageViewGestureDetectorListener);

			// Merge bitmap
			// iv.setImageResource(R.drawable.conversation_files_button);
			mDocDisplayContainer.addView(iv, new FrameLayout.LayoutParams(
					FrameLayout.LayoutParams.MATCH_PARENT,
					FrameLayout.LayoutParams.MATCH_PARENT));
			// mDocDisplayContainer.postInvalidate();

			return;
		}

		// // recycle shape bitmap
		// recycleBitmap(mShapeBitmap);
		// // recycle image bitmap
		// recycleBitmap(mBackgroundBitMap);
		// recycleBitmap(mImageViewBitmap);

		// if (this.mCurrentDoc.getDocType() == V2Doc.DOC_TYPE_BLANK_BOARD) {
		// mShapeBitmap = Bitmap.createBitmap(800, 600,
		// Bitmap.Config.ARGB_8888);
		// matrix = new Matrix();
		// RectF src = new RectF();
		// RectF dest = new RectF();
		// src.left = 0;
		// src.right = 800;
		// src.top = 0;
		// src.bottom = 600;
		//
		// dest.left = 0;
		// dest.right = 800;
		// dest.top = 0;
		// dest.bottom = 600;
		// matrix.mapRect(src, dest);
		// mDocDisplayContainer.removeAllViews();
		// TouchImageView iv = new TouchImageView(this.getContext());
		// // Merge bitmap
		// mergeBitmapToImage(mBackgroundBitMap, mShapeBitmap);
		// iv.setImageBitmap(mImageViewBitmap);
		// mDocDisplayContainer.addView(iv, new FrameLayout.LayoutParams(
		// FrameLayout.LayoutParams.MATCH_PARENT,
		// FrameLayout.LayoutParams.MATCH_PARENT));
		// mDocDisplayContainer.postInvalidate();
		//
		// return;
		// }

		if (!TextUtils.isEmpty(p.getFilePath())) {
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
				ops.inJustDecodeBounds = false;

				V2Log.d(TAG,
						"updateCurrentDocPage --> doc file path : "
								+ p.getFilePath());

				recycleBitmap(mBackgroundBitMap);
				mBackgroundBitMap = BitmapUtil.getDocCompressBitmap(p
						.getFilePath());
				Matrix m = new Matrix();
				m.postRotate(BitmapUtil.getBitmapRotation(p.getFilePath()));
				mBackgroundBitMap = Bitmap.createBitmap(mBackgroundBitMap, 0,
						0, mBackgroundBitMap.getWidth(),
						mBackgroundBitMap.getHeight(), m, true);

				int[] params = new int[2];
				BitmapUtil.getFullBitmapBounds(p.getFilePath(), params);
				dest.left = (src.right - params[0]) / 2;
				dest.right = params[0];
				dest.top = (src.bottom - params[1]) / 2;
				dest.bottom = params[1];
				matrix.postScale(params[0] / src.right, params[1] / src.bottom);
				matrix.mapRect(dest, src);

				for (int i = 0; i < mDocDisplayContainer.getChildCount(); i++) {
					View v = mDocDisplayContainer.getChildAt(i);
					if (v instanceof TouchImageView) {
						((TouchImageView) v).setImageBitmap(null);
					}
				}
				mDocDisplayContainer.removeAllViews();
				View v = null;
				if (params[0] > GlobalConfig.BITMAP_MAX_SIZE
						|| params[1] > GlobalConfig.BITMAP_MAX_SIZE) {
					v = new SubsamplingScaleImageView(this.getContext());
					SubsamplingScaleImageView subImage = (SubsamplingScaleImageView) v;
					subImage.setFitScreen(true);
					subImage.setImageFile(p.getFilePath());
					subImage.setOnDoubleTapListener(mTouchImageViewGestureDetectorListener);
				} else {
					v = new TouchImageView(this.getContext());
					TouchImageView iv = (TouchImageView) v;
					iv.setOnDoubleTapListener(mTouchImageViewGestureDetectorListener);
					// Merge bitmap
					mergeBitmapToImage(mBackgroundBitMap,mShapeBitmap);
					if (!mImageViewBitmap.isRecycled()) {
						iv.setImageBitmap(mImageViewBitmap);
					} else {
						iv.setImageBitmap(null);
					}
				}

				FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.MATCH_PARENT,
						FrameLayout.LayoutParams.MATCH_PARENT);
				mDocDisplayContainer.addView(v, fl);
				// mDocDisplayContainer.postInvalidate();
			} else {
				// // Set interval timer for waiting page download
				// mTimeHanlder.postDelayed(new Runnable() {
				//
				// @Override
				// public void run() {
				// updateCurrentDocPage(mCurrentPage);
				// }
				//
				// }, 1000);
				V2Log.e("VideoDocLayout drawShape --> doc file doesn't exist:"
						+ f.getAbsolutePath() + " p.getFilePath() = "
						+ p.getFilePath());

				for (int i = 0; i < mDocDisplayContainer.getChildCount(); i++) {
					View v = mDocDisplayContainer.getChildAt(i);
					if (v instanceof TouchImageView) {
						((TouchImageView) v).setImageBitmap(null);
					}
				}
				mDocDisplayContainer.removeAllViews();
				TouchImageView iv = new TouchImageView(this.getContext());
				iv.setOnDoubleTapListener(mTouchImageViewGestureDetectorListener);
				// Merge bitmap
				// iv.setImageResource(R.drawable.conversation_files_button);
				mDocDisplayContainer.addView(iv, new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.MATCH_PARENT,
						FrameLayout.LayoutParams.MATCH_PARENT));
				// mDocDisplayContainer.postInvalidate();

			}
		} else {
			V2Log.e("VideoDocLayout drawShape --> doc file path is empty:"
					+ p.getFilePath());
			for (int i = 0; i < mDocDisplayContainer.getChildCount(); i++) {
				View v = mDocDisplayContainer.getChildAt(i);
				if (v instanceof TouchImageView) {
					((TouchImageView) v).setImageBitmap(null);
				}
			}
			mDocDisplayContainer.removeAllViews();
			TouchImageView iv = new TouchImageView(this.getContext());
			iv.setOnDoubleTapListener(mTouchImageViewGestureDetectorListener);
			// Merge bitmap
			// iv.setImageResource(R.drawable.conversation_files_button);
			mDocDisplayContainer.addView(iv, new FrameLayout.LayoutParams(
					FrameLayout.LayoutParams.MATCH_PARENT,
					FrameLayout.LayoutParams.MATCH_PARENT));
			// mDocDisplayContainer.postInvalidate();
		}

	}

	public void setListener(DocListener listener) {
		this.listener = listener;
	}

	public void updateLectureStateGranted(boolean flag) {
		isLectureStateGranted = flag;
		if (isLectureStateGranted) {
			mShareDocButton.setVisibility(View.VISIBLE);
		} else {
			mShareDocButton.setVisibility(View.GONE);
			mShareDocCloseButton.setVisibility(View.GONE);
		}
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
			}
		}

		Canvas canvas = new Canvas(mImageViewBitmap);
		Paint paint = new Paint();
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		canvas.drawRect(new Rect(0, 0, mImageViewBitmap.getWidth(),
				mImageViewBitmap.getHeight()), paint);

		Paint p = new Paint();
		if (shareDocBm != null && !shareDocBm.isRecycled()) {
			canvas.drawBitmap(shareDocBm, 0, 0, p);
		}
		// draw shape must after share doc, because we make sure shape be front
		// of shared doc
		if (shapesBm != null && !shapesBm.isRecycled()) {
			canvas.drawBitmap(shapesBm, 0, 0, p);
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
		if ("float".equals(mFixedPosButton.getTag())) {
			return;
		}

		if (this.listener != null) {
			this.listener.requestDocViewFloatLayout(rootView);
		}

		mFixedPosButton.setTag("float");
	}

	/**
	 * Used to manually fixed FloatLayout, Because when this layout will hide,
	 * call this function to inform interface
	 */
	public void requestFixedLayout() {
		// Ignore same state
		if ("fix".equals(mFixedPosButton.getTag())) {
			return;
		}

		if (this.listener != null) {
			this.listener.requestDocViewFixedLayout(rootView);
		}

		mFixedPosButton.setTag("fix");
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
		if (mUpdateSizeButton.equals("fullscreen")) {
			return;
		}
		// restore image
		mUpdateSizeButton.setTag("fullscreen");
		mUpdateSizeButton
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
			addDocNameViewToDocListView(doc);
		}
	}

	/**
	 * Update page information according current document
	 */
	public void updateLayoutPageInformation() {
		if (mCurrentDoc == null) {
			mDocPageNumberTV.setText("");
		} else {
			int activtePageNo = mCurrentDoc.getActivatePageNo();
			int pageSize = mCurrentDoc.getPageSize();
			if (activtePageNo > pageSize) {
				mDocPageNumberTV.setText(pageSize + "/" + pageSize);
			} else {
				mDocPageNumberTV.setText(activtePageNo + "/" + pageSize);
			}

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
			mDocListButton.setEnabled(false);
			((ImageView) mDocListButton)
					.setImageResource(R.drawable.video_show_doc_button_gray);
			if (mDocListWindow != null && mDocListWindow.isShowing()) {
				mDocListWindow.dismiss();
			}
			mNextPageButton.setEnabled(false);
			mPrePageButton.setEnabled(false);

		} else {
			updatePageButton();
			mDocListButton.setEnabled(true);
			((ImageView) mDocListButton)
					.setImageResource(R.drawable.video_show_doc_button_selector);
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

	public void drawShape(final List<V2ShapeMeta> list) {
		if (list == null) {
			V2Log.w(" shape list is null");
			return;
		}

		// If background bitmap doesn't exist. means doesn't download picture
		// from server yet.
		// we send delay message until mBackgroundBitMap is created.
		if (mBackgroundBitMap == null) {
			mTimeHanlder.postDelayed(new Runnable() {

				@Override
				public void run() {
					drawShape(list);
				}

			}, 1000);
			return;
		}

		if (mShapeBitmap == null || mShapeBitmap.isRecycled()) {
			mShapeBitmap = Bitmap.createBitmap(mBackgroundBitMap.getWidth(),
					mBackgroundBitMap.getHeight(), Config.ARGB_8888);
		}

		Canvas cv = new Canvas(mShapeBitmap);
		cv.concat(matrix);
		for (V2ShapeMeta meta : list) {
			meta.draw(cv);
		}

		mergeBitmapToImage(mBackgroundBitMap, mShapeBitmap);

		// mDocDisplayContainer.postInvalidate();
	}

	/**
	 */
	public void cleanCache() {
		mDocDisplayContainer.removeAllViews();

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
		return "fullscreen".equals(mUpdateSizeButton.getTag());
	}

	private class ShareDocCloseButtonOnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			Log.i("20150119 1", "onShareDocCloseButtonClick()");
			v.setVisibility(View.GONE);
			// 关闭当前文档
			listener.requestShareDocClose(v);
		}
	}

	private class TouchImageViewGestureDetectorListener implements
			GestureDetector.OnDoubleTapListener {

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			if (isLectureStateGranted) {
				if (mShareDocCloseButton.getVisibility() == View.VISIBLE) {
					mShareDocCloseButton.setVisibility(View.GONE);
				} else {
					mShareDocCloseButton.setVisibility(View.VISIBLE);
				}
			}
			return false;
		}

		@Override
		public boolean onDoubleTapEvent(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}
	}

	private class TurnPageButtonOnClickListener implements OnClickListener {

		@Override
		public void onClick(View view) {
			if (mCurrentDoc == null) {
				V2Log.w(" mCurrentDoc is null  doc is not selected yet");
				return;
			}
			if (view == mNextPageButton) {
				if (!isLectureStateGranted) {
					((ConferenceActivity) mContext)
							.setNeedToFollowThePage(true);
				} else {
					((ConferenceActivity) mContext)
							.setNeedToFollowThePage(false);
				}
				if (mCurrentDoc.getActivatePageNo() < mCurrentDoc.getPageSize()) {
					mCurrentDoc.setActivatePageNo(mCurrentDoc
							.getActivatePageNo() + 1);
					updateCurrentDoc();
					if (listener != null) {
						listener.updateDoc(mCurrentDoc,
								mCurrentDoc.getActivatePage());
					}
				}
			} else if (view == mPrePageButton) {
				if (!isLectureStateGranted) {
					((ConferenceActivity) mContext)
							.setNeedToFollowThePage(true);
				} else {
					((ConferenceActivity) mContext)
							.setNeedToFollowThePage(false);
				}
				if (mCurrentDoc.getActivatePageNo() > 1) {
					mCurrentDoc.setActivatePageNo(mCurrentDoc
							.getActivatePageNo() - 1);
					updateCurrentDoc();
					if (listener != null) {
						listener.updateDoc(mCurrentDoc,
								mCurrentDoc.getActivatePage());
					}
				}
			} else {
				V2Log.e(" Invalid click listener");
			}
		}

	};

	private class DocListButtonOnClickListener implements OnClickListener {

		@Override
		public void onClick(View view) {
			showDocListPopWindow(view);
		}

		private void showDocListPopWindow(View anchor) {
			if (mDocs.isEmpty()) {
				// TODO prompt doc list is empty
				return;
			}

			if (mDocListWindow == null) {
				LayoutInflater inflater = (LayoutInflater) LeftShareDocLayout.this
						.getContext().getSystemService(
								Context.LAYOUT_INFLATER_SERVICE);
				View view = inflater.inflate(R.layout.video_doc_list_layout,
						null);

				mDocListWindowScroller = (ScrollView) view
						.findViewById(R.id.video_doc_list_container_scroller);
				mDocListWindow = new PopupWindow(view,
						(int) (rootView.getWidth() * 0.5),
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

				mDocListView = (LinearLayout) view
						.findViewById(R.id.video_doc_list_container);
				View currActivateView = null;
				for (String key : mDocs.keyOrderList()) {
					View v = addDocNameViewToDocListView(mDocs.get(key));
					if (mDocs.get(key) == mCurrentDoc) {
						currActivateView = v;
					}
				}

				view.measure(mDocDisplayContainer.getMeasuredWidth(),
						mDocDisplayContainer.getMeasuredHeight());
				mDocListWindowScroller.measure(View.MeasureSpec.UNSPECIFIED,
						View.MeasureSpec.UNSPECIFIED);
				mDocListView.measure(View.MeasureSpec.UNSPECIFIED,
						View.MeasureSpec.UNSPECIFIED);
				if (currActivateView != null) {
					currActivateView.measure(View.MeasureSpec.EXACTLY,
							View.MeasureSpec.EXACTLY);
					mDocListWindowScroller.computeScroll();
					mDocListWindowScroller.scrollTo(0,
							currActivateView.getBottom());
				}
			}
			if (mDocListWindow.isShowing()) {
				mDocListWindow.dismiss();
			} else {
				int[] l = new int[2];
				anchor.getLocationInWindow(l);

				mDocListWindow.getContentView().measure(
						mDocDisplayContainer.getMeasuredWidth(),
						mDocDisplayContainer.getMeasuredHeight());

				mDocListWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, l[0],
						l[1] - mDocListWindow.getHeight());

				moveToShowedTab();
			}
		}

	};

	private class DocListItemOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			if (mSyncStatus) {
				Toast.makeText(getContext(), R.string.warning_under_sync,
						Toast.LENGTH_SHORT).show();
				return;
			}
			V2Doc v2Doc = (V2Doc) v.getTag();
			if (v2Doc == null) {
				return;
			}
			if (v2Doc != mCurrentDoc) {
				V2Doc.Page p = v2Doc.getActivatePage();

				for (int i = 0; i < mDocListView.getChildCount(); i++) {
					View childView = mDocListView.getChildAt(i);
					if (childView instanceof TextView) {
						if (childView.getTag() != null) {
							childView.setBackgroundColor(Color.WHITE);
						} else {
							continue;
						}
					}
				}

				v.setBackgroundColor(getResources().getColor(
						R.color.in_meeting_doc_list_activited_doc_bg));

				mCurrentDoc = v2Doc;
				if (mCurrentDoc != null) {
					mDocTitleView.setText(mCurrentDoc.getDocName());
				} else {
					mDocTitleView.setText(R.string.confs_doc);
				}
				updateCurrentDoc();

				mDocListWindow.dismiss();

				if (p == null
						|| ((p.getFilePath() == null || p.getFilePath()
								.isEmpty()) && (v2Doc.getDocType() != V2Doc.DOC_TYPE_BLANK_BOARD))) {
					// Toast.makeText(getContext(),
					// R.string.warning_downloading_doc,
					// Toast.LENGTH_SHORT).show();

					// return;
				} else {

					if (listener != null) {
						listener.updateDoc(v2Doc, v2Doc.getActivatePage());
					}
				}

				if (!isLectureStateGranted) {
					((ConferenceActivity) mContext)
							.setNeedToFollowThePage(true);
				} else {
					((ConferenceActivity) mContext)
							.setNeedToFollowThePage(false);
				}
			}
		}

	};

	private class FixedPosButtonOnClickListener implements OnClickListener {

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

	private class ShareDocButtonOnClickListener implements OnClickListener {

		@Override
		public void onClick(View view) {
			if (listener != null) {
				listener.requestShareImageDoc(view);
			}
		}
	};

	private class UpdateSizeButtonOnClickListener implements OnClickListener {

		@Override
		public void onClick(View view) {
			if (view.getTag().equals("fullscreen")) {
				view.setTag("restorescreen");
				mUpdateSizeButton
						.setImageResource(R.drawable.video_doc_full_screen_button_selector);
			} else {
				view.setTag("fullscreen");
				mUpdateSizeButton
						.setImageResource(R.drawable.video_doc_restore_screen_button_selector);
			}

			if (view.getTag().equals("fullscreen")) {
				if (listener != null) {
					listener.requestDocViewFillParent(rootView);
					if (mDocDisplayContainer.getChildCount() > 0) {
						View child = mDocDisplayContainer.getChildAt(0);
						if(child instanceof TouchImageView){
							TouchImageView tiv = (TouchImageView) mDocDisplayContainer
									.getChildAt(0);
							tiv.setZoom(tiv.getCurrentZoom());
						}
					}
				}
			} else {
				if (listener != null) {
					listener.requestDocViewRestore(rootView);
					if (mDocDisplayContainer.getChildCount() > 0) {
						View child = mDocDisplayContainer.getChildAt(0);
						if(child instanceof TouchImageView){
							TouchImageView tiv = (TouchImageView) mDocDisplayContainer
									.getChildAt(0);
							tiv.setZoom(tiv.getCurrentZoom());
						}
					}
				}
			}

		}

	};
}
