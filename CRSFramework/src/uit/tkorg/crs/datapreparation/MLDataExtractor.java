/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uit.tkorg.crs.datapreparation;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uit.tkorg.crs.model.CoAuthorGraph;
import uit.tkorg.utility.common.TextFileUtility;
import uit.tkorg.crs.model.CoAuthorGraph;

/**
 *
 * @author TinHuynh
 */
public class MLDataExtractor {

    /**
     * Loc ra cac junior researchers ton tai trong G1, nhung chua ton tai trong
     * G0. uc moi bat dau nghien cuu va lan dau xuat hien trong cong dong. Co so
     * bai bao trong trong G1 < n (n=3?) 
     * @p
     *
     * aram G0
     * @param G1
     * @param outFileName
     */
    private static void getListofJuniorFromCoAuthorGraph(CoAuthorGraph G0, CoAuthorGraph G1,
            String outFileName) {
        StringBuffer listOfJunior = new StringBuffer();
        listOfJunior.append("AuthorID \t NumOfPub \n");
        for (int authorID : G1._coAuthorGraph.keySet()) {
            // authorID khong ton tai trong G0
            if (!G0._authorPaper.containsKey(authorID)) {
                // So bai bao trong G1 < 3
                if (G1._authorPaper.get(authorID).size() < 3) {
                    listOfJunior.append(authorID + "\t" + G1._authorPaper.get(authorID).size() + "\n");
                }
            }
        }
        TextFileUtility.writeTextFile(outFileName, listOfJunior.toString());
    }

