/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uit.tkorg.crs.utility;

import ir.vsr.HashMapVector;
import ir.utilities.Weight;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Writer;
import org.apache.hadoop.io.Text;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import uit.tkorg.crs.model.Author;
import uit.tkorg.crs.model.Paper;

/**
 *
 * @author THNghiep
 */
public class MahoutFile {

    private MahoutFile() {}

    /**
     * Read vector created by mahout.
     * @param vectorDir: directory outputted by mahout.
     * @return HashMap Integer key and String word.
     * @throws Exception 
     */
    public static HashMap<Integer, String> readMahoutDictionaryFiles(String vectorDir) throws Exception {
        HashMap<Integer, String> dictMap = new HashMap();

        Configuration conf = new Configuration();
        SequenceFile.Reader reader = new SequenceFile.Reader(FileSystem.get(conf), new Path(vectorDir + "\\dictionary.file-0"), conf);
        Text term = new Text();
        IntWritable dictKey = new IntWritable();

        // Note: sequence file mapping from term to its key code.
        // our map will map from key code to term.
        while (reader.next(term, dictKey)) {
            dictMap.put(Integer.valueOf(dictKey.toString()), term.toString());
        }
        reader.close();

        return dictMap;
    }

    /**
     * Read vector created by mahout.
     * @param vectorDir: directory outputted by mahout.
     * @return HashMap document's tf-idf vector.
     * @throws Exception 
     */
    public static HashMap<String, HashMapVector> readMahoutVectorFiles(String vectorDir) throws Exception {
        HashMap<String, HashMapVector> vectorizedDocuments = new HashMap<>();
        
        Configuration conf = new Configuration();
        SequenceFile.Reader reader = new SequenceFile.Reader(FileSystem.get(conf), new Path(vectorDir + "\\tfidf-vectors\\part-r-00000"), conf);
        Text key = new Text(); // document id.
        VectorWritable value = new VectorWritable(); // document content.
        while (reader.next(key, value)) {
            Vector vector = value.get();
            String documentId = key.toString();
            documentId = documentId.substring(documentId.lastIndexOf("/") + 1, documentId.length() - 4);
            // Other way: using regex.
//            Pattern pattern = Pattern.compile(".*/(\\d+)\\.txt");
//            Matcher matcher = pattern.matcher(documentId);
//            if (matcher.find()) {
//                documentId = matcher.group(1);
//            }
            HashMapVector vectorContent = new HashMapVector();
            Iterator<Vector.Element> iter = vector.nonZeroes().iterator();
            while (iter.hasNext()) {
                Vector.Element element = iter.next();
                vectorContent.increment(String.valueOf(element.index()), element.get());
            }
            vectorizedDocuments.put(documentId, vectorContent);
        }
        reader.close();
        
        return vectorizedDocuments;
    }
    
    public static HashMap<String, HashMapVector> readMahoutVectorFiles(HashMap<String,Paper> papers, String vectorDir) throws Exception {
        HashMap<String, HashMapVector> vectorizedDocuments = new HashMap<>();
        
        Configuration conf = new Configuration();
        SequenceFile.Reader reader = new SequenceFile.Reader(FileSystem.get(conf), new Path(vectorDir + "tfidf/tfidf-vectors/part-r-00000"), conf);
        Text key = new Text(); // document id.
        VectorWritable value = new VectorWritable(); // document content.
        while (reader.next(key, value)) {
            Vector vector = value.get();
            String documentId = key.toString();
//            documentId = documentId.substring(documentId.lastIndexOf("/") + 1, documentId.length() - 4);
            if (papers.containsKey(documentId)){
                HashMapVector vectorContent = new HashMapVector();
                Iterator<Vector.Element> iter = vector.nonZeroes().iterator();
                while (iter.hasNext()) {
                    Vector.Element element = iter.next();
                    vectorContent.increment(String.valueOf(element.index()), element.get());
                }
                vectorizedDocuments.put(documentId, vectorContent);
            }
        }
        reader.close();
        return vectorizedDocuments;
    }
    public static void writeAuthorsFV(String vectorDir,HashMap authors){
        try {
            Configuration conf = new Configuration();
            Path outputPath = new Path(vectorDir);
            FileSystem fs = FileSystem.get(conf);
            Text key = new Text();
            HashMapVector value = new HashMapVector();
            SequenceFile.Writer writer = null;
            try {
                writer = openWriter(vectorDir);
                Set<String> authorList = authors.keySet();
                Iterator<String> ir = authors.keySet().iterator();
                while (ir.hasNext()){
                    String authorId = ir.next();
                    Author author = (Author)authors.get(authorId);
                    
                    key = new Text(authorId);
                    value = author.getFeatureVector();
                    Map<String,Weight> map = value.hashMap;
                    writer.append(key, value);
                }
                System.out.println("Writing author FV finished");
            } finally { 
                IOUtils.closeStream( writer);
            }
        }   catch (IOException ex) {
            Logger.getLogger(MahoutFile.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        } 
    }
    
    public static Writer openWriter(String p) {
        Writer w = null;
        try {
            Configuration conf = new Configuration();
            Path dstPath = new Path(p + "/fv.dat");
            FileSystem hdfs = dstPath.getFileSystem(conf);
            w = new SequenceFile.Writer(hdfs,conf,dstPath,Text.class,VectorWritable.class);
        } catch (IOException ex) {
            Logger.getLogger(MahoutFile.class.getName()).log(Level.SEVERE, null, ex);
        }
        return w;
    } 
}