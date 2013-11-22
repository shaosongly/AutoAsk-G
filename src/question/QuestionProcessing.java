package question;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import configuration.ConfigurationFileLoader;
import dao.ConnectionUtil;

public class QuestionProcessing {
	
	private ConfigurationFileLoader loader = null;
	private ConnectionUtil con = null;
	private String[] configSet = { "config_air_conditioner", "config_external",
			"config_internal", "config_light", "config_manipulate",
			"config_multimedia", "config_safety", "config_seat",
			"config_tech", "config_window" };
	private String[] autoSet = { "auto_body", "auto_chassis", "auto_color",
			"auto_engine", "auto_gearbox", "auto_tire" };
	
	public QuestionProcessing(String path) {
		super();
		loader=new ConfigurationFileLoader(path);
		loader.fileLoad();
		con = new ConnectionUtil();
		con.dbConnect();
	}

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
	 * 返回问题类型 -1表示异常 1判断，2表示2个主体的判断，11表示查询，12表示比较
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
			ArrayList<String>results=singleQuery(params,0);
			System.out.println(results);
			break;
		case 12:
			compareQuery(params);
			break;
		default:
			break;
		}

	}

	private void compareQuery(Map<String, ArrayList<String>> params) {
		// TODO Auto-generated method stub
		
	}

	private ArrayList<String> singleQuery(Map<String, ArrayList<String>> params,int index) {
		// TODO Auto-generated method stub
		ArrayList<String> results = new ArrayList<String>();
		Map<String, String> dbTables = loader.getDbTables();
		Map<String, String> dbNames = loader.getDbNames();
		String target = null, targetTable = null;
		for (String key : params.keySet()) {
			if (params.get(key) == null) {
				target = key;
				params.remove(key);
				break;
			}
		}
		targetTable = dbTables.get(target);
		target=dbNames.get(target);
		Map<String, ArrayList<String>> tables = getTables(params);
		if (tables.size() == 0
				|| (tables.size() == 1 && tables.keySet().contains(targetTable))) {
			ResultSet rs=getSqlAndQuery(index,params,tables,targetTable,target,null,null,null,null);
			try {
				while(rs.next())
					results.add(rs.getString(target));
				rs.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} else {
			//System.out.println(tables);
			if(!tables.keySet().contains("auto_base"))
			{
				System.out.println("no base information!");
			}
			else
			{
				int type=getTableType(targetTable);
				ResultSet rs=getSqlAndQuery(index,params,tables,"auto_base","auto_id",null,null,null,null);
				try {
					while(rs.next())
					{
						String autoId=rs.getString("auto_id");
						boolean rOne=true;
						if(tables.keySet()!=null)
							rOne = doesAutoIdExist(params, index, tables, autoId,rOne);
						if(rOne)
						{
							switch (type) {
							case 0:
							case 1:
								ResultSet r1=getSqlAndQuery2(targetTable,target,"auto_id",autoId,"i","e");
								if(r1.next())
									results.add(r1.getString(target));
								r1.close();
								break;
							case 2:
								String configId=targetTable.substring(targetTable.indexOf("_")+1)+"_id";
								ResultSet r21=getSqlAndQuery2("auto_config",configId,"auto_id",autoId,"i","e");
								if(r21.next())
								{
									ResultSet r22=getSqlAndQuery2(targetTable,target,configId,r21.getString(configId),"i","e");
									if(r22.next())
										results.add(r22.getString(target));
									r22.close();
								}
								r21.close();
								break;
							case 3:
								ResultSet r31=getSqlAndQuery2("auto_engine","engine_type","auto_id",autoId,"i","e");
								if(r31.next())
								{
									ResultSet r32=getSqlAndQuery2(targetTable,target,"engine_type",r31.getString("engine_type"),"s","e");
									if(r32.next())
										results.add(r32.getString(target));
									r32.close();
								}
								r31.close();
								break;
							default:
								break;
							}
						}
					}
					rs.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return results;
	}

	private boolean doesAutoIdExist(Map<String, ArrayList<String>> params,
			int index, Map<String, ArrayList<String>> tables, String autoId,
			boolean rOne) throws SQLException {
		for(String table:tables.keySet())
		{
			int type=getTableType(table);
			switch (type) {
			case 1:
				ResultSet r1=getSqlAndQuery(index,params,tables,table,"1","auto_id",autoId,"i","e");
				rOne=rOne&&r1.next();
				r1.close();
				break;
			case 2:
				String configId=table.substring(table.indexOf("_")+1)+"_id";
				ResultSet r21=getSqlAndQuery(index,params,tables,"auto_config",configId,"auto_id",autoId,"i","e");
				if(r21.next())
				{
					ResultSet r22=getSqlAndQuery(index,params,tables,table,"1",configId,r21.getString(configId),"i","e");
					rOne=rOne&&r22.next();
					r22.close();
				}
				r21.close();
				break;
			case 3:
				ResultSet r31=getSqlAndQuery(index,params,tables,"auto_engine","engine_type","auto_id",autoId,"i","e");
				if(r31.next())
				{
					ResultSet r32=getSqlAndQuery(index,params,tables,table,"1","engine_type",r31.getString("engine_type"),"s","e");
					rOne=rOne&&r32.next();
					r32.close();
				}
				r31.close();
				break;
			default:
				break;
			}
		}
		return rOne;
	}

	private boolean existQuery2(Map<String, ArrayList<String>> params) {
		// TODO Auto-generated method stub
		return existQuery(params,0)&&existQuery(params,1);
	}

	private boolean existQuery(Map<String, ArrayList<String>> params,int index) {
		// TODO Auto-generated method stub
		boolean rAll=false;
		Map<String, ArrayList<String>> tables = getTables(params);
		//System.out.println(tables);
		if(!tables.keySet().contains("auto_base"))
		{
			System.out.println("no base information!");
		}
		else
		{
			ResultSet rs=getSqlAndQuery(index,params,tables,"auto_base","auto_id",null,null,null,null);
			try {
				while(rs.next())
				{
					String autoId=rs.getString("auto_id");
					boolean rOne=true;
					if(tables.keySet()==null)
						break;
					rOne = doesAutoIdExist(params, index, tables, autoId, rOne);
					rAll=rAll||rOne;
					if(rAll)
						break;
				}
				rs.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return rAll;
		
	}

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
     * 判断表格类型，如果是auto_base,返回0，如果是其它auto表，返回1，如果是config表，返回2，如果是auto_engine_base表，返回3
     * @param table
     * @return
     */
	private int getTableType(String table) {
		// TODO Auto-generated method stub
		if (table.equals("auto_base"))
			return 0;
		if (table.equals("auto_engine_base"))
			return 3;
		for (String str : autoSet) {
			if (table.equals(str))
				return 1;
		}
		for (String str : configSet) {
			if (table.equals(str))
				return 2;
		}
		return -1;
	}
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
	private ResultSet getSqlAndQuery(int index,Map<String, ArrayList<String>> params,
			Map<String, ArrayList<String>> tables, String tableName,
			String target, String extraPropName, String extraPropValue,
			String extraPropType, String PropMatchType)
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
		if(extraPropName!=null && extraPropValue!=null)
			sql=sql+" and "+extraPropName+getPropValueStr(extraPropValue,extraPropType,PropMatchType);
		System.out.println(sql);
		ResultSet rs=con.executeQuerySql(sql);
		return rs;
	}
	
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
