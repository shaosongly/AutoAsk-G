package test;

import java.util.ArrayList;
import java.util.Map;

import configuration.ConfigurationFileLoader;
import question.QuestionProcessing;

public class AskTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String path = "D:/correspondence.txt";
		//String question="Ʒ��;����,Ʒ��;����,����;���ͳ�,����;���ͳ�,����ṹ;���ᳵ,����ṹ;���ᳵ,�ⲿ����;�綯�촰:����,�ⲿ����;�綯�촰:����,������ʽ;������ѹ,������ʽ;������ѹ";
		String question="Ʒ��;����,����;���ͳ�,������ʽ;?";
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
