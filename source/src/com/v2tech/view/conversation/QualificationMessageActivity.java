package com.v2tech.view.conversation;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.view.JNIService;
import com.v2tech.view.group.CrowdApplicantDetailActivity;
import com.v2tech.vo.Crowd;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.VMessageQualification;
import com.v2tech.vo.VMessageQualificationApplicationCrowd;
import com.v2tech.vo.VMessageQualificationInvitationCrowd;

/**
 * FIXME should combine with AuthenticationActivity
 * 
 * @author jiangzhen
 * 
 */
public class QualificationMessageActivity extends Activity {

	private View mBackButton;

	private Context mContext;
	private ListView mMessageListView;
	private MessageAdapter adapter;

	private List<VMessageQualification> mMessageList;

	private CrowdGroupService crowdService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		setContentView(R.layout.message_authentication);
		// back button
		mBackButton = findViewById(R.id.message_back);
		mBackButton.setOnClickListener(mBackButtonListener);

		mMessageListView = (ListView) findViewById(R.id.message_authentication);
		mMessageList = new ArrayList<VMessageQualification>();
		adapter = new MessageAdapter();
		mMessageListView.setAdapter(adapter);
		mMessageListView.setOnItemClickListener(mItemClickLIstener);
		
		crowdService = new CrowdGroupService();
		loadMessage();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		crowdService.clearCalledBack();
	}

	private void loadMessage() {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				mMessageList.clear();
				List<VMessageQualification> list = MessageBuilder
						.queryQualMessageList(mContext, GlobalHolder
								.getInstance().getCurrentUser());
				if (list != null && list.size() > 0) {
					mMessageList.addAll(list);
				}

				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				adapter.notifyDataSetChanged();
			}

		}.execute();
	}

	private OnClickListener mBackButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			onBackPressed();
		}

	};
	
	
	private OnItemClickListener mItemClickLIstener = new OnItemClickListener () {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			VMessageQualification  msg = mMessageList.get(position);
			if (msg.getType() == VMessageQualification.Type.CROWD_INVITATION) {
				VMessageQualificationInvitationCrowd imsg = (VMessageQualificationInvitationCrowd)msg;
				
	
				Crowd crowd = new Crowd(imsg.getId(), imsg.getCrowdGroup().getOwnerUser(), imsg.getCrowdGroup().getName(),
						imsg.getCrowdGroup().getBrief());
				Intent i = new Intent(JNIService.JNI_BROADCAST_CROWD_INVATITION);
				i.addCategory(JNIService.JNI_ACTIVITY_CATEGROY);
				i.putExtra("crowd", crowd);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mContext.startActivity(i);
			} else if (msg.getType() == VMessageQualification.Type.CROWD_APPLICATION) {
				Intent i = new Intent();
				i.setClass(QualificationMessageActivity.this, CrowdApplicantDetailActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mContext.startActivity(i);
				
			}
			
		}
		
	};

	class MessageAdapter extends BaseAdapter {

		class ViewItem {
			ImageView mMsgBanneriv;
			TextView mNameTV;
			TextView mContentTV;
			TextView mRes;
			View mAcceptButton;
		}

		class AcceptedButtonTag {
			ViewItem item;
			VMessageQualification msg;
		}

		private LayoutInflater layoutInflater;

		public MessageAdapter() {
			layoutInflater = LayoutInflater.from(mContext);
		}

		@Override
		public int getCount() {
			return mMessageList.size();
		}

		@Override
		public Object getItem(int position) {
			return mMessageList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return mMessageList.get(position).getId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewItem item = null;
			if (convertView == null) {
				convertView = layoutInflater.inflate(
						R.layout.qualification_message_adapter_item, null);
				item = new ViewItem();
				item.mMsgBanneriv = (ImageView) convertView
						.findViewById(R.id.qualification_msg_image_view);
				item.mNameTV = (TextView) convertView
						.findViewById(R.id.qualification_msg_name);
				item.mContentTV = (TextView) convertView
						.findViewById(R.id.qualification_msg_content);
				item.mRes = (TextView) convertView
						.findViewById(R.id.qualification_msg_res);
				item.mAcceptButton = convertView
						.findViewById(R.id.qualification_msgconfirm_button);
			//	item.mAcceptButton.setOnClickListener(mAcceptButtonListener);
				convertView.setTag(item);
			} else {
				item = (ViewItem) convertView.getTag();
			}
			updateViewItem(mMessageList.get(position), item);

			return convertView;
		}

		private void updateViewItem(VMessageQualification msg, ViewItem item) {

			AcceptedButtonTag tag = null;
			if (item.mAcceptButton.getTag() == null) {
				tag = new AcceptedButtonTag();
				item.mAcceptButton.setTag(tag);
			} else {
				tag = (AcceptedButtonTag)item.mAcceptButton.getTag();
			}
			tag.item = item;
			tag.msg = msg;

			// to be invited
			if (msg.getType() == VMessageQualification.Type.CROWD_INVITATION) {
				VMessageQualificationInvitationCrowd vqic = (VMessageQualificationInvitationCrowd) msg;
				CrowdGroup cg = vqic.getCrowdGroup();
				item.mMsgBanneriv.setImageResource(R.drawable.chat_group_icon);
				item.mNameTV.setText(cg.getName());
				item.mContentTV.setText(vqic.getInvitationUser().getName()
						+ mContext.getText(R.string.crowd_invitation_content));
				// If current user is invitor
			} else if (msg.getType() == VMessageQualification.Type.CROWD_APPLICATION) {
				VMessageQualificationApplicationCrowd vqac = (VMessageQualificationApplicationCrowd) msg;
				if (vqac.getApplicant().getAvatarBitmap() != null) {
					item.mMsgBanneriv.setImageBitmap(vqac.getApplicant()
							.getAvatarBitmap());
				} else {
					item.mMsgBanneriv.setImageResource(R.drawable.avatar);
				}
				item.mNameTV.setText(vqac.getApplicant().getName());
				item.mContentTV.setText(mContext
						.getText(R.string.crowd_applicant_content));

			}

			if (msg.getQualState() == VMessageQualification.QualificationState.WAITING) {
				item.mRes.setVisibility(View.GONE);
				item.mAcceptButton.setVisibility(View.VISIBLE);

			} else if (msg.getQualState() == VMessageQualification.QualificationState.ACCEPTED) {
				item.mRes.setVisibility(View.VISIBLE);
				item.mAcceptButton.setVisibility(View.GONE);
				item.mRes.setText(R.string.crowd_invitation_accepted);
			} else if (msg.getQualState() == VMessageQualification.QualificationState.REJECT) {
				item.mRes.setVisibility(View.VISIBLE);
				item.mAcceptButton.setVisibility(View.GONE);
				item.mRes.setText(R.string.crowd_invitation_rejected);
			}
		}

		private OnClickListener mAcceptButtonListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				AcceptedButtonTag tag = (AcceptedButtonTag) v.getTag();
				tag.msg.setQualState(VMessageQualification.QualificationState.ACCEPTED);

				if (tag.msg.getType() == VMessageQualification.Type.CROWD_INVITATION) {
					CrowdGroup cg = ((VMessageQualificationInvitationCrowd) tag.msg)
							.getCrowdGroup();
					Crowd crowd = new Crowd(cg.getmGId(), cg.getOwnerUser(),
							cg.getName(), cg.getBrief());
					crowdService.acceptInvitation(crowd, null);
				} else if (tag.msg.getType() == VMessageQualification.Type.CROWD_APPLICATION) {
					VMessageQualificationApplicationCrowd vqac = ((VMessageQualificationApplicationCrowd) tag.msg);
					crowdService.acceptApplication(vqac.getCrowdGroup(),
							vqac.getApplicant(), null);
				}
				updateViewItem(tag.msg, tag.item);
			}

		};

	}
}
