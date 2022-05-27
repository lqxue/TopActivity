package com.ls.tools.topactivity.service;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.ls.tools.topactivity.utils.DatabaseUtil;
import com.ls.tools.topactivity.ui.MainActivity;
import com.ls.tools.topactivity.utils.WindowUtil;
import com.ls.tools.topactivity.model.NotificationMonitor;

@TargetApi(Build.VERSION_CODES.N)
public class QuickSettingsTileService extends TileService {
	public static final String ACTION_UPDATE_TITLE = "com.ls.tools.topactivity.ACTION.UPDATE_TITLE";
	private UpdateTileReceiver mReceiver;

	public static void updateTile(Context context) {
		TileService.requestListeningState(context, new ComponentName(context, QuickSettingsTileService.class));
		context.sendBroadcast(new Intent(QuickSettingsTileService.ACTION_UPDATE_TITLE));
	}

	public void updateTile() {
		getQsTile().setState(DatabaseUtil.isShowWindow() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
		getQsTile().updateTile();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mReceiver = new UpdateTileReceiver();
	}

	@Override
	public void onTileAdded() {
		DatabaseUtil.setQSTileAdded(true);
		sendBroadcast(new Intent(MainActivity.ACTION_STATE_CHANGED));
	}

	@Override
	public void onTileRemoved() {
		super.onTileRemoved();
		DatabaseUtil.setQSTileAdded(false);
		sendBroadcast(new Intent(MainActivity.ACTION_STATE_CHANGED));
	}

	@Override
	public void onStartListening() {
		registerReceiver(mReceiver, new IntentFilter(ACTION_UPDATE_TITLE));
		super.onStartListening();
		updateTile();
	}

	@Override
	public void onStopListening() {
		unregisterReceiver(mReceiver);
		super.onStopListening();
	}

	@Override
	public void onClick() {
		if (DatabaseUtil.isShowWindow())
			return;
		if (!MainActivity.usageStats(this) || !Settings.canDrawOverlays(this)) {
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra(MainActivity.EXTRA_FROM_QS_TILE, true);
			startActivityAndCollapse(intent);
		} else {
			if (DatabaseUtil.hasAccess() && AccessibilityMonitoringService.getInstance() == null)
				startService(new Intent().setClass(this, AccessibilityMonitoringService.class));
			DatabaseUtil.setIsShowWindow(!DatabaseUtil.isShowWindow());
			if (DatabaseUtil.isShowWindow()) {
				if (WindowUtil.sWindowManager == null)
					WindowUtil.init(this);
				NotificationMonitor.showNotification(this, false);
				startService(new Intent(this, MonitoringService.class));
			} else {
				WindowUtil.dismiss(this);
				NotificationMonitor.showNotification(this, true);
			}
			sendBroadcast(new Intent(MainActivity.ACTION_STATE_CHANGED));
		}
	}

	class UpdateTileReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateTile();
		}
	}
}
