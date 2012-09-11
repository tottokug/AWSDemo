package com.tottokug.awsdemos.cloudwatch;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.tottokug.Config;

public class Watch {

	AmazonCloudWatchClient cw;

	public void a() {
		cw = new AmazonCloudWatchClient(Config.getAWSCredentials());
		cw.getMetricStatistics(new GetMetricStatisticsRequest());
	}
	
	

}
