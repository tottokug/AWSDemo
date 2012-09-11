package com.tottokug.awsdemos.sqs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationContext;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.tottokug.Config;

public class SQSTweet {

	public static void main(String[] args) {
		SQSTweet listener = new SQSTweet();
		listener.listen();
	}

	protected String instanceId;

	protected AmazonSQSClient sqs;
	protected String queueUrl;
	private Thread listner;
	private boolean listen;

	public SQSTweet() {
		System.out.println(Config.getConfigString());
		this.sqs = new AmazonSQSClient(Config.getAWSCredentials());
		this.sqs.setEndpoint("sqs.ap-northeast-1.amazonaws.com");
		CreateQueueResult res = this.sqs.createQueue(new CreateQueueRequest(
				Config.getValue(Config.KEY.SQSTweet_QUEUE)));
		this.queueUrl = res.getQueueUrl();
		this.instanceId = this.getInstanceId();
	}

	private String getInstanceId() {
		URL url;
		try {
			url = new URL("http://169.254.169.254/latest/meta-data/instance-id");
			InputStream is = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String buf = "";
			while ((buf = br.readLine()) != null) {
				br.close();
				is.close();
				return buf;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	public void listen() {
		if (listner == null) {
			listner = new Thread(new Runnable() {
				public void run() {
					listen = true;
					while (listen) {
						ReceiveMessageResult res = sqs
								.receiveMessage(new ReceiveMessageRequest(
										queueUrl).withMaxNumberOfMessages(5));
						for (Message m : res.getMessages()) {
							String body = m.getBody();
							System.out.println("SQSからメッセージを受信しました");
							System.out.println(body);
							tweet(body);
							System.out.println("Tweet完了");
							sqs.deleteMessage(new DeleteMessageRequest(
									queueUrl, m.getReceiptHandle()));
						}
						try {
							System.out.println("listen");
							Thread.sleep(1000);
						} catch (InterruptedException e) {
						}
						if (res.getMessages().size() > 0) {
							System.out.print("## > ");
						}
					}
				}
			});
			listner.start();
		}
	}

	private void authTwitter() {
		if (this.twitter == null) {
			Configuration conf = ConfigurationContext.getInstance();
			OAuthAuthorization oauth = new OAuthAuthorization(conf);
			oauth.setOAuthConsumer(
					Config.getValue(Config.KEY.SQSTweet_CONSUMER_KEY),
					Config.getValue(Config.KEY.SQSTweet_CONSUMER_SECRET));
			oauth.setOAuthAccessToken(new AccessToken(Config
					.getValue(Config.KEY.SQSTweet_OAUTH_TOKEN), Config
					.getValue(Config.KEY.SQSTweet_OAUTH_SECRET_TOKEN)));
			TwitterFactory f = new TwitterFactory(conf);
			this.twitter = f.getInstance(oauth);
		}
	}

	private Twitter twitter;

	private void tweet(String message) {
		this.authTwitter();
		try {
			this.twitter.updateStatus(message + " from " + this.instanceId
					+ " " + Config.getValue(Config.KEY.SQSTweet_HASHTAG));
		} catch (TwitterException e) {
		}
	}
}
