package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class TransferStressSimulation extends Simulation {

  // 1️. Configuración HTTP
  val httpConf = http
    .baseUrl(url)
    .header("Content-Type", "application/json")
    .check(status.is(200)) // Toda transferencia debe ser exitosa

  val scn = scenario("AccountHistory")
    .exec(
      http("login")
        .get(s"/login/$username/$password")
        .check(status.is(200))
    )
    .pause(1)

    .feed(accountsFeeder)
    .exec(
      http("GetAccountHistory")
        .get("/accounts/${accountId}/transactions?cb=${cb}")
        .check(status.is(200))
      .check(jsonPath("$[0].id").exists)
      .check(bodyString.saveAs("resp"))
    )
    .pause(1)
    .exec { session =>
      if (System.getProperty("gatling.debug") == "true") {
        println("RESP: " + session("resp").asOption[String].getOrElse("<no body>"))
      }
      session
    }

  // 2️. Feeder desde CSV con datos de cuentas y montos
  val feeder = csv("transacciones.csv").circular
  // Formato esperado del CSV:
  // fromAccountId,toAccountId,amount
  // 13899,14010,100

  // 3️. Escenario de transferencia bancaria
  val scn = scenario("Transferencias Concurrentes")
    .feed(feeder)
    .exec(
      http("transferencia")
        .post("/transfer")
        .queryParam("fromAccountId", "${fromAccountId}")
        .queryParam("toAccountId", "${toAccountId}")
        .queryParam("amount", "${amount}")
        .check(status.is(200))
    )

  // 4️. Inyección de carga para cumplir criterio de 150 TPS sostenidos
  val injectionProfile = Seq(
    rampUsersPerSec(50) to 150 during (30.seconds), // Calentamiento progresivo
    constantUsersPerSec(150) during (2.minutes)     // Mantener 150 TPS
  )

  // 5️. Assertions para validar estabilidad bajo estrés
  setUp(
    scn.inject(injectionProfile).protocols(httpConf)
  ).assertions(
    global.successfulRequests.percent.gte(99),   // No deben perderse transferencias
    global.responseTime.percentile(95).lte(2000) // Opcional: latencia aceptable
  )
}
