package entrance

import calculator.{PcCalculator, TfIdfCalculator}
import connector.{HbaseConnector, RedisConnector}
import crawler.{ChinanewsCatcher, WordMessageCatcher}
import preprocessor.C

/**
  * 整合其他模块 实现获取搜索结果的方法
  */
object CalculatorEntrance {
  /**静态变量 与redis连接的变量*/
  def rc = new RedisConnector()
  /**静态变量 与hbase连接的变量*/
  def hc = new HbaseConnector()

  /**
    * 传入关键词数组 搜索pc数值满足区间的一组关键词<br>
    *   计算pc变量 得到搜索的结果的Json字符串<br>
    *     Json中为搜索结果与其相关信息
    * @param words 待搜索关键词数组
    * @param lo [lo,hi) rank从0开始
    * @param hi [lo,hi) rank从0开始
    * @return 含有搜索结果及其信息的Json字符串
    */
  def pcWordsJson(words:Array[String],lo:Int,hi:Int):String={
    val sb = new StringBuffer
    sb.append("[")
    val mid = new PcCalculator().cal(words).range(lo, hi)
    mid.foreach(x=>sb.append(getWordJsonMessageAndUpdateRedis(x)+","))
    if(sb.length()>1)sb.deleteCharAt(sb.length() - 1) // 可能出现上一步操作后sb为 "[" 的情况
    sb.append("]")
    sb.toString
  }

  /**
    * 传入关键词数组 搜索tf-idf数值满足区间的一组网页url<br>
    *   计算tf-idf变量 得到搜索的结果的Json字符串<br>
    *     Json中为搜索结果与其相关信息
    * @param words 待搜索关键词数组
    * @param lo [lo,hi) rank从0开始
    * @param hi [lo,hi) rank从0开始
    * @return 二元组 _1中含有搜索结果及其信息的Json字符串 _2中为总搜索结果数目
    */
  def tfIdfUrlJson(words:Array[String],lo:Int,hi:Int):(String,Int)={
    val sb = new StringBuffer
    sb.append("[")
    val tc = new TfIdfCalculator()
    tc.cal(words).range(lo, hi).foreach(x=>sb.append(getUrlJsonMessageAndUpdateRedis(x,words)+","))
    if(sb.length()>1)sb.deleteCharAt(sb.length() - 1)
    sb.append("]")
    (sb.toString,tc.size())
  }

  /**
    * 传入url 获得含有该网页url 标题 图片概要信息 以及文字概要信息的Json字符串<br>
    *   由于文字概要信息与搜索的关键词相关 所以也需要传入该次搜索的关键词数组<br>
    *     如果该篇文章的html内容原本不存在于redis 更新该键值对
    * @param url 网页url
    * @param words 关键词数组
    * @return 含有网页信息的Json字符串
    */
  def getUrlJsonMessageAndUpdateRedis(url:String,words:Array[String]):String={
    var html = new String
    if(rc.exists(url+":"+C.redisUrlDocSubKey)){
      rc.expire(url+":"+C.redisUrlDocSubKey,C.redisKeyDefaultTtl)
      html = rc.get(url+":"+C.redisUrlDocSubKey)
    }else{
      html = hc.get(url,C.hbaseDocTable,C.hbaseAttrColFamily,C.hbaseOriDocCol)
      rc.set(url+":"+C.redisUrlDocSubKey,html,C.redisKeyDefaultTtl)
    }
    new ChinanewsCatcher(url,html).getJsonMessage(words)
  }

  /**
    * 传入一个词 获得该词的相关信息的Json字符串<br>
    *   如果该Json信息原本不存在于redis 更新该键值对
    * @param word 一个词
    * @return 相关信息的Json字符串
    */
  def getWordJsonMessageAndUpdateRedis(word:String):String={
    var message = new String
    if(rc.exists(word+":"+C.redisWordMessageSubKey)){
      rc.expire(word+":"+C.redisWordMessageSubKey,C.redisKeyDefaultTtl)
      message = rc.get(word+":"+C.redisWordMessageSubKey)
    }else{
      message = WordMessageCatcher.json(word)
      rc.set(word+":"+C.redisWordMessageSubKey,message,C.redisKeyDefaultTtl)
    }
    message
  }
}
