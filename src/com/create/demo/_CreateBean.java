package com.create.demo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.create.demo.beans.MySqlStructBean;
import com.create.demo.beans.MySqlTableBean;
import com.create.demo.beans.MySqlViewBean;
import com.create.demo.db.DaoUtil;

public class _CreateBean {
	// query mysql table
	static String sqlTable = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?";
	// query mysql view
	static String sqlView = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.VIEWS WHERE TABLE_SCHEMA = ?;";
	// query mysql table or view colum info
	static String sqlStruct = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_KEY FROM INFORMATION_SCHEMA.COLUMNS WHERE table_name = ?;";

	// output file path
	static String beanOutputDir = "/JavaCreateBeanForMySql/src/com/create/demo/beansoutput";

	public static void main(String[] args) {
		Properties info = DaoUtil.getProperties();//get configuration.properties which on src root path
		// String jdbcUrl = info.getProperty("jdbc.url");
		// String dbUserName = info.getProperty("jdbc.user");
		// String dbPassword = info.getProperty("jdbc.password");
		String dbName = info.getProperty("jdbc.dbname");

		queryTable(dbName);
		queryView(dbName);
	}

	/**
	 * query MySql table with database name
	 * @param dbName
	 * @return
	 */
	public static List<MySqlTableBean> queryTable(String dbName) {
		ResultSet resultSet = DaoUtil.getObject(sqlTable, new Object[] { dbName });
		List<MySqlTableBean> resultObject = new ArrayList<>();
		try {
			while (resultSet.next()) {
				MySqlTableBean resultObjectItem = new MySqlTableBean();
				String fieldObject = resultSet.getString(1);
				if (fieldObject == null) {
					fieldObject = "null";//prevent null pointer
				}
				resultObjectItem.setTableName(fieldObject);
				resultObject.add(resultObjectItem);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DaoUtil.release();
		if (null != resultObject && resultObject.size() > 0) {
			for (int i = 0, size = resultObject.size(); i < size; i++) {
				System.out.println(resultObject.get(i).getTableName());
				String tableName = resultObject.get(i).getTableName();
				List<MySqlStructBean> columnAndType = queryStruct(tableName);

				String fileContent = buildFileContent(tableName, columnAndType);
				wirteToFile(tableName, fileContent);
			}
		}
		return resultObject;
	}

	/**
	 * query MySql view with database name
	 * @param dbName
	 * @return
	 */
	public static List<MySqlViewBean> queryView(String dbName) {
		ResultSet resultSet = DaoUtil.getObject(sqlView, new Object[] { dbName });
		List<MySqlViewBean> resultObject = new ArrayList<>();
		try {
			while (resultSet.next()) {
				MySqlViewBean resultObjectItem = new MySqlViewBean();
				String fieldObject = resultSet.getString(1);
				if (fieldObject == null) {
					fieldObject = "null";//prevent null pointer
				}
				resultObjectItem.setTableName(fieldObject);
				resultObject.add(resultObjectItem);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DaoUtil.release();
		if (null != resultObject && resultObject.size() > 0) {
			for (int i = 0, size = resultObject.size(); i < size; i++) {
				System.out.println(resultObject.get(i).getTableName());
				queryStruct(resultObject.get(i).getTableName());
			}
		}
		return resultObject;
	}

	/**
	 * query table structure with table name
	 * @param tableName
	 * @return
	 */
	public static List<MySqlStructBean> queryStruct(String tableName) {
		ResultSet resultSet = DaoUtil.getObject(sqlStruct, new Object[] { tableName });
		List<MySqlStructBean> resultObject = new ArrayList<>();
		try {
			while (resultSet.next()) {
				MySqlStructBean resultObjectItem = new MySqlStructBean();
				String fieldColumnName = resultSet.getString(1);
				String fieldDataType = resultSet.getString(2);
				String fieldIsPK = resultSet.getString(3);
				if (fieldColumnName == null) {
					fieldColumnName = "null";//prevent null pointer
				}
				if (fieldDataType == null) {
					fieldDataType = "null";//prevent null pointer
				}
				if (fieldIsPK == null) {
					fieldIsPK = "null";//prevent null pointer
				}
				// COLUMN_NAME, DATA_TYPE, COLUMN_KEY
				resultObjectItem.setColumnName(fieldColumnName);
				resultObjectItem.setDataType(fieldDataType);
				resultObjectItem.setColumnKey(fieldIsPK);
				resultObject.add(resultObjectItem);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DaoUtil.release();
		if (null != resultObject && resultObject.size() > 0) {
			for (int i = 0, size = resultObject.size(); i < size; i++) {
				System.out.println(resultObject.get(i).getColumnName() + " # " + resultObject.get(i).getDataType()
						+ " # " + resultObject.get(i).getColumnKey());
			}
		}
		return resultObject;
	}

	//bean file style
	private final static String classTemplate = "public class %s{";
	private final static String fieldTemplate = "\tprivate %s %s;%n";
	private final static String setterTemplate = "\tpublic void set%s(%s %s) {%n" + "\t\tthis.%s = %s;%n" + "\t}%n%n";
	private final static String getterTemplate = "\tpublic %s get%s() {%n" + "\t\treturn %s;%n" + "\t}%n%n";
	//bean file style
	
	/**
	 * create bean file
	 * 
	 * @param tableName
	 * @param columnAndType
	 * @return
	 */
	private static String buildFileContent(String tableName, List<MySqlStructBean> columnList) {

		StringBuilder content = new StringBuilder("");
		String className = tableName.toLowerCase();
		content.append(String.format(classTemplate, className));
		content.append("\n");
		for (MySqlStructBean entry : columnList) {
			content.append(
					String.format(fieldTemplate, typeFilter(entry.getDataType()), entry.getColumnName().toLowerCase()));
		}
		content.append("\n");
		for (MySqlStructBean entry : columnList) {
			String colName = entry.getColumnName().toLowerCase();
			String colType = typeFilter(entry.getDataType());
			String mName = firstCharToUpper(colName);
			content.append(String.format(getterTemplate, colType, mName, colName));
			content.append(String.format(setterTemplate, mName, colType, colName, colName, colName));
		}
		content.append("\n");
		content.append("}");
		return content.toString();
	}

	/**
	 * replace column type
	 * 
	 * @param type
	 * @return
	 */
	private static String typeFilter(String type) {
		if (type == null) {
			return "";
		} else if (type.equals("date")) {
			return "java.util.Date";
		} else if (type.equals("datetime")) {
			return "java.util.Date";
		} else if (type.equals("timestamp")) {
			return "java.util.Date";
		} else if (type.equals("[B")) {
			return "byte[]";
		} else if (type.startsWith("varchar")) {
			return "java.lang.String";
		} else if (type.equals("text")) {
			return "java.lang.String";
		}
		return type;
	}

	/**
	 * write to file
	 * 
	 * @param tableName
	 * @param content
	 */
	private static void wirteToFile(String tableName, String content) {

		String dirPath = getProjectRootPath() + beanOutputDir;
		File dir = new File(dirPath);
		if (!dir.exists())
			dir.mkdirs();

		File targetFile = new File(dirPath + File.separator + tableName + ".java");
		if (targetFile.exists()) {
			targetFile.delete();
		}
		System.out.println(targetFile.getAbsolutePath());

		FileWriter fw = null;
		try {
			if (!targetFile.exists())
				targetFile.createNewFile();
			fw = new FileWriter(targetFile);
			fw.write(content);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fw.flush();
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * get project root path
	 * 
	 * @return
	 */
	private static String getProjectRootPath() {
		String WebRootPath = "";
		try {
			String path = _CreateBean.class.getResource("/").toURI().getPath();
			WebRootPath = new File(path).getParentFile().getParentFile().getCanonicalPath();

		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return WebRootPath;
	}

	/**
	 * replace "_"
	 * upper character before "_" & upper class first character
	 * 
	 * @param str
	 * @return
	 */
	private static String firstCharToUpper(String str) {
		String[] strArry = str.split("_");
		StringBuilder sb = new StringBuilder();
		if (strArry.length > 0) {
			for (int i = 0, size = strArry.length; i < size; i++) {
				char[] buffer = strArry[i].toCharArray();
				buffer[0] = Character.toUpperCase(strArry[i].charAt(0));
				sb.append(new String(buffer));
			}
			return sb.toString();
		} else {
			char[] buffer = str.toCharArray();
			buffer[0] = Character.toUpperCase(str.charAt(0));
			return new String(buffer);
		}
	}
}
