package com.deathkon.dji.flyingtest;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import dji.sdk.MissionManager.DJIMission;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.SDKManager.DJISDKManager;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseComponent.DJIComponentListener;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;
import dji.sdk.base.DJISDKError;
public class MainActivity extends AppCompatActivity  implements OnClickListener, DJIBaseProduct.DJIVersionCallback {

    public DJIBaseProduct mProduct;

    private Button changePageButton;
    private TextView showInfoView;

    private Handler mHandler;
    protected Context mContext;
    private String androidid;
    private AlertDialog inputKeyDialog;
    private TextView inputKeyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.mContext = this;


        changePageButton=(Button)findViewById(R.id.button1);
        showInfoView=(TextView)findViewById(R.id.linkInfoView);

        changePageButton.setOnClickListener(this);



        //register process
        try{
            androidid= Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            inputKeyText=new EditText(getApplicationContext());

            initDialog();

            initHandleListen();


            login();

        }catch (Exception e){
            Utils.setResultToToast(getApplicationContext(),e.getMessage());
        }

    }

    private void restartApplication() {
        final Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void initHandleListen(){
        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                try{
                    //super.handleMessage(msg);
                    Bundle data = msg.getData();
                    String val = data.getString("authority");
                    if(val.equals("true")){
                        Utils.setResultToToast(getApplicationContext(), "验证成功。");
                        inputKeyDialog.dismiss();
                    }else  if(val.equals("false")){
                        Utils.setResultToToast(getApplicationContext(), "验证失败。");
                        changePageButton.setEnabled(false);
                        try{
                            inputKeyDialog.show();
                        }catch (Exception e){
                            Utils.setResultToToast(getApplicationContext(),e.getMessage());
                        }
                    }
                }catch (Exception e){
                    Utils.setResultToToast(getApplicationContext(), e.getMessage());
                }

            }
        };
    }

    private void initDialog(){
        AlertDialog.Builder inputKeyDialogBuilder= new AlertDialog.Builder(mContext)
                .setTitle("请输入注册码")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(inputKeyText)
                .setPositiveButton(
                        "确定",
                        new android.content.DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                register();
                            }
                        })
                .setNegativeButton(
                        "取消",
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                exitProgrames();
                            }
                        });

        inputKeyDialog=inputKeyDialogBuilder.create();
    }


    private String readStream(InputStream res){
        InputStreamReader inputStreamReader = null;
        try{
            inputStreamReader = new InputStreamReader(res, "gbk");
        }catch(Exception e){
            Utils.setResultToToast(getApplicationContext(),"验证失败："+e.getMessage());
        }
        BufferedReader reader = new BufferedReader(inputStreamReader);
        StringBuffer sb = new StringBuffer("");
        String line;
        try {
            while((line = reader.readLine())!=null){
                sb.append(line);
                //sb.append("\n");
            }
        }catch(Exception e) {
            Utils.setResultToToast(getApplicationContext(), "验证失败：" + e.getMessage());
        }
        return sb.toString();
    }

    private void login(){
        Runnable networkTask = new Runnable() {

            @Override
            public void run() {
                HttpURLConnection urlConnection=null;
                try {
                    URL url = new URL("http://121.42.187.116:81/Service1.svc/DJIGetUserAuthority?userid="+androidid);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    String res = readStream(in);
                    Message msg = new Message();
                    Bundle data = new Bundle();
                    data.putString("authority",res);
                    msg.setData(data);
                    mHandler.sendMessage(msg);
                }catch(Exception e){
                    Utils.setResultToToast(getApplicationContext(),"连接验证服务器失败："+e.getMessage());
                }
                finally {
                    if(urlConnection!=null)urlConnection.disconnect();
                }
            }
        };
        new Thread(networkTask).start();
    }

    private void register(){
        Runnable networkTask = new Runnable() {
            @Override
            public void run() {
                HttpURLConnection urlConnection=null;
                try {
                    String key=inputKeyText.getText().toString();
                    URL url = new URL("http://121.42.187.116:81/Service1.svc/DJIRegister?userid="+androidid+"&key="+key);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    String res = readStream(in);

                    Thread.sleep(1000);
                    restartApplication();
                    /*
                    //重新尝试登陆
                    url = new URL("http://121.42.187.116:81/Service1.svc/DJIGetUserAuthority?userid="+androidid);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = new BufferedInputStream(urlConnection.getInputStream());
                    res = readStream(in);
                    Message msg = new Message();
                    Bundle data = new Bundle();
                    data.putString("authority", res);
                    msg.setData(data);
                    mHandler.sendMessage(msg);
                    */
                }catch(Exception e){
                    Utils.setResultToToast(getApplicationContext(),"连接验证服务器失败："+e.getMessage());
                }
                finally {
                    if(urlConnection!=null)urlConnection.disconnect();
                }
            }
        };
        new Thread(networkTask).start();
    }

    @Override
    public void onStart(){
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Utils.FLAG_CONNECTION_CHANGE);
        try{
            //unregisterReceiver(mReceiver);
            registerReceiver(mReceiver, filter);
        }catch(Exception e) {}
        //initSDK();
    }

    @Override
    public void onDetachedFromWindow() {
        try{
            unregisterReceiver(mReceiver);
        }catch(Exception e) {}

        super.onDetachedFromWindow();
    }

    @Override
    public void onClick(View v){
        switch (v.getId()) {
            case R.id.button1:
                goPointMission();
            default:
                break;
        }
    }


    @Override
    protected void onResume() {
        /**
         * 设置为横屏
         */
        if(getRequestedOrientation()!=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        super.onResume();
    }

    @Override
    public void onProductVersionChange(String oldVersion, String newVersion) {
        //updateVersion();
    }


    @Override
    public void onBackPressed() {
        exitProgrames();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Log.e("KEY",","+event.getKeyCode());
        Utils.setResultToToast(this,"!"+keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exitProgrames();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void exitProgrames(){
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void goPointMission(){

        Intent intent=new Intent(this,WayPointMissionActivity.class);
        startActivity(intent);
        finish();
    }


    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            refreshSDKRelativeUI();
            unregisterReceiver(mReceiver);
        }

    };

    private void refreshSDKRelativeUI() {
        mProduct = DJIApplication.getProductInstance();

        if (null != mProduct && mProduct.isConnected()) {
            Log.v("TAG", "refreshSDK: True");
            changePageButton.setEnabled(true);

            String str = mProduct instanceof DJIAircraft ? "DJIAircraft" : "DJIHandHeld";
            showInfoView.setText(str + " 已连接");
            mProduct.setDJIVersionCallback(this);
            //updateVersion();

            if (null != mProduct.getModel()) {
                //mTextProduct.setText("" + mProduct.getModel().getDisplayName());
            } else {
                //mTextProduct.setText(R.string.product_information);
            }
        } else {
            Log.v("TAG", "refreshSDK: False");
            //changePageButton.setEnabled(true);

            //mTextProduct.setText(R.string.product_information);
            showInfoView.setText("设备未连接");
        }
    }



}
