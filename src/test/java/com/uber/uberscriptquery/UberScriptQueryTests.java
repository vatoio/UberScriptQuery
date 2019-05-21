package com.uber.uberscriptquery;

import com.uber.uberscriptquery.execution.QueryEngine;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UberScriptQueryTests {
    @Test
    public void sampleQueryWithFile() throws IOException {
//        System.setProperty("hadoop.home.dir", "/Users/minh/hadoop/hadoop-2.6.6/");

        StringBuilder stringBuilder = new StringBuilder();

        Path path = Paths.get("/Users/minh/Desktop/sample-script.txt");
        BufferedReader reader = Files.newBufferedReader(path);

        // Read from the stream
        String currentLine;
        while ((currentLine = reader.readLine()) != null) {
            stringBuilder.append(currentLine);
        }

        String query = stringBuilder.toString();

        System.out.println("Query: " + query);

        // Start Spark Session

//        String master = "spark://master:7077";
        String master = "local[*]";
        String appName = "WhatTheHell";

//        query = "message = 'Hello World'; \n"
//                + "result = select cast(unix_timestamp() as timestamp) as time, '${message}' as message; \n"
//                + "printTable(result);";

        SparkConf sparkConf = new SparkConf()
                .setMaster(master)
                .setAppName(appName);
//                .config("spark.executor.uri", "<your_executor_uri>")
////                .config("spark.executor.memory", "<your_conf>")
//                .set("spark.jars", "/Users/minh/Desktop/vato/UberScriptQuery/target/UberScriptQuery-1.1.01.jar");

        SparkSession sparkSession = SparkSession
                .builder()
                .config(sparkConf).getOrCreate();

        // Run query
        QueryEngine engine = new QueryEngine();
        engine.executeScript(query, null, sparkSession, true);

        sparkSession.stop();
    }
}
