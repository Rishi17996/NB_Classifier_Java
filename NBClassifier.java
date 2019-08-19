/**
 * import statements
 */
import java.io.*;
import java.util.*;
import java.lang.Math;

/**
 * A program to predict the class values of unknown documents.
 * Course:           ISTE 612: Knowledge Process Technologies
 * Name:             Wadekar, Rishi
 * Lab:              Lab #4
 * Date:             04/12/2019
 * @auhor            Rishi Wadekar
 */

public class NBClassifier {
   private ArrayList<String> trainingDocs;      // training data
	private ArrayList<Integer> trainingClasses;  // training class values
   private int numClasses = 0;
	private int[] classDocCounts;                // number of docs per class 
   private String[] classStrings;               // concatenated string for a given class 
	private int[] classTokenCounts;              // total number of tokens per class 
	private HashMap<String,Double>[] condProb;   // term conditional prob
	private HashSet<String> vocabulary;          // entire vocabulary
   private double[] priorProb;                  // prior probabilities
   private int numDocuments;                    // total number of docs
	
	/**
	 * Build a Naive Bayes classifier using a training document set
	 * @param trainDataFolder the training document folder
	 */
	public NBClassifier(String trainDataFolder){
      preprocess(trainDataFolder);
      
//      System.out.println(numClasses);
      
      classDocCounts = new int[numClasses];
		classStrings = new String[numClasses];
		classTokenCounts = new int[numClasses];
      
      condProb = new HashMap[numClasses];
		vocabulary = new HashSet<String>();
      
      for(int i=0;i<numClasses;i++) {
			classStrings[i] = "";
			condProb[i] = new HashMap<String,Double>();
		}
      
      // Getting class document counts
      for(int i=0;i<trainingClasses.size();i++) {
			classDocCounts[trainingClasses.get(i)]++;
			classStrings[trainingClasses.get(i)] += (trainingDocs.get(i) + " ");
		}
//       for(int i: classDocCounts) {
//          System.out.println(i);
//       }
//       for(String s: classStrings) {
//          System.out.println(s+ "\n\n");
//       }

      
      // Calculating prior probabilites
      priorProb = new double[numClasses];
      for(int i = 0; i < numClasses; i++) {
         priorProb[i] = classDocCounts[i]/ (numDocuments * 1.0);
      }
//       for(double i: priorProb) {
//          System.out.println(i);
//       }
      
      
      for(int i=0;i<numClasses;i++){
			//String[] tokens = classStrings[i].replaceAll("^a-zA-Z\\s", "").toLowerCase().split(" ");
         String[] tokens = classStrings[i].toLowerCase().split("[ '.,?!:;$&%+()\\-\\*\\/\\p{Punct}\\s]+");
			classTokenCounts[i] = tokens.length;
			for(String token:tokens){
				vocabulary.add(token);
				if(condProb[i].containsKey(token)){
					double count = condProb[i].get(token);
					condProb[i].put(token, count+1);
				}
				else
					condProb[i].put(token, 1.0);
			}
		}
      
      for(int i=0;i<numClasses;i++){
			Iterator<Map.Entry<String, Double>> iterator = condProb[i].entrySet().iterator();
			int vSize = vocabulary.size();
			while(iterator.hasNext())
			{
				Map.Entry<String, Double> entry = iterator.next();
				String token = entry.getKey();
				Double count = entry.getValue();
            Double prob = (count+1)/(classTokenCounts[i]+vSize);
            condProb[i].put(token, prob);
			}
			System.out.println(condProb[i]);
		}
      
	}
	
