package com.youdeyi.serialport;

/**
 * @author Rui Chaoqun
 * @date :2019/11/8 9:29
 * description:
 */
public class Constant {

    //查询身份信息
    public static final String SEARCH_STATE = "01";

    //查询状态 POLL 轮询指令
    public static final String POLL_STSTUS = "03";

    //设置温度
    public static final String SET_TEMPRETOR = "04";

    //启动电机
    public static final String START_MOTOR = "05";

    //ACK 结果确认
    public static final String ACK_CONFIRM = "06";

    //RUN2 启动 多路电机
    public static final String START_MULTIPLE_MOTOR  = "15";

    //补货灯开
    public static final String LIGHT_OPEN  = "0F";

    //补货灯关
    public static final String LIGHT_CLOSE  = "10";

    //电动门
    public static final String ELECTRONIC_GATE = "16";

    //传送带
    public static final String CONVEYOR_CONTROL = "14";

    //制冷
    public static final String REFRIGERATION_CONTROL = "11";

    //药柜内照明灯
    public static final String MACHINE_LIGHT = "12";

    //灯箱控制
    public static final String LIGHT_BOX_CONTROL = "13";

    public static final String FULL_POLL_STSTUS = getCommand(POLL_STSTUS);

    public static final String FULL_ACK_CONFIRM = getCommand(ACK_CONFIRM);

    public static final String COMMAND_OPEN_DOOR = getCommand(ELECTRONIC_GATE+"01");

    public static final String COMMAND_CLOSE_DOOR = getCommand(ELECTRONIC_GATE+"00");

    public static final String COMMAND_START_CONVEYOR = getCommand(CONVEYOR_CONTROL+"01");

    public static final String COMMAND_CLOSE_CONVEYOR = getCommand(CONVEYOR_CONTROL+"00");


    public static String getCommand(String command){
        return DeviceKey.BOARD_ADDRESS+command+ CRCUtils.getCRC(DeviceKey.BOARD_ADDRESS+command);
    }
}
