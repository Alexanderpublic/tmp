package weka.main.weka.threeway;

import weka.main.weka.SentimentClass;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by Created by dmitrykan on 27.04.2014.
 *
 * Reads a TAB separated file of the format:
 *
 * PhraseId	SentenceId	Phrase	Sentiment
 *
 */
public class DataSetReader {

    private String line;
    private CSVInstance csvInstance;
    private int step = 0;

    private BufferedReader br;

    private int showStatsAt = 1000;

    void readKaggleCSV(String csvFile) throws IOException {
        br = new BufferedReader(new FileReader(csvFile));

        line = br.readLine();

        if (line != null) {
            if (line.startsWith("PhraseId")) {
                line = br.readLine();
            }

            if (line != null) {
                extractInstance();
            }
        }
    }

    private void extractInstance() {
        String[] attrs = line.split("\t");

        if (csvInstance == null) {
            csvInstance = new CSVInstance();
        }
        csvInstance.phraseID = Integer.valueOf(attrs[0]);
        csvInstance.phrase = attrs[1];
        // there is additionally sentiment tag for training data
        if (attrs.length > 3) {
            Integer sentimentOrdinal = Integer.valueOf(attrs[2]);

            if (sentimentOrdinal <= 1) {
                csvInstance.sentiment = SentimentClass.ThreeWayClazz.values()[sentimentOrdinal];
                csvInstance.isValidInstance = true;
            } else {
                // can't process the instance, because the sentiment ordinal is out of the acceptable range of two classes
                csvInstance.isValidInstance = false;
            }
        }
    }

    CSVInstance next() {
        if (step == 0) {
            step++;
            return csvInstance;
        }

        if (step % showStatsAt == 0) {
            System.out.println("Processed instances: " + step);
        }

        try {
            line = br.readLine();
            if (line != null) {
                extractInstance();
            } else {
                return null;
            }
            step++;
            return csvInstance;
        } catch (IOException e) {
            return null;
        }
    }

    void close() {
        try {
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class CSVInstance {
        public int phraseID;
        public String phrase;
        SentimentClass.ThreeWayClazz sentiment;
        public boolean isValidInstance;

        @Override
        public String toString() {
            return "CSVInstance{" +
                    "phraseID=" + phraseID +
                    ", phrase='" + phrase + '\'' +
                    ", sentiment=" + sentiment +
                    '}';
        }
    }
}
