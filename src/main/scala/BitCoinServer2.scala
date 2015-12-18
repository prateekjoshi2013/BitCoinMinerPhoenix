
import akka.actor._
import collection.mutable
import java.security.MessageDigest
import akka.routing.BalancingPool
import akka.routing._
import akka.util._
import com.typesafe.config.ConfigFactory
import java.io.File
import akka.pattern.Patterns._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

/**
 * @author Prateek
 */
object BitCoinServer2 extends App {

  case class generateBitCoins(k: Int)
  case class mineBitCoins(k: Int, id: String)
  case class BitCoinArray(a: mutable.ArrayBuffer[String])

  val configFile = getClass.getClassLoader.getResource("local_application.conf").getFile
  //parse the config
  val config = ConfigFactory.parseFile(new File(configFile))
  println(config)
  //create an actor system with that config
  val system = ActorSystem("BitCoinServerEnv", config)
  //create a remote actor from actorSystem
  val server = system.actorOf(Props(new BitCoinServerActor), "BitCoinServer")
  // val remote = system.actorOf(Props[], name="remote")
  println("BitcoinServer is  ready")

  server ! "START"

  class BitCoinServerActor extends Actor {

    var client = 0
    var k = 0
    var start_time = System.currentTimeMillis()
    var attempts = 0

    var myBitCoinMap = mutable.ArrayBuffer.empty[String]
    var myLocalBitCoinMap = mutable.ArrayBuffer.empty[String]

    var results = 0
    val id = "prateekjoshi2013"

    def receive = {
      case "START" =>
        {
          println("enter  number of zeroes")
          k = readInt()

          start_time = System.currentTimeMillis()

          server ! generateBitCoins(k)

        }
      case 1 => {
        sender ! k
        client = 1
        println("sender")
      }

      case "increment" => {
        attempts += 1
        // println("attempting......."+attempts) 
        //  if ( System.currentTimeMillis()-start_time>30000 || attempts < 100000*4)
        //   { 
        //   println("total_time->"+(System.currentTimeMillis()-start_time))
        //  system.shutdown() 

        // }

      }
      //context.stop(self)

      case a: mutable.ArrayBuffer[String] => {

        myBitCoinMap ++= a

        results += 1
        if (a.nonEmpty) {
          println("coins mined from remote client->")
          for (i <- 0 to a.length - 1)
            println(a(i))
        }
        if (System.currentTimeMillis() - start_time > 30000 && results > 1) {
          // println("coins mined from remote client->" + a)
          println("total_time->" + (System.currentTimeMillis() - start_time))
          system.shutdown()

        }

      }

      case generateBitCoins(k) => //assign work
        {
          var BitCoinString = ""

          var processors = Runtime.getRuntime().availableProcessors();
          var nthreads = (3 * processors)
          println("no. of processors->" + processors)
          val remoteBitCoinClientServer = context.actorSelection("akka.tcp://BitCoinRemoteSystemEnv@127.0.0.1:5000/user/BitCoinRemoteServer")

          implicit val ec = system.dispatcher

          {

            {

             // println("Remote is not active ")

              val WorkerActor = context.actorOf(Props[ServerLocalWorker].withRouter(RoundRobinRouter(nthreads)))
              for (i <- 1 to nthreads) {

                WorkerActor ! mineBitCoins(k, "prateekjoshi2013")

              }

            }
          }

          //end of generateCoin 
        }

      case mineBitCoins(k, id) => {
        var i = 1
        //  println("inside mining worker")
        while (System.currentTimeMillis() - start_time < 30000) {
          var BitCoinRandomString = scala.util.Random.alphanumeric.take(5).mkString
          i = i + 1

          val sha = MessageDigest.getInstance("SHA-256")

          //      println("creating SHA hashes"  )
          val stringwithseed = id + BitCoinRandomString
          sha.update(stringwithseed.getBytes("UTF-8"))
          val digest = sha.digest();
          val hexString = new StringBuffer();
          for (j <- 0 until digest.length - 1) {
            val hex = Integer.toHexString(0xff & digest(j));
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
          }

          //println("incrementing attempts")
          server ! "increment"

          var no_zeros = 0
          for (i <- 0 to k - 1) {
            if (hexString.toString().charAt(i) == '0') {
              no_zeros += 1
              if (no_zeros == k) {

                myLocalBitCoinMap += (hexString.toString + "found by local worker:" + "->" + stringwithseed)
                //--   println(myLocalBitCoinMap)
                //coin_found(hexString.toString,stringwithseed+"found by local worker:")
              }
            }
          }

          //end for loop
        }
        println("worker job complete")
        server ! BitCoinArray(myLocalBitCoinMap)
        server ! myLocalBitCoinMap
        server ! "increment"
        //context.stop(self)
      }

      case x: ActorRef => { x ! k }

      case BitCoinArray(m) => {

        myBitCoinMap ++= m

        if (System.currentTimeMillis() - start_time > 30000 && results > 1) {
          println("results" + results)
          println("total_time->" + (System.currentTimeMillis() - start_time))
          system.shutdown()

        }
        //   println("current bitcoinmap:"+myBitCoinMap)          
      }

      //end of def recieve
    }

    //end of BitCoinServerActor
  }

  class ServerLocalWorker extends Actor {
    //val timeout = Timeout()
    val initial_Time = System.currentTimeMillis()
    val myLocalBitCoinMap = mutable.ArrayBuffer.empty[String]
    var BitCoinRandomString = ""

    def receive = {

      case mineBitCoins(k, id) => {
        var i = 1
        //   println("inside mining worker")
        while (i <= 100000 && System.currentTimeMillis() - initial_Time < 300000) {
          BitCoinRandomString = scala.util.Random.alphanumeric.take(5).mkString
          i = i + 1

          val sha = MessageDigest.getInstance("SHA-256")

          //      println("creating SHA hashes"  )
          val stringwithseed = id + BitCoinRandomString
          sha.update(stringwithseed.getBytes("UTF-8"))
          val digest = sha.digest();
          val hexString = new StringBuffer();
          for (j <- 0 until digest.length - 1) {
            val hex = Integer.toHexString(0xff & digest(j));
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
          }

          //println("incrementing attempts")
          //  server ! "increment"

          var no_zeros = 0
          for (i <- 0 to k - 1) {
            if (hexString.toString().charAt(i) == '0') {
              no_zeros += 1
              if (no_zeros == k) {
                // println("BitCoin found updating bitcoinmap")
                myLocalBitCoinMap += (stringwithseed + hexString.toString)
                println(stringwithseed + " " + hexString.toString)
                //coin_found(hexString.toString,stringwithseed+"found by local worker:")
              }
            }
          }

          //end for loop
        }

        server ! BitCoinArray(myLocalBitCoinMap)
        //myLocalBitCoinMap

        //context.stop(self)
      }

    }

  }

  //end BitCoinServer

}