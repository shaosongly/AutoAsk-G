package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import question.QuestionProcessing;

public class AskTest {

	public static void main(String[] args) {
		//�����ļ�·��
		String path = "config-files/correspondence.txt";
		
		//����ʾ��
		//String question="Ʒ��:����,Ʒ��:����,����:���ͳ�,����ṹ:���ᳵ,������ʽ:������ѹ";
		//String question="Ʒ��:����,����:���ͳ�,����ṹ:���ᳵ,�۸�:?";
		//String question="Ʒ��:����,����:���ͳ�,����:SUV,����ṹ:���ᳵ,����ṹ:SUV,������ʽ:������ѹ";
		//String question="Ʒ��:����,Ʒ��:����,����:���ͳ�,�۸�:?";
		//String question="Ʒ��:����,����:(���ͳ�;SUV),����ṹ:(���ᳵ;SUV),�۸�:>=10";
		String question="Ʒ��:����,��ɫ:(��;��),�۸�:>=10";
		
		//��ʼ�����⴦��ģ��
		QuestionProcessing qs = new QuestionProcessing(path);
		
		//����������ɼ�ֵ�Դ洢��Map������
		Map<String,ArrayList<String>> params=new HashMap<String,ArrayList<String>>();
		Map<String,ArrayList<String>> querys=new HashMap<String,ArrayList<String>>();
		Map<String,ArrayList<String>> results=new HashMap<String,ArrayList<String>>();
		
		System.out.println("���ɵ�SQL���Ϊ��");
		qs.questionParse(question,params,querys);
		
		//���ݽ��������ת��SQL��ѯ���ݿ�
		results=qs.questionQuery(params,querys);
		System.out.println("��ѯ���Ľ��Ϊ��");
		System.out.println(results);

	}

}
