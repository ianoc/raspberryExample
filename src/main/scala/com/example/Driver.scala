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
  val cfg_map = List(RaspiPin.GPIO_00, RaspiPin.GPIO_01, RaspiPin.GPIO_02,
    RaspiPin.GPIO_03, RaspiPin.GPIO_04, RaspiPin.GPIO_05, RaspiPin.GPIO_06,
    RaspiPin.GPIO_07, RaspiPin.GPIO_08, RaspiPin.GPIO_09, RaspiPin.GPIO_10,
    RaspiPin.GPIO_11, RaspiPin.GPIO_12, RaspiPin.GPIO_13, RaspiPin.GPIO_14,
    RaspiPin.GPIO_15, RaspiPin.GPIO_16, RaspiPin.GPIO_17, RaspiPin.GPIO_18,
    RaspiPin.GPIO_19, RaspiPin.GPIO_20).map{case x => (x.getName(), x)}.toMap
    
  val conf = ConfigFactory.load();
  def loadPin(pin_name:String): GpioPinDigitalOutput = {
    val led_enum = cfg_map.getOrElse(conf.getString(pin_name), null)
    if(led_enum == null) throw new Exception("Not found led") else gpio.provisionDigitalOutputPin(led_enum)
  }
  
  val red_led = loadPin("red_led")
  val blue_led = loadPin("blue_led")
  val green_led = loadPin("green_led")

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
