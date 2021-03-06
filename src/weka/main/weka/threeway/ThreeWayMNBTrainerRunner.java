package weka.main.weka.threeway;

import weka.main.weka.SentimentClass;

/**
 * Created by dmitrykan on 27.04.2014.
 */
public class ThreeWayMNBTrainerRunner {
    public static void main(String[] args) throws Exception {
        DataSetReader datasetReader = new DataSetReader();
        datasetReader.readKaggleCSV("kaggle/train.tsv");
        DataSetReader.CSVInstance csvInstanceThreeWay;

        String outputModel = "models/three-way-sentiment-mnb.model";

        ThreeWayMNBTrainer threeWayMNBTrainer = new ThreeWayMNBTrainer(outputModel);

        int sentimentPositiveCount = 0;
        int sentimentNegativeCount = 0;
        int sentimentOtherCount = 0;

        System.out.println("Adding training instances");
        int addedNum = 0;
        while ((csvInstanceThreeWay = datasetReader.next()) != null) {
            if (csvInstanceThreeWay.isValidInstance) {
                if (csvInstanceThreeWay.sentiment.equals(SentimentClass.ThreeWayClazz.POSITIVE) && sentimentPositiveCount < 7072) {
                    sentimentPositiveCount++;
                    threeWayMNBTrainer.addTrainingInstance(csvInstanceThreeWay.sentiment, csvInstanceThreeWay.phrase.split("\\s+"));
                    addedNum++;
                }
                else if (csvInstanceThreeWay.sentiment.equals(SentimentClass.ThreeWayClazz.NEGATIVE) && sentimentNegativeCount < 7072) {
                    sentimentNegativeCount++;
                    threeWayMNBTrainer.addTrainingInstance(csvInstanceThreeWay.sentiment, csvInstanceThreeWay.phrase.split("\\s+"));
                    addedNum++;
                }
                else {
                    sentimentOtherCount++;
                }

                if (sentimentPositiveCount >= 7072 && sentimentNegativeCount >= 7072)
                    break;
            }
        }

        datasetReader.close();

        System.out.println("Added " + addedNum + " instances");
        System.out.println("Of which " + sentimentPositiveCount + " positive instances, " +
                            sentimentNegativeCount + " negative instances and " +
                            sentimentOtherCount + " other sentiment instances");

        System.out.println("Training and saving Model");
        threeWayMNBTrainer.trainModel();
        threeWayMNBTrainer.saveModel();

        System.out.println("Testing model");
        threeWayMNBTrainer.testModel();
    }
}
