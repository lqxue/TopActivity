package com.ls.tools.topactivity.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;
import com.ls.tools.topactivity.utils.DatabaseUtil;
import com.ls.tools.topactivity.utils.WindowUtil;
import com.ls.tools.topactivity.model.NotificationMonitor;
import com.ls.tools.topactivity.service.MonitoringService;
import com.ls.tools.topactivity.service.AccessibilityMonitoringService;

@TargetApi(Build.VERSION_CODES.N)
public class ShortcutHandlerActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DatabaseUtil.setDisplayWidth(MainActivity.getScreenWidth(this));

		if (!MainActivity.usageStats(this) || !Settings.canDrawOverlays(this)) {
			Intent intent = new Intent(this, MainActivity.class);
			intent.putExtra(MainActivity.EXTRA_FROM_QS_TILE, true);
			startActivity(intent);
			finish();
		} else if (AccessibilityMonitoringService.getInstance() == null && DatabaseUtil.hasAccess())
			startService(new Intent().setClass(this, AccessibilityMonitoringService.class));

		boolean isShow = !DatabaseUtil.isShowWindow();
		DatabaseUtil.setIsShowWindow(isShow);
		if (!isShow) {
			WindowUtil.dismiss(this);
			NotificationMonitor.showNotification(this, true);
		} else {
			WindowUtil.init(this);
			NotificationMonitor.showNotification(this, false);
			startService(new Intent(this, MonitoringService.class));
		}
		sendBroadcast(new Intent(MainActivity.ACTION_STATE_CHANGED));
		finish();
	}
}
