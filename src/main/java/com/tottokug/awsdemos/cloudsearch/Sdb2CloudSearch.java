package com.tottokug.awsdemos.cloudsearch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudsearch.AmazonCloudSearch;
import com.amazonaws.services.cloudsearch.AmazonCloudSearchClient;
import com.amazonaws.services.cloudsearch.model.CreateDomainRequest;
import com.amazonaws.services.cloudsearch.model.DefineIndexFieldRequest;
import com.amazonaws.services.cloudsearch.model.IndexDocumentsRequest;
import com.amazonaws.services.cloudsearch.model.IndexField;
import com.amazonaws.services.cloudsearch.model.IndexFieldType;
import com.amazonaws.services.elasticmapreduce.util.BootstrapActions.ConfigFile;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.tottokug.Config;

public class Sdb2CloudSearch {

	private final static String AWS_ACCESS_KEY = Config
			.getValue(Config.KEY.ACCESS_KEY);
	private final static String AWS_SECRET_KEY = Config
			.getValue(Config.KEY.SECRET_KEY);
	private final static String CS_DOMAIN = Config
			.getValue(Config.KEY.SDBToCloudSearch_CLOUDSEARCH_DOMAIN);
	private final static String SDB_DOMAIN = Config
			.getValue(Config.KEY.SDBToCloudSearch_SIMPLEDB_DOMAIN);

	private AmazonSimpleDBClient sdb;
	private AmazonCloudSearch cs;

	public Sdb2CloudSearch() {
		AWSCredentials cred = new BasicAWSCredentials(AWS_ACCESS_KEY,
				AWS_SECRET_KEY);
		this.sdb = new AmazonSimpleDBClient(cred);
		this.sdb.setEndpoint("sdb.ap-northeast-1.amazonaws.com");
		this.cs = new AmazonCloudSearchClient(cred);

	}

