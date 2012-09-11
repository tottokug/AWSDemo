package com.tottokug;

import java.io.File;
import java.util.Iterator;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;

public class Config {

	static XMLConfiguration config;
	static boolean loaded;

	static {
		Config.config = new XMLConfiguration();
		loaded = false;
	}

	public static String getValue(KEY key) {
		return config.getString(key.toString());
	}

	public static void main(String[] args) {
		// load();
		for (KEY key : KEY.values()) {
			config.setFile(new File("./config.xml"));
			Config.config.setProperty(key.toString(), "HERECONFIGVALUE");
			try {
				config.save();
			} catch (ConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static void load() {
		String[] path = { "./config.xml", "~/.awsdemo/config.xml",
				"/etc/awsdemo/config.xml" };
		for (String p : path) {
			File f = new File(p);
			if (f.exists() && f.canRead()) {
				load(f);
				System.out.println(p + "is loaded");
				break;
			}
		}
	}

	private static void load(File file) {
		if (!loaded) {
			try {
				Config.config.load(file);
				loaded = true;
			} catch (ConfigurationException e) {
				e.printStackTrace();
			}
		}
	}

	public static XMLConfiguration getConfig() {
		load();
		return config;
	}

	public static AWSCredentials getAWSCredentials() {
		load();
		String useIamRole = getValue(KEY.USE_IAM_ROLE);
		if (useIamRole != null && useIamRole.matches("^[true|1|yes]$")) {
			return null;
		}
		BasicAWSCredentials cred = new BasicAWSCredentials(
				getValue(KEY.ACCESS_KEY), getValue(KEY.SECRET_KEY));
		return cred;
	}

	public static String getConfigString() {
		load();
		StringBuilder sb = new StringBuilder();
		Iterator<String> keys = config.getKeys();
		while (keys.hasNext()) {
			String key = keys.next();
			sb.append(key + " =>" + config.getString(key));
			System.out.println(key + " =>" + config.getString(key));
		}
		return sb.toString();
	}

	public enum KEY {
		ACCESS_KEY("aws.access_key"), SECRET_KEY("aws.secret_key"), USE_IAM_ROLE(
				"aws.useIAMrole"), SQSCHAT_QUEUE("sqschat.queue.self"), SQSCHAT_FOREIGN_QUEUE(
				"sqschat.queue.foreign"), SQSTweet_QUEUE("sqstweet.queue"), SQSTweet_OAUTH_TOKEN(
				"sqstweet.oauth.token"), SQSTweet_OAUTH_SECRET_TOKEN(
				"sqstweet.oauth.secrettoken"), SQSTweet_CONSUMER_KEY(
				"sqstweet.oauth.consumer_key"), SQSTweet_CONSUMER_SECRET(
				"sqstweet.oauth.consumer_secret"), SQSTweet_HASHTAG(
				"sqstweet.hashtag"), SDBToCloudSearch_CLOUDSEARCH_DOMAIN(
				"sdb2cloudsearch.cloudsearch.domain"), SDBToCloudSearch_SIMPLEDB_DOMAIN(
				"sdb2cloudsearch.simpledb.domain");
		private final String key;

		private KEY(String key) {
			this.key = key;
		}

		public String toString() {
			return this.key;
		}
	}

}