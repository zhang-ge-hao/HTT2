package servlet

import crawler.WordMessageCatcher
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

/**
  * Servlet类 前端的请求中指定了关键词<br>
  *   获取和该词相关的一个图片的连接<br>
  *     重定向到该链接
  */
@SerialVersionUID(1L)
class ImageSearch extends HttpServlet {
  /**
    * 前端的请求中指定了关键词<br>
    *   获取和该词相关的一个图片的连接<br>
    *     重定向到该链接
    * @param request 请求对象
    * @param response 响应对象
    */
  override def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    request.setCharacterEncoding("UTF-8")
    response.setCharacterEncoding("UTF-8") // 设置编码！
    val wd = request.getParameter("wd")
    var reRequestUrl:String = null
    if(wd == null || wd.trim.equals(""))reRequestUrl = "PIC/default.png"
    else reRequestUrl = WordMessageCatcher.getWordMessageImg(wd)
    response.sendRedirect(reRequestUrl)
  }
}
