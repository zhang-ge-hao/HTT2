package connector

import java.io.IOException
import java.util.TreeMap

import org.apache.hadoop.hbase.Cell
import org.apache.hadoop.hbase.CellUtil
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import preprocessor.C
/**
	* 封装了与hbase连接的非静态方法
	*/
object HbaseConnector{
	final private var connection:Connection  = null
	private def getHbaseConnection():Connection = {
		try {
			if(connection == null)
				connection = ConnectionFactory.createConnection(C.hbaseConfig)
		} catch  {
			case e:Exception=> C.handleException(e)
		}
		connection
	}
}
/**
 * 封装了与hbase连接的非静态方法
*/
class HbaseConnector {
	/**与hbase连接的Connection对象*/
	final def connection:Connection  = HbaseConnector.getHbaseConnection()

	/**
	 * 以C.hbaseConfig为设置对象 获取hbase的连接对象
	 * @return 建立的连接对象 可用于设置该类的connection变量
	 */
	private def getHbaseConnection():Connection = {
		try {
			val connection = ConnectionFactory.createConnection(C.hbaseConfig)
			return connection
		} catch  {
			case e:Exception=> C.handleException(e)
		}
		null
	}
	/**
	 * 传入所需参数 向hbase中执行put操作
	 * @param key 该put操作的行名
	 * @param value 该put操作存入的value
	 * @param tableName 表名
	 * @param colFamilyName 列簇名
	 * @param colName 列名
	 * @throws IOException 连接hbase的Table对象的创建和关闭 以及类型转换中可能出现的异常
	 */
	@throws[IOException]
	def put(key:String,value:String,tableName:String,colFamilyName:String,colName:String):Unit ={
		val table = connection.getTable(TableName.valueOf(tableName))
		val put = new Put(Bytes.toBytes(key))
		put.addColumn(Bytes.toBytes(colFamilyName),Bytes.toBytes(colName),Bytes.toBytes(value))
		table.put(put)
		table.close()
	}
	/**
	 * 传入所需参数 向hbase中执行get操作
	 * @param key 该get操作的行名
	 * @param tableName 表名
	 * @param colFamilyName 列簇名
	 * @param colName 列名
	 * @return 返回get到的value值
	 * @throws IOException 连接hbase的Table对象的创建和关闭 以及类型转换中可能出现的异常
	 */
	@throws[IOException]
	def get(key:String,tableName:String,colFamilyName:String,colName:String):String={
		val table = connection.getTable(TableName.valueOf(tableName))
		val get = new Get(Bytes.toBytes(key))
		get.addFamily(Bytes.toBytes(colFamilyName))
		get.addColumn(Bytes.toBytes(colFamilyName), Bytes.toBytes(colName))
		val result = table.get(get)
		val cells = result.rawCells()
		table.close()
		if(cells.isEmpty)return null
		val cell = cells(0)
		new String(CellUtil.cloneValue(cell),"UTF-8");
	}

	/**
	 * 传入所需参数 将value的值累加到hbase的对应位置<br>
	 *     需要hbase的对应位置可以解析为double
	 * @param key 该操作的行名
	 * @param value 待累加的值
	 * @param tableName 表名
	 * @param colFamilyName 列簇名
	 * @param colName 列名
	 * @return 累加后的值
	 * @throws IOException 连接hbase的Table对象的创建和关闭 以及类型转换中可能出现的异常
	 */
	@throws[IOException]
	def add(key:String,value:Double,tableName:String,colFamilyName:String,colName:String) :Double={
		val oriStr = get(key,tableName,colFamilyName,colName)
		val ori = (if(oriStr!=null) oriStr else "0").toDouble
		put(key,String.valueOf(ori+value),tableName,colFamilyName,colName)
		ori+value
	}

	/**
	 * 获得对应位置的 一个行的一个列簇的各个列组成的Map值
	 * @param key 行名
	 * @param tableName 表名
	 * @param colFamilyName 列簇名
	 * @return 由(列名,value)键值对组成的Map
	 * @throws IOException 连接hbase的Table对象的创建和关闭 以及类型转换中可能出现的异常
	 */
	@throws[IOException]
	def getObj(key:String,tableName:String,colFamilyName:String):TreeMap[String, String]={
		val table = connection.getTable(TableName.valueOf(tableName))
		val get = new Get(Bytes.toBytes(key))
		get.addFamily(Bytes.toBytes(colFamilyName))
		val result = table.get(get)
		val cells = result.rawCells()
		table.close()
		val res = new TreeMap[String, String]()
		for(cell <- cells) {
			val value = new String(CellUtil.cloneValue(cell),"UTF-8");
			val field = new String(CellUtil.cloneQualifier(cell),"UTF-8");
			res.put(field,value)
		}
		res
	}

	/**
	 * 关闭与hbase的连接
	 * @throws IOException 连接关闭中可能发生的异常
	 */
	@throws[IOException]
	def close():Unit= {
		// connection.close()
	}
}
