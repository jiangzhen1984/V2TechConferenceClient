package com.v2tech.view.conference;

import java.io.File;
import java.io.IOException;
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
import android.media.ExifInterface;
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
import com.v2tech.R;
import com.v2tech.util.BitmapUtil;
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

	private FrameLayout mDocDisplayContainer;

	private TextView mDocPageNumberTV;
	private TextView mDocTitleView;

	private ImageView mPrePageButton;

	private ImageView mNextPageButton;
	private PopupWindow mDocListWindow;
	private LinearLayout mDocListView;
	private View mShowDocListButton;
	private View mRequestFixedPosButton;
	private ImageView mRequestUpdateSizeButton;
	private View mSharedDocButton;

	private ScrollView mDocListWindowScroller;

	private boolean mSyncStatus;

	private V2Doc mCurrentDoc;

	private V2Doc.Page mCurrentPage;

	private Handler mTimeHanlder = new Handler();

	Context mContext = null;

	public interface DocListener {
		public void updateDoc(V2Doc doc, V2Doc.Page p);

		public void requestDocViewFixedLayout(View v);

		public void requestDocViewFloatLayout(View v);

		public void requestDocViewFillParent(View v);

		public void requestDocViewRestore(View v);

		public void requestShareImageDoc(View v);

	};

	public VideoDocLayout(Context context, Map<String, V2Doc> docs,
			String currentLecturerActivateDocId) {
		super(context);
		this.mContext = context;
		mDocs = new HashMap<String, V2Doc>();
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
		mPrePageButton = (ImageView) view
				.findViewById(R.id.video_doc_pre_button);
		mPrePageButton.setOnClickListener(pageChangeListener);
		mNextPageButton = (ImageView) view
				.findViewById(R.id.video_doc_next_button);
		mNextPageButton.setOnClickListener(pageChangeListener);
		mShowDocListButton = view.findViewById(R.id.video_doc_list_button);
		mShowDocListButton.setOnClickListener(showDocListListener);
		mRequestFixedPosButton = view.findViewById(R.id.video_doc_pin_button);
		mRequestFixedPosButton.setOnClickListener(mRequestFixedListener);
		mRequestUpdateSizeButton = (ImageView) view
				.findViewById(R.id.video_doc_screen_button);
		mRequestUpdateSizeButton.setOnClickListener(mUpdateSizeListener);

		mSharedDocButton = view.findViewById(R.id.video_doc_share_button);
		mSharedDocButton.setOnClickListener(mShareDocButtonListener);

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

	private void showDocListPopWindow(View anchor) {
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
			for (Entry<String, V2Doc> e : mDocs.entrySet()) {
				View v = addDocNameViewToDocListView(e.getValue());
				if (e.getValue() == mCurrentDoc) {
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
				mDocListWindowScroller
						.scrollTo(0, currActivateView.getBottom());
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

		docNameView.setOnClickListener(docListViewListener);
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

	int width;
	int height;
	int sampl;

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
			
			iv.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {

				@Override
				public boolean onSingleTapConfirmed(MotionEvent e) {
			           Log.i("20150112 2","onClick");
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
			});

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

				V2Log.d("updateCurrentDocPage --> doc file path : "
						+ p.getFilePath());

				recycleBitmap(mBackgroundBitMap);
				mBackgroundBitMap = BitmapFactory.decodeFile(p.getFilePath(),
						opsNew);

				Matrix m = new Matrix();
				m.postRotate(BitmapUtil.getBitmapRotation(p.getFilePath()));
				mBackgroundBitMap = Bitmap.createBitmap(mBackgroundBitMap, 0,
						0, mBackgroundBitMap.getWidth(),
						mBackgroundBitMap.getHeight(), m, true);

				dest.left = (src.right - opsNew.outWidth) / 2;
				dest.right = opsNew.outWidth;
				dest.top = (src.bottom - opsNew.outHeight) / 2;
				dest.bottom = opsNew.outHeight;
				matrix.postScale(opsNew.outWidth / src.right, opsNew.outHeight
						/ src.bottom);
				matrix.mapRect(dest, src);

				for (int i = 0; i < mDocDisplayContainer.getChildCount(); i++) {
					View v = mDocDisplayContainer.getChildAt(i);
					if (v instanceof TouchImageView) {
						((TouchImageView) v).setImageBitmap(null);
					}
				}
				mDocDisplayContainer.removeAllViews();
				TouchImageView iv = new TouchImageView(this.getContext());
				// Merge bitmap
				mImageViewBitmap = mergeBitmapToImage(mBackgroundBitMap,
						mShapeBitmap);
				if (mImageViewBitmap != null && !mImageViewBitmap.isRecycled()) {
					iv.setImageBitmap(mImageViewBitmap);
				} else {
					iv.setImageBitmap(null);
				}
				FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.MATCH_PARENT,
						FrameLayout.LayoutParams.MATCH_PARENT);
				mDocDisplayContainer.addView(iv, fl);
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

	public void requestShowSharedButton(boolean flag) {
		mSharedDocButton.setVisibility(flag ? View.VISIBLE : View.GONE);
	}

	/**
	 * Merge bitmaps to image view bitmap
	 * 
	 * @param shareDocBm
	 * @param shapesBm
	 */
	private Bitmap mergeBitmapToImage(Bitmap shareDocBm, Bitmap shapesBm) {
		// if (mImageViewBitmap == null || mImageViewBitmap.isRecycled()) {
		// if (shareDocBm != null && !shareDocBm.isRecycled()) {
		// mImageViewBitmap = Bitmap.createBitmap(shareDocBm.getWidth(),
		// shareDocBm.getHeight(), Config.ARGB_8888);
		// } else if (shapesBm != null && !shapesBm.isRecycled()) {
		// mImageViewBitmap = Bitmap.createBitmap(shapesBm.getWidth(),
		// shapesBm.getHeight(), Config.ARGB_8888);
		// } else {
		// V2Log.e(" No available bitmap");
		// return;
		// }
		// } else {
		// if (shareDocBm != null && !shareDocBm.isRecycled()) {
		// // Current image view bitmap is smaller than shared document
		// // bitmap, we have to create new one
		// if (mImageViewBitmap.getWidth() < shareDocBm.getWidth()
		// || mImageViewBitmap.getHeight() < shareDocBm
		// .getHeight()) {
		// mImageViewBitmap.recycle();
		// mImageViewBitmap = Bitmap.createBitmap(shareDocBm.getWidth(),
		// shareDocBm.getHeight(), Config.ARGB_8888);
		// // }
		// }
		// }

		if (shareDocBm != null && !shareDocBm.isRecycled()) {
			// if(mImageViewBitmap != null && !mImageViewBitmap.isRecycled())
			// mImageViewBitmap.recycle();
			recycleBitmap(mImageViewBitmap);
			mImageViewBitmap = Bitmap.createBitmap(shareDocBm.getWidth(),
					shareDocBm.getHeight(), Config.ARGB_8888);
		} else if (shapesBm != null && !shapesBm.isRecycled()) {
			// if(mImageViewBitmap != null && !mImageViewBitmap.isRecycled())
			// mImageViewBitmap.recycle();
			recycleBitmap(mImageViewBitmap);
			mImageViewBitmap = Bitmap.createBitmap(shapesBm.getWidth(),
					shapesBm.getHeight(), Config.ARGB_8888);
		} else {
			V2Log.e(" No available bitmap");
			return null;
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

		return mImageViewBitmap;
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
			mShowDocListButton.setEnabled(false);
			((ImageView) mShowDocListButton)
					.setImageResource(R.drawable.video_show_doc_button_gray);
			if (mDocListWindow != null && mDocListWindow.isShowing()) {
				mDocListWindow.dismiss();
			}
			mNextPageButton.setEnabled(false);
			mPrePageButton.setEnabled(false);

		} else {
			updatePageButton();
			mShowDocListButton.setEnabled(true);
			((ImageView) mShowDocListButton)
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
				((ConferenceActivity) mContext)
						.setCurrentAttendeeTurnedpage(true);
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
				((ConferenceActivity) mContext)
						.setCurrentAttendeeTurnedpage(true);
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

	private OnClickListener showDocListListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			showDocListPopWindow(view);
		}

	};

	private OnClickListener docListViewListener = new OnClickListener() {

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

	private OnClickListener mShareDocButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (listener != null) {
				listener.requestShareImageDoc(view);
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
					if (mDocDisplayContainer.getChildCount() > 0) {
						TouchImageView tiv = (TouchImageView) mDocDisplayContainer
								.getChildAt(0);
						tiv.setZoom(tiv.getCurrentZoom());
						// tiv.setZoom(2F);
					}
				}
			} else {
				if (listener != null) {
					listener.requestDocViewRestore(rootView);
					if (mDocDisplayContainer.getChildCount() > 0) {
						TouchImageView tiv = (TouchImageView) mDocDisplayContainer
								.getChildAt(0);
						tiv.setZoom(tiv.getCurrentZoom());
						// tiv.setZoom(1F);
					}
				}
			}

		}

	};
}
