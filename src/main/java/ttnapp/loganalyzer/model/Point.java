package ttnapp.loganalyzer.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Point implements Comparable<Point> {
	
	public final static DateFormat DEFAULT_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	
	private Date timestamp;
	private double lat;
	private double lon;		
	
	public Point(Date timestamp, double lat, double lon) {
		super();
		this.timestamp = timestamp;
		this.lat = lat;
		this.lon = lon;
	}
	public Point() {
		super();
		// TODO Auto-generated constructor stub
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public double getLat() {
		return lat;
	}
	public void setLat(double lat) {
		this.lat = lat;
	}
	public double getLon() {
		return lon;
	}
	public void setLon(double lon) {
		this.lon = lon;
	}
	@Override
	public int compareTo(Point o) {
		Date thisTimestamp = this.getTimestamp();
		Date thatTimestamp = o.getTimestamp();
		return thisTimestamp.compareTo(thatTimestamp);
	}
	public void printPoint(){
		System.out.println("Point timestamp: "+DEFAULT_TIMESTAMP_FORMAT.format(this.getTimestamp()));
		System.out.println("Point latitude: "+this.getLat());
		System.out.println("Point longitude: "+this.getLon());
	}
	
}
