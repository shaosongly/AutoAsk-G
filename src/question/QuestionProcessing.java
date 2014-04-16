package question;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import configuration.ConfigurationFileLoader;
import dao.ConnectionUtil;

public class QuestionProcessing {
	
	private ConfigurationFileLoader loader = null;//���������ļ�
	private ConnectionUtil con = null;	//�����������ݿ�
	
	
	public QuestionProcessing(String path) {
		super();
		loader=new ConfigurationFileLoader(path);//��ʼ�������ļ�������
		loader.fileLoad();//���������ļ��е���Ϣ���ڴ�
		con = new ConnectionUtil();
		con.dbConnect();//�������ݿ�
	}
	
	/**
	 * �������������������洢��Map����
	 * @param question �������
	 * @param params   �޶����Լ���
	 * @param querys   ����ѯ������
	 */
	public void questionParse(String question,
			Map<String, ArrayList<String>> params,
			Map<String, ArrayList<String>> querys) {
		String[] pairs = question.split(",");// �Ƚ����ⰴ���Ž��зָ�õ�key;value��
		// ����ÿһ�����Ժ�ֵ
		for (int i = 0; i < pairs.length; i++) {
			String[] kv = pairs[i].split(":");// ����ð�ŷָ�
			String key = kv[0];// �õ������������ģ�
			String value = kv[1];// �õ�����ֵ
			if (isQuery(value))
				saveKV(querys, key, value);
			else
				saveKV(params, key, value);
		}
	}
	
	/**
	 * ��key��value�洢����Ӧ��map������
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
	 * �ж��Ƿ��Ǵ���ѯ����
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
	 * ��鴫��Ĳ����Ƿ�Ϸ� 
	 * @param params �޶����Լ���
	 * @return -1��ʾ�������Ϸ���1��ʾ����ֻ�漰��һ������2��ʾ�����漰����������
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
	 * �������漰��������ʱ���Զ���ȫֻ��һ��ֵ������
	 * @param params
	 */
	private void autoCompleValues(Map<String, ArrayList<String>> params) {
		for (ArrayList<String> value : params.values()) {
			if(value.size()==1)
				value.add(value.get(0));
		}
	}
	
	/**
	 * ���ݴ��������ѯ���ݿⷵ�ؽ��
	 * @param params
	 * @param querys
	 * @return
	 */
	public Map<String, ArrayList<String>> questionQuery(Map<String, ArrayList<String>> params,
			Map<String, ArrayList<String>> querys) {
		int num = questionVerify(params);
		if (num == -1) {
			System.out.println("�������룡�޷��ش�����⣡");
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
			System.out.println("����ѯ����������");
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
	 * ��ȡ������������Լ��������ID
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
	 * �󼯺ϵĽ���
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
	 * �����Լ��ϰ������ڵı�����
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
				System.out.println("���������������ļ��в����ڲ���"+key+"��");
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
	 * ��ȡ������sql����ж�Ӧ�Ĳ����ַ���
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
	 * �ж�����ֵ�Ƿ��������
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
	 * ��ȡ��ѯ���ű��sql���
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
	 * ��ȡ��ѯ���ű��sql���
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
