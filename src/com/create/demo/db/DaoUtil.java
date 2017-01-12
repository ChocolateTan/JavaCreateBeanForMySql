package com.create.demo.db;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.create.demo.beans.annotation.DatabaseField;
import com.create.demo.beans.annotation.DatabaseTable;

/**
 * 需要在src的路径下配置configuration.properties eg:
 * jdbc.url=jdbc\:mysql\://localhost\:3306/DBDemoJsp jdbc.user=root
 * jdbc.password=111111 jdbc.driver=com.mysql.jdbc.Driver
 * 
 * @author don
 *
 */
public class DaoUtil<T> {
	/**
	 * 声明数据库连接对象
	 */
	private static Connection sConnection;
	/**
	 * 声明预处理对象
	 */
	private static PreparedStatement sPreparedStatement;
	/**
	 * 声明sql语句返回结果集对象
	 */
	private static ResultSet sResultSet;

	/**
	 * 加载默认的configuration.properties资源文件
	 * 
	 * @return
	 */
	public static Properties getProperties() {
		// return getProperties("/src/configuration.properties");
		return getProperties(null);
	}

	/**
	 * 加载资源文件
	 * 
	 * @param propertyName
	 *            传入要加载的资源文件名称;
	 * @return properties 返回一个属性配置对象
	 */
	public static Properties getProperties(String propertyName) {
		/**
		 * 设置配置资源文件的默认文件名
		 */
		if (propertyName == null) {
			propertyName = "configuration.properties";
		}
		/**
		 * 声明属性文件类,读取配置使用
		 */
		Properties properties = new Properties();
		try {
			/**
			 * currentThread()是Thread的一个静态方法，返回的是当前的进程对象
			 */
			properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(propertyName));
		} catch (IOException e) {
			System.out.println(propertyName + "文件加载出现错误!");
			e.printStackTrace();
		}
		return properties;
	}

	/**
	 * 获取默认的数据库连接对象
	 * 
	 * @return
	 */
	public static Connection getConnection() {
		return getConnection(getProperties());
	}

	/**
	 * 获取数据库连接对象
	 * 
	 * @param properties
	 *            传入已经配置好的属性配置对象;
	 * @return <b>connection</b> 数据库连接对象
	 */
	public static Connection getConnection(Properties properties) {
		if (sConnection == null) {
			/**
			 * 加载数据库驱动文件
			 */
			try {
				Class.forName(properties.getProperty("jdbc.driver"));
				/**
				 * 创建数据库连接对象
				 */
				try {
					Properties info = new Properties(); // 定义Properties对象
					info.setProperty("user", properties.getProperty("jdbc.user")); // 设置Properties对象属性
					info.setProperty("password", properties.getProperty("jdbc.password"));
					info.setProperty("useSSL", properties.getProperty("jdbc.useSSL"));
					// connection =
					// DriverManager.getConnection(properties.getProperty("jdbc.url"),
					// properties.getProperty("jdbc.user"),
					// properties.getProperty("jdbc.password"));
					sConnection = DriverManager.getConnection(properties.getProperty("jdbc.url"), info);
					// connection =
					// DriverManager.getConnection(properties.getProperty("jdbc.url"),
					// properties);
					System.out.println("数据库连接成功>>>>>>>>>>>");
				} catch (SQLException e) {
					System.out.println("数据库连接参数错误!");
					e.printStackTrace();
				}
			} catch (ClassNotFoundException e) {
				System.out.println("缺少数据库驱动文件:" + properties.getProperty("jdbc.driver") + "!");
				e.printStackTrace();
			}
		}
		return sConnection;
	}

	/**
	 * 释放资源的方法;<br>
	 * 
	 * @param releaseSet
	 * @param preparedStatement
	 * @param connection
	 */
	public static void release(ResultSet releaseSet, PreparedStatement preparedStatement, Connection connection) {
		if (releaseSet != null) {
			try {
				releaseSet.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (preparedStatement != null) {
			try {
				preparedStatement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	// /////////////////////////////////CRUD基础业务/////////////////////////////
	/**
	 * 采用默认的连接,并且数据库表名与实体类名一致[不区分大小写]
	 * 
	 * @param entity
	 * @throws SQLException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public int save(T entity)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, SQLException {
		return save(entity, getConnection());
	}

	/**
	 * 采用数据库表名与实体类名一致[不区分大小写]外部传入数据库连接;
	 * 
	 * @param entity
	 * @param connection
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws SQLException
	 */
	public int save(T entity, Connection connection)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, SQLException {
		return save(entity, connection, null);
	}

	/**
	 * 采用默认的连接,并且数据库表名与实体类名不相同[不区分大小写]
	 * 
	 * @param entity
	 * @param tableName
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws SQLException
	 */
	public void save(T entity, String tableName)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, SQLException {
		save(entity, getConnection(), tableName);
	}

	/**
	 * 将实体存入数据库
	 * 
	 * @param entity
	 *            要操作的数据对象
	 * @param connection
	 *            传数据库连接
	 * @param tableName
	 *            要操作的表的名称,如果传入null,则对传入的对象名称一致的表进行操作
	 * @throws SQLException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public int save(T entity, Connection connection, String tableName)
			throws SQLException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		/**
		 * 获取操作实体的类型
		 */
		Class<? extends Object> clazz = entity.getClass();
		/**
		 * 获取传入实体的所有公开的方法;
		 */
		// Method[] methods = clazz.getDeclaredMethods();
		/**
		 * 获取传入实体中的所有公开的的属性
		 */
		Field[] fields = clazz.getDeclaredFields();
		/**
		 * 如果没有输入指定的数据表名就采用类名进行操作
		 */
		if (tableName == null) {
			DatabaseTable databaseTableClazz = clazz.getAnnotation(DatabaseTable.class);
			if (null != databaseTableClazz && !"".equals(databaseTableClazz.tableName())) {
				tableName = databaseTableClazz.tableName();
			} else {
				tableName = clazz.getSimpleName().toLowerCase();
			}
		}
		/**
		 * 拼接类中的属性字段,即数据表中的字段名
		 */
		String fieldsName = "";
		/**
		 * 占位符的设置
		 */
		String placeholder = "";
		List<Field> keyValueList = new ArrayList<>();
		for (int i = 0; i < fields.length; i++) {
			fields[i].setAccessible(true);
			DatabaseField databaseField = fields[i].getAnnotation(DatabaseField.class);
			if (null != databaseField) {
				/**
				 * 如果为id就不写入sql
				 */
				if (databaseField.id()) {
					continue;
				}
				/**
				 * 没有赋值不写入sql
				 */
				if (null == fields[i].get(entity)) {
					continue;
				}
				if (!"".equals(databaseField.columnName())) {
					fieldsName = fieldsName + databaseField.columnName() + ",";
					placeholder = placeholder + "?" + ",";
					keyValueList.add(fields[i]);
				}
			}
		}
		/**
		 * 去除多余的标点
		 */
		fieldsName = fieldsName.substring(0, fieldsName.length() - 1);
		placeholder = placeholder.substring(0, placeholder.length() - 1);
		/**
		 * 拼接sql语句
		 */
		String sql = "insert into " + tableName + "(" + fieldsName + ")" + " values " + "(" + placeholder + ")";
		System.out.println(sql);

		/**
		 * 预编译sql语句
		 */
		PreparedStatement pst = connection.prepareStatement(sql);
		/**
		 * 给预编译语句赋值
		 */
		int index = 1;
		for (int j = 0; j < keyValueList.size(); j++) {
			/**
			 * 接收指定的方法执行后的数据
			 */
			Object propertyObj = keyValueList.get(j).get(entity);
			/**
			 * 为指定的占位符进行赋值
			 */
			pst.setObject(index++, propertyObj);

		}
		/**
		 * 执行已经加载的sql语句
		 */
		return pst.executeUpdate();
	}

	/**
	 * 使用默认的数据库连接进行删除,传入的对象
	 * 
	 * @param entity
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws SQLException
	 */
	public int deleteById(Class<T> clazz, String id)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, SQLException {
		return deleteById(clazz, getConnection(), null, id);
	}

	/**
	 * 使用传入的数据库连接,删除指定的对象.
	 * 
	 * @param entity
	 * @param connection
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws SQLException
	 */
	public int deleteById(Class<T> clazz, Connection connection, String id)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, SQLException {

		return deleteById(clazz, connection, null, id);
	}

	public int delete(Class<T> clazz, String tableName, String id)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, SQLException {

		return deleteById(clazz, getConnection(), null, id);
	}

	/**
	 * 
	 * @param entity
	 *            传入操作的对象实体
	 * @param connection
	 *            传入数据库连接对象
	 * @param id
	 *            要删除数据的id
	 * @param tableName
	 *            要操作的表的名称,如果传入null,则对传入的对象名称一致的表进行操作
	 * @throws SQLException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * 
	 */
	public int deleteById(Class<T> clazz, Connection connection, String tableName, String id)
			throws SQLException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		// Class<? extends Object> clazz = entity.getClass();
		// Method[] methods = clazz.getDeclaredMethods();
		Field[] fields = clazz.getDeclaredFields();
		if (tableName == null) {
			DatabaseTable databaseTableClazz = clazz.getAnnotation(DatabaseTable.class);
			if (null != databaseTableClazz && !"".equals(databaseTableClazz.tableName())) {
				tableName = databaseTableClazz.tableName();
			} else {
				tableName = clazz.getSimpleName().toLowerCase();
			}
		}

		String idName = null;
		// Object id = null;

		for (int i = 0; i < fields.length; i++) {
			fields[i].setAccessible(true);
			DatabaseField databaseField = fields[i].getAnnotation(DatabaseField.class);
			if (null != databaseField) {
				/**
				 * 如果为id就写入sql
				 */
				if (databaseField.id()) {
					idName = databaseField.columnName();
					// id = fields[i].get(clazz);
				}
			}
		}

		String sql = "delete from " + tableName + " where " + idName + "=?";
		System.out.println(sql);
		PreparedStatement pst = connection.prepareStatement(sql);

		pst.setObject(1, id);
		return pst.executeUpdate();
	}

	/**
	 * 使用默认的数据库连接修改传入的对象.
	 * 
	 * @param entity
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws SQLException
	 */
	public void update(T entity)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, SQLException {
		update(entity, getConnection(), null);
	}

	/**
	 * 使用传入的数据库连接进行数据库修改;
	 * 
	 * @param entity
	 * @param connection
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws SQLException
	 */
	public void update(T entity, Connection connection)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, SQLException {
		update(entity, connection, null);
	}

	public void update(T entity, String tableName)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, SQLException {
		update(entity, getConnection(), tableName);
	}

	/**
	 * 
	 * @param entity
	 *            传入操作的对象实体
	 * @param connection
	 *            传入数据库连接对象
	 * @param tableName
	 *            要操作的表的名称,如果传入null,则对传入的对象名称一致的表进行操作
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 */
	public int update(T entity, Connection connection, String tableName)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, SQLException {
		Class<? extends Object> clazz = entity.getClass();
		// Method[] methods = clazz.getDeclaredMethods();
		Field[] fields = clazz.getDeclaredFields();
		if (tableName == null) {
			DatabaseTable databaseTableClazz = clazz.getAnnotation(DatabaseTable.class);
			if (null != databaseTableClazz && !"".equals(databaseTableClazz.tableName())) {
				tableName = databaseTableClazz.tableName();
			} else {
				tableName = clazz.getSimpleName().toLowerCase();
			}
		}
		String fieldsName = "";
		String idName = null;
		// 创建id字段的默认数据
		Object id = null;
		List<Field> keyValueList = new ArrayList<>();
		// 循环遍历以获取的公开的属性
		for (int i = 0; i < fields.length; i++) {
			fields[i].setAccessible(true);
			DatabaseField databaseField = fields[i].getAnnotation(DatabaseField.class);
			if (null != databaseField) {
				/**
				 * 如果为id就不写入sql
				 */
				if (databaseField.id()) {
					idName = databaseField.columnName();
					id = fields[i].get(entity);
					continue;
				}
				/**
				 * 没有赋值不写入sql
				 */
				if (null == fields[i].get(entity)) {
					continue;
				}
				if (!"".equals(databaseField.columnName())) {
					fieldsName = fieldsName + databaseField.columnName() + "=?,";
					keyValueList.add(fields[i]);
				}
			}
		}
		fieldsName = fieldsName.substring(0, fieldsName.length() - 1);
		String sql = "update " + tableName + " set " + fieldsName + " where " + idName + "=?";
		System.out.println(sql);
		PreparedStatement pst = connection.prepareStatement(sql);
		int index = 1;
		for (int j = 0; j < keyValueList.size(); j++) {
			Object propertyObj = keyValueList.get(j).get(entity);
			// 为指定的占位符进行赋值
			pst.setObject(index++, propertyObj);
		}
		pst.setObject(index++, id);
		return pst.executeUpdate();
	}

	/**
	 * 
	 * 根据id查询数据
	 * 
	 * @param entity
	 *            查询的实体对象
	 * @param connection
	 *            数据库连接对象
	 * @param id
	 *            查询的id
	 * @param tableName
	 *            操作的数据库表名
	 * @return 返回一个查询结果对象
	 * @throws SQLException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public T queryById(Class<T> clazz, Connection connection, String id, String tableName) throws SQLException,
			InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		// Class<? extends Object> clazz = entity.getClass();
		// Method[] methods = clazz.getDeclaredMethods();
		Field[] fields = clazz.getDeclaredFields();
		// 声明查询的结果对象
		T resultObject = null;
		if (tableName == null) {
			DatabaseTable databaseTableClazz = clazz.getAnnotation(DatabaseTable.class);
			if (null != databaseTableClazz && !"".equals(databaseTableClazz.tableName())) {
				tableName = databaseTableClazz.tableName();
			} else {
				tableName = clazz.getSimpleName().toLowerCase();
			}
		}

		String idName = null;
		String selectColum = "";
		// 循环遍历以获取的公开的属性
		for (int i = 0; i < fields.length; i++) {
			fields[i].setAccessible(true);
			DatabaseField databaseField = fields[i].getAnnotation(DatabaseField.class);
			if (null != databaseField) {
				/**
				 * 如果为id就不写入sql
				 */
				/**
				 * 如果为id就不写入sql
				 */
				if (databaseField.id()) {
					idName = databaseField.columnName();
					// continue;
				}
				if (null != databaseField.columnName() && !"".equals(databaseField.columnName())) {
					selectColum = selectColum + databaseField.columnName() + ",";
				}
			}
		}

		selectColum = selectColum.substring(0, selectColum.length() - 1);

		String sql = "select " + selectColum + " from " + tableName + " where " + idName + "=?";
		System.out.println(sql);
		PreparedStatement pst = connection.prepareStatement(sql);
		pst.setObject(1, id);
		ResultSet resultSet = pst.executeQuery();
		if (resultSet.next()) {
			resultObject = (T) clazz.newInstance();
			for (int i = 0; i < fields.length; i++) {
				fields[i].setAccessible(true);
				DatabaseField databaseField = fields[i].getAnnotation(DatabaseField.class);

				if (null != databaseField) {
					if (null != databaseField.columnName() && !"".equals(databaseField.columnName())) {
						String fieldName = databaseField.columnName();

						Object fieldObject = resultSet.getObject(i + 1);
						if (fieldObject == null) {
							fieldObject = "null";// 防止数据为null时引发空指针异常
						}
						fields[i].set(resultObject, resultSet.getObject(fieldName));
						// for (int j = 0; j < methods.length; j++) {
						// if
						// ((fieldName).equalsIgnoreCase(methods[j].getName()))
						// {
						// methods[j].invoke(resultObject,
						// resultSet.getObject(fieldName));
						// }
						// }
					}
				}
			}
		}
		return resultObject;
	}

	public List<T> queryAll(Class<T> clazz, Connection connection, String tableName) throws SQLException,
			InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		// Class<? extends Object> clazz = entity.getClass();
		// Method[] methods = clazz.getDeclaredMethods();
		Field[] fields = clazz.getDeclaredFields();
		// 声明查询的结果对象
		List<T> resultObject = new ArrayList<>();
		if (tableName == null) {
			DatabaseTable databaseTableClazz = clazz.getAnnotation(DatabaseTable.class);
			if (null != databaseTableClazz && !"".equals(databaseTableClazz.tableName())) {
				tableName = databaseTableClazz.tableName();
			} else {
				tableName = clazz.getSimpleName().toLowerCase();
			}
		}

		String selectColum = "";
		// 循环遍历以获取的公开的属性
		for (int i = 0; i < fields.length; i++) {
			fields[i].setAccessible(true);
			DatabaseField databaseField = fields[i].getAnnotation(DatabaseField.class);
			if (null != databaseField) {
				if (null != databaseField.columnName() && !"".equals(databaseField.columnName())) {
					selectColum = selectColum + databaseField.columnName() + ",";
				}
			}
		}

		selectColum = selectColum.substring(0, selectColum.length() - 1);

		String sql = "select " + selectColum + " from " + tableName;
		System.out.println(sql);
		PreparedStatement pst = connection.prepareStatement(sql);
