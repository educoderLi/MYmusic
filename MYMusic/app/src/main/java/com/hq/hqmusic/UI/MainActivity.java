package com.hq.hqmusic.UI;

import android.Manifest;
import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hq.hqmusic.Adapter.MyAdapter;
import com.hq.hqmusic.CustomView.CustomDialog;
import com.hq.hqmusic.CustomView.GradientTextView;
import com.hq.hqmusic.CustomView.MyDialog;
import com.hq.hqmusic.Entity.Song;
import com.hq.hqmusic.R;
import com.hq.hqmusic.StatusBar.BaseActivity;
import com.hq.hqmusic.StatusBar.SystemBarTintManager;
import com.hq.hqmusic.Utils.ImageCacheUtil;
import com.hq.hqmusic.Utils.MusicUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import kr.co.namee.permissiongen.PermissionFail;
import kr.co.namee.permissiongen.PermissionGen;
import kr.co.namee.permissiongen.PermissionSuccess;

public class MainActivity extends BaseActivity {

    private SharedPreferences sharedPreferences;
    private ListView listview;
    private List<Song> list;
    private MyAdapter adapter;
    private PopupWindow popupWindow;
    private MediaPlayer mplayer = new MediaPlayer();
    private GradientTextView text_main;
    private MyDialog myDialog, myDialog_bestlove;
    private SeekBar seekBar;
    private TextView textView1, textView2;
    private ImageView imageView_play, imageView_next, imageView_front,
            imageview, imageview_playstyle, imageview_location,
            imageview_setting;
    private int screen_width;
    private Random random = new Random();
    // 用于判断当前的播放顺序，0->单曲循环,1->顺序播放,2->随机播放
    private int play_style = 0;
    // 判断seekbar是否正在滑动
    private boolean ischanging = false;
    private Thread thread;
    // 当前音乐播放位置,从0开始
    private int currentposition;
    // 屏幕显示的最大listview条数
    private int max_item;
    // 该字符串用于判断主题
    private String string_theme;
    // 修改顶部状态栏颜色使用
    private SystemBarTintManager mTintManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //获取权限
        getAuthority();

        sharedPreferences = getSharedPreferences("location", MODE_PRIVATE);
        // 主题设置
        string_theme = sharedPreferences.getString("theme_select", "blue");
        if (string_theme.equals("blue")) {
            setTheme(R.style.Theme_blue);
        } else if (string_theme.equals("purple")) {
            setTheme(R.style.Theme_purple);
        } else if (string_theme.equals("green")) {
            setTheme(R.style.Theme_green);
        } else {
            setTheme(R.style.Theme_red);
        }
        setContentView(R.layout.activity_main);

        // 顶部状态栏颜色设置
        mTintManager = new SystemBarTintManager(MainActivity.this);
        mTintManager.setStatusBarTintEnabled(true);
        if (string_theme.equals("blue")) {
            mTintManager.setStatusBarTintResource(R.color.blue);
        } else if (string_theme.equals("purple")) {
            mTintManager.setStatusBarTintResource(R.color.purple);
        } else if (string_theme.equals("green")) {
            mTintManager.setStatusBarTintResource(R.color.green);
        } else {
            mTintManager.setStatusBarTintResource(R.color.red);
        }

        // 获得屏幕宽度并保存在screen_width中
        init_screen_width();

        // 加载currentposition的初始数据
        currentposition = sharedPreferences.getInt("currentposition", 0);

        // 顶部视图控件的绑定
        initTopView();

        // 顶部和 底部操作栏按钮点击事件
        setClick();

        // 给textView1和textView2赋初值
        initText();

        // 加载popupwindow并设置相关属性
        setpopupwindow();

        // listview的绑定,数据加载,以及相关事件的监听
        setListView();

