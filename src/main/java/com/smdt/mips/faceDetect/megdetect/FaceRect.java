package com.smdt.mips.faceDetect.megdetect;


import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class FaceRect {
    // left:box[0],top:box[1],right:box[2],bottom:box[3]

	//12位数
	public int[] face_rect;
	public float[] pose;
	public float[] quality;
	public float face_completeness;
	public int errcode;
	

    FaceRect() {
    	face_rect = new int[4];
    	pose = new float[3];
    	quality=new float[3];
    }

//    public int left() {
//        return face_rect[0];
//    }
//
//    public int right() {
//        return face_rect[2];
//    }
//
//    public int top() {
//        return face_rect[1];
//    }
//
//    public int bottom() {
//        return face_rect[3];
//    }
//
//    public int width() {
//        return box[2] - box[0] + 1;
//    }
//
//    public int height() {
//        return box[3] - box[1] + 1;
//    }
//
//    //面积
//    public int area() {
//        return width() * height();
//    }
//
//    //Bounding Box Regression
//    public void calibrate() {
//        int w = box[2] - box[0] + 1;
//        int h = box[3] - box[1] + 1;
//        box[0] = (int) (box[0] + w * bbr[0]);
//        box[1] = (int) (box[1] + h * bbr[1]);
//        box[2] = (int) (box[2] + w * bbr[2]);
//        box[3] = (int) (box[3] + h * bbr[3]);
//        for (int i = 0; i < 4; i++) {
//            bbr[i] = 0.0f;
//        }
//    }
//
//    //当前box转为正方形
//    public void toSquareShape() {
//        int w = width();
//        int h = height();
//        if (w > h) {
//            box[1] -= (w - h) / 2;
//            box[3] += (w - h + 1) / 2;
//        } else {
//            box[0] -= (h - w) / 2;
//            box[2] += (h - w + 1) / 2;
//        }
//    }
//
//    //防止边界溢出，并维持square大小
//    public void limit_square(int w, int h) {
//        if (box[0] < 0 || box[1] < 0) {
//            int len = max(-box[0], -box[1]);
//            box[0] += len;
//            box[1] += len;
//        }
//        if (box[2] >= w || box[3] >= h) {
//            int len = max(box[2] - w + 1, box[3] - h + 1);
//            box[2] -= len;
//            box[3] -= len;
//        }
//    }
//
//    public void limit_square2(int w, int h) {
//        if (width() > w) {
//            box[2] -= width() - w;
//        }
//        if (height() > h) {
//            box[3] -= height() - h;
//        }
//        if (box[0] < 0) {
//            int sz = -box[0];
//            box[0] += sz;
//            box[2] += sz;
//        }
//        if (box[1] < 0) {
//            int sz = -box[1];
//            box[1] += sz;
//            box[3] += sz;
//        }
//        if (box[2] >= w) {
//            int sz = box[2] - w + 1;
//            box[2] -= sz;
//            box[0] -= sz;
//        }
//        if (box[3] >= h) {
//            int sz = box[3] - h + 1;
//            box[3] -= sz;
//            box[1] -= sz;
//        }
//    }
//
//    public void calAngles() {
//        // left eye
//        int x1 = (int)landmark[0];
//        int y1 = (int)landmark[5];
//
//        // right eye
//        int x2 = (int)landmark[1];
//        int y2 = (int)landmark[6];
//
//        // nose
//        int x3 = (int)landmark[2];
//        int y3 = (int)landmark[7];
//
//        // left side of mouth
//        int x4 = (int)landmark[3];
//        int y4 = (int)landmark[8];
//
//        // right side of mouth
//        int x5 = (int)landmark[4];
//        int y5 = (int)landmark[9];
//
//        // distant of eyes
//        int x_dist = x1 - x2;
//        int y_dist = y1 - y2;
//
//        // cal cosine value and angle degrees
//        double circle_angle_cosine = (double)abs(x_dist) / Math.sqrt(x_dist*x_dist + y_dist*y_dist);
//        angles[0] = (float)Math.toDegrees(Math.acos(min(circle_angle_cosine, 0.99999)));
//
//        // mid point of 2 eyes
//        int x_mid_1 = (x1 + x2) / 2;
//        int y_mid_1 = (y1 + y2) / 2;
//
//        // mid point of mouth
//        int x_mid_2 = (x4 + x5) / 2;
//        int y_mid_2 = (y4 + y5) / 2;
//
//        // 两眼中点, 嘴中点, 鼻点构成的三角形三边长的平方
//        double a_sqr = Math.pow(x3-x_mid_2, 2) + Math.pow(y3-y_mid_2, 2);
//        double b_sqr = Math.pow(x_mid_2-x_mid_1, 2) + Math.pow(y_mid_2-y_mid_1, 2);
//        double c_sqr = Math.pow(x_mid_1-x3, 2) + Math.pow(y_mid_1-y3, 2);
//
//        // 余弦定理
//        double left_right_cosine = (double)(b_sqr+c_sqr-a_sqr) / (2*Math.sqrt(b_sqr)*Math.sqrt(c_sqr));
//        angles[1] = (float)Math.toDegrees(Math.acos(min(Math.pow(left_right_cosine,1.7), 0.99999)));
//
//        // 两个中点分别到鼻的y轴距离
//        int y1_dist = y_mid_1 - y3;
//        int y2_dist = y_mid_2 - y3;
//
//        // 两距离之差比之和作为正弦值
//        double up_down_sine = (double)(abs(abs(y1_dist) - abs(y2_dist))) / (double)(abs(y1_dist) + abs(y2_dist));
//        angles[2] = (float)Math.toDegrees(Math.asin(min(up_down_sine, 0.99999)));
//    }
}
