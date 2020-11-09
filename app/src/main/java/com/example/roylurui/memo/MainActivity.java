package com.example.roylurui.memo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.sfzhang.memo.R;

import org.litepal.crud.DataSupport;
import org.litepal.tablemanager.Connector;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {


    //设置一个备忘录的表，适配器的数据源
    private List<OneMemo> memolist = new ArrayList<>();
    //适配器
    MemoAdapter adapter;
    //备忘录已有的小便条list view
    ListView lv;
    //闹钟最多为100个，上线
    int BIG_NUM_FOR_ALARM = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //数据库的创建Memo类，
        //litepal是一块开源的android数据库框架，采用了对象关系映射、面向对象的语言的模式
        //要对android中的三个文件进行创建和配置
        //app/build.gradle文件中声明开源库

        //memo是数据库的创建
        //创建了与备忘录有关的变量，响应的getter/setter方法
        //类中的每个字段对应表中的每个列

        //应用配置，AndroidMainfest.xml

        //创建表
        Connector.getDatabase();

        //读取表中历史数据方法
        loadHistoryData();

        adapter = new MemoAdapter(MainActivity.this, R.layout.memo_list, memolist);
        lv = (ListView) findViewById(R.id.list);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener(this);
        lv.setOnItemLongClickListener(this);

    }

    //创建菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }


    //点击菜单toolbar的加号出发添加事件
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                onAdd();
                break;
            default:
        }
        return true;
    }

    //从数据库中取数据
    private void loadHistoryData() {
        //读取memo表中所有的数据，返回的是一个memo的list集合
        List<Memo> memoes = DataSupport.findAll(Memo.class);

        //如果没取出来，从新初始化数据库
        if (memoes.size() == 0) {
            initializeLitePal();//空指针
            memoes = DataSupport.findAll(Memo.class);
        }

        //遍历数组
        for (Memo record : memoes) {
            //打印日志logcat
            Log.d("MainActivity", "current num: " + record.getNum());
            Log.d("MainActivity", "id: " + record.getId());
            Log.d("MainActivity", "getAlarm: " + record.getAlarm());
            //从当前memo对象中去除属性，封装为onemenmo添加到onememo集合（防止布尔值存储出现错误）
            int tag = record.getTag();
            String textDate = record.getTextDate();
            String textTime = record.getTextTime();
            boolean alarm = record.getAlarm().length() > 1 ? true : false;
            String mainText = record.getMainText();
            OneMemo temp = new OneMemo(tag, textDate, textTime, alarm, mainText);
            memolist.add(temp);
        }

    }

    //test
    public void testAdd(View v) {
        /*
        Memo record=new Memo();
        record.setNum(1);
        record.setTag(1);
        record.setTextDate("1212");
        record.setTextTime("23:00");
        record.setAlarm("123");
        record.setMainText("hahaha");
        record.save();
        */
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //准备跳转到编辑界面
        Intent it = new Intent(this, Edit.class);
        //获取当前位置对应的memo对象
        Memo record = getMemoWithNum(position);
        //对象信息存储
        transportInformationToEdit(it, record);
        //带着信息跳转
        startActivityForResult(it, position);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        //长按删除逻辑

        //获取当前memolist的大小
        int n = memolist.size();

        //如果需要删除memo已经设置闹钟
        //取消它
        if (memolist.get(position).getAlarm()) {
            cancelAlarm(position);
        }
        memolist.remove(position);
        adapter.notifyDataSetChanged();
        //长按删除，更新数据库
        String whereArgs = String.valueOf(position); //why not position ?
        DataSupport.deleteAll(Memo.class, "num = ?", whereArgs);
        //获取当前int position转化为string类型
        //删除获取的当前序号的memo
        for (int i = position + 1; i < n; i++) {
            ContentValues temp = new ContentValues();
            //先创建ContentValues对象
            //ContentValues 和HashTable类似都是一种存储的机制
            //存储基本类型的数据，像string，int之类的
            //不能存储对象这种东西，而HashTable却可以存储对象
            //现在删除了一个memo
            //序号减一，修改num变量
            temp.put("num", i - 1);
            String where = String.valueOf(i);
            DataSupport.updateAll(Memo.class, temp, "num = ?", where);
        }

        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent it) {
        //参数memo序号，确定与否，intent传递当前memo信息
        if (resultCode == RESULT_OK) {
            updateLitePalAndList(requestCode, it);
            //编辑完memo之后，按了确定按钮，更新数据库信息
        }
    }

    //更新数据信息
    private void updateLitePalAndList(int requestCode, Intent it) {
        //参数memo序号，intent传递当前memo信息
        int num = requestCode;
        int tag = it.getIntExtra("tag", 0);
        //默认背景图片位置是0

        //获取当时的时间
        Calendar c = Calendar.getInstance();
        String current_date = getCurrentDate(c);
        String current_time = getCurrentTime(c);

        String alarm = it.getStringExtra("alarm");
        String mainText = it.getStringExtra("mainText");

        boolean gotAlarm = alarm.length() > 1 ? true : false;
        //当前新的memo对象信息
        //相当于一个中间的数据类，传递数据
        OneMemo new_memo = new OneMemo(tag, current_date, current_time, gotAlarm, mainText);

        if ((requestCode + 1) > memolist.size()) {
            //数据库插入
            addRecordToLitePal(num, tag, current_date, current_time, alarm, mainText);
            //把onememo的对象加到memolist中
            //list添加
            memolist.add(new_memo);
        }
        else {
            //如果当前的memo已经有闹钟先取消当前的闹钟
            if (memolist.get(num).getAlarm()) {
                cancelAlarm(num);
            }

            //更新当前memo的信息
            ContentValues temp = new ContentValues();
            //先创建ContentValues对象
            //ContentValues 和HashTable类似都是一种存储的机制
            //存储基本类型的数据，像string，int之类的
            //不能存储对象这种东西，而HashTable却可以存储对象
            //将更新的数据加进去DataSupport.updateAll更新数据
            temp.put("tag", tag);
            temp.put("textDate", current_date);
            temp.put("textTime", current_time);
            temp.put("alarm", alarm);
            temp.put("mainText", mainText);
            String where = String.valueOf(num);
            DataSupport.updateAll(Memo.class, temp, "num = ?", where);
            memolist.set(num, new_memo);
        }
        //如果当的memo有设置闹钟
        if (gotAlarm) {
            loadAlarm(alarm, requestCode, 0);
        }

        adapter.notifyDataSetChanged();
        //更新数据库
    }

    //当数据库中没有数据的时候
    private void initializeLitePal() {
        Calendar c = Calendar.getInstance();
        String textDate = getCurrentDate(c);
        String textTime = getCurrentTime(c);


        //初始化的数据，插入两条数据
        addRecordToLitePal(0, 0, textDate, textTime, "", "点击是编辑");
        addRecordToLitePal(1, 1, textDate, textTime, "", "长按是删除");
    }

    //获得当前日期并格式化,格式：XX/XX
    private String getCurrentDate(Calendar c) {
        return c.get(Calendar.YEAR) + "/" + (c.get(Calendar.MONTH) + 1) + "/" + c.get(Calendar.DAY_OF_MONTH);
    }

    //得到当前时间并格式化，格式：XX：XX
    private String getCurrentTime(Calendar c) {
        String current_time = "";
        if (c.get(Calendar.HOUR_OF_DAY) < 10)
            current_time = current_time + "0" + c.get(Calendar.HOUR_OF_DAY);
        else current_time = current_time + c.get(Calendar.HOUR_OF_DAY);

        current_time = current_time + ":";

        if (c.get(Calendar.MINUTE) < 10) current_time = current_time + "0" + c.get(Calendar.MINUTE);
        else current_time = current_time + c.get(Calendar.MINUTE);

        return current_time;
    }

    //根据下列数据将memo对象存入数据库
    private void addRecordToLitePal(int num, int tag, String textDate, String textTime, String alarm, String mainText) {
        Memo record = new Memo();
        record.setNum(num);
        record.setTag(tag);
        record.setTextDate(textDate);
        record.setTextTime(textTime);
        record.setAlarm(alarm);
        record.setMainText(mainText);
        record.save();
    }

    //将memo对象中的数据提取出来放进intent中（其实可以javabean实现serializable接口，即可直接传递对象）
    private void transportInformationToEdit(Intent it, Memo record) {
        it.putExtra("num", record.getNum());
        it.putExtra("tag", record.getTag());
        it.putExtra("textDate", record.getTextDate());
        it.putExtra("textTime", record.getTextTime());
        it.putExtra("alarm", record.getAlarm());
        it.putExtra("mainText", record.getMainText());
    }



    //添加memo
    public void onAdd() {
        Intent it = new Intent(this, Edit.class);
        //传递数据Intent
        int position = memolist.size();

        Calendar c = Calendar.getInstance();
        String current_date = getCurrentDate(c);
        String current_time = getCurrentTime(c);

        it.putExtra("num", position);
        it.putExtra("tag", 0);
        it.putExtra("textDate", current_date);
        it.putExtra("textTime", current_time);
        it.putExtra("alarm", "");
        it.putExtra("mainText", "");

        startActivityForResult(it, position);
        //editclass
    }

    //根据序号从数据库中找到对应序号的memo对象
    private Memo getMemoWithNum(int num) {
        String whereArgs = String.valueOf(num);
        Memo record = DataSupport.where("num = ?", whereArgs).findFirst(Memo.class);
        return record;
    }

    //设置闹钟
    private void loadAlarm(String alarm, int num, int days) {

        //根据传入的alarm字符串，解析时间
        int alarm_hour = 0;
        int alarm_minute = 0;
        int alarm_year = 0;
        int alarm_month = 0;
        int alarm_day = 0;

        //获取当前的日期，substring（k,i）
        //当前string alarm中从k-i的字符，也就是当前的年月日，时间
        int i = 0, k = 0;
        while (i < alarm.length() && alarm.charAt(i) != '/') i++;
        alarm_year = Integer.parseInt(alarm.substring(k, i));
        k = i + 1;
        i++;
        while (i < alarm.length() && alarm.charAt(i) != '/') i++;
        alarm_month = Integer.parseInt(alarm.substring(k, i));
        k = i + 1;
        i++;
        while (i < alarm.length() && alarm.charAt(i) != ' ') i++;
        alarm_day = Integer.parseInt(alarm.substring(k, i));
        k = i + 1;
        i++;
        while (i < alarm.length() && alarm.charAt(i) != ':') i++;
        alarm_hour = Integer.parseInt(alarm.substring(k, i));
        k = i + 1;
        i++;
        alarm_minute = Integer.parseInt(alarm.substring(k));

        Memo record = getMemoWithNum(num);


        //响铃提醒时
        Intent intent = new Intent(MainActivity.this, OneShotAlarm.class);
        intent.putExtra("alarmId", record.getId() + BIG_NUM_FOR_ALARM);
        //调用PendingIntent.getBroadcast()方法能够执行广播
        PendingIntent sender = PendingIntent.getBroadcast(
                MainActivity.this, record.getId() + BIG_NUM_FOR_ALARM, intent, 0);

        // 我们希望10秒后警报响起。
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        //calendar.add(Calendar.SECOND, 5);

        Calendar alarm_time = Calendar.getInstance();
        alarm_time.set(alarm_year, alarm_month - 1, alarm_day, alarm_hour, alarm_minute);

        int interval = 1000 * 60 * 60 * 24 * days;//毫秒数一天的*天数

        //设置一个闹钟，通过AlarmManager类实现
        //通过Context的getSystemService（）方法获取实例467页书上
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        //set定时，多少时间后执行
        //RTC_WAKEUP表示会唤醒CPU从指定时间触发闹钟
        //getTimeInMillis()获取从时间点至今的时间
        //第三个参数调用PendingIntent.getBroadcast()方法能够执行广播
        am.set(AlarmManager.RTC_WAKEUP, alarm_time.getTimeInMillis(), sender);
    }

    //取消闹钟
    private void cancelAlarm(int num) {
        Memo record = getMemoWithNum(num);
        //获取当前memo的num序号

        Intent intent = new Intent(MainActivity.this,
                OneShotAlarm.class);
        //用来设置点击通知之后跳转到对应edit页面
        PendingIntent sender = PendingIntent.getBroadcast(
                MainActivity.this, record.getId() + BIG_NUM_FOR_ALARM, intent, 0);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(sender);
    }

}

