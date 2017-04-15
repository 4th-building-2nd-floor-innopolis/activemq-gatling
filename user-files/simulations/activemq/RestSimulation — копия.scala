import util.Random
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class RestSimulation extends Simulation {

  val count = 2000;

  val feeder = csv("destinations_2000_dups.csv").circular
  //Iterator.continually(Map("destination" -> (Random.alphanumeric.take(10).mkString)))

  val httpConf = http
    .baseURL("http://127.0.0.1:8161/api/") // Here is the root for all relative URLs
	.basicAuth("admin", "admin")

  def sendTo() = exec(http("Send to ${destination}").post("message/${destination}?type=topic").formParam("body", "${message}").formParam("originator",  "${userId}"))
  
  
  //.header("Connection", "Keep-Alive") readTimeout=20000 //
  def receiveAndCheck() = tryMax(5, "t") {exec(http("Receive from ${destination}").get("message/${destination}?type=topic").header("selector", "originator='${userId}'").check(status.is(200)).check(bodyString.is("${message}")))}
  
  
  //.header("selector", "originator=${userId}")
  
  /*exec(http("Receive from ${destination}").get("message/${destination}?type=topic&clientId=${userId}&readTimeout=30000").check(status.is(200)).check(bodyString.is("${message}")))*/
  
  val initScn = scenario("Init queues")
    .feed(feeder)
	.exec(session => {session.set("message", "Msg " + session("userId").as[String])})
    .exec(sendTo)
	
  val rcvScn = scenario("Wait queues")
    .feed(feeder)
	.exec(session => {session.set("message", "Msg " + session("userId").as[String])})
    .exec(receiveAndCheck)
	
  setUp(rcvScn.inject(rampUsers(count) over (5 seconds)).protocols(httpConf), initScn.inject(nothingFor(10), rampUsers(count) over (15 seconds)).protocols(httpConf))
	  //setUp(rcvScn.inject(rampUsers(1000) over (5 seconds)).protocols(httpConf))
	 //setUp(initScn.inject(rampUsers(100) over (15 seconds)).protocols(httpConf))
}
