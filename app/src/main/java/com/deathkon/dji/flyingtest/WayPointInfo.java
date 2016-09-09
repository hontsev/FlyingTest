package com.deathkon.dji.flyingtest;

import dji.sdk.MissionManager.DJIWaypoint;

/**
 * Created by Administrator on 2016.04.12 012.
 */
public class WayPointInfo {
    public DJIWaypoint point ;
    public boolean isTakePhoto;
    public int gimbal;
    public String name;
    public WayPointInfo(String name,DJIWaypoint point){
        this.name=name;
        this.point=point;
    }

}
