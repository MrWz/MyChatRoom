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
//java nio  同步非阻塞
/*
* 主线程selector只处理新连接请求
* 子线程selector处理所有channel的读写事件
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
		//创建selector对象
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
						System.out.println("新客户端接入");
					}
					//???
					continue;
				}
				
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				while(it.hasNext()){
					SelectionKey key = it.next();
					//先删除事件集合里面的key
					//System.out.println("动作"+key);
					it.remove();
					
					if(key.isValid() && key.isReadable()){
						SocketChannel cChannel = (SocketChannel)key.channel();
						try{
						
						// array => ByteBuffer=>byte[]
						ByteBuffer buffer = ByteBuffer.allocate(1024);//每次都定义新的buffer
						
						int readcnt=cChannel.read(buffer);
						
						String recvMsg=new String(buffer.array()).trim();
						JsonParser parser = new JsonParser();
						JsonElement json = parser.parse(recvMsg);
						JsonObject object = json.getAsJsonObject();//解析Json字符串
						System.out.println(object);
						
						int msgtype = object.get("msgtype").getAsInt();
						
						switch(msgtype){
						case 1:
							//处理登录消息
							login(object,cChannel);
							break;
						case 2:
							register(object,cChannel);
							//发不过去   CharSet   java.util.nio 加回车
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
						 * 关闭服务器和客户端的通信链路资源
						 * channel.close();   把链路关闭掉
						 * key.cancel();      把channel从selector中删除掉
						 * 
						 * channel.close();
						 * cchannel.keyFor(selector).cancel();
						 * 
						 * 
						 * 
						 * 1.如何在main thread中给子线程添加channel
						 * 2.如何处理client的异常关闭和正常关闭
						 * 3.cchannel.write的消息发送不出去怎么办？？？
						 */
						}
						catch(IOException e){
							//..........................????
								
								System.out.println("客户端异常关闭");
								
								cChannel.close();   //把链路关闭掉
								key.cancel();      //把channel从selector中删除掉
								//continue label3;
								//从移除表中移除？？？
								//bug有异常关闭后，整个系统就不能再用了
							}
						}
					} 
				}
		}
		catch(IOException e){
		
		//	e.printStackTrace();
			System.out.println("客户端异常关闭");
			//continue label3;
			//从移除表中移除？？？
			//bug有异常关闭后，整个系统就不能再用了
		}
		catch(Exception e){
			e.printStackTrace();/////////////////////////////////??????????
			System.out.println("Exception");
		}
		
	}
	
	public static int register(JsonObject jobject,SocketChannel cChannel){//处理注册
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
		}//JDBC操纵数据库
		
	/*	Mybatis操纵数据库
	 * SqlSession session = MySqlSessionFactory.getInstance().openSession(true);
		//mybatis自动生成了实现IUser接口的类对象
		LogMapper usermodel = session.getMapper(LogMapper.class);
		
		Log user=usermodel.selectByPrimaryKey(name);//如果注册同名的，则会有错误？？？bug？？？
		//数据库中没有，则返回空
		System.out.println(user);
		if(user!=null){
			regisState=true;
		}*/
		
		if(regisState){
			//给客户端回复
			JsonObject json = new JsonObject();	
			json.addProperty("msgtype", 19);
			json.addProperty("rack", "fail");//(注册失败消息)
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
					// 19表示服务器的rack响应消息
					json.addProperty("rack", "ok");//注册成功
					//out.println(json.toString());
					cChannel.write(ByteBuffer.wrap((json.toString()+"\n").getBytes()));
				}
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}//JDBC操纵数据库	
			
		/*usermodel.insert(new Log(name,pwd));
		session.close();
		JsonObject json = new JsonObject();
		json.addProperty("msgtype", 19);
		// 19表示服务器的rack响应消息
		json.addProperty("rack", "ok");//注册成功
		//out.println(json.toString());
		try {
			cChannel.write(ByteBuffer.wrap((json.toString()+"\n").getBytes()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//Mybits操纵数据库
		*/
		return 1;
	}

	public void login(JsonObject jobject,SocketChannel cChannel){//处理登录
	//访问数据库，鉴定name  password
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
					//登录成功
					bloginstate = true;
					ServerNIO.stringSocket.put(_name,cChannel);//将在线用户保存在Map表中。
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
	//mybatis自动生成了实现IUser接口的类对象
	LogMapper usermodel = session.getMapper(LogMapper.class);
	
	Log user=usermodel.selectByPrimaryKey(name);//?????????????????????????
	if(user.getUsename().compareTo(name)==0 && user.getPassword().compareTo(pwd)==0){
		bloginstate = true;
		ServerNIO.stringSocket.put(name,cChannel);//将在线用户保存在Map表中
	}*/
	
	//给客户端回复

	JsonObject json = new JsonObject();
	json.addProperty("msgtype", 20);
	if(bloginstate){
		//1.看是否有离线消息
		String offmsg=ServerNIO.stringMsg.get(name);//首先查看离线消息
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
			//out.println(json.toString());//给客户端的读子线程发消息（成功）
			cChannel.write(ByteBuffer.wrap((json.toString()+"\n").getBytes()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}else{
		json.addProperty("ack", "fail");
		json.addProperty("reason", "name or pwd is wrong!!!");
		
		try {
			//out.println(json.toString());//给客户端的读子线程发消息（失败）
			cChannel.write(ByteBuffer.wrap((json.toString()+"\n").getBytes()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	try {
		Thread.sleep(1000);
		//out.println(json.toString());//给客户端的main进程（发送线程）发消息，可能成功，也可能失败，在客户端自己判断。
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
		//将在线用户广播到所有用户上
		JsonObject json1 = new JsonObject();
		json1.addProperty("msgtype", 18);//将刚上线的用户广播到所有在线的用户上。
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
	
	public void myexit(JsonObject jobject,SocketChannel channel){//处理客户端退出
		try {
			channel.close();
			System.out.println("客户端正常关闭");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void passMessage(JsonObject jobject){//处理客户发送消息
		String toName=jobject.get("to").getAsString();
		String fromName=jobject.get("from").getAsString();
		String msg=jobject.get("msg").getAsString();
		
		SocketChannel toClient=ServerNIO.stringSocket.get(toName);
		
		if(toClient==null){//目标不在线
			JsonObject resultjson = new JsonObject();
			resultjson.addProperty("msgtype", 6);//目标不在线
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
		else{//即将成功发送
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
	
	public void offLine(JsonObject jobject,SocketChannel cchannel){//处理用户下线
		String offLineName = jobject.get("from").getAsString();
		
		ServerNIO.stringSocket.remove(offLineName);//将要下线的用户，从map表中移除。
		JsonObject json1 = new JsonObject();
		json1.addProperty("msgtype", 17);//即将下线的用户广播到所有在线的用户上。
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
	
	public static Map<String,SocketChannel> stringSocket;//存放客户端socket
	static {
		stringSocket=new HashMap<String,SocketChannel>(10);
	}
	
	public static Map<String,String> stringMsg;//存放离线消息
	static{
		stringMsg=new HashMap<String,String>(10);
	}
	
	public ServerNIO() throws IOException {
		super();
		// TODO Auto-generated constructor stub
		//创建selector对象
		selector = Selector.open();
		//创建server的channel
		sschannel = ServerSocketChannel.open();
		sschannel.bind(new InetSocketAddress("127.0.0.1", 6000));
		sschannel.configureBlocking(false);//设置通道为非阻塞
		//注册server channel的accept事件
		sschannel.register(selector, SelectionKey.OP_ACCEPT);
		System.out.println("server start on 6000...");
		//创建处理读写事件的子线程
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
				//先删除事件集合里面的key
				it.remove();
				
				if(key.isValid() && key.isAcceptable()){
					SocketChannel cchannel = sschannel.accept();
					cchannel.configureBlocking(false);
					
					//添加到新的线程里面  
					workTask.getList().add(cchannel);//先将该SocketChannel添加到list中
					System.out.println(++count);
					workTask.getSelector().wakeup();//记忆功能，这轮执行不到，下轮执行（唤醒子线程的select）
					//先唤醒在注册是错误的。
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
