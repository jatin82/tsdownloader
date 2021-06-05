package com.ts.app;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import com.ts.app.utils.FileLogger;
import com.ts.app.utils.TerminalCMD;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClients;

public class MainApp {

    static String configFile = "/Users/j0s0p9w/work/personal/tx/tsdownloader/config.properties";

//    static String configFile = "config.properties";

    static String tsFiles = "ts.file.path";

    static String referer = "ts.referer";

    static String tsUrl = "ts.url";

    static String baseFileToSavePath = "base.file.save.path";

    static String fileName = "file.name";

    static String tsFileConcatFileName = "ts.file.concat.file.name";

    static String ffmpegCommand = "ffmpeg.command";

    static String postCompleteCommands = "post.complete.command";

    static TerminalCMD terminalCMD;

    static String DYNAMIC_PREFIX = "${";
    static String DYNAMIC_SUFFIX = "}";


    static Properties properties = new Properties();

    public static void main(String[] args) {

        System.out.println("version 0.2");
        if (args.length > 0) {
            configFile = args[0];
        }

        try {
            loadProperties();
            replaceVariables();
            createRequiredDirs(Arrays.asList(new String[]{properties.getProperty(baseFileToSavePath),properties.getProperty(baseFileToSavePath)+"/out"}));
            terminalCMD = new TerminalCMD(true, new FileLogger(properties.getProperty(baseFileToSavePath) + "/log.txt"));
            List<String> tsFiles = download();
            mergeAllTSFiles(tsFiles);

            terminalCMD.run(properties.getProperty(postCompleteCommands));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            terminalCMD.close();
        }
    }

    public static void loadProperties() throws IOException {
        FileInputStream fis = new FileInputStream(configFile);
        properties.load(fis);

    }

    public static void mergeAllTSFiles(List<String> tsFiles) throws IOException, InterruptedException {
        logMergeFiles(tsFiles);
        terminalCMD.run(properties.getProperty(ffmpegCommand));
    }

    public static void logMergeFiles(List<String> tsFiles) throws IOException {
        FileWriter fileWriter = new FileWriter(new File(
                properties.getProperty(baseFileToSavePath) + "/out/" + properties.getProperty(tsFileConcatFileName)));
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        for (String file : tsFiles) {
            bufferedWriter.write(file + "\n");
        }
        bufferedWriter.close();
    }

    public static List<String> download() throws Exception {
        List<String> tsFileNames = parseTsFileNames();

        String baseURL = properties.getProperty(tsUrl);
        String refHeader = properties.getProperty(referer);
        String baseFileToSave = properties.getProperty(baseFileToSavePath);
        for (int i = 0; i < tsFileNames.size(); i++) {
            String file = tsFileNames.get(i);
            String progress = ((float) ((int) (((float) (i + 1) / (float) tsFileNames.size()) * 10000F)) / 100F) + "";

            String fileLocation = baseFileToSave + "/out/" + file;
            if (!isFilePresent(fileLocation)) {
                long start = System.currentTimeMillis();
                downloadTS(baseURL + "/" + file, refHeader, fileLocation);
                long end = System.currentTimeMillis();

                String speed = formatDecimal(calculateSpeed(start, end, new File(fileLocation).getUsableSpace()), 2);
                terminalCMD.log(" [DOWNLOAD] :: " + file + " completed :: " + progress + " % -- " + speed + " kb/s", true);
            } else {
                terminalCMD.log("SKIPPED [DOWNLOAD] :: " + file + " completed :: " + progress + " %", true);
            }
        }
        return tsFileNames.parallelStream().map(t -> {
            return "file '" + baseFileToSave + "/out/" + t + "'";
        }).collect(Collectors.toList());
    }

    public static double calculateSpeed(long start, long end, long spaceOccupied) {
        spaceOccupied /= Math.pow(10, 9);
        long seconds = (end - start) / 1000;
        double speed = (double) spaceOccupied / seconds;
        return speed;
    }

    public static String formatDecimal(double decimalValue, int roundDigit) {
        double roundOff = Math.round(decimalValue * Math.pow(10, roundDigit)) / Math.pow(10, roundDigit);
        return roundOff + "";
    }

    public static boolean isFilePresent(String fileLocation) {
        File file = new File(fileLocation);
        return file.exists();
    }

    public static void downloadTS(String url, String refHeader, String fileLocation) throws Exception {
        HttpClient client = HttpClients.custom().build();
        HttpUriRequest request = RequestBuilder.get()
                .setUri(url)
                .setHeader("referer", refHeader)
                .build();

        HttpResponse response = client.execute(request);

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new Exception("TS file download failed reponsse code:" + response.getStatusLine().getStatusCode() + " URL:" + url);
        }


        HttpEntity entity = response.getEntity();
        entity.getContentLength();
        InputStream stream = entity.getContent();

        FileOutputStream fos = new FileOutputStream(fileLocation);
        int data;
        while ((data = stream.read()) != -1) {
            fos.write(data);
        }
        fos.close();
        System.out.print("SUCCESS");

    }

    public static List<String> parseTsFileNames() throws IOException {
        FileReader reader = new FileReader(properties.getProperty(tsFiles));

        BufferedReader br = new BufferedReader(reader);

        List<String> files = new ArrayList<>();

        String read = null;
        while ((read = br.readLine()) != null) {
            read = read.trim();
            if (read.charAt(0) != '#') {
                files.add(read);
            }
        }

        return files;
    }

    private static void createRequiredDirs(List<String> createDirs) {
        for (String createDir : createDirs) {
            File file = new File(createDir);
            Boolean created = file.mkdir();
            if (!created) {
                System.out.println("dir not created : " + file.getName());
            }
        }
    }

    private static void replaceVariables() {
        Set<String> keys = properties.stringPropertyNames();
        keys.forEach(key -> {
            String value = properties.get(key).toString();
            while (value.contains(DYNAMIC_PREFIX)) {
                int si = value.indexOf(DYNAMIC_PREFIX);
                int ei = value.indexOf(DYNAMIC_SUFFIX) + 1;
                String dynamicKey = value.substring(si + 2, ei - 1);
                value = value.replace(DYNAMIC_PREFIX + dynamicKey + DYNAMIC_SUFFIX, properties.get(dynamicKey).toString());
                properties.setProperty(key, value);
            }
        });
    }


}
