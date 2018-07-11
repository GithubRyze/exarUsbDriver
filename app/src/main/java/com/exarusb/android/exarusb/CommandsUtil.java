package com.exarusb.android.exarusb;

/**
 * Created by ryze.liu on 7/9/2018.
 */

public final class CommandsUtil {

    private CommandsUtil(){
        throw new UnsupportedOperationException("stub");
    }
    //register command and response
    public final  static  byte[] COMMAND_REGISTER_USER = {0x02,0x01,0x0,0x0,0x0,0x03};
    public final static byte[] COMMAND_RECEIVE_REGISTER_ACK_SUCCESS = {0x02,0x01,0x07,0x00,0x00,0x04};
    public final static byte[] COMMAND_RECEIVE_START_REGISTER = {0x02,0x01,0x08,0x00,0x00,0x0B};
    //public final static byte[] COMMAND_RECEIVE_REGISTER_SUCCESS = {0x02,0x01,0x10,0x00,0x00,0x13};
    public final static byte[] COMMAND_RECEIVE_REGISTER_ERROR_1 = {0x02,0x01,0x50,0x00,0x00,0x53};
    public final static byte[] COMMAND_RECEIVE_REGISTER_ERROR_2 = {0x02,0x01,0x51,0x00,0x00,0x52};
    public final static byte[] COMMAND_RECEIVE_REGISTER_ERROR_3 = {0x02,0x01,0x52,0x00,0x00,0x51};

    //capture command and response
    public final static byte[] COMMAND_CAPTURE_JPEG = {0x02,0x09,0x00,0x00,0x0,0x0B};
    public final static byte[] COMMAND_RECEIVE_REQUEST_SEND_JPEG_ACK_SUCCESS = {0x02,0x09,0x07,0x00,0x00,0x0c};
    public final static byte[] COMMAND_RECEIVE_JPEG_SEND_START = {0x02,0x09,0x08,0x00,0x00,0x03};

    //chech version command and response
    public final static byte[] COMMADN_FMI_CHECH_VERSION = {0x02,0x0d,0x0,0x0,0x0,0x0f};
    public final static byte[] COMMAND_RECEIVE_GET_VERSION = {0x02,0x0d,0x3,0x2,0x0,0x0e};

    //clear command and response
    public final static byte[] COMMAND_CLEAR_RECORD = {0x02,0x03,0x0,0x0,0x0,0x01};
    public final static byte[] COMMAND_RECEIVE_CLEAR_ACK_SUCCESS = {0x02,0x03,0x07,0x00,0x00,0x06};
    public final static byte[] COMMAND_RECEIVE_CLEAR_START = {0x02,0x03,0x08,0x00,0x00,0x09};
    public final static byte[] COMMAND_RECEIVE_CLEAR_SUCCESS = {0x02,0x03,0x10,0x00,0x00,0x11};
    //public final static byte[] COMMAND_RESET_PREVIEW = {};

    //delete command and response
    public final static byte[] COMMADN_DELETE_USER = {0x02,0x08,0x00,0x00,0x00,0x0A};
    public final static byte[] COMMAND_RECEIVE_DELETE_ACK_SUCCESS = {0x02,0x08,0x07,0x00,0x00,0x0d};
    public final static byte[] COMMAND_RECEIVE_DELETE_START = {0x02,0x08,0x08,0x00,0x00,0x02};
    public final static byte[] COMMAND_RECEIVE_DELETE_SUCCESS = {0x02,0x08,0x10,0x00,0x00,0x1a};


    // led set command and response
    public final static byte[] COMMADN_LED_OFF = {0x02,0x12,0x0,0x0,0x0,0x10};
    public final static byte[] COMMADN_LED_LEVEL_O1 = {0x02,0x12,0x1,0x0,0x0,0x11};
    public final static byte[] COMMADN_LED_LEVEL_O2 = {0x02,0x12,0x2,0x0,0x0,0x12};
    public final static byte[] COMMADN_LED_LEVEL_O3 = {0x02,0x12,0x3,0x0,0x0,0x13};
    public final static byte[] COMMAND_RECEIVE_SET_LED_SUCCESS = {0x02,0x12,0x10,0x00,0x00,0x10};
    public final static byte[] COMMAND_RECEIVE_SET_LED_FAILED = {0x02,0x12,0x50,0x00,0x00,0x40};


    //face enable command and response
    public final static byte[] COMMADN_FACE_ENABLE = {0x02,0x13,0x00,0x0,0x0,0x11};
    public final static byte[] COMMADN_FACE_DISABLE = {0x02,0x13,0x01,0x0,0x0,0x10};
    public final static byte[] COMMAND_RECEIVE_SET_FACE_AUTO_REG_ENABLE = {0x02,0x13,0x10,0x00,0x00,0x01};

    //unknow command response
    public final static byte[] COMMAND_RECEIVE_UNKNOW_CMD = {0x02,0x0,0x00,0x00,0x00,0x02};
    //fmi is ready response
    public final static byte[] COMMAND_RECEIVE_NOTICE_HOST_FRM_IS_READY = {0x02,0x04,0x0,0x0,0x00,0x06};








