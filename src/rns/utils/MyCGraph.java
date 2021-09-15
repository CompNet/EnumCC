package rns.utils;

import java.util.*; 
import myUtils.Clustering;

public class MyCGraph {
	public int n;
    public Clustering c;
    public double[][] posSumInClusters;
    public double[][] weightSumInClusters;
    public double[][] absWeightSumInClusters;

    
	public MyCGraph(int n_, Clustering c_){
		n = n_;
		c = c_;
		posSumInClusters = new double[c.n][c.getNbCluster()+n]; 
		// +n for new clusters (normally it is sufficient to add k new clusters, where k is the number of moving vertices)
		weightSumInClusters = new double[c.n][c.getNbCluster()+n];
		absWeightSumInClusters = new double[c.n][c.getNbCluster()+n];
		for(int i=0; i<c.n; i++){
    		for(int j=1; j<=(c.getNbCluster()+n); j++){ // +n for new clusters
    			posSumInClusters[i][j-1] = 0.0;
    			weightSumInClusters[i][j-1] = 0.0;
    			absWeightSumInClusters[i][j-1] = 0.0;
    		}
		}

	}

    
    public void fillInNodeToWeightedDegreeSum(double[][] d){
    	//ArrayList<ArrayList<Integer>> clusters = c.getClustersInArrayFormat();
    	for(int i=0; i<n; i++){
    		for(int j=0; j<n ; j++){
    			if(i!=j){
	    			int clu_j = c.membership[j];
	    			if(d[i][j]>0)
	    				posSumInClusters[i][clu_j-1] += d[i][j];
	    			weightSumInClusters[i][clu_j-1] += d[i][j];
	    			absWeightSumInClusters[i][clu_j-1] += Math.abs(d[i][j]);
    			}
    		}
    	}
    }
	    
    
    // A utility function to print nodeToWeightedDegreeSumInClusters
    public void printNodeToWeightedDegreeSumInClusters() 
    { 
    	for(int i=0; i<n; i++){
    		System.out.println("\nnode id:" + i + " -> "); 
    		System.out.print("\t"); 
    		for(int clusterId=1; clusterId<=c.getNbCluster(); clusterId++){
    			System.out.print("c:" + clusterId + ", sum:" + weightSumInClusters[i][clusterId-1]+" -- "); 
    		}
    	}
    	System.out.print("\n"); 
        
    }
    
    // A utility function to print nodeToWeightedDegreeSumInClusters
    public void printNodeToAbsWeightedDegreeSumInClusters() 
    { 
    	for(int i=0; i<n; i++){
    		System.out.println("\nnode id:" + i + " -> "); 
    		System.out.print("\t"); 
    		for(int clusterId=1; clusterId<=c.getNbCluster(); clusterId++){
    			System.out.print("c:" + clusterId + ", abs sum:" + absWeightSumInClusters[i][clusterId-1]+" -- "); 
    		}
    	}
    	System.out.print("\n"); 
        
    } 
	  
} 