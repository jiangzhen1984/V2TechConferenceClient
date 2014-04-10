package com.v2tech.view.conference;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.util.V2Log;
import com.v2tech.view.vo.V2Doc;

public class VideoDocLayout extends LinearLayout {

	private DocListener listener;

	private Map<String, V2Doc> mDocs;

	private Bitmap mCurrentBitMap;

	private FrameLayout container;

	private V2Doc mCurrentDoc;

	private V2Doc.Page mCurrentPage;

	public interface DocListener {
		public void updateDoc(V2Doc doc, V2Doc.Page p);
	};

	public VideoDocLayout(Context context) {
		super(context);
		initLayout();
	}

	public VideoDocLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		initLayout();
	}

	public VideoDocLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initLayout();
	}

	private void initLayout() {
		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.video_doc_layout, null, false);
		container = (FrameLayout) view.findViewById(R.id.video_doc_container);

		this.addView(view, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT));

		mDocs = new HashMap<String, V2Doc>();

	}

	public void updateCurrentDoc() {

		if (mDocs.size() <= 0) {
			// TODO update icon
			return;
		}

		if (mCurrentDoc == null) {
			mCurrentDoc = mDocs.entrySet().iterator().next().getValue();
		}

		if (mCurrentPage == null) {
			mCurrentPage = mCurrentDoc.getActivatePage();
		}

		updateCurrentDocPage(mCurrentPage);
	}

	private void updateCurrentDocPage(V2Doc.Page p) {
		if (p == null) {
			return;
		}
		if (mCurrentBitMap != null && !mCurrentBitMap.isRecycled()) {
			mCurrentBitMap.recycle();
			mCurrentBitMap = null;
		}
		if (p.getFilePath() != null) {
			File f = new File(p.getFilePath());
			if (f.exists()) {
				BitmapFactory.Options ops = new BitmapFactory.Options();
				ops.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(p.getFilePath(), ops);
				ops.inJustDecodeBounds = false;
				BitmapFactory.Options opsNew = new BitmapFactory.Options();
				opsNew.inPurgeable = true;  
				opsNew.inInputShareable = true; 
				// TODO
				if (ops.outHeight < 600 || ops.outWidth < 1080) {
					opsNew.inSampleSize = 1;
				} else if (ops.outHeight < 1080 || ops.outWidth < 1920) {
					opsNew.inSampleSize = 2;
				} else if (ops.outHeight > 1080 || ops.outWidth > 1920) {
					opsNew.inSampleSize = 2;
				} else {
					opsNew.inSampleSize = 2;
				}

				mCurrentBitMap = BitmapFactory.decodeFile(p.getFilePath(), opsNew);
				container.removeAllViews();
				ImageView iv = new ImageView(this.getContext());
				iv.setImageBitmap(mCurrentBitMap);
				container.addView(iv, new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.WRAP_CONTENT,
						FrameLayout.LayoutParams.WRAP_CONTENT));
			} else {
				V2Log.e(" doc file doesn't exist " + f.getAbsolutePath());
			}
		}
	}

	public void setListener(DocListener listener) {
		this.listener = listener;
	}

	public void addDoc(V2Doc doc) {
		mDocs.put(doc.getId(), doc);
	}

}
