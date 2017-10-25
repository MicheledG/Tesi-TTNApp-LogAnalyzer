package ttnapp.loganalyzer.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Packet implements Comparable<Packet> {

	public final static DateFormat DEFAULT_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		
	private Date timestamp;
	private String devId;
	private int counter;
	private String dataRate;
	private double frequency;
	private double rssi;
	private double snr;
	private byte[] rawPayload;
	private Point point;
	private double distance;
	
	public Packet() {
		super();
		// TODO Auto-generated constructor stub
	}
	public Packet(Date timestamp, String devId, int counter, String dataRate, double frequency, double rssi, double snr,
			byte[] rawPayload, Point point, double distance) {
		super();
		this.timestamp = timestamp;
		this.devId = devId;
		this.counter = counter;
		this.dataRate = dataRate;
		this.frequency = frequency;
		this.rssi = rssi;
		this.snr = snr;
		this.rawPayload = rawPayload;
		this.point = point;
		this.distance = distance;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public String getDevId() {
		return devId;
	}
	public void setDevId(String devId) {
		this.devId = devId;
	}
	public int getCounter() {
		return counter;
	}
	public void setCounter(int counter) {
		this.counter = counter;
	}
	public String getDataRate() {
		return dataRate;
	}
	public void setDataRate(String dataRate) {
		this.dataRate = dataRate;
	}
	public double getFrequency() {
		return frequency;
	}
	public void setFrequency(double frequency) {
		this.frequency = frequency;
	}
	public double getRssi() {
		return rssi;
	}
	public void setRssi(double rssi) {
		this.rssi = rssi;
	}
	public double getSnr() {
		return snr;
	}
	public void setSnr(double snr) {
		this.snr = snr;
	}
	public byte[] getRawPayload() {
		return rawPayload;
	}
	public void setRawPayload(byte[] rawPayload) {
		this.rawPayload = rawPayload;
	}
	public Point getPoint() {
		return point;
	}
	public void setPoint(Point point) {
		this.point = point;
	}
	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}
	@Override
	public int compareTo(Packet o) {
		Date thisTimestamp = this.getTimestamp();
		Date thatTimestamp = o.getTimestamp();
		return thisTimestamp.compareTo(thatTimestamp);
	}
	public void printPacket(){
		System.out.println("Packet timestamp: "+DEFAULT_TIMESTAMP_FORMAT.format(this.getTimestamp()));
		System.out.println("Packet devId: "+this.getDevId());
		System.out.println("Packet counter: "+this.getCounter());
		System.out.println("Packet data rate: "+this.getDataRate());
		System.out.println("Packet frequency: "+this.getFrequency());
		System.out.println("Packet RSSI: "+this.getRssi());
		System.out.println("Packet SNR: "+this.getSnr());
		System.out.println("Packet point timestamp: "+Point.DEFAULT_TIMESTAMP_FORMAT.format(this.getPoint().getTimestamp()));
		System.out.println("Packet point latitude: "+this.getPoint().getLat());
		System.out.println("Packet point longitude: "+this.getPoint().getLon());
		
		StringBuilder rawPayloadString = new StringBuilder();
		for(byte b:this.getRawPayload()){
			rawPayloadString.append(String.format("%02X", b));
		}
		System.out.println("Packet raw payload: "+rawPayloadString.toString());
	}	
}
