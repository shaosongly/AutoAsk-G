package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import question.QuestionProcessing;

public class AskTest {

	public static void main(String[] args) {
		//配置文件路径
		String path = "D:/correspondence.txt";
		
		//传入的问题
		//String question="A|品牌:宝马,品牌:奔驰,级别:中型车,车体结构:三厢车,进气形式:涡轮增压";
		//String question="品牌:奔驰,级别:中型车,车体结构:三厢车,价格:?";
		//String question="品牌:宝马,级别:中型车,级别:SUV,车体结构:三厢车,车体结构:SUV,进气形式:涡轮增压";
		String question="品牌:宝马,品牌:奔驰,级别:中型车,价格:?";
		//String question="品牌:奔驰,级别:(中型车;SUV),车体结构:(三厢车;SUV),价格:>=10";
		//String question="品牌:宝马,颜色:(红;蓝),价格:>=10";
		//初始化问题处理模块
		QuestionProcessing qs = new QuestionProcessing(path);
		
		//将问题解析成键值对存储到Map集合中
		Map<String,ArrayList<String>> params=new HashMap<String,ArrayList<String>>();
		Map<String,ArrayList<String>> querys=new HashMap<String,ArrayList<String>>();
		Map<String,ArrayList<String>> results=new HashMap<String,ArrayList<String>>();
		qs.questionParse(question,params,querys);
		//System.out.println(result);
		//根据解析结果，转换SQL查询数据库
		results=qs.questionQuery(params,querys);
		System.out.println(results);

	}

}
