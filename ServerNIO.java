package classing;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
//java nio  ͬ��������
/*
* ���߳�selectorֻ��������������
* ���߳�selector��������channel�Ķ�д�¼�
* 
* 
* selector.select
*/
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import db.DbConnector;


import org.apache.ibatis.session.SqlSession;

import classing.bean.Log;
import classing.dao.LogMapper;
import classing.sql.MySqlSessionFactory;


class WorkTask implements Runnable{
	
	private Selector selector;
	//private ByteBuffer buffer;
	private List<SocketChannel> list;

	public WorkTask() throws IOException {
		super();
		// TODO Auto-generated constructor stub
		//����selector����
		selector = Selector.open();
		//buffer = ByteBuffer.allocate(1024);
		list=Collections.synchronizedList(new ArrayList<SocketChannel>(10));
		
	}
	
	public Selector getSelector(){
		return selector;
	}
	public List<SocketChannel> getList(){
		return list;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try{
			while(!Thread.currentThread().isInterrupted()){
				
				int num = selector.select();
				if(num <= 0){
					if(!list.isEmpty()){
						Iterator<SocketChannel> it=list.iterator();
						while(it.hasNext()){
							it.next().register(selector,SelectionKey.OP_READ);
							it.remove();
						}
						System.out.println("�¿ͻ��˽���");
					}
					//???
					continue;
				}
				
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				while(it.hasNext()){
					SelectionKey key = it.next();
					//��ɾ���¼����������key
					//System.out.println("����"+key);
					it.remove();
					
					if(key.isValid() && key.isReadable()){
						SocketChannel cChannel = (SocketChannel)key.channel();
						try{
						
						// array => ByteBuffer=>byte[]
						ByteBuffer buffer = ByteBuffer.allocate(1024);//ÿ�ζ������µ�buffer
						
						int readcnt=cChannel.read(buffer);
						
						String recvMsg=new String(buffer.array()).trim();
						JsonParser parser = new JsonParser();
						JsonElement json = parser.parse(recvMsg);
						JsonObject object = json.getAsJsonObject();//����Json�ַ���
						System.out.println(object);
						
						int msgtype = object.get("msgtype").getAsInt();
						
						switch(msgtype){
						case 1:
							//�����¼��Ϣ
							login(object,cChannel);
							break;
						case 2:
							register(object,cChannel);
							//������ȥ   CharSet   java.util.nio �ӻس�
							//cChannel.write(ByteBuffer.wrap(new String("server recv your msg111!\n").getBytes()));
							break;
						case 3:
							myexit(object,cChannel);
					
							break;
						case 4:
							passMessage(object);
							break;
						case 5:
							offLine(object,cChannel);
							break;
						}
						// socket  out cchannel.write()
						//in  cchannel.read()   	
						/**
						 * �رշ������Ϳͻ��˵�ͨ����·��Դ
						 * channel.close();   ����·�رյ�
						 * key.cancel();      ��channel��selector��ɾ����
						 * 
						 * channel.close();
						 * cchannel.keyFor(selector).cancel();
						 * 
						 * 
						 * 
						 * 1.�����main thread�и����߳����channel
						 * 2.��δ���client���쳣�رպ������ر�
						 * 3.cchannel.write����Ϣ���Ͳ���ȥ��ô�죿����
						 */
						}
						catch(IOException e){
							//..........................????
								
								System.out.println("�ͻ����쳣�ر�");
								
								cChannel.close();   //����·�رյ�
								key.cancel();      //��channel��selector��ɾ����
								//continue label3;
								//���Ƴ������Ƴ�������
								//bug���쳣�رպ�����ϵͳ�Ͳ���������
							}
						}
					} 
				}
		}
		catch(IOException e){
		
		//	e.printStackTrace();
			System.out.println("�ͻ����쳣�ر�");
			//continue label3;
			//���Ƴ������Ƴ�������
			//bug���쳣�رպ�����ϵͳ�Ͳ���������
		}
		catch(Exception e){
			e.printStackTrace();/////////////////////////////////??????????
			System.out.println("Exception");
		}
		
	}
	
