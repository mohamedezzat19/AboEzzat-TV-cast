package com.aboezzat.castreceiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpServerService extends Service {

    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AboEzzat::CastLock");
                wakeLock.acquire();
            }
        } catch (Exception ignored) {}

        startForegroundServiceNotification();
        startServer();
    }

    private void startForegroundServiceNotification() {
        String channelId = "cast_service_permanent";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "AboEzzat Cast Service",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("AboEzzat Receiver")
                .setContentText("الخادم يعمل دائماً وجاهز لاستقبال الروابط")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .build();

        startForeground(1, notification);
    }

    private void startServer() {
        if (isRunning) return;
        isRunning = true;
        new Thread(() -> {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
                serverSocket = new ServerSocket(8080);
                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    handleClient(socket);
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void handleClient(Socket socket) {
        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                
                String line;
                int contentLength = 0;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    if (line.toLowerCase().startsWith("content-length:")) {
                        contentLength = Integer.parseInt(line.substring(15).trim());
                    }
                }

                StringBuilder body = new StringBuilder();
                if (contentLength > 0) {
                    char[] buffer = new char[contentLength];
                    int read = reader.read(buffer, 0, contentLength);
                    if (read > 0) {
                        body.append(buffer, 0, read);
                    }
                }

                String receivedText = body.toString().trim();

                // الرد فوراً لتأكيد الاستلام مع الموبايل
                OutputStream out = socket.getOutputStream();
                String httpResponse = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain; charset=UTF-8\r\n" +
                        "Content-Length: 2\r\n" +
                        "Connection: close\r\n\r\n" +
                        "OK";
                out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                out.flush();
                socket.close();

                // استخراج الرابط
                String extractedUrl = extractUrl(receivedText);
                if (extractedUrl != null && !extractedUrl.isEmpty()) {
                    Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(extractedUrl));
                    
                    // أعلام إجبار النظام على فتح الشاشة فوراً في الواجهة ومنع حجبها
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    // استخدام PendingIntent لتخطي قيود الخلفية في Fire OS
                    try {
                        PendingIntent pendingIntent = PendingIntent.getActivity(
                                getApplicationContext(),
                                0,
                                launchIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
                        );
                        pendingIntent.send();
                    } catch (Exception e) {
                        startActivity(launchIntent);
                    }
                }
            } catch (Exception ignored) {
                try {
                    socket.close();
                } catch (Exception ignored2) {}
            }
        }).start();
    }

    private String extractUrl(String text) {
        Pattern pattern = Pattern.compile("https?://\\S+");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startServer();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
