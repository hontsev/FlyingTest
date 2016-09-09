package com.deathkon.dji.flyingtest;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.AttributeSet;

import dji.sdk.AirLink.DJILBAirLink;
import dji.sdk.Battery.DJIBattery;
import dji.sdk.Camera.DJICameraSettingsDef;
import dji.sdk.Gimbal.DJIGimbal;
import dji.sdk.MissionManager.DJIWaypointMission.DJIWaypointMissionStatus;
import jxl.NumberCell;
import jxl.Sheet;
import jxl.Workbook;


import java.io.File;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import dji.sdk.Camera.DJICamera;
import dji.sdk.Codec.DJICodecManager;
import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType;
import dji.sdk.FlightController.DJIFlightControllerDelegate;
import dji.sdk.MissionManager.DJIMission;
import dji.sdk.MissionManager.DJIMissionManager;
import dji.sdk.MissionManager.DJIWaypoint;
import dji.sdk.MissionManager.DJIWaypointMission;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;

import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import static dji.sdk.AirLink.DJILBAirLink.*;
import static dji.sdk.FlightController.DJIFlightControllerDataType.*;
import static dji.sdk.MissionManager.DJIWaypointMission.*;
import static dji.sdk.MissionManager.DJIWaypointMission.DJIWaypointMissionExecuteState.*;


