package nlprocessor

import com.huaban.analysis.jieba.{JiebaSegmenter, SegToken}
import com.huaban.analysis.jieba.JiebaSegmenter.SegMode

/**
  * 含有自然语言处理相关的方法 如分词
  */
object NLProcessor {
  /**引用了Jieba分词的Java版*/
  private val jiebaSegmenter = new JiebaSegmenter()

  /**
    * 使用Jieba分词的Search模式分词
    * @param line 待分词语句
    * @return 分词结果
    */
  def jSearch(line:String)={
    val ts = jiebaSegmenter.process(line,SegMode.SEARCH)
    val it = ts.iterator()
    val res = new Array[String](ts.size())
    for(i <- res.indices) res(i) = it.next().word
    res
  }

  /**
    * 使用Jieba分词的Index模式分词
    * @param line 待分词语句
    * @return 分词结果
    */
  def jIndex(line:String)={
    val ts = jiebaSegmenter.process(line,SegMode.INDEX)
    val it = ts.iterator()
    val res = new Array[String](ts.size())
    for(i <- res.indices) res(i) = it.next().word
    res
  }
}
