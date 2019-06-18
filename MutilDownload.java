package com.liliangcai.download;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import com.liliangcai.download.MutilDownload.DownloadThread;

public class MutilDownload {

	// 获取文件路径
	static String path = "http://down.sandai.net/thunderx/XunLeiWebSetup10.1.15.448.exe";
	private static final int threadCount = 3;
	private static int runningThread;

	public static void main(String[] args) {

		// 获取要下载的文件的大小，计算每个线程开始和结束位置
		try {
			URL url = new URL(path);
			// url开启连接，获取HttpUrlConnection连接对象
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			// 设置参数 发送GET请求
			conn.setRequestMethod("GET"); // 默认请求就是GET(大写)
			// 设置连接网络超时时间
			conn.setConnectTimeout(5000);
			// 获取服务器返回的状态吗
			int code = conn.getResponseCode();// 200代表获取服务器资源全部成功 206请求部分资源
			if (code == 200) {

				// 获取服务器文件大小
				int length = conn.getContentLength();
				// 把线程的数量赋值给正在运行的线程数
				runningThread = threadCount;
				// 创建一个大小和服务器文件一样的文件空间，
				RandomAccessFile rafAccessFile = new RandomAccessFile(getFileName(path), "rw");
				rafAccessFile.setLength(length);

				// 每个线程下载的大小
				int blockSize = length / threadCount;

				// 计算每个线程下载的开始和结束位置
				for (int i = 0; i < threadCount; i++) {
					int startIndex = i * blockSize; // 每个线程下载的开始位置
					int endIndex = (i + 1) * blockSize - 1; // 每个线程下载的结束位置
					if (i == threadCount - 1) {
						endIndex = length - 1;
					}

					// 开启线程下载服务器文件
					DownloadThread downloadThread = new DownloadThread(startIndex, endIndex, i);
					downloadThread.start();

				}
			}

		} catch (Exception e) {
			System.out.println("Exception!!!!!!!!!!");
		}
	}

	public static class DownloadThread extends Thread {
		private int startIndex;
		private int endIndex;
		private int threadID;

		// 通过构造方法把每个线程下载的开始位置和结束位置传递进来
		public DownloadThread(int startIndex, int endIndex, int threadID) {
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.threadID = threadID;

		}

		@Override
		public void run() {
			// 下载
			try {
				// 创建一个url对象，参数就是网址
				URL url = new URL(path);
				// 获取HttpURLConnection连接对象
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				// 设置参数发送get请求
				conn.setRequestMethod("GET");// 默认请求就是get(大写)
				// 设置连接超时时间
				conn.setConnectTimeout(5000);
				// 设置一个请求头(作用就是告诉服务器，每个线程下载的始末)
				conn.setRequestProperty("Range", "bytes=" + startIndex + "-" + endIndex);// 固定写法

				// 如果之前下载断过，继续上次的位置下载，
				File file = new File(getFileName(path) + threadID + ".txt");
				if (file.exists() && file.length() > 0) {
					FileInputStream fis = new FileInputStream(file);
					BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fis));
					String lastPosition = bufferedReader.readLine();
					int lastPositionn = Integer.parseInt(lastPosition);
					startIndex = lastPositionn;
					fis.close();
				}

				// 获取服务器返回的状态码
				int code = conn.getResponseCode();
				if (code == 206) {// 200 代表获取服务器资源全部成功， 206请求部分资源成功
					// 创建随机读写文件对象
					RandomAccessFile raf = new RandomAccessFile(getFileName(path), "rw");
					// 每个线程要从自己的位置开始写
					raf.seek(startIndex);

					InputStream in = conn.getInputStream(); // 存的是服务器那个文件

					// 把数据写到文件中
					int len = -1;
					byte[] buffer = new byte[1024 * 1024 * 2]; // 创建缓冲区
					int total = 0;
					while ((len = in.read(buffer)) != -1) {
						raf.write(buffer, 0, len);
						total += len;

						int currentThreadPosition = startIndex + total;

						RandomAccessFile raff = new RandomAccessFile(getFileName(path) + threadID + ".txt", "rwd");
						raff.write(String.valueOf(currentThreadPosition).getBytes());
						raff.close();

					}
					raf.close(); // 关闭流，释放资源
					System.out.println("线程" + threadID + "-----------下载完成");
					//线程同步
					synchronized (DownloadThread.class) {
						runningThread--;
						for (int i = 0; i < threadCount; i++) {
							File deleteFile = new File(getFileName(path) + i + ".txt");
							deleteFile.delete();

						}

					}

				}

			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("IOException");
			}

		}
	}

	// 获取文件的名字
	public static String getFileName(String path) {
		int start = path.lastIndexOf("/") + 1;
		return path.substring(start);
	}
}
