import org.junit.Test

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
    memGraph.run("create (n:Person{age:1}) return n").show()
  }
}
