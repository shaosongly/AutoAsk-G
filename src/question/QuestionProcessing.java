package question;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import configuration.ConfigurationFileLoader;
import dao.ConnectionUtil;

public class QuestionProcessing {
	
	private ConfigurationFileLoader loader = null;//加载配置文件
	private ConnectionUtil con = null;	//用于连接数据库
	
	
	public QuestionProcessing(String path) {
		super();
		loader=new ConfigurationFileLoader(path);//初始化配置文件加载器
		loader.fileLoad();//加载配置文件中的信息到内存
		con = new ConnectionUtil();
		con.dbConnect();//连接数据库
	}
	
	/**
	 * 解析传入的问题参数，存储到Map集合
	 * @param question 问题参数
	 * @param params   限定属性集合
	 * @param querys   待查询的属性
	 */
	public void questionParse(String question,
			Map<String, ArrayList<String>> params,
			Map<String, ArrayList<String>> querys) {
		String[] pairs = question.split(",");// 先将问题按逗号进行分割，得到key;value对
		// 对于每一对属性和值
		for (int i = 0; i < pairs.length; i++) {
			String[] kv = pairs[i].split(":");// 按照冒号分割
			String key = kv[0];// 得到属性名（中文）
			String value = kv[1];// 得到属性值
			if (isQuery(value))
				saveKV(querys, key, value);
			else
				saveKV(params, key, value);
		}
	}
	
	/**
	 * 将key、value存储到相应的map集合中
	 * @param map
	 * @param key
	 * @param value
	 */
	private void saveKV(Map<String, ArrayList<String>> map, String key,
			String value) {
		if (map.containsKey(key)) {
			map.get(key).add(value);
		} else {
			ArrayList<String> tempList = new ArrayList<String>();
			tempList.add(value);
			map.put(key, tempList);
		}
	}
	
	/**
	 * 判断是否是待查询参数
	 * @param value
	 * @return
	 */
	private boolean isQuery(String value) {
		// TODO Auto-generated method stub
		String[] flag = { "?", "min", "max", "avg", "num" };
		for (String str : flag) {
			if (value.equals(str))
				return true;
		}
		return false;
	}
	
	/**
	 * 检查传入的参数是否合法 
	 * @param params 限定属性集合
	 * @return -1表示参数不合法；1表示问题只涉及到一个对象；2表示问题涉及到两个对象
	 */
	private int questionVerify(Map<String,ArrayList<String>> params)
	{
		int flag=1;
		for (ArrayList<String> value : params.values()) {
			if(value.size()>2)
				return -1;
			else if(value.size()==2)
				flag=2;
		}
		if(flag==2)
			autoCompleValues(params);
		return flag;
	}
	
	
	/**
	 * 当问题涉及两个对象时，自动补全只有一个值的属性
	 * @param params
	 */
	private void autoCompleValues(Map<String, ArrayList<String>> params) {
		for (ArrayList<String> value : params.values()) {
			if(value.size()==1)
				value.add(value.get(0));
		}
	}
	
	/**
	 * 根据传入参数查询数据库返回结果
	 * @param params
	 * @param querys
	 * @return
	 */
	public Map<String, ArrayList<String>> questionQuery(Map<String, ArrayList<String>> params,
			Map<String, ArrayList<String>> querys) {
		int num = questionVerify(params);
		if (num == -1) {
			System.out.println("错误输入！无法回答该问题！");
			return null;
		}
		Map<String, ArrayList<String>> results=new HashMap<String, ArrayList<String>>();
		if (querys.size() == 0) {
			Map<String, ArrayList<String>> tables = getTables(params);
			if (tables == null) {
				return null;
			}
			for(int i=0;i<num;i++)
				results.put("autoId"+Integer.toString(i), doesAutoIdExist(params, i, tables));
		} else {
			for(int i=0;i<num;i++)
				for(String target:querys.keySet())
				{
					results.put(target+Integer.toString(i), singleQuery(target,params,i));
				}
		}
		return results;
	}

