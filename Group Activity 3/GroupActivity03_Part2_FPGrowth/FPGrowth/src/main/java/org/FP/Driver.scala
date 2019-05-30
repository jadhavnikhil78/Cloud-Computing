package org.FP

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.fpm.FPGrowth
import org.apache.spark.rdd.RDD

object Driver {
  def main(args : Array[String]) : Unit = {
    System.setProperty("hadoop.home.dir", "H:\\Hadoop\\hadoop-common-2.2.0-bin-master\\")
    
    val conf = new SparkConf().setAppName("Association Action Rules Spark").setMaster("local").set("spark.executor.heartbeatInterval","100")
                              .set("spark-executor-memory","8G")
                              .set("spark-driver-memory","8G")
                              .set("spark.kryoserializer.buffer.max.mb", "512")
    val sc = new SparkContext(conf)
    
    val data = sc.textFile(args(0))

    val transactions: RDD[Array[String]] = data.map(s => s.trim.split(','))
    
    val fpg = new FPGrowth()
      .setMinSupport(args(1).toDouble)
      .setNumPartitions(10)
    val model = fpg.run(transactions)
    
    model.generateAssociationRules(args(2).toDouble).map(rule => s"${rule.antecedent.mkString("[", ",", "]")}=> " +
        s"${rule.consequent .mkString("[", ",", "]")},${rule.confidence}").repartition(1).saveAsTextFile(args(3))
    
  }
}