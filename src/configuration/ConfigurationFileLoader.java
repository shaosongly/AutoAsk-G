package configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationFileLoader {
	private Map<String, String> dbNames = null;
	private Map<String, String> dbTables = null;
	private Map<String, String> dataType = null;
	private Map<String, String> matchType = null;
	private String filePath = null;

	public ConfigurationFileLoader(String filePath) {
		super();
		this.filePath = filePath;
		dbNames = new HashMap<String, String>();
		dbTables = new HashMap<String, String>();
		dataType = new HashMap<String, String>();
		matchType = new HashMap<String, String>();
	}

	public ConfigurationFileLoader() {
		super();
		dbNames = new HashMap<String, String>();
		dbTables = new HashMap<String, String>();
		dataType = new HashMap<String, String>();
		matchType = new HashMap<String, String>();
	}

	public Map<String, String> getDataType() {
		return dataType;
	}

	public void setDataType(Map<String, String> dataTYPE) {
		this.dataType = dataTYPE;
	}

	public Map<String, String> getMatchType() {
		return matchType;
	}

	public void setMatchType(Map<String, String> matchType) {
		this.matchType = matchType;
	}

	public Map<String, String> getDbNames() {
		return dbNames;
	}

	public void setDbNames(Map<String, String> dbNames) {
		this.dbNames = dbNames;
	}

	public Map<String, String> getDbTables() {
		return dbTables;
	}

	public void setDbTables(Map<String, String> dbTables) {
		this.dbTables = dbTables;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public void fileLoad() {
		File file = new File(filePath);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			if (reader != null) {
				String tempString = null;
				while ((tempString = reader.readLine()) != null) {
					String[] temp=tempString.split("	");
					dbNames.put(temp[0], temp[1]);
					dbTables.put(temp[0], temp[2]);
					dataType.put(temp[0], temp[3]);
					matchType.put(temp[0], temp[4]);
				}
				reader.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
