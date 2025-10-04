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

  // ✅ CARGA MUY REDUCIDA para diagnosticar
  val injectionProfile = Seq(
    constantConcurrentUsers(5) during (1.minutes)  // Solo 5 usuarios por 1 minuto
  )

  setUp(
    scn.inject(injectionProfile).protocols(httpConf)
  ).assertions(
    details("Bill Payment").responseTime.percentile(95).lte(3000),
    global.failedRequests.percent.lte(1.0)
  )
}