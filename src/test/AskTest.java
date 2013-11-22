package test;

import java.util.ArrayList;
import java.util.Map;

import configuration.ConfigurationFileLoader;
import question.QuestionProcessing;

public class AskTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String path = "D:/correspondence.txt";
		//String question="品牌;宝马,品牌;奔驰,级别;中型车,级别;中型车,车体结构;三厢车,车体结构;三厢车,外部配置;电动天窗:标配,外部配置;电动天窗:标配,进气形式;涡轮增压,进气形式;涡轮增压";
		String question="品牌;宝马,级别;中型车,进气形式;?";
		//ConfigurationFileLoader loader=new ConfigurationFileLoader(path);
		//loader.fileLoad();
		QuestionProcessing qs = new QuestionProcessing(path);
		Map<String,ArrayList<String>> result=qs.questionParse(question);
		//int qkind=qs.questionClassification(result);
		qs.questionQuery(result);
		//System.out.println(loader.getDbTables());
		//System.out.println(r);

	}

}
