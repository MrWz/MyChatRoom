package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


//����ģʽ
public class DbConnector {
	private Connection connection;
	private PreparedStatement prestatement;
	private ResultSet resultset;
	
	private DbConnector(){
		try {
			//����jdbc������
			Class.forName("com.mysql.jdbc.Driver");
			String url = "jdbc:mysql://localhost:3306/chatroom";
			//������mysql server��һ������
			connection = DriverManager.getConnection(url, "root", "111111");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static class InnerDbConnector{
		public static final DbConnector db = new DbConnector();
	}
	
	public static DbConnector getInstance(){
		return InnerDbConnector.db;
	}
	
	public ResultSet select(String sql){
		try {
			Statement statement = connection.createStatement();
			return statement.executeQuery(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public PreparedStatement add(String sql){
		try {
			prestatement = connection.prepareStatement(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return prestatement;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
