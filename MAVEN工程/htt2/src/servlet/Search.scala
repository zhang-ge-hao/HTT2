package servlet

import java.io.IOException

import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import entrance.CalculatorEntrance
import nlprocessor.NLProcessor
import preprocessor.C


/**
  * 接受前端传入的参数 返回搜索结果的Json信息
  */
@SerialVersionUID(1L)
class Search extends HttpServlet {
  /** 默认的每页多少条搜索结果 */
  private val defaultCapacity = 20
  /**默认返回的 相关词条条数*/
  private val defaultNebCapacity = 12
  /** 处理get请求 取得请求中的关键词文本
    * 指定的页码以及每页多少个结果 搜索得到结果并输出
    *
    * @param request  前端发来的请求
    * @param response 对前端的相应
    * @throws ServletException 我也不太懂的Exception
    * @throws IOException      在搜索操作中可能产生的异常
    */
  @throws[ServletException]
  @throws[IOException]
  override def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    request.setCharacterEncoding("UTF-8")
    response.setCharacterEncoding("UTF-8") // 设置编码！

    val out = response.getWriter
    response.setContentType("application/json;charset=utf-8")
    val wd = request.getParameter("wd")
    val pg = request.getParameter("pg") // page 页码
    val cp = request.getParameter("cp") // capacity 指定一页展示多少个结果
    val nc = request.getParameter("nc")

    C.log.info("Servlet get request to search " +
      wd + ". Page : " + pg + ". Capacity : " + cp + ".")

    if (wd == null) {out.print("[]");return}

    var page = 0;var capacity = 0;var nebCapacity = 0
    try {
      page = pg.toInt
      page = Math.max(page, 1)
    } catch {case e: Exception => page = 1}
    try {
      capacity = cp.toInt
      capacity = Math.max(capacity, 0)
    } catch {case e: Exception => capacity = defaultCapacity}
    try {
      nebCapacity = nc.toInt
      nebCapacity = Math.max(nebCapacity, 0)
    } catch {case e: Exception => nebCapacity = defaultNebCapacity}
    val wds = NLProcessor.jIndex(wd)
    val sbData = CalculatorEntrance.tfIdfUrlJson(wds,(page - 1) * capacity, page * capacity)
    val sbNeb = CalculatorEntrance.pcWordsJson(wds,0,nebCapacity)
    val sbWd = new StringBuffer("[")
    wds.map("\""+_+"\",").foreach(sbWd.append)
    if(sbWd.length() > 0)sbWd.deleteCharAt(sbWd.length()-1) // 可能出现上一步操作后sb为 "[" 的情况
    sbWd.append("]")
    val outJson = "{\"count\":" + sbData._2 +
      ",\"wd\":\"" + wd.replaceAll("\"", "\\\\\"") + "\"" +
      ",\"pg\":" + page +
      ",\"cp\":" + capacity +
      ",\"data\":" + sbData._1 +
      ",\"neb\":" + sbNeb +
      ",\"wds\":" + sbWd + "}"
    out.println(outJson)
    C.log.info("Servlet get result to search " + wd + ". Page : " + pg + ". Capacity : " + cp + ".")
  }
}