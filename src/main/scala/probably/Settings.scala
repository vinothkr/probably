package probably

import com.typesafe.config.Config

class Settings(config:Config) {
  val httpHost = "0.0.0.0"
  val httpPort = config.getInt("probably.http.port")
  val expectedErrorPercent = config.getDouble("probably.expectedErrorPercent")
}
