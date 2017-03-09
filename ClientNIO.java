package classing;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


class GetMsgFromServer{
	public static int aa;
	public GetMsgFromServer(){
		
	}
	public synchronized void getLock(){
		try {
			wait();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public synchronized void unLock(){
			notify();
	}
}
class ReaderTask implements Runnable{
	private Socket client;
	private Scanner scanner;
	private PrintWriter out;

	GetMsgFromServer syn;
	private Lock lock;
	
	public ReaderTask(Socket client,GetMsgFromServer _syn,Lock _lock) {
		super();
		this.client = client;
		this.syn=_syn;
		this.lock=_lock;
		try {
			scanner = new Scanner(client.getInputStream());
			out = new PrintWriter(client.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {//客户端读取服务器发来消息线程的执行函数
		// TODO Auto-generated method stub
		while(!Thread.currentThread().isInterrupted()){
			//System.out.println("run...");
			lock.lock();
			String recvMsg = scanner.nextLine();
			//System.out.println("run1...");
			//System.out.println(recvMsg);
			JsonParser parser = new JsonParser();
			JsonElement json = parser.parse(recvMsg);
			JsonObject object = json.getAsJsonObject();
			
			int msgtype = object.get("msgtype").getAsInt();
			switch(msgtype){
			case 20: //接收服务器ack响应消息的
				//处理登录消息
				String ack = object.get("ack").getAsString();
				if(ack.compareTo("fail") == 0){
					System.out.println("login fail!");
					System.out.println(object.get("reason").getAsString());
				}else{
					System.out.println("login success!");
					//alreadyOnly(out,object.get("name").getAsString());
				}
				break;
			case 19:
				String rack=object.get("rack").getAsString();
				if(rack.compareTo("fail") == 0){
					System.out.println("register fail!");
					System.out.println(object.get("reason").getAsString());
				}else{
					System.out.println("register success!");//？？？显示不出来
				}
				break;
			case 18:
				String onLineName=object.get("onLineName").getAsString();
				System.out.println(onLineName+" Go online......");
				break;
			case 17:
				String offLinename=object.get("offLinename").getAsString();
				System.out.println(offLinename+" Go offline......");
				break;
			case 16:
				String offmsg=object.get("offmsg").getAsString();
				System.out.println("您有离线消息"+offmsg);
				break;
			case 6:
				String result=object.get("result").getAsString();
				System.out.println(result);
				break;
			case 5:
				String fromName=object.get("fromName").getAsString();
				String msg=object.get("msg").getAsString();
				
				System.out.println(fromName+" speak:"+msg);
				break;
			}
			
			//syn.getLock();
			lock.unlock();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

public class ClientNIO {
	public static GetMsgFromServer syn=new GetMsgFromServer();
	private static Lock lock = new ReentrantLock();// 锁对象
	
	public static void sendMessage(PrintWriter out,String username){
		Scanner scan=new Scanner(System.in);
		String name=null;
		System.out.println("输入消息接收者的名字：");
		name=scan.nextLine();
		
		String message=null;
		System.out.println("输入消息内容：");
		message=scan.nextLine();
		
		JsonObject json = new JsonObject();
		json.addProperty("msgtype", 4);//发送消息
		json.addProperty("from", username);
		json.addProperty("to", name);
		json.addProperty("msg", message);
		
		out.println(json.toString());
	}
	
	public static void offLine(PrintWriter out,String username){
		JsonObject json = new JsonObject();
		json.addProperty("msgtype", 5);//用户下线
		json.addProperty("from", username);
	
		out.println(json.toString());
	}
	public static void main(String[] args) {//写操作线程
		// TODO Auto-generated method stub
		try {
			ExecutorService threadpool = Executors.newSingleThreadExecutor();
			Socket client = new Socket("127.0.0.1", 6000);
			threadpool.submit(new ReaderTask(client,syn,lock));
			PrintWriter out =null;
			boolean endSingle=true;
			String username = null;
			
			while(!Thread.currentThread().isInterrupted()){
				System.out.println("                                      *********************");
				System.out.println("                                      *      1.login      *");
				System.out.println("                                      *      2.register   *");
				System.out.println("                                      *      3.exit       *");
				System.out.println("                                      *********************");
				System.out.print("请输入您的选择：");
				Scanner s=new Scanner(System.in);
				
				
				int t=s.nextInt();
				if(t==1){
					Scanner scan = new Scanner(System.in);
					System.out.println("请输入你的用户名：");
					String name=scan.nextLine();
					System.out.println("请输入你的密码：");
					int pwd=scan.nextInt();
							
					JsonObject json = new JsonObject();
					json.addProperty("msgtype", 1);
					json.addProperty("name", name);
					json.addProperty("pwd", pwd);
					
					out = new PrintWriter(client.getOutputStream(), true);
					out.println(json.toString());
					
					try {
						Thread.sleep(100);//让子线程先获得锁。
						lock.lock();
						Scanner scanner=new Scanner(client.getInputStream());
						String recvMsg = scanner.nextLine();
						
						JsonParser parser = new JsonParser();
						JsonElement json1 = parser.parse(recvMsg);
						JsonObject object = json1.getAsJsonObject();
						//System.out.println("++++++"+object.toString());
						
						int msgtype = object.get("msgtype").getAsInt();
						lock.unlock();
						if(msgtype==20){
							String ack = object.get("ack").getAsString();
							if(ack.compareTo("ok")==0){
								endSingle=false;
								username=object.get("name").getAsString();
								break;
							}
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if(t==2){
					Scanner scan = new Scanner(System.in);
					System.out.println("输入新用户名：");
					String name=scan.nextLine();
					System.out.println("输入密码：");
					int pwd=scan.nextInt();
					
					JsonObject json = new JsonObject();
					json.addProperty("msgtype", 2);
					json.addProperty("name", name);
					json.addProperty("pwd", pwd);
					
					out = new PrintWriter(client.getOutputStream(), true);
					out.println(json.toString());
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if(t==3){
					JsonObject json = new JsonObject();
					json.addProperty("msgtype", 3);
					
					out = new PrintWriter(client.getOutputStream(), true);
					out.println(json.toString());
					System.out.println("bye.....");
		
					client.close();
					break;
				}
			}
			if(!endSingle){
				while(!Thread.currentThread().isInterrupted()){
					System.out.println("                                      *********************");
					System.out.println("                                      *      1.Chat       *");;
					System.out.println("                                      *      2.Offline    *");
					System.out.println("                                      *********************");
					System.out.print("请输入您的选择：");
					Scanner s=new Scanner(System.in);
					int n=s.nextInt();
			
					if(n==1){
						System.out.println("**************您已经进入发送消息页面**********");
						sendMessage(out,username);
					}
					else{
						System.out.println("**************您即将下线**********");
						offLine(out,username);
						//out.close();
						client.close();
						break;
					}
				}
			}
		/*	out.close();
			client.close();*/
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}



