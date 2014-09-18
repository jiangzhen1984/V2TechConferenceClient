package com.v2tech.view.contacts.add;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.v2tech.db.DataBaseContext;
import com.v2tech.db.V2TechDBHelper;
import com.v2tech.service.GlobalHolder;
import com.v2tech.vo.AddFriendHistorieNode;
import com.v2tech.vo.User;

public class AddFriendHistroysHandler {
	public static final String tableName = "AddFriendHistories";

	// 多个别人加我未处理和多个我加别人未处理只留最后一条
	// 查询方法为查字段RemoteUserID和AddState
	// ====================================================================================
	// 别人加我

	// 1
	// 别人加我不需要验证（已加您为好友）:
	// OnAddGroupUserInfo ->2:30:<user account='wenzl'
	// authtype='1' birthday='2000-01-01' bsystemavatar='1' commentname=''
	// id='166' nickname='wenzl' sex='0'/>
	// 查别人加我，有验证为0则删，然后加。没则直接加。
	// 参看becomeFriendHanler---------------------------------------

	// 2
	// 别人加我需要验证:
	// OnInviteJoinGroup::==>groupType:2,groupInfo:,userInfo:<user
	// account='wenzl' authtype='1' bsystemavatar='1' email='' id='166'
	// nickname='wenzl' sex='0'/>,additInfo:验证
	public static void addMeNeedAuthentication(Context context,
			User remoteUser, String additInfo) {
		String sql = null;
		
		// 我加别人未处理的全删。
		// 别人加我未处理的也全删。
		sql = "delete from " + tableName + " where RemoteUserID="
				+ remoteUser.getmUserId() + " and " + "AddState=0";
		del(context, sql);

		// 加一条别人加我的未处理记录
		// 加入到数据库中
		AddFriendHistorieNode node = new AddFriendHistorieNode();
		User currentUser = GlobalHolder.getInstance().getCurrentUser();
		node.ownerUserID = currentUser.getmUserId();
		node.ownerAuthType = currentUser.getAuthtype();
		node.remoteUserID = remoteUser.getmUserId();
		node.fromUserID = node.remoteUserID;
		node.toUserID = node.ownerUserID;
		node.applyReason = additInfo;
		node.refuseReason = null;
		node.addState = 0;
		node.readState = 0;
		node.saveDate = System.currentTimeMillis() / 1000;

		sql = "insert into "
				+ tableName
				+ " (OwnerUserID,OwnerAuthType,RemoteUserID,FromUserID,ToUserID,ApplyReason,RefuseReason,AddState,SaveDate,ReadState) values("
				+ node.ownerUserID
				+ ","
				+ node.ownerAuthType
				+ ","
				+ node.remoteUserID
				+ ","
				+ node.fromUserID
				+ ","
				+ node.toUserID
				+ ","
				+ (node.applyReason == null || node.applyReason.equals("") ? "NULL"
						: "'" + node.applyReason + "'")
				+ ","
				+ (node.refuseReason == null || node.refuseReason.equals("") ? "NULL"
						: "'" + node.refuseReason + "'") + "," + node.addState
				+ "," + node.saveDate + "," + node.readState + ")";
		add(context, sql);

	}

	// 3
	// 别人加我需要验证同意:
	// acceptInviteJoinGroup(int groupType, long groupId,long nUserID);
	// OnAddGroupUserInfo ==>groupType:2,nGroupID:30,sXml:<user account='wenzl'
	// authtype='1' bsystemavatar='1' commentname='wenzl' email='' id='166'
	// nickname='wenzl' sex='0'/>
	// 查别人加我，有验证为1则改。
	// 参看becomeFriendHanler---------------------------------------

	// 4
	// 别人加我需要验证拒绝:
	// refuseInviteJoinGroup(int groupType, long
	// nGroupID,long nUserID, String reason);
	// 改（查别人加我未处理的）状态

	public static void addMeRefuse(Context context, long remoteUserId,
			String refuseReason) {
		String sql = null;
		// 查别人加我未处理的
		sql = "update " + tableName + " set AddState=2" + ",RefuseReason='"
				+ refuseReason + "'" + " where FromUserID=" + remoteUserId
				+ " and " + "AddState=0";
		update(context, sql);
	}

