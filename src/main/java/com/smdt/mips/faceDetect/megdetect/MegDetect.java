package com.smdt.mips.faceDetect.megdetect;

import java.util.Vector;

public class MegDetect {
    static {
        System.loadLibrary("megdetect");
    }

    public MegDetect() {}

    public MegDetect(String modelPath, int minSize, int num) {
        initDetect(modelPath, minSize, num);
    }

    public Vector<FaceRect> detectFacesByName(String image_file, int id) {
//      System.out.println("-----------********");
        float[][] result = detectByName(image_file, id);
//      System.out.println(result);
        Vector<FaceRect> boxes = new Vector<FaceRect>();
//      tmp[0] = detect_result->face_list[i].rect.left;
//      tmp[1] = detect_result->face_list[i].rect.top;
//      tmp[2] = detect_result->face_list[i].rect.right;
//      tmp[3] = detect_result->face_list[i].rect.bottom;
//      tmp[4] = detect_result->face_list[i].pose.roll;
//      tmp[5] = detect_result->face_list[i].pose.pitch;
//      tmp[6] = detect_result->face_list[i].pose.yaw;
//      tmp[7] = detect_result->face_list[i].blur;
//      tmp[8] = detect_result->face_list[i].brightness;
//      tmp[9] = detect_result->face_list[i].brightness_deviation;
//      tmp[10] = detect_result->face_list[i].face_completeness;
//      tmp[11] = detect_result->face_list[i].errcode;
        for(int i=0; i<result.length; ++i) {
            FaceRect box = new FaceRect();
            box.face_rect[0] = (int)result[i][0];
            box.face_rect[1] = (int)result[i][1];
            box.face_rect[2] = (int)result[i][2];
            box.face_rect[3] = (int)result[i][3];

            box.pose[0] = result[i][4];
            box.pose[1] = result[i][5];
            box.pose[2] = result[i][6];

            box.quality[0] = result[i][7];
            box.quality[1] = result[i][8];
            box.quality[2] = result[i][9];

            box.face_completeness = result[i][10];

            box.errcode = (int) result[i][11];

            boxes.add(box);
        }

        return boxes;
    }

    public Vector<FaceRect> detectFacesByJpgByte(byte[] image_data, int id) {
//      System.out.println("-----------********");
        float[][] result = detectByJpgBytes(image_data, id);

        Vector<FaceRect> boxes = new Vector<FaceRect>();
//      tmp[0] = detect_result->face_list[i].rect.left;
//      tmp[1] = detect_result->face_list[i].rect.top;
//      tmp[2] = detect_result->face_list[i].rect.right;
//      tmp[3] = detect_result->face_list[i].rect.bottom;
//      tmp[4] = detect_result->face_list[i].pose.roll;
//      tmp[5] = detect_result->face_list[i].pose.pitch;
//      tmp[6] = detect_result->face_list[i].pose.yaw;
//      tmp[7] = detect_result->face_list[i].blur;
//      tmp[8] = detect_result->face_list[i].brightness;
//      tmp[9] = detect_result->face_list[i].brightness_deviation;
//      tmp[10] = detect_result->face_list[i].face_completeness;
//      tmp[11] = detect_result->face_list[i].errcode;
        if(result != null){
            for(int i=0; i<result.length; ++i) {
                FaceRect box = new FaceRect();
                box.face_rect[0] = (int)result[i][0];
                box.face_rect[1] = (int)result[i][1];
                box.face_rect[2] = (int)result[i][2];
                box.face_rect[3] = (int)result[i][3];

                box.pose[0] = result[i][4];
                box.pose[1] = result[i][5];
                box.pose[2] = result[i][6];

                box.quality[0] = result[i][7];
                box.quality[1] = result[i][8];
                box.quality[2] = result[i][9];

                box.face_completeness = result[i][10];

                box.errcode = (int) result[i][11];

                boxes.add(box);
            }
        }

        return boxes;
    }

    public native boolean initDetect(String model_path, int min_size, int num);
    public native float[][] detectByName(String image_file, int id);
    public native float[][] detectByJpgBytes(byte[] image_data, int id);
    public native void setMinSize(int min_size, int id);
    public native void releaseDetect();
}