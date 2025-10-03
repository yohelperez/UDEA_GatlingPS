package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class TransferStressSimulation extends Simulation {

  // 1 - HTTP conf
  val httpConf = http
    .baseUrl(url)
    .acceptHeader("application/json")
    .header("Content-Type", "application/json")

  val feeder = csv("transacciones.csv").circular

  // 3 - Escenario: primer login, luego transfer
  val scn = scenario("TransferenciasConcurrentesConLogin")
    .exec(
      http("Login")
        .get(s"/login/$username/$password")
        .check(status.is(200))
    )
    .pause(500.millis) // pequeña pausa para simular comportamiento real
    .feed(feeder)
    .exec(
      http("Transferencia")
        .post("/transfer")
        .queryParam("fromAccountId", "${fromAccountId}")
        .queryParam("toAccountId", "${toAccountId}")
        .queryParam("amount", "${amount}")
        .check(status.is(200)) // espera 200 si todo ok
    )

  // 4 - Inyección (para alcanzar ~150 TPS usamos open model con tasa)
  val injectionProfile = Seq(
    rampUsersPerSec(50) to 150 during (30.seconds), // warm-up
    constantUsersPerSec(150) during (2.minutes)     // manter 150 requests/s
  )

  // 5 - Setup con assertions
  setUp(
    scn.inject(injectionProfile).protocols(httpConf)
  ).assertions(
    global.successfulRequests.percent.gte(99),      // no perder transacciones
    global.responseTime.percentile(95).lte(2000)    // latencia objetivo (opcional)
  )
}
