package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class LoginTest extends Simulation{

  // 1 Http Conf
  val httpConf = http.baseUrl(url)
    .acceptHeader("application/json")
    //Verificar de forma general para todas las solicitudes
    .check(status.is(200))

  // 2 Scenario Definition
  val scn = scenario("Login").
    exec(http("login")
      .get(s"/login/$username/$password")
       //Recibir información de la cuenta
      .check(status.is(200))
    )

  // 3 Load Scenario
  setUp(
    scn.inject(
      rampUsers(100).during(60.seconds))
  ).protocols(httpConf)
   .assertions(
     // Aserciones más realistas para sistema de demostración
     global.responseTime.max.lt(10000),    // Máximo 10 segundos
     global.responseTime.mean.lt(5000),    // Promedio menor a 5 segundos
     global.successfulRequests.percent.gt(80)  // Más del 80% de éxito
   )
}








