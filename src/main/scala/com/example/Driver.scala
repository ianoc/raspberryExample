package com.example
import com.pi4j.io.gpio._
import java.net._
import scala.xml._
import scala.io.Source
import java.io.InputStream
import com.sun.org.apache.xml.internal.security.utils.Base64
import com.typesafe.config._
import scala.actors.Actor
import scala.actors.Actor._

case object Update
case object Stop
case class NewMails(new_mails: Int)

class FlashActor(pins: GPIOInterface) extends Actor {
  def act() {
    loop {
      react {
        case flashes: Int =>
          for (i <- 1 to flashes) {
            pins.red.high
            Thread.sleep(200)
            pins.red.low
            Thread.sleep(200)
          }
        case Stop =>
          exit
      }
    }
  }
}

class FetchActor(username: String, password: String) extends Actor {
  private def getStream(): InputStream = {
    val url = new URL("https://mail.google.com/gmail/feed/atom/%5Eiim")
    val uc = url.openConnection()
    val userpass = username + ":" + password
    val basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());
    uc.setRequestProperty("Authorization", basicAuth)
    uc.getInputStream()
  }

  private def fetch_new_mails: Int = {
    val xmlString = Source.fromInputStream(getStream()).mkString
    val xml = XML.loadString(xmlString)
    (xml \\ "feed" \\ "fullcount").map(e => e.text.toInt).head
  }

  def act() {
    while (true) {
      loop {
        react {
          case Update =>
            sender ! NewMails(fetch_new_mails)
          case Stop =>
            exit
        }
      }
    }
  }
}

class SystemActor(fetch_actor: FetchActor, flash_actor: FlashActor, pins: GPIOInterface) extends Actor {
  var num_new_mails: Int = 0
  var last_update: Long = 0
  def act() {
    while (true) {
      loop {
        react {
          case Update =>
            val now_time = System.currentTimeMillis
            if (now_time - last_update >= 60000) { fetch_actor ! Update; last_update = now_time }
            flash_actor ! num_new_mails
          case NewMails(mails) =>
            if (mails > 0) pins.green.high else pins.green.low
            num_new_mails = mails
          case Stop =>
            exit
        }
      }
    }
  }
}

class GPIOInterface(conf: Config) {
  private val gpio = GpioFactory.getInstance();
  private val cfg_map = List(RaspiPin.GPIO_00, RaspiPin.GPIO_01, RaspiPin.GPIO_02,
    RaspiPin.GPIO_03, RaspiPin.GPIO_04, RaspiPin.GPIO_05, RaspiPin.GPIO_06,
    RaspiPin.GPIO_07, RaspiPin.GPIO_08, RaspiPin.GPIO_09, RaspiPin.GPIO_10,
    RaspiPin.GPIO_11, RaspiPin.GPIO_12, RaspiPin.GPIO_13, RaspiPin.GPIO_14,
    RaspiPin.GPIO_15, RaspiPin.GPIO_16, RaspiPin.GPIO_17, RaspiPin.GPIO_18,
    RaspiPin.GPIO_19, RaspiPin.GPIO_20).map { case x => (x.getName(), x) }.toMap
  private def loadPin(pin_name: String): GpioPinDigitalOutput = {
    val led_enum = cfg_map.getOrElse(conf.getString(pin_name), null)
    if (led_enum == null) throw new Exception("Not found led") else gpio.provisionDigitalOutputPin(led_enum)
  }
  val blue = loadPin("blue_led")
  val green = loadPin("green_led")
  val red = loadPin("red_led")
  def Stop = {
    blue.low
    red.low
    green.low
  }
}

object Driver {
  val conf = ConfigFactory.load();
  val username = conf.getString("username")
  val password = conf.getString("password")

  val pins = new GPIOInterface(conf)

  val flash_actor = new FlashActor(pins)

  val fetch_actor = new FetchActor(username, password)

  val system_actor = new SystemActor(fetch_actor, flash_actor, pins)

  sys.addShutdownHook({
    system_actor ! Stop
    flash_actor ! Stop
    fetch_actor ! Stop
    pins.Stop
  })

  def main(args: Array[String]) {
    fetch_actor.start
    flash_actor.start
    system_actor.start
    pins.blue.high // System online
    while (true) {
      system_actor ! Update
      Thread.sleep(3000) // Redraw every 60 seconds
    }
  }
}
