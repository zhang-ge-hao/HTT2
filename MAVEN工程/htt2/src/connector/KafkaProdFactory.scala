package connector

import java.util.Properties

import org.apache.kafka.clients.producer._

import preprocessor.C

/**
  * 类中的静态变量producer 为继承了KafkaProducer[String,String]的匿名内部类的对象<br>
  *   该对象重载了send()方法 用于向kafka生产消息
  */
object KafkaProdFactory {
  /**静态变量 继承了KafkaProducer[String,String]的匿名内部类的对象*/
  val producer = getProducer()

  /**
    * 获取为继承了KafkaProducer[String,String]的匿名内部类的对象<br>
    *   该对象重载了send()方法
    * @return producer变量
    */
  private def getProducer() = {
    val props = new Properties()
    props.put("bootstrap.servers", C.bootstrap_servers)
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("request.required.acks", C.kafka_acks)
    props.put("producer.type", C.producer_type)
    new KafkaProducer[String,String](props){
      /**
        * 向kafka生产消息
        * @param topic kafka的topic名
        * @param key 生产的消息的key值
        * @param value 生产的消息的value值
        */
      def send(topic:String,key:String,value:String):Unit={
        C.log.info("produce topic : "+topic+", key : "+key+".")
        super.send(new ProducerRecord[String, String](topic,key,value))
      }
    }
  }
}