package calculator

import java.io.IOException
import scala.collection.JavaConversions._

import connector.RedisConnector
import preprocessor.C

/**
  * 用于计算一组关键词累加的pc值<br>
  *     排序过程与循环调用过程与TfIdfCalculator类相同<br>
  *         所以继承了TfIdfCalculator类
  */
class PcCalculator extends TfIdfCalculator {
    /**
      * 从redis中读入pc记录 计算一个关键词对其他单词的pc值 累加到this对象的value中
      * @param word 待计算的关键词
      * @throws IOException 与redis连接时可能产生的异常
      * @throws ClassNotFoundException 与redis连接时可能产生的异常
      */
    @throws[IOException]
    @throws[ClassNotFoundException]
    override def calWord(word:String):Unit ={
        val tfMap = getRc.getObj(word+":"+ C.hbaseNebColFamily).asInstanceOf[java.util.TreeMap[String,String]]
        for(e:java.util.Map.Entry[String, String] <- tfMap.entrySet) {
            var pc = e.getValue.toDouble
            val another = e.getKey
            if(this.containsKey(another))pc += this.get(another)
            this.put(another,pc)
        }
    }

    /**
      * 检查该关键词的pc记录是否存在于redis中<br>
      *     如果不存在则从hbase中读取pc记录到redis中
      * @param word 待检查的关键词
      * @throws IOException 与connector连接过程中可能发生的异常
      */
    @throws[IOException]
    override def getTotPutted(word:String):Unit={
        if(getRc.exists(word+":"+C.hbaseNebColFamily)){
            getRc.expire(word+":"+C.hbaseNebColFamily,RedisConnector.redisKeyDefaultTtl)
        }else {
            val tfMap = getHc.getObj(word,C.hbaseWordTable,C.hbaseNebColFamily)
            getRc.setObj(word+":"+C.hbaseNebColFamily,tfMap,RedisConnector.redisKeyDefaultTtl)
        }
    }

}
