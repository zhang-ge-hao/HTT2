package calculator

import java.io.IOException
import java.util
import java.util.{Collections, Comparator}
import scala.collection.JavaConversions._

import connector.HbaseConnector
import connector.RedisConnector
import preprocessor.C

/**
 * 用于计算tf-idf的数值<br>
 *     继承了TreeMap&lt;String,Double&gt;<br>
 *         该类的一个对象自身就储存了搜索结果<br>
 *             key为网页url value为关键词的tf-idf数值<br>
 *                 调用示例可以参照Entrance类中的代码
 */
object TfIdfCalculator{
    /**用于连接hbase的单例*/
    private var hc:HbaseConnector = null

    /**用于连接redis的单例*/
    private var rc:RedisConnector = null
}

class TfIdfCalculator extends util.TreeMap[String,Double]{
    /**
      * 连接hbase的单例
      * @return 连接hbase的单例
      */
    def getHc:HbaseConnector={
        if(TfIdfCalculator.hc == null)TfIdfCalculator.hc = new HbaseConnector()
        TfIdfCalculator.hc
    }

    /**
      * 连接redis的单例
      * @return 连接redis的单例
      */
    def getRc:RedisConnector={
        if(TfIdfCalculator.rc == null)TfIdfCalculator.rc = new RedisConnector()
        TfIdfCalculator.rc
    }

    /**
     * 用于在calWord()操作之前 保证关键词相关的tf参数存在于redis中<br>
     *     如果不在redis缓存中 则加入缓存<br>
     *         同时也在这个过程中将所有的与该关键词相关的(url,tf数值)的键值对存入this对象中<br>
     *             以便得到含有关键词的文章总数 从而得到idf的数值
     * @param word 待检测的关键词
     * @throws IOException 在与hbase以及redis连接的过程中可能发生的异常
     * @throws ClassNotFoundException 在与hbase以及redis连接的过程中可能发生的异常
     */
    @throws[IOException]
    @throws[ClassNotFoundException]
    protected def getTotPutted(word:String):Unit ={
        var tfMap:java.util.TreeMap[String,String] = null
        if(getRc.exists(word+":"+C.hbaseBelColFamily)){
            tfMap = getRc.getObj(word+":"+C.hbaseBelColFamily).asInstanceOf[java.util.TreeMap[String,String]]
            getRc.expire(word+":"+C.hbaseBelColFamily,RedisConnector.redisKeyDefaultTtl)
        }else {
            tfMap = getHc.getObj(word,C.hbaseWordTable,C.hbaseBelColFamily)
            getRc.setObj(word+":"+C.hbaseBelColFamily,tfMap,RedisConnector.redisKeyDefaultTtl)
        }
        for(url <- tfMap.keySet()) this.put(url,0)
    }
    /**
     * 传入一个关键词 从redis中得到所有含有该词的文档 计算出该文档对该关键词对应的tf-idf参数<br>
     *     然后累加到该文档的url作为key 对应的value位置 存入该TfIdfCalculator对象自身<br>
     *         注意 由于难以维护和获取系统中网页文档总数(在idf的计算中需要)<br>
     *             所以在idf的计算中 用含有单次搜索的关键词之一的文章总数代替系统中总文档数
     * @param word 待计算的关键词
     * @throws IOException 连接redis 以及类型转换中可能出现的异常
     * @throws ClassNotFoundException 连接redis 以及类型转换中可能出现的异常
     */
    @throws[IOException]
    @throws[ClassNotFoundException]
    def calWord(word:String):Unit ={
        val tfMap = getRc.getObj(word+":"+C.hbaseBelColFamily).asInstanceOf[util.TreeMap[String,String]]
        val idf = Math.log((1.0*this.size()+1)/(tfMap.size()+1))+1; // IDF(x) = log\frac{N+1}{N(x)+1} + 1
        for(e:util.Map.Entry[String, String] <- tfMap.entrySet()) {
            val tf:Double = e.getValue.toDouble
            val url = e.getKey
            var tfIdf:Double = tf*idf
            if(containsKey(url))tfIdf += get(url)
            this.put(url,tfIdf)
        }
    }
    /**
     * 传入一个关键词数组 循环对每个词调用getTotPutted()和calWord()方法<br>
     *     返回该TfIdfCalculator对象自身 可用于进一步调用
     * @param words 关键词数组
     * @return 循环调用过callWord()方法后的该TfIdfCalculator对象自身
     * @throws  IOException 来自callWord()方法
     * @throws  ClassNotFoundException 来自callWord()方法
     */
    @throws[IOException]
    @throws[ClassNotFoundException]
    def cal(words:Array[String]):TfIdfCalculator={
        for(word <- words) getTotPutted(word)
        for(word <- words) calWord(word)
        this
    }
    /**
     * 当下的TfIdfCalculator对象中已经储存了搜索结果的url以及该url对应的tf-idf数值<br>
     *     传入lo和hi两个数字 返回所有url按照tf-idf排序后的序列的[lo,hi)的子序列组成的String数组
     * @param lo 返回的是排序后的序列的[lo,hi)的子序列
     * @param hi 返回的是排序后的序列的[lo,hi)的子序列
     * @return 搜索结果网页的url组成的String数组
     */
    def range(l:Int,h:Int):Array[String] ={ // [lo,hi) rank从0开始
        var lo:Int = l;var hi:Int = h
        val li = new util.ArrayList[util.Map.Entry[String, Double]](this.entrySet())
        Collections.sort(li,new Comparator[util.Map.Entry[String, Double]](){
            def compare(arg0:util.Map.Entry[String, Double],arg1:util.Map.Entry[String, Double]):Int ={
                -arg0.getValue.compareTo(arg1.getValue); // 按照tf-idf倒序排序
            }
        })
        //for(int i=0;i<li.size();i++)C.debug(li.get(i).getValue());
        lo = Math.min(Math.max(lo,0),this.size())
        hi = Math.min(Math.max(hi,0),this.size()) // 否则可能会超出范围
        // C.debug(lo+" "+hi+" "+this.size());
        if(hi <= lo)return new Array[String](0)
        val res = new Array[String](hi-lo)
        for(i <- lo until hi) {
            res(i-lo) = li.get(i).getKey // key为url value为tf-idf的值
        }
        res
    }
}
