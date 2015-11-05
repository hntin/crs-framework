/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uit.tkorg.crs.datapreparation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import uit.tkorg.crs.common.Pair;
import uit.tkorg.crs.model.CitationGraph;
import uit.tkorg.crs.model.Sample;

/**
 *
 * @author TinHuynh
 */
public class AuthorRankComputation extends FeatureComputation {

    private final CitationGraph _citedGraph;

    public AuthorRankComputation(String postiveSampleFile, String negativeSampleFile,
            String inputFile_AuthorID_PaperID, String inputFile_PaperID_Year_RefID) {
        this._positiveSample = Sample.readSampleFile(postiveSampleFile);
        this._negativeSample = Sample.readSampleFile(negativeSampleFile);
        _citedGraph = new CitationGraph(inputFile_AuthorID_PaperID, inputFile_PaperID_Year_RefID);
    }

    @Override
    public void computeFeatureValues(String outputFile, int typeOfSample) throws IOException {
        //citedGraph.calculateImportantRate("C:\\CRS-Experiment\\MAS\\ColdStart\\Input\\Input1\\ImportantRate\\pagerank.txt");
        HashMap<Integer, Float> authorID_PageRank_HM = _citedGraph.calculateImportantRate();
        ArrayList<Pair> pairs = null;

        if (typeOfSample == 1) {
            pairs = this._positiveSample.getPairOfAuthor();
        } else {
            pairs = this._negativeSample.getPairOfAuthor();
        }

        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)))) {
            out.println("AuthorID, SecondAuthorID, RankValue-SecondAuthor");
            out.flush();
            for (Pair p : pairs) {
                int firstAuthorID = p.getFirst();
                int secondAuthorID = p.getSecond();
                float authorRankValue = authorID_PageRank_HM.get(secondAuthorID);
                out.println("(" + firstAuthorID + "," + secondAuthorID + ")\t" + authorRankValue);
                out.flush();
            }
        }
    }

    public static void main(String args[]) {

    }
}
