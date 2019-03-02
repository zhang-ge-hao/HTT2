package connector

import kafka.serializer.StringDecoder
import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream.{InputDStream, _}
import org.apache.spark.streaming.kafka.KafkaUtils

import preprocessor.C

/**
  * 调用该类的run()方法 可以启动一个spark streaming的kafka消费者<br>
  *   可以对获得到的RDD进行传入的操作
  */
object KafkaConsFactory {
  /**
    * 启动一个spark streaming的kafka消费者
    * @param topic 消费者消费的kafka topic名
    * @param func 函数变量 对接收到的stream进行的函数操作
    */
  def run(topic : String,func : (InputDStream[(String, String)])=>Unit) {
    System.setProperty("hadoop.home.dir", C.hadoop_home_dir)
    val sccAndStream = getSccAndStream(topic)
    func(sccAndStream._2)
    sccAndStream._1.start() // 真正启动程序
    sccAndStream._1.awaitTermination() //阻塞等待
  }

  /**
    * 获取消费者的相关变量
    * @param topic 消费者消费的kafka topic名
    * @return 获取到的scc与stream组成的二维元组
    */
  def getSccAndStream(topic : String): (StreamingContext, InputDStream[(String, String)]) ={
    val sparkConf = new SparkConf().setMaster("local[*]").setAppName(topic)
    val scc = new StreamingContext(sparkConf, Duration(5000))
    scc.sparkContext.setLogLevel("ERROR")
    scc.checkpoint(C.cpDir) // 因为使用到了updateStateByKey,所以必须要设置checkpoint
    val topics = Set(topic) //我们需要消费的kafka数据的topic
    val kafkaParam = Map[String, String](
      "zookeeper.connect" -> C.hbase_zookeeper_quorum,
      "metadata.broker.list" -> C.bootstrap_servers, // kafka的broker list地址
      "serializer.class" -> "kafka.serializer.StringEncoder")
    (scc,KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](scc, kafkaParam, topics))
  }

}
