package com.tottokug.dynamodb.mediaserver;

import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;

public class Player {

	AmazonDynamoDBClient dynamo;

	public Player() {
		this.dynamo = new AmazonDynamoDBClient();
	}
}
