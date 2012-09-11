package com.tottokug.awsdemos.sqs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.tottokug.App;

/**
 * Hello world!
 * 
 */
public class SQSChat {

	public static void main(String[] args) {
		XMLConfiguration config;
		try {
			config = new XMLConfiguration(new File("./config.xml"));
			if (args.length == 2) {
				SQSChat app = new SQSChat(config.getString("aws.access_key"),
						config.getString("aws.secret_key"), args[0], args[1]);
				app.start();
			}
		} catch (ConfigurationException e) {
			System.out.println("config.xml‚ª‚ ‚è‚Ü‚¹‚ñB");
			e.printStackTrace();
		}
	}

	private final String MESSAGE_SEPARATER = " > ";
	protected AmazonSQSClient sqs;
	protected String awsAccessKey;
	protected String awsSecretKey;
	protected String queueName;
	protected String queueUrl;
	protected String sendQueueName;
	protected String sendQueueUrl;
	private Thread listner;
	private boolean listen;

	public SQSChat(String accessKey, String secretKey, String sendQueue,
			String recieveQueue) {
		this.awsAccessKey = accessKey;
		this.awsSecretKey = secretKey;
		this.sqs = new AmazonSQSClient(new BasicAWSCredentials(
				this.awsAccessKey, this.awsSecretKey));
		this.sqs.setEndpoint("sqs.ap-northeast-1.amazonaws.com");
		this.setQueueName(recieveQueue);
		this.setSendQueueName(sendQueue);
	}

	public void start() {
		this.listen();
		this.input();
	}

	public void setSendQueueName(String queueName) {
		this.sendQueueName = queueName;
		CreateQueueResult res = this.sqs.createQueue(new CreateQueueRequest(
				this.sendQueueName));
		this.sendQueueUrl = res.getQueueUrl();
	}

	public String getSendQueueName() {
		return this.sendQueueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
		CreateQueueResult res = this.sqs.createQueue(new CreateQueueRequest(
				this.queueName));
		this.queueUrl = res.getQueueUrl();
	}

	public String getQueueName() {
		return this.queueName;
	}

	public void stop() {
		this.listen = false;
	}

	public void input() {
		Thread l = new Thread(new Runnable() {
			@Override
			public void run() {
				String str;
				BufferedReader br = new BufferedReader(new InputStreamReader(
						System.in));
				while (true) {
					try {
						System.out.print("## > ");
						str = br.readLine();
						if (str.length() > 0) {
							sqs.sendMessage(new SendMessageRequest(
									sendQueueUrl, sendQueueName
											+ MESSAGE_SEPARATER + str));
						} else {
						}

					} catch (IOException e) {
						System.out.println("“ü—ÍƒGƒ‰[:" + e.getMessage());
					}
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
					}
				}
			}
		});
		l.start();
	}

	public void listen() {
		if (listner == null) {
			listner = new Thread(new Runnable() {
				public void run() {
					listen = true;
					ArrayList<DeleteMessageBatchRequestEntry> bmbre = new ArrayList<DeleteMessageBatchRequestEntry>();
					while (listen) {
						bmbre.clear();
						ReceiveMessageResult res = sqs
								.receiveMessage(new ReceiveMessageRequest(
										queueUrl).withMaxNumberOfMessages(5));
						for (Message m : res.getMessages()) {
							System.out.print("\b\b\b\b\b");
							String body = m.getBody();
							System.out.println(body);
							sqs.deleteMessage(new DeleteMessageRequest(
									queueUrl, m.getReceiptHandle()));
						}
						try {
							Thread.sleep(500);
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
}