import util.Random
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class RestSimulationCorrectness extends Simulation {

  val count = 2000;

  val feeder = csv("destinations_2000_dups.csv").circular

  val httpConf = http
    .baseURL("http://127.0.0.1:8161/api/") // Here is the root for all relative URLs
	.basicAuth("admin", "admin")

  def sendTo() = exec(http("Send to ${destination}").post("message/${destination}?type=topic").formParam("body", "${message}").formParam("originator",  "${userId}"))
  
  def receiveAndCheck() = tryMax(5, "t") {exec(http("Receive from ${destination}").get("message/${destination}?type=topic").header("selector", "originator='${userId}'").check(status.is(200)).check(bodyString.is("${message}")))}
  
  
  val initScn = scenario("Init queues")
    .feed(feeder)
	.exec(session => {session.set("message", "Msg " + session("userId").as[String])})
    .exec(sendTo)
	
  val rcvScn = scenario("Wait queues")
    .feed(feeder)
	.exec(session => {session.set("message", "Msg " + session("userId").as[String])})
    .exec(receiveAndCheck)
	
  setUp(rcvScn.inject(rampUsers(count) over (5 seconds)).protocols(httpConf), initScn.inject(nothingFor(10), rampUsers(count) over (15 seconds)).protocols(httpConf))
}
