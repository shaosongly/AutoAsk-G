package question;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;



import configuration.ConfigurationFileLoader;
import dao.ConnectionUtil;

public class QuestionProcessing {
	
	private ConfigurationFileLoader loader = null;
	private ConnectionUtil con = null;	
	public QuestionProcessing(String path) {
		super();
		loader=new ConfigurationFileLoader(path);
		loader.fileLoad();
		con = new ConnectionUtil();
		con.dbConnect();
	}
    /**
     * ���������������������⣬�������Դ���Map����
     * @param question
     * @return
     */
	public Map<String,ArrayList<String>> questionParse(String question)
	{
		Map<String,ArrayList<String>> params=new HashMap<String,ArrayList<String>>();
		String[] pairs=question.split(",");
		for(int i=0;i<pairs.length;i++)
		{
			String[] kv=pairs[i].split(";");
			String key=kv[0];
			String value=kv[1];
			if(value.equals("?"))
			{
				params.put(key, null);
				continue;
			}
			if(params.containsKey(key))
			{
				params.get(key).add(value);				
			}
			else
			{
				ArrayList<String> tempList=new ArrayList<String>();
				tempList.add(value);
				params.put(key, tempList);
			}
		}
		return params;
	}
	
	/**
	 * ������������ -1��ʾ�쳣 1�жϣ�2��ʾ2��������жϣ�11��ʾ��ѯ��12��ʾ�Ƚ�
	 * @param params
	 * @return
	 */
	public int questionClassification(Map<String,ArrayList<String>> params)
	{
		int flag=1;
		int qValue=0,countS1=0,countS2=0;
		if(params.size()==0)
			return -1;
		for (ArrayList<String> value : params.values()) {
			if(value==null)
				qValue++;
			else if(value.size()>2)
				return -1;
			else if(value.size()==2)
				countS2++;
			else
				countS1++;
		}
		if(qValue>1||(countS1!=0&&countS2!=0))
			return -1;
		if(countS1!=0)
			flag=1;
		else																																																															
			flag=2;
		return qValue*10+flag;
	}
	
	/**
	 * �����ѯ�����ݲ�ͬ�������ͣ���ѯ���ݿ��ȡ��Ϣ
	 * @param params
	 */
	public void questionQuery(Map<String, ArrayList<String>> params) {
		int qType = questionClassification(params);
		switch (qType) {
		case -1:
			System.out.println("Wrong input! Can not answer this question!");
			break;
		case 1:
			boolean r1=existQuery(params,0);
			System.out.println(r1);
			break;
		case 2:
			boolean r2=existQuery2(params);
			System.out.println(r2);
			break;
		case 11:
			String target = getTargetProp(params);
			ArrayList<String>results=singleQuery(target,params,0);
			System.out.println(results);
			break;
		case 12:
			String target2 = getTargetProp(params);
			compareQuery(target2,params);
			break;
		default:
			break;
		}

	}

	/**
	 * һ��������ж�
	 * @param params
	 * @param index
	 * @return
	 */
	private boolean existQuery(Map<String, ArrayList<String>> params,int index) {
		Map<String, ArrayList<String>> tables = getTables(params);
		//System.out.println(tables);
		ArrayList<String> autoIds= doesAutoIdExist(params, index, tables);
		if(autoIds.size()>0)
			return true;
		else
			return false;		
	}
	
	/**
	 * ����������ж�
	 * @param params
	 * @return
	 */
	private boolean existQuery2(Map<String, ArrayList<String>> params) {
		return existQuery(params,0)&&existQuery(params,1);
	}

	/**
	 * ��ѯ�����������Ϣ
	 * @param target
	 * @param params
	 * @param index
	 * @return
	 */
	private ArrayList<String> singleQuery(String target,Map<String, ArrayList<String>> params,int index) {
		ArrayList<String> results = new ArrayList<String>();
		Map<String, String> dbTables = loader.getDbTables();
		Map<String, String> dbNames = loader.getDbNames();
		String targetTable =  dbTables.get(target);
		target=dbNames.get(target);
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
	 * �Ƚ��������ѯ
	 * @param target
	 * @param params
	 */
	private void compareQuery(String target,Map<String, ArrayList<String>> params) {
		ArrayList<String> r1=singleQuery(target,params,0);
		ArrayList<String> r2=singleQuery(target,params,1);
		System.out.println(r1);
		System.out.println(r2);
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
	 * ���ݲ������ϻ�ȡ�ĸ����Զ�Ӧ��ֵ��ȱʧ��
	 * @param params
	 * @return
	 */
	private String getTargetProp(Map<String, ArrayList<String>> params) {
		String target = null;
		for (String key : params.keySet()) {
			if (params.get(key) == null) {
				target = key;
				params.remove(key);
				break;
			}
		}
		return target;
	}
	
	/**
	 * ��ȡ������sql����ж�Ӧ�Ĳ����ַ���
	 * @param value
	 * @param dataType
	 * @param matchType
	 * @return
	 */
	private String getPropValueStr(String value,String dataType, String matchType)
	{
		String result="";
		if(matchType.equals("l"))
			result+=" like '%%"+value+"%%'";
		else
		{
			result+=" = ";
			if(dataType.equals("s"))
				result+="'"+value+"'";
			else
				result+=value;
		}
		return result;
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
						+ getPropValueStr(params.get(prop).get(index),
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
		sql=sql+" and "+extraPropName+getPropValueStr(extraPropValue,extraPropType,PropMatchType);
		System.out.println(sql);
		ResultSet rs=con.executeQuerySql(sql);
		return rs;
	}
}