	private ArrayList<String> singleQuery(String target,Map<String, ArrayList<String>> params,int index) {
		ArrayList<String> results = new ArrayList<String>();
		Map<String, String> dbTables = loader.getDbTables();
		Map<String, String> dbNames = loader.getDbNames();
		String targetTable =  dbTables.get(target);
		target=dbNames.get(target);
		if(targetTable ==null || target == null) {
			System.out.println("待查询参数名错误！");
			System.exit(-1);
		}
		Map<String, ArrayList<String>> tables = getTables(params);
		if (tables.size() == 0
				|| (tables.size() == 1 && tables.keySet().contains(targetTable))) {
			ResultSet rs=getSqlAndQuery(index,params,tables,targetTable,target);
			try {
				while(rs.next())
					results.add(rs.getString(target));
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			// System.out.println(tables);
			ArrayList<String> autoIds = new ArrayList<String>();
			if (tables.keySet() != null)
				autoIds = doesAutoIdExist(params, index, tables);
			for (int i = 0; i < autoIds.size(); i++) {
				ResultSet rs = getSqlAndQuery2(targetTable, target, "auto_id",
						autoIds.get(i), "i", "e");
				try {
					if (rs.next())
						results.add(rs.getString(target));
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return results;
	}
	

	/**
	 * 获取满足所有属性约束的汽车ID
	 * @param params
	 * @param index
	 * @param tables
	 * @return
	 */
	private ArrayList<String> doesAutoIdExist(
			Map<String, ArrayList<String>> params, int index,
			Map<String, ArrayList<String>> tables) {
		ArrayList<ArrayList<String>> results = new ArrayList<ArrayList<String>>();
		for (String table : tables.keySet()) {
			ArrayList<String> autoIds = new ArrayList<String>();
			ResultSet rs = getSqlAndQuery(index, params, tables, table,
					"auto_id");
			try {
				while (rs.next()) {
					autoIds.add(rs.getString("auto_id"));
				}
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			results.add(autoIds);
		}
		return getIntersection(results);
	}

	/**
	 * 求集合的交集
	 * @param results
	 * @return
	 */
	private ArrayList<String> getIntersection(
			ArrayList<ArrayList<String>> results) {
		ArrayList<String> r=results.get(0);
		for(int i=1;i<results.size();i++)
		{
			r.retainAll(results.get(i));
		}
		return r;
	}

	/**
	 * 将属性集合按照所在的表格分类
	 * @param params
	 * @return
	 */
	private Map<String, ArrayList<String>> getTables(
			Map<String, ArrayList<String>> params) {
		Map<String, ArrayList<String>> tables = new HashMap<String, ArrayList<String>>();
		Map<String, String> dbTables = loader.getDbTables();
		for (String key : params.keySet()) {
			String tableName = dbTables.get(key);
			if(tableName == null) {
				System.out.println("参数名错误，配置文件中不存在参数"+key+"！");
				System.exit(-1);
			}
			if (!tables.containsKey(tableName)) {
				ArrayList<String> tempList = new ArrayList<String>();
				tempList.add(key);
				tables.put(tableName, tempList);
			} else
				tables.get(tableName).add(key);
		}
		return tables;
	}
	
	
	/**
	 * 获取属性在sql语句中对应的部分字符串
	 * @param value
	 * @param dataType
	 * @param matchType
	 * @return
	 */
	private String getPropValueStr(String prop,String value, String dataType,
			String matchType) {
		String result = "";
		String temp[];
		int type = getConditionalExpression(value);
		switch (type) {
		case 0:
			if (matchType.equals("l"))// like
				result += " like '%%" + value + "%%'";
			else {
				result += " = ";
				if (dataType.equals("s"))
					result += "'" + value + "'";
				else
					result += value;
			}
			break;
		case 1:
			result += " " + value;
			break;
		case 2:
			temp = value.substring(1, value.length() - 1).split(";");
			result += " between " + temp[0] + " and " + temp[1];
			break;
		case 3:
			temp = value.substring(1, value.length() - 1).split(";");
			if (matchType.equals("l"))
			{
				result += " like '%%" + temp[0] + "%%'";
				for(int i=1;i<temp.length;i++)
					result += " or "+prop+" like '%%" + temp[i] + "%%'";
			}
			else{
				result += " in (";
				if (dataType.equals("s")) {
					for (String str : temp)
						result += "'" + str + "',";
				} else {
					for (String str : temp)
						result += str + ",";
				}
				result += ")";
			}
			break;
		default:
			break;
		}

		return result;
	}
	/**
	 * 判断属性值是否包含条件
	 * @param value
	 * @return
	 */
	private int getConditionalExpression(String value) {
		// TODO Auto-generated method stub
		String[] exp={"<",">","[","("};
		String f=value.substring(0, 1);
		int flag=0;
		for(String str:exp)
		{
			if(f.equals(str))
			{
				flag=1;
				break;
			}
		}
		if(flag==1)
		{
			if(f.equals("["))
				flag=2;
			else if(f.equals("("))
				flag=3;
		}
		return flag;
	}
	/**
	 * 获取查询单张表的sql语句
	 * @param index
	 * @param params
	 * @param tables
	 * @param tableName
	 * @param target
	 * @return
	 */
	private ResultSet getSqlAndQuery(int index,Map<String, ArrayList<String>> params,
			Map<String, ArrayList<String>> tables, String tableName,
			String target)
	{
		Map<String, String> dbNames=loader.getDbNames();
		Map<String, String> dataType=loader.getDataType();
		Map<String, String> matchType=loader.getMatchType();
		ArrayList<String> props=tables.get(tableName);
		
		String sql="select "+target+" from "+tableName+" where 1=1";
		if (props != null) {
			for (String prop : props) {
				sql = sql
						+ " and "
						+ dbNames.get(prop)
						+ getPropValueStr(dbNames.get(prop),params.get(prop).get(index),
								dataType.get(prop), matchType.get(prop));
			}
		}
		System.out.println(sql);
		ResultSet rs=con.executeQuerySql(sql);
		return rs;
	}
	
	
	/**
	 * 获取查询单张表的sql语句
	 * @param tableName
	 * @param target
	 * @param extraPropName
	 * @param extraPropValue
	 * @param extraPropType
	 * @param PropMatchType
	 * @return
	 */
	private ResultSet getSqlAndQuery2(String tableName, String target,
			String extraPropName, String extraPropValue, String extraPropType,
			String PropMatchType)
	{
		String sql="select "+target+" from "+tableName+" where 1=1";
		sql=sql+" and "+extraPropName+getPropValueStr(extraPropName,extraPropValue,extraPropType,PropMatchType);
		System.out.println(sql);
		ResultSet rs=con.executeQuerySql(sql);
		return rs;
	}
}
