package crawler

import org.jsoup.Jsoup

/**
 * 在htt2中加入了关联词搜索功能 出于前端展示美观<br>
 *     通过一些手段来获得关键词相关的信息
 */
object WordMessageCatcher {
    /**
     * 获得关键词相关的信息Json字符串<br>
     *     开发早期的Json字符串格式<br>
     *         <code>{"wd" : 关键词, "img" : 概要图片url}</code><br>
     *             由于性能问题概要图片的获取改为其他实现方式 当前Json字符串的格式较为简单<br>
     *                 <code>{"wd" : 关键词}</code>
     * @param word 关键词
     * @return 关键词相关信息的Json字符串
     */
    def json(word:String):String ={
        val sb = new StringBuffer()
        sb.append("{")
        // sb.append("\"img\":\""+getWordMessageImg(word).replaceAll("\"","\\\\\"")+"\",");
        // 由于速度过慢，改变实现方式
        sb.append("\"wd\":\""+word.replaceAll("\"","\\\\\"")+"\"")
        sb.append("}")
        sb.toString
    }

    /**
     * 传入关键词 将百度百科页面和必应搜图作为Api 获得与该关键词相关的一张图片的url<br>
     *     访问Api页面需要时间 如果循环单线程调用 速度较慢<br>
     *         所以改在servlet中调用 servlet中实现了一个重定向 调用该函数获取重定向的url
     * @param word 待查关键词
     * @return 和关键词相关的一张图片的url
     */
    def getWordMessageImg(word:String):String ={
        try {
            val wikiUrl = "https://baike.baidu.com/item/" + word
            val document = Jsoup.connect(wikiUrl).get()
            return document.select(".summary-pic").select("img[src]").first().attr("abs:src")
        }catch {
            case e:Exception=> {
                try {
                    val picQueryUrl = "https://cn.bing.com/images/search?form=HDRSC2&first=1&cw=990&ch=929&q=" + word;
                    val document = Jsoup.connect(picQueryUrl).get();
                    return document.select(".imgpt").select("img[src]").first().attr("abs:src");
                } catch {
                    case e:Exception=>return ""
                }
            }
        }
    }
}
