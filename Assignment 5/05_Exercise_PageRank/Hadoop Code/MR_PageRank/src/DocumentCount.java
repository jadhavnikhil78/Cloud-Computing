/*
 * Name: Arunkumar Bagavathi
 * ID: 800888454 
 * */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;

import com.google.common.collect.Iterators;


/*
 * This class is to parse the given input , count number of documents in the given input , 
 * 		formats parsed input in such a way that it will be useful in the next job
 * Driver instructs this class to execute and write the output
 * Job configuration is done inside run() method
 * */
public class DocumentCount extends Configured implements Tool {

	/*
	 * Mapper class splits checks if each line in the given document contains title and links
	 * If the line contains title and link, identified title, number of outlinks and links are combined using delimiters and sent as a value to reducer
	 * This Mapper outputs like ("Key", combined Title and Value)
	 * Same key is used for all lines to count number of documents in the input in Reducer
	 * Parsing is done in this job itself to avoid reading entire input data again in the next job 
	 * */
	
	/*
	 * 
	 * 
	 * ************** NOTE ***************
	 * Input lines without title are not considered as a document and not taken into account for Document Count
	 * 
	 * 
	 */
	
	public static class DocumentCountMapper
    extends Mapper<Object, Text, Text, Text>{
		@Override
		protected void map(Object key, Text value,
				Mapper<Object, Text, Text, Text>.Context context)
				throws IOException, InterruptedException {
			
			String document = value.toString();
			List<String> links = null;
			
			//Pattern to match page title
			Pattern titlePattern = Pattern.compile("<title>(.*?)</title>");
			Matcher titleMatcher = titlePattern.matcher(document);
			
			//Pattern to match links of the page
			Pattern linkPattern = Pattern.compile("(\\[{2})(.*?)(\\]{2})");
			Matcher linkMatcher = linkPattern.matcher(document);
			
			if(titleMatcher.find()){
				links = new ArrayList<String>();
				
				//Tabs are replaced as spaces in both title and links
				while(linkMatcher.find()){
					String link = linkMatcher.group(2);
					

					if(!link.contains("[["))
						links.add(link.replace('\t', ' '));
				}
				
				
				/*
				 * Output from Mapper is in the format of 
				 * Page<<<<----####---->>>>Links_from_Page
				 * Links are separated by ####-->LINKEND<--####
				 * */
				String valueToReturn = titleMatcher.group(1).replace('\t', ' ')
							+ "<<<<----####---->>>>" + StringUtils.join(links,"####-->LINKEND<--####");
				
				context.write(new Text("Key"), new Text(valueToReturn));
			}
			
		}
	}
	
	/*
	 * This Reducer collects all Mapper outputs
	 * Splits title from the links
	 * It determines count of number of documents(N)
	 * Using N it also calculates initial PageRank using formula (1/N) where N is number of valid documents
	 * Outputs like (title,links) links are combined together by a delimiter
	 * Output title from this Reducer is a concatenation of title,number of outlinks and initial page rank
	 * */
	
	/*
	 * Input format for the Reducer: 
	 *  ("Key", Page<<<<----####---->>>>Links_from_Page),
	 *  where Links are separated by ####-->LINKEND<--####
	 * Output format for the Reducer:
	 *  (Page , initial_PageRank####-->PAGERANK<--####Links_from_Page) ,
	 *  where Links are separated by ####-->LINKEND<--####
	 * */
	public static class DocumentCountReducer
    extends Reducer<Text, Text, Text, Text>{
		@Override
		protected void reduce(Text key, Iterable<Text> values,
				Reducer<Text, Text, Text, Text>.Context context)
				throws IOException, InterruptedException {
			
			Iterator<Text> docsIterator = values.iterator();
			List<String> vals = new ArrayList<String>();
			
			int docCount = 0;
			
			//Counting number of documents in the corpus
			while(docsIterator.hasNext()){
				vals.add(docsIterator.next().toString());
				
				docCount++;
			}
			
			//Computing initial page rank using document count
			Double initialPageRank = (1.0/docCount);
			
			
			for (String string : vals) {
				String[] docs = string.split("<<<<----####---->>>>");
				
				/*
				 * Some pages are without any outlinks
				 * These pages are considered valid pages, but not considered for PageRank, since the formula is (PageRank/Number of outlinks)
				 * */
				Text returnKey = new Text(docs[0]);
				Text returnValue;
				
				if(docs.length == 2)
					returnValue = new Text(initialPageRank+"####-->PAGERANK<--####"+docs[1]);
				else returnValue = new Text(initialPageRank+"####-->PAGERANK<--####");
				
				context.write(returnKey, returnValue);
			}
		}
	}
	
	
	/*
	 * Document Count Configuration
	 * Number of reducers are set to 1 to count final number of valid pages in the corpus
	 * */
	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Job job;
		
		job = Job.getInstance(conf, "Document Count");
		job.setJarByClass(DocumentCount.class);
	    job.setMapperClass(DocumentCountMapper.class);
	    job.setReducerClass(DocumentCountReducer.class);
	    
	    job.setOutputKeyClass(Text.class);
	    job.setOutputValueClass(Text.class);
	    
	    job.setNumReduceTasks(1);
	    
	    FileInputFormat.addInputPath(job, new Path(args[0]));
	    FileOutputFormat.setOutputPath(job, new Path(args[1]));
		
	    return job.waitForCompletion(true) ? 0 : 1;
	}

}
