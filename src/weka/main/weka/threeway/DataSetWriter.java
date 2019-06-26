package weka.main.weka.threeway;

import weka.main.weka.SentimentClass;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Created by dmitrykan on 27.04.2014.
 */
public class DataSetWriter {
    public static final String CSV_HEADER = "Phrase,Sentiment";
    BufferedWriter bw;

    public DataSetWriter(String csvFile) throws IOException {
        bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile), "utf8"));
        bw.write(CSV_HEADER);
        bw.write("\n");
    }

    public void writeCSV(DataSetReader.CSVInstance csvInstanceThreeWay, SentimentClass.ThreeWayClazz sentiment) throws IOException {
        try {
            bw.write(String.valueOf(csvInstanceThreeWay.phraseID));
            bw.write(",");
            bw.write(String.valueOf(sentiment.ordinal()));
            bw.write("\n");
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    public void close() throws IOException {
        bw.close();
    }

}