	// ====================================================================================
	// 我加别人

	// 5
	// 我加别人不需要验证（成为好友）:
	// 别人加我未处理的全删。
	// 我加别人未处理的也全删。
	// 加一条我加别人未处理记录
	// OnAddGroupUserInfo ==>groupType:2,nGroupID:30,sXml:<user account='wenzl'
	// address='' authtype='0' avatarlocation='' avatarname=''
	// birthday='2000-01-01' bsystemavatar='1' commentname='wenzl' email=''
	// fax='' homepage=''
	// id='166' job='' mobile='' nickname='wenzl' privacy='0' sex='0'
	// sign=''telephone=''/>
	// 查我加别人，有则改。
	// 参看becomeFriendHanler---------------------------------------
	public static void addOtherNoNeedAuthentication(Context context,
			User remoteUser) {
		addOtherNeedAuthentication(context, remoteUser, null);
	}

	// 6
	// 我加别人需要验证：
	// 别人加我未处理的全删。
	// 我加被人未处理的也全删。
	// 加一条我加别人未处理记录
	public static void addOtherNeedAuthentication(Context context,
			User remoteUser, String applyReason) {
		String sql = null;

		if (remoteUser == null) {
			return;
		}

		// 我加别人未处理的全删。
		// 别人加我未处理的也全删。

		sql = "delete from " + tableName + " where RemoteUserID="
				+ remoteUser.getmUserId() + " and " + "AddState=0";
		del(context, sql);

		// 加一条我加别人的未处理记录
		// 加入到数据库中
		AddFriendHistorieNode node = new AddFriendHistorieNode();
		User currentUser = GlobalHolder.getInstance().getCurrentUser();
		node.ownerUserID = currentUser.getmUserId();
		node.ownerAuthType = currentUser.getAuthtype();
		node.remoteUserID = remoteUser.getmUserId();
		node.fromUserID = node.ownerUserID;
		node.toUserID = node.remoteUserID;
		node.applyReason = applyReason;
		node.refuseReason = null;
		node.addState = 0;
		node.readState = 0;
		node.saveDate = System.currentTimeMillis() / 1000;

		sql = "insert into "
				+ tableName
				+ " (OwnerUserID,OwnerAuthType,RemoteUserID,FromUserID,ToUserID,ApplyReason,RefuseReason,AddState,SaveDate,ReadState) values("
				+ node.ownerUserID
				+ ","
				+ node.ownerAuthType
				+ ","
				+ node.remoteUserID
				+ ","
				+ node.fromUserID
				+ ","
				+ node.toUserID
				+ ","
				+ (node.applyReason == null || node.applyReason.equals("") ? "NULL"
						: "'" + node.applyReason + "'")
				+ ","
				+ (node.refuseReason == null || node.refuseReason.equals("") ? "NULL"
						: "'" + node.refuseReason + "'") + "," + node.addState
				+ "," + node.saveDate + "," + node.readState + ")";
		add(context, sql);

	}

	// 7
	// 我加别人需要验证被同意（成为好友）:
	// OnAcceptInviteJoinGroup ==>groupType:2,groupId:30,nUserID:166
	// OnAddGroupUserInfo ==>groupType:2,nGroupID:30,sXml:<user account='wenzl'
	// authtype='1' bsystemavatar='1' commentname='wenzl' email='' id='166'
	// nickname='wenzl' sex='0'/>
	// 查我加别人，有则改。
	// 参看becomeFriendHanler---------------------------------------

	// 8
	// 我加别人需要验证被拒绝:
	// OnRefuseInviteJoinGroup ==>groupType:2,nGroupID:0,nUserID:166,sxml:5555
	// 改（查我加别人未处理的）状态
	public static void addOtherRefused(Context context, long remoteUserId,
			String refuseReason) {
		String sql = null;
		// 查我加别人未处理的
		sql = "update " + tableName + " set AddState=2" + ",RefuseReason='"
				+ refuseReason + "'" + " where ToUserID=" + remoteUserId
				+ " and " + "AddState=0";
		update(context, sql);
	}

