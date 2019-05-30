/*
 * Name: Arunkumar Bagavathi
 * ID: 800888454
 * */


import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;


public class CleanUp extends Configured implements Tool{

	
	/*
	 * RankComparator is used to overrirde the sorting function done by MapReduce
	 * compare() in this class compares two values and returns the negated value
	 * Negative value is returned to sort the values in descending order 
	 * */
	public static class RankComparator extends WritableComparator{
		
		protected RankComparator() {
			super(DoubleWritable.class,true);
		}
		
		@Override
		public int compare(WritableComparable a, WritableComparable b) {
			DoubleWritable v1 = (DoubleWritable) a;
			DoubleWritable v2 = (DoubleWritable) b;
			
			int cmp = v1.compareTo(v2);
			
			return -1 * cmp;
		}
	}
	
	/*
	 * Mapper gets input in the format of : page	PageRank####-->PAGERANK<--####Links
	 * 		where Links are separated by a delimiter ####-->LINKEND<--####
	 * 		Links are optional for a page. If a page doen't have any outlinks, this Links won't be available
	 * 
	 * Mapper takes only page and its PageRank only
	 * Mapper writes output as : (PageRank , page)
	 * 		PageRank is used as Mapper output key because output of this job has to be in descending order of PageRank
	 * 		Reducer automatically sorts by key
	 * */
	public static class CleanUpMapper
	extends Mapper<Object, Text, DoubleWritable, Text>{
		@Override
		protected void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {
			String[] inputSplits = value.toString().split("\t");
			String pageTitle = inputSplits[0];
			//Obtaining PageRank
			String pageRank = inputSplits[1].split("####-->PAGERANK<--####")[0];
			
			context.write(new DoubleWritable(new Double(pageRank)), new Text(pageTitle));
		}
		
	}
	
	/*
	 * Reducer gets (PageRank, list_of_pages)
	 * Reducer writes output as (page, PageRank)
	 * */
	public static class CleanUpReducer
	extends Reducer<DoubleWritable, Text, Text, DoubleWritable>{
		
		
		@Override
		protected void reduce(DoubleWritable key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			Iterator<Text> pageIterator = values.iterator();
			
			while(pageIterator.hasNext()){
				context.write(pageIterator.next(), key);
			}
		}
		
	}
	
	/*
	 * CleanUp job Configuration
	 * Apart from Mapper and Reducer classes, Comparator class is defined
	 * By default, reducer sorts by key in ascending order
	 * 		To sort in descending order, this Comparator is defined
	 * */
	@Override
	public int run(String[] args) throws Exception {
		
		Configuration conf = new Configuration();
		Job job;
		
		job = Job.getInstance(conf, "CleanUp");
		job.setJarByClass(CleanUp.class);
		
		job.setMapperClass(CleanUpMapper.class);
		job.setReducerClass(CleanUpReducer.class);
		job.setSortComparatorClass(RankComparator.class);
		
		
		job.setMapOutputKeyClass(DoubleWritable.class);
		job.setMapOutputValueClass(Text.class);
		
		
		job.setOutputKeyClass(Text.class);
	    job.setOutputValueClass(DoubleWritable.class);
	    
	    job.setNumReduceTasks(1);
	    
	    FileInputFormat.addInputPath(job, new Path(args[0]));
	    FileOutputFormat.setOutputPath(job, new Path(args[1]));
	    
	    return job.waitForCompletion(true) ? 0 : 1;
	}

}
