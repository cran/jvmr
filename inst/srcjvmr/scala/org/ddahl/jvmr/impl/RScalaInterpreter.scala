package org.ddahl.jvmr.impl

import java.io._

class RScalaInterpreter private (settings : scala.tools.nsc.Settings, val outputBuffer : ByteArrayOutputStream, val outputPrintStream : PrintStream) extends scala.tools.nsc.interpreter.IMain(settings) {

  def getOutput : String = {
    outputPrintStream.flush
    val result = outputBuffer.toString("UTF8")
    outputBuffer.reset
    result
  }

  def update(name : String, x : Any, boundType : String) = bind(name,boundType,x)
  def update(name : String, x : Int) = bind(name,x)
  def update(name : String, x : Float) = bind(name,x)
  def update(name : String, x : Double) = bind(name,x)
  def update(name : String, x : String) = bind(name,x)
  def update(name : String, x : Array[Int]) = bind(name,"Array[Int]",x)
  def update(name : String, x : Array[Float]) = bind(name,"Array[Float]",x)
  def update(name : String, x : Array[Double]) = bind(name,"Array[Double]",x)
  def update(name : String, x : Array[String]) = bind(name,"Array[String]",x)
  def update(name : String, x : Array[Array[Int]]) = bind(name,"Array[Array[Int]]",x)
  def update(name : String, x : Array[Array[Float]]) = bind(name,"Array[Array[Float]]",x)
  def update(name : String, x : Array[Array[Double]]) = bind(name,"Array[Array[Double]]",x)
  def update(name : String, x : Array[Array[String]]) = bind(name,"Array[Array[String]]",x)

}

object RScalaInterpreter {

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
    result
  }

}

