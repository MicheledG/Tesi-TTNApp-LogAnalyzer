package ttnapp.loganalyzer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import ttnapp.loganalyzer.model.Packet;
import ttnapp.loganalyzer.model.Point;
import ttnapp.loganalyzer.util.DistanceCalculator;

public class LogAnalyzer {
	
	private static final String DEFAULT_LOG_FILE_NAME = "log.dat";
	private static final String DEFAULT_LOG_FILE_FOLDER = "log";
	private static final String DEFAULT_TRACK_FILE_NAME = "track.dat";
	private static final String DEFAULT_TRACK_FILE_FOLDER = "log";
	private static final String DEFAULT_RECEIVED_PACKETS_FILE_NAME = "receivedpackets.dat";
	private static final String DEFAULT_RECEIVED_PACKETS_FILE_FOLDER = "log";
	private static final String DEFAULT_LOST_PACKETS_FILE_NAME = "lostpackets.dat";
	private static final String DEFAULT_LOST_PACKETS_FILE_FOLDER = "log";
	private static final Point DEFAULT_GATEWAY_POINT;
	
	static {
		//set the gateway @5T, via Bertola
		DEFAULT_GATEWAY_POINT = new Point(
				null,
				45.07136,
				7.67811
				);		
	}
	
	
	public static void main(String[] args) {				
		
		
		String logFileName = DEFAULT_LOG_FILE_NAME;
		String logFileFolder = DEFAULT_LOG_FILE_FOLDER;
		String trackFileName = DEFAULT_TRACK_FILE_NAME;
		String trackFileFolder = DEFAULT_TRACK_FILE_FOLDER;
		String receivedPacketsFileName = DEFAULT_RECEIVED_PACKETS_FILE_NAME;
		String receivedPacketsFileFolder = DEFAULT_RECEIVED_PACKETS_FILE_FOLDER;
		String lostPacketsFileName = DEFAULT_LOST_PACKETS_FILE_NAME;
		String lostPacketsFileFolder = DEFAULT_LOST_PACKETS_FILE_FOLDER;
				
		if(args.length > 0){
			logFileName = args[0];
		}
		if(args.length > 1){
			logFileFolder = args[1];
		}
		if(args.length > 2){
			trackFileName = args[2];
		}
		if(args.length > 3){
			trackFileFolder = args[3];
		}
		if(args.length > 4){
			receivedPacketsFileName = args[4];
		}
		if(args.length > 5){
			receivedPacketsFileFolder = args[5];
		}
		if(args.length > 6){
			lostPacketsFileName = args[4];
		}
		if(args.length > 7){
			lostPacketsFileFolder = args[5];
		}
		
		
		//create the log path
		String logPath = logFileFolder 
				+ "/"
				+ logFileName;
		
		//create the track path
		String trackPath = trackFileFolder 
				+ "/"
				+ trackFileName;
						
		//create the receivedpackets path
		String receivedPacketsPath = receivedPacketsFileFolder
				+ "/"
				+ receivedPacketsFileName;
		
		//create the lostpackets path
		String lostPacketsPath = lostPacketsFileFolder
				+ "/"
				+ lostPacketsFileName;
		
		//put in memory the received packets
		List<Packet> receivedPackets = null;
		try {
			receivedPackets = readLogFile(logPath);
		} catch (IOException | ParseException e) {			
			e.printStackTrace();
			return;
		}
		
		//put in memory the track points
		List<Point> trackPoints = null;
		try {
			trackPoints = readTrack(trackPath);
		} catch (IOException | ParseException e) {
			e.printStackTrace();
			return;
		}
		
		//geotag the received packets
		List<Packet> receivedPacketsGeo = computePacketsGeo(receivedPackets, trackPoints);
		
		//create the list of the lost packets
		List<Packet> lostPackets = computeLostPackets(receivedPackets);
		
		//geotag the lost packets
		List<Packet> lostPacketsGeo = computePacketsGeo(lostPackets, trackPoints);
		
		//print the list of the lost packets
		for (Packet packet : lostPacketsGeo) {
			System.out.println("===============");
			packet.printPacket();
			System.out.println("===============");
		}
		
		//write the list of the received packets to the output file
		try {
			logPackets(receivedPacketsPath, receivedPacketsGeo);
		} catch (FileNotFoundException e) {			
			e.printStackTrace();
			return;
		}
		
		
		//write the list of the lost packets to the output file
		try {
			logPackets(lostPacketsPath, lostPacketsGeo);
		} catch (FileNotFoundException e) {			
			e.printStackTrace();
			return;
		}
		
	}

