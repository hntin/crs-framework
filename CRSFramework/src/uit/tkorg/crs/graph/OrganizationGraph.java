package uit.tkorg.crs.graph;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author TinHuynh
 */
public class OrganizationGraph {

    private static OrganizationGraph _instance;

    public static OrganizationGraph getInstance() {
        if (_instance == null) {
            _instance = new OrganizationGraph();
        }
        return _instance;
    }
    
    public HashMap<Integer, HashMap<Integer, Float>> _rssORGGraph;
    public HashMap<Integer, HashMap<Integer, Integer>> _collaborativeOrgGraph;
    public HashMap<Integer, Integer> _authorID_OrgID;
    public HashMap<Integer, ArrayList<Integer>> _paperOrganization;
    public HashMap<Integer, ArrayList<Integer>> _organizationPaper;
    
    public OrganizationGraph() {
        _collaborativeOrgGraph = new HashMap<>();
        _rssORGGraph = new HashMap<>();
    }

    public void load_AuthorID_PaperID_OrgID(String file_AuthorID_PubID_OrgID) {
        try {
            _paperOrganization = new HashMap<>();
            _organizationPaper = new HashMap<>();
            FileInputStream fis = new FileInputStream(file_AuthorID_PubID_OrgID);
            Reader reader = new InputStreamReader(fis, "UTF8");
            BufferedReader bufferReader = new BufferedReader(reader);
            // Skip the first line (the header)
            bufferReader.readLine();
            String line = null;
            String[] tokens;
            int authorId;
            int paperId;
            int orgId;
            while ((line = bufferReader.readLine()) != null && !line.equals("")) {
                tokens = line.split(",");
                if (tokens.length > 0 && tokens[0] != null && !tokens[0].equals("")) {
                    authorId = Integer.parseInt(tokens[0]);
                } else {
                    authorId = -1;
                }
                if (tokens.length > 1 && tokens[1] != null && !tokens[1].equals("")) {
                    paperId = Integer.parseInt(tokens[1]);
                } else {
                    paperId = -1;
                }
                if (tokens.length > 2 && tokens[2] != null && !tokens[2].equals("")) {
                    orgId = Integer.parseInt(tokens[2]);
                } else {
                    orgId = -1;
                }

                ArrayList<Integer> listPaper = _organizationPaper.get(orgId);
                if (listPaper == null) {
                    listPaper = new ArrayList<>();
                }
                listPaper.add(paperId);
                _organizationPaper.put(orgId, listPaper);

                ArrayList<Integer> listOrg = _paperOrganization.get(paperId);
                if (listOrg == null) {
                    listOrg = new ArrayList<>();
                }
                listOrg.add(orgId);
                _paperOrganization.put(paperId, listOrg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Build graphs CollaborativeOrgGraph (weight is number of collaborations)
     */
    public void buildCollaborativeOrgGraph() {
        for (int pubId : _paperOrganization.keySet()) {
            ArrayList<Integer> listOrgs = _paperOrganization.get(pubId);
            if (listOrgs.size() == 1 && !_collaborativeOrgGraph.containsKey(listOrgs.get(0))) {
                _collaborativeOrgGraph.put(listOrgs.get(0), new HashMap<Integer, Integer>());
            } else {
                for (int org1 : listOrgs) {
                    for (int org2 : listOrgs) {
                        if (org1 != org2) {
                            HashMap<Integer, Integer> collaboration;
                            collaboration = _collaborativeOrgGraph.get(org1);
                            if (collaboration == null) {
                                collaboration = new HashMap<>();
                            }

                            Integer numofPaper = collaboration.get(org2);
                            if (numofPaper == null) {
                                numofPaper = 0;
                            }
                            numofPaper++;
                            collaboration.put(org2, numofPaper);
                            _collaborativeOrgGraph.put(org1, collaboration);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Build RSSOrgGraph (weight is relation strength of different organizations.)
     */
    public HashMap<Integer, HashMap<Integer, Float>> buildRSSOrgGraph() {
        for (int orgId1 : _collaborativeOrgGraph.keySet()) {
            if (_collaborativeOrgGraph.get(orgId1).size() == 0) {
                _rssORGGraph.put(orgId1, new HashMap<Integer, Float>());
            } else {
                int totalCollaborationOfOrg1 = 0;
                for (int orgId2 : _collaborativeOrgGraph.get(orgId1).keySet()) {
                    totalCollaborationOfOrg1 += _collaborativeOrgGraph.get(orgId1).get(orgId2);
                }

                for (int orgId2 : _collaborativeOrgGraph.get(orgId1).keySet()) {
                    if (orgId1 != orgId2) {
                        float weight = ((float) _collaborativeOrgGraph.get(orgId1).get(orgId2)) / ((float) totalCollaborationOfOrg1);
                        HashMap<Integer, Float> rssWeight = _rssORGGraph.get(orgId1);
                        if (rssWeight == null) {
                            rssWeight = new HashMap<>();
                        }

                        Float _weight = rssWeight.get(orgId2);
                        if (_weight == null) {
                            _weight = weight;
                            rssWeight.put(orgId2, _weight);
                        }
                        _rssORGGraph.put(orgId1, rssWeight);
                    }
                }
            }
        }
        return _rssORGGraph;
    }

    public static void main(String args[]) {
        OrganizationGraph orgGraph = new OrganizationGraph();
        orgGraph.load_AuthorID_PaperID_OrgID("C:\\CRS-Experiment\\Sampledata\\Input\\Link-Net\\[Training]AuthorId_PaperID_OrgID.txt");
        orgGraph.buildCollaborativeOrgGraph();
        orgGraph.buildRSSOrgGraph();
    }
}