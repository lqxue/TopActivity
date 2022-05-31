package com.ls.tools.topactivity.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.ls.tools.topactivity.R;
import com.ls.tools.topactivity.model.NotificationMonitor;
import com.ls.tools.topactivity.service.QuickSettingsService;
import com.ls.tools.topactivity.ui.BackgroundActivity;
import com.ls.tools.topactivity.ui.MainActivity;

public class WindowUtil {
	private static WindowManager.LayoutParams sWindowParams;
	public static WindowManager sWindowManager;
	private static View sView;
	private static int xInitCord = 0;
	private static int yInitCord = 0;
	private static int xInitMargin = 0;
	private static int yInitMargin = 0;
	private static String text, text1;
	private static TextView appName, packageName, className,uninstall_app;
	private static ClipboardManager clipboard;
	public static boolean viewAdded = false;

	public static void init(final Context context) {
		sWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

		sWindowParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
						: WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

		sWindowParams.gravity = Gravity.CENTER;
		sWindowParams.width = (DatabaseUtil.getDisplayWidth() / 2) + 300;
		sWindowParams.windowAnimations = android.R.style.Animation_Toast;

		sView = LayoutInflater.from(context).inflate(R.layout.window_tasks, null);
		clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
		uninstall_app = sView.findViewById(R.id.uninstall_app);
		appName = sView.findViewById(R.id.text);
		packageName = sView.findViewById(R.id.text1);
		className = sView.findViewById(R.id.text2);
		ImageView closeBtn = sView.findViewById(R.id.closeBtn);
		uninstall_app.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				toSelfSetting(context,packageName.getText().toString().trim());
			}
		});
		closeBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dismiss(context);
				DatabaseUtil.setIsShowWindow(false);
				NotificationMonitor.cancelNotification(context);
				context.sendBroadcast(new Intent(MainActivity.ACTION_STATE_CHANGED));
			}
		});

		appName.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				copyString(context, text, "App name copied");
			}
		});

		packageName.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
//				copyString(context, text, "Package name copied");
				toSelfSetting(context,packageName.getText().toString().trim());
			}
		});

		className.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				copyString(context, text1, "Class name copied");
			}
		});

		sView.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View view, MotionEvent event) {
				WindowManager.LayoutParams layoutParams = sWindowParams;

				int xCord = (int) event.getRawX();
				int yCord = (int) event.getRawY();
				int xCordDestination;
				int yCordDestination;
				int action = event.getAction();

				if (action == MotionEvent.ACTION_DOWN) {
					xInitCord = xCord;
					yInitCord = yCord;
					xInitMargin = layoutParams.x;
					yInitMargin = layoutParams.y;
				} else if (action == MotionEvent.ACTION_MOVE) {
					int xDiffMove = xCord - xInitCord;
					int yDiffMove = yCord - yInitCord;
					xCordDestination = xInitMargin + xDiffMove;
					yCordDestination = yInitMargin + yDiffMove;

					layoutParams.x = xCordDestination;
					layoutParams.y = yCordDestination;
					sWindowManager.updateViewLayout(view, layoutParams);
				}
				return true;
			}
		});
	}

	public static void toSelfSetting(Context context,String packageName) {

		Intent mIntent = new Intent();
		mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		if (Build.VERSION.SDK_INT >= 9) {
			mIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
			mIntent.setData(Uri.fromParts("package", packageName, null));
		} else if (Build.VERSION.SDK_INT <= 8) {
			mIntent.setAction(Intent.ACTION_VIEW);
			mIntent.setClassName("com.android.settings", "com.android.setting.InstalledAppDetails");
			mIntent.putExtra("com.android.settings.ApplicationPkgName", packageName);
		}
		context.startActivity(mIntent);

	}

	/**
	 * 卸载指定包名的应用
	 * @param packageName
	 */
	public static boolean uninstall(Context context,String packageName) {
		boolean b = checkApplication(context,packageName);
		if (b) {
			Uri packageURI = Uri.parse("package:".concat(packageName));
			Intent intent = new Intent(Intent.ACTION_DELETE);
			intent.setData(packageURI);
			context.startActivity(intent);
			return true;
		}
		return false;
	}

	/**
	 * 判断该包名的应用是否安装
	 *
	 * @param packageName
	 * @return
	 */
	private static boolean checkApplication(Context context,String packageName) {
		if (packageName == null || "".equals(packageName)) {
			return false;
		}
		try {
			context.getPackageManager().getApplicationInfo(packageName,
					PackageManager.MATCH_UNINSTALLED_PACKAGES);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
		}
		return false;
	}

	private static void copyString(Context context, String str, String msg) {
		if (Build.VERSION.SDK_INT < 29) {
			ClipData clip = ClipData.newPlainText("Current Activity", str);
			clipboard.setPrimaryClip(clip);
		} else {
			context.startActivity(
					new Intent(context, BackgroundActivity.class).putExtra(BackgroundActivity.STRING_COPY, str)
							.putExtra(BackgroundActivity.COPY_MSG, msg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		}
	}

	public static String getAppName(Context context, String pkg) {
		try {
			PackageManager pm = context.getPackageManager();
			return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
		} catch (Exception e) {
			// Ignored
			e.printStackTrace();
			return "";
		}
	}

	public static void show(Context context, String pkg, String clas) {
		if (sWindowManager == null) {
			init(context);
		}
		appName.setText(getAppName(context, pkg));
		packageName.setText(pkg);
		className.setText(clas);

		if (!viewAdded) {
			viewAdded = true;
			if (DatabaseUtil.isShowWindow()) {
				sWindowManager.addView(sView, sWindowParams);
			}
		}

		if (NotificationMonitor.builder != null) {
			NotificationMonitor.builder.setContentTitle(text);
			NotificationMonitor.builder.setContentText(text1);
			NotificationMonitor.notifManager.notify(NotificationMonitor.NOTIFICATION_ID,
					NotificationMonitor.builder.build());
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			QuickSettingsService.updateTile(context);
		}
	}

	public static void dismiss(Context context) {
		viewAdded = false;
		try {
			sWindowManager.removeView(sView);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			QuickSettingsService.updateTile(context);
		}
	}
}
