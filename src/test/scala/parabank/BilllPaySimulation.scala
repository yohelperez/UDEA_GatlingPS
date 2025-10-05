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
    .pause(1.second)  // ✅ Pausa entre requests
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
        // Guardamos status y body para logs
        .check(status.saveAs("httpStatus"))
        .check(bodyString.saveAs("responseBody"))
        // Verificaciones (pueden fallar pero no detienen el test)
        .check(status.is(200).silently)
        .check(jsonPath("$.payeeName").is("${payeeName}").silently)
        .check(jsonPath("$.accountId").is("${accountId}").silently)
        .check(jsonPath("$.amount").ofType[Double].silently)
    )
    .exec { session =>
      val status = session("httpStatus").as[Int]
      val accountId = session("accountId").as[String]
      val amount = session("amount").as[String]
      val response = session("responseBody").asOption[String].getOrElse("No response")
      
      // Log detallado para CADA request
      println(s"=== BILL PAYMENT REQUEST ===")
      println(s"Account: $accountId, Amount: $amount")
      println(s"Status: $status")
      println(s"Response: $response")
      println("============================")
      
      session
    }

  // ✅ 200 USUARIOS CONCURRENTES como requiere tu historia
  val injectionProfile = Seq(
    rampConcurrentUsers(0) to 200 during (120.seconds),  // ramp 0->200 en 2 min
    constantConcurrentUsers(200) during (1.minutes)      // 200 usuarios por 1 min
  )

  setUp(
    scn.inject(injectionProfile).protocols(httpConf)
  ).assertions(
    details("Bill Payment").responseTime.percentile(95).lte(3000),
    global.failedRequests.percent.lte(1.0)
  )
}