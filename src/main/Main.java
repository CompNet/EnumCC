package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import cplex.Cplex;
import formulation.MyPartition;
import formulation.Edge;
import formulation.Param;
import formulation.Partition;
import formulation.MyParam;
import formulation.MyParam.Triangle;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.CplexStatus;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.UnknownObjectException;
import myUtils.Clustering;
import variable.VariableLister.VariableListerException;


/**
* 
* Some references:
* <ul>
* <li> Cartwright D, Harary F (1956) Structural balance:
* 	 a generalization of Heider’s theory. Psychol Rev 63:277-293 <\li>
* <li> Heider F (1946) Attitudes and cognitive organization. J Psychol 21:107-112 <\li>
* <li> N. Arınık & R. Figueiredo & V. Labatut. Efficient Enumeration of Correlation Clustering Optimal Solution Space (submitted), Journal of Global Optmization, 2021. <\li>
* <\lu>
*/
public class Main {

	
	static String tempFile = "temp.txt";
	
	/**
	 * 
	 * EnumCC is an optimal solution space enumeration method for the CC problem.
	 * It relies on two essential tasks: recurrent neighborhood search and jumping onto an undiscovered solution.
	 
	 * In the first step, instead of directly "jumping" onto undiscovered optimal solutions one by one
	 * 	 like in the sequential approach, it discovers the recurrent neighborhood of the current optimal solution P 
	 * 	 with the hope of discovering new optimal solutions. The recurrent neighborhood of an optimal solution P,
	 *  represents the set of optimal solutions, reached directly or indirectly from P depending on 
	 *  the maximum distance parameter 'maxNbEdit'. Whether a new solution is found or not through RNSCC, 
	 *  the jumping process into a new solution P is performed (or the verification of the completeness of the solution space)
	 * 
	 * Input parameters:
	 * <ul>
	 * <li> inFile (String): Input file path. </li>
	 * <li> outDir (String): Output directory path. Default ".", i.e. the current directory). </li>
	 * <li> initMembershipFilePath (String): The membership file path, from which the RNS starts. </li>
	 * <li> java.library.path (String): The Cplex java library path. </li>
	 * <li> maxNbEdit (Integer): the maximum value of the edit distance to consider in edit operations. </li>
	 * <li> tilim (Integer): time limit in seconds. </li>
	 * <li> solLim (Integer): the maximum number of optimal solutions to limit. </li>
	 * <li> nbThread (Integer): number of threads. </li>
	 * <li> JAR_filepath_RNSCC (String): The file path for the RNSCC executable method  </li>
	 * <li> LPFilePath (String): The file path pointing to the ILP model of the given signed graph 
	 * 							(produced by the functionnality 'exportModel' in Cplex) </li>
	 * </ul>
	 * 
	 * 
	 * Example:
	 * <pre>
	 * {@code
	 * 
	 * ant -v -buildfile build.xml -DinFile="in/""$name" -DoutDir="out/""$modifiedName" 
	 * 		-DmaxNbEdit=3 -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath"
	 *  	-DJAR_filepath_RNSCC="RNSCC.jar" -DnbThread=4 -Dtilim=-1 -DsolLim=50000 run
	 * }
	 * </pre>
	 * 
	 * @param args  (Not used in this program. Instead, user parameters are obtained
	 * 	 through ant properties. See the build.xml for more details).
	 * @throws VariableListerException 
	 * @throws IOException 
	 * 
	 * @throws FileNotFoundException.
	 * @throws UnknownObjectException. 
	 * @throws IloException.
	 */
	public static void main(String[] args) throws UnknownObjectException, IloException, IOException {
		System.out.println("!!===============================================");

		String inputFilePath = "";
		String outputDirPath = ".";
		String initMembershipFilePath = "";
		String LPFilePath = "";
		int maxNbEdit = 3; // by default
		String JAR_filepath_RNSCC = "";
		int nbThread = 1;
		long tilim = -1;
		long remainingTime = -1;
		long startTime;
		long enumTime = 0;
		int solLim = -1;
		int nbSols = 1; // init
		int remainingNbSols = -1;
		boolean isBruteForce = false; // init
		
		boolean lazyCB = false;
		boolean userCutCB = false;

		System.out.println("===============================================");

		if( !System.getProperty("inFile").equals("${inFile}") )
			inputFilePath = System.getProperty("inFile");
		else {
			System.out.println("input file is not specified. Exit");
			return;
		}

		
		
		if( !System.getProperty("JAR_filepath_RNSCC").equals("${JAR_filepath_RNSCC}") )
			JAR_filepath_RNSCC = System.getProperty("JAR_filepath_RNSCC");
		else {
			System.out.println("JAR_filepath_EnumCC file is not specified. Exit");
			return;
		}
		
		if( !System.getProperty("outDir").equals("${outDir}") )
			outputDirPath = System.getProperty("outDir");
	

		System.out.println(System.getProperty("initMembershipFilePath"));
		if( !System.getProperty("initMembershipFilePath").equals("${initMembershipFilePath}") ) // it is not useful
			initMembershipFilePath = System.getProperty("initMembershipFilePath");
		else {
			System.out.println("initMembershipFilePath file is not specified.");
		}
		
		if( !System.getProperty("LPFilePath").equals("${LPFilePath}") )
			LPFilePath = System.getProperty("LPFilePath");
		else {
			System.out.println("LPFilePath file is not specified. Exit.");
			return;
		}

		if( !System.getProperty("maxNbEdit").equals("${maxNbEdit}") )
			maxNbEdit = Integer.parseInt(System.getProperty("maxNbEdit"));
		
		if( !System.getProperty("nbThread").equals("${nbThread}") )
			nbThread = Integer.parseInt(System.getProperty("nbThread"));
		
		if( !System.getProperty("tilim").equals("${tilim}") )
			tilim = Long.parseLong(System.getProperty("tilim"));
		
		if( !System.getProperty("solLim").equals("${solLim}") )
			solLim = Integer.parseInt(System.getProperty("solLim"));
		
		
		System.out.println("===============================================");
		System.out.println("inputFilePath: " + inputFilePath);
		System.out.println("outputDirPath: " + outputDirPath);
		System.out.println("initMembershipFilePath: " + initMembershipFilePath);
		System.out.println("LPFilePath: " + LPFilePath);
		System.out.println("maxNbEdit: " + maxNbEdit);
		System.out.println("JAR_filepath_RNSCC: " + JAR_filepath_RNSCC);
		System.out.println("nbThread: " + nbThread);
		System.out.println("tilim: " + tilim);
		System.out.println("solLim: " + solLim);
		System.out.println("===============================================");
		
		// ------------------------------------------------
		// if this is true, this means we will perform Miyauchi's filtering for triangle constraints.
		int n = createTempFileFromInput(inputFilePath);
		
		ArrayList<int[]> allPrevEdgeVarsList = new ArrayList<>();
		ArrayList<int[]> prevEdgeVarsList = new ArrayList<>();
		

		double[][] adjMat = createAdjMatrixFromInput(inputFilePath); // it is used when includeFastJump = true
		
		// -----------------------------------------
		
		remainingTime = tilim;
		System.out.println("remainingTime: " + remainingTime);
		remainingNbSols = solLim;
		System.out.println("remaining number of solutions: " + remainingNbSols);
		
		
		String clusterings_LB_AssocFileName = "assoc.txt";
		int passCounter = 0;
		
		
		
		// =================================================================		
		// STEP 0: init cplex and formulation
		// =================================================================
		
		boolean statusReadLPModelFromFile = false;
		
		
		MyParam myp = null;
		Cplex cplex = new Cplex(); // start
		cplex.setParam(IntParam.ClockType, 2);
		
		
		if(!LPFilePath.equals("")){ // LPFilePath has to point to an existing LP cplex file
			System.out.println("laod LP");			
			cplex.iloCplex.importModel(LPFilePath);
			statusReadLPModelFromFile = true;
		}
		

		myp = new MyParam(tempFile, cplex, Triangle.USE, userCutCB);
		

		myp.useCplexPrimalDual = true;
		myp.useCplexAutoCuts = true;
		myp.tilim = tilim;
		myp.userCutInBB = userCutCB;
		myp.setStatusReadLPModelFromFile(statusReadLPModelFromFile);
		
		MyPartition p = null;
		try {
			p = (MyPartition) Partition.createPartition(myp);
		} catch (IloException | VariableListerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	    int[] initMembership = readMembership(initMembershipFilePath, n);
	    Clustering c_init = new Clustering(initMembership, 0);
	    c_init.computeImbalance(adjMat);
	    
		//double optimalObjectiveValue = p.getObjectiveValue(c_init); // I commented, because when we perform Miyauchi's filtering for triangle constraints, this may cause a problem.
		double optimalObjectiveValue = c_init.getImbalance();
		
		p.createOptimalityConstraint(optimalObjectiveValue);
		System.out.println("imb: " + optimalObjectiveValue);
		
		p.getCplex().setParam(IloCplex.Param.Threads, nbThread);
		// to stop at the first feasible solution (no need to prove optimality, since we know them already):
		p.getCplex().setParam(IloCplex.Param.MIP.Limits.Solutions, 1);
		////p.getCplex().setParam(IloCplex.Param.Advance.FPHeur, 2);
        p.getCplex().setParam(IloCplex.Param.Emphasis.MIP, 1);
		//p.getCplex().setParam(IloCplex.Param.Advance.FeasOptMode, 1); // TODO make this input parameter
	    
	    
		
		// =================================================================		
		// STEP 1
		// =================================================================
		// create an empty file where all the file paths of the results will be stored step by step
		String allPreviousResultsFilePath = outputDirPath+"/allResults.txt";
		File file = new File(allPreviousResultsFilePath);
	    file.createNewFile();
	    
		file = new File(outputDirPath+"/" + clusterings_LB_AssocFileName);
	    file.createNewFile();
	    
	    String _initMembershipFilePath = initMembershipFilePath;
	    initMembershipFilePath = outputDirPath + "/" + "membership0.txt";
		try {
			Files.copy(new File(_initMembershipFilePath).toPath(), new File(initMembershipFilePath).toPath(), 
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		

		ArrayList<Clustering> allCurrentClusterings = new ArrayList<>();

		while(true) {

		    if(tilim > 0 && remainingTime<=0)
		    	break;
			
			// =================================================================
		    // STEP 2
			// =================================================================
		    //String JAR_filepath_EnumCC = "lib/EnumPopulateCCOnePass.jar";
		    //int nbThread = 6;
			startTime = System.currentTimeMillis();
			
		    initMembershipFilePath = outputDirPath + "/membership"+passCounter+".txt";
			
			passCounter++;
		    String outputPassDirPath = outputDirPath + "/" + passCounter ;
		    
			List<String> cmdArgsEnumCC = buildEnumCCCommand(JAR_filepath_RNSCC, inputFilePath, outputPassDirPath, 
					initMembershipFilePath, allPreviousResultsFilePath, maxNbEdit, remainingTime, remainingNbSols,
					isBruteForce, nbThread, false);
			String cmdEnumCC = cmdArgsEnumCC.stream()
				      .collect(Collectors.joining(" "));
			runCommand(cmdEnumCC);
		    
			if(tilim > 0) { // if time limit is provided by user
				enumTime = (System.currentTimeMillis()-startTime)/1000;
				remainingTime = remainingTime-enumTime;
				System.out.println("remainingTime: " + remainingTime);
			}
			
			ArrayList<Clustering> prevClusterings = LoadPreviousClusterings(n,outputPassDirPath,clusterings_LB_AssocFileName);
			allCurrentClusterings.addAll(prevClusterings);
			nbSols += prevClusterings.size();
		    System.out.println("current number of optimal solutions: "+allCurrentClusterings.size());
			if(solLim > 0){
				remainingNbSols = solLim - nbSols;
			}
		    
					
			// ==========================================================================
			for(Clustering c : prevClusterings){
				//c.computeImbalance(adjMat);
				//System.out.println("imb: " + c.getImbalance());
				allPrevEdgeVarsList.add(c.retreiveEdgeVars(p.getEdges()));
				prevEdgeVarsList.add(c.retreiveEdgeVars(p.getEdges()));
			}
			// ==========================================================================
			
			p.getCplex().iloCplex.exportModel(outputDirPath+"/"+"strengthedModelAfterRootRelaxation.lp");
			
			
			 if((tilim > 0 && remainingTime<=0) || (solLim > 0 && remainingNbSols<=0))
			    	break;
		 
//		if((tilim<0 || (tilim > 0 && remainingTime>0)) && (solLim<0 || (solLim > 0 && remainingNbSols>0))){
	    
			// =================================================================
			// STEP 3
			// =================================================================
			
			System.out.println("=== STEP 3 =====");

			startTime = System.currentTimeMillis();
						
			boolean alreadyVisited = true;
			while(alreadyVisited){
				String logpath = outputDirPath + "/" + "jump-log" + passCounter + ".txt";
				p.setLogPath(logpath);
				
				if(tilim > 0)
					p.getCplex().setParam(IloCplex.Param.TimeLimit, remainingTime);
		
				////p.getCplex().setParam(IloCplex.Param.MIP.Strategy.VariableSelect, 3);
				////p.getCplex().setParam(IloCplex.Param.MIP.Strategy.VariableSelect, -1); // when a custom Branch callback is used, use this line
				////p.getCplex().iloCplex.setParam(IloCplex.Param.Preprocessing.Dual, 1);
				////p.getCplex().iloCplex.setParam(IloCplex.Param.MIP.Strategy.Probe, 3);
				////p.getCplex().iloCplex.setParam(IloCplex.Param.MIP.Limits.RepairTries, -1);
				if(passCounter==1) // only at the first iteration
					p.getCplex().iloCplex.setParam(IloCplex.IntParam.RootAlg, 3); // network
				else // for the remaining iterations
				    p.getCplex().iloCplex.setParam(IloCplex.IntParam.RootAlg, 2); // dual
				
				System.out.println("trying to find a new solution with CPLEX. Waiting ...");
				p.solve(prevEdgeVarsList);
				System.out.println("status: " + p.getCplex().getCplexStatus());
				writeStringIntoFile(outputDirPath + "/jump-status"+passCounter+".txt", p.getCplex().getCplexStatus()+"");
				
				enumTime = (System.currentTimeMillis()-startTime)/1000;
				String execTimeFilename = outputDirPath + "/" + "jump-exec-time" + passCounter + ".txt";
				writeDoubleIntoFile(execTimeFilename, enumTime);
				
				if(tilim > 0) { // if time limit is provided by user
					remainingTime = remainingTime-enumTime;
					System.out.println("remainingTime: " + remainingTime);
				
					if(remainingTime<=0)
				    	break;
				}
				
				if(p.getCplex().getCplexStatus() == CplexStatus.Optimal || p.getCplex().getCplexStatus() == CplexStatus.SolLim){
					p.retreiveClusters();
					
	//				nbSols += 1;
	//				//p.writeClusters(outputDirPath + "/membership"+passCounter+".txt");
	//				p.writeMembershipIntoFile(outputDirPath, "membership"+passCounter+".txt");
	//				System.out.println("status: " + p.getCplex().getCplexStatus());
					
					Clustering currClustering = new Clustering(p.retreiveMembership(), -1);
					currClustering.computeImbalance(adjMat);
					System.out.println(currClustering);
					alreadyVisited = isAlreadyVisitedSolution(currClustering, allCurrentClusterings);
					if(alreadyVisited){
						System.out.println("!!!! ALREADY VISITED OPT SOL. RUN AGAIN !!!!");
						
						int[] currEdgeVars = p.retreiveEdgeVariables();
						//p.displayEdgeVariables(6);
						prevEdgeVarsList.clear();
						prevEdgeVarsList.add(currEdgeVars);
						allPrevEdgeVarsList.add(currEdgeVars);
					}
					else {
							nbSols += 1;
							//p.writeClusters(outputDirPath + "/membership"+passCounter+".txt");
							p.writeClusters(outputDirPath + "/membership"+passCounter+".txt");
							System.out.println("status: " + p.getCplex().getCplexStatus());
						}
				
				} else // CplexStatus.Infeasible
					return; // quit the program
				
			}
			
		}
		
		cplex.end(); // end
		
	}

	
	
	
	
	
	

	
	
	/**
	 * This method reads input graph file, then stocks it as weighted adjacency matrix, 
	 * finally writes the graph in lower triangle format into a temp file.
	 * 
	 * @param filename  input graph filename
	 * @return 
	 */
	//private static double[][] createAdjMatrixFromInput(String fileName, boolean isReducedTriangleConstraints) {
	private static double[][] createAdjMatrixFromInput(String fileName) {
		
		  double[][] weightedAdjMatrix = null;
		  int n = -1;
		// =====================================================================
		// read input graph file
		// =====================================================================
		try{
		  InputStream  ips=new FileInputStream(fileName);
		  InputStreamReader ipsr=new InputStreamReader(ips);
		  BufferedReader   br=new
		  BufferedReader(ipsr);
		  String ligne;
		  
		  ligne = br.readLine();
		  
		  /* Get the number of nodes from the first line */
		  n = Integer.parseInt(ligne.split("\t")[0]);
		  

		  weightedAdjMatrix = new double[n][n];
		  if(weightedAdjMatrix[0][0] != 0.0d)
			  System.out.println("Main: Error default value of doubles");
		  
		  /* For all the other lines */
		  while ((ligne=br.readLine())!=null){
			  
			  String[] split = ligne.split("\t");
			  
			  if(split.length >= 3){
				  int i = Integer.parseInt(split[0]);
				  int j = Integer.parseInt(split[1]);
				  double v = Double.parseDouble(split[2]);
				  weightedAdjMatrix[i][j] = v;
				  weightedAdjMatrix[j][i] = v;
			  }
			  else
				  System.err.println("All the lines of the input file must contain three values" 
						+ " separated by tabulations"
						+ "(except the first one which contains two values).\n"
				  		+ "Current line: " + ligne);
		  }
		  br.close();
		}catch(Exception e){
		  System.out.println(e.toString());
		  n = -1;
		}
		// end =================================================================
		
		

		return(weightedAdjMatrix);
	}


	
	/**
	 * This method reads input graph file, then stocks it as weighted adjacency matrix, 
	 * finally writes the graph in lower triangle format into a temp file.
	 * 
	 * @param filename  input graph filename
	 * @return 
	 */
	private static int createTempFileFromInput(String fileName) {
		double[][] weightedAdjMatrix = null;
		int n=-1;
		
		// =====================================================================
		// read input graph file
		// =====================================================================
		try{
		  InputStream  ips=new FileInputStream(fileName);
		  InputStreamReader ipsr=new InputStreamReader(ips);
		  BufferedReader   br=new
		  BufferedReader(ipsr);
		  String ligne;
		  
		  ligne = br.readLine();
		  
		  /* Get the number of nodes from the first line */
		  n = Integer.parseInt(ligne.split("\t")[0]);
		  

		  weightedAdjMatrix = new double[n][n];
		  if(weightedAdjMatrix[0][0] != 0.0d)
			  System.out.println("Main: Error default value of doubles");
		  
		  /* For all the other lines */
		  while ((ligne=br.readLine())!=null){
			  
			  String[] split = ligne.split("\t");
			  
			  if(split.length >= 3){
				  int i = Integer.parseInt(split[0]);
				  int j = Integer.parseInt(split[1]);
				  double v = Double.parseDouble(split[2]);
				  weightedAdjMatrix[i][j] = v;
				  weightedAdjMatrix[j][i] = v;
			  }
			  else
				  System.err.println("All the lines of the input file must contain three values" 
						+ " separated by tabulations"
						+ "(except the first one which contains two values).\n"
				  		+ "Current line: " + ligne);
		  }
		  br.close();
		}catch(Exception e){
		  System.out.println(e.toString());
		}
		// end =================================================================


		// =====================================================================
		// write into temp file (in lower triangle format)
		// =====================================================================
		if(weightedAdjMatrix != null){
			 try{
			     FileWriter fw = new FileWriter(tempFile, false);
			     BufferedWriter output = new BufferedWriter(fw);

			     for(int i = 1 ; i < weightedAdjMatrix.length ; ++i){
			    	 String s = "";
			    	 
			    	 for(int j = 0 ; j < i ; ++j) // for each line, iterate over columns
			    		 s += weightedAdjMatrix[i][j] + " ";

			    	 s += "\n";
			    	 output.write(s);
			    	 output.flush();
			     }
			     
			     output.close();
			 }
			 catch(IOException ioe){
			     System.out.print("Erreur in reading input file: ");
			     ioe.printStackTrace();
			 }

		}
		// end =================================================================

		return(n);
	}
	
	
	
	private static boolean isAlreadyVisitedSolution(Clustering c_new, ArrayList<Clustering> allCurrentClusterings){
		int i=0;
		for(Clustering c : allCurrentClusterings){
			if(c_new.equals(c)) {
				System.out.println("redundant solution found at index : " + i);
				System.out.println(c.getClustersInArrayFormat());
//				System.out.println(c.retreiveEdgeVars().toString());
				return(true);
			}
		}
		return(false);
	}
	
	
	
	private static void writeDoubleIntoFile(String filename, double value){
		try{
		     FileWriter fw = new FileWriter(filename, false);
		     BufferedWriter output = new BufferedWriter(fw);

	    	 String s = value+"";
	    	 output.write(s);
	    	 output.flush();
		     output.close();
		 }
		 catch(IOException ioe){
		     System.out.print("Erreur in reading input file: ");
		     ioe.printStackTrace();
		 }
	}
	
	private static void writeStringIntoFile(String filename, String s){
		try{
		     FileWriter fw = new FileWriter(filename, false);
		     BufferedWriter output = new BufferedWriter(fw);

	    	 output.write(s);
	    	 output.flush();
		     output.close();
		 }
		 catch(IOException ioe){
		     System.out.print("Erreur in reading input file: ");
		     ioe.printStackTrace();
		 }
	}
	
	/**
	 * This method reads a clustering result file.
	 * 
	 * @param filename  input clustering filename
	 * @param n: nb node in the graph
	 * @return 
	 */
	private static ArrayList<Clustering> LoadPreviousClusterings(int n, String inputDirPath, String clusterings_LB_AssocFileName) {
		ArrayList<Clustering> clusterings = new ArrayList<Clustering>();
		
		//System.out.println(inputDirPath+"/"+clusterings_LB_AssocFileName);
		try{
			  InputStream  ips=new FileInputStream(inputDirPath+"/"+clusterings_LB_AssocFileName);
			  InputStreamReader ipsr=new InputStreamReader(ips);
			  BufferedReader   br=new
			  BufferedReader(ipsr);
			  String line;

			  /* For all the lines */
			  while ((line=br.readLine())!=null){
				  String[] items = line.split(":");
				  int diversityLowerBound = Integer.parseInt(items[0]);
				  String clusteringFilePath = items[1];
				  
				  //int[] membership = readClusteringExCCResult(inputDirPath+"/"+clusteringFileName, n);
			      int[] membership = readMembership(clusteringFilePath, n);
			      Clustering c = new Clustering(membership, -1);
			      clusterings.add(c);
			  }
			  br.close();
			  
			}catch(Exception e){
			  System.out.println(e.toString());
			}
		
		return(clusterings);
	}
	

	/**
	 * read a solution from file
	 * 
	 */
	public static int[] readMembership(String fileName, int n){
		//String fileName = "membership" + id_ + ".txt";
		//String filepath = inputDirPath + "/" + fileName;
		int[] membership_ = new int[n];
		
		try{
			InputStream  ips = new FileInputStream(fileName);
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br = new BufferedReader(ipsr);
			String line;
			  
			for(int i=0; i<n; i++){ // for each node
				line = br.readLine();
				membership_[i] = Integer.parseInt(line);	
			}
			
			line = br.readLine();
			br.close();
			
			// verify that the file we just read corresponds to a correct nb node
			if(line != null){
				return(null);
			}
		
		}catch(Exception e){
		  System.out.println(e.toString());
		  return(null);
		}
		
		return(membership_);
	}
	
	
	
	public static int getNbLinesInFile(String filepath) throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(filepath));
		int lines = 0;
		while (reader.readLine() != null) lines++;
		reader.close();
		return(lines);
	}
	
	
	 public static TreeSet<ArrayList<Integer>> getMIPStartSolutionInArrayFormat(int[] membership){
	    	int n = membership.length;
	    	int nbCluster=0;
			for(int i=0; i<n; i++){
				if(membership[i]>nbCluster)
					nbCluster = membership[i];
			}
			
			TreeSet<ArrayList<Integer>> orderedClusters = new TreeSet<ArrayList<Integer>>(
					new Comparator<ArrayList<Integer>>(){
						// descending order by array size
						@Override
						public int compare(ArrayList<Integer> o1, ArrayList<Integer> o2) {
							int value=-1;
							if(o1.size() < o2.size())
								value = 1;
//							else if(o1.size() < o2.size())
//									value = -1;
							return value;
						}
					}
			);

			
	    	ArrayList<ArrayList<Integer>> clusters = new ArrayList<ArrayList<Integer>>(nbCluster);
			for(int i=1; i<=nbCluster; i++) // for each cluster
				clusters.add(new ArrayList<Integer>());
			for(int i=0; i<n; i++) // for each node
				clusters.get(membership[i]-1).add(i); // membership array has values starting from 1
			
			for(int i=1; i<=nbCluster; i++){ // for each cluster
				ArrayList<Integer> newCluster = clusters.get(i-1);
				orderedClusters.add(newCluster);
			}
			

			return(orderedClusters);
	    }
	 
	 
	    
	 	
	 	public static List<String> buildEnumCCCommand(String JAR_filepath, String inputFilePath,
	 			String outDir, String initMembershipFilePath, String allPreviousResultsFilePath, int maxNbEdit,
	 			long tilim, int solLim, boolean isBruteForce, int nbThread, boolean isIncrementalEditBFS
	 			){
			List<String> cmdArgsDistCC = new ArrayList<>();
			cmdArgsDistCC.add("java");
			cmdArgsDistCC.add("-DinputFilePath="+ inputFilePath);
			cmdArgsDistCC.add("-DoutDir=" + outDir);
			cmdArgsDistCC.add("-DinitMembershipFilePath=" + initMembershipFilePath);
			cmdArgsDistCC.add("-DallPreviousResultsFilePath=" + allPreviousResultsFilePath);
			cmdArgsDistCC.add("-DmaxNbEdit=" + maxNbEdit);
			cmdArgsDistCC.add("-Dtilim=" + tilim);
			cmdArgsDistCC.add("-DsolLim=" + solLim);
			cmdArgsDistCC.add("-DisBruteForce=" + isBruteForce);
			cmdArgsDistCC.add("-DnbThread=" + nbThread);
			cmdArgsDistCC.add("-DisIncrementalEditBFS=" + isIncrementalEditBFS);
			cmdArgsDistCC.add("-jar " + JAR_filepath);
			
			return(cmdArgsDistCC);
		}
		
		
	 	public static void runCommand(String cmd){
			Process p;
			try {
				String line;
				p = Runtime.getRuntime().exec(cmd);
				System.out.println(cmd+"\nWaiting ...");
				BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				  while ((line = input.readLine()) != null) {
				    //System.out.println(line); //==> if you want to see console output, decomment this line
				}
				input.close();
				//int exitVal = p.waitFor();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
}
