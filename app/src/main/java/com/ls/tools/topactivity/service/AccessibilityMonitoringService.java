package com.ls.tools.topactivity.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.accessibility.AccessibilityEvent;

import com.ls.tools.topactivity.model.NotificationMonitor;
import com.ls.tools.topactivity.utils.DatabaseUtil;
import com.ls.tools.topactivity.utils.WindowUtil;

import java.util.List;

public class AccessibilityMonitoringService extends AccessibilityService {
	private static AccessibilityMonitoringService sInstance;

	public static AccessibilityMonitoringService getInstance() {
		return sInstance;
	}

	public boolean isPackageInstalled(String packageName) {
		final PackageManager packageManager = getPackageManager();
		Intent intent = packageManager.getLaunchIntentForPackage(packageName);
		if (intent == null) {
			return false;
		}
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	public boolean isSystemClass(String className) {
		try {
			ClassLoader.getSystemClassLoader().loadClass(className);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		if (WindowUtil.viewAdded && DatabaseUtil.isShowWindow() && DatabaseUtil.hasAccess()) {
			String act1 = event.getClassName().toString();
			String act2 = event.getPackageName().toString();

			if (isSystemClass(act1))
				return;
			WindowUtil.show(this, act2, act1);
		}
	}

	@Override
	public void onInterrupt() {
		sInstance = null;
	}

	@Override
	protected void onServiceConnected() {
		sInstance = this;
		super.onServiceConnected();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		sInstance = null;
		WindowUtil.dismiss(this);
		NotificationMonitor.cancelNotification(this);
		sendBroadcast(new Intent(QuickSettingsService.ACTION_UPDATE_TITLE));
		return super.onUnbind(intent);
	}
}
