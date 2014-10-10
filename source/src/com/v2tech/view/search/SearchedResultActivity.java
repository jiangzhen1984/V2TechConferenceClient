package com.v2tech.view.search;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.vo.SearchedResult;

public class SearchedResultActivity extends Activity {

	private Context mContext;
	private ListView mListView;
	
	private TextView mReturnButton;

	private SearchedResult sr;
	private List<SearchedResult.SearchedResultItem> mList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.searched_result_activity);
		mContext = this;
		mListView = (ListView)findViewById(R.id.search_result_list_view);
		mListView.setOnItemClickListener(mItemClickListener);
		
		mReturnButton = (TextView) findViewById(R.id.search_title_return_button);
		mReturnButton.setOnClickListener(mReturnButtonListener);
		if (getIntent().getExtras() != null) {
			sr = (SearchedResult)getIntent().getExtras().get("result");
			if (sr != null) {
				mList = sr.getList();
				mListView.setAdapter(new LocalAdapter());
			}
		}
		overridePendingTransition(R.animator.left_in, R.animator.left_out);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.animator.right_in, R.animator.right_out);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}



	private OnClickListener mReturnButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			onBackPressed();

		}

	};
	
	private OnItemClickListener mItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			SearchedResult.SearchedResultItem item = mList.get(position);
			if (item.mType == SearchedResult.Type.CROWD) {
				//CrowdApplicationActivity
			}
		}
		
	};

	private Handler mLocalHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			}
		}

	};
	
	
	class LocalAdapter extends BaseAdapter {

		class ViewItem {
			ImageView iv;
			TextView tv;
		}
		
		@Override
		public int getCount() {
			return mList.size();
		}

		@Override
		public Object getItem(int position) {
			return mList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			ViewItem item = null;
			if (view == null) {
				view = LayoutInflater.from(mContext).inflate(
						R.layout.searched_result_list_item, null,
						false);
				item = new ViewItem();
				item.iv = (ImageView)view.findViewById(R.id.searched_result_image);
				item.tv = (TextView)view.findViewById(R.id.searched_result_name);
				view.setTag(item);
			} else {
				item = (ViewItem)view.getTag();
			}
			SearchedResult.SearchedResultItem srItem = mList.get(position);
			item.tv.setText(srItem.name);
			if (srItem.mType == SearchedResult.Type.CROWD) {
				item.iv.setImageResource(R.drawable.conference_icon_self);
			} else if (srItem.mType == SearchedResult.Type.USER) {
				item.iv.setImageResource(R.drawable.avatar);
			}
			
			return view;
		}
		
	}


}