        // 设置mediaplayer监听器
        setMediaPlayerListener();

    }

    @Override
    protected void onDestroy() {

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("song_name",
                cut_song_name(list.get(currentposition).getSong()));
        editor.putString("song_singer", list.get(currentposition).getSinger());
        editor.putInt("currentposition", currentposition);
        editor.commit();
        if (mplayer.isPlaying()) {
            mplayer.stop();
        }
        mplayer.release();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == 1) {

            Bitmap bitmap = ImageCacheUtil.getResizedBitmap(null, null,
                    MainActivity.this, data.getData(), screen_width, true);
            BitmapDrawable drawable = new BitmapDrawable(null, bitmap);

            listview.setBackground(drawable);
            saveDrawable(drawable);
            myDialog.dismiss();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // 监听返回键
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        final CustomDialog customDialog = new CustomDialog(MainActivity.this,
                R.layout.layout_customdialog, R.style.dialogTheme);
        customDialog.setT("系统提示");
        customDialog.setM("确定要退出播放器了吗?");
        customDialog.setButtonLeftText("确定");
        customDialog.setButtonRightText("取消");
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (myDialog.isShowing()) {
                myDialog.dismiss();
            } else if (popupWindow.isShowing()) {
                popupWindow.dismiss();
            } else {
                customDialog.show();
                customDialog.setOnClickBtnListener(new CustomDialog.OnClickBtnListener() {

                    @Override
                    public void onClickOk() {
                        // TODO Auto-generated method stub
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        startActivity(intent);
                        customDialog.dismiss();
                    }

                    @Override
                    public void onClickCancel() {
                        // TODO Auto-generated method stub
                        customDialog.cancel();
                    }
                });
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // 给屏幕宽度赋值
    private void init_screen_width() {
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        screen_width = size.x;
    }

    // 设置popupwindow相关属性及里面控件的点击事件
    private void setpopupwindow() {
        View popup_layout = LayoutInflater.from(MainActivity.this).inflate(
                R.layout.popupwindow_setting, null);
        popupWindow = new PopupWindow(popup_layout, ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(new ColorDrawable(
                android.graphics.Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);

        RelativeLayout relativeLayout_listbg = (RelativeLayout) popup_layout
                .findViewById(R.id.layout_listbg);

        RelativeLayout relativeLayout_skin = (RelativeLayout) popup_layout
                .findViewById(R.id.layout_skin);

        RelativeLayout relativeLayout_location = (RelativeLayout) popup_layout
                .findViewById(R.id.layout_location_bestlove);

        RelativeLayout relativeLayout_exit = (RelativeLayout) popup_layout
                .findViewById(R.id.layout_exit);

        // 初始化第一行点击后弹出的自定义dialog并设置相关属性
        myDialog = new MyDialog(MainActivity.this, R.style.dialogTheme,
                R.layout.popup_changelistbg);
        final Window window = myDialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.setWindowAnimations(R.style.enter);

        relativeLayout_listbg.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                popupWindow.dismiss();

                myDialog.show();
                WindowManager.LayoutParams params = window.getAttributes();
                params.width = screen_width;
                params.dimAmount = 0.4f;
                window.setAttributes(params);

                // 弹出dialog的点击事件
                RelativeLayout select_localimage = (RelativeLayout) myDialog
                        .findViewById(R.id.select_localimage);
                RelativeLayout use_default = (RelativeLayout) myDialog
                        .findViewById(R.id.use_default);
                RelativeLayout select_xhh = (RelativeLayout) myDialog
                        .findViewById(R.id.select_xhh);
                select_localimage.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.putExtra("return-data", true);

                        intent.putExtra("crop", "circle");
                        // 使用Intent.ACTION_GET_CONTENT这个Action
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        // 取得相片后返回本画面
                        startActivityForResult(intent, 1);
                    }
                });
                use_default.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        listview.setBackground(null);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("listbg", "");
                        editor.commit();
                        myDialog.dismiss();
                    }
                });
                select_xhh.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        listview.setBackground(getResources().getDrawable(
                                R.mipmap.xhh));
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("listbg", "xhh");
                        editor.commit();
                        myDialog.dismiss();
                    }
                });

            }
        });

        relativeLayout_skin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                popupWindow.dismiss();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("song_name",
                        cut_song_name(list.get(currentposition).getSong()));
                editor.putString("song_singer",
                        list.get(currentposition).getSinger());
                editor.putInt("currentposition", currentposition);

                editor.commit();
                Intent intent = new Intent(MainActivity.this,
                        ThemeSettingActivity.class);
                MainActivity.this.startActivity(intent);
                overridePendingTransition(R.anim.fade, R.anim.hold);

                if (mplayer.isPlaying()) {
                    mplayer.stop();
                    mplayer.reset();
                }
            }
        });

        // 定位至最爱歌曲
        relativeLayout_location.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                popupWindow.dismiss();
                int bestlove_position = sharedPreferences.getInt(
                        "bestlove_position", 0);
                currentposition = bestlove_position;

                adapter.setFlag(currentposition);
                switch (string_theme) {
                    case "blue":
                        adapter.setTheme("blue");
                        break;
                    case "purple":
                        adapter.setTheme("purple");
                        break;
                    case "green":
                        adapter.setTheme("green");
                        break;
                    case "red":
                        adapter.setTheme("red");
                        break;
                    default:
                        break;
                }
                adapter.notifyDataSetChanged();

                if (bestlove_position - 3 < 0) {
                    listview.setSelection(0);
                } else {
                    listview.setSelection(bestlove_position - 3);
                }
                musicplay(bestlove_position);
            }
        });

        // 初始化退出行点击后弹出的dialog并设置相关属性
        final CustomDialog customDialog = new CustomDialog(MainActivity.this,
                R.layout.layout_customdialog, R.style.dialogTheme);
        customDialog.setT("系统提示");
        customDialog.setM("确定要退出播放器了吗?");
        customDialog.setButtonLeftText("确定");
        customDialog.setButtonRightText("取消");

        relativeLayout_exit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                popupWindow.dismiss();

                customDialog.show();
                customDialog.setOnClickBtnListener(new CustomDialog.OnClickBtnListener() {

                    @Override
                    public void onClickOk() {
                        // TODO Auto-generated method stub
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        startActivity(intent);
                        customDialog.dismiss();
                    }

                    @Override
                    public void onClickCancel() {
                        // TODO Auto-generated method stub
                        customDialog.cancel();
                    }
                });
            }
        });
    }

    private void initTopView() {
        text_main = (GradientTextView) this.findViewById(R.id.text_main);
        imageview_playstyle = (ImageView) this.findViewById(R.id.play_style);
        imageview_setting = (ImageView) this.findViewById(R.id.settings);
        imageview_location = (ImageView) this
                .findViewById(R.id.imageview_location);
    }

    private void setClick() {

        imageview_setting.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                popupWindow.showAsDropDown(v, 0, 10);
                ;
            }
        });

        imageview_playstyle.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                play_style++;
                if (play_style > 2) {
                    play_style = 0;
                }

                switch (play_style) {
                    case 0:
                        imageview_playstyle.setImageResource(R.mipmap.cicle);
                        Toast.makeText(MainActivity.this, "单曲循环",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        imageview_playstyle.setImageResource(R.mipmap.ordered);
                        Toast.makeText(MainActivity.this, "顺序播放",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        imageview_playstyle.setImageResource(R.mipmap.unordered);
                        Toast.makeText(MainActivity.this, "随机播放",
                                Toast.LENGTH_SHORT).show();
                        break;

                }
            }
        });

        imageview_location.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (currentposition - 3 <= 0) {
                    listview.setSelection(0);
                } else {
                    listview.setSelection(currentposition - 3);
                }

            }
        });

        View layout_playbar = (View) findViewById(R.id.layout_playbar);
        imageview = (ImageView) layout_playbar.findViewById(R.id.imageview);
        imageView_play = (ImageView) layout_playbar
                .findViewById(R.id.imageview_play);
        imageView_next = (ImageView) layout_playbar
                .findViewById(R.id.imageview_next);
        imageView_front = (ImageView) layout_playbar
                .findViewById(R.id.imageview_front);
        textView1 = (TextView) layout_playbar.findViewById(R.id.name);
        textView2 = (TextView) layout_playbar.findViewById(R.id.singer);
        seekBar = (SeekBar) layout_playbar.findViewById(R.id.seekbar);
        imageView_play.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                switch (string_theme) {
                    case "blue":
                        change_play_image(R.mipmap.play, R.mipmap.pause);
                        break;
                    case "purple":
                        change_play_image(R.mipmap.play_purple,
                                R.mipmap.pause_purple);
                        break;
                    case "green":
                        change_play_image(R.mipmap.play_green,
                                R.mipmap.pause_green);
                        break;
                    case "red":
                        change_play_image(R.mipmap.play_red, R.mipmap.pause_red);
                        break;
                    default:
                        break;
                }

                if (mplayer.isPlaying()) {
                    mplayer.pause();
                    imageview.clearAnimation();
                } else {
                    mplayer.start();
                    // thread = new Thread(new SeekBarThread());
                    // thread.start();
                    imageview.startAnimation(AnimationUtils.loadAnimation(
                            MainActivity.this, R.anim.imageview_rotate));
                }
            }
        });

        imageView_next.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (play_style == 2) {
                    random_nextMusic();
                    auto_change_listview();
                } else {
                    nextMusic();
                    auto_change_listview();
                }
            }
        });

        imageView_front.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                if (play_style == 2) {
                    random_nextMusic();
                    auto_change_listview();
                } else {
                    frontMusic();
                    auto_change_listview();
                }
            }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                ischanging = false;
                mplayer.seekTo(seekBar.getProgress());
                thread = new Thread(new SeekBarThread());
                thread.start();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                ischanging = true;
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                // 可以用来写拖动的时候实时显示时间
            }
        });

    }

    private void initText() {

        textView1.setText(sharedPreferences.getString("song_name", "歌曲名").trim());
        textView2.setText(sharedPreferences.getString("song_singer", "歌手").trim());
    }

    private void setListView() {
        listview = (ListView) this.findViewById(R.id.listveiw);

        list = new ArrayList<Song>();
        list = MusicUtils.getMusicData(MainActivity.this);
        adapter = new MyAdapter(MainActivity.this, list);
        // 标记正在播放的音乐条目为主题色
        adapter.setFlag(currentposition);
        switch (string_theme) {
            case "blue":
                adapter.setTheme("blue");
                break;
            case "purple":
                adapter.setTheme("purple");
                break;
            case "green":
                adapter.setTheme("green");
                break;
            case "red":
                adapter.setTheme("red");
                break;
            default:
                break;
        }
        adapter.notifyDataSetChanged();

        listview.setAdapter(adapter);

        // 判断当前歌曲是否在屏幕可见范围内，若是则显示定位图标，否则隐藏
        check_location();

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // TODO Auto-generated method stub
                currentposition = position;
                musicplay(currentposition);

                text_main.setText(cut_song_name(list.get(currentposition).getSong()));
                adapter.setFlag(currentposition);
                adapter.notifyDataSetChanged();
            }
        });

        myDialog_bestlove = new MyDialog(MainActivity.this,
                R.style.dialogTheme, R.layout.setting_best_lovesong);
        final Window window2 = myDialog_bestlove.getWindow();
        window2.setGravity(Gravity.CENTER);
        listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {
                // TODO Auto-generated method stub
                myDialog_bestlove.show();
                WindowManager.LayoutParams params = window2.getAttributes();
                params.width = (int) (screen_width * 0.75);
                params.dimAmount = 0.4f;
                window2.setAttributes(params);

                RelativeLayout relativeLayout_make_bestlove = (RelativeLayout) myDialog_bestlove
                        .findViewById(R.id.make_bestlove);
                RelativeLayout relativeLayout_make_cancel = (RelativeLayout) myDialog_bestlove
                        .findViewById(R.id.make_cancel);
                final int best_love_position = position;
                relativeLayout_make_bestlove
                        .setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                // TODO Auto-generated method stub
                                myDialog_bestlove.dismiss();

                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putInt("bestlove_position",
                                        best_love_position);
                                editor.commit();
                                Toast.makeText(MainActivity.this, "设定成功", Toast.LENGTH_SHORT)
                                        .show();

                            }
                        });
                relativeLayout_make_cancel
                        .setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                // TODO Auto-generated method stub
                                myDialog_bestlove.dismiss();
                            }
                        });
                return true;
            }
        });

        listview.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                // TODO Auto-generated method stub
                if (currentposition >= firstVisibleItem
                        && currentposition <= firstVisibleItem
                        + visibleItemCount) {
                    imageview_location.setVisibility(View.INVISIBLE);
                } else {
                    imageview_location.setVisibility(View.VISIBLE);
                }
                max_item = visibleItemCount;
            }
        });
        // 给listview设置初始背景
        if (loadDrawable() != null) {
            listview.setBackground(loadDrawable());
        } else if (sharedPreferences.getString("listbg", "").equals("xhh")) {
            listview.setBackground(getResources().getDrawable(R.mipmap.xhh));
        } else {
            listview.setBackground(null);
        }
    }

    private void musicplay(int position) {

        textView1.setText(cut_song_name(list.get(position).getSong()).trim());
        textView2.setText(list.get(position).getSinger().trim());
        text_main.setText(cut_song_name(list.get(currentposition).getSong()));
        seekBar.setMax(list.get(position).getDuration());
        imageview.startAnimation(AnimationUtils.loadAnimation(
                MainActivity.this, R.anim.imageview_rotate));
        switch (string_theme) {
            case "blue":
                imageView_play.setImageResource(R.mipmap.pause);
                break;
            case "purple":
                imageView_play.setImageResource(R.mipmap.pause_purple);
                break;
            case "green":
                imageView_play.setImageResource(R.mipmap.pause_green);
                break;
            case "red":
                imageView_play.setImageResource(R.mipmap.pause_red);
                break;
            default:
                break;
        }

        try {
            mplayer.reset();
            mplayer.setDataSource(list.get(position).getPath());
            mplayer.prepare();
            mplayer.start();


        } catch (Exception e) {
            e.printStackTrace();
        }
        thread = new Thread(new SeekBarThread());
        thread.start();
    }

    private void setMediaPlayerListener() {
        // 监听mediaplayer播放完毕时调用
        mplayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                // TODO Auto-generated method stub
                switch (play_style) {
                    case 0:
                        musicplay(currentposition);
                        break;
                    case 1:
                        // 这里会引发初次进入时直接点击播放按钮时，播放的是下一首音乐的问题
                        nextMusic();
                        break;
                    case 2:
                        random_nextMusic();
                        break;
                    default:

                        break;
                }
            }
        });
        // 设置发生错误时调用
        mplayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                // TODO Auto-generated method stub
                mp.reset();
                // Toast.makeText(MainActivity.this, "未发现音乐", 1500).show();
                return false;
            }
        });
    }

    // 自定义的线程,用于下方seekbar的刷新
    class SeekBarThread implements Runnable {

        @Override
        public void run() {
            while (!ischanging && mplayer.isPlaying()) {
                // 将SeekBar位置设置到当前播放位置
                seekBar.setProgress(mplayer.getCurrentPosition());

                try {
                    // 每500毫秒更新一次位置
                    Thread.sleep(500);
                    // 播放进度

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 下一曲
    private void nextMusic() {
        currentposition++;
        if (currentposition > list.size() - 1) {
            currentposition = 0;
        }
        musicplay(currentposition);
        adapter.setFlag(currentposition);
        adapter.notifyDataSetChanged();
    }

    // 上一曲
    private void frontMusic() {
        currentposition--;
        if (currentposition < 0) {
            currentposition = list.size() - 1;
        }
        musicplay(currentposition);
        adapter.setFlag(currentposition);
        adapter.notifyDataSetChanged();
    }

    // 随机播放下一曲
    private void random_nextMusic() {
        currentposition = currentposition + random.nextInt(list.size() - 1);
        currentposition %= list.size();
        musicplay(currentposition);
        adapter.setFlag(currentposition);
        adapter.notifyDataSetChanged();
    }

    // 切掉音乐名字最后的.mp3
    private String cut_song_name(String name) {
        if (name.length() >= 5
                && name.substring(name.length() - 4, name.length()).equals(
                ".mp3")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    // 定位图标的显示与隐藏
    private void check_location() {
        if (currentposition >= listview.getFirstVisiblePosition()
                && currentposition <= listview.getLastVisiblePosition()) {
            imageview_location.setVisibility(View.INVISIBLE);
        } else {
            imageview_location.setVisibility(View.VISIBLE);
        }
    }

    // 点击下一曲上一曲时自动滚动列表
    private void auto_change_listview() {
        if (currentposition <= listview.getFirstVisiblePosition()) {
            listview.setSelection(currentposition);
        }
        if (currentposition >= listview.getLastVisiblePosition()) {
            // listview.smoothScrollToPosition(currentposition);
            listview.setSelection(currentposition - max_item + 2);
        }
    }

    // 使用sharedPreferences保存listview背景图片
    private void saveDrawable(Drawable drawable) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
        Bitmap bitmap = bitmapDrawable.getBitmap();
        // Bitmap bitmap = BitmapFactory.decodeResource(getResources(), id);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        String imageBase64 = new String(Base64.encodeToString(
                baos.toByteArray(), Base64.DEFAULT));
        editor.putString("listbg", imageBase64);
        editor.commit();
    }

    // 加载用sharedPreferences保存的图片
    private Drawable loadDrawable() {
        String temp = sharedPreferences.getString("listbg", "");
        ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decode(
                temp.getBytes(), Base64.DEFAULT));
        return Drawable.createFromStream(bais, "");
    }

    // 该方法用于判断点击播放使用那张图片替换
    private void change_play_image(int resID_play, int resID_pause) {
        if (imageView_play
                .getDrawable()
                .getCurrent()
                .getConstantState()
                .equals(getResources().getDrawable(resID_play)
                        .getConstantState())) {
            imageView_play.setImageResource(resID_pause);
        } else {
            imageView_play.setImageResource(resID_play);
        }
    }

    private void getAuthority(){
        //适配6.0以上机型请求权限
        PermissionGen.with(MainActivity.this)
                .addRequestCode(100)
                .permissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .request();
    }

    //以下三个方法用于6.0以上权限申请适配
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionGen.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @PermissionSuccess(requestCode = 100)
    public void doSomething(){
        //Toast.makeText(this, "相关权限已允许", Toast.LENGTH_SHORT).show();
    }

    @PermissionFail(requestCode = 100)
    public void doFailSomething(){
        //Toast.makeText(this, "相关权限已拒绝", Toast.LENGTH_SHORT).show();
    }
}
