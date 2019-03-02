package crawler

import java.io.IOException

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * 使用了我喜爱的JSoup包 通过url爬取网页相关内容
 */
class Catcher {
	/**该对象要处理的url*/
	protected var url:String = null;
	/**该对象对应的document对象*/
	protected var document:Document = null;

	/**
	 * 构造函数 提供url 访问该url获得document
	 * @param u 该对象对应的url
	 * @throws IOException 在访问url的过程中可能产生的异常
	 */
	def this(u:String) ={
		this()
		url = u
		document = Jsoup.connect(url).get()
	}

	/**
	 * 构造函数 提供url和网页html值 初始化该对象
	 * @param u 网页url
	 * @param h 网页html
	 */
	def this(u:String,h:String)={
		this()
		url = u
		document = Jsoup.parse(h)
	}

	/**
	 * 获取url
	 * @return url
	 */
	def getUrl:()=>String = ()=>url

	/**
		* 获取url的网页html内容
		* @return html
		*/
	def getHTML:()=>String = document.html

	/**
	 * 获取url的网页文本内容
	 * @return 网页的文本字符串
	 */
	def getText() ={
		document.text()
	}

	/**
	 * 获取网页标题
	 * @return 网页标题
	 */
	def getTitle()={
		document.select("title").first().text()
	}

	/**
	 * 获取用于概览的图片链接 此处为网页中的第一张图片的url
	 * @return 图片的url
	 */
	def getImgSummary()={
		var imgUrl = ""
		val images = document.getElementsByTag("img")
		if(images.size() > 0) {
			imgUrl = images.get(0).attr("abs:src")
			if(imgUrl.equals(""))imgUrl = images.get(0).attr("abs:data-src")
		}
		imgUrl
	}

	/**
	 * 得到该Catcher对象的url的基本信息<br>
	 *     用于前端的美观展示<br>
	 *         包括url 标题 摘要 以及网页的第一张图片的链接<br>
	 *             转换为Json字符串并返回
	 * @param words 由于返回的摘要需要尽量与关键词相关 所以传入关键词数组
	 * @return 网页的基本信息的Json数组
	 */
	def getJsonMessage(words:Array[String]) ={

		val title = getTitle(); // 标题
		val imgUrl = getImgSummary(); // 第一张图片链接
		var summary:String = null; // 摘要

		val sentences = getText().split("[。；]");
		def for1():String= {
			for (sentence <- sentences) {
				def for2(): String = {
					for (word <- words) {
						if (sentence.contains(word)) summary = sentence
						if (summary != null) return summary
					}
					null
				}
				summary = for2()
				if (summary != null) return summary
			}
			null
		}
		summary = for1()
		if(summary == null)summary = sentences(0)

		// summary = dealWithLength(summary,sumMaxLen,words);
		// title = dealWithLength(title,titMaxLen,words);
		
		val sb = new StringBuffer();
		sb.append("{")
		sb.append("\"url\":\""+url.replaceAll("\"","\\\\\"")+"\",")
		sb.append("\"tit\":\""+title.replaceAll("\"","\\\\\"")+"\",")
		sb.append("\"sum\":\""+summary.replaceAll("\"","\\\\\"")+"\",")
		sb.append("\"img\":\""+imgUrl.replaceAll("\"","\\\\\"")+"\"")
		sb.append("}")
		sb.toString
	}
}
