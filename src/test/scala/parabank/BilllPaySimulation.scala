package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class BillPaySimulation extends Simulation {

  // 1. HTTP configuration
  val httpConf = http
    .baseUrl("https://parabank.parasoft.com/parabank/services/bank")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // 2. Feeder desde archivo CSV
  val feeder = csv("billpay-data.csv").circular

  // 3. Scenario: Solo billpay (sin login)
  val scn = scenario("BillPayUnderLoad")
    .feed(feeder)
    .exec(
      http("Bill Payment")
        .post("/billpay")
        // Query Parameters
        .queryParam("accountId", "${accountId}")
        .queryParam("amount", "${amount}")
        // JSON Body
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
        // Verificaciones específicas basadas en la respuesta real
        .check(jsonPath("$.payeeName").is("${payeeName}"))
        .check(jsonPath("$.amount").is("${amount}"))
        .check(jsonPath("$.accountId").is("${accountId}"))
        .check(bodyString.saveAs("responseBody"))
    )
    .exec { session =>
      if (System.getProperty("gatling.debug") == "true") {
        println(s"=== BILL PAYMENT RESPONSE ===")
        println(s"Request - Account: ${session("accountId").as[String]}, Amount: ${session("amount").as[String]}")
        println(s"Response: ${session("responseBody").asOption[String].getOrElse("No response")}")
        println("=============================")
      }
      session
    }

  // 4. Load profile -> 200 usuarios concurrentes durante 10 minutos
  val injectionProfile = Seq(
    rampConcurrentUsers(0) to 200 during (120.seconds),  // ramp 0->200 en 2 min
    constantConcurrentUsers(200) during (1.minutes)     // steady-state 200 por 1 min
  )

  // 5. Setup + Assertions para criterios de aceptación
  setUp(
    scn.inject(injectionProfile).protocols(httpConf)
  ).assertions(
    // Criterio 1: Tiempo de respuesta por transacción <= 3 segundos (p95)
    details("Bill Payment").responseTime.percentile(95).lte(3000),
    
    // Criterio 2: Tasa de errores funcionales <= 1%
    global.failedRequests.percent.lte(1.0),
    
    // Verificación específica del endpoint de pago
    details("Bill Payment").failedRequests.percent.lte(1.0)
  )
}