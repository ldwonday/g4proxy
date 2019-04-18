package com.virjar.g4proxy.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;

import com.virjar.g4proxy.BuildConfig;
import com.virjar.g4proxy.R;
import com.virjar.g4proxy.client.G4ProxyClient;
import com.virjar.g4proxy.client.LittelProxyBootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

/**
 * Created by virjar on 2019/2/22.
 */

public class HttpProxyService extends Service {

    private static final Logger log = LoggerFactory.getLogger(HttpProxyService.class);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        startService();

        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startService();
        return START_STICKY;
    }

    private AtomicBoolean serviceStarted = new AtomicBoolean(false);

    private void startService() {
        if (serviceStarted.compareAndSet(false, true)) {
            Thread thread = new Thread("g4ProxyThread") {
                @Override
                public void run() {
                    startServiceInternal();
                }
            };
            thread.setDaemon(true);
            thread.start();

        }
    }

    private void setNotifyChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel notificationChannel = new NotificationChannel(BuildConfig.APPLICATION_ID,
                "channel", NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.YELLOW);
        notificationChannel.setShowBadge(true);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        manager.createNotificationChannel(notificationChannel);
    }

    private void startServiceInternal() {

        setNotifyChannel();

        Notification.Builder builder = new Notification.Builder(this.getApplicationContext()); //获取一个Notification构造器
        // 设置PendingIntent
        Intent nfIntent = new Intent(this, MainActivity.class);
        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, FLAG_UPDATE_CURRENT))
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher)) // 设置下拉列表中的图标(大图标)
                .setContentTitle("G4Proxy") // 设置下拉列表里的标题
                .setSmallIcon(R.mipmap.ic_launcher) // 设置状态栏内的小图标
                .setContentText("代理服务agent") // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(BuildConfig.APPLICATION_ID);
        }

        Notification notification = builder.build(); // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
        startForeground(110, notification);// 开始前台服务


        String clientKey = Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        //新的代理服务器实现
        LogbackConfig.config();

        G4ProxyClient g4ProxyClient = new G4ProxyClient("www.scumall.com", 50000, clientKey);
        g4ProxyClient.startup();
    }


}
