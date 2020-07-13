package com.youdeyi.serialport.data;

/**
 * @author Rui Chaoqun
 * @date :2019/11/9 9:23
 * description:电机info
 */
public class MotorRequestInfo {

    //电机类型   0：单路电机   1：多路电机
    private int type;

    //电机ID
    private int id;

    //多路电机数
    private int count;

    //原始id
    private String originalId;


    public MotorRequestInfo() {
    }

    public MotorRequestInfo(int type, int id, int count) {
        this.type = type;
        this.id = id;
        this.count = count;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getOriginalId() {
        return originalId;
    }

    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }
}
