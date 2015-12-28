/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package weka.visualize;

import java.awt.*;
import java.io.*;
import javax.swing.*;
import weka.core.*;
import weka.gui.visualize.*;

/**
 * Visualizes previously saved ROC curves.
 *
 * @author FracPete
 */
public class VisualizeMultipleROC {
  
  /**
   * takes arbitraty number of arguments: 
   * previously saved ROC curve data (ARFF file)
   */
  public static void main(String[] args) throws Exception {
    String[] files = {
        "D:\\1.CRS-Experiment\\MLData\\3-Hub\\TrainedModel\\AllFeatures\\DownSampling_Random\\3.Bayes_ROC.arff",
        //"D:\\1.CRS-Experiment\\MLData\\3-Hub\\TrainedModel\\AllFeatures\\DownSampling_Random\\5.Logistic_ROC.arff",
        "D:\\1.CRS-Experiment\\MLData\\3-Hub\\TrainedModel\\AllFeatures\\DownSampling_Random\\1.RandomForest_ROC.arff",
        "D:\\1.CRS-Experiment\\MLData\\3-Hub\\TrainedModel\\AllFeatures\\DownSampling_Random\\2.J48_ROC.arff",
        "D:\\1.CRS-Experiment\\MLData\\3-Hub\\TrainedModel\\AllFeatures\\DownSampling_Random\\4.MultiLayerPerceptron_ROC.arff",
        "D:\\1.CRS-Experiment\\MLData\\3-Hub\\TrainedModel\\AllFeatures\\DownSampling_Random\\6.LibSVM_ROC.arff"
        //"D:\\1.CRS-Experiment\\MLData\\3-Hub\\TrainedModel\\EachFeatures\\MultiLayerPerceptron_CBSim_ROC.arff",
        //"D:\\1.CRS-Experiment\\MLData\\3-Hub\\TrainedModel\\EachFeatures\\MultiLayerPerceptron_CBSim_OrgRSS_ROC.arff",
        //"D:\\1.CRS-Experiment\\MLData\\3-Hub\\TrainedModel\\EachFeatures\\MultiLayerPerceptron_CBSim_CoAuthorRSS_OrgRSS_ROC.arff",
        //"D:\\1.CRS-Experiment\\MLData\\3-Hub\\TrainedModel\\EachFeatures\\MultiLayerPerceptron_CBSim_CoAuthorRSS_OrgRSS_AuthorRank_ROC.arff",
        //"D:\\1.CRS-Experiment\\MLData\\3-Hub\\TrainedModel\\EachFeatures\\MultiLayerPerceptron_AllFeatures_ROC.arff"
    };
    
    boolean first = true;
    ThresholdVisualizePanel vmc = new ThresholdVisualizePanel();
    for (int i = 0; i < files.length; i++) {
      Instances result = new Instances(
                            new BufferedReader(
                              new FileReader(files[i])));
      result.setClassIndex(result.numAttributes() - 1);
      int numInstances = result.numInstances();
      if (numInstances > 1000){
          for (int j = 0; j < numInstances; j++){
              double[] values = result.instance(i).toDoubleArray();
              if (values[values.length - 1] < 0.7){
                  result.delete(i);
              }
          }
      }
      // method visualize
      PlotData2D tempd = new PlotData2D(result);
      tempd.setPlotName(result.relationName());
//      tempd.addInstanceNumberAttribute();
    
      // specify which points are connected
      
      boolean[] cp = new boolean[result.numInstances()];
        for (int n = 1; n < cp.length; n++){
            cp[n] = true;
          }
      tempd.setConnectPoints(cp);
      // add plot
      if (first)
        vmc.setMasterPlot(tempd);
      else
        vmc.addPlot(tempd);
      first = false;
    }
    // method visualizeClassifierErrors
    final javax.swing.JFrame jf = 
      new javax.swing.JFrame("Weka Classifier ROC");
    jf.setSize(500,400);
    jf.getContentPane().setLayout(new BorderLayout());

    jf.getContentPane().add(vmc, BorderLayout.CENTER);
    jf.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent e) {
        jf.dispose();
      }
    });

    jf.setVisible(true);
  }
}