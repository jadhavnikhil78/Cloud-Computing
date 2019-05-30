package org.Apriori

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import scala.util.control.Breaks._

object Driver {
  def main(args : Array[String]) = {
    System.setProperty("hadoop.home.dir", "C:\\Hadoop\\hadoop-common-2.2.0-bin-master\\")
    
    val conf = new SparkConf().setAppName("SparkAssociationRules").setMaster("local")
    val sc = new SparkContext(conf)
    
    var associationRules : scala.collection.mutable.ListBuffer[String] = scala.collection.mutable.ListBuffer()
    
    //Reading the given input file
    //The input file should be ','
    val dataRDD = sc.textFile(args(0)).map { line => line.split(",").toList }
    
    //Reading minimum support and confidence
    val minSupport = args(1).toInt
    val minConfidence = args(2).toDouble
    
    //Broadcasting minimum support and confidence
    val minSupportBroadcast = sc.broadcast(minSupport)
    val minConfidenceBroadcast = sc.broadcast(minConfidence)
    
    //Getting longest transaction to set number of iterations
    //Assuming that there are no duplicate items within a transactions
    val iterations = dataRDD.map { transaction => transaction.size }.max()
    
    //Starting iterations to get frequent itemsets
    //Starting from 2 since we can form association rules with 2 items
    breakable(for(i <- 2 until iterations+1){
      println("Iteration-" + (i))
      
      val combinationBraodcast = sc.broadcast(i)
      
      val frequentItemsetsRDD = dataRDD.flatMap { transaction => transaction.combinations(combinationBraodcast.value) }
                                    .map(combinations => (combinations.sorted,1))
                                    .groupByKey()
                                    .map(tuple => (tuple._1,tuple._2.size))
                                    .filter(_._2 >= minSupportBroadcast.value)
      if(frequentItemsetsRDD.count() == 0)
        break
                                    
      val associationRulesRDD = frequentItemsetsRDD.flatMap(tuple => {
                                      var associationRules : scala.collection.mutable.ListBuffer[(String,(String,Int))] = scala.collection.mutable.ListBuffer()
                                      
                                      for(j <- 1 until tuple._1.size){
                                        val leftCombinations = tuple._1.combinations(j).toList
                                        val rightCombinations = tuple._1.combinations(tuple._1.size-j).toList
                                        
                                        leftCombinations.foreach { leftComb => {
                                          val leftCombSet = leftComb.sorted.toSet
                                          rightCombinations.foreach { rightComb => {
                                            val rightCombSet = rightComb.sorted.toSet
                                            if(leftCombSet.intersect(rightCombSet).size == 0 && leftCombSet.size != 0 && rightCombSet!= 0){
                                              val toAdd = (leftCombSet.mkString(","),(rightCombSet.mkString(","),tuple._2))
                                              associationRules += toAdd
                                            }
                                          } }
                                        } }
                                      }
                                      
                               
                                      associationRules
                                    })
                                    
     associationRulesRDD.collect().foreach(pattern => {
       val ruleLeftSize = pattern._1.split(",").size
       val patternBroadcast = sc.broadcast(pattern)
       
       val localAssociationRules = dataRDD.flatMap { transaction => transaction.combinations(ruleLeftSize) }
                                    .map(combinations => (combinations.sorted.mkString(","),1))
                                    .filter(_._1 == pattern._1)
                                    .groupByKey()
                                    .map(tuple => tuple._2.size)
                                    .map(conf => {
                                      val rule = patternBroadcast.value
                                      
                                      val ruleConf = (rule._2._2.toDouble/conf.toDouble)*100.0
                                      (rule._1 + " ==> " + rule._2._1 , rule._2._2, ruleConf)
                                    })
                                    .filter(_._3 >= minConfidenceBroadcast.value)
                                    .map(triple => triple._1 + "[Support= " + triple._2 + ", Confidence=" + triple._3 +"%]")
                                    .collect()
                                    
        localAssociationRules.foreach { rule => {
          associationRules += rule
        }}
     })
    })
    
     sc.parallelize(associationRules, 1).saveAsTextFile(args(3))
  }
}