	private static List<Packet> computePacketsGeo(List<Packet> packets, List<Point> trackPoints) {
		
		//geo tag each packet with a coordinate pair
		for (Packet packet : packets) {
			Point packetPoint = findPacketPoint(packet, trackPoints);
			packet.setPoint(packetPoint);
		}
		
		//compute the distance between each packet and the default gateway position
		for (Packet packet : packets) {
			double distance = DistanceCalculator.distance(
					DEFAULT_GATEWAY_POINT.getLat(),
					DEFAULT_GATEWAY_POINT.getLon(),
					packet.getPoint().getLat(),
					packet.getPoint().getLon(),
					"K"
					) * 1000; 
			packet.setDistance(distance);
		}
		
		return packets;
	}


	private static Point findPacketPoint(Packet packet, List<Point> trackPoints) {
		
		//WARNING: this method works properly only if the trackPoints are chronological sorted
		Point closestPoint = trackPoints.get(0);
		long minDelay = Math.abs(packet.getTimestamp().getTime() - closestPoint.getTimestamp().getTime());
		long tmpDelay = 0;
				
		for (Point point : trackPoints) {
			tmpDelay = Math.abs(packet.getTimestamp().getTime() - point.getTimestamp().getTime());
			
			if(tmpDelay <= minDelay){
				minDelay = tmpDelay;
				closestPoint = point;
			}
			else{
				break;
			}
		}
						
		return closestPoint;
	}

	private static List<Point> readTrack(String trackPath) throws IOException, ParseException {
		List<Point> trackPoints = new ArrayList<>();
		FileReader trackFile = new FileReader(trackPath);
		BufferedReader bufReader = new BufferedReader(trackFile);
		
		boolean eof = false;
		String trackLine = null;

		while(!eof){
			trackLine = bufReader.readLine();
			
			if(trackLine == null){
				eof = true;
				break;
			}
			
			Point trackPoint = extractPoint(trackLine);
			
			trackPoints.add(trackPoint);					
		}
		
		bufReader.close();
		
		Collections.sort(trackPoints);
		
		return trackPoints;
	}

	private static Point extractPoint(String trackLine) throws ParseException {
		Point point = new Point();
		Scanner lineScanner = new Scanner(trackLine);
		lineScanner.useLocale(Locale.US);
		
		String timestamp = lineScanner.next().substring(0, 19);		
		point.setTimestamp(Point.DEFAULT_TIMESTAMP_FORMAT.parse(timestamp.toString()));
		point.setLat(lineScanner.nextDouble());
		point.setLon(lineScanner.nextDouble());
		
		lineScanner.close();
		
		return point;
	}

	private static void logPackets(String filePath, List<Packet> packets) throws FileNotFoundException{
    	    	
		FileOutputStream fileOut = new FileOutputStream(filePath, false);
		PrintWriter printWriter = new PrintWriter(fileOut, true);
		
		for (Packet packet : packets) {
			
			String timestamp = Packet.DEFAULT_TIMESTAMP_FORMAT.format(packet.getTimestamp());
			String devId = packet.getDevId();
			int counter = packet.getCounter();
			String dataRate = packet.getDataRate();
			double frequency = packet.getFrequency();
			byte[] rawPayload = packet.getRawPayload();
			StringBuilder rawPayloadString = new StringBuilder();
			for(byte b:rawPayload){
				rawPayloadString.append(String.format("%02X", b));
			}
			double rssi = packet.getRssi();
			double snr = packet.getSnr();
			double lat = packet.getPoint().getLat();
			double lon = packet.getPoint().getLon();
			int distance = (int) packet.getDistance();
					
			printWriter.format("%s\t%s\t%d\t%s\t%f\t%f\t%f\t%s\t%f\t%f\t%d\n",
					timestamp,
					devId,
					counter,
					dataRate,
					frequency,
					rssi,
					snr,
					rawPayloadString,
					lat,
					lon,
					distance
					);
		}		
		
		printWriter.close();		
    	
    }

