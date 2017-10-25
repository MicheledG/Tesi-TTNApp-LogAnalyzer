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

public class LogAnalyzer {
	
	private static final String DEFAULT_LOG_FILE_NAME = "log.dat";
	private static final String DEFAULT_LOG_FILE_FOLDER = "log";
	private static final String DEFAULT_TRACK_FILE_NAME = "track.dat";
	private static final String DEFAULT_TRACK_FILE_FOLDER = "log";
	private static final String DEFAULT_LOST_PACKETS_FILE_NAME = "lostpackets.dat";
	private static final String DEFAULT_LOST_PACKETS_FILE_FOLDER = "log";
	
	
	public static void main(String[] args) {				
		
		
		String logFileName = DEFAULT_LOG_FILE_NAME;
		String logFileFolder = DEFAULT_LOG_FILE_FOLDER;
		String trackFileName = DEFAULT_TRACK_FILE_NAME;
		String trackFileFolder = DEFAULT_TRACK_FILE_FOLDER;
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
			lostPacketsFileName = args[4];
		}
		if(args.length > 5){
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
						
		//create the lostpackets path
		String lostPacketsPath = lostPacketsFileFolder
				+ "/"
				+ lostPacketsFileName;
		
		//put in memory the received packets
		List<Packet> receivedPackets = null;
		try {
			receivedPackets = readReceivedPackets(logPath);
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		//put in memory the track points
		List<Point> trackPoints = null;
		try {
			trackPoints = readTrack(trackPath);
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		//create the list of the lost packets
		List<Packet> lostPackets = computeLostPackets(receivedPackets);
		
		//geotag the lost packets
		List<Packet> lostPacketsGeo = computeLostPacketsGeo(lostPackets, trackPoints);
		
		//print the list of the lost packets
		for (Packet packet : lostPacketsGeo) {
			System.out.println("===============");
			packet.printPacket();
			System.out.println("===============");
		}
		
		//write the list of the lost packets to the output file
		try {
			logLostPackets(lostPacketsPath, lostPacketsGeo);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
	}

	private static List<Packet> computeLostPacketsGeo(List<Packet> lostPackets, List<Point> trackPoints) {
		
		for (Packet lostPacket : lostPackets) {
			Point packetPoint = findPacketPoint(lostPacket, trackPoints);
			lostPacket.setPoint(packetPoint);
		}
		
		return lostPackets;
	}


	private static Point findPacketPoint(Packet lostPacket, List<Point> trackPoints) {
		
		
		//WARNING: this method works properly only if the trackPoints are chronological sorted
		Point closestPoint = trackPoints.get(0);
		long minDelay = Math.abs(lostPacket.getTimestamp().getTime() - closestPoint.getTimestamp().getTime());
		long tmpDelay = 0;
				
		for (Point point : trackPoints) {
			tmpDelay = Math.abs(lostPacket.getTimestamp().getTime() - point.getTimestamp().getTime());
			
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

	private static void logLostPackets(String lostPacketsPath, List<Packet> lostPackets) throws FileNotFoundException{
    	    	
		FileOutputStream fileOut = new FileOutputStream(lostPacketsPath, false);
		PrintWriter printWriter = new PrintWriter(fileOut, true);
		
		for (Packet packet : lostPackets) {
			
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
					
			printWriter.format("%s\t%s\t%d\t%s\t%f\t%f\t%f\t%s\t%f\t%f\n",
					timestamp,
					devId,
					counter,
					dataRate,
					frequency,
					rssi,
					snr,
					rawPayloadString,
					lat,
					lon
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
					lostPacket.setFrequency(previousPacket.getFrequency());
					lostPacket.setRssi(previousPacket.getRssi());
					lostPacket.setSnr(previousPacket.getSnr());
					lostPacket.setRawPayload(previousPacket.getRawPayload());
					
					lostPackets.add(lostPacket);
				}
			}
		}
		
		Collections.sort(lostPackets);
		
		return lostPackets;
	}

	private static List<Packet> readReceivedPackets(String logPath) throws IOException, ParseException {
		
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