    /**
     *
     * @param inputFileName
     * @return
     */
    private static HashMap<Integer, Integer> loadAuthorIDFromTextFile(String inputFileName) {
        HashMap<Integer, Integer> listOfAuthorHM = new HashMap<>();
        try {
            FileInputStream fis = new FileInputStream(inputFileName);
            Reader reader = new InputStreamReader(fis, "UTF8");
            BufferedReader bufferReader = new BufferedReader(reader);
            bufferReader.readLine(); // skip the first line
            String line = null;
            String[] tokens;
            while ((line = bufferReader.readLine()) != null) {
                tokens = line.split("\t");
                if (tokens.length > 2) {
                    continue;
                }
                int authorID = Integer.parseInt(tokens[0]);
                int numOfPub = Integer.parseInt(tokens[1]);
                if (!listOfAuthorHM.containsKey(authorID)) {
                    listOfAuthorHM.put(authorID, numOfPub);
                }
            }
            bufferReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return listOfAuthorHM;
    }

    /**
     * loading authorID of junior researchers from the PostiveSample file
     *
     * @param positiveSampleFile
     * @return
     */
    private static HashMap<Integer, Integer> loadAuthorIDFromPositiveSample(String positiveSampleFile) {
        final String REGEX = "\\W+";
        Pattern p = Pattern.compile(REGEX);

        HashMap<Integer, Integer> authorIDInPositiveSampleHM = new HashMap<>();
        try {
            FileInputStream fis = new FileInputStream(positiveSampleFile);
            Reader reader = new InputStreamReader(fis, "UTF8");
            BufferedReader bufferReader = new BufferedReader(reader);
            bufferReader.readLine(); // skip the first line
            String line = null;
            String[] tokens;
            while ((line = bufferReader.readLine()) != null) {
                String[] elements = p.split(line.trim());

                if (elements.length > 3) {
                    continue;
                }
                int authorID = Integer.parseInt(elements[1]);
                int numOfPub = Integer.parseInt(elements[2]);
                if (!authorIDInPositiveSampleHM.containsKey(authorID)) {
                    authorIDInPositiveSampleHM.put(authorID, numOfPub);
                }
            }
            bufferReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return authorIDInPositiveSampleHM;
    }

    /**
     * Loc ra nhung link(+) cho danh sach AuthorID: ton tai trong G2, nhung
     * khong ton tai trong G1 --> luu xuong file outFileName (2 dinh deu ton tai
     * trong G1 va G2)
     *
     * @param listOfAuthorID
     * @param G0
     * @param G1
     * @param outFileName
     */
    private static int getPositiveSampleFromCoAuthorGraph(HashMap<Integer, Integer> listOfAuthors, CoAuthorGraph G1, CoAuthorGraph G2,
            String outFileName) {
        int numOfPositiveSample = 0;
        StringBuffer strBuffer = new StringBuffer();
        strBuffer.append("(AuthorID, CoAuthorID) \n");
        for (int authorID : listOfAuthors.keySet()) {
            if (G2._coAuthorGraph.containsKey(authorID)) {
                for (int coAuthorID : G2._coAuthorGraph.get(authorID).keySet()) {
                    if (G1._coAuthorGraph.containsKey(authorID) && G1._coAuthorGraph.containsKey(coAuthorID)) {
                        if (!CoAuthorGraph.isLinkExistInCoAuthorGraph(G1._coAuthorGraph, authorID, coAuthorID)) {
                            strBuffer.append("(" + authorID + "," + coAuthorID + ")\n");
                            numOfPositiveSample++;
                        }
                    }
                }
            }
        }
        TextFileUtility.writeTextFile(outFileName, strBuffer.toString());
        return numOfPositiveSample;
    }

    /**
     *
     * @param listOfAuthors
     * @param numOfNegativeSample
     * @param G1
     * @param G2
     * @param outFileName
     */
    private static double getRandomlyNegativeSampleFromCoAuthorGraph(HashMap<Integer, Integer> listOfAuthors,
            int numOfNegativeSample,
            CoAuthorGraph G1, CoAuthorGraph G2,
            String outFileName) {

        double numOfSelectedNegativeSample = 0;
        StringBuffer strBuffer = new StringBuffer();
        strBuffer.append("(AuthorID, ID-NoneCoAuthor)\n");

        int numOfNegativeSamplePerOneAuthor = numOfNegativeSample / listOfAuthors.size();

        for (int authorID : listOfAuthors.keySet()) {
            int count = 0;
            for (int anotherAuthorID : G2._coAuthorGraph.keySet()) {
                if (count < numOfNegativeSamplePerOneAuthor) {
                    if ((authorID != anotherAuthorID) && G1._coAuthorGraph.containsKey(authorID)
                            && G1._coAuthorGraph.containsKey(anotherAuthorID)
                            && G2._coAuthorGraph.containsKey(authorID)) {
                        if (!CoAuthorGraph.isLinkExistInCoAuthorGraph(G2._coAuthorGraph, authorID, anotherAuthorID)
                                && !CoAuthorGraph.isLinkExistInCoAuthorGraph(G1._coAuthorGraph, authorID, anotherAuthorID)) {
                            strBuffer.append("(" + authorID + "," + anotherAuthorID + ")\n");
                            count++;
                            numOfSelectedNegativeSample++;
                        }
                    }
                } else {
                    break;
                }
            }

        }
        TextFileUtility.writeTextFile(outFileName, strBuffer.toString());
        return numOfSelectedNegativeSample;
    }

    /**
     *
     * @param listOfAuthors
     * @param numOfNegativeSample
     * @param G1
     * @param G2
     * @param outFileName
     */
    private static double getAllNegativeSampleFromCoAuthorGraph(HashMap<Integer, Integer> listOfAuthors,
            CoAuthorGraph G1, CoAuthorGraph G2,
            String outFileName) {

        double numOfSelectedNegativeSample = 0;
        StringBuffer strBuffer = new StringBuffer();
        strBuffer.append("(AuthorID, ID-NoneCoAuthor)\n");
        for (int authorID : listOfAuthors.keySet()) {
            for (int anotherAuthorID : G2._coAuthorGraph.keySet()) {
                if ((authorID != anotherAuthorID) && G1._coAuthorGraph.containsKey(authorID)
                        && G1._coAuthorGraph.containsKey(anotherAuthorID)
                        && G2._coAuthorGraph.containsKey(authorID)) {
                    if (!CoAuthorGraph.isLinkExistInCoAuthorGraph(G2._coAuthorGraph, authorID, anotherAuthorID)
                            && !CoAuthorGraph.isLinkExistInCoAuthorGraph(G1._coAuthorGraph, authorID, anotherAuthorID)) {
                        strBuffer.append("(" + authorID + "," + anotherAuthorID + ")\n");
                        numOfSelectedNegativeSample++;
                    }
                }

            }

        }
        TextFileUtility.writeTextFile(outFileName, strBuffer.toString());
        return numOfSelectedNegativeSample;
    }

    private static void getTrainingData(String authorID_paperID_T0_FileName, String paperID_Year_T0_FileName,
            String authorID_paperID_T1_FileName, String paperID_Year_T1_FileName,
            String authorID_paperID_T2_FileName, String paperID_Year_T2_FileName,
            String outFile_JuniorIDList, String outFile_PositiveSample, String outFile_NegativeSample) {
        
        //<editor-fold defaultstate="collapsed" desc="For SampleData">
        // G0 < T1 (before 2003)
//        CoAuthorGraph G0 = new CoAuthorGraph("/1.CRS-ExperimetalData/SampleData/AuthorID_PaperID_Before_2003.txt",
//                "/1.CRS-ExperimetalData/SampleData/PaperID_Year_Before_2003.txt");
//        
//        boolean isExist = G0.isLinkExistInCoAuthorGraph(G0._coAuthorGraph, 4, 6);
//        System.out.println("Link (4, 6) in G1 is " + isExist);
//        isExist = G0.isLinkExistInCoAuthorGraph(G0._coAuthorGraph, 5, 6);
//        System.out.println("Link (5, 6) in G1 is " + isExist);
//        isExist = G0.isLinkExistInCoAuthorGraph(G0._coAuthorGraph, 4, 5);
//        System.out.println("Link (4, 5) in G1 is " + isExist);
//        System.out.println("Weight of (1, 2) in G0 is " + G0._coAuthorGraph.get(1).get(2));
//        System.out.println("Building G0 ... DONE.");
//        
//        // G1 in T1
//        CoAuthorGraph G1 = new CoAuthorGraph("/1.CRS-ExperimetalData/SampleData/AuthorID_PaperID_2003_2005.txt",
//                "/1.CRS-ExperimetalData/SampleData/PaperID_Year_2003_2005.txt", 2003, 2005);
//        
//        isExist = G1.isLinkExistInCoAuthorGraph(G1._coAuthorGraph, 4, 6);
//        System.out.println("Link (4, 6) in G1 is " + isExist);
//        isExist = G1.isLinkExistInCoAuthorGraph(G1._coAuthorGraph, 5, 6);
//        System.out.println("Link (5, 6) in G1 is " + isExist);
//        isExist = G1.isLinkExistInCoAuthorGraph(G1._coAuthorGraph, 4, 5);
//        System.out.println("Link (4, 5) in G1 is " + isExist);
//        System.out.println("Weight of (1, 2) in G1 is " + G1._coAuthorGraph.get(1).get(2));
//        System.out.println("Building G1 ... DONE.");
//        
//        // Loc ra cac junior researchers ton tai trong G1, nhung chua ton tai trong G0.
//        // Tuc moi bat dau nghien cuu va lan dau xuat hien trong cong dong. So bai bao trong trong G1 < 3
//        getListofJuniorFromCoAuthorGraph(G0, G1, "/1.CRS-ExperimetalData/SampleData/JuniorIDList.txt");
//        
//        // G2 in T2
//        CoAuthorGraph G2 = new CoAuthorGraph("/1.CRS-ExperimetalData/SampleData/AuthorID_PaperID_2006_2008.txt",
//                "/1.CRS-ExperimetalData/SampleData/PaperID_Year_2006_2008.txt", 2006, 2008);
//        isExist = G2.isLinkExistInCoAuthorGraph(G2._coAuthorGraph, 4, 6);
//        System.out.println("Link (4, 6) in G2 is " + isExist);
//        isExist = G2.isLinkExistInCoAuthorGraph(G2._coAuthorGraph, 5, 6);
//        System.out.println("Link (5, 6) in G2 is " + isExist);
//        isExist = G2.isLinkExistInCoAuthorGraph(G2._coAuthorGraph, 4, 5);
//        System.out.println("Link (4, 5) in G2 is " + isExist);
//        System.out.println("Weight of (1, 2) in G2 is " + G2._coAuthorGraph.get(1).get(2));
//        System.out.println("Building G2 ..... DONE.");
//        
//        // Loc ra cac link(+) xuat hien trong G2 cua cac junior (xuat hien trong G1)
//        HashMap<Integer, Integer> juniorAuthorIDHM = loadAuthorIDFromTextFile("/1.CRS-ExperimetalData/SampleData/JuniorIDList.txt");
//        int numOfPositiveSample = getPositiveSampleFromCoAuthorGraph(juniorAuthorIDHM, G1, G2, "/1.CRS-ExperimetalData/SampleData/PositiveSamples.txt");
//        System.out.println("So mau (+):" + numOfPositiveSample);
//        
//        // Chon ngau nhien cac cap author khong link trong G2 va G1 cho cac junior (xuat hien trong G1)
//        // So luong mau (-) muon chon = Tat ca mau am trong G2 cua nhung junior co mau (+)
//        // Chi chon mau am (-) trong G2 cho nhung junior ma co xuat hien mau (+) trong G2. Khong can xet nhung junior ma khong co mau (+)???
//        HashMap<Integer, Integer> authorsInPositveSample = loadAuthorIDFromPositiveSample("/1.CRS-ExperimetalData/SampleData/PositiveSamples.txt");
//        int numOfSelectedNagativeSample = getAllNegativeSampleFromCoAuthorGraph(authorsInPositveSample, G1, G2, "/1.CRS-ExperimetalData/SampleData/NegativeSamples.txt");
//        System.out.println("So mau (-):" + numOfSelectedNagativeSample);
        //</editor-fold>
        
        //<editor-fold defaultstate="collapsed" desc="For The RealData">
        // G0 in T0
        CoAuthorGraph G0 = new CoAuthorGraph(authorID_paperID_T0_FileName, paperID_Year_T0_FileName);
        // G1 in T1
        CoAuthorGraph G1 = new CoAuthorGraph(authorID_paperID_T1_FileName, paperID_Year_T1_FileName, 2001, 2003);
        // Loc ra cac junior researchers ton tai trong G1, nhung chua ton tai trong G0.
        // Tuc moi bat dau nghien cuu va lan dau xuat hien trong cong dong. So bai bao trong trong G1 < 3
        getListofJuniorFromCoAuthorGraph(G0, G1, outFile_JuniorIDList);
        
        // G2 in T2
        CoAuthorGraph G2 = new CoAuthorGraph(authorID_paperID_T2_FileName, paperID_Year_T2_FileName, 2004, 2006);
        
        // Loc ra cac link(+) xuat hien trong G2 cua cac junior (xuat hien trong G1)
        HashMap<Integer, Integer> juniorAuthorIDHM = loadAuthorIDFromTextFile(outFile_JuniorIDList);        
        
        double numOfPositiveSample = getPositiveSampleFromCoAuthorGraph(juniorAuthorIDHM, G1, G2, outFile_PositiveSample);
        System.out.println("So mau (+):" + numOfPositiveSample);
        
        // Chon ngau nhien cac cap author khong link trong G2 va G1 cho cac junior (xuat hien trong G1)
        // So luong mau (-) muon chon = Tat ca mau am trong G2 cua nhung junior co mau (+)
        // Chi chon mau am (-) trong G2 cho nhung junior ma co xuat hien mau (+) trong G2. Khong can xet nhung junior ma khong co mau (+)???
        HashMap<Integer, Integer> authorsInPositveSample = loadAuthorIDFromPositiveSample(outFile_PositiveSample);
        System.out.println("So Junior Researchers lien quan mau (+):" + authorsInPositveSample.size());
        System.out.println("Tong so Researchers trong G2:" + G2._coAuthorGraph.size());
        
        double numOfSelectedNagativeSample = getAllNegativeSampleFromCoAuthorGraph(authorsInPositveSample, G1, G2, outFile_NegativeSample);
        System.out.println("So mau (-):" + numOfSelectedNagativeSample);
        //</editor-fold>
 
    }
    
    private static void getTestingData() {
        
    }
    
    public static void main(String args[]) {
        getTrainingData("/1.CRS-ExperimetalData/TrainingData/AuthorID_PaperID_Before_2001.txt", 
                        "/1.CRS-ExperimetalData/TrainingData/PaperID_Year_Before_2001.txt",
                        "/1.CRS-ExperimetalData/TrainingData/AuthorID_PaperID_2001_2003.txt", 
                        "/1.CRS-ExperimetalData/TrainingData/PaperID_Year_2001_2003.txt",
                        "/1.CRS-ExperimetalData/TrainingData/AuthorID_PaperID_2004_2006.txt", 
                        "/1.CRS-ExperimetalData/TrainingData/PaperID_Year_2004_2006.txt",
                        "/1.CRS-ExperimetalData/TrainingData/JuniorIDList.txt", 
                        "/1.CRS-ExperimetalData/TrainingData/PositiveSamples.txt", 
                        "/1.CRS-ExperimetalData/TrainingData/NegativeSamples.txt");
        
        getTestingData();
        System.out.println("DONE");
    }
}