	private static List<Packet> computeLostPackets(List<Packet> receivedPackets) {
				
		List<Packet> lostPackets = new ArrayList<>();
		
		int N = receivedPackets.size();
		for(int i = 0; i < N - 1; i++){
			Packet previousPacket = receivedPackets.get(i);
			int  previousPacketCounter = previousPacket.getCounter();			
			Packet nextPacket = receivedPackets.get(i + 1);
			int nextPacketCounter = nextPacket.getCounter();			
			
			if(nextPacketCounter <= previousPacketCounter){
				continue;
			}
			else if(nextPacketCounter > previousPacketCounter + 1){
				//some packets were lost
				int numberOfLostPackets = nextPacketCounter - previousPacketCounter - 1;
				long previousPacketTimestamp = previousPacket.getTimestamp().getTime();
				long nextPacketTimestamp = nextPacket.getTimestamp().getTime();
				long lostInterval = nextPacketTimestamp - previousPacketTimestamp;
				long packetInterval = lostInterval / (nextPacketCounter - previousPacketCounter);
				for(int j = 1; j <= numberOfLostPackets; j++){
					Packet lostPacket = new Packet();
					
					Calendar calendar = Calendar.getInstance();
					calendar.setTimeInMillis(previousPacketTimestamp + (j*packetInterval));					 					
					lostPacket.setTimestamp(calendar.getTime());
					lostPacket.setDevId(previousPacket.getDevId());
					lostPacket.setCounter(previousPacketCounter + j);
					lostPacket.setDataRate(previousPacket.getDataRate());
					lostPacket.setFrequency(0);
					lostPacket.setRssi(0);
					lostPacket.setSnr(0);
					lostPacket.setRawPayload(new byte[]{0, 0, 0, 0});
					
					lostPackets.add(lostPacket);
				}
			}
		}
		
		Collections.sort(lostPackets);
		
		return lostPackets;
	}

	private static List<Packet> readLogFile(String logPath) throws IOException, ParseException {
		
		List<Packet> receivedPackets = new ArrayList<>();
		FileReader logFile = new FileReader(logPath);
		BufferedReader bufReader = new BufferedReader(logFile);
		
		boolean eof = false;
		String logLine = null;

		while(!eof){
			logLine = bufReader.readLine();
			
			if(logLine == null){
				eof = true;
				break;
			}
			
			Packet receivedPacket = extractPacket(logLine);
			
			receivedPackets.add(receivedPacket);					
		}
		
		bufReader.close();
		
		Collections.sort(receivedPackets);
		
		return receivedPackets;
	}

	private static Packet extractPacket(String logLine) throws ParseException {
		
		Packet packet = new Packet();
		Scanner lineScanner = new Scanner(logLine);
		lineScanner.useLocale(Locale.US);
		
		String timestamp = lineScanner.next().substring(0, 19);		
		packet.setTimestamp(Packet.DEFAULT_TIMESTAMP_FORMAT.parse(timestamp.toString()));
		packet.setDevId(lineScanner.next());
		packet.setCounter(lineScanner.nextInt());
		packet.setDataRate(lineScanner.next());
		packet.setFrequency(lineScanner.nextDouble());
		packet.setRssi(lineScanner.nextDouble());
		packet.setSnr(lineScanner.nextDouble());
		
		String rawPayload = lineScanner.next();
		packet.setRawPayload(rawPayload.getBytes(StandardCharsets.UTF_8));
		
		lineScanner.close();
		
		return packet;		
	}

}