	public static void main(String[] args) {
		System.out.println("------fdafasdfasfdasdfa");
		Sdb2CloudSearch s2c = new Sdb2CloudSearch();
		try {
			s2c.getSimpleDBItems();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void getSimpleDBItems() throws FileNotFoundException, JSONException {
		System.out.println("--------------------");
		SelectRequest sr;
		String nextToken = null;
		SelectResult res;
		PrintWriter putCommand = new PrintWriter(new File(
				"./uploadDocuments.sh"));
		PrintWriter putCSCommand = new PrintWriter(new File("./cs-upload.sh"));
		int fileCounter = 1;
		PrintWriter pw = new PrintWriter(new File("./data/" + fileCounter
				+ ".sdf"));
		putCommand.println("#!/bin/bash -x");
		putCommand
				.println("curl -X POST --upload-file data/"
						+ fileCounter
						+ ".sdf  doc-cloudmix-5us7e6vqezfpkb2jxcmni2exoa.us-east-1.cloudsearch.amazonaws.com/2011-02-01/documents/batch --header \"Content-Type:application/json\"");
		putCSCommand.println("#!/bin/bash -x");
		putCSCommand.println("cs-post-sdf -a " + AWS_ACCESS_KEY + " -k "
				+ AWS_SECRET_KEY + " -d " + CS_DOMAIN + " -s data/"
				+ fileCounter + ".sdf ");

		pw.println("[");
		int counter = 0;
		SimpleDateFormat sdf = new SimpleDateFormat(
				"EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
		do {
			sr = new SelectRequest(
					"select text,from_user,from_user_name,id,created_at from "
							+ SDB_DOMAIN
							+ " where id is not null  order by id desc limit 2000");
			if (nextToken != null) {
				sr.setNextToken(nextToken);
			}

			res = this.sdb.select(sr);

			for (Item i : res.getItems()) {
				JSONObject json = new JSONObject();
				try {
					pw.println("{");
					pw.println("  \"type\": \"add\",");
					pw.println("  \"id\": \"" + i.getName() + "\",");
					Calendar ver = Calendar.getInstance();
					pw.println("  \"version\": "
							+ Integer.parseInt(ver.get(Calendar.DAY_OF_YEAR)
									+ "" + ver.get(Calendar.HOUR_OF_DAY) + ""
									+ ver.get(Calendar.MINUTE)) + ",");
					pw.println("  \"lang\": \"en\",");
					pw.println("  \"fields\": {");

					boolean first = true;
					String created_at = "";
					for (Attribute a : i.getAttributes()) {
						if (first) {
							first = false;
							pw.print("    ");
						} else {
							pw.print("    ,");
						}
						pw.println("\""
								+ a.getName()
								+ "\": \""
								+ this.encodeUnicode(a.getValue().replaceAll(
										"\n", "")) + "\"");
						System.out.println(a.getName() + ":"
								+ this.encodeUnicode(a.getValue()));

						if (a.getName().equals("text")) {
							pw.println("    ,\"index_jp\":\""
									+ this.encodeUnicode(this.splitIndex(a
											.getValue().replaceAll("\n", "")))
									+ "\"");
						}

						if (a.getName().equals("created_at")) {
							created_at = a.getValue();
							try {
								Date date = sdf.parse(created_at);
								Calendar c = Calendar.getInstance();
								c.setTime(date);
								pw.println("    ,\"dayofweek\":\""
										+ this.transWeekOfDay(c
												.get(Calendar.DAY_OF_WEEK))
										+ "\"");
								pw.println("    ,\"day\":\""
										+ c.get(Calendar.YEAR) + "/"
										+ (c.get(Calendar.MONTH) + 1) + "/"
										+ c.get(Calendar.DAY_OF_MONTH) + "\"");
								pw.println("    ,\"hour\":\""
										+ c.get(Calendar.HOUR_OF_DAY) + "\"");
								pw.println("    ,\"month\":\""
										+ (c.get(Calendar.MONTH) + 1) + "\"");
							} catch (ParseException e) {
								e.printStackTrace();
								System.exit(1);
							}
						}
					}
					pw.println("  }\n}");
					if (++counter == 2) {
						pw.println("]");
						pw.close();
						fileCounter++;
						pw = new PrintWriter(new File("./data/" + fileCounter
								+ ".sdf"));
						putCSCommand.println("cs-post-sdf -a " + AWS_ACCESS_KEY
								+ " -k " + AWS_SECRET_KEY
								+ " -d cloudmix -s data/" + fileCounter
								+ ".sdf ");

						putCommand
								.println("curl -X POST --upload-file data/"
										+ fileCounter
										+ ".sdf doc-cloudmix-5us7e6vqezfpkb2jxcmni2exoa.us-east-1.cloudsearch.amazonaws.com/2011-02-01/documents/batch --header \"Content-Type:application/json\"");
						pw.println("[");
						counter = 0;
					} else {
						pw.println(",");
					}

				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// System.out.println("getSimpleDBItems" +
			// "=====================================================");
			nextToken = res.getNextToken();

		} while (nextToken != null);
		if (pw != null) {

			pw.println("]");
			pw.close();
		}
		putCSCommand.close();
		putCommand.close();
	}

	private String splitIndex(String value) {
		List<String> result = new ArrayList<String>();
		if (value.length() < 1) {
			return value;
		}

		for (int i = 0; i < value.length(); i++) {
			String tmp = value.substring(i, i + 1);
			result.add(tmp);
		}
		StringBuilder sb = new StringBuilder();
		for (String a : result) {
			sb.append(a);
			sb.append(' ');
		}
		return sb.toString();
	}

	private String transWeekOfDay(int d) {
		String w = "";
		switch (d) {
		case Calendar.MONDAY:
			w = "Mon";
			break;
		case Calendar.TUESDAY:
			w = "Tue";
			break;
		case Calendar.WEDNESDAY:
			w = "Wed";
			break;
		case Calendar.THURSDAY:
			w = "Thu";
			break;
		case Calendar.FRIDAY:
			w = "Fri";
			break;
		case Calendar.SATURDAY:
			w = "Sat";
			break;
		case Calendar.SUNDAY:
			w = "Sun";
			break;
		}
		return w;
	}

	private String encodeUnicode(String value) {
		if (value == null)
			return "";

		char[] charValue = value.toCharArray();

		StringBuilder result = new StringBuilder();
		for (char ch : charValue) {
			if (ch != '_' && !(ch >= '0' && '9' >= ch)
					&& !(ch >= 'a' && 'z' >= ch) && !(ch >= 'A' && 'Z' >= ch)) {
				String unicodeCh = Integer.toHexString((int) ch);

				result.append("\\u");
				for (int i = 0; i < 4 - unicodeCh.length(); i++) {
					result.append("0");
				}
				result.append(unicodeCh);
			} else {
				result.append(ch);
			}
		}
		// return value;
		return result.toString();
	}

	private void createDomain(String domainName) {
		CreateDomainRequest createDomainRequest = new CreateDomainRequest();
		createDomainRequest.withDomainName(domainName);
		this.cs.createDomain(createDomainRequest);
	}

	private void addindex(String domainName) {
		DefineIndexFieldRequest defineIndexFieldRequest = new DefineIndexFieldRequest();
		defineIndexFieldRequest.setDomainName(domainName);
		IndexField i = new IndexField().withIndexFieldName("twitter_id")
				.withIndexFieldType(IndexFieldType.Text);
		this.cs.defineIndexField(defineIndexFieldRequest);
	}

	private void indexDucuments(String domainName) {
		IndexDocumentsRequest indexDocumentsRequest = new IndexDocumentsRequest();
		indexDocumentsRequest.withDomainName(domainName);
		this.cs.indexDocuments(indexDocumentsRequest);
	}
}
