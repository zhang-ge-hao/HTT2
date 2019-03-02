package entrance

import connector.{HbaseConnector, KafkaConsFactory, RedisConnector}
import counter.{PcCounter, TfCounter}
import crawler.ChinanewsCatcher
import org.apache.spark.streaming.dstream.InputDStream
import preprocessor.C

/**
  * 含有计算并持久化索引的方法
  */
object CounterEntrance {
  /**
    * 用于计算并持久化索引的程序入口
    * @param args 命令行参数
    */
  def main(args: Array[String]): Unit = {
    KafkaConsFactory.run("htt2",dealWithHTMLRDD)
  }
  /**
    * 处理spark streaming连接 计算出tf与pc的索引并存入hbase<br>
    *   跟随hbase中索引的更新 使用softUpdate()方法更新redis中已经缓存的索引
    * @param stream spark streaming连接对象
    */
  def dealWithHTMLRDD(stream : InputDStream[(String, String)]): Unit ={
    val docs = stream.map(p=>new ChinanewsCatcher(p._1,p._2)).map(cc=>(cc.getUrl(),cc.getTitle(),cc.getText()))
    docs.flatMap(cc=>TfCounter.count(cc._1,cc._3))
      .foreachRDD(r=>{
        r.foreachPartition(p=>{
          val rc = new RedisConnector()
          val hc = new HbaseConnector()
          p.foreach(t=>{
            hc.put(t._2,t._3.toString,C.hbaseWordTable,C.hbaseBelColFamily,t._1)
            rc.softUpdate(t._2 + ":" + C.hbaseBelColFamily,t._1,t._3.toString)
          })
          rc.close()
          hc.close()
        })
      })
    docs.flatMap(cc=>{PcCounter.count(cc._2)})
      .foreachRDD(r=>{
        r.foreachPartition(p=>{
          val rc = new RedisConnector()
          val hc = new HbaseConnector()
          p.foreach(t=>{
            val newVal = hc.add(t._1,1.0,C.hbaseWordTable,C.hbaseNebColFamily,t._2)
            rc.softUpdate(t._1+":"+C.hbaseNebColFamily,t._2,newVal.toString)
          })
          rc.close()
          hc.close()
        })
      })
  }
}
