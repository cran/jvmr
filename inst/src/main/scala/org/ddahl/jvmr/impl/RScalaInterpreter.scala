package org.ddahl.jvmr.impl

import java.io._
import scala.reflect.runtime.universe._

class RScalaInterpreter private (settings : scala.tools.nsc.Settings, val outputBuffer : ByteArrayOutputStream, val outputPrintStream : PrintStream) {

  val interpreter = new scala.tools.nsc.interpreter.IMain(settings)
  type Request = interpreter.Request
  private var _lastResult : Request = null

  def interpret(line: String) = interpreter.interpret(line)
  def eval(line : String) = {
    val result = interpret(line)
    if ( result == scala.tools.nsc.interpreter.Results.Success ) _lastResult = interpreter.prevRequestList.last
    else _lastResult = null
    result
  }

  def getOutput : String = {
    outputPrintStream.flush
    val result = outputBuffer.toString("UTF8")
    outputBuffer.reset
    result
  }

  def lastResult : Option[Any] = if ( _lastResult == null ) None else Some(_lastResult.lineRep.call("$result"))

  def update(name : String, x : Any, boundType : String) = interpreter.bind(name,boundType,x)
  def update(name : String, x : Int) = interpreter.bind(name,x)
  def update(name : String, x : Float) = interpreter.bind(name,x)
  def update(name : String, x : Double) = interpreter.bind(name,x)
  def update(name : String, x : String) = interpreter.bind(name,x)
  def update(name : String, x : Array[Int]) = interpreter.bind(name,"Array[Int]",x)
  def update(name : String, x : Array[Float]) = interpreter.bind(name,"Array[Float]",x)
  def update(name : String, x : Array[Double]) = interpreter.bind(name,"Array[Double]",x)
  def update(name : String, x : Array[String]) = interpreter.bind(name,"Array[String]",x)

}

object RScalaInterpreter {

  val Version = "0.1-25"

  def apply() : RScalaInterpreter = {
    val settings = new scala.tools.nsc.Settings
    settings.usejavacp.value = true
    apply(settings)
  }

  def apply(classpath : String) : RScalaInterpreter = {
    val settings = new scala.tools.nsc.Settings
    settings.usejavacp.value = true
    settings.classpath.value = classpath
    apply(settings)
  }

  def apply(settings : scala.tools.nsc.Settings) : RScalaInterpreter = {
    val outputBuffer = new ByteArrayOutputStream()
    val outputPrintStream = new PrintStream(outputBuffer)
    System.setOut(outputPrintStream)
    System.setErr(outputPrintStream)
    val result = new RScalaInterpreter(settings,outputBuffer,outputPrintStream)
    result.interpret("val rjvm = \""+Version+"\"")
    result.getOutput
    result
  }

}
