package com.ls.tools.topactivity.ui;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ls.tools.topactivity.R;
import com.ls.tools.topactivity.model.NotificationMonitor;
import com.ls.tools.topactivity.service.AccessibilityMonitoringService;
import com.ls.tools.topactivity.service.MonitoringService;
import com.ls.tools.topactivity.utils.DatabaseUtil;
import com.ls.tools.topactivity.utils.WindowUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;

public class MainActivity extends AppCompatActivity {
	private Activity activity = this;
	public static final String EXTRA_FROM_QS_TILE = "from_qs_tile";
	public static final String ACTION_STATE_CHANGED = "com.ls.tools.topactivity.ACTION_STATE_CHANGED";
	private Switch mWindowSwitch, mNotificationSwitch, mAccessibilitySwitch;
	private BroadcastReceiver mReceiver;
	public static MainActivity INSTANCE;
	private AlertDialog.Builder builder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		builder = new AlertDialog.Builder(activity);
		INSTANCE = this;
		if (AccessibilityMonitoringService.getInstance() == null && DatabaseUtil.hasAccess())
			startService(new Intent().setClass(this, AccessibilityMonitoringService.class));

		DatabaseUtil.setDisplayWidth(getScreenWidth(this));

		mWindowSwitch = findViewById(R.id.sw_window);
		mNotificationSwitch = findViewById(R.id.sw_notification);
		mAccessibilitySwitch = findViewById(R.id.sw_accessibility);
		if (Build.VERSION.SDK_INT < 24) {
			mNotificationSwitch.setVisibility(View.INVISIBLE);
			findViewById(R.id.divider_useNotificationPref).setVisibility(View.INVISIBLE);
		}

		mReceiver = new UpdateSwitchReceiver();
		registerReceiver(mReceiver, new IntentFilter(ACTION_STATE_CHANGED));

		mNotificationSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton button, boolean isChecked) {
				DatabaseUtil.setNotificationToggleEnabled(!isChecked);
			}
		});
		mAccessibilitySwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton button, boolean isChecked) {
				DatabaseUtil.setHasAccess(isChecked);
				if (isChecked && AccessibilityMonitoringService.getInstance() == null)
					startService(new Intent().setClass(MainActivity.this, AccessibilityMonitoringService.class));
			}
		});
		mWindowSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton button, boolean isChecked) {
				if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(MainActivity.this)) {
					AlertDialog.Builder builder = new AlertDialog.Builder(activity);
					builder.setTitle(R.string.dialog_overlay_permission_title)
							.setMessage(R.string.dialog_overlay_permission_content)
							.setNegativeButton(R.string.dialog_close, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									// User cancelled the dialog
									dialog.cancel();
								}
							})
							.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface di, int btn) {
									Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
									intent.setData(Uri.parse("package:" + getPackageName()));
									startActivity(intent);
									di.dismiss();
								}
							}).show();
					mWindowSwitch.setChecked(false);
				} else if (DatabaseUtil.hasAccess() && AccessibilityMonitoringService.getInstance() == null) {
					builder.setTitle(R.string.dialog_accessibility_permission_title).setMessage(
							R.string.dialog_accessibility_permission_content)
							.setNegativeButton(R.string.dialog_close, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									// User cancelled the dialog
									dialog.cancel();
								}
							})
							.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface di, int btn) {
									Intent intent = new Intent();
									intent.setAction("android.settings.ACCESSIBILITY_SETTINGS");
									startActivity(intent);
									di.dismiss();
								}
							}).show();
					mWindowSwitch.setChecked(false);
				} else if (!usageStats(MainActivity.this)) {
					builder.setTitle(R.string.dialog_usage_access_title).setMessage(
							R.string.dialog_usage_access_content)
							.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface di, int btn) {
									Intent intent = new Intent();
									intent.setAction("android.settings.USAGE_ACCESS_SETTINGS");
									startActivity(intent);
									di.dismiss();
								}
							}).show();
					mWindowSwitch.setChecked(false);
				} else {
					DatabaseUtil.setAppInitiated(true);
					DatabaseUtil.setIsShowWindow(isChecked);
					if (!isChecked) {
						WindowUtil.dismiss(MainActivity.this);
					} else {
						WindowUtil.show(MainActivity.this, getPackageName(), getClass().getName());
						startService(new Intent(MainActivity.this, MonitoringService.class));
					}
				}
			}
		});

		if (getIntent().getBooleanExtra(EXTRA_FROM_QS_TILE, false))
			mWindowSwitch.setChecked(true);
	}

	public static int getScreenWidth(Activity activity) {
		DisplayMetrics displayMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		return displayMetrics.widthPixels;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (getIntent().getBooleanExtra(EXTRA_FROM_QS_TILE, false)) {
			mWindowSwitch.setChecked(true);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshWindowSwitch();
		refreshNotificationSwitch();
		refreshAccessibilitySwitch();
		NotificationMonitor.cancelNotification(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (DatabaseUtil.isShowWindow()) {
			NotificationMonitor.showNotification(this, false);
		}
	}

	private void refreshWindowSwitch() {
		mWindowSwitch.setChecked(DatabaseUtil.isShowWindow());
		if (DatabaseUtil.hasAccess() && AccessibilityMonitoringService.getInstance() == null) {
			mWindowSwitch.setChecked(false);
		}
	}

	private void refreshAccessibilitySwitch() {
		mAccessibilitySwitch.setChecked(DatabaseUtil.hasAccess());
	}

	private void refreshNotificationSwitch() {
		mNotificationSwitch.setChecked(!DatabaseUtil.isNotificationToggleEnabled());
	}

	public void showToast(String str, int length) {
		Toast.makeText(this, str, length).show();
	}

	public String readFile(File file) {
		StringBuilder text = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = br.readLine();
			while (line != null) {
				text.append(line);
				text.append("\n");
				line = br.readLine();
			}

			new FileOutputStream(file).write(text.toString().getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return text.toString();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}

	class UpdateSwitchReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			refreshWindowSwitch();
			refreshNotificationSwitch();
			refreshAccessibilitySwitch();
		}
	}

	public static boolean usageStats(Context context) {
		boolean granted = false;
		AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
		int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(),
				context.getPackageName());

		if (mode == AppOpsManager.MODE_DEFAULT) {
			granted = (context.checkCallingOrSelfPermission(
					android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
		} else {
			granted = (mode == AppOpsManager.MODE_ALLOWED);
		}
		return granted;
	}

	public void setupBattery() {
		builder.setTitle("Battery Optimizations").setMessage(
				"Please remove battery optimization/restriction from this app in order to run in background with full functionality")
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface di, int btn) {
						di.dismiss();
						Intent intent = new Intent();
						intent.setAction("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS");
						intent.setData(Uri.parse("package:" + getPackageName()));
						startActivity(intent);
					}
				}).show();

	}
}
