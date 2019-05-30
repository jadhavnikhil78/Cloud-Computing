/*
 * Name: Arunkumar Bagavathi
 * ID: 800888454
 * */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;

/*
 * PageRank class contains MapReduce module to find PageRank of each page
 * */
public class PageRank extends Configured implements Tool{

	
	/*
	 * Mapper gets input in the format of : page	PageRank####-->PAGERANK<--####Links
	 * 		where Links are separated by a delimiter ####-->LINKEND<--####
	 * 		Links are optional for a page. If a page doen't have any outlinks, this Links won't be available
	 * 
	 * If there are no Links, Mapper writes (page,PageRank)
	 * Else Maper writes the following:
	 * 		1. For each link, (link,(PageRank/number_of_links_for_page)) is written
	 * 		2. (page,<PAGELINKS>Links</PAGELINKS>) is also written
	 * 
	 * Former output means that current page's rank is shared equally among all outgoing links
	 * 
	 * The latter output is written to denote the Reducer that a particular page has some outlinks
	 * In the reducer, new PageRank is computed and prepended to the Links
	 * */
	public static class PageRankMapper
	extends Mapper<Object, Text, Text, Text>{
		@Override
		protected void map(Object key, Text value,
				Mapper<Object, Text, Text, Text>.Context context)
				throws IOException, InterruptedException {
			
			List<String> links = new ArrayList<String>();
			
			//Identifying PageTitle and links from the input
			String[] input = value.toString().split("\t");
			String pageTitle = input[0];
			
			//Separating PageRank and Outlinks from links
			String[] linkSplits = input[1].split("####-->PAGERANK<--####");
			double initialPageRank = Double.parseDouble(linkSplits[0]);
			
			//If the page contain outlinks, some set of outputs are written
			if(linkSplits.length > 1){
				links = Arrays.asList(linkSplits[1].split("####-->LINKEND<--####"));
				
				double rankShare = initialPageRank / links.size();
				
				for (String link : links) {
					context.write(new Text(link), new Text(String.valueOf(rankShare)));
				}
				context.write(new Text(pageTitle),new Text("<PAGELINKS>" + linkSplits[1] + "</PAGELINKS>"));
			} 
			//Else page along with its PageRank is written
			else{
				context.write(new Text(pageTitle), new Text("<PAGELINKS></PAGELINKS>"));
			}
			
			
		}
		
	}
	
	/*
	 * Reducer receives input as : (page, list_of_PageRankShares_from_each_page_and_outlinks_of_current_page)
	 * Reduce function checks if each value contains any set of links. If the value contains links, those links are put in a separate variable
	 * If the value doesn't contain any links, it means that the value is a PageRankShare which is summed up
	 * Final PageRank is calculated using the summed up value
	 * After iterating all values, output is written in the format : page	PageRank####-->PAGERANK<--####Links
	 * 		where Links are separated by a delimiter ####-->LINKEND<--####
	 * 
	 * Note that this output is in the same format of Mapper output
	 * By this way, this output can be used as input for next iteration
	 * */
	public static class PageRankReducer
	extends Reducer<Text, Text, Text, Text>{
		
		
		@Override
		protected void reduce(Text key, Iterable<Text> values,
				Reducer<Text, Text, Text, Text>.Context context)
				throws IOException, InterruptedException {
			final double d = 0.85;
			double sum = 0;
			
			boolean isPageTitle = false;
			
			String links = "";
			Iterator<Text> valueIterator = values.iterator();
			
			//Pattern for identifying links
			Pattern linkPattern = Pattern.compile("<PAGELINKS>(.*?)</PAGELINKS>");
			
			while(valueIterator.hasNext()){
				String value = valueIterator.next().toString();
				Matcher linkMatcher = linkPattern.matcher(value);
				
				//If links are found, they are put into a variable
				if(linkMatcher.find()){
					isPageTitle = true;
					
					links = linkMatcher.group(1);
				} 
				//Else ranks are summed up
				else{
					sum += Double.parseDouble(value);
				}
				
			}
			
			/*
			 * Final PageRank is identified using the formula:
			 * 		PageRank = (1-d) + d*(summedUpValue), d is damping factor which is set to 0.85
			 * */
			double finalPageRank = 0.15 + (0.85 * sum);
			
			if(isPageTitle)
				context.write(key, new Text(finalPageRank + "####-->PAGERANK<--####" + links));
		}
	
	}
	
	
	/*
	 * PageRank job Configuration module
	 * */
	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Job job;
		
		job = Job.getInstance(conf, "Page Rank");
		job.setJarByClass(PageRank.class);
	    job.setMapperClass(PageRankMapper.class);
	    job.setReducerClass(PageRankReducer.class);
	    
	    
	    job.setOutputKeyClass(Text.class);
	    job.setOutputValueClass(Text.class);
	    
	    FileInputFormat.addInputPath(job, new Path(args[0]));
	    FileOutputFormat.setOutputPath(job, new Path(args[1]));
	    
	    return job.waitForCompletion(true) ? 0 : 1;
	}

}