//		pst.setObject(1, id);
		ResultSet resultSet = pst.executeQuery();
		while (resultSet.next()) {
			T resultObjectItem = (T) clazz.newInstance();
			for (int i = 0; i < fields.length; i++) {
				fields[i].setAccessible(true);
				DatabaseField databaseField = fields[i].getAnnotation(DatabaseField.class);

				if (null != databaseField) {
					if (null != databaseField.columnName() && !"".equals(databaseField.columnName())) {
						String fieldName = databaseField.columnName();

						Object fieldObject = resultSet.getObject(i + 1);
						if (fieldObject == null) {
							fieldObject = "null";// 防止数据为null时引发空指针异常
						}
						fields[i].set(resultObjectItem, resultSet.getObject(fieldName));
						resultObject.add(resultObjectItem);
						// for (int j = 0; j < methods.length; j++) {
						// if
						// ((fieldName).equalsIgnoreCase(methods[j].getName()))
						// {
						// methods[j].invoke(resultObject,
						// resultSet.getObject(fieldName));
						// }
						// }
					}
				}
			}
		}
		return resultObject;
	}
	
	/**
	 * 一个需要用户手动输入sql和参数语句的:增/删/改/的操作
	 * 
	 * @param sql
	 * @param args
	 * @return
	 */
	public static int upDate(String sql, Object[] args) {
		try {
			sPreparedStatement = getConnection().prepareStatement(sql);
			for (int i = 1; i <= args.length; i++) {
				sPreparedStatement.setObject(i, args[i - 1]);
			}
			return sPreparedStatement.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * 传入自定义的sql语句和参数进行查询;
	 * 
	 * @param sql
	 *            sql语句
	 * @param args
	 *            传入的参数条件
	 * @return 返回一个set集合
	 */
	public static ResultSet getObject(String sql, Object[] args) {
		System.out.println(sql);
		try {
			sPreparedStatement = getConnection().prepareStatement(sql);
			if (args != null) {
				for (int i = 1; i <= args.length; i++) {
					sPreparedStatement.setObject(i, args[i - 1]);
				}
			}
			sResultSet = sPreparedStatement.executeQuery();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sResultSet;
	}
	
	/**
	 * 释放资源的方法;<br>
	 * 
	 * @param releaseSet
	 * @param preparedStatement
	 * @param connection
	 */
	public static void release() {
		if (sResultSet != null) {
			try {
				sResultSet.close();
				sResultSet = null;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (sPreparedStatement != null) {
			try {
				sPreparedStatement.close();
				sPreparedStatement = null;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (sConnection != null) {
			try {
				sConnection.close();
				sConnection = null;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
