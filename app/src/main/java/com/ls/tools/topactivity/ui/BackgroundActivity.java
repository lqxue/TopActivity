package com.ls.tools.topactivity.ui;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.content.ClipboardManager;
import android.content.ClipData;
import androidx.appcompat.app.AppCompatActivity;
import com.ls.tools.topactivity.App;

@TargetApi(Build.VERSION_CODES.O)
public class BackgroundActivity extends AppCompatActivity {
    public static String STRING_COPY = "com.ls.tools.topactivity.COPY_STRING";
    public static String COPY_MSG = "com.ls.tools.topactivity.COPY_STRING_MSG";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String str = getIntent().getStringExtra(STRING_COPY);
        String msg = getIntent().getStringExtra(STRING_COPY);
        
        if (str != null) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = new ClipData(ClipData.newPlainText("", str));
            clipboard.setPrimaryClip(clip);
            App.showToast(msg, 0);
        }
        finish();
    }
}
