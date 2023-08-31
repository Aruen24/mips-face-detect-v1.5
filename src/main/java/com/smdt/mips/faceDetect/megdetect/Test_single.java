package com.smdt.mips.faceDetect.megdetect;


import com.smdt.mips.faceDetect.utils.ImageUtiles;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Test_single {

	public static void main(String args[]) throws IOException, InterruptedException {
    	// 模型路径
    	String model_path = args[0];
//    	String model_path = "/home/wangyuanwen/det_jni/models/M_det_x86_v1.2.bin";
//		String model_path = "./model/M_det_x86_v1.2.bin";

    	// 人脸区域最小像素值 40 60 90
    	MegDetect megdetect = new MegDetect(model_path, 40, 1);

    	List<String> files = new ArrayList<String>();
//      String picture_path = "/home/wangyuanwen/mtcnn_jni/data";
      //处理图片路径
      String picture_path = args[1];
//      String picture_path = "/home/wangyuanwen/det_jni/data/dst_face_jpg_folder";

   // 输出结果路径
//    String output_path = "/home/wangyuanwen/mtcnn_jni/result.txt";
	String result_path = args[2];

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

    //输出结果到指定文件路径
      PrintStream ps = new PrintStream(result_path);
      System.setOut(ps);//把创建的打印输出流赋给系统。即系统下次向 ps输出
       for(String img_name : files) {
//	                System.out.println("*****"+img_name+"----"+megdetect.detect(img_name));
//	                img_buff = ImageIO.read(new File(img_name));
           byte[] data = ImageUtiles.image2Bytes(img_name);
            //判断被检测图片是否有人脸
            if(megdetect.detectByJpgBytes(data, 0) != null){
            	float[][] boxes = megdetect.detectByJpgBytes(data, 0);

				  //如果一张图片有多个Bbox框，取质量最好（quality ok(根据errcode去确定，为0就是ok,非0就是bad) and blur biggest）的那个框
				    if(boxes.length > 0){
		            	 int k = 0;
		            	 for (int i=0;i<boxes.length;i++){
					    	//rect[84 137 277 331], roll=0.000000 pitch=-10.977843, yaw=-0.271695, blur=0.120760, face brightness=173, brightness_deviation=33, completeness=1.000000, errcode=0
					    	//[84.0, 137.0, 277.0, 331.0, 0.0, -10.977843, -0.27169517, 0.12076038, 173.0, 33.0, 1.0, 0.0]
					    	String result = Arrays.toString (boxes[i]);
					    	String[] arrays_list = result.split(",");
					    	float max = Float.parseFloat(Arrays.toString (boxes[0]).split(",")[7]);
					    	if(Float.parseFloat(arrays_list[7]) > max && Float.parseFloat(arrays_list[11].replace("]", "").trim()) == 0.0) {
		            			 max=Float.parseFloat(arrays_list[7]);
		            			 k = i;
		            		 }
					    }
		            	String[] arrays_list_result = Arrays.toString (boxes[k]).split(",");
					    //输出图片路径,bbox框坐标
					    System.out.println(img_name+","+"["+arrays_list_result[0].replace("[", "")+arrays_list_result[1]+arrays_list_result[2]+arrays_list_result[3]+"]");
					    //输出图片路径,角度
//						    System.out.println(img_name+","+"["+arrays_list_result[4].trim()+arrays_list_result[5]+arrays_list_result[6]+"]");

				    }
            }
       }

    	megdetect.releaseDetect(); //所有线程完成后释放c++对象
    	long endTime = System.currentTimeMillis();
    	long t1 = endTime - startTime;
    	System.out.println("耗时:"+t1+"毫秒");
        //mtcnn.initDetect("/home/chenyong/android_project/tf_face_filter/mtcnn_jni/model", 30, 1);

//        String fnm = "/home/wangyuanwen/mtcnn_jni/test.png";



    }
}
