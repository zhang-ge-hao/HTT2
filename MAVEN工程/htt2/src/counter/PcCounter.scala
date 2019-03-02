package counter

import nlprocessor.NLProcessor
import preprocessor.C

/**
  * 计算出所有的关键词两两组合的元组 用于计算pc值
  */
object PcCounter {
  /**
    * 传入一行文字 将其分词<br>
    *   调用<code>count(words:Array[String]):Array[(String,String)]</code>
    * @param title 传入的一行文字
    * @return 所有的关键词两两组合的元组组成的Array 用于计算pc值
    */
  def count(title:String):Array[(String,String)]={
    count(NLProcessor.jSearch(title))
  }

  /**
    * 传入一组关键词 计算出所有的关键词(去除停止词后)两两组合的元组组成的Array 用于计算pc值
    * @param words 关键词数组
    * @return 所有的关键词两两组合的元组组成的Array 用于计算pc值
    */
  def count(words:Array[String]):Array[(String,String)]={
    val validWord = (words.toSet -- C.stopWords.toSet).toArray
    validWord.flatMap(x=>(validWord.map((_,x)))).filter(p=>p._1!=p._2)
  }
}
