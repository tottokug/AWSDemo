package com.tottokug.awsdemos.ec2.eip;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map.Entry;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.ReleaseAddressRequest;
import com.maxmind.geoip.Country;
import com.maxmind.geoip.LookupService;
import com.tottokug.Config;

public class EIPLoop {
	AmazonEC2Client ec2;
	HashMap<String, Integer> ips;
	LookupService geo;

	public static void main(String[] args) {
		EIPLoop loop = new EIPLoop();
		try {
			loop.setOutputStream(new FileOutputStream("./eip_allocate.txt"));
			loop.run(1000);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public EIPLoop() {
		this.ec2 = new AmazonEC2Client(Config.getAWSCredentials());
		// this.ec2 = new AmazonEC2Client(new BasicAWSCredentials("accessKey",
		// "secretKey"));
		this.ips = new HashMap<String, Integer>();
		try {
			this.geo = new LookupService(new File(
					"/opt/local/share/GeoIP/GeoIP.dat"));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void setOutputStream(OutputStream out) {
		PrintStream ps = new PrintStream(out);
		System.setOut(ps);
	}

	public void run(int times) {
		this.run(times, "ec2.ap-northeast-1.amazonaws.com");
	}

	public void run(int times, String region) {
		this.ec2.setEndpoint(region);
		String ip = "";
		for (int i = 0; i < times; i++) {
			ip = this.allocateAddress();
			this.increment(ip);
			try {
				// —D‚µ‚³’“ü
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			this.releaseAddress(ip);
		}
		this.printSummary();
	}

	private void printSummary() {
		for (Entry<String, Integer> e : this.ips.entrySet()) {
			String ip = e.getKey();
			Country c = this.geo.getCountry(ip);
			System.out.printf("%s\t%-15s\t%d\n", c.getName(), e.getKey(),
					e.getValue());
		}
	}

	private void increment(String key) {
		int count = 0;
		if (this.ips.containsKey(key)) {
			count = this.ips.get(key);
		}
		count++;
		this.ips.put(key, count);
	}

	public void releaseAddress(String ipaddress) {
		ReleaseAddressRequest rar = new ReleaseAddressRequest();
		rar.setPublicIp(ipaddress);
		while (true) {
			try {
				this.ec2.releaseAddress(rar);
			} catch (Exception e) {
				System.out.println("continue");
				continue;
			}
			break;
		}
	}

	public String allocateAddress() {
		AllocateAddressResult res = this.ec2.allocateAddress();
		String ip = res.getPublicIp();
		// System.out.println("IPAdress = " + ip);
		return ip;
	}

}
