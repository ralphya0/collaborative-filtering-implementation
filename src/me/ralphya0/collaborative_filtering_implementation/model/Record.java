package me.ralphya0.collaborative_filtering_implementation.model;


public class Record {

	private String brand_id;
	private int brand_int;
	private Record brandListNest;
	private int counter;
	private Record next;
	private int time_int;
	private int type;
	private String user_id;
	private String visit_datetime;
	public Record(String uid,String bid,int type,int counter){
		this.user_id = uid;
		this.brand_id = bid;
		this.type = type;
		this.counter = counter;
	}
	public Record(String uid,String brandid,int type,String date){
		this.user_id = uid;
		this.brand_id = brandid;
		this.visit_datetime = date;
		this.type = type;
		this.counter = 1;
		this.next = null;
		this.brandListNest = null;
		String t = date.replace("ÔÂ", ",").replace("ÈÕ", " ").trim();
		String [] tt = t.split(",");
		this.time_int = (Integer.parseInt(tt[0])) * 100 + (Integer.parseInt(tt[1])); 
		this.brand_int = Integer.parseInt(brandid);
	}
	public String getBrand_id() {
		return brand_id;
	}
	public int getBrand_int() {
		return brand_int;
	}
	public Record getBrandListNest() {
		return brandListNest;
	}
	public int getCounter() {
		return counter;
	}
	
	public Record getNext() {
		return next;
	}
	
	public int getTime_int() {
		return time_int;
	}
	
	public int getType() {
		return type;
	}
	public String getUser_id() {
		return user_id;
	}
	public String getVisit_datetime() {
		return visit_datetime;
	}
	
	public void setBrand_id(String brand_id) {
		this.brand_id = brand_id;
	}
	public void setBrand_int(int brand_int) {
		this.brand_int = brand_int;
	}
	public void setBrandListNest(Record brandListNest) {
		this.brandListNest = brandListNest;
	}
	
	public void setCounter(int counter) {
		this.counter = counter;
	}
	public void setNext(Record next) {
		this.next = next;
	}
	public void setTime_int(int time_int) {
		this.time_int = time_int;
	}
	
	public void setType(int type) {
		this.type = type;
	}
	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}
	public void setVisit_datetime(String visit_datetime) {
		this.visit_datetime = visit_datetime;
	}
	
}

