package com.ls.tools.topactivity.model;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.ls.tools.topactivity.R;
import com.ls.tools.topactivity.service.QuickSettingsService;
import com.ls.tools.topactivity.ui.MainActivity;
import com.ls.tools.topactivity.utils.DatabaseUtil;
import com.ls.tools.topactivity.utils.WindowUtil;

public class NotificationMonitor extends BroadcastReceiver {
	public static final int NOTIFICATION_ID = 696969691;
	private static String CHANNEL_ID;
	private static final int ACTION_STOP = 2;
	private static final String EXTRA_NOTIFICATION_ACTION = "command";
	public static NotificationCompat.Builder builder;
	public static NotificationManager notifManager;

	public static void showNotification(Context context, boolean isPaused) {
		if (!DatabaseUtil.isNotificationToggleEnabled()) {
			return;
		}
		notifManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CHANNEL_ID = context.getPackageName() + "_channel_007";
			CharSequence name = "活动信息";

			int importance = NotificationManager.IMPORTANCE_HIGH;
			NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
			mChannel.setDescription(context.getString(R.string.show_current_activity_info));
			mChannel.enableLights(false);
			mChannel.enableVibration(false);
			mChannel.setShowBadge(false);
			notifManager.createNotificationChannel(mChannel);
		}

		Intent intent = new Intent(context, MainActivity.class);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		stackBuilder.addParentStack(MainActivity.class);
		stackBuilder.addNextIntent(intent);
		PendingIntent pIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

		builder = new NotificationCompat.Builder(context, CHANNEL_ID)
				.setContentTitle(context.getString(R.string.is_running, context.getString(R.string.app_name)))
				.setSmallIcon(R.drawable.ic_shortcut).setPriority(NotificationCompat.PRIORITY_HIGH)
				.setContentText(context.getString(R.string.touch_to_open))
				.setColor(ContextCompat.getColor(context, R.color.layerColor)).setVisibility(NotificationCompat.VISIBILITY_SECRET)
				.setOngoing(!isPaused);

		builder.addAction(R.drawable.ic_launcher_foreground, context.getString(R.string.noti_action_stop),
				getPendingIntent(context, ACTION_STOP)).setContentIntent(pIntent);

		notifManager.notify(NOTIFICATION_ID, builder.build());
	}

	public static PendingIntent getPendingIntent(Context context, int command) {
		Intent intent = new Intent(context, NotificationMonitor.class);
		intent.setAction("com.ls.tools.topactivity.ACTION_NOTIFICATION_RECEIVER");
		intent.putExtra(EXTRA_NOTIFICATION_ACTION, command);
		return PendingIntent.getBroadcast(context, 0, intent, 0);
	}

	public static void cancelNotification(Context context) {
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(NOTIFICATION_ID);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		int command = intent.getIntExtra(EXTRA_NOTIFICATION_ACTION, -1);
		if (command == ACTION_STOP) {
			WindowUtil.dismiss(context);
			DatabaseUtil.setIsShowWindow(false);
			cancelNotification(context);
			context.sendBroadcast(new Intent(MainActivity.ACTION_STATE_CHANGED));
		}
		context.sendBroadcast(new Intent(QuickSettingsService.ACTION_UPDATE_TITLE));
	}
}