	/**
	 * Classify a test doc
	 * @param doc test doc
	 * @return class label
	 */
	public int classify(String doc){
      int classValue;
      File toBeClassified = new File(doc);
      String query = "";
      try {
         Scanner sc = new Scanner(toBeClassified);
         while(sc.hasNextLine()) {
            query += sc.nextLine().toLowerCase();
         }
      }
      catch(Exception ex) {
         ex.printStackTrace();
      }
      String[] queryTerms = query.split("[ '.,?!:;$&%+()\\-\\*\\/\\p{Punct}\\s]+");
      //String[] queryTerms = query.replaceAll("[^a-zA-Z\\s ]", "").split(" ");
      double[] queryClassProb = new double[numClasses];
      
//       for(String s: queryTerms) {
//          System.out.println(s);
//       }
      
      // Calculating conditional probabilities for each class
      for(int i = 0; i < numClasses; i++) {
         for(String queryToken: queryTerms) {
            if(condProb[i].containsKey(queryToken)) {
               queryClassProb[i] += Math.log(condProb[i].get(queryToken));
            }
         }
         queryClassProb[i] += Math.log(priorProb[i]);
      }
//        for (int i = 0; i < numClasses; i++) {
//          System.out.println(queryClassProb[i]);
//       }
//      for(int j=0; j< numClasses; j++) {
//         System.out.println(j+ ": " +queryClassProb[j]);
//      }
      if(queryClassProb[0] >= queryClassProb[1]) {
         classValue = 0;
      } 
      else {
         classValue = 1;
      }
      
		return classValue;
	}
	
	/**
	 * Load the training documents
	 * @param trainDataFolder
	 */
	public void preprocess(String trainDataFolder) {
		trainingDocs = new ArrayList<String>();
      trainingClasses = new ArrayList<Integer>();
      numDocuments = 0;
      try {   
         File[] filesInTrain = new File(trainDataFolder).listFiles();
         for(File f1: filesInTrain) {
            if(f1.isDirectory()) {
               
               String dirPath = f1.getAbsolutePath();
               //System.out.println(dirPath);
               File[] filesInSubDir = new File(dirPath).listFiles();
               for(File f2: filesInSubDir) {
                  numDocuments += 1;
                  String f2Content = "";
                  Scanner sc = new Scanner(f2);
                  while(sc.hasNextLine()) {
                     f2Content += sc.nextLine().toLowerCase();
                  }
                  //System.out.println(f2Content);
                  
                  trainingDocs.add(f2Content);
                  trainingClasses.add(numClasses);
               }
               this.numClasses += 1;
            }
         }
//         System.out.println(numDocuments);
//          for(int i: trainingClasses) {
//             System.out.println(i);
//          }
		}
      catch(Exception e) {
         e.printStackTrace();
      }
	}
	
	/**
	 *  Classify a set of testing documents and report the accuracy
	 * @param testDataFolder fold that contains the testing documents
	 * @return classification accuracy
	 */
	public double classifyAll(String testDataFolder)
	{
      int totalTestDocuments = 0;
      int correctlyClassified = 0;
      ArrayList<Integer> testClassesForAccuracy = new ArrayList<Integer>();
      File[] testDirs = new File(testDataFolder).listFiles();
      for(int i = 0; i < testDirs.length; i++) {
         String testSubFolderPath = testDirs[i].getAbsolutePath();
         File[] testSubFolder = new File(testSubFolderPath).listFiles();
         for(File f: testSubFolder) {
            totalTestDocuments++;
            String filePath = f.getAbsolutePath();
            int classifiedAs = classify(filePath);
            if((int)classifiedAs == (int)i) correctlyClassified++;
         }
      }
      System.out.println("Correctly classified: " + correctlyClassified);
      System.out.println("Total documents: " + totalTestDocuments);
      double accuracy = (correctlyClassified * 1.0) / totalTestDocuments;
		return accuracy;
	}
	
	
	public static void main(String[] args) {		
		NBClassifier nb = new NBClassifier("data/train");
      
      // Classify test Cases
      String filePath1 = "data/test/neg/cv917_29484.txt";
      System.out.println("Test 1:\n" + filePath1);
      System.out.println("Classified as = " + nb.classify(filePath1));
      
      System.out.println();
      String filePath2 = "data/test/pos/cv901_11017.txt";
      System.out.println("Test 2:\n" + filePath2);
      System.out.println("Classified as = " + nb.classify(filePath2));
      
      System.out.println();
      System.out.println("Accuracy for all test documents = " + nb.classifyAll("data/test"));
	}
}
