package com.example
import com.pi4j.io.gpio._
import java.net._
import scala.xml._
import scala.io.Source
import java.io.InputStream
import com.sun.org.apache.xml.internal.security.utils.Base64
import com.typesafe.config._

object Driver {
  val gpio = GpioFactory.getInstance();
  val red_led = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04)
  val blue_led = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06)
  val green_led = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01)

  val conf = ConfigFactory.load();

  val username = conf.getString("username")
  val password = conf.getString("password")

  def getStream(username: String, password: String): InputStream = {
    val url = new URL("https://mail.google.com/gmail/feed/atom/%5Eiim")
    val uc = url.openConnection()
    val userpass = username + ":" + password
    val basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());
    uc.setRequestProperty("Authorization", basicAuth)
    uc.getInputStream()
  }

  def fetch_new_mails: Int = {
    val xmlString = Source.fromInputStream(getStream(username, password)).mkString
    val xml = XML.loadString(xmlString)
    (xml \\ "feed" \\ "fullcount").map(e => e.text.toInt).head
  }

  def main(args: Array[String]) {
    while (true) {
      if (fetch_new_mails > 0) {
        red_led.low
        green_led.high
      } else {
        green_led.low
        red_led.high
      }
      Thread.sleep(60000) // 60 second pause between checking
    }
  }
}
