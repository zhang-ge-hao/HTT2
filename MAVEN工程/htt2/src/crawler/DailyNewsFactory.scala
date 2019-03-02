package crawler

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import scala.collection.JavaConversions._

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

import preprocessor.C

/**
 * 含有关于中新网信息爬取的相关类和方法
 */
object DailyNewsFactory {
    /**
     * 用于Java函数对象 在这个项目中混入了scala之后<br>
     *     这样的函数对象实现方式显得有些愚蠢
     */
    class oper {
        /**
         * 处理一个Catcher的方法 重载后用于实现函数对象
         * @param c 待处理的Catcher
         * @throws Exception 处理Catcher的过程中可能出现的异常
         */
        @throws[Exception]
        def exe(c:Catcher){

        }
    }

    /**
     * 继承了Thread类 <br>
     *     指定了处理每天新闻之间的时间间隔<br>
     *         指定了对Catcher的操作<br>
     *             指定了新闻开始爬取的日期<br>
     *                 run()方法用于以指定间隔来爬取新闻信息进行处理
     */
    class DailyThread extends Thread{
        /**开始处理的日期 从该日的新闻开始爬取*/
        var date:Date = null
        /**存放了对Catcher的处理方法*/
        var op:oper = null
        /**处理每天的新闻之间的时间间隔*/
        var ms:Long = 1000

        /**
         * 构造函数 初始化了一系列变量
         * @param d 用于初始化date
         * @param m 用于初始化millis
         * @param o 用于初始化op
         */
        def this(d:Date,m:Long,o:oper)={
            this()
            date = d;op = o;ms=m
        }

        /**
         * 线程运行时被调用的函数 循环处理从date至当期时间的新闻
         */
        override def run():Unit={
            val now:Date = new Date()
            val calendar = Calendar.getInstance()
            calendar.setTime(date)
            while(now.compareTo(calendar.getTime) >= 0){
                oneDay(calendar.getTime,op)
                calendar.add(Calendar.DAY_OF_MONTH,1)
                Thread.sleep(ms)
            }
        }
    }

    /**
     * 循环处理某一天的新闻网页
     * @param date 日期
     * @param op 储存了对网页的操作
     * @throws Exception 处理网页的过程中可能产生的异常
     */
    def oneDay(date:Date,op:oper) {
        val document = Jsoup.connect(format(date)).get()
        val list = document.select(".content_list").first()
        val links = list.select("a[href]")
        for (link:Element <- links) {
            val url = link.attr("href")
            C.log.info("The factory has dealt with '"+url+"'.")
            val catcher = new Catcher(url)
            op.exe(catcher)
        }
    }

    /**
     * 传入日期对象 得到中新网当日的新闻汇总页面链接
     * @param date 日期
     * @return 中新网在该日的新闻汇总页面链接
     */
    def format(date:Date)={
        val dateFormat = new SimpleDateFormat("yyyy/MMdd")
        "http://www.chinanews.com/scroll-news/gn/" + dateFormat.format(date) + "/news.shtml"
    }
}
