package com.pocketledger.app;
import android.app.*;
import android.content.*;
import android.os.Build;
import android.content.pm.PackageManager;
import java.time.*;

public class ReminderReceiver extends BroadcastReceiver {
    static final String CHANNEL="allowance";
    static void schedule(Context context) {
        if(!context.getSharedPreferences("ledger",Context.MODE_PRIVATE).getBoolean("reminder",false))return;
        ZoneId zone=ZoneId.of("Europe/Amsterdam");
        ZonedDateTime now=ZonedDateTime.now(zone);
        ZonedDateTime next=now.withDayOfMonth(25).toLocalDate().atStartOfDay(zone);
        if(!next.isAfter(now)) next=next.plusMonths(1);
        AlarmManager manager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pending=PendingIntent.getBroadcast(context,25,new Intent(context,ReminderReceiver.class).setAction("com.pocketledger.ALLOWANCE"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(Build.VERSION.SDK_INT<31||manager.canScheduleExactAlarms())manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.toInstant().toEpochMilli(),pending);
        else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.toInstant().toEpochMilli(),pending);
    }
    @Override public void onReceive(Context context,Intent intent) {
        if("com.pocketledger.ALLOWANCE".equals(intent.getAction())){
            NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Monthly allowance reminder",NotificationManager.IMPORTANCE_DEFAULT));
            if(Build.VERSION.SDK_INT<33||context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")==PackageManager.PERMISSION_GRANTED){
                PendingIntent open=PendingIntent.getActivity(context,0,new Intent(context,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                nm.notify(25,new Notification.Builder(context,CHANNEL).setSmallIcon(com.pocketledger.app.R.drawable.ic_launcher).setContentTitle("Allowance reminder").setContentText("Email Codam and enter your allowance.").setStyle(new Notification.BigTextStyle().bigText("Email Codam and enter your allowance manually in Pocket Ledger.")).setContentIntent(open).setAutoCancel(true).build());
            }
        }
        schedule(context);
    }
}
