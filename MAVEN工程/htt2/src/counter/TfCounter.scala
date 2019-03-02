package counter

import nlprocessor.NLProcessor

/**
  * 其中的方法可以计算出一个关键词在一个网页中的词频(tf值)
  */
object TfCounter {
  /**
    * 传入网页文本内容 计算tf值
    * @param url 网页url
    * @param doc 网页文本
    * @return 由(url,单词,该单词在文档中出现的词频)的三元组组成的Array对象
    */
  def count(url:String,doc:String):Array[(String,String,Double)]={
    count(url,NLProcessor.jSearch(doc))
  }

  /**
    * 传入网页文本分词后的关键词数组 计算tf值
    * @param url 网页url
    * @param words 关键词数组
    * @return 由(url,单词,该单词在文档中出现的词频)的三元组组成的Array对象
    */
  def count(url:String,words:Array[String]):Array[(String,String,Double)]={
    words.map((_,1)).groupBy(_._1).map(p=>(url,p._1,1.0*p._2.length/words.length)).toArray
  }
}
