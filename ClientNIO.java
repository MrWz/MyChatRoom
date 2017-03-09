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
	public void run() {//�ͻ��˶�ȡ������������Ϣ�̵߳�ִ�к���
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
			case 20: //���շ�����ack��Ӧ��Ϣ��
				//�����¼��Ϣ
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
					System.out.println("register success!");//��������ʾ������
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
				System.out.println("����������Ϣ"+offmsg);
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
	private static Lock lock = new ReentrantLock();// ������
	
	public static void sendMessage(PrintWriter out,String username){
		Scanner scan=new Scanner(System.in);
		String name=null;
		System.out.println("������Ϣ�����ߵ����֣�");
		name=scan.nextLine();
		
		String message=null;
		System.out.println("������Ϣ���ݣ�");
		message=scan.nextLine();
		
		JsonObject json = new JsonObject();
		json.addProperty("msgtype", 4);//������Ϣ
		json.addProperty("from", username);
		json.addProperty("to", name);
		json.addProperty("msg", message);
		
		out.println(json.toString());
	}
	
	public static void offLine(PrintWriter out,String username){
		JsonObject json = new JsonObject();
		json.addProperty("msgtype", 5);//�û�����
		json.addProperty("from", username);
	
		out.println(json.toString());
	}
	public static void main(String[] args) {//д�����߳�
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
				System.out.print("����������ѡ��");
				Scanner s=new Scanner(System.in);
				
				
				int t=s.nextInt();
				if(t==1){
					Scanner scan = new Scanner(System.in);
					System.out.println("����������û�����");
					String name=scan.nextLine();
					System.out.println("������������룺");
					int pwd=scan.nextInt();
							
					JsonObject json = new JsonObject();
					json.addProperty("msgtype", 1);
					json.addProperty("name", name);
					json.addProperty("pwd", pwd);
					
					out = new PrintWriter(client.getOutputStream(), true);
					out.println(json.toString());
					
					try {
						Thread.sleep(100);//�����߳��Ȼ������
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
					System.out.println("�������û�����");
					String name=scan.nextLine();
					System.out.println("�������룺");
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
					System.out.print("����������ѡ��");
					Scanner s=new Scanner(System.in);
					int n=s.nextInt();
			
					if(n==1){
						System.out.println("**************���Ѿ����뷢����Ϣҳ��**********");
						sendMessage(out,username);
					}
					else{
						System.out.println("**************����������**********");
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



