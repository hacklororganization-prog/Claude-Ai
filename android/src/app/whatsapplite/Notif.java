package app.whatsapplite;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;

/* بناء الإشعارات مع إنشاء قناة إشعارات عند أندرويد 8+ (API 26+).
   نستعمل reflection لأننا نُترجم مقابل مكتبة API 23 التي لا تحتوي NotificationChannel. */
public class Notif {

    static void ensureChannel(Context c, String id, String name, int importance) {
        if (Build.VERSION.SDK_INT < 26) return;
        try {
            Class<?> cc = Class.forName("android.app.NotificationChannel");
            Object ch = cc.getConstructor(String.class, CharSequence.class, int.class)
                    .newInstance(id, name, importance);
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationManager.class.getMethod("createNotificationChannel", cc).invoke(nm, ch);
        } catch (Exception e) {}
    }

    static Notification build(Context c, String channelId, String title, String text,
                              boolean ongoing, int priority, PendingIntent pi) {
        Notification.Builder b = new Notification.Builder(c)
                .setSmallIcon(c.getApplicationInfo().icon)
                .setContentTitle(title == null ? "WhatsApp Lite" : title)
                .setContentText(text)
                .setAutoCancel(!ongoing)
                .setOngoing(ongoing)
                .setPriority(priority);
        if (!ongoing) b.setDefaults(Notification.DEFAULT_ALL);
        if (pi != null) b.setContentIntent(pi);
        if (Build.VERSION.SDK_INT >= 26) {
            try { Notification.Builder.class.getMethod("setChannelId", String.class).invoke(b, channelId); }
            catch (Exception e) {}
        }
        return b.build();
    }
}
