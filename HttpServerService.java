package com.aboezzat.castreceiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class HttpServerService extends Service {

    private ServerSocket serverSocket;
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundServiceNotification();
        startServer();
    }

    private void startForegroundServiceNotification() {
        String channelId = "cast_service_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "AboEzzat Cast Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("AboEzzat Receiver شغال")
                .setContentText("في انتظار استقبال الروابط...")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build();

        startForeground(1, notification);
    }

    private void startServer() {
        isRunning = true;
        new Thread(() -> {
            try {
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

                String receivedUrl = body.toString().trim();

                // إرسال رد HTTP 200 OK للموبايل حتى لا يظهر خطأ
                OutputStream out = socket.getOutputStream();
                String httpResponse = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain; charset=UTF-8\r\n" +
                        "Content-Length: 2\r\n" +
                        "Connection: close\r\n\r\n" +
                        "OK";
                out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                out.flush();
                socket.close();

                // فتح الرابط على الفاير ستيك فوراً
                if (!receivedUrl.isEmpty() && (receivedUrl.startsWith("http://") || receivedUrl.startsWith("https://"))) {
                    Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(receivedUrl));
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launchIntent);
                }
            } catch (Exception ignored) {
                try {
                    socket.close();
                } catch (Exception ignored2) {}
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
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
