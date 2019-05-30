/*
 * Name: Arunkumar Bagavathi
 * ID: 800888454
 * */


import java.io.IOException;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ToolRunner;

/*
 * Driver class is the Main class and it contains only one main function which controls all modules
 * Main function gets 3 arguments: InputPath, OutputPath and number of iterations
 * Main function runs jobs in the following order
 * 		1. Document Count - to perform document count in the input corpus and formatting pages and corresponding links 
 * 		2. PageRank - n iterations of PageRank algorithm
 *   	3. CleanUp - removes all outlinks and produces only Page and its corresponding rank
 * */

public class Driver {
	
	public static void main(String[] args) throws Exception {
		Driver driver = new Driver();
		
		/*
		 * Given Output path is modified
		 * If the given output path /user/abagavat/Assignment3/Output,
		 * additional intermediate output path is created like /user/abagavat/Assignment3/IntermediateOutputFiles to store job1 outputs
		 * Given output folder will be used only to write final outputs
		 * Job1 parameters are given input path and intermediate output folder path
		 * */
		String finalOutputPath = args[1];
		String[] splitOutputpath = finalOutputPath.split("/");
		int outputPathLength = splitOutputpath.length;
		
		//JOB1 - Document Count, separates pages and links, find intial PageRank and counts number of outlinks 
		splitOutputpath[splitOutputpath.length-1] = "IntermediateOutputFiles";
		String intermediateOutputPath = StringUtils.join(splitOutputpath, "/");
			
		String[] job1Parameters = {args[0],intermediateOutputPath};
		
		int DocCountToolRunner = ToolRunner.run(new DocumentCount(), job1Parameters);

	    
	    //PageRank jobs
	    int iterationCount = Integer.parseInt(args[2]) + 1;
		
		String[] job2Parameters = new String[2];
	    job2Parameters[0] = intermediateOutputPath;
	    
	    /*
	     * Following for loop runs the job 'n' times where 'n' is the given number of iterations
	     * InputPath is the Output from previous job
	     * New OutputPath is created to store current job's output
	     * During Iteration 1, DocumentCount job output is deleted
	     * For iterations 'i' from 2 to (n-1), Iteration (i-1) output folder is deleted
	     * */
	    for(int i = 1; i < iterationCount; i++){
	    	splitOutputpath[outputPathLength-1] = "Iteration" + i + "Output";
	    	String pageRankOutputPath = StringUtils.join(splitOutputpath, "/");
	    	
	    	job2Parameters[1] = pageRankOutputPath;
	    	int pageRank = ToolRunner.run(new PageRank(), job2Parameters);
	    	
	    	if(i == 1)
	    		driver.deleteDirectory(intermediateOutputPath);
	    	else {
	    		splitOutputpath[outputPathLength-1] = "Iteration" + (i-1) + "Output";
	    		String pathToDelete = StringUtils.join(splitOutputpath, "/");
	    		driver.deleteDirectory(pathToDelete);
	    	}
	    	
	    	job2Parameters[0] = pageRankOutputPath;
	    }
	    
	    
	    //CleanUp Job
	    /*
	     * After this job complete executing, Iteration n output folder is deleted
	     * After this job, no intermediate output folder will be in the disk
	     * */
	    String[] job3Parameters = new String[2];
	    job3Parameters[0] = job2Parameters[0];
	    job3Parameters[1] = args[1];
	    
	    int cleanUp = ToolRunner.run(new CleanUp(), job3Parameters);
	    
	    driver.deleteDirectory(job3Parameters[0]);
	    
	    System.exit(cleanUp);
	    
	}

	/*
	 * Function to delete the given directory
	 * */
	protected void deleteDirectory(String path) throws IOException{
		FileSystem hdfs = FileSystem.get(new Configuration());
		Path directoryPath = new Path(path);
		
		if(hdfs.exists(directoryPath))
			hdfs.deleteOnExit(directoryPath);
	}

}
