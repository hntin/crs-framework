/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uit.tkorg.crs.experiment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import uit.tkorg.crs.graph.Graph;
import uit.tkorg.utility.TextFileUtility;

/**
 *
 * @author TinHuynh
 */
public class ExperimentSetting {

    private Graph _graph = Graph.getInstance();
    private ArrayList<Integer> _listAuthorRandom;
    private String _file_TraingAuthorIDPaperID;
    private String _file_TraingPaperID_Year;
    private String _file_NF_AuthorIDPaperID;
    private String _file_FF_AuthorIDPaperID;
    private String _fileSaveTo;
    private int _numberOfAuthor;

    public static enum GeneratingOption {

        NONE,
        LOWEST,
        HIGHEST,
        LOWMIDHIGH,
        POTENTIALLINK,
    }

    public ExperimentSetting(int numberOfAuthor, String file_TraingAuthorIDPaperID, String file_TraingPaperID_Year,
            String file_NF_AuthorIDPaperID, String file_FF_AuthorIDPaperID, String file_SaveTo) {
        _numberOfAuthor = numberOfAuthor;
        _file_TraingAuthorIDPaperID = file_TraingAuthorIDPaperID;
        _file_TraingPaperID_Year = file_TraingPaperID_Year;
        _file_NF_AuthorIDPaperID = file_NF_AuthorIDPaperID;
        _file_FF_AuthorIDPaperID = file_FF_AuthorIDPaperID;
        _fileSaveTo = file_SaveTo;
    }

