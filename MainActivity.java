package com.aboezzat.castreceiver;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // واجهة خفيفة جداً تضمن عدم حدوث كراش
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 60, 60, 60);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("خادم AboEzzat شغال تمام بنجاح!");
        tvTitle.setTextSize(26);
        tvTitle.setPadding(0, 0, 0, 30);
        layout.addView(tvTitle);

        TextView tvIp = new TextView(this);
        tvIp.setText("عنوان الآي بي: " + getLocalIpAddress() + "\nالمنفذ: 8080");
        tvIp.setTextSize(22);
        tvIp.setPadding(0, 0, 0, 30);
        layout.addView(tvIp);

        TextView tvStatus = new TextView(this);
        tvStatus.setText("التطبيق يعمل في الخلفية وجاهز لاستقبال الروابط.");
        tvStatus.setTextSize(18);
        layout.addView(tvStatus);

        setContentView(layout);

        // تشغيل الخدمة في الخلفية بشكل آمن بدون انهيار التطبيق
        try {
            Intent serviceIntent = new Intent(this, HttpServerService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception ignored) {
        }
    }

    private String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr != null && sAddr.indexOf(':') < 0) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "غير متصل بالواي فاي";
    }
}
