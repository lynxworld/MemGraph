import org.junit.Test

import java.io.File
import scala.::
import scala.io.Source



/**
 * @ClassName MemGraphTest
 * @Description TODO
 * @Author huchuan
 * @Date 2022/1/18
 * @Version 0.1
 */
@Test
class MemGraphTest {
  val memGraph = new MemGraph()

  @Test
  def createTest(): Unit ={
    /*替换成resouces下的queries目录*/
    val path = "/Users/along/github/MemGraph/src/test/resources/queries"
    val queriesDir = new File(path)
    val file = queriesDir.listFiles()

    var cyphers:List[String] = List()

    file.foreach(e=>{
      val source = Source.fromFile(e)
      val lines = source.mkString
      try{
        memGraph.run(lines)
      }catch {
        case ex:Exception=>{
          cyphers = e.getName :: cyphers
        }
      }
    })

    println("不支持的cypher文件：")
    cyphers = cyphers.sorted
    cyphers.foreach(println)
  }
}
