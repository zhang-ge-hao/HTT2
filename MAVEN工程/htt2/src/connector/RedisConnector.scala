package connector

import redis.clients.jedis.Jedis
import java.io.IOException
import java.util
import java.util.regex.Pattern

import serializeutils.SerializeUtils
import preprocessor.C

/**
 * 封装了与redis连接的非静态方法
 */
object RedisConnector{
    /**
      * 该类中的方法向redis中设定的所有键值对均为定时消亡的键值对<br>
      *     该变量即为默认的初始生存时长
      */
    final def redisKeyDefaultTtl:Long = C.redisKeyDefaultTtl
}
/**
  * 封装了与redis连接的非静态方法
  */
class RedisConnector {
    /**与redis连接的Jedis对象*/
    private val jedis = new Jedis(C.redisHost)

    /**
     * 获得当前redis的已用内存字节数
     * @return 当前redis的已用内存字节数
     */
    def getUsedMemory:Integer={
        val m = Pattern.compile("(?<=used_memory:)[0-9]+").matcher(jedis.info())
        if(m.find())m.group(0).toInt else null
    }

    /**
     * 判断传入的key在redis中是否存在
     * @param key 键值
     * @return 是否存在
     */
    def exists(key:String):Boolean={
        jedis.exists(key)
    }

    /**
     * 设定一个键值对 有效时长为ttl秒
     * @param key 键
     * @param value 值
     * @param ttl 有效时长秒数
     * @return 提示信息
     */
    def set(key:String,value:String,ttl:Long):String={
        val res:String = jedis.set(key,value)
        expire(key,ttl)
        res
    }

    /**
     * 获取key对应的value值
     * @param key 键
     * @return key对应的value
     */
    def get(key:String):String={
        jedis.get(key)
    }

    /**
     * 获取key中对应的value值 并反序列化为对象
     * @param key 键
     * @return key对应的Java对象
     * @throws IOException 反序列化过程中可能出现的异常
     * @throws ClassNotFoundException 反序列化过程中可能出现的异常
     */
    @throws[IOException]
    @throws[ClassNotFoundException]
    def getObj(key:String):AnyRef={
        SerializeUtils.serializeToObject(get(key))
    }

    /**
     * 将对象序列化作为value 和key一同存入redis中
     * @param key 健
     * @param value 作为值的Java对象
     * @param ttl 过期时间
     * @return 提示信息
     * @throws IOException 序列化过程中可能出现的异常
     */
    @throws[IOException]
    def setObj(key:String,value:AnyRef,ttl:Long):String={
        set(key,SerializeUtils.serialize(value),ttl)
    }

    /**
     * 更新key对应的对象<br>
     *     该对象为Map&lt;String,String&gt;类型<br>
     *         为该Map更新(filed,value)的键值对 并设定过期时间
     * @param key redis中的键
     * @param field key对应的Map中待更新的键
     * @param value key对应的Map中待更新的值
     * @param ttl 过期时间
     * @return 提示信息
     * @throws Exception redis存取过程中可能产生的异常
     */
    @throws[Exception]
    def updateObj(key:String,field:String,value:String,ttl:Long):String={
        val obj = getObj(key).asInstanceOf[util.TreeMap[String,String]]
        obj.put(field,value)
        setObj(key,obj,ttl)
    }

    /**
     * 关闭与redis的连接
     */
    def close():Unit =jedis.close()

    /**
     * 获取key对应的键值对的过期时间
     * @param key 键
     * @return 过期时间
     */
    def ttl(key:String):Long={
        jedis.ttl(key)
    }

    /**
     * 更新key对应的键值对的过期时间
     * @param key 键
     * @param ttl 过期时间
     * @return 提示信息
     */
    def expire(key:String,ttl:Long){
        jedis.expire(key,ttl.toInt)
    }

    /**
     * 用于在更新hbase同时对redis进行更新<br>
     *     传入key 如果key存在则更新值 但ttl不变
     * @param key 键
     * @param field 见updateObj()方法
     * @param value 见updateObj()方法
     * @return 是否有更新
     * @throws Exception 更新redis过程中可能发生的异常
     */
    @throws[Exception]
    def softUpdate(key:String,field:String,value:String):Boolean={
        if(exists(key)) {
            updateObj(key, field, value, ttl(key))
            return true
        }else{
            return false
        }
    }
}
