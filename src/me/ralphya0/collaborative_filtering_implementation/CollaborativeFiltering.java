package me.ralphya0.collaborative_filtering_implementation;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.ralphya0.collaborative_filtering_implementation.model.Record;



public class CollaborativeFiltering {

   /*  Implementation of Collaborative Filtering Algorithm.
    *  
    *  This program is used to generate recommendation goods lists for users based on their shopping transaction log.
    *  Collaborative filtering is the underlying algorithm of this program which works with Jacarrd Matrix and Pearson
    *  Matrix separately, after computation, bring out two recommendation lists respectively. 
    *  
	*  Required input file should looks like this : 
	*  	
	*   9842500,16199,0,5月6日
	*   4356600,13299,3,5月8日
	*   ...
	*  
	*  Each row in this file is an entry of transaction log. The first column stands for user id, the second one means
	*  brand id, the third one is action code(0 means click event, 1 means cart event,2 means favorite event and 3
	*  means buy event) and the last column is time stamp of this entry.
	*  
	*  This program do some pre-process(combine duplicate entries and then sort entries) against original input file
	*  before computation actually starts. After that, program will build score matrix according to the sorted transaction
	*  log. With the score matrix we can now generate Jacarrd correlation matrix and Pearson correlation matrix which then
	*  are used to compute correlation coefficients separately, the correlation coefficients indicate the probability of 
	*  user finally buy a particular brand in future.    
	*  
	*  USAGE:
	*  	First, you need to compile this source code;
	*  	then you can run this program in command line like this :
	*  	java CollaborativeFiltering input_file_path output_dir click_threshold neighbor_filter_value recommendation_length_limit[OPTIONNAL] 
	*/
	
	//assistance variables
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	private int [][] scoreMatrix;
	private Map<String , Map<String,List<String>>> mutualSet = new HashMap<String ,Map<String,List<String>>>();
	private float [][] jaccardMatrix;
	private float [][] personMatrix;
	private Map<String , Map<String,Float>> jaccardRecommedation = new HashMap<String,Map<String,Float>>();
	private Map<String , Map<String,Float>> pearsonRecommedation = new HashMap<String,Map<String,Float>>();
	private Map<String, List<String>> jaccardNeighbors = new HashMap<String,List<String>>();
	private Map<String,List<String>> pearsonNeighbor = new HashMap<String,List<String>>();
	private Map<String,Map<String,Float>> buyHistory = new HashMap<String,Map<String,Float>> ();
	private long nextUserIndex = 0;
	private long nextBrandIndex = 0;
	private Map<String , Integer> userIndex = new HashMap<String , Integer>();
	private Map<String ,Integer> brandIndex = new HashMap<String ,Integer>();
	private Map<Integer,String> index2User = new HashMap<Integer,String>();
	private Map<Integer,String> index2Brand = new HashMap<Integer,String>();
	private String filePath;
	private String outputPath;
	private int clickEventthreshold ;
	private int userNum = 0;
	private int brandNum = 0;
	private String recommLongFile;
	private float neighborFilter;
	private int recommCounter = 0;
	private Map<String,Integer> recommLong = null;
	private static final String C = System.getProperty("line.separator");
	private Map<String,Record> userOrderedList = new HashMap<String,Record>();
	private List<String> brandList = new ArrayList<String>();
	private List<String> userList = new ArrayList<String>();
	
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	
	public CollaborativeFiltering(String inputFile,String outputpath,int threshold,float neighborfilter,String recommLongfile){
		this.filePath = inputFile;
		this.outputPath = outputpath;
		this.neighborFilter = neighborfilter;
		this.recommLongFile = recommLongfile;
		this.clickEventthreshold = threshold;		
		//begin the pre-process and then the computation
		compute();
	}
	
