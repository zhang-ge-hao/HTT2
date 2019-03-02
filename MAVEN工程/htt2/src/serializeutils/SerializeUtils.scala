package serializeutils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
  * Java对象序列化与反序列化
  */
object SerializeUtils {
    /**
      * 对象序列化
      * @param obj 待序列化对象
      * @throws IOException 序列化中可能产生的异常
      * @return 序列化字符串
      */
    @throws[IOException]
    def serialize(obj : Any):String = {
        val byteArrayOutputStream = new ByteArrayOutputStream()
        val objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)
        objectOutputStream.writeObject(obj)
        val string = byteArrayOutputStream.toString("ISO-8859-1")
        objectOutputStream.close()
        byteArrayOutputStream.close()
        string
    }

    /**
      * 反序列化得到对象
      * @param str 待反序列化字符串
      * @throws IOException
      * @throws ClassNotFoundException
      * @return 反序列化后的对象
      */
    @throws[IOException]
    @throws[ClassNotFoundException]
    def serializeToObject(str:String) = {
        val byteArrayInputStream = new ByteArrayInputStream(str.getBytes("ISO-8859-1"))
        val objectInputStream = new ObjectInputStream(byteArrayInputStream)
        val obj = objectInputStream.readObject()
        objectInputStream.close()
        byteArrayInputStream.close()
        obj
    }
}