	public static void becomeFriendHanler(Context context, String userInfo) {
		String sql = null;
		String[] sqlArgs = null;
		Cursor cr = null;
		if (userInfo == null) {
			return;
		}
		// 解析出字段
		User remoteUser = User.fromXmlToUser(userInfo);
		boolean ret1 = false;
		boolean ret2 = false;
		// 别人加我的记录未处理的最多只有一条
		// 对应两种情况别人加我我不需验证，和需要验证并同意
		sql = "select * from " + tableName + " where RemoteUserID=?" + " and "
				+ "AddState=0";
		sqlArgs = new String[] { String.valueOf(remoteUser.getmUserId()) };
		cr = select(context, sql, sqlArgs);
		if (cr != null && cr.getCount() != 0) {
			ret1 = true;
		}

		sql = "delete from " + tableName + " where RemoteUserID="
				+ remoteUser.getmUserId() + " and " + "AddState=0" + " and "
				+ "(OwnerAuthType=0 or OwnerAuthType=2)";
		del(context, sql);

		sql = "update " + tableName + " set AddState=1"
				+ " where RemoteUserID=" + remoteUser.getmUserId() + " and "
				+ "AddState=0" + " and " + "OwnerAuthType=1";
		update(context, sql);

		// 我加别人的记录未处理的最多只有一条
		// 对应两种情况我加别人别人不需验证，和需要验证并同意，一样的处理
		sql = "select * from " + tableName + " where ToUserID=?" + " and "
				+ "AddState=0";
		sqlArgs = new String[] { String.valueOf(remoteUser.getmUserId()) };
		cr = select(context, sql, sqlArgs);
		if (cr != null && cr.getCount() != 0) {
			ret2 = true;
		}

		sql = "update " + tableName + " set AddState=1" + " where ToUserID="
				+ remoteUser.getmUserId() + " and " + "AddState=0";
		update(context, sql);

		if ((ret1 == false) && (ret2 == false)) {
			// 加一条别人加我为好友的记录
			AddFriendHistorieNode node = new AddFriendHistorieNode();
			User currentUser = GlobalHolder.getInstance().getCurrentUser();
			node.ownerUserID = currentUser.getmUserId();
			node.ownerAuthType = currentUser.getAuthtype();
			node.remoteUserID = remoteUser.getmUserId();
			node.fromUserID = node.remoteUserID;
			node.toUserID = node.ownerUserID;
			node.applyReason = null;
			node.refuseReason = null;
			node.addState = 1;
			node.readState = 0;
			node.saveDate = System.currentTimeMillis() / 1000;
			sql = "insert into "
					+ tableName
					+ " (OwnerUserID,OwnerAuthType,RemoteUserID,FromUserID,ToUserID,ApplyReason,RefuseReason,AddState,SaveDate,ReadState) values("
					+ node.ownerUserID
					+ ","
					+ node.ownerAuthType
					+ ","
					+ node.remoteUserID
					+ ","
					+ node.fromUserID
					+ ","
					+ node.toUserID
					+ ","
					+ (node.applyReason == null || node.applyReason.equals("") ? "NULL"
							: "'" + node.applyReason + "'")
					+ ","
					+ (node.refuseReason == null
							|| node.refuseReason.equals("") ? "NULL" : "'"
							+ node.refuseReason + "'") + "," + node.addState
					+ "," + node.saveDate + "," + node.readState + ")";
			add(context, sql);
		}
	}

	public static void add(Context context, String sql) {
		V2TechDBHelper dbHelper = new V2TechDBHelper(new DataBaseContext(
				context));
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		if (db == null) {
			return;
		}
		db.execSQL(sql);
	}

	public static void del(Context context, String sql) {
		V2TechDBHelper dbHelper = new V2TechDBHelper(new DataBaseContext(
				context));
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		if (db == null) {
			return;
		}
		db.execSQL(sql);
	}

	public static void update(Context context, String sql) {
		V2TechDBHelper dbHelper = new V2TechDBHelper(new DataBaseContext(
				context));
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		if (db == null) {
			return;
		}

		db.execSQL(sql);
	}

	public static Cursor select(Context context, String sql, String[] sqlArgs) {
		V2TechDBHelper dbHelper = new V2TechDBHelper(new DataBaseContext(
				context));
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		if (db == null) {
			return null;
		}

		return db.rawQuery(sql, sqlArgs);
	}

}
