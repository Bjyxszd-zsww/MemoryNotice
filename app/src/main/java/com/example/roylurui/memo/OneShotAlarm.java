package com.example.roylurui.memo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.sfzhang.memo.R;

import org.litepal.crud.DataSupport;

//做一个广播接收器
//接收广播的BroadcastReceiver及广播之间传递数据的Intent
//静态注册AndroidMainfest.xml
public class OneShotAlarm extends BroadcastReceiver {

    private int alarmId;
    int BIG_NUM_FOR_ALARM=100;

    @Override
    public void onReceive(Context context, Intent intent) {
        //重写父类，在有广播来的时候，得到执行170页
        //广播传过来的Intent，因为后面我们在发送广播时
        //可以利用GetStringExtra把这个字符串取出来
        alarmId=intent.getIntExtra("alarmId",0);

        Toast.makeText(context,"Time UP!",Toast.LENGTH_LONG).show();

        //设置震动
        ///Vibrator只是定义在android.os 包里的一个抽象类
        //public abstract class Vibrator{
        //abstract void cancel() 取消振动
        //abstract boolean hasVibrator() 是否有振动功能
        //abstract void vibrate(long[] pattern, int repeat) 按节奏重复振动
        //abstract void vibrate(long milliseconds) 持续振动
        //}
        Vibrator vb =(Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vb.vibrate(300);
        //参数用来指定振动的毫秒数。

        showNotice(context);
    }

    //显示通知，并且提供点击功能
    private void showNotice(Context context) {
        //Context 可以通过 sendBroadcast() 和 sendOrderedBroadcast()方法实现广播的发送
        //首先在需要发送信息的地方
        //把要发送的信息和用于过滤的信息装入一个 Intent 对象，
        //然后通过调用 Context.sendBroadcast()等方法，把Intent对象以广播方式发送出去。
        int num=alarmId-BIG_NUM_FOR_ALARM;
        Log.d("MainActivity","alarmNoticeId "+num);


        Intent intent=new Intent(context,Edit.class);

        Memo record= getMemoWithId(num);
        deleteTheAlarm(num);

        transportInformationToEdit(intent,record);

        //用来设置点击通知之后跳转到对应edit页面
        PendingIntent pi=PendingIntent.getActivity(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);

        //通知NotificationManager对同志进行管理283页
        //可以调用context.getSystemService（）方法获取
        NotificationManager manager=(NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        //设置通知的属性
        Notification notification=new NotificationCompat.Builder(context)
                .setContentTitle(record.getTextDate()+" "+record.getTextTime())
                .setContentText(record.getMainText())
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),R.drawable.icon))
                .setContentIntent(pi)
                .setAutoCancel(true)
                //.setStyle(new NotificationCompat.BigTextStyle().bigText(record.getMainText()))
                .setLights(Color.GREEN,1000,1000)
                .build();
        manager.notify(num,notification);
        //第一个参数闹钟序号，第二个当前通知
    }

    //删除数据库对应id的alarm（alarm为第二列，存储的是通知日期时间的字符串）
    private void deleteTheAlarm(int num) {
        ContentValues temp = new ContentValues();
        //先创建ContentValues对象
        //ContentValues 和HashTable类似都是一种存储的机制
        //存储基本类型的数据，像string，int之类的
        //不能存储对象这种东西，而HashTable却可以存储对象
        temp.put("alarm", "");
        String where = String.valueOf(num);
        DataSupport.updateAll(Memo.class, temp, "id = ?", where);
    }

    //存储数据到intent中去
    private void transportInformationToEdit(Intent it, Memo record) {
        it.putExtra("num",record.getNum());
        it.putExtra("tag",record.getTag());
        it.putExtra("textDate",record.getTextDate());
        it.putExtra("textTime",record.getTextTime());
        record.setAlarm("");
        it.putExtra("alarm","");
        it.putExtra("mainText",record.getMainText());
    }

    private Memo getMemoWithId(int num) {
        String whereArgs = String.valueOf(num);
        Memo record= DataSupport.where("id = ?", whereArgs).findFirst(Memo.class);
        return record;
    }
}
