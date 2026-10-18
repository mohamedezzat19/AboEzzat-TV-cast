package com.aboezzat.castreceiver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    private TextView tvIp;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // الحاوية الرئيسية بملء الشاشة مع خلفية متدرجة فخمة
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        rootLayout.setPadding(60, 50, 60, 50);

        GradientDrawable backgroundGradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF0F2027, 0xFF203A43, 0xFF2C5364} // تدرج كحلي مريح وراقي
        );
        rootLayout.setBackground(backgroundGradient);

        // 1. أعلى الشاشة في المنتصف: أهلاً بك يا أبو عزت
        TextView tvWelcome = new TextView(this);
        tvWelcome.setText("أهلاً بك يا أبو عزت");
        tvWelcome.setTextSize(34);
        tvWelcome.setTextColor(0xFF00E676); // أخضر زمردي زاهي
        tvWelcome.setTypeface(null, Typeface.BOLD);
        tvWelcome.setGravity(Gravity.CENTER);
        tvWelcome.setPadding(0, 10, 0, 30);
        rootLayout.addView(tvWelcome);

        // بطاقة عرض بيانات الخادم والـ IP
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setGravity(Gravity.CENTER);
        cardLayout.setPadding(40, 25, 40, 25);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(0x33FFFFFF); // خلفية شبه شفافة أنيقة
        cardBg.setCornerRadius(20);
        cardLayout.setBackground(cardBg);

        tvIp = new TextView(this);
        tvIp.setText("عنوان الآي بي: " + getLocalIpAddress() + "  |  المنفذ: 8080");
        tvIp.setTextSize(20);
        tvIp.setTextColor(Color.WHITE);
        tvIp.setGravity(Gravity.CENTER);
        cardLayout.addView(tvIp);

        tvStatus = new TextView(this);
        tvStatus.setText("الخادم يعمل في الخلفية وجاهز لاستقبال الروابط فوراً");
        tvStatus.setTextSize(17);
        tvStatus.setTextColor(0xFFB0BEC5);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setPadding(0, 15, 0, 0);
        cardLayout.addView(tvStatus);

        rootLayout.addView(cardLayout);

        // زر إعادة تشغيل الخدمة المخصص للريموت
        Button btnRestart = new Button(this);
        btnRestart.setText("إعادة تشغيل الخادم");
        btnRestart.setTextSize(18);
        btnRestart.setTextColor(Color.WHITE);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(0xFF1E88E5);
        btnBg.setCornerRadius(15);
        btnRestart.setBackground(btnBg);
        btnRestart.setPadding(35, 15, 35, 15);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(0, 30, 0, 0);
        btnRestart.setLayoutParams(btnParams);
        btnRestart.setFocusable(true); // ليتعرف عليه ريموت الفاير ستيك بسهولة

        btnRestart.setOnClickListener(v -> {
            restartBackgroundService();
            tvIp.setText("عنوان الآي بي: " + getLocalIpAddress() + "  |  المنفذ: 8080");
            Toast.makeText(MainActivity.this, "تمت إعادة تشغيل الخادم وتحديث الآي بي", Toast.LENGTH_SHORT).show();
        });
        rootLayout.addView(btnRestart);

        // مسافة مرنة لدفع الجملة التحتية إلى أسفل الشاشة تماماً
        View spacer = new View(this);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        );
        spacer.setLayoutParams(spacerParams);
        rootLayout.addView(spacer);

        // 2. أسفل الشاشة في المنتصف بخط كبير ملء الصفحة: الذكر المبارك
        TextView tvZikr = new TextView(this);
        tvZikr.setText("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ");
        tvZikr.setTextSize(28);
        tvZikr.setTextColor(0xFFFFD54F); // ذهبي هادئ وواضح
        tvZikr.setTypeface(null, Typeface.BOLD);
        tvZikr.setGravity(Gravity.CENTER);
        tvZikr.setPadding(0, 20, 0, 15);
        rootLayout.addView(tvZikr);

        setContentView(rootLayout);

        // بدء الخدمة فور فتح الواجهة
        startBackgroundService();
    }

    private void startBackgroundService() {
        try {
            Intent serviceIntent = new Intent(this, HttpServerService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception ignored) {}
    }

    private void restartBackgroundService() {
        try {
            Intent serviceIntent = new Intent(this, HttpServerService.class);
            stopService(serviceIntent);
            startBackgroundService();
        } catch (Exception ignored) {}
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
