package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class BillPaySimulation extends Simulation {

  val httpConf = http
    .baseUrl("https://parabank.parasoft.com/parabank/services/bank")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val feeder = csv("billpay-data.csv").circular

  val scn = scenario("BillPayUnderLoad")
    .feed(feeder)
    .exec(
      http("Bill Payment")
        .post("/billpay")
        .queryParam("accountId", "${accountId}")
        .queryParam("amount", "${amount}")
        .body(StringBody(
          """{
            "name": "${payeeName}",
            "address": {
              "street": "${payeeAddress}",
              "city": "${payeeCity}", 
              "state": "${payeeState}",
              "zipCode": "${payeeZipCode}"
            },
            "phoneNumber": "${payeePhone}",
            "accountNumber": "${payeeAccountNumber}"
          }"""
        ))
        .check(status.is(200))
        .check(jsonPath("$.payeeName").is("${payeeName}"))
        .check(jsonPath("$.accountId").is("${accountId}"))
        .check(jsonPath("$.amount").ofType[Double])
    )

  // ✅ 2 minutos en estado estable
  val injectionProfile = Seq(
    rampConcurrentUsers(0) to 200 during (60.seconds),  // 1 minuto de rampa
    constantConcurrentUsers(200) during (2.minutes)     // 2 minutos estado estable
  )

  setUp(
    scn.inject(injectionProfile).protocols(httpConf)
  ).assertions(
    // Criterio 1: Tiempo de respuesta <= 3 segundos (p95)
    details("Bill Payment").responseTime.percentile(95).lte(3000),
    // Criterio 2: Tasa de errores <= 1%
    global.failedRequests.percent.lte(1.0)
  )
}