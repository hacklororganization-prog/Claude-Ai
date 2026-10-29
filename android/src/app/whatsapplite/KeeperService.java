package app.whatsapplite;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

/* خدمة تعمل في المقدمة: تُبقي عملية التطبيق حيّة في الخلفية
   حتى يستمر اتصال الرسائل وتصلك الإشعارات بعد مغادرة التطبيق. */
public class KeeperService extends Service {

    static final int ID = 7;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent open = new Intent(this, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) piFlags |= 0x4000000; // FLAG_IMMUTABLE
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, piFlags);

        Notification n = new Notification.Builder(this)
                .setSmallIcon(getApplicationInfo().icon)
                .setContentTitle("WhatsApp Lite")
                .setContentText("يعمل في الخلفية لاستقبال الرسائل")
                .setPriority(Notification.PRIORITY_MIN)
                .setOngoing(true)
                .setContentIntent(pi)
                .build();

        startForeground(ID, n);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
