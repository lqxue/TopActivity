package com.ls.tools.topactivity.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ls.tools.topactivity.R;

public class CrashActivity extends AppCompatActivity {
	public static String EXTRA_CRASH_INFO = "crash";
	private String crashInfo;
	private boolean restart;
	private Activity activity = this;
	private AlertDialog.Builder builder;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.crash_view);
		builder = new AlertDialog.Builder(activity);

		restart = getIntent().getBooleanExtra("Restart", true);
		String mLog = getIntent().getStringExtra(EXTRA_CRASH_INFO);
		crashInfo = mLog;
		TextView crashed = findViewById(R.id.crashed);
		crashed.setText(mLog);
	}

	@Override
	public void onBackPressed() {
		if (!restart) {
			finish();
			return;
		}
		builder.setTitle("Exit").setMessage("App will restart, are you sure to exit")
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface di, int btn) {
						di.dismiss();
						restart();
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface di, int btn) {
						di.dismiss();
					}
				}).setCancelable(false).show();
	}

	private void restart() {
		PackageManager pm = getPackageManager();
		Intent intent = pm.getLaunchIntentForPackage(getPackageName());
		if (intent != null) {
			intent.addFlags(
					Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(intent);
		}
		finish();
		android.os.Process.killProcess(android.os.Process.myPid());
		System.exit(0);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.copy) {
			ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			cm.setPrimaryClip(ClipData.newPlainText(getPackageName(), crashInfo));
			Toast.makeText(this, "Copied", Toast.LENGTH_LONG).show();
		} else if (item.getItemId() == android.R.id.redo) {
			onBackPressed();
		}
		return super.onOptionsItemSelected(item);
	}
}
