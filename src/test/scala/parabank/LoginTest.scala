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
      // Fase 1: Carga normal - 100 usuarios concurrentes
      constantConcurrentUsers(100).during(3.minutes),
      
      // Fase 2: Carga pico - 200 usuarios concurrentes  
      constantConcurrentUsers(200).during(3.minutes)
    ).protocols(httpConf)
  ).assertions(
    // Aserciones específicas para cada criterio
    // Criterio 1: ≤ 2 segundos con 100 usuarios
    details("Perform Login").responseTime.percentile(95).lte(2000),
    
    // Criterio 2: ≤ 5 segundos con 200 usuarios  
    details("Perform Login").responseTime.percentile(95).lte(5000),
    
    // Validación adicional de estabilidad
    global.failedRequests.percent.lte(1.0) // Máximo 1% de errores
  ).maxDuration(10.minutes) // Tiempo máximo total de prueba
}


