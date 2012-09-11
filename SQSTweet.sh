#!/bin/bash

# -Dexec.args="自分のID 相手のID"
mvn exec:java -Dexec.mainClass=com.tottokug.awsdemos.sqs.SQSTweet