    public final static byte[] COMMAND_RECEIVE_RECOGNITION_ACK_SUCCESS = {0x02,0x02,0x07,0x00,0x00,0x07};
    public final static byte[] COMMAND_RECEIVE_RECOGNITION_START = {0x02,0x02,0x08,0x00,0x00,0x08};
    public final static byte[] COMMAND_RECEIVE_RECOGNITION_UNKNOW_ERROR = {0x02,0x02,0x50,0x00,0x00,0x50};
    public final static byte[] COMMAND_RECEIVE_RECOGNITION_NO_PEOPLE_ERROR= {0x02,0x02,0x52,0x00,0x00,0x52};
    public final static byte[] COMMAND_RECEIVE_RECOGNITION_IMPOSTER_ERROR = {0x02,0x02,0x53,0x00,0x00,0x53};



    public static String[] compareBytes(byte[] bytes){

        if (bytes.length != 6 && (bytes[0] ^ bytes[1] ^ bytes[2] ^ bytes[3] ^ bytes[4]) != bytes[5])
            return new String[]{"unknown command,command format is wrong","1"};


        if(bytes[0] == 0x02 && bytes[1] == 0x01){
            //todo register response
            if(compareBytes(bytes,COMMAND_RECEIVE_REGISTER_ACK_SUCCESS)){
                return new String[]{"register ack success","0"};
            }else if(compareBytes(bytes,COMMAND_RECEIVE_START_REGISTER)){
                return new String[]{"register start","0"};
            }else if(bytes[3] == 0x10){
                return new String[]{"register success","1"};
            } else if(compareBytes(bytes,COMMAND_RECEIVE_REGISTER_ERROR_1)){
                return new String[]{"no face was detected","1"};
            }else if(compareBytes(bytes,COMMAND_RECEIVE_REGISTER_ERROR_2)){
                return new String[]{"user register is full","1"};

            }else if(compareBytes(bytes, COMMAND_RECEIVE_REGISTER_ERROR_3)){
                return new String[]{"register is occurred other error","1"};
            }
        }else if(bytes[0] == 0x02&& bytes[1] == 0x09){
            //todo capture response
            if(compareBytes(bytes , COMMAND_RECEIVE_REQUEST_SEND_JPEG_ACK_SUCCESS)){
                return new String[]{"capture jpeg ack success","0"};
            }else if(compareBytes(bytes , COMMAND_RECEIVE_JPEG_SEND_START)){
                return new String[]{"capture jpeg start","0"};
            }
        }
        else if(bytes[0] == 0x02&& bytes[1] == 0x0d){
            //todo version response
            String version = Integer.toHexString(bytes[2]) + "." + Integer.toHexString(bytes[3]);
            return new String[]{"Fmi version is " + version,"1"};

        }else if(bytes[0] == 0x02 && bytes[1] == 0x03){
            //todo clear response
            if(compareBytes(bytes, COMMAND_RECEIVE_CLEAR_ACK_SUCCESS)){
                return new String[]{"clear ack success","0"};
            }else if(compareBytes(bytes, COMMAND_RECEIVE_CLEAR_START)){
                return new String[]{"clear record start","0"};
            }else if(compareBytes(bytes, COMMAND_RECEIVE_CLEAR_SUCCESS)){
                return new String[]{"clear record success","1"};
            }
        }else if(bytes[0] ==0x02 && bytes[1] == 0x08){
            //todo delete response
            if(compareBytes(bytes,COMMAND_RECEIVE_DELETE_ACK_SUCCESS)){
                return new String[]{"Delete user ack success","0"};
            }else if(compareBytes(bytes, COMMAND_RECEIVE_DELETE_START)){
                return new String[]{"Delete user start","0"};
            }else if(compareBytes(bytes, COMMAND_RECEIVE_DELETE_SUCCESS)){
                return new String[]{"Delete user success","1"};
            }

        }else if(bytes[0] ==0x02 && bytes[1] == 0x12){
            //todo led response
            if(compareBytes(bytes, COMMAND_RECEIVE_SET_LED_FAILED)){
                return new String[]{"led set failed","1"};
            }else if(compareBytes(bytes, COMMAND_RECEIVE_SET_LED_SUCCESS)){
                return new String[]{"led set success","1"};
            }

        }else if(bytes[0] ==0x02 && bytes[1] == 0x13){
            //todo reg enable or disable
            if(bytes[3] == 0x00){
                return new String[]{"auto detect is enable","1"};
            }else {
                return new String[]{"auto detect is disable","1"};
            }
        }else if(bytes[0] == 0x02 && bytes[1] == 0x00){
            //todo unknow command
            return new String[]{"unknown command","1"};

        }else if(bytes[0] == 0x02 && bytes[1] == 0x04){
            //todo fmi is ready
            return new String[]{"Fmi is ready","1"};
        }
        return new String[]{"no this command","1"};


    }

    private static boolean compareBytes(byte[] source, byte[] target){
            if (source.length != target.length)
                return false;

            for (int i = 0;i < source.length;i++)
            {
                if (source[i] != target[i])
                    return false;
            }
            return true;
    }

  }
