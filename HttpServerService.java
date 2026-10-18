package com.aboezzat.castreceiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;

public class HttpServerService extends Service {
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private static final int PORT = 8080;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundServiceNotification();
        startServer();
        return START_STICKY;
    }

    private void startServer() {
        if (isRunning) return;
        isRunning = true;

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                while (isRunning) {
                    Socket client = serverSocket.accept();
                    handleClient(client);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleClient(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());

            String line = in.readLine();
            if (line != null && line.startsWith("GET")) {
                // استخراج الرابط المرسل بعد علامة الاستفهام
                String requestPath = line.split(" ")[1];
                if (requestPath.contains("?")) {
                    String rawUrl = requestPath.substring(requestPath.indexOf("?") + 1);
                    String targetUrl = URLDecoder.decode(rawUrl, "UTF-8");
                    
                    // توجيه وتشغيل الرابط على الفاير ستيك
                    launchMedia(targetUrl);
                }

                // إرسال رد سريع بنجاح العملية
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: text/plain; charset=UTF-8");
                out.println();
                out.println("تم الاستلام بنجاح!");
                out.flush();
            }
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void launchMedia(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // 1. لو رابط يوتيوب -> SmartTube
        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            intent.setPackage("com.liskovsoft.videomanager"); // حزمة سمارت تيوب الأساسية
            try {
                startActivity(intent);
                return;
            } catch (Exception ignored) {
                // إذا لم يعثر على النسخة المستقرة، يحاول مع النسخة التجريبية (Beta)
                intent.setPackage("com.liskovsoft.smarttubetv.beta");
                try {
                    startActivity(intent);
                    return;
                } catch (Exception e) {
                    intent.setPackage(null); // يفتح بالاختيار العادي لو مش مثبت
                }
            }
        } 
        // 2. لو ملف فيديو مباشر -> VLC
        else if (url.contains(".mp4") || url.contains(".m3u8") || url.contains(".mkv")) {
            intent.setPackage("org.videolan.vlc");
            try {
                startActivity(intent);
                return;
            } catch (Exception e) {
                intent.setPackage(null);
            }
        }

        // 3. أي رابط آخر (فيسبوك، مواقع ويب) يفتح عبر المتصفح تلقائياً
        try {
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startForegroundServiceNotification() {
        String channelId = "CastServerChannel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "AboEzzat Cast Server", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, channelId)
                    .setContentTitle("خادم العرض جاهز")
                    .setContentText("في انتظار استقبال الروابط على المنفذ " + PORT)
                    .setSmallIcon(android.R.drawable.ic_menu_upload)
                    .build();
        }
        startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
