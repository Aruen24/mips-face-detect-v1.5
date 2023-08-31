package com.smdt.mips.faceDetect.mtcnn;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;


import java.util.List;
import java.util.Vector;
import java.io.PrintStream;

public class Test_single {
	
    public static byte[] image2ByteArr(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        byte[] rgb = new byte[w*h*3];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int val = img.getRGB(j, i);
                int red = (val >> 16) & 0xFF;
                int green = (val >> 8) & 0xFF;
                int blue = val & 0xFF;

                rgb[(i*w+j)*3] = (byte) red;
                rgb[(i*w+j)*3+1] = (byte) green;
                rgb[(i*w+j)*3+2] = (byte) blue;
                //System.out.println(String.valueOf((i*h+j)*3));
            }
        }
        return rgb;
    }

    public static int[][][] image2FloatArr(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[][][] floatValues = new int[w][h][3];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int val = img.getRGB(j, i);
                floatValues[j][i][0] = (val >> 16) & 0xFF;
                floatValues[j][i][1] = (val >> 8) & 0xFF;
                floatValues[j][i][2] = val & 0xFF;
                //System.out.println(Arrays.toString(floatValues[j][i]));
            }
        }
        return floatValues;
    }


    public static void main(String args[]) throws InterruptedException, IOException {
      	// 模型路径
    	String model_path = args[0]; 
//        String model_path = "/home/wangyuanwen/mtcnn_jni/model";
        
        List<String> files = new ArrayList<String>();
//        String picture_path = "/home/wangyuanwen/test_data/lfw/";
//        String picture_path = "/home/wangyuanwen/mtcnn_jni/data";
        //处理图片路径
        String picture_path = args[1]; 
        File file = new File(picture_path);
        if(file!=null){// 判断对象是否为空
        	if(!file.isFile()){
        		File[] tempList = file.listFiles() ;// 列出全部的文件
        		for(int i=0;i<tempList.length;i++){
        			if(tempList[i].isDirectory()){// 如果是目录
        				File[] p_path = tempList[i].listFiles() ;// 列出全部的文件
        				for(int j=0;j<p_path.length;j++){
        					files.add(p_path[j].toString());
        				}
        			}else if(tempList[i].isFile()){
        				files.add(tempList[i].toString());// 如果不是目录，输出路径
        			}
        		}
        	}else{
        		files.add(file.toString());// 如果不是目录，输出路径
        	}
        }
        
        long startTime = System.currentTimeMillis();
        
        // 输出结果路径
//        String output_path = "/home/wangyuanwen/mtcnn_jni/result.txt";
    	String result_path = args[2];
    	
    	Mtcnn mtcnn_multi = new Mtcnn(model_path, 40, 1);
    	
    	BufferedImage img_buff=null;
        Vector<Box> boxes=null;

        //输出结果到指定文件路径
        PrintStream ps = new PrintStream(result_path); 
        System.setOut(ps);//把创建的打印输出流赋给系统。即系统下次向 ps输出
        for(String img_name : files) {
            img_buff = ImageIO.read(new File(img_name));
            
            boxes = mtcnn_multi.detectFaces(img_buff,1);
       	 //如果待检测图片有人脸且一张图片有多个Bbox框，取得分最高的那个框
            if(!(boxes.isEmpty()) && boxes.size() > 0){
	           	 int k = 0;
	           	 for (int i = 0; i < boxes.size(); i++) {
	           		 float max = boxes.get(0).score;
	           		 if(boxes.get(i).score > max) {
	           			 max=boxes.get(i).score;
	           			 k = i;
	           		 }
	           	 }
	           	 System.out.println(img_name+","+Arrays.toString(boxes.get(k).angles));
	//           	 System.out.println(img_name+","+Arrays.toString(boxes.get(k).angles)+","+max);
            }
       	 
        }
    	
        
    	mtcnn_multi.releaseDetect(); //所有线程完成后释放c++对象
    	long endTime = System.currentTimeMillis();
    	long t1 = endTime - startTime;
    	System.out.println("耗时:"+t1+"毫秒");
    }
}
