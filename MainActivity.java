package com.aboezzat.castreceiver;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // تشغيل خدمة الاستقبال في الخلفية فور فتح التطبيق
        Intent serviceIntent = new Intent(this, HttpServerService.class);
        startService(serviceIntent);

        // واجهة سريعة بدون ملفات XML معقدة
        TextView textView = new TextView(this);
        textView.setTextSize(24);
        textView.setPadding(50, 50, 50, 50);

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        textView.setText("خادم AboEzzat شغال تمام بنجاح!\n\n"
                + "عنوان الآي بي: " + ip + "\n"
                + "المنفذ: 8080\n\n"
                + "التطبيق يعمل في الخلفية وجاهز لاستقبال الروابط.");

        setContentView(textView);
    }
}
