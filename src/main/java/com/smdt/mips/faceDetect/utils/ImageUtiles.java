package com.smdt.mips.faceDetect.utils;

//import com.smdt.mips.constv.Const;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Properties;

public class ImageUtiles {


    /**
     * 根据图片路径，把图片转为byte数组
     * @param imgPath  图片路径
     * @return      byte[]
     */
    public static byte[] image2Bytes(String imgPath) {
        FileInputStream fin;
        byte[] bytes = null;
        try {
            fin = new FileInputStream(new File(imgPath));
            bytes = new byte[fin.available()];
            //将文件内容写入字节数组
            fin.read(bytes);
            fin.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bytes;
    }

//    public static byte[] image2Bytes(String imgPath) {
//        byte[] data = null;
//
//        FileImageInputStream input = null;
//        try {
//            input = new FileImageInputStream(new File(imgPath));  //报错的地方
//            ByteArrayOutputStream output = new ByteArrayOutputStream();
//            byte[] buf = new byte[1024];
//            int numBytesRead = 0;
//            while ((numBytesRead = input.read(buf)) != -1) {
//                output.write(buf, 0, numBytesRead);
//            }
//            data = output.toByteArray();
//            output.close();
//            input.close();
//        }
//        catch (FileNotFoundException ex1) {
//            ex1.printStackTrace();
//        }
//        catch (IOException ex1) {
//            ex1.printStackTrace();
//        }
//        return data;
//    }

    /**
     * 得到文件流
     * @param url
     * @return  jpg的byte[]
     */
    public static byte[] getByteByImgUrl(String url) throws MalformedURLException, Exception{
        Properties properties = Config.getProperties();
        try {
            URL httpUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection)httpUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(Integer.parseInt(properties.getProperty("requestUrlTimeOut", "500")));//连接主机的超时时间，超时500毫秒，可能有6次重连
            conn.setReadTimeout(Integer.parseInt(properties.getProperty("requestUrlTimeOut", "500")));//从主机读取数据的超时时间，500毫秒
//            System.setProperty("sun.net.client.defaultConnectTimeout", "5000");
//            System.setProperty("sun.net.client.defaultReadTimeout", "5000");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            //防止403错误
            conn.setRequestProperty("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65 Safari/537.31");
            String strMes = conn.getResponseMessage();
            if(strMes.compareTo("Not Found") == 0){
                conn.disconnect();
                return null;
            }
            InputStream inStream = conn.getInputStream();//通过输入流获取图片数据
            byte[] btImg = readInputStream(inStream);//得到图片的二进制数据
            inStream.close();
            conn.disconnect();
            return btImg;
        } catch (SocketTimeoutException e) {
            return null;
        }catch(FileNotFoundException ex1){
            return null;
        }catch (ConnectException etime){
            return null;
        }catch (SocketException esocket){
            return null;
        }

    }

    /**
     * 得到文件流
     * @param imageUrl
     * @return  jpg的BufferedImage
     */
    public static BufferedImage urlToBufferedImage(String imageUrl) throws MalformedURLException, IOException {
        Properties properties = Config.getProperties();
        try{
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET"); //连接方式GET
            connection.setConnectTimeout(Integer.parseInt(properties.getProperty("requestUrlTimeOut", "500")));//连接主机的超时时间，超时500毫秒，可能有6次重连
            connection.setReadTimeout(Integer.parseInt(properties.getProperty("requestUrlTimeOut", "500"))); //从主机读取数据的超时时间，500毫秒
//            System.setProperty("sun.net.client.defaultConnectTimeout", "5000");
//            System.setProperty("sun.net.client.defaultReadTimeout", "5000");
            connection.setDoOutput(true);
            connection.setDoInput(true);

            //防止403错误
            connection.setRequestProperty("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65 Safari/537.31");
            String strMes = connection.getResponseMessage();
            if(strMes.compareTo("Not Found") == 0){
                connection.disconnect();
                return null;
            }
            InputStream inputStream=connection.getInputStream();
            BufferedImage image = ImageIO.read(inputStream);
            inputStream.close();
            connection.disconnect();
            return image;
        }catch(SocketTimeoutException e){
            return null;
        }catch(IllegalArgumentException ex){  //当图片url为灰度图片时报异常
            return null;
        }catch(FileNotFoundException ex1){
            return null;
        }catch (ConnectException etime){
            return null;
        }catch (SocketException esocket){
            return null;
        }
    }


    /**
     * 根据图像数据缓冲区，把图片转为byte数组
     * @param img  图像数据缓冲区
     * @return      rgb的byte[]
     */
    public static byte[] image2ByteRgb(BufferedImage img) {
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
            }
        }

        return rgb;
    }

    /**
     * 从输入流中获取数据
     * @param inStream 输入流
     * @return
     * @throws Exception
     */
    public static byte[] readInputStream(InputStream inStream) throws Exception{
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while( (len=inStream.read(buffer)) != -1 ){
            outStream.write(buffer, 0, len);
        }
        inStream.close();
        return outStream.toByteArray();
    }
}
