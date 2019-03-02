package entrance

import java.text.SimpleDateFormat

import crawler.{Catcher,DailyNewsFactory}
import connector.{HbaseConnector, KafkaProdFactory}
import preprocessor.C

/**
  * 含有爬取新闻html内容 存入hbase并向kafka生产(url,html)键值对的方法
  */
object CrawlerEntrance {
  /**与hbase连接的静态变量*/
  def hbaseConnector = new HbaseConnector()

  /**
    * 调用了爬取新闻html内容 存入hbase并向kafka生产(url,html)键值对的过程
    * @param args 命令行参数 第1个和第2个参数为 <code>crawl()</code>方法的两个参数
    */
  def main(args: Array[String]) {
    crawl(args(0),args(1).toLong)
    hbaseConnector.close()
  }

  /**
    * 实现了爬取新闻html内容 存入hbase并向kafka生产(url,html)键值对的过程
    * @param startDate 格式为 yyyy-MM-dd 爬取从该日期到当期时间的新闻
    * @param millis 爬取每天的新闻之后线程休眠的毫秒数
    */
  def crawl(startDate:String,millis:Long): Unit ={
    val sdf = new SimpleDateFormat("yyyy-MM-dd")
    new DailyNewsFactory.DailyThread(sdf.parse(startDate),1000,new DailyNewsFactory.oper(){
      override def exe(c:Catcher) = {
        KafkaProdFactory.producer.send(C.kafka_topic,c.getUrl(),c.getHTML())
        hbaseConnector.put(c.getUrl(),c.getHTML(),C.hbaseDocTable,C.hbaseAttrColFamily,C.hbaseOriDocCol)
        // 网页原文存入hbase
      }
    }).run()
  }
}