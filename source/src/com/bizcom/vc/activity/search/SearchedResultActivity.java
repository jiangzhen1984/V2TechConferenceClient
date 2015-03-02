package com.bizcom.vc.activity.search;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

import com.V2.jni.ImRequest;
import com.V2.jni.util.V2Log;
import com.bizcom.bo.ConversationNotificationObject;
import com.bizcom.request.util.BitmapManager;
import com.bizcom.request.util.BitmapManager.BitmapChangedListener;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.service.JNIService;
import com.bizcom.vo.Conversation;
import com.bizcom.vo.Crowd;
import com.bizcom.vo.CrowdGroup;
import com.bizcom.vo.Group;
import com.bizcom.vo.SearchedResult;
import com.bizcom.vo.User;
import com.bizcom.vo.Group.GroupType;
import com.bizcom.vo.SearchedResult.SearchedResultItem;
import com.bizcom.vo.SearchedResult.Type;
import com.v2tech.R;

public class SearchedResultActivity extends Activity {

	private Context mContext;
	private ListView mListView;
	
	private TextView mReturnButton;

	private SearchedResult sr;
	private List<SearchedResult.SearchedResultItem> mList;
	
	private LocalAdapter adapter;
	
	private LocalReceiver localReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.searched_result_activity);
		mContext = this;
		mListView = (ListView)findViewById(R.id.search_result_list_view);
		mListView.setOnItemClickListener(mItemClickListener);
		
		mReturnButton = (TextView) findViewById(R.id.search_title_return_button);
		mReturnButton.setOnClickListener(mReturnButtonListener);
		
		adapter = new LocalAdapter();
		if (getIntent().getExtras() != null) {
			sr = (SearchedResult)getIntent().getExtras().get("result");
			if (sr != null) {
				mList = sr.getList();
				mListView.setAdapter(adapter);
			}
		}
		initReceiver();
		// Register listener for avatar changed
		BitmapManager.getInstance().registerBitmapChangedListener(
				listener);
		overridePendingTransition(R.anim.left_in, R.anim.left_out);
	}
	
	private void initReceiver() {
		localReceiver = new LocalReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(JNIService.JNI_BROADCAST_GROUP_UPDATED);
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		this.registerReceiver(localReceiver, filter);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.right_in, R.anim.right_out);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(localReceiver);
		BitmapManager.getInstance().unRegisterBitmapChangedListener(listener);
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
				long cid = item.id;
				Intent i = new Intent();
				CrowdGroup crowd = (CrowdGroup)GlobalHolder.getInstance().getGroupById(GroupType.CHATING.intValue(), cid);
				if (crowd == null) {
					i.setAction(PublicIntent.SHOW_CROWD_APPLICATION_ACTIVITY);
					User u = new User(item.creator, item.creatorName);
					Crowd cr = new Crowd(item.id, u, item.name, item.brief);
					cr.setAuth(item.authType);
					i.putExtra("crowd", cr);
					//set disable authentication
					i.putExtra("authdisable", false);
				} else {
					i.putExtra("obj", new ConversationNotificationObject(
							Conversation.TYPE_GROUP, crowd.getmGId()));
					i.setAction(PublicIntent.START_CONVERSACTION_ACTIVITY);
				}
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				startActivity(i);
			} else if (item.mType == SearchedResult.Type.USER) {
				boolean isGetInfo = false;
				List<Group> contactsList = GlobalHolder.getInstance().getGroup(GroupType.ORG.intValue());
				User u = new User(item.id);
				for (int i = 0; i < contactsList.size(); i++) {
					Group g = contactsList.get(i);
					if (g.findUser(u) != null) {
						isGetInfo = true;
					}
				}
				
				if(u.getmUserId() == GlobalHolder.getInstance().getCurrentUserId())
					isGetInfo = true;
				
				if(!isGetInfo) {
					Log.i("20150203 1","5");
                    ImRequest.getInstance().proxy.getUserBaseInfo(item.id);
                    Intent intent = new Intent(PublicIntent.SHOW_CONTACT_DETAIL_DETAIL_ACTIVITY);
                    intent.addCategory(PublicIntent.DEFAULT_CATEGORY);
                    intent.putExtra("uid", u.getmUserId());
                    intent.putExtra("fromActivity" , "SearchedResultActivity");
                    startActivity(intent);
                }
				else {
                    Intent intent = new Intent(PublicIntent.SHOW_CONTACT_DETAIL_ACTIVITY);
                    intent.addCategory(PublicIntent.DEFAULT_CATEGORY);
                    intent.putExtra("uid", u.getmUserId());
                    startActivity(intent);
                }
			}
		}
		
	};
	
	class LocalReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(JNIService.JNI_BROADCAST_GROUP_UPDATED)) {
				long gid = intent.getLongExtra("gid", 0);
				for (int i = 0; i < mList.size(); i++) {
					SearchedResultItem item = mList.get(i);
					if(item.mType == Type.CROWD && item.id == gid){
						Group searchGroup = GlobalHolder.getInstance().getGroupById(item.id);
						if(searchGroup != null){
							item.name = searchGroup.getName();
							adapter.notifyDataSetChanged();
						}
						break;
					}
				}
			}
		}
	}

	class LocalAdapter extends BaseAdapter {

		ViewItem item = null;
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
			item = null;
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
				item.iv.setImageResource(R.drawable.chat_group_icon);
			} else if (srItem.mType == SearchedResult.Type.USER) {
				String avatarPath = GlobalHolder.getInstance().getAvatarPath(srItem.id);
				if(TextUtils.isEmpty(avatarPath))
					item.iv.setImageResource(R.drawable.avatar);
				else {
					V2Log.e("test", " uid : " + srItem.id + " avatarPath : " + avatarPath);
					BitmapManager.getInstance().loadBitmapFromPath(BitmapManager.getInstance().
							new LoadBitmapCallBack(avatarPath , item.iv) {
						
						@Override
						public void bitmapCallBack(ImageView iv , Bitmap bitmap) {
							//FIXME There is a warning to deal with
							try{
								iv.setImageBitmap(bitmap);
							}
							catch(Exception e){}
						}
					});
				}
			}
			return view;
		}
		
	}
	
	private BitmapChangedListener listener = new BitmapChangedListener() {
		
		@Override
		public void notifyAvatarChanged(User user, Bitmap bm) {
			if(user == null || bm == null)
				return ;
			for (SearchedResultItem item : mList) {
				if(item.mType == Type.USER){
					adapter.notifyDataSetChanged();
					break;
				}
			}
		}
	};
}
