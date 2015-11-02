/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uit.tkorg.crs.method.link;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author daolv
 */
public class AdamicAdar {

    private HashMap<Integer, HashMap<Integer, Float>> _adamicadarData;
    private HashMap<Integer, HashMap<Integer, Float>> _graph;
    
    private void runAdamicAdar(int authorId1) {
        for (int authorId2 : _graph.keySet()) {
            if (authorId1 != authorId2) {
                Set<Integer> neighborsOfAuthor1 = _graph.get(authorId1).keySet();
                Set<Integer> neighborsOfAuthor2 = _graph.get(authorId2).keySet();
                ArrayList<Integer> sharedNeighbors = new ArrayList<>();
                for (int authorId : neighborsOfAuthor1) {
                    if (neighborsOfAuthor2.contains(authorId)) {
                        sharedNeighbors.add(authorId);
                    }
                }

                float value = 0f;
                for (int authorId : sharedNeighbors) {
                    Set<Integer> neighborsOfSharedAuthor = _graph.get(authorId).keySet();
                    value += 1.0f / (float) Math.log10(neighborsOfSharedAuthor.size());
                }

                if (value > 0f) {
                    HashMap<Integer, Float> listAdamicAdar = _adamicadarData.get(authorId1);
                    if (listAdamicAdar == null) {
                        listAdamicAdar = new HashMap<>();
                    }
                    listAdamicAdar.put(authorId2, value);
                    _adamicadarData.put(authorId1, listAdamicAdar);
                }
            }
        }
    }
    
    /**
     * 
     * @param graph
     * @param listAuthor
     * @return 
     */
    public HashMap<Integer, HashMap<Integer, Float>> process(HashMap<Integer, HashMap<Integer, Float>> graph,
            HashMap<Integer, String> listAuthor) {
        _adamicadarData = new HashMap<>();
        _graph = graph;

        Runtime runtime = Runtime.getRuntime();
        int numOfProcessors = runtime.availableProcessors();

        ExecutorService executor = Executors.newFixedThreadPool(numOfProcessors - 1);
        for (final int authorId : listAuthor.keySet()) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    runAdamicAdar(authorId);
                }
            });
        } 

        executor.shutdown();
        while (!executor.isTerminated()) {
        }
        return _adamicadarData;
    }
}
