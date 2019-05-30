-------------------------------------------------
*********************README**********************
-------------------------------------------------

Download a .zip file from http://webpages.uncc.edu/aatzache/ITCS6190/Exercises/05_Exercise_PageRank.zip
Unzip the downloaded file

PART - 1:
Steps to execute a simple java program for PageRank:
1. Read the PageRankAlgorithm.pdf , PageRankLogicDiagram.pdf , and PageRankPseudoCode.pdf   for PageRank , which comes with 05_Exercise_PageRank.zip
2. Open your Eclipse
3. Create a simple java project
4. Copy the attached PageRank.java in 'Simple Java Code' folder that comes from 05_Exercise_PageRank.zip into your java project
5. Run PageRank.java
6. When it asks for number of iterations and nodes, give:
5
5


7. When it asks for the node links, give:
0
1
0
0
1
1
0
1
1
0
1
0
0
0
1
0
1
0
0
1
0
0
1
1
0


8. Copy the output into a SimpleJavaPageRankOutput.txt file



PART - 2:
Steps to execute PageRank in Hadoop code:
1. Open WinSCP (in Windows) or Cyberduck (in Macintosh)
2. Open Putty (in Windows) or Terminal (in Macintosh) 
3. Copy PageRank.jar and input-pages.txt from 'Hadoop Code' folder that comes with 05_Exercise_PageRank.zip to WinSCP or Cyberduck
	(Description about the data is provided in the 'Data Description' file inside the .zip file)
	(The code is well documented. MapReduce code can be found in the 'MR_PageRank' folder inside the .zip folder)

4. Copy input-pages.txt to HDFS using
	hadoop fs -put /users/your_UNCCName/input-pages.txt /user/your_UNCCName/ 

5. Run the .jar file like:
	hadoop jar /users/your_UNCCName/PageRank.jar Driver /user/your_UNCCName/input-pages.txt /user/your_UNCCName/PageRankOutput 6

6. Copy output file using:
	hadoop fs -get /user/your_UNCCName/PageRankOutput/part-r-00000 /users/your_UNCCName/PageRankOutput.txt

7. Copy the PageRankOutput.txt from WinSCP or CyberDuck to your local machine


Submit the SimpleJavaPageRankOutput.txt and PageRankOutput.txt in Canvas.
