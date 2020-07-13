package com.youdeyi.serialport.data;

/**
 * @author Rui Chaoqun
 * @date :2019/11/8 15:13
 * description:轮询指令解析info
 */
public class CheckStatusInfo {
    /**
     * 控制板状态 0=空闲 1=出货中 2=出货结束（注意仅表示结束， 不代表成功或者失败）。
     * 状态为 2（出货结束）时， 需要发送 ACK 确认帧，控制板收到确认帧后，状 态会变为 0（空闲），否则不能接收 RUN 启动电机 指令。
     */
    private int state;

    /**
     * 电机索引
     */
    private int motorIndex;

    /**
     * 实时温度  数值：0---127; 单位：℃
     */
    private int currentTemp;

    /**
     * 实时湿度  数值：0---100; 单位：%
     */
    private int currentHumidity;


    /**
     * 掉货光眼状态 0掉货光眼检查到物体 1未检查到
     */
    private int dropLightEyeState;


    /**
     * 其它状态 0出货电机正常，1出货电机故障，2主板与分板通信故 障，
     */
    private int otherState;

    /**
     * 传送带状态 01 正在运行  00停止
     */
    private int conveyorState;

    /**
     * 门状态 0：门关  1：门开  2：正在开关门
     */
    private int doorState;

    /**
     * 取货口是否有货
     */
    private boolean haveCargo;

    /**
     * 压缩机是否已打开
     */
    private boolean isCompressorOpen;

    /**
     * 灯箱控制是否打开
     */
    private boolean isLightBoxControllOpen;

    /**
     * 药柜内照明灯是否已打开
     */
    private boolean isFloodLightOpen;



    public static CheckStatusInfo parseCheckStatusInfo(String hexStr){
        CheckStatusInfo info = null;
        try {
            info = new CheckStatusInfo();
            info.motorIndex  = Integer.parseInt(hexStr.substring(4,8),16);
            info.state = Integer.parseInt(hexStr.substring(8,10),16);
            info.dropLightEyeState = Integer.parseInt(hexStr.substring(10,12),16);
            info.conveyorState = Integer.parseInt(hexStr.substring(12,14),16);
            info.doorState = Integer.parseInt(hexStr.substring(14,16),16);
            info.otherState = Integer.parseInt(hexStr.substring(16,18),16);
            int machineState = Integer.parseInt(hexStr.substring(18,20),16);
            info.haveCargo = (machineState%2) != 0;
            info.isCompressorOpen = (machineState>>2)%2 != 0;
            info.isFloodLightOpen = (machineState>>3)%2 != 0;
            info.isLightBoxControllOpen = (machineState>>4)%2 != 0;
            info.currentTemp = Integer.parseInt(hexStr.substring(20,22),16);
            info.currentHumidity = Integer.parseInt(hexStr.substring(22,24),16);
        }catch (Exception e){
            e.printStackTrace();
        }
        return info;
    }

    public int getConveyorState() {
        return conveyorState;
    }

    public int getDoorState() {
        return doorState;
    }

    public boolean isHaveCargo() {
        return haveCargo;
    }

    public boolean isCompressorOpen() {
        return isCompressorOpen;
    }

    public boolean isLightBoxControllOpen() {
        return isLightBoxControllOpen;
    }

    public boolean isFloodLightOpen() {
        return isFloodLightOpen;
    }

    public int getState() {
        return state;
    }

    public int getMotorIndex() {
        return motorIndex;
    }


    public int getCurrentTemp() {
        return currentTemp;
    }

    public int getCurrentHumidity() {
        return currentHumidity;
    }

    public int getDropLightEyeState() {
        return dropLightEyeState;
    }

    public int getOtherState() {
        return otherState;
    }
}
