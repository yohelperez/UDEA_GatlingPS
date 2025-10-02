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

  // 3 Load Scenarios - Configurados para cumplir los criterios de aceptación
  
  // Escenario 1: Carga Normal - 100 usuarios concurrentes
  val normalLoadScenario = setUp(
    scn.inject(
      // Rampa gradual para llegar a 100 usuarios concurrentes
      rampUsers(100).during(60.seconds) // 60 segundos para alcanzar los 100 usuarios
    )
  ).protocols(httpConf)
    .assertions(
      global.responseTime.max.lt(2000), // ≤ 2 segundos
      global.successfulRequests.percent.gt(95) // Más del 95% de requests exitosos
    )

  // Escenario 2: Carga Pico - 200 usuarios concurrentes  
  /*val peakLoadScenario = setUp(
    scn.inject(
      // Inyección más agresiva para simular carga pico
      rampUsers(200).during(30.seconds) // 30 segundos para alcanzar los 200 usuarios
    )
  ).protocols(httpConf)
    .assertions(
      global.responseTime.max.lt(5000), // ≤ 5 segundos
      global.successfulRequests.percent.gt(90) // Más del 90% de requests exitosos
    )

  // Escenario 3: Prueba de resistencia con carga mixta
  val enduranceLoadScenario = setUp(
    scn.inject(
      // Fase 1: Carga normal sostenida
      constantUsersPerSec(20).during(5.minutes),
      // Fase 2: Incremento a carga pico
      rampUsersPerSec(20).to(100).during(2.minutes),
      // Fase 3: Carga pico sostenida
      constantUsersPerSec(100).during(10.minutes),
      // Fase 4: Descenso gradual
      rampUsersPerSec(100).to(10).during(2.minutes)
    )
  ).protocols(httpConf)
    .maxDuration(20.minutes) */
}






