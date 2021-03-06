package ir.salazar.university;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class DBInterface {
	private Connection connect = null;
	private Statement statement = null;
	private ResultSet resultSet = null;
	private static DBInterface myObject = null;
	
	private DBInterface(String user, String pass) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			try {
				connect = DriverManager
						.getConnection("jdbc:mysql://localhost/?" + "user=" + user + "&password=" + pass);
				statement = connect.createStatement();
				try {
					statement.executeUpdate("create database salazar");
				} catch (SQLException e) {
					System.err.println("Database 'salazar' already exists.");
				}
				statement.executeUpdate("use salazar");
				try {
					statement
							.executeUpdate("create table user(id INT NOT NULL AUTO_INCREMENT, name VARCHAR(30) NOT NULL, ip VARCHAR(20) NOT NULL  , is_busy BOOLEAN DEFAULT FALSE, PRIMARY KEY (id,name))");
				} catch (SQLException e) {
					System.err.println("Table 'user' already exists.");
				}
				try {
					statement
							.executeUpdate("create table file(id INT NOT NULL AUTO_INCREMENT, name VARCHAR(60) NOT NULL, user VARCHAR(30) NOT NULL, PRIMARY KEY (id,name))");
				} catch (SQLException e) {
					System.err.println("Table 'file' already exists.");
				}
				try {
					statement
							.executeUpdate("create table userfile(id INT NOT NULL AUTO_INCREMENT, user_name VARCHAR(30) NOT NULL ,file_name VARCHAR(60) NOT NULL, is_valid BOOLEAN DEFAULT FALSE, PRIMARY KEY (id))");
				} catch (SQLException e) {
					System.err.println("Table 'userfile' already exists.");
				}

			} catch (SQLException e) {
				e.printStackTrace();
			}

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	public static DBInterface instance()
	{
		if (myObject == null)
			myObject = new DBInterface("root", "123456");
		return myObject;
	}
	
	public synchronized ArrayList<String> getUsersWithThisFileName(String fileName) {
		ArrayList<String> ret = new ArrayList<String>();
		try {
			resultSet = statement.executeQuery("SELECT user_name FROM userfile WHERE (file_name ='" + fileName + "')");
			while (resultSet.next())
				ret.add(resultSet.getString(1));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public synchronized boolean addUser(String userName, String ip) {
		try {
			resultSet = statement.executeQuery("SELECT COUNT(*) FROM user WHERE name ='" + userName + "'");
			if (resultSet.next()) {
				int count = resultSet.getInt(1);
				if (count > 0)
					return false;
				if (statement.executeUpdate("INSERT INTO user VALUES(default,'" + userName + "','"+ ip +"',default)") == 0)
					return false;
				return true;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public synchronized boolean addFile(String userName, String fileName) {
		try {
			resultSet = statement.executeQuery("SELECT COUNT(*) FROM file WHERE name ='" + fileName + "'");
			if (resultSet.next()) {
				int count = resultSet.getInt(1);
				if (count > 0)
					return false;
				resultSet = statement.executeQuery("SELECT COUNT(*) FROM user WHERE name ='" + userName + "'");
				if (resultSet.next()) {
					count = resultSet.getInt(1);
					if (count == 0)
						return false;
				}
				if ((statement.executeUpdate("INSERT INTO file VALUES(default,'" + fileName + "', '" + userName + "')") == 0)
						|| (statement.executeUpdate("INSERT INTO userfile VALUES(default,'" + userName + "', '"
								+ fileName + "',default)") == 0))
					return false;
				return true;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public synchronized boolean shareFileToUser(String userName, String fileName) {
		try {
			resultSet = statement.executeQuery("SELECT COUNT(id) FROM userfile WHERE (file_name ='" + fileName
					+ "' AND  user_name ='" + userName + "')");
			if (resultSet.next()) {
				if (resultSet.getInt(1) > 0)
					return false;
				if (statement.executeUpdate("INSERT INTO userfile VALUES(default,'" + userName + "', '" + fileName
						+ "',default)") >= 0)
					return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public synchronized boolean validateUserFile(String userName, String fileName) {
		try {
			if (statement.executeUpdate("UPDATE userfile SET is_valid=TRUE WHERE (user_name='" + userName
					+ "'AND file_name='" + fileName + "')") >= 0)
				return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public synchronized boolean setUserIsBusy(String userName, boolean val) {
		try {
			if (statement.executeUpdate("UPDATE user SET is_busy=" + (val ? "TRUE" : "FALSE")
					+ " WHERE (name='" + userName + "')") >= 0)
				return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public synchronized boolean isUserBusy(String userName)
	{
		try {
			resultSet = statement.executeQuery("SELECT is_busy FROM user WHERE (name ='" + userName + "')");
			if (resultSet.next())
				return resultSet.getBoolean(1);

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public synchronized boolean doesUserHaveThisFile(String userName, String fileName) {
		try {
			resultSet = statement.executeQuery("SELECT COUNT(*) FROM userfile WHERE (file_name ='" + fileName
					+ "' AND  user_name ='" + userName + "')");
			if (resultSet.next())
				return resultSet.getInt(1) > 0;

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public synchronized boolean isValidUserFile(String userName, String fileName) {
		try {
			resultSet = statement.executeQuery("SELECT is_valid FROM userfile WHERE (file_name ='" + fileName
					+ "' AND  user_name ='" + userName + "')");
			if (resultSet.next())
				return resultSet.getBoolean(1);

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public synchronized boolean updateFile(String fileName) {
		try {
			if (statement.executeUpdate("UPDATE userfile SET is_valid=FALSE WHERE (file_name='" + fileName + "')") >= 0)
				return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public synchronized String getOwner(String fileName) {
		try {
			resultSet = statement.executeQuery("SELECT user FROM file WHERE name ='" + fileName + "'");
			if (resultSet.next())
				return resultSet.getString(1);

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "";

	}
	
	public synchronized int getID(String userName) {
		try {
			resultSet = statement.executeQuery("SELECT id FROM user WHERE name ='" + userName + "'");
			if (resultSet.next())
				return resultSet.getInt(1);

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;

	}
	
	public synchronized String getIP(String userName) {
		try {
			resultSet = statement.executeQuery("SELECT ip FROM user WHERE name ='" + userName + "'");
			if (resultSet.next())
				return resultSet.getString(1);

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "";

	}

	public synchronized boolean isOwner(String userName, String fileName) {
		try {
			resultSet = statement.executeQuery("SELECT COUNT(*) FROM file WHERE (name ='" + fileName + "' AND user = '"
					+ userName + "')");
			if (resultSet.next()) {
				int count = resultSet.getInt(1);
				if (count > 0)
					return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void close() {
		try {
			if (resultSet != null) {
				resultSet.close();
			}

			if (statement != null) {
				statement.close();
			}

			if (connect != null) {
				connect.close();
			}
		} catch (Exception e) {

		}
	}

}
