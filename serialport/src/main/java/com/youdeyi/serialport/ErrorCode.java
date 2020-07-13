package com.youdeyi.serialport;

/**
 * @author Rui Chaoqun
 * @date :2019/11/8 10:20
 * description:错误码
 */
public enum  ErrorCode {
    //
    ERROR_SERIALPORT_OPEN(200000,"串口打开失败"),
    ERROR_INVALID_INDEX(200002,"无效的电机索引"),
    ERROR_ORTHER_RUNNING(200003,"另一个电机正在运行"),
    ERROR_OTHER_RESULT_NOT_TAKE_OFF(200004,"另一台电机的运转结果还未取走"),
    ERROR_PARSE(200009,"指令解析错误--"),
    ERROR_DROP_LIGHT_NO_DETECT(200010,"掉货光眼未检测到物品"),
    ERROR_OPEN_LIGHT(200011,"打开补货灯失败，表示无效的电机索引，或者 2 个电机号不是一层"),
    ERROR_GOOD_NOT_TAKE_OVER(200012,"门关 货没有取走"),
    ERROR_READ_COMMAND_TIMEOUT(200013,"读取指令超时-->"),
    ERROR_SHIPMENT_MOTOR_FAULT(200014,"出货电机故障"),
    ERROR_GATE(200016,"出货电动门卡住"),
    ERROR_CONNECT_FAULT(200015,"主板与分板通信故障"),;


    private String msg;
    private int errCode;

    ErrorCode(int errCode,String msg) {
        this.msg = msg;
        this.errCode = errCode;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getErrCode() {
        return errCode;
    }

    public void setErrCode(int errCode) {
        this.errCode = errCode;
    }

    @Override
    public String toString() {
        return errCode+"   "+msg;
    }
}