public class WayPointMissionActivity extends Activity
        implements
        View.OnClickListener,
        DJIMissionManager.MissionProgressStatusCallback,
        TextureView.SurfaceTextureListener {

    protected DJIMission mDJIMission;
    private DJIMissionManager mMissionManager;
    private DJIFlightController mFlightController;

    //飞行状态数据
    private int batteryPercent = 0;
    private float gimbalPitch = 0;
    private float speed = 10;
    protected double mHomeLatitude = 0;
    protected double mHomeLongitude = 0;
    private List<DJIWaypoint> wayPointsList;
    private List<WayPointInfo> wayPointsInfoList;
    private boolean isRun = true;
    private float startAltitude = 0;
    private boolean isSDCardInsert = false;
    private boolean isHeadThroughWay=true;
    private boolean showPointList=true;

    protected Context mContext;
    protected Button mPrepareBtn;
    protected Button mStartBtn;
    protected Button mStopBtn;
    protected Button mPauseBtn;
    protected Button mNoStopBtn;
    protected Button mSpeedBtn;
    protected Button mReadExcelBtn;
    protected Button mHomeLocationBtn;
    protected Button mShowListBtn;
    private Button flyTypeBtn;
    private TextureView cameraView;
    private TextView showStateView;
    private GridView gridView;
    private AlertDialog inputSpeedDialog;
    private AlertDialog flyTypeDialog;
    private TextView inputSpeedText;
    private TextView nowStatusView;
    private LinearLayout layout;

    private MyAdapter mMyAdapter;

    private boolean updateStatus = true;

    protected DJICodecManager mCodecManager = null;
    private DJIAircraft mProduct = null;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_way_point_mission);
        this.mContext = this;
        initUI(this, null);
        initSDKCallback();
        initStatus();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onResume() {
        /**
         * 设置为横屏
         */
        if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        super.onResume();
    }


    protected DJIMission initMission() {

        if (!Utils.checkGpsCoordinate(mHomeLatitude, mHomeLongitude)) {
            Utils.setResultToToast(this, "请先设置飞行器返航点。");
            return null;
        }
        if (wayPointsList == null) {
            Utils.setResultToToast(this, "请先导入航点数据。");
            showFileChooser();
            return null;
        }
        // create mission
        DJIWaypointMission waypointMission = new DJIWaypointMission();
        waypointMission.maxFlightSpeed = 15;
        waypointMission.autoFlightSpeed = speed;
        //waypointMission.finishedAction= DJIWaypointMission.DJIWaypointMissionFinishedAction.AutoLand;
        if(isHeadThroughWay){
            waypointMission.headingMode = DJIWaypointMissionHeadingMode.Auto;
        }else{
            waypointMission.headingMode = DJIWaypointMissionHeadingMode.UsingWaypointHeading;
        }

        waypointMission.flightPathMode = DJIWaypointMissionFlightPathMode.Normal;
        waypointMission.needRotateGimbalPitch = true;
        waypointMission.addWaypoints(wayPointsList);

        return waypointMission;
    }

    private void initUI(Context context, AttributeSet attrs) {

        mPrepareBtn = (Button) findViewById(R.id.btn_prepare);
        mStartBtn = (Button) findViewById(R.id.btn_start);
        mStopBtn = (Button) findViewById(R.id.btn_stop);
        mPauseBtn = (Button) findViewById(R.id.btn_pause);

        mNoStopBtn = (Button) findViewById(R.id.btn_nostop);
        mSpeedBtn = (Button) findViewById(R.id.button_speed);
        mHomeLocationBtn = (Button) findViewById(R.id.button);
        mReadExcelBtn = (Button) findViewById(R.id.button2);
        mShowListBtn=(Button)findViewById(R.id.btn_showList);
        flyTypeBtn=(Button)findViewById(R.id.flytype);

        cameraView = (TextureView) findViewById(R.id.textureView);
        cameraView.setSurfaceTextureListener(this);
        nowStatusView = (TextView) findViewById(R.id.nowStatusView);

        layout=(LinearLayout)findViewById(R.id.layout1);
        gridView = (GridView) findViewById(R.id.gridView);
        showStateView = (TextView) findViewById(R.id.textView10);

        mPrepareBtn.setOnClickListener(this);
        mStartBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        mPauseBtn.setOnClickListener(this);
        mNoStopBtn.setOnClickListener(this);
        mSpeedBtn.setOnClickListener(this);
        mHomeLocationBtn.setOnClickListener(this);
        mReadExcelBtn.setOnClickListener(this);
        flyTypeBtn.setOnClickListener(this);
        mShowListBtn.setOnClickListener(this);
        initDialog();
    }

    private void changeSpeed() {
        try {
            float newspeed = Float.valueOf(inputSpeedText.getText().toString());
            if (newspeed <= 0) {
                this.speed = 1;
                Utils.setResultToToast(mContext, "飞行器速度最小值为1，设置速度为：" + this.speed + "m/s");
            } else if (newspeed > 15) {
                this.speed = 15;
                Utils.setResultToToast(mContext, "飞行器速度最大值为15，设置速度为：" + this.speed + "m/s");
            } else {
                this.speed = newspeed;
                Utils.setResultToToast(mContext, "修改飞行器速度成功，速度为：" + this.speed + "m/s");
            }

        } catch (Exception e) {
            Utils.setResultToToast(mContext, "修改飞行器速度失败：" + e.getMessage());
        }
    }


    private void initDialog() {
        inputSpeedText = new EditText(getApplicationContext());

        AlertDialog.Builder inputSpeedDialogBuilder = new AlertDialog
                .Builder(mContext)
                .setTitle("请输入飞行速度(1~15 ,单位是米每秒)")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(inputSpeedText)
                .setPositiveButton(
                        "确定",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                changeSpeed();
                            }
                        })
                .setNegativeButton(
                        "取消",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //exitProgrames();
                                inputSpeedDialog.dismiss();
                            }
                        });

        inputSpeedDialog = inputSpeedDialogBuilder.create();

        AlertDialog.Builder flyTypeDialogBuilder =new AlertDialog
                .Builder(mContext)
                .setTitle("选择飞行模式")
                .setItems(
                        new String[]{"机头朝向飞行方向", "机头朝向指定方向"},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(i==0){
                                    isHeadThroughWay=true;
                                }else{
                                    isHeadThroughWay=false;
                                }
                            }
                        })
                .setPositiveButton(
                        "确定",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //setFlyType();
                            }
                        })
                .setNegativeButton(
                        "取消",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                flyTypeDialog.dismiss();
                            }
                        });
        flyTypeDialog=flyTypeDialogBuilder.create();
    }

    private void initSDKCallback() {
        try {
            mProduct = DJIApplication.getAircraftInstance();

            if (mProduct == null || !mProduct.isConnected()) {
                Utils.setResultToToast(this, "未找到飞行器");
                Utils.setResultToText(mContext, nowStatusView, "未找到飞行器");
                mMissionManager = null;
                mFlightController = null;
                return;
            }

            mFlightController = mProduct.getFlightController();
            mMissionManager = mProduct.getMissionManager();

            if (mProduct.getModel() != DJIBaseProduct.Model.UnknownAircraft) {
                //接收摄像头信息回调
                mProduct.getCamera().setDJICameraReceivedVideoDataCallback(
                        new DJICamera.CameraReceivedVideoDataCallback() {
                            @Override
                            public void onResult(byte[] videoBuffer, int size) {
                                if (null != mCodecManager) {
                                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                                }
                            }
                        });
                //获取SD卡状态回调
                mProduct.getCamera().setDJIUpdateCameraSDCardStateCallBack(
                        new DJICamera.CameraUpdatedSDCardStateCallback() {
                            @Override
                            public void onResult(DJICamera.CameraSDCardState cameraSDCardState) {
                                isSDCardInsert = cameraSDCardState.isInserted();
                            }
                        }
                );
            } else {
                mProduct.getAirLink().getLBAirLink().setDJIOnReceivedVideoCallback(
                        new DJIOnReceivedVideoCallback() {
                            @Override
                            public void onResult(byte[] videoBuffer, int size) {
                                if (mCodecManager != null) {
                                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                                }
                            }
                        });
            }

            //电池信息回调
            mProduct.getBattery().setBatteryStateUpdateCallback(
                    new DJIBattery.DJIBatteryStateUpdateCallback() {
                        @Override
                        public void onResult(DJIBattery.DJIBatteryState djiBatteryState) {
                            batteryPercent = djiBatteryState.getBatteryEnergyRemainingPercent();
                        }
                    }
            );

            // 更新初始位置回调
            mFlightController.setUpdateSystemStateCallback(new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {

                @Override
                public void onResult(DJIFlightControllerCurrentState state) {

                    mHomeLatitude = state.getHomeLocation().getLatitude();
                    mHomeLongitude = state.getHomeLocation().getLongitude();
                    //flightState = state.getFlightMode();
/*
                        Utils.setResultToText(mContext, mFCPushInfoTV, "home point latitude: " + mHomeLatitude +
                                "\nhome point longitude: " + mHomeLongitude +
                                "\nFlight state: " + flightState.name());
                                */

                }
            });

            //取消飞行限制回调
            mProduct.getFlightController().getFlightLimitation().setMaxFlightRadiusLimitationEnabled(false,
                    new DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Utils.setResultToToast(mContext, "取消飞行距离限制失败：" + djiError.getDescription());
                            }
                        }
                    });


            //云台状态回调
            mProduct.getGimbal().setGimbalStateUpdateCallback(
                    new DJIGimbal.GimbalStateUpdateCallback() {
                        @Override
                        public void onGimbalStateUpdate(DJIGimbal djiGimbal, DJIGimbal.DJIGimbalState djiGimbalState) {
                            if (djiGimbalState.getAttitudeInDegrees() != null) {

                                gimbalPitch = djiGimbalState.getAttitudeInDegrees().pitch;
                            }
                        }
                    }
            );

            //飞行过程信息回调
            mMissionManager.setMissionProgressStatusCallback(
                    new DJIMissionManager.MissionProgressStatusCallback() {
                        @Override
                        public void missionProgressStatus(DJIMissionProgressStatus djiMissionProgressStatus) {
                            DJIError err = djiMissionProgressStatus.getError();
                            if (err == null) {
                                //Utils.setResultToToast(mContext, "执行任务中…");
                            } else {
                                Utils.setResultToToast(mContext, "执行任务出错：" + err.getDescription());
                            }
                        }
                    }
            );
            mMissionManager.setMissionExecutionFinishedCallback(new DJICompletionCallback() {

                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        //成功
                        //Utils.setResultToToast(mContext, "任务执行完毕。");
                        Utils.setResultToText(mContext, nowStatusView, "任务执行完毕");
                    } else {
                        //失败
                        //Utils.setResultToText(mContext, nowStatusView, "任务执行出错");
                        Utils.setResultToToast(mContext, "任务执行出错：" + error.getDescription());
                    }
                    startLand();
                }
            });


        } catch (Exception exception) {
            Utils.setResultToToast(mContext, "初始化飞行器控制出错：" + exception.getLocalizedMessage());
        }
    }


    private void initStatus() {
        new Thread(new Runnable() {
            public void run() {
                while (updateStatus) {
                    try {
                        Thread.sleep(700);
                        DJIFlightControllerCurrentState djiFlightControllerCurrentState = mProduct.getFlightController().getCurrentState();
                        DJILocationCoordinate3D PlanePoint = djiFlightControllerCurrentState.getAircraftLocation();
                        String content = "";
                        //经纬度的小数点后保留位数
                        int scaleNum = 7;

                        //SD Card
                        try {
                            //content+="SD："+(isSDCardInsert?"yes":"no");
                        } catch (Exception e) {
                            //content += "SD：未知";
                        }

                        // 电量
                        try {
                            content += "电量：" + batteryPercent + "%";
                        } catch (Exception e) {
                            content += "电量：未知";
                        }

                        //GPS信号
                        try {
                            content += "\nGPS信号：" + djiFlightControllerCurrentState.getGpsSignalStatus();
                        } catch (Exception e) {
                            content += "\nGPS信号：未知";
                        }

                        //飞行模式
                        try {
                            content += "\n飞行模式：" + (isHeadThroughWay?"机头朝飞行方向":"机头朝指定方向");
                        } catch (Exception e) {
                            content += "\n飞行模式：未知";
                        }

                        //经纬度，高度，航向
                        try {
                            BigDecimal lat = new BigDecimal(PlanePoint.getLatitude());
                            lat = lat.setScale(scaleNum, BigDecimal.ROUND_HALF_UP);
                            BigDecimal lng = new BigDecimal(PlanePoint.getLongitude());
                            lng = lng.setScale(scaleNum, BigDecimal.ROUND_HALF_UP);
                            content += "\n经度：" + lng
                                    + "\n纬度：" + lat
                                    + "\n高度：" + PlanePoint.getAltitude()+" m";

                            float dir = djiFlightControllerCurrentState.getAircraftHeadDirection();
                            content += "\n航向：" + dir+"°";
                        } catch (Exception e) {
                            content = "\n经度：未知\n纬度：未知\n高度：未知\n航向：未知";
                        }

                        // 云台俯仰
                        try {
                            content += "\n云台俯仰：" + gimbalPitch + "°";
                        } catch (Exception e) {
                            content += "\n云台俯仰：未知";
                        }


                        // 设定云台俯仰
                        try {
                            //content+="设定云台俯仰："+setGimbalPitch+"°";
                        } catch (Exception e) {
                            //content += "设定云台俯仰：未知";
                        }

                        // 速度
                        try {
                            float vx = djiFlightControllerCurrentState.getVelocityX();
                            float vy = djiFlightControllerCurrentState.getVelocityY();
                            float vz = djiFlightControllerCurrentState.getVelocityZ();
                            double v = Math.sqrt(vx * vx + vy * vy + vz * vz);
                            BigDecimal tv = new BigDecimal(v);
                            content += "\n速度：" + tv.setScale(2, BigDecimal.ROUND_HALF_UP)+" m/s";
                        } catch (Exception e) {
                            content += "\n速度：未知";
                        }

                        //设定速度
                        try {
                            BigDecimal tv = new BigDecimal(speed);
                            content += "\n设定速度：" + tv.setScale(2, BigDecimal.ROUND_HALF_UP)+" m/s";
                        } catch (Exception e) {
                            content += "\n设定速度：未知";
                        }

                        //起飞点高度
                        try {
                            BigDecimal tv = new BigDecimal(speed);
                            content += "\n起飞点高度：" + startAltitude;
                        } catch (Exception e) {
                            content += "\n起飞点高度：未知";
                        }

                        //返航点经纬度
                        try {
                            //DJILocationCoordinate2D homePoint = djiFlightControllerCurrentState.getHomeLocation();

                            BigDecimal lat = new BigDecimal(mHomeLatitude);
                            lat = lat.setScale(scaleNum, BigDecimal.ROUND_HALF_UP);
                            BigDecimal lng = new BigDecimal(mHomeLongitude);
                            lng = lng.setScale(scaleNum, BigDecimal.ROUND_HALF_UP);
                            content += "\n返航点经度：" + lng
                                    + "\n返航点纬度：" + lat;
                        } catch (Exception e) {
                            content += "\n返航点经度：未知"
                                    + "\n返航点纬度：未知";
                        }

                        //距返航点距离
                        try {
                            double len=Utils.getDistance(PlanePoint.getLatitude(),PlanePoint.getLongitude(),mHomeLatitude,mHomeLongitude);
                            BigDecimal lend = new BigDecimal(len);
                            lend = lend.setScale(1, BigDecimal.ROUND_HALF_UP);
                            content += "\n距返航点：" + lend+" m";
                        } catch (Exception e) {
                            content += "\n距返航点：未知";
                        }

                        Utils.setResultToText(mContext, showStateView, content);

                    } catch (Exception e) {
                        Utils.setResultToToast(mContext, "状态获取出错：" + e.getLocalizedMessage());
                    }
                }
            }
        }).start();
    }


    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateStatus = true;

        if (mMissionManager != null) {
            //The callback method is implemented in the subclasses
            mMissionManager.setMissionProgressStatusCallback(this);
        }


        //拍照初始化
        if (isModuleAvailable()) {
            mProduct.getCamera().setCameraMode(
                    DJICameraSettingsDef.CameraMode.ShootPhoto,
                    new DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Utils.setResultToToast(mContext, "摄像头初始化出错：" + djiError.getDescription());
                            }
                        }
                    }
            );
        }
    }


    private boolean isModuleAvailable() {
        return (null != DJIApplication.getProductInstance())
                && (null != DJIApplication.getProductInstance().getCamera());
    }

    private void startLand() {
        if (mProduct.getFlightController().getCurrentState().isFlying()) {
            //如果飞行器在空中，则开始自动降落
            Utils.setResultToToast(mContext, "飞行器正在自动降落");
            Utils.setResultToText(mContext, nowStatusView, "飞行器正在自动降落");
            mProduct.getFlightController().autoLanding(
                    new DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Utils.setResultToText(mContext, nowStatusView, "飞行器悬停");
                                Utils.setResultToToast(mContext, "自动降落停止：" + djiError.getDescription());
                            }
                        }
                    }
            );
        }
    }

    private void stopLand() {
        mProduct.getFlightController().cancelAutoLanding(
                new DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null) {
                            Utils.setResultToToast(mContext, "中止了飞行器的降落。");
                            Utils.setResultToText(mContext, nowStatusView, "飞行器悬停");
                        } else {
                            Utils.setResultToToast(mContext, "中止降落时出错：" + djiError.getDescription());
                        }
                    }
                }
        );
    }

    /**
     * 添加一个控制点到待提交的任务序列中
     * 必须先执行初始化任务（initMission）后方可调用
     *
     * @param name        名称，为String值
     * @param latitude    纬度，为double值实数
     * @param longitude   经度，为double值实数
     * @param height      海拔高度，为float值实数。实际飞行中将其值减去startAltitude转化为距离开始点的高度。
     * @param yaw         航向，取值-180到180的整数
     * @param gimbalAngle 云台的摄像机俯仰角，取值-90到0的整数
     * @param takePhoto   是否拍照，布尔值，true为拍照，false为不拍照
     */
    private void addPoint(String name, double latitude, double longitude, double height, int yaw, int gimbalAngle, boolean takePhoto) {
        if (wayPointsList == null) return;

        float realHeight = (float) (height - startAltitude);

        DJIWaypoint newPoint = new DJIWaypoint(latitude, longitude, realHeight);
        WayPointInfo newPointInfo = new WayPointInfo(name, newPoint);

        //航向整数，-180到180
        newPoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.RotateAircraft, yaw));
        //摄像头角度，-90到0
        //newPoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.GimbalPitch, gimbalAngle));
        newPointInfo.gimbal = gimbalAngle;
        newPointInfo.isTakePhoto = takePhoto;
        if (takePhoto) {
            newPoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.StopRecord, 0));
            newPoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.StartTakePhoto, 0));
            newPoint.addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.StartRecord, 0));
        }
        if (wayPointsList.size() > 0) {
            int oldyaw = wayPointsList.get(wayPointsList.size() - 1).heading;
            int newyaw = yaw;
            boolean ssj = true;
            if (oldyaw < 0) oldyaw = 360 + oldyaw;
            if (newyaw < 0) newyaw = 360 + newyaw;
            if (
                    (oldyaw < 180 && (newyaw <= oldyaw || newyaw >= oldyaw + 180))
                            ||
                            (oldyaw >= 180 && (newyaw <= oldyaw && newyaw >= oldyaw - 180))) {
                ssj = false;
                wayPointsList.get(wayPointsList.size() - 1).turnMode = DJIWaypoint.DJIWaypointTurnMode.CounterClockwise;
            } else {
                ssj = true;
                wayPointsList.get(wayPointsList.size() - 1).turnMode = DJIWaypoint.DJIWaypointTurnMode.Clockwise;
            }
            //Utils.setResultToToast(this, "old:"+oldyaw+",new:"+newyaw+","+ssj);
        }

        newPoint.heading = (short) yaw;
        newPoint.actionTimeoutInSeconds = 200;
        wayPointsList.add(newPoint);


        wayPointsInfoList.add(newPointInfo);
    }

    private void prepareFly() {
        //准备阶段。读入坐标位置
        //初始化mission
        mDJIMission = initMission();
        if (mDJIMission == null) {
            Utils.setResultToToast(mContext, "任务生成失败");
        }
        //提交mission
        mMissionManager.prepareMission(mDJIMission, new DJIMissionProgressHandler() {

            @Override
            public void onProgress(DJIProgressType type, float progress) {
                Utils.setResultToText(mContext, nowStatusView, "正在上传任务到飞行器:" + (int) (progress * 100f) + "%");
            }

        }, new DJICompletionCallback() {


            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    Utils.setResultToText(mContext, nowStatusView, "任务成功上传到飞行器");
                    Utils.setResultToToast(mContext, "任务成功上传到飞行器!");
                } else {
                    Utils.setResultToToast(mContext, "任务上传错误: " + error.getDescription());
                }
            }
        });
    }

    private void startFly() {

        new AlertDialog.Builder(this).setTitle("确认开始执行飞行任务吗？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 点击“确认”后的操作
                        //检查SD卡
                        if (!isSDCardInsert) {
                            Utils.setResultToToast(mContext, "警告：SD卡未插入，无法开始任务。");
                            return;
                        }
                        //检查电量
                        if (batteryPercent < 90) {
                            Utils.setResultToToast(mContext, "警告：飞行器电量小于90%");
                        }
                        //开始执行任务
                        if (mDJIMission != null) {
                            Utils.setResultToToast(mContext, "飞行器开始执行任务。");
                            mMissionManager.startMissionExecution(new DJICompletionCallback() {

                                @Override
                                public void onResult(DJIError mError) {

                                    if (mError == null) {
                                    } else {
                                        Utils.setResultToToast(mContext, "启动任务时出错: " + mError.getDescription());
                                    }
                                }
                            });
                        }
                    }
                })
                .setNegativeButton("返回", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 点击“返回”后的操作
                    }
                }).show();
    }

    private void pauseFly() {
        // 暂停

        mMissionManager.pauseMissionExecution(new DJICompletionCallback() {

            @Override
            public void onResult(DJIError mError) {
                if (mError == null) {
                    Utils.setResultToToast(mContext, "任务暂停");
                    isRun = false;
                    Utils.changeButtonText(mContext, mPauseBtn, "继续飞行任务");
                } else {
                    Utils.setResultToToast(mContext, "任务暂停出错: " + mError.getDescription());
                }
            }
        });
    }

    private void resumeFly() {
        //继续

        mMissionManager.resumeMissionExecution(new DJICompletionCallback() {

            @Override
            public void onResult(DJIError mError) {
                if (mError == null) {
                    Utils.setResultToToast(mContext, "任务继续进行!");
                    isRun = true;
                    Utils.changeButtonText(mContext, mPauseBtn, "暂停飞行任务");
                } else {
                    Utils.setResultToToast(mContext, "任务继续时出错:" + mError.getDescription());
                }
            }
        });
    }

    private void stopFly() {
        new AlertDialog.Builder(this).setTitle("要停止飞行任务吗？停止后飞行器将自动降落。")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("停止", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 点击“确认”后的操作
                        mProduct.getCamera().stopRecordVideo(new DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {

                            }
                        });
                        mMissionManager.stopMissionExecution(new DJICompletionCallback() {

                            @Override
                            public void onResult(DJIError mError) {
                                if (mError == null) {
                                    Utils.setResultToToast(mContext, "任务中止。");
                                } else {
                                    Utils.setResultToToast(mContext, "任务中止出错: " + mError.getDescription());
                                }
                                startLand();
                            }
                        });

                    }
                })
                .setNegativeButton("返回", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();

    }


    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        updateStatus = false;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
                                          int height) {
        // TODO Auto-generated method stub
        Log.i("TAG", "onSurfaceTextureAvailable...");
        if (mCodecManager == null) {
            Log.e("TAG", "mCodecManager is null 2");
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        // TODO Auto-generated method stub
        Log.i("TAG", "onSurfaceTextureDestroyed...");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        //CameraInterface.getInstance().doStopCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
                                            int height) {
        // TODO Auto-generated method stub
        Log.i("TAG", "onSurfaceTextureSizeChanged...");
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // TODO Auto-generated method stub
        Log.i("TAG", "onSurfaceTextureUpdated...");

    }

    private void setHome() {

        mProduct.getFlightController().setHomeLocationUsingAircraftCurrentLocation(
                new DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        Utils.setResultToToast(
                                mContext,
                                "设置当前飞行器位置为返航点: " + (djiError == null ?
                                        "成功" : djiError.getDescription()));

                        //更新返航点数据
                        mFlightController.getHomeLocation(
                                new DJICompletionCallbackWith<DJILocationCoordinate2D>() {
                                    @Override
                                    public void onSuccess(DJILocationCoordinate2D t) {
                                        //更新返航点数据
                                        mHomeLatitude = t.getLatitude();
                                        mHomeLongitude = t.getLongitude();
                                    }

                                    @Override
                                    public void onFailure(DJIError djiError) {
                                    }
                                }
                        );
                    }
                });

    }


    private void setGimbalPitch(int nowIndex) {
        final float nowGimbal = wayPointsInfoList.get(nowIndex).gimbal;
        mProduct.getGimbal().rotateGimbalByAngle(
                DJIGimbal.DJIGimbalRotateAngleMode.AbsoluteAngle,
                new DJIGimbal.DJIGimbalAngleRotation(true, nowGimbal, DJIGimbal.DJIGimbalRotateDirection.CounterClockwise),
                null,
                null,
                new DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                    }
                });
    }

    private String getStatusChineseName(DJIWaypointMissionExecuteState status) {
        String nowStatusName = "无";
        switch (status) {
            case Initializing:
                nowStatusName = "正飞向第一个航点";
                break;
            case Moving:
                nowStatusName = "正飞向下一航点";
                break;
            case DoingAction:
                nowStatusName = "正执行航点动作";
                break;
            case FinishedAction:
                nowStatusName = "执行航点动作完毕";
                break;
            case BeginAction:
                nowStatusName = "开始执行航点动作";
                break;
            case ReturnToFirstWaypoint:
                nowStatusName = "正返回第一个航点";
                break;
        }
        return nowStatusName;
    }


    //任务状态回调
    @Override
    public void missionProgressStatus(DJIMissionProgressStatus progressStatus) {
        try{
            if (progressStatus == null) {
                return;
            }
            // StringBuffer pushSB = new StringBuffer();
            if (progressStatus instanceof DJIWaypointMissionStatus) {
                DJIWaypointMissionStatus status = (DJIWaypointMissionStatus) progressStatus;
                int nowIndex = status.getTargetWaypointIndex();
                String nowName = wayPointsInfoList.get(nowIndex).name;
                final float nowGimbal = wayPointsInfoList.get(nowIndex).gimbal;

                DJIWaypointMissionExecuteState nowStatus = status.getExecState();
                String nowStatusName = getStatusChineseName(nowStatus);

                //刷新状态提示信息
                Utils.setResultToText(mContext, nowStatusView, nowStatusName + ",下一航点:" + nowName + ",云台" + nowGimbal + "°");

                if ((nowStatus == Initializing)
                        ||
                        (nowStatus == Moving)) {
                    //途中修改云台俯仰
                    setGimbalPitch(nowIndex);
                }
                //途中修改飞行器速度
                DJIWaypointMission.setAutoFlightSpeed(speed, new DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                    }
                });
            }
        }catch(Exception e){
            Utils.setResultToText(mContext, nowStatusView, "获取飞行状态信息出错");
            //Utils.setResultToToast(mContext, "出错："+e.getMessage());
        }

    }

    private boolean isSetHomePoint() {
        if (
                DJIApplication.getProductInstance() instanceof DJIAircraft &&
                        !Utils.checkGpsCoordinate(mHomeLatitude, mHomeLongitude) &&
                        mFlightController != null
                ) {
            final CountDownLatch cdl = new CountDownLatch(1);
            mFlightController.getHomeLocation(new DJICompletionCallbackWith<DJILocationCoordinate2D>() {

                @Override
                public void onSuccess(DJILocationCoordinate2D t) {
                    //更新返航点数据
                    mHomeLatitude = t.getLatitude();
                    mHomeLongitude = t.getLongitude();
                }

                @Override
                public void onFailure(DJIError error) {
                    cdl.countDown();
                }
            });
            try {
                cdl.await(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (!Utils.checkGpsCoordinate(mHomeLatitude, mHomeLongitude)) {
                Utils.setResultToToast(mContext, "未设置飞行器返航点");
                return false;
            }
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        mMissionManager = DJIMissionManager.getInstance();
        //先检查返航点是否设置

        switch (v.getId()) {
            case R.id.btn_prepare:
                if (!isSetHomePoint()) break;
                prepareFly();
                break;
            case R.id.btn_start:
                if (!isSetHomePoint()) break;
                startFly();
                break;
            case R.id.btn_stop:
                if (!isSetHomePoint()) break;
                stopFly();
                break;
            case R.id.btn_pause:
                if (!isSetHomePoint()) break;
                if (isRun) {
                    pauseFly();
                } else {
                    resumeFly();
                }
                break;
            case R.id.button:
                //设置返航点
                setHome();
                break;
            case R.id.button2:
                //打开文件
                showFileChooser();
                break;
            case R.id.button_speed:
                //修改速度
                inputSpeedDialog.show();
                break;
            case R.id.flytype:
                //修改机头移动方式
                flyTypeDialog.show();
                break;
            case R.id.btn_nostop:
                //中止降落
                stopLand();
                break;
            case R.id.btn_showList:
                //打开或隐藏点列表栏
                if(this.showPointList){
                    //关闭
                    showPointList=false;
                    Utils.changeButtonText(mContext, mShowListBtn, "+");
                    Utils.changeVisibility(mContext, layout, false);
                }else{
                    //打开
                    showPointList=true;
                    Utils.changeButtonText(mContext, mShowListBtn, "-");
                    Utils.changeVisibility(mContext, layout, true);
                }
            default:
                break;

        }
    }

    private double getMeter(double meter) {
        return meter * Utils.ONE_METER_OFFSET;
    }

    private void updateGridView() {
        mMyAdapter = new MyAdapter(this.wayPointsInfoList);
        gridView.setAdapter(mMyAdapter);
        gridView.setNumColumns(7);
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "WayPointMission Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.deathkon.dji.flyingtest/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "WayPointMission Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.deathkon.dji.flyingtest/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }


    private class MyAdapter extends BaseAdapter {
        public LinkedList<WayPointInfo> data;
        public int colNum = 7;

        public MyAdapter(List<WayPointInfo> pointList) {
            data = (LinkedList) pointList;
        }

        @Override
        public int getCount() {
            return (data.size() + 1) * colNum;
        }

        @Override
        public Object getItem(int arg0) {
            return data.get(arg0);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView mTextView = new TextView(getApplicationContext());

            String content = "";
            int rowN = position % colNum;
            int colN = position / colNum;
            int dataColN = colN - 1;
            switch (rowN) {
                case 0:
                    content = (colN == 0 ? "名称" : String.valueOf(data.get(dataColN).name));
                    break;
                case 1:
                    content = (colN == 0 ? "经度" : new DecimalFormat("#.000").format(data.get(dataColN).point.longitude ).toString());
                    break;
                case 2:
                    content = (colN == 0 ? "纬度" : new DecimalFormat("#.000").format(data.get(dataColN).point.latitude).toString());
                    break;
                case 3:
                    content = (colN == 0 ? "海拔" : new DecimalFormat("#.0").format(data.get(dataColN).point.altitude + startAltitude).toString());
                    break;
                case 4:
                    content = (colN == 0 ? "航向" : new DecimalFormat("#.0").format(data.get(dataColN).point.heading).toString());
                    break;
                case 5:
                    content = (colN == 0 ? "云台角" : new DecimalFormat("#.0").format(data.get(dataColN).gimbal).toString());
                    break;
                case 6:
                    content = (colN == 0 ? "拍照" : (data.get(dataColN).isTakePhoto ? "是" : "否"));
                    break;
            }

            mTextView.setText(content);
            mTextView.setTextColor(Color.BLACK);
            //mTextView.setBackgroundColor(icolor[position]);
            return mTextView;
        }

    }


    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult((Intent.createChooser(intent, "请选择文件")), 1);
        } catch (ActivityNotFoundException ex) {
            Utils.setResultToToast(mContext, "请安装文件管理器");
        }
    }

    private String getRealPath(Uri fileUrl) {
        String fileName = null;
        Uri filePathUri = fileUrl;
        if (fileUrl != null) {
            if (fileUrl.getScheme().toString().compareTo("content") == 0)           //content://开头的uri
            {
                Cursor cursor = mContext.getContentResolver().query(fileUrl, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    fileName = cursor.getString(column_index);          //取出文件路径
                    if (!fileName.startsWith("/mnt")) {
                        //检查是否有”/mnt“前缀

                        //fileName = "/mnt" + fileName;
                    }
                    cursor.close();
                }
            } else if (fileUrl.getScheme().compareTo("file") == 0)         //file:///开头的uri
            {
                fileName = filePathUri.toString().replace("file://", "");
                //fileName = "/mnt" + fileName;
            }
        }
        return fileName;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            try {
                //是否选择，没选择就不会继续
                Uri uri = data.getData();
                //Utils.setResultToEditText(mContext,editText,uri.toString());
                String realPath = getRealPath(uri);

                readExcel(realPath);
            } catch (Exception e) {
                Utils.setResultToToast(mContext, "打开文件出错：" + e.getMessage());
            }

        }
    }

    private void readExcel(String filePath) {

        this.wayPointsList = new LinkedList<>();
        this.wayPointsInfoList = new LinkedList<>();
        try {
            Workbook workbook = null;
            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    //中文路径，解码之
                    String realPath = URLDecoder.decode(filePath, "UTF-8");
                    file = new File(realPath);
                }
                workbook = Workbook.getWorkbook(file);
            } catch (Exception e) {
                Utils.setResultToToast(mContext, "打开文件出错：" + e.getMessage());
            }
            //得到第一张表
            Sheet sheet = workbook.getSheet(0);
            //列数
            int columnCount = sheet.getColumns();


            //行数
            //int rowCount = sheet.getRows();
            int rowCount = 0;
            for (int i = 0; i < sheet.getRows(); i++) {
                if (sheet.getCell(0, i) != null && sheet.getCell(0, i).getContents().length() > 0)
                    rowCount++;
            }

            //读取起始点海拔值
            startAltitude = (float) ((NumberCell) sheet.getCell(1, 0)).getValue();
            //掠过前2行，从第三行开始读数据
            for (int row = 2; row < rowCount; row++) {
                String name = sheet.getCell(0, row).getContents();
                double longitude = ((NumberCell) sheet.getCell(1, row)).getValue();
                double latitude = ((NumberCell) sheet.getCell(2, row)).getValue();
                float height = (float) ((NumberCell) sheet.getCell(3, row)).getValue();
                int yaw = (int) ((NumberCell) sheet.getCell(4, row)).getValue();
                int gimbalAngle = (int) ((NumberCell) sheet.getCell(5, row)).getValue();
                String isGetPhotoStr = sheet.getCell(6, row).getContents();
                boolean isGetPhoto = false;
                if (isGetPhotoStr.equals("Y")) {
                    isGetPhoto = true;
                }
                addPoint(name, latitude, longitude, height, yaw, gimbalAngle, isGetPhoto);
            }
            //在第一个点时开始采集图像

            wayPointsList.get(0).addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.StartRecord, 0));
            //在最后一个点停止采集图像
            if(wayPointsInfoList.get(wayPointsInfoList.size()-1).isTakePhoto){
                //防止在最后一个点时出现多余录像动作
                wayPointsList.get(wayPointsList.size()-1).removeActionAtIndex(wayPointsList.get(wayPointsList.size()-1).waypointActions.size()-1);
            }else{
                wayPointsList.get(wayPointsList.size() - 1).addAction(new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.StopRecord, 0));
            }
            workbook.close();
            updateGridView();
        } catch (Exception e) {
            Utils.setResultToToast(mContext, "读取文件出错：" + e.getMessage());
        }
    }
}
