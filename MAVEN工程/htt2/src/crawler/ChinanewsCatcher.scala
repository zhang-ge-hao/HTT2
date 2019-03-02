package crawler

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * 中新网专用Catcher
 */
class ChinanewsCatcher extends Catcher{
    /**
      * 构造函数 提供url和网页html值 初始化该对象
      * @param u 网页url
      * @param h 网页html
      */
    def this(u:String,h:String){
        this()
        url = u
        document = Jsoup.parse(h)
    }
    /**
     * 获得中新网新闻页面主要内容的Element
     * @return 包含主要内容的Element
     */
    private def getMainContent():Element = {
        val elements = document.select(".content")
        if(elements.isEmpty)
            return null
        val element = elements.first().clone()
        val adEles = element.select("iframe")
        val broaderEles = element.select(".div624")
        if(!adEles.isEmpty)adEles.remove()
        if(!broaderEles.isEmpty)broaderEles.remove()
        element
    }

    /**
     * 获取新闻文字内容
     * @return 新闻文字内容
     */
    override def getText()={
        val e = getMainContent()
        if (e!=null)e.text else ""
    }

    /**
     * 获取新闻正文中第一张图片
     * @return 新闻正文中第一张图片url
     */
    override def getImgSummary():String={
        var imgUrl = ""
        val me = getMainContent()
        if(me == null)return ""
        val lt = me.select(".left-time")
        if(!lt.isEmpty)lt.remove()
        val images = me.getElementsByTag("img")
        if(images.size() > 0) {
            imgUrl = images.get(0).attr("abs:src")
            if(imgUrl.equals(""))imgUrl = images.get(0).attr("abs:data-src")
        }
        imgUrl
    }
}
