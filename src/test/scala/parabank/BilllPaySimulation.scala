package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import java.time.Instant
import parabank.Data._

class BillPaySimulation extends Simulation {

  // 1 Http Conf
  val httpConf = http
    .baseUrl("https://parabank.parasoft.com/parabank/services/bank")
    .acceptHeader("application/json")
    .userAgentHeader("Gatling-Perf-Lab")
    .header("Content-Type", "application/json")

  // 2 Feeder desde archivo CSV (ubicado en src/test/resources/data/billpay-data.csv)
  val feeder = csv("billpay-data.csv").circular

  // 3 Scenario Definition - Flujo REAL de pago de facturas 
  val scn = scenario("BillPay")
    .exec(
      http("login")
        .get(s"/login/$username/$password")
        .check(status.is(200))
        .check(jsonPath("$.customer.id").saveAs("customerId"))
    )
    .pause(1.second, 3.seconds)
    
    .feed(feeder)
    .exec(
      http("BillPayment")
        .post("/billpay")
        .body(StringBody(
          """{
            "accountId": ${accountId},
            "amount": ${amount},
            "payeeName": "${payeeName}",
            "payeeAddress": "${payeeAddress}",
            "payeeCity": "${payeeCity}",
            "payeeState": "${payeeState}",
            "payeeZipCode": "${payeeZipCode}",
            "payeePhone": "${payeePhone}",
            "payeeAccountNumber": "${payeeAccountNumber}"
          }"""
        ))
        .check(status.is(200))
        .check(jsonPath("$.message").exists)
        .check(jsonPath("$.success").is("true"))
        .check(jsonPath("$.paymentId").saveAs("paymentId"))
    )
    .pause(1.second, 2.seconds)
    
    .exec(
      http("VerifyPaymentInHistory")
        .get("/accounts/${accountId}/transactions")
        .check(status.is(200))
        .check(jsonPath("$[?(@.id == '${paymentId}')].amount").is("${amount}"))
        .check(jsonPath("$[?(@.id == '${paymentId}')]").count.is(1)) // Verifica que no hay duplicados
        .check(bodyString.saveAs("paymentHistoryResp"))
    )
    .pause(1.second)
    
    .exec { session =>
      if (System.getProperty("gatling.debug") == "true") {
        println(s"BILL PAYMENT - Payment ID: ${session("paymentId").asOption[String].getOrElse("N/A")}")
        println("VERIFICATION RESP: " + session("paymentHistoryResp").asOption[String].getOrElse("<no body>"))
      }
      session
    }

  // 4 Load Scenario - 200 usuarios concurrentes (modelo cerrado)
  val injectionProfile = Seq(
    rampConcurrentUsers(0) to 200 during (120.seconds),
    constantConcurrentUsers(200) during (10.minutes)
  )

  // 5 Setup + Assertions para criterios de aceptación
  setUp(
    scn.inject(injectionProfile).protocols(httpConf)
  ).assertions(
    // Criterio 1: Tiempo de respuesta por transacción <= 3 segundos (p95)
    details("BillPayment").responseTime.percentile(95).lte(3000),
    
    // Criterio 2: Tasa de errores funcionales <= 1%
    global.failedRequests.percent.lte(1.0),
    
    // Verificaciones específicas por request
    details("BillPayment").failedRequests.percent.lte(1.0),
    details("VerifyPaymentInHistory").failedRequests.percent.lte(1.0)
  )
}