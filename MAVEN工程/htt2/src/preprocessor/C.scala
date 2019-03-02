package preprocessor

import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.util
import java.util.Scanner
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import scala.collection.JavaConversions._

import org.apache.hadoop.hbase.HBaseConfiguration
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
	* 包含从配置文件中获得系统所需参数的方法<br>
	*   以及logger相关的方法<br>
	*     以及初始化hbaseConfig（Configuration）的方法<br>
	*       包含系统的配置文件
	*/

object C {
	/**系统开始运行的时间 用于log文件命名*/
	final def startTime = System.currentTimeMillis()
	/**读取用户根目录*/
	final def userRoot = System.getProperty("user.home")+"\\"
	/**读取配置文件的目录 配置文件储存在用户根目录下*/
	final def sitePath = userRoot+"htt\\conf\\htt-site.xml"
	/**从配置文件中读取该系统的根目录 配置文件以外的文件会储存在该目录下*/
	final def appRoot = getConf("app.root")+"\\"
	/**得到该次运行的log文件路径*/
	final def logPath = appRoot+"logs\\htt-log-"+startTime+".xml"
	final def  cpDir = appRoot+"cps\\"
	/**从配置文件中读取hbase里文档表（爬虫的输出表 Mapper的输入表）的表名*/
	final def hbaseDocTable = getConf("hbase.doc.table")
	/**文档表的列簇名*/
	final def hbaseAttrColFamily = "attr"
	/**文档表的列名*/
	final def hbaseOriDocCol = "ori"
	/**从配置文件中读取hbase里索引表（Mapper的输出表 搜索过程使用的表）的表名*/
	final def hbaseWordTable = getConf("hbase.word.table")
	/**索引表的列簇名，列名为url*/
	final def hbaseBelColFamily:String = "belong"
	/**索引表的列簇名，列名为和这个词一同出现的词*/
	final def hbaseNebColFamily = "neighbor"
	/**在Redis缓存时 key需要区分 该字符串缓存网页原Html时添加在key后用于区分*/
	final def redisUrlDocSubKey = "url:doc"
	/**在Redis缓存时 key需要区分 该字符串缓存关键词简介时添加在key后用于区分*/
	final def redisWordMessageSubKey = "word:message"
	/**Redis服务器地址*/
	final def redisHost = getConf("redis.host")
	/**该系统中设置key 均需要设置过期时间 该参数为默认ttl*/
	final def redisKeyDefaultTtl = getConf("redis.key.default.ttl").toLong
	/*
	 * 配置属性
	 * metadata.broker.list : kafka集群的broker
	 * serializer.class : 如何序列化发送消息
	 * request.required.acks : 1代表需要broker接收到消息后acknowledgment,默认是0
	 * producer.type : async/sync 默认就是同步sync
	 */
	/**kafka相关配置变量*/
	final def bootstrap_servers = getConf("bootstrap.servers")
	/**kafka相关配置变量*/
	final def kafka_batch_size = getConf("kafka.batch.size")
	/**kafka相关配置变量*/
	final def kafka_acks = getConf("kafka.acks")
	/**kafka相关配置变量*/
	final def kafka_retries = getConf("kafka.retries")
	/**kafka相关配置变量*/
	final def kafka_topic = getConf("kafka.topic")
	/**kafka相关配置变量*/
	final def producer_type = getConf("producer.type")

	/**得到用于此次运行的log操作的log对象*/
	def log = getLogger()

	/**hbase相关的参数*/
	final def hbase_zookeeper_quorum = getConf("hbase.zookeeper.quorum")
	/**hbase相关的参数*/
	final def hadoop_home_dir = getConf("hadoop.home.dir")
	/**hbase相关的参数*/
	final def hbase_zookeeper_property_clientPort = getConf("hbase.zookeeper.property.clientPort")
	/**hbase相关的参数*/
	final def hbase_master = getConf("hbase.master")
	/**从配置文件中读取hbase相关的参数 并初始化hbase的配置对象*/
	final def hbaseConfig = getHbaseConfig()
	/**停止词文件储存的路径*/
	final def stopWords = getDic(appRoot+"bin\\dic\\stopwords.txt")

	/**
		* 方便在系统的别处输出到命令行
		* @param out 待输出的变量
	  */
	def debug(out:AnyRef) ={
		System.out.println(out)
	}
	
	/**
		* 处理异常的方法 搜索引擎系统中出现异常 基本都使用这个方法来处理
		* @param exception 待处理的异常
		*/
	def handleException(exception : Exception) = {
		
		try {
			val sw = new StringWriter()
			val pw = new PrintWriter(sw)
			exception.printStackTrace(pw)
			val msg = sw.toString()
			C.log.severe(msg)
		} catch {
			case e:SecurityException=>e.printStackTrace()
		}
	}
	
	/**
		* 得到一个logger
		* @return 生成的logger
		*/
	def getLogger() ={
		val log = Logger.getLogger("lavasoft")
		log.setLevel(Level.INFO);//级别在该级别以上的log会输出
		val fileHandler = new FileHandler(logPath)
		fileHandler.setLevel(Level.INFO)
		log.addHandler(fileHandler)
		log
	}
	
	/**
		* 传入参数的名字 得到配置文件中配置的参数
		* @param varName 参数名字
		* @return 得到配置文件中配置的参数值
		*/
	def getConf(varName:String) :String={
		val site = Jsoup.parse(new File(sitePath),"UTF-8")
		for(pair : Element <- site.select("property")){
			if(varName.equals(pair.select("name").text()))
				return pair.select("value").text()
		}
		null
	}
	
	/**
		* 从文件得到词典的方法
		* @param path 词典文件路径
		* @return 储存词典中所有词的Set
		*/
	def getDic(path : String) ={
			val fileInputScanner = new Scanner(new FileInputStream(path))
			val resList = new util.ArrayList[String]()
			while(fileInputScanner.hasNextLine())
				resList.add(fileInputScanner.nextLine())
			fileInputScanner.close()
			val res = new Array[String](resList.size())
			for(i <- res.indices) res(i) = resList.get(i)
			res
	}
	
	/**
		* 从配置文件中读取hbase相关的参数后 初始化hbase的配置对象的方法
		* @return hbase配置对象
		*/
	def getHbaseConfig() ={
		System.setProperty("hadoop.home.dir",C.hadoop_home_dir)
		val config = HBaseConfiguration.create()
		config.set("hbase.zookeeper.quorum",C.hbase_zookeeper_quorum)
		config.set("hbase.zookeeper.property.clientPort",C.hbase_zookeeper_property_clientPort)
		config.set("hbase.master",C.hbase_master)
		config
	}
}

