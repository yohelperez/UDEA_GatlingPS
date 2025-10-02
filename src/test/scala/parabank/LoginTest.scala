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
    // Escenario 1: Carga normal (100 usuarios concurrentes)
    scn.inject(
      nothingFor(5.seconds), // Tiempo de espera inicial
      rampUsersPerSec(1).to(100).during(2.minutes), // Rampa hasta 100 usuarios
      constantUsersPerSec(100).during(5.minutes) // Mantener 100 usuarios por 5 minutos
    ).protocols(httpConf),
    
    // Escenario 2: Carga pico (200 usuarios concurrentes)
    scn.inject(
      nothingFor(10.seconds),
      rampUsersPerSec(1).to(200).during(1.minute),
      constantUsersPerSec(200).during(3.minutes)
    ).protocols(httpConf)
  )
    .assertions(
      // Aserciones basadas en los criterios de aceptación
      global.responseTime.percentile(95).lt(2000), // 95% de requests < 2s
      global.responseTime.percentile(99).lt(5000), // 99% de requests < 5s
      global.failedRequests.percent.lt(1.0) // Menos del 1% de errores
    )
    .maxDuration(10.minutes)
}



