import util.Random
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class RestSimulation900Topics extends Simulation {

  val feeder = csv("destinations900.csv").circular

  val httpConf = http
    .baseURL("http://127.0.0.1:8161/api/") // Here is the root for all relative URLs
	.basicAuth("admin", "admin")

  def sendTo() = exec(http("Send to ${destination}").post("message/${destination}?type=topic").formParam("body", "${message}").formParam("originator",  "${userId}"))
  
  
  val initScn = scenario("Init queues")
    .feed(feeder)
	.exec(session => {session.set("message", "Msg " + session("userId").as[String])})
    .exec(sendTo)

	setUp(initScn.inject(rampUsers(3000) over (15 seconds)).protocols(httpConf))
}
