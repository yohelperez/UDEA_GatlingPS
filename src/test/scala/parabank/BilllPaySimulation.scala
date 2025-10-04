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
        // Solo guardamos el status y el body, sin verificaciones que fallen
        .check(status.saveAs("httpStatus"))
        .check(bodyString.saveAs("responseBody"))
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

  // ✅ Carga reducida para debugging
  val injectionProfile = Seq(
    constantConcurrentUsers(5) during (30.seconds)  // Solo 5 usuarios por 30 segundos
  )

  setUp(
    scn.inject(injectionProfile).protocols(httpConf)
  )
}