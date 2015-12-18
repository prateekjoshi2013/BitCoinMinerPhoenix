

/**
 * @author Prateek
 */

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
object BitCoinClient2 extends App {
 
 
case class  generateBitCoins(k:Int)
case class mineBitCoins(k:Int,id:String)
case class BitCoinArray(a:mutable.ArrayBuffer[String])
case class mine(k:Int)
val configFile = getClass.getClassLoader.getResource("remote_application.conf").getFile
//parse the config

val config = ConfigFactory.parseFile(new File(configFile))
//create an actor system with that config
println(config)
val system = ActorSystem("BitCoinRemoteSystemEnv",config)
//create a remote actor from actorSystem
//
//val server = system.actorOf(Props(new RemoteLocalWorker),"BitCoinServer")

println("remote is ready")
println("which server do you want to connect to:")
                 val ip = readLine()

               println("BitcoinServer is  ready")



val remote = system.actorOf(Props[RemoteLocalWorker], name="BitCoinRemoteServer")
remote ! "START"



class RemoteLocalWorker extends Actor{
  //val timeout = Timeout()

  val initial_Time=System.currentTimeMillis()
      val  myLocalBitCoinMap= mutable.ArrayBuffer.empty[String]
          var BitCoinRandomString=""
          implicit val ec=system.dispatcher

          var processors = Runtime.getRuntime().availableProcessors();
  var nthreads=(3*processors)/2
      println("no. of processors->"+processors)
      val remoteBitCoinServer = context.actorSelection("akka.tcp://BitCoinServerEnv@"+ip+":2520/user/BitCoinServer")
      def receive={
     case "START"=>{

      val pingFuture = ask(remoteBitCoinServer,1, 1000000)

          pingFuture.onSuccess{
           case i =>{ var k=i.toString.toInt
             println("connection successful !!!")
             remote !mine(k)

           }


      }
      
          pingFuture.onFailure{
            case _=>  println("connection unsuccessful !!!")
          }



    } 


    /*       case a:ActorRef=>{
             server=a
           }*/
     
      case mine(k)=>

      { println("mining started") 


        val WorkerActor= context.actorOf(Props[RemoteLocalWorker].withRouter(RoundRobinRouter(1)))
        for(i<-1 to nthreads)
        {              
          println("mining started")        
          WorkerActor ! mineBitCoins(k,"prateekjoshi2013byRemoteWorker")

        }


      }






      case mineBitCoins(k,id)=>{
        var server =context.actorSelection("akka.tcp://BitCoinServerEnv@"+ip+":2520/user/BitCoinServer")
            var i=1
            println("inside mining worker")
            while (i<=10000 && System.currentTimeMillis()-initial_Time<30000){  
              BitCoinRandomString =scala.util.Random.alphanumeric.take(5).mkString
                  i=i+1



                  val sha = MessageDigest.getInstance("SHA-256")


                  //      println("creating SHA hashes"  )
                  val stringwithseed=id+BitCoinRandomString
                  sha.update(stringwithseed.getBytes("UTF-8"))  
                  val digest = sha.digest();
              val hexString = new StringBuffer();
              for ( j <- 0 until digest.length-1)   
              {
                val hex = Integer.toHexString(0xff & digest(j)); 
                if(hex.length() == 1) hexString.append('0'); 
                hexString.append(hex);
              }

              //println("incrementing attempts")
              server ! "increment"


              var no_zeros=0
              for(i <- 0 to k-1)
              {
                if(hexString.toString().charAt(i)=='0')
                { no_zeros +=1
                if(no_zeros==k)

                {
                  println("BitCoin found updating bitcoinmap")
                  myLocalBitCoinMap+=(hexString.toString+" "+stringwithseed)
                  //--   println(myLocalBitCoinMap)
                  //coin_found(hexString.toString,stringwithseed+"found by local worker:")
                }  
                }
              } 

              //end for loop
            }   
        println("worker job complete")
        //  server ! BitCoinArray(myLocalBitCoinMap)
        server ! myLocalBitCoinMap
        server ! "increment"
        //context.stop(self)
      }

  }

}

 
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
}