    public void generateAuthorList(GeneratingOption generatingOption) {
        _listAuthorRandom = new ArrayList<>();
        try {
            _graph.LoadTrainingData(_file_TraingAuthorIDPaperID, _file_TraingPaperID_Year);
            _graph.LoadTestingData(_file_NF_AuthorIDPaperID, _file_FF_AuthorIDPaperID);
            _graph.BuidCoAuthorGraph();
            _graph.BuildingRSSGraph();

            // <AuthorID, AuthorDegree>
            HashMap<Integer, Integer> authorDegree = new HashMap<>();
            for (int authorId : _graph.rssGraph.keySet()) {
                authorDegree.put(authorId, _graph.rssGraph.get(authorId).size());
            }

            // <Degree X, Number of Authors who have the Degree X>
            HashMap<Integer, Integer> degreeCounter = new HashMap<>();
            for (int authorId : authorDegree.keySet()) {
                Integer counter = degreeCounter.get(authorDegree.get(authorId));
                if (counter == null) {
                    counter = 0;
                }
                counter++;
                degreeCounter.put(authorDegree.get(authorId), counter);
            }

            // degreeArray index
            int[] degreeArray = new int[degreeCounter.keySet().size()];
            int index = 0;
            for (int degree : degreeCounter.keySet()) {
                degreeArray[index] = degree;
                index++;
            }

            //sort degree Array
            for (int i = 0; i < degreeArray.length - 1; i++) {
                for (int j = i + 1; j < degreeArray.length; j++) {
                    if (degreeArray[i] > degreeArray[j]) {
                        int temp = degreeArray[i];
                        degreeArray[i] = degreeArray[j];
                        degreeArray[j] = temp;
                    }
                }
            }

            // Clustering into 3 sets author of their degree (Low Degree Group, Medium Degree Group, High Degree Group)
            int numberOfDegree, numberOfLowDegree, numberOfMidDegree, numberOfHighDegree;
            numberOfDegree = numberOfLowDegree = numberOfMidDegree = numberOfHighDegree = (int) degreeArray.length / 3;
            if (degreeArray.length - numberOfDegree * 3 == 1) {
                numberOfLowDegree += 1;
            } else if (degreeArray.length - numberOfDegree * 3 == 2) {
                numberOfLowDegree += 1;
                numberOfMidDegree += 1;
            }

            int maxLowDegree = degreeArray[numberOfLowDegree - 1];
            int maxMidDegree = degreeArray[numberOfLowDegree + numberOfMidDegree - 1];
            int maxHighDegree = degreeArray[numberOfLowDegree + numberOfMidDegree + numberOfHighDegree - 1];

            ArrayList<Integer> listAuthorIdInLow = new ArrayList<>();
            ArrayList<Integer> listAuthorIdInMid = new ArrayList<>();
            ArrayList<Integer> listAuthorIdInHigh = new ArrayList<>();

            ArrayList<Integer> listAuthorIdInLowForStatic = new ArrayList<>();
            ArrayList<Integer> listAuthorIdInMidForStatic = new ArrayList<>();
            ArrayList<Integer> listAuthorIdInHighForStatic = new ArrayList<>();

            // Just get authors who really exist in the future network for the experiment
            HashSet<Integer> listAuthorNearTesting = _graph.GetAllAuthorNearTest();
            HashSet<Integer> listAuthorFarTesting = _graph.GetAllAuthorFarTest();

            for (int authorId : authorDegree.keySet()) {
                if (authorDegree.get(authorId) < maxLowDegree) {
                    listAuthorIdInLowForStatic.add(authorId);
                    if (listAuthorNearTesting.contains(authorId)
                            && listAuthorFarTesting.contains(authorId)) {
                        listAuthorIdInLow.add(authorId);
                    }
                } else if (authorDegree.get(authorId) < maxMidDegree) {
                    listAuthorIdInMidForStatic.add(authorId);
                    if (listAuthorNearTesting.contains(authorId)
                            && listAuthorFarTesting.contains(authorId)) {
                        listAuthorIdInMid.add(authorId);
                    }
                } else {
                    listAuthorIdInHighForStatic.add(authorId);
                    if (listAuthorNearTesting.contains(authorId)
                            && listAuthorFarTesting.contains(authorId)) {
                        listAuthorIdInHigh.add(authorId);
                    }
                }
            }
            // End of Clustering into 3 sets author of their degree

            // Genertaing random authors from the degree groups 
            if (generatingOption == GeneratingOption.LOWEST) {
                _listAuthorRandom.addAll(randomAuthorIdFromList(_numberOfAuthor, listAuthorIdInLow));
            } else if (generatingOption == GeneratingOption.HIGHEST) {
                _listAuthorRandom.addAll(randomAuthorIdFromList(_numberOfAuthor, listAuthorIdInHigh));
            } else if (generatingOption == GeneratingOption.LOWMIDHIGH) {
                if (_numberOfAuthor % 3 == 1) {
                    _listAuthorRandom.addAll(randomAuthorIdFromList(_numberOfAuthor / 3 + 1, listAuthorIdInLow));
                } else {
                    if (_numberOfAuthor % 3 == 2) {
                        _listAuthorRandom.addAll(randomAuthorIdFromList(_numberOfAuthor / 3 + 2, listAuthorIdInLow));
                    } else {
                        _listAuthorRandom.addAll(randomAuthorIdFromList(_numberOfAuthor / 3, listAuthorIdInLow));
                    }
                }
                _listAuthorRandom.addAll(randomAuthorIdFromList(_numberOfAuthor / 3, listAuthorIdInMid));
                _listAuthorRandom.addAll(randomAuthorIdFromList(_numberOfAuthor / 3, listAuthorIdInHigh));
            }

            // Generating the list of authors for the potential link option
            // Get the list of authors who exist in future and the past, but their links are in future, not in the past.
            // authorID1 & authorID2 have a link in the near future net
            for (int authorID1 : _graph.nearTestingData.keySet()) {
                if (_graph.rssGraph.containsKey(authorID1) && _graph.farTestingData.containsKey(authorID1)) {
                    ArrayList<Integer> listCoAuthor = _graph.nearTestingData.get(authorID1);
                    for (int i = 0; i < listCoAuthor.size(); i++) {
                        int authorID2 = listCoAuthor.get(i);
                        if (_graph.rssGraph.containsKey(authorID2) && _graph.farTestingData.containsKey(authorID2)) {
                            if (!_graph.isLinkExistInRSSGraph(_graph.rssGraph, authorID1, authorID2)) {

                                boolean foundAuthorID1 = false;
                                boolean foundAuthorID2 = false;
                                if (_listAuthorRandom != null) {
                                    for (int j = 0; j < _listAuthorRandom.size(); j++) {
                                        int currentID = _listAuthorRandom.get(j);
                                        if (currentID == authorID1) {
                                            foundAuthorID1 = true;
                                        }
                                        if (currentID == authorID2) {
                                            foundAuthorID2 = true;
                                        }
                                    }
                                    if (!foundAuthorID1) {
                                        _listAuthorRandom.add(authorID1);
                                    }
                                    if (!foundAuthorID2) {
                                        _listAuthorRandom.add(authorID2);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            while (_listAuthorRandom.size() > _numberOfAuthor) {
                _listAuthorRandom.remove(0);
            }
            // End of Generating the list of authors for the potential link option

            // Get Authors who exist in the training network, but they have no any Connection/Collaboration yet. 
            ArrayList listAuthorWithoutAnyLink = new ArrayList();
            StringBuffer buffAuthorWithoutAnyLink = new StringBuffer();
            String parentDir = (new File(_fileSaveTo)).getParent();
            buffAuthorWithoutAnyLink.append("Authors have no any links \n");
            for (int authorID1 : _graph.rssGraph.keySet()) {
                // authorID not any connection before
                if (_graph.rssGraph.get(authorID1).size() == 0) {
                    // If authorID has any link in the future?
                    boolean hasNewLink = false;
                    for (int authorID2 : _graph.nearTestingData.keySet()) {
                        if (((authorID2 == authorID1) && _graph.nearTestingData.get(authorID2).size() > 0)
                                || (_graph.nearTestingData.get(authorID2) != null
                                && _graph.nearTestingData.get(authorID2).contains(authorID1))) {
                            hasNewLink = true;
                            break;
                        }
                    }

                    if (hasNewLink) {
                        listAuthorWithoutAnyLink.add(authorID1);
                        buffAuthorWithoutAnyLink.append(authorID1 + "\n");
                    }
                }
            }
            
            TextFileUtility.writeTextFile(parentDir + "\\ListAuthorNoAnyLink.txt", buffAuthorWithoutAnyLink.toString());
            // End of getting authors who exist in the training network, 
            // but they have no any Connection/Collaboration yet. 

            StringBuffer listAuthorBuff = new StringBuffer();
            listAuthorBuff.append("AuthorID" + "\n");
            for (int authorId : _listAuthorRandom) {
                listAuthorBuff.append(authorId + "\n");
            }
            TextFileUtility.writeTextFile(_fileSaveTo, listAuthorBuff.toString());

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private ArrayList<Integer> randomAuthorIdFromList(int numberOfAuthorId, ArrayList<Integer> listId) {
        ArrayList<Integer> result = new ArrayList<>();
        if (listId.size() < numberOfAuthorId) {
            numberOfAuthorId = listId.size();
        }

        int counter = 0;
        Random rd = new Random();
        while (counter < numberOfAuthorId) {
            int index = rd.nextInt(listId.size() - 1);
            result.add(listId.get(index));
            listId.remove(index);
            counter++;
        }

        return result;
    }
//    public static void main(String args[]) {
//        System.out.println("START");
//        ExperimentSetting experimentSetting = new ExperimentSetting(
//                10,
//                "C:\\CRS-Experiment\\Sampledata\\[Training]AuthorId_PaperID.txt",
//                "C:\\CRS-Experiment\\Sampledata\\[Training]PaperID_Year.txt",
//                "C:\\CRS-Experiment\\Sampledata\\[NearTesting]AuthorId_PaperID.txt",
//                "C:\\CRS-Experiment\\Sampledata\\[FarTesting]AuthorId_PaperID.txt",
//                "C:\\CRS-Experiment\\Sampledata\\Output\\PotentialAuthorList.txt");
//        
//        experimentSetting.generateAuthorList(GeneratingOption.POTENTIALLINK);
//        System.out.println("END");
//    }
}