	public static int register(JsonObject jobject,SocketChannel cChannel){//����ע��
		String name = jobject.get("name").getAsString();
		String pwd = jobject.get("pwd").getAsString();
		
		boolean regisState = false;//?
		
		DbConnector db = DbConnector.getInstance();
		ResultSet rset = db.select("select * from log");
		try {
			while(rset.next()){
				String _name = rset.getString("usename");
				if(_name.compareTo(name) == 0){
					regisState=true;
				}
			}
			rset.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//JDBC�������ݿ�
		
	/*	Mybatis�������ݿ�
	 * SqlSession session = MySqlSessionFactory.getInstance().openSession(true);
		//mybatis�Զ�������ʵ��IUser�ӿڵ������
		LogMapper usermodel = session.getMapper(LogMapper.class);
		
		Log user=usermodel.selectByPrimaryKey(name);//���ע��ͬ���ģ�����д��󣿣���bug������
		//���ݿ���û�У��򷵻ؿ�
		System.out.println(user);
		if(user!=null){
			regisState=true;
		}*/
		
		if(regisState){
			//���ͻ��˻ظ�
			JsonObject json = new JsonObject();	
			json.addProperty("msgtype", 19);
			json.addProperty("rack", "fail");//(ע��ʧ����Ϣ)
			json.addProperty("reason", "name existed!!!");
			try {
				//out.println(json.toString());
				cChannel.write(ByteBuffer.wrap((json.toString()+"\n").getBytes()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return 0;
		}
		
			PreparedStatement prestatement=db.add("insert into log(usename,password) values(?,?)");
			try {
				prestatement.setString(1, name);
				prestatement.setString(2, pwd);
				if(prestatement.executeUpdate() > 0){
					JsonObject json = new JsonObject();
					json.addProperty("msgtype", 19);
					// 19��ʾ��������rack��Ӧ��Ϣ
					json.addProperty("rack", "ok");//ע��ɹ�
					//out.println(json.toString());
					cChannel.write(ByteBuffer.wrap((json.toString()+"\n").getBytes()));
				}
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}//JDBC�������ݿ�	
			
		/*usermodel.insert(new Log(name,pwd));
		session.close();
		JsonObject json = new JsonObject();
		json.addProperty("msgtype", 19);
		// 19��ʾ��������rack��Ӧ��Ϣ
		json.addProperty("rack", "ok");//ע��ɹ�
		//out.println(json.toString());
		try {
			cChannel.write(ByteBuffer.wrap((json.toString()+"\n").getBytes()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//Mybits�������ݿ�
		*/
		return 1;
	}

	public void login(JsonObject jobject,SocketChannel cChannel){//�����¼
	//�������ݿ⣬����name  password
	String name = jobject.get("name").getAsString();
	String pwd = jobject.get("pwd").getAsString();
	boolean bloginstate = false;
	
	DbConnector db = DbConnector.getInstance();
	ResultSet rset = db.select("select * from log");
	
	try {
		while(rset.next()){
			String _name = rset.getString("usename");
			if(_name.compareTo(name) == 0){
				if(rset.getString("password").compareTo(pwd) == 0){
					//��¼�ɹ�
					bloginstate = true;
					ServerNIO.stringSocket.put(_name,cChannel);//�������û�������Map���С�
					//???	
				}
			}
		}
		rset.close();
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	
/*	SqlSession session = MySqlSessionFactory.getInstance().openSession(true);
	//mybatis�Զ�������ʵ��IUser�ӿڵ������
	LogMapper usermodel = session.getMapper(LogMapper.class);
	
	Log user=usermodel.selectByPrimaryKey(name);//?????????????????????????
	if(user.getUsename().compareTo(name)==0 && user.getPassword().compareTo(pwd)==0){
		bloginstate = true;
		ServerNIO.stringSocket.put(name,cChannel);//�������û�������Map����
	}*/
	
	//���ͻ��˻ظ�

	JsonObject json = new JsonObject();
	json.addProperty("msgtype", 20);
	if(bloginstate){
		//1.���Ƿ���������Ϣ
		String offmsg=ServerNIO.stringMsg.get(name);//���Ȳ鿴������Ϣ
		ServerNIO.stringMsg.remove(name);
		if(offmsg!=null){
			JsonObject jsonoff=new JsonObject();
			jsonoff.addProperty("msgtype", 16);
			jsonoff.addProperty("offmsg", offmsg);
			
			//out.println(jsonoff.toString());
			try {
				cChannel.write(ByteBuffer.wrap((jsonoff.toString()+"\n").getBytes()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
		json.addProperty("ack", "ok");
		json.addProperty("name", name);
	
		try {
			//out.println(json.toString());//���ͻ��˵Ķ����̷߳���Ϣ���ɹ���
			cChannel.write(ByteBuffer.wrap((json.toString()+"\n").getBytes()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}else{
		json.addProperty("ack", "fail");
		json.addProperty("reason", "name or pwd is wrong!!!");
		
		try {
			//out.println(json.toString());//���ͻ��˵Ķ����̷߳���Ϣ��ʧ�ܣ�
			cChannel.write(ByteBuffer.wrap((json.toString()+"\n").getBytes()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	try {
		Thread.sleep(1000);
		//out.println(json.toString());//���ͻ��˵�main���̣������̣߳�����Ϣ�����ܳɹ���Ҳ����ʧ�ܣ��ڿͻ����Լ��жϡ�
		cChannel.write(ByteBuffer.wrap((json.toString()+"\n").getBytes()));
		Thread.sleep(1000);
	} catch (InterruptedException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	if(bloginstate){
		//�������û��㲥�������û���
		JsonObject json1 = new JsonObject();
		json1.addProperty("msgtype", 18);//�������ߵ��û��㲥���������ߵ��û��ϡ�
		json1.addProperty("onLineName", name);
		for(SocketChannel cli:ServerNIO.stringSocket.values()){
			try {
				//System.out.println(Server.stringSocket.keySet());
				/*PrintWriter out1=new PrintWriter(cli.getOutputStream(), true);
				out1.println(json1.toString());*/
				cli.write(ByteBuffer.wrap((json1.toString()+"\n").getBytes()));
				//out1.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	}
	
	public void myexit(JsonObject jobject,SocketChannel channel){//����ͻ����˳�
		try {
			channel.close();
			System.out.println("�ͻ��������ر�");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void passMessage(JsonObject jobject){//����ͻ�������Ϣ
		String toName=jobject.get("to").getAsString();
		String fromName=jobject.get("from").getAsString();
		String msg=jobject.get("msg").getAsString();
		
		SocketChannel toClient=ServerNIO.stringSocket.get(toName);
		
		if(toClient==null){//Ŀ�겻����
			JsonObject resultjson = new JsonObject();
			resultjson.addProperty("msgtype", 6);//Ŀ�겻����
			resultjson.addProperty("result", "not onLine!!!");
		
			ServerNIO.stringMsg.put(toName, msg);
			
			SocketChannel fromClient=ServerNIO.stringSocket.get(fromName);
			try {
			/*	PrintWriter out1 = new PrintWriter(fromClient.getOutputStream(), true);
				out1.println(resultjson.toString());*/
				
				fromClient.write(ByteBuffer.wrap((resultjson.toString()+"\n").getBytes()));
				//???
				//out1.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else{//�����ɹ�����
			try {	
				JsonObject tojson = new JsonObject();
				tojson.addProperty("msgtype", 5);
				tojson.addProperty("fromName", fromName);
				tojson.addProperty("msg", msg);
				
				/*PrintWriter out1=new PrintWriter(toClient.getOutputStream(), true);
				out1.println(tojson);*/
				
				toClient.write(ByteBuffer.wrap((tojson.toString()+"\n").getBytes()));
				//out1.close();
				//???
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
	}
	
	public void offLine(JsonObject jobject,SocketChannel cchannel){//�����û�����
		String offLineName = jobject.get("from").getAsString();
		
		ServerNIO.stringSocket.remove(offLineName);//��Ҫ���ߵ��û�����map�����Ƴ���
		JsonObject json1 = new JsonObject();
		json1.addProperty("msgtype", 17);//�������ߵ��û��㲥���������ߵ��û��ϡ�
		json1.addProperty("offLinename", offLineName);
		for(SocketChannel cli:ServerNIO.stringSocket.values()){
			try {
				/*PrintWriter out1=new PrintWriter(cli.getOutputStream(), true);
				out1.println(json1.toString());*/
				cli.write(ByteBuffer.wrap((json1.toString()+"\n").getBytes()));
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			cchannel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
public class ServerNIO {
	private Selector selector;
	private ServerSocketChannel sschannel;
	private WorkTask workTask;
	private static int count;
	
	public static Map<String,SocketChannel> stringSocket;//��ſͻ���socket
	static {
		stringSocket=new HashMap<String,SocketChannel>(10);
	}
	
	public static Map<String,String> stringMsg;//���������Ϣ
	static{
		stringMsg=new HashMap<String,String>(10);
	}
	
	public ServerNIO() throws IOException {
		super();
		// TODO Auto-generated constructor stub
		//����selector����
		selector = Selector.open();
		//����server��channel
		sschannel = ServerSocketChannel.open();
		sschannel.bind(new InetSocketAddress("127.0.0.1", 6000));
		sschannel.configureBlocking(false);//����ͨ��Ϊ������
		//ע��server channel��accept�¼�
		sschannel.register(selector, SelectionKey.OP_ACCEPT);
		System.out.println("server start on 6000...");
		//���������д�¼������߳�
		workTask = new WorkTask();
		new Thread(workTask).start();
	}
	
	public void startServer() throws IOException{
		while(!Thread.currentThread().isInterrupted()){
			int num = selector.select();
			
			if(num <= 0){
				continue;
			}
			
			//key(channel()) -> channel(socket()) -> socket
			Iterator<SelectionKey> it = selector.selectedKeys().iterator();
			while(it.hasNext()){
				SelectionKey key = it.next();
				//��ɾ���¼����������key
				it.remove();
				
				if(key.isValid() && key.isAcceptable()){
					SocketChannel cchannel = sschannel.accept();
					cchannel.configureBlocking(false);
					
					//��ӵ��µ��߳�����  
					workTask.getList().add(cchannel);//�Ƚ���SocketChannel��ӵ�list��
					System.out.println(++count);
					workTask.getSelector().wakeup();//���书�ܣ�����ִ�в���������ִ�У��������̵߳�select��
					//�Ȼ�����ע���Ǵ���ġ�
				}
			}
		}
	}

	public static void main(String[] args){
		try {
			new ServerNIO().startServer();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