	//combine duplicate entries and then sort entries
	public void preProcess(){
		BufferedReader reader = null;
		//the results of pre-process can be reused later so we write them into files 
		BufferedWriter writer1 = null;
		BufferedWriter writer2 = null;
		BufferedReader reader22 = null;
		Date begin = new Date();
		
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.filePath),"GBK"));
			String tmp = "";
			String [] item = null;
			Record work = null;
			Record pre = null;
			while((tmp = reader.readLine()) != null){
				item = tmp.split(",");
				Record tr = new Record(item[0],item[1],Integer.parseInt(item[2]),item[3]);
				work = null;
				pre = null;
				//System.out.println("get item " + item[0] + "," + item[1] + "," + Integer.parseInt(item[2]) + "," + item[3] + ",time_int=" + tr.getTime_int());
				
				if(!this.brandList.contains(tr.getBrand_id())){
					this.brandList.add(tr.getBrand_id());
					this.brandNum ++;
					//System.out.println("<<<<add new brand to brandList");
				}
				if(!this.userList.contains(tr.getUser_id())){
					this.userList.add(tr.getUser_id());
					this.userNum ++;
				}
				//append the new record to the userOrderedList
				if((work = this.userOrderedList.get(tr.getUser_id())) != null ){
					
					//以时间戳和品牌编号来排序，这样就不需要在插入时遍历整个链表
					
					while(work != null && work.getTime_int() < tr.getTime_int()){
						pre = work;
						work = work.getNext();
					}
					
					if(work != null){
						if(work.getTime_int() > tr.getTime_int()){
							if(pre == null){
								tr.setNext(work);
								this.userOrderedList.put(tr.getUser_id(), tr);
							}
							else{
								
								tr.setNext(work);
								pre.setNext(tr);
								
							}
							//System.out.println("<<<<add new item into userOrderedList");
							continue;
						}
						else if(work.getTime_int() == tr.getTime_int()){
							
							while(work != null && work.getTime_int() == tr.getTime_int() 
									&& work.getBrand_int() < tr.getBrand_int()){
								pre = work;
								work = work.getNext();
							}
							
							if(work != null){
								
							
								if(work.getTime_int() > tr.getTime_int()){
									tr.setNext(work);
									pre.setNext(tr);
									//System.out.println("<<<<add new item into userOrderedList");
									continue;
								}
								else if(work.getBrand_int() > tr.getBrand_int()){
									if(pre == null){
										tr.setNext(work);
										this.userOrderedList.put(tr.getUser_id(), tr);
										//System.out.println("<<<<add new item into userOrderedList");
										continue;
									}
									else{
										
									
										tr.setNext(work);
										pre.setNext(tr);
										//System.out.println("<<<<add new item into userOrderedList");
									}
								}
								else if(work.getBrand_int() == tr.getBrand_int()){
									while(work != null && work.getTime_int() == tr.getTime_int() 
											&& work.getBrand_int() == tr.getBrand_int()
											&& work.getType() != tr.getType()){
										pre = work;
										work = work.getNext();
									}
									
									if(work != null){
										if(work.getTime_int() > tr.getTime_int() 
												|| work.getBrand_int() > tr.getBrand_int()) {
											tr.setNext(work);
											pre.setNext(tr);
											//System.out.println("<<<<add new item into userOrderedList");
										}
										
										else if(work.getType() == tr.getType()){
											work.setCounter(work.getCounter() + 1);
											//System.out.println("<<<<combine duplicate entries ");
										}
									}
									else{
										tr.setNext(work);
										pre.setNext(tr);
										//System.out.println("<<<<add new item into userOrderedList");
									}
								}
							}
							else{
								tr.setNext(work);
								pre.setNext(tr);
								//System.out.println("<<<<add new item into userOrderedList");
								continue;
							}
						}
					}
					else{
						tr.setNext(work);
						pre.setNext(tr);
						//System.out.println("<<<<add new item into userOrderedList");
						continue;
					}
				}
				//map 中没有这个记录，新建之
				else{
					this.userOrderedList.put(tr.getUser_id(), tr);
					//System.out.println("<<<<add new item into HashMap");
				}
			}
			//直接输出userOrderedList内容即可（按brandid 进行遍历）
			System.out.println("entry sort done , now begin to write result into file " + this.outputPath + "/userid_ordered_sample.csv");
			//<<<<<<<<< 向文件中输出处理结果
			writer1 = new BufferedWriter(new FileWriter(this.outputPath + "/userid_ordered_sample.csv"));
			StringBuilder sb1 = new StringBuilder();
			String [] keys = this.userOrderedList.keySet().toArray(new String[0]);
			Record work2 = null;
			for(int i = 0;i < keys.length ;i++){
				
				work2 = this.userOrderedList.get(keys[i]);
						
				while(work2 != null){
				
					sb1.append(work2.getUser_id())
						.append(",")
						.append(work2.getBrand_id())
						.append(",")
						.append(work2.getType())
						.append(",")
						.append(work2.getVisit_datetime())
						.append(",")
						.append(work2.getCounter())
						.append(C);
					
					work2 = work2.getNext();
				}				
			}
			writer1.write(sb1.toString());
			
			//System.out.println("Now begin to write result into file " + this.outputPath + "/brandid_ordered_sample.csv");
			//<<<<<<<<
			writer2 = new BufferedWriter(new FileWriter(this.outputPath + "/brandid_ordered_sample.csv"));
			StringBuilder sb2 = new StringBuilder();
			String temp = null;
			Record work3 = null;
			for(int j = 0; j < this.brandList.size() ;j ++){
				temp = brandList.get(j);
				for(int k = 0;k < keys.length ; k ++){
					work3 = this.userOrderedList.get(keys[k]);
					while(work3 != null){
						if(work3.getBrand_id().equals(temp)){
							sb2.append(work3.getBrand_id())
							.append(",")
							.append(work3.getUser_id())
							.append(",")
							.append(work3.getType())
							.append(",")
							.append(work3.getVisit_datetime())
							.append(",")
							.append(work3.getCounter())
							.append(C);
						}
						
						work3 = work3.getNext();
					}
				}
			}
			//filePath = this.outputPath + "/userid_ordered_sample.csv";
			writer2.write(sb2.toString());
			Date end = new Date();
			System.out.println("pre-process done !");
			System.out.println("<<<<<<<<< 共耗时 ： " + (begin.getTime() - end.getTime())/1000 + "秒");
			
			scoreMatrix = new int[this.userNum][this.brandNum];
			jaccardMatrix = new float[userNum][brandNum];
			personMatrix = new float[userNum][brandNum];
			//initialize array
			for(int i = 0;i < userNum;i ++){
				for(int j = 0;j < brandNum; j ++){
					scoreMatrix[i][j] = 0;
				}
			}
			for(int i = 0;i < userNum;i ++){
				for(int j = 0;j < brandNum;j ++){
					jaccardMatrix[i][j] = 0;
					personMatrix[i][j] = 0;
				}
			}
			//release resources
			this.userList = null;
			this.brandList = null;
			this.userOrderedList = null;
			
			if(this.recommLongFile != null){
			
				this.recommLong = new HashMap<String,Integer>();
				
				reader22 = new BufferedReader(new InputStreamReader(new FileInputStream(this.recommLongFile),"UTF-8"));
				String t = null;
				String [] tt = null;
				while((t = reader22.readLine()) != null && !"".equals(t)){
					tt = t.split(",");
					recommLong.put(tt[1], Integer.parseInt(tt[2].trim()));
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			if(reader != null){
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(writer1 != null){
				try {
					writer1.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(writer2 != null){
				try {
					writer2.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(reader22 != null){
				try {
					reader22.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	//build score matrix according to transaction log
	public void buildScoreMatrix(){
		BufferedReader reader = null;
		//BufferedWriter writer = null;
		BufferedWriter writer2 = null;
		BufferedWriter writer3 = null;
		BufferedWriter writer4 = null;
		BufferedWriter writer5 = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.outputPath + "/userid_ordered_sample.csv"),"UTF-8"));
			String line = null;
			String [] temp = null;
			Record work = null;
			Integer uindex = null;
			Integer bindex = null;
			int actionCounter = 0;
			int actionType = -1;
			while((line = reader.readLine()) != null){
				temp = line.split(",");
				uindex = null;
				bindex = null;
				actionCounter = 0;
				actionType = -1;
				work = new Record(temp[0],temp[1],Integer.parseInt(temp[2]),Integer.parseInt(temp[4]));
				
				
				//分配新行
				if((bindex = this.brandIndex.get(work.getBrand_id())) == null){
					this.brandIndex.put(work.getBrand_id(), (int) nextBrandIndex ++);
					bindex = this.brandIndex.get(work.getBrand_id());
					this.index2Brand.put(bindex, work.getBrand_id());
				}
				
				if((uindex = this.userIndex.get(work.getUser_id())) == null){
					//该用户还未被分配独立的数组行
					this.userIndex.put(work.getUser_id(), (int) nextUserIndex ++);
					uindex = this.userIndex.get(work.getUser_id());
					this.index2User.put(uindex, work.getUser_id());
				}
				
				//将当前record存入数组
				
				if((actionCounter = work.getCounter()) > 0 && (actionType = work.getType()) > -1){
					if(this.scoreMatrix[uindex][bindex] == 5){
						continue;
					}
					else if(actionType == 1){
						//购买行为
						this.scoreMatrix[uindex][bindex] = 5;
						continue;
					}
					else if(actionType == 0 && actionCounter > this.clickEventthreshold){
						this.scoreMatrix[uindex][bindex] = 3;
						continue;
					}
					else if(actionType == 2){
						this.scoreMatrix[uindex][bindex] = 3;
						continue;
					}
					else if(actionType == 3){
						this.scoreMatrix[uindex][bindex] = 4;
					}
				}
			}
			
			//打印计算得到的矩阵
			StringBuilder sb2 = new StringBuilder();
			//sb3以链表形式打印结果矩阵
			StringBuilder sb3 = new StringBuilder();
			
			for(int i = 0;i < this.userNum; i ++){
				//sb2.append(index2User.get(i) + ",");
				sb3.append(index2User.get(i) + "---->");
			
				
				for(int j = 0;j < this.brandNum ; j ++){
					
					//sb.append(this.scoreMatrix[i][j] + "\t");
					sb2.append(this.scoreMatrix[i][j] + ",");
					if(this.scoreMatrix[i][j] > 0){
						sb3.append(index2Brand.get(j) + "#" + scoreMatrix[i][j] + ",");
						
					}
					
				}
				if(sb3.lastIndexOf(",") == sb3.length() - 1){
					sb3.deleteCharAt(sb3.length() - 1);
				}
				if(sb2.lastIndexOf(",") == sb2.length() - 1){
					sb2.deleteCharAt(sb2.length() - 1);
				}
				//sb.append(C);
				sb2.append('\n');
				sb3.append('\n');
			}
			//writer = new BufferedWriter(new FileWriter(this.outFile));
			//writer.write(sb.toString());
			
			writer2 = new BufferedWriter(new FileWriter(this.outputPath + "/score_matrix.csv"));
			
			writer2.write(sb2.toString());
			//再以链表的形式存储评分矩阵(过滤掉所有评分为零点元素)
			writer3 = new BufferedWriter(new FileWriter(this.outputPath + "/score_list.csv"));
			writer3.write(sb3.toString());
			
			//打印辅助文件
			StringBuilder sb4 = new StringBuilder();
			for(int i = 0;i < this.userNum;i ++){
				sb4.append(i + "," + this.index2User.get(i) + '\n');
			}
			writer4 = new BufferedWriter(new FileWriter(this.outputPath + "/index_2_userid_assist.csv"));
			writer4.write(sb4.toString());
			
			StringBuilder sb5 = new StringBuilder();
			for(int i = 0;i < this.brandNum;i ++){
				sb5.append(i + "," + this.index2Brand.get(i) + '\n');
			}
			writer5 = new BufferedWriter(new FileWriter(this.outputPath + "/index_2_brandid_assist.csv"));
			writer5.write(sb5.toString());
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			if(reader != null){
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(writer2 != null){
				try {
					writer2.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if(writer3 != null){
				try {
					writer3.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(writer4 != null){
				try {
					writer4.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(writer5 != null){
				try {
					writer5.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	//build Jaccard correlation matrix
	public void buildJaccardMatrix(){
		BufferedWriter writer = null;
		StringBuilder sb = new StringBuilder();
		
		int currentHost = 0;
		int neighborTest = 0;
		
		float j = 0;
		long f11 = 0;
		long fTotal = 0;
		//对称矩阵
		for(currentHost = 0;currentHost < this.userNum; currentHost ++){
			
			for(neighborTest = currentHost + 1;neighborTest < this.userNum; neighborTest ++){
				f11 = 0;
				fTotal = 0;
				j = 0;
				for(int k = 0;k < this.brandNum;k ++){
					if(this.scoreMatrix[currentHost][k] != 0 && this.scoreMatrix[neighborTest][k] != 0){
						f11 ++;
						fTotal ++;
					}
					else if(this.scoreMatrix[currentHost][k] != 0 || this.scoreMatrix[neighborTest][k] != 0){
						fTotal ++;
					
						
					}
				}
				
				//计算jaccard系数
				if(fTotal != 0){
					j = (float)f11/(float)fTotal;
					this.jaccardMatrix[currentHost][neighborTest] = j;
					this.jaccardMatrix[neighborTest][currentHost] = j;
				}
				
			}
			
			
		}
		
		//将结果写入文件
		try {
			//sb.append("--------\t");
			/*for(int i = 0;i < this.userNum;i ++){
				sb.append(this.index2User.get(i) + "\t");
			}*/
			//sb.append(C);
			for(int i = 0;i < this.userNum; i ++){
				//sb.append(this.index2User.get(i) + "\t");
				for(int k = 0;k < this.userNum ;k ++){
					sb.append(this.jaccardMatrix[i][k] + ",");
					
				}
				if(sb.lastIndexOf(",") == sb.length() - 1){
					sb.deleteCharAt(sb.length() - 1);
				}
				sb.append('\n');
			}
			writer = new BufferedWriter(new FileWriter(this.outputPath + "/jaccard_matrix.csv"));
			writer.write(sb.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			if(writer != null){
				try {
					writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	//build Pearson correlation matrix
	public void buildPearsonMatrix(){
		
		//对称矩阵
		
		//首先计算两用户的共有集
		// 采用二级表的方式存储共有集合
		//初始化共有集结构
		for(int i = 0;i < this.userNum;i ++){
			Map<String , List<String>> item = new HashMap<String , List<String>>();
			for(int j = 0;j < this.userNum;j ++){
				if(j == i){
					continue;
				}
				List<String> mutualItems = new ArrayList<String>();
				item.put(this.index2User.get(j), mutualItems);
				
			}
			this.mutualSet.put(this.index2User.get(i), item);
		}
		//建立共有集
		for(int i = 0;i < this.userNum;i ++){
			for(int j = i + 1;j < this.userNum;j ++){
				//一定要注意对称性  u-v与v-u
				for(int k = 0;k < this.brandNum;k ++){
					if(this.scoreMatrix[i][k] > 0 && this.scoreMatrix[j][k] > 0){
						this.mutualSet.get(this.index2User.get(i)).get(this.index2User.get(j)).add(this.index2Brand.get(k));
						this.mutualSet.get(this.index2User.get(j)).get(this.index2User.get(i)).add(this.index2Brand.get(k));
						
						
					}
				}
			}
		}
		
		List<String> array = null;
		float averageU = 0;
		float averageV = 0;
		float sim = 0;
		int sum1 = 0;
		int sum2 = 0;
		float sum3 = 0;
		float sum4 = 0;
		float sum5 = 0;
		//计算相关矩阵
		for(int i = 0;i < this.userNum;i ++){
			
			for(int j = i + 1;j < this.userNum; j ++){
				//u-v和v-u的相关性
				array = null;
				averageU = 0;
				averageV = 0;
				sim = 0;
				sum1 = 0;
				sum2 = 0;
				sum3 = 0;
				sum4 = 0;
				sum5 = 0;
				if((array = this.mutualSet.get(this.index2User.get(i)).get(this.index2User.get(j))) != null 
						&& array.size() > 0){
					for(int x = 0;x < array.size(); x ++){
						sum1 += this.scoreMatrix[i][this.brandIndex.get(array.get(x))];
						sum2 += this.scoreMatrix[j][this.brandIndex.get(array.get(x))];
						
					}
					averageU = (float)sum1 / (float)array.size();
					averageV = (float)sum2 / (float)array.size();
					
					for(int c = 0;c < array.size();c ++){
						sum3 += ((float)this.scoreMatrix[i][this.brandIndex.get(array.get(c))] - averageU)
								* ((float)this.scoreMatrix[j][this.brandIndex.get(array.get(c))] - averageV) ;
						
						sum4 += ((float)this.scoreMatrix[i][this.brandIndex.get(array.get(c))] - averageU)
								* ((float)this.scoreMatrix[i][this.brandIndex.get(array.get(c))] - averageU) ;
						
						sum5 += ((float)this.scoreMatrix[j][this.brandIndex.get(array.get(c))] - averageV)
								* ((float)this.scoreMatrix[j][this.brandIndex.get(array.get(c))] - averageV) ;
					}
					
					if(sum4 > 0 && sum5 > 0){
						sim =  sum3 / (float)(Math.sqrt(sum4) * Math.sqrt(sum5));
						this.personMatrix[i][j] = sim;
						this.personMatrix[j][i] = sim;
					}
				}
				/*else{
					//无共有集
					this.personMatrix[i][j] = 0;
					this.personMatrix[j][i] = 0;
				}*/
				
			}
		}
		//打印矩阵
		BufferedWriter writer = null;
		try {
			StringBuilder sb = new StringBuilder();
			for(int i = 0;i < this.userNum;i ++){
				for(int j = 0;j < this.userNum;j ++){
					sb.append(this.personMatrix[i][j] + ",");
				}
				if(sb.lastIndexOf(",") == sb.length() - 1){
					sb.deleteCharAt(sb.length() - 1);
				}
				sb.append('\n');
			}
			writer = new BufferedWriter(new FileWriter(this.outputPath + "/pearson_matrix.csv"));
			writer.write(sb.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			if(writer != null){
				try {
					writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	
	//generate recommendation list
	public void recommendationItems(){
		
		//首先过滤并选择出各用户的“邻居”
		
		//初始化
		for(int i = 0;i < this.userNum;i ++){
			List<String> nei = new ArrayList<String>();
			this.jaccardNeighbors.put(this.index2User.get(i), nei);
			
			List<String> nei2 = new ArrayList<String>();
			this.pearsonNeighbor.put(this.index2User.get(i), nei2);
			
			//其中的float用于存储品牌对当前用户的推荐评分     每轮处理过后必须将该值清空
			Map<String,Float> his1 = new HashMap<String,Float>();
			this.buyHistory.put(this.index2User.get(i), his1);
			
			Map<String,Float> m1 = new HashMap<String,Float>();
			this.jaccardRecommedation.put(this.index2User.get(i), m1);
			
			Map<String,Float> m2 = new HashMap<String ,Float>();
			this.pearsonRecommedation.put(this.index2User.get(i), m2);

		}
		//筛选邻居
		for(int i = 0;i < this.userNum;i ++){
			for(int j = 0;j < this.userNum;j ++){
				if(this.jaccardMatrix[i][j] >= this.neighborFilter){
					this.jaccardNeighbors.get(this.index2User.get(i)).add(this.index2User.get(j));
				}
				if(this.personMatrix[i][j] >= this.neighborFilter){
					this.pearsonNeighbor.get(this.index2User.get(i)).add(this.index2User.get(j));
				}
			}
		}
		int buyTotal = 0;
		//选取出每个用户的购买历史（参考评分矩阵 ，但必须保证用户购买行为的评分是唯一的，否则必须从userid_ordered中读取数据.而且无法从评分矩阵得到购买数量信息）
		for(int i = 0;i < this.userNum;i ++){
			for(int j = 0;j < this.brandNum;j ++){
				if(this.scoreMatrix[i][j] == 5){
					this.buyHistory.get(this.index2User.get(i)).put(this.index2Brand.get(j), (float) 0);
					buyTotal ++;
				}
			}
		}
		
		
		String [] a1 = this.buyHistory.keySet().toArray(new String[0]);
		String [] a2 = null;
		String [] a3 = null;
		Map<String,Integer> brand2DegreeJac = null;
		Map<String,Integer> brand2DegreePear = null;
		for(int i = 0;i < this.userNum;i ++){
			brand2DegreeJac = null;
			brand2DegreePear = null;
			brand2DegreeJac = new HashMap<String,Integer>();
			brand2DegreePear = new HashMap<String,Integer>();
			a3 = null;
			//首先为当前 host初始化所有 buyhistory中相关商品的value
			if(a1 != null && a1.length > 0){
				
				for(int k = 0;k < a1.length;k ++){
					a2 = null;
					a2 = this.buyHistory.get(a1[k]).keySet().toArray(new String[0]);
					if(a2 != null && a2.length > 0){
						for(int q = 0;q < a2.length;q ++){
							if(k == i){
								this.buyHistory.get(a1[k]).put(a2[q], (float) 1);
								//向community 中添加商品
								brand2DegreeJac.put(a2[q], 1);
								brand2DegreePear.put(a2[q], 1);
							}
							else{
								
								this.buyHistory.get(a1[k]).put(a2[q], (float) 0);
							}
							
						}
					}
				}
				
				// 初始化各商品 在当前community 中的 degree
				
				//jaccard
				List<String> jn = this.jaccardNeighbors.get(a1[i]);
				if(jn != null && jn.size() > 0){
					for(int u = 0;u < jn.size(); u ++){
						a3 = this.buyHistory.get(jn.get(u)).keySet().toArray(new String[0]);
						
						if(a3 != null && a3.length > 0){
						
							for(int p = 0;p < a3.length;p ++){
								if(brand2DegreeJac.get(a3[p]) != null){
									brand2DegreeJac.put(a3[p], brand2DegreeJac.get(a3[p]) + 1);
								}
								else{
									brand2DegreeJac.put(a3[p], 1);
								}
							}
						}
					}
				}
				
				
				a3 = null;
				//pearson
				List<String> pn = this.pearsonNeighbor.get(a1[i]);
				if(pn != null && pn.size() > 0){
					for(int m = 0;m < pn.size();m ++){
						a3 = this.buyHistory.get(pn.get(m)).keySet().toArray(new String[0]);
						if(a3 != null && a3.length > 0){
							for(int h = 0;h < a3.length;h ++){
								if(brand2DegreePear.get(a3[h]) != null){
									brand2DegreePear.put(a3[h], brand2DegreePear.get(a3[h]) + 1);
								}
								else{
									brand2DegreePear.put(a3[h], 1);
								}
							}
						}
					}
				}
				
				a3 = null;
				//然后计算邻居的value
				Map<String,Float> userValueJac = new HashMap<String,Float>();
				
				// jaccard
				if(jn != null && jn.size() > 0){
					
					for(int t = 0;t < jn.size();t ++){
						a3 = this.buyHistory.get(jn.get(t)).keySet().toArray(new String[0]);
						float tempSum = 0;
						if(a3 != null && a3.length > 0){
							for(int y = 0;y < a3.length;y ++){
								
								if(this.buyHistory.get(a1[i]).containsKey(a3[y])){
									tempSum += (float) 1 / (float) brand2DegreeJac.get(a3[y]);
								}
							}
							userValueJac.put(jn.get(t), tempSum);
						}
					}
				}
				
				
				a3 = null;
				//pearson
				Map<String,Float> userValuePear = new HashMap<String,Float>();
				if(pn != null && pn.size() > 0){
					for(int e = 0;e < pn.size();e ++){
						float tempSum2 = 0;
						a3 = this.buyHistory.get(pn.get(e)).keySet().toArray(new String[0]);
						if(a3 != null && a3.length > 0){
							for(int w = 0;w < a3.length;w ++){
								if(this.buyHistory.get(a1[i]).containsKey(a3[w])){
									tempSum2 += (float) 1 / (float) brand2DegreePear.get(a3[w]);
								}
							}
							userValuePear.put(pn.get(e), tempSum2);
						}
					}
				}
				
				
				//最后计算待推荐品牌的value, 不包含当前用户a1[i]已经购买的物品
				
				a3 = null;
				
				//jaccard
				if(jn != null && jn.size() > 0){
					for(int g = 0;g < jn.size();g ++){
						
						a3 = this.buyHistory.get(jn.get(g)).keySet().toArray(new String[0]);
						
						if(a3 != null && a3.length > 0){
							for(int b = 0;b < a3.length;b ++){
								
								if(!this.buyHistory.get(a1[i]).containsKey(a3[b]) 
										&& !this.jaccardRecommedation.get(a1[i]).containsKey(a3[b])){
									float recommValue = 0;
									//在当前community 中查看还有哪些邻居购买了该物品
									for(int f = 0;f < jn.size();f ++){
										if(this.buyHistory.get(jn.get(f)).containsKey(a3[b])){
											
											recommValue += userValueJac.get(jn.get(f)) / (float) (this.buyHistory.get(jn.get(f)).size()); 
										}
											
									}
									if(recommValue > 0){
										this.jaccardRecommedation.get(a1[i]).put(a3[b], recommValue);
									}
								}
							}
						}
					}
				}
				
				
				a3 = null;
				//pearson
				if(pn != null && pn.size() > 0){
					for(int s = 0;s < pn.size();s ++){
						a3 = this.buyHistory.get(pn.get(s)).keySet().toArray(new String[0]);
						if(a3 != null && a3.length > 0){
							for(int z = 0;z < a3.length;z ++){
								if(!this.buyHistory.get(a1[i]).containsKey(a3[z])
										&& !this.pearsonRecommedation.get(a1[i]).containsKey(a3[z])){
									float res = 0;
									for(int r = 0;r < pn.size(); r ++){
										
										if(this.buyHistory.get(pn.get(r)).containsKey(a3[z])){
											res += userValuePear.get(pn.get(r)) / (float) (this.buyHistory.get(pn.get(r)).size());
										}
									}
									if(res > 0){
										this.pearsonRecommedation.get(a1[i]).put(a3[z], res);
									}
								}
							}
						}
					}
				}
			}
			
		}
		
		//所有结果已就绪，打印推荐列表
		
		//打印jaccardNeighbor
		BufferedWriter writer = null;
		BufferedWriter writer2 = null;
		BufferedWriter writer3 = null;
		BufferedWriter writer4 = null;
		BufferedWriter writer5 = null;
		BufferedWriter writer6 = null;
		BufferedWriter writer7 = null;
		BufferedWriter writer8 = null;
		try {
			StringBuilder sb1 = new StringBuilder();
			String [] tt = this.jaccardNeighbors.keySet().toArray(new String[0]);
			for(int u = 0;u < tt.length;u ++){
				//为方便观察，将无实际数据的记录从结果中剔除，不再打印
				if(this.jaccardNeighbors.get(tt[u]) != null && this.jaccardNeighbors.get(tt[u]).size() > 0){
					sb1.append(tt[u] + '\t');
					
					for(int r = 0;r < this.jaccardNeighbors.get(tt[u]).size();r ++){
						sb1.append(this.jaccardNeighbors.get(tt[u]).get(r) + ",");
					}
					if(sb1.lastIndexOf(",") == sb1.length() - 1){
						sb1.deleteCharAt(sb1.length() - 1);
					}
					sb1.append('\n');
				}
			}
			writer = new BufferedWriter(new FileWriter(this.outputPath + "/jaccard_neighbor_list.csv"));
			writer.write(sb1.toString());
			
			//打印pearsonNeighbor
			tt = null;
			StringBuilder sb2 = new StringBuilder();
			tt = this.pearsonNeighbor.keySet().toArray(new String[0]);
			for(int u = 0;u < tt.length;u ++){
				if(this.pearsonNeighbor.get(tt[u]) != null && this.pearsonNeighbor.get(tt[u]).size() > 0){
				
					sb2.append(tt[u] + '\t');
					for(int x = 0;x < this.pearsonNeighbor.get(tt[u]).size();x ++){
						sb2.append(this.pearsonNeighbor.get(tt[u]).get(x) + ",");
					}
					if(sb2.lastIndexOf(",") == sb2.length() - 1){
						sb2.deleteCharAt(sb2.length() - 1);
					}
					sb2.append('\n');
				}
			}
			writer2 = new BufferedWriter(new FileWriter(this.outputPath + "/pearson_neighbor_list.csv"));
			writer2.write(sb2.toString());
			
			//打印jaccardRecommedation
			StringBuilder sb3 = new StringBuilder();
			StringBuilder sb5 = new StringBuilder();
			tt = null;
			String [] tt2 = null;
			tt = this.jaccardRecommedation.keySet().toArray(new String[0]);
			for(int h = 0;h < tt.length;h ++){
				tt2 = null;
				
				tt2 = this.jaccardRecommedation.get(tt[h]).keySet().toArray(new String[0]);
				if(tt2 != null && tt2.length > 0){
					sb3.append(tt[h] + '\t');
					sb5.append(tt[h] + '\t');
					for(int y = 0;y < tt2.length;y ++){
						sb3.append(tt2[y] + ",");
						sb5.append(tt2[y] + "#" +this.jaccardRecommedation.get(tt[h]).get(tt2[y])+ ",");
						
					}
					if(sb3.lastIndexOf(",") == sb3.length() - 1){
						sb3.deleteCharAt(sb3.length() - 1);
					}
					sb3.append('\n');
					if(sb5.lastIndexOf(",") == sb5.length() - 1){
						sb5.deleteCharAt(sb5.length() - 1);
					}
					sb5.append('\n');
				}
			}
			writer3 = new BufferedWriter(new FileWriter(this.outputPath + "/jaccard_recommendation.csv"));
			writer3.write(sb3.toString());
			//System.out.println("sb5 = " + sb5.toString());
			
			
			writer5 = new BufferedWriter(new FileWriter(this.outputPath + "/jaccard_recommendation_with_score.csv"));
			writer5.write(sb5.toString());
			
			//打印pearsonRecommdation
			StringBuilder sb4 = new StringBuilder();
			StringBuilder sb6 = new StringBuilder();
			tt = null;
			tt2 = null;
			tt = this.pearsonRecommedation.keySet().toArray(new String[0]);
			for(int u = 0;u < tt.length;u ++){
				tt2 = null;
				tt2 = this.pearsonRecommedation.get(tt[u]).keySet().toArray(new String[0]);
				if(tt2 != null && tt2.length > 0){
					sb4.append(tt[u] + '\t');
					sb6.append(tt[u] + '\t');
					for(int v = 0;v < tt2.length;v ++){
						sb4.append(tt2[v] + ",");
						sb6.append(tt2[v] + "#" +this.pearsonRecommedation.get(tt[u]).get(tt2[v])+ ",");
						
					}
					if(sb4.lastIndexOf(",") == sb4.length() - 1){
						sb4.deleteCharAt(sb4.length() - 1);
					}
					sb4.append('\n');
					if(sb6.lastIndexOf(",") == sb6.length() - 1){
						sb6.deleteCharAt(sb6.length() - 1);
					}
					sb6.append('\n');
				}
			}
			writer4 = new BufferedWriter(new FileWriter(this.outputPath + "/pearson_recommendation.csv"));
			writer4.write(sb4.toString());
			
			writer6 = new BufferedWriter(new FileWriter(this.outputPath + "/pearson_recommendation_with_score.csv"));
			writer6.write(sb6.toString());
			
			
			//<<<<<<<<< 将对推荐长度的限制纳入结果生成过程 (若用户提供了推荐长度限制)
			
			if(this.recommLong != null){
				
				//从推荐列表为各位用户选取出规定长度的推荐品牌
				int jr = 0;
				int pr = 0;
				List<String> l1 = null;
				List<String> l2 = null;
				StringBuilder sb7 = new StringBuilder();
				StringBuilder sb8 = new StringBuilder();
				String max = null;
				int index = 0;
				for(int u = 0;u < this.userNum;u ++){
					jr = 0;
					pr = 0;
					l1 = null;
					l2 = null;
					jr = this.jaccardRecommedation.get(this.index2User.get(u)).keySet().toArray(new String[0]).length;
					pr = this.pearsonRecommedation.get(this.index2User.get(u)).keySet().toArray(new String[0]).length;
					
					if(jr > 0 && this.recommLong.get(this.index2User.get(u)) != null
							&& this.recommLong.get(this.index2User.get(u)) > 0){
							sb7.append(this.index2User.get(u) + '\t');
							//选取出value最高的x个推荐品牌
							l1 = new ArrayList<String>(this.jaccardRecommedation.get(this.index2User.get(u)).keySet());
							
							max = null;
							index = 0;
							index = Math.min(jr, this.recommLong.get(this.index2User.get(u)));
							
							for(int y = 0;y < index;y ++){
								max = l1.get(0);
								for(int o = 1;o < l1.size();o ++){
									if(this.jaccardRecommedation.get(this.index2User.get(u)).get(max) 
											< this.jaccardRecommedation.get(this.index2User.get(u)).get(l1.get(o))){
										max = l1.get(o);
									}
									
								}
								sb7.append(max + ",");
								l1.remove(l1.indexOf(max));
							}
							if(sb7.lastIndexOf(",") == sb7.length() - 1){
								sb7.deleteCharAt(sb7.length() - 1);
							}
							sb7.append('\n');
						
					}
					if(pr == 0 && this.recommLong.get(this.index2User.get(u)) != null 
							&& this.recommLong.get(this.index2User.get(u)) > 0){
					}
						
					if(pr > 0 && this.recommLong.get(this.index2User.get(u)) != null 
							&& this.recommLong.get(this.index2User.get(u)) > 0){
						l2 = new ArrayList<String>(this.pearsonRecommedation.get(this.index2User.get(u)).keySet());
						sb8.append(this.index2User.get(u) + '\t');
						index = 0;
						index = Math.min(pr, this.recommLong.get(this.index2User.get(u)));
						max = null;
						this.recommCounter += index;
						for(int w = 0;w < index;w ++){
							max = l2.get(0);
							for(int k = 1;k < l2.size();k ++){
								if(this.pearsonRecommedation.get(this.index2User.get(u)).get(max)
										< this.pearsonRecommedation.get(this.index2User.get(u)).get(l2.get(k))){
									max = l2.get(k);
								}
								
							}
							
							sb8.append(max + ",");
							l2.remove(l2.indexOf(max));
						}
						if(sb8.lastIndexOf(",") == sb8.length() - 1){
							sb8.deleteCharAt(sb8.length() - 1);
						}
						sb8.append('\n');
					}
					//临时
					else if(pr > 0){
						sb8.append(this.index2User.get(u) + '\t');
						l2 = new ArrayList<String>(this.pearsonRecommedation.get(this.index2User.get(u)).keySet());
						sb8.append(l2.get(0));
						sb8.append('\n');
					}
				}
				
				writer7 = new BufferedWriter(new FileWriter(this.outputPath + "/jaccard_recommendation_with_limitation.txt"));
				writer7.write(sb7.toString());
				
				writer8 = new BufferedWriter(new FileWriter(this.outputPath + "/pearson_recommendation_with_limitation.txt"));
				writer8.write(sb8.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(writer != null){
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(writer2 != null){
			try {
				writer2.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(writer3 != null){
			try {
				writer3.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(writer4 != null){
			try {
				writer4.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(writer5 != null){
			try {
				writer5.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(writer6 != null){
			try {
				writer6.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(writer7 != null){
			try {
				writer7.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(writer8 != null){
			try {
				writer8.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public void compute(){
		Date begin = new Date();
		preProcess();
		buildScoreMatrix();
		buildJaccardMatrix();
		buildPearsonMatrix();
		recommendationItems();
		Date end = new Date();
		System.out.println("<<<<<<<<共耗时  ：" + (end.getTime() - begin.getTime()) / 1000 + "秒");
		System.out.println("共推荐商品 ： " + this.recommCounter);
	}
	public static void main(String[] args){
		if(args.length < 4)
			System.exit(1);
		
		if(args.length == 5)
			new CollaborativeFiltering(args[0],args[1],Integer.parseInt(args[2]),Float.parseFloat(args[3]),args[4]);
		else 
			new CollaborativeFiltering(args[0],args[1],Integer.parseInt(args[2]),Float.parseFloat(args[3]),null);
			
	}
}

