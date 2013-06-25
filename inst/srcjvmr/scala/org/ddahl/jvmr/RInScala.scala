package org.ddahl.jvmr

import java.io._
import java.net._

import scala.sys.process._
import scala.reflect.ClassTag
import scala.language.dynamics

/** Allows the use of R's statistical methods within Scala by creating a high-level bridge between Scala and R.  
  * An instance of this class may be created using its companion object. 
  * Multiple interpreters are permitted and each interpreter maintains its own
  * workspace and memory.
  * 
  * A paper describing the software in detail is available here: [[http://dahl.byu.edu/software/jvmr/ http://dahl.byu.edu/software/jvmr/]]
  *
  * ===Example:===
  * {{{
  * scala> import org.ddahl.jvmr.RInScala
  * scala> val R = RInScala()
  *
  * scala> val zScore = -1.7
  * 
  * scala> val Pvalue = R.toPrimitive[Double]("pnorm(%s,0,1)".format(zScore))
  * Pvalue: Double = 0.04456546275854304  
  * }}}
  * @author David B. Dahl
  */ 
class RInScala private (command : Array[String]) extends Dynamic {

  private def this(command : String) = this(command +: RInScala.defaultArguments)
  private def this() = this(RInScala.defaultExecutable)


  /** For developer use only.  If true, R variables will be checked to ensure they follow
    * R's variable naming conventions in the [[RInScala!.update(name:String,value:Any)* update(name:String,value:Any)]] function. The default value is true.
    */
  var validateNamesForAssignment = true

  /** Developer feature only.  If true, all R code that is called will be printed in Scala.
    * The default value is false. 
    */
  var debug = false

  /** If true, any information retrieved from R will have its own color scheme (only on non-Windows machines).  
    * This includes the prompt method as well as any
    * information retrieved when debug = true.  
    */
  var colorize = ( RInScala.OS != "windows" )

  private def reader(labelPre : String, labelPost : String)(input : InputStream) = {
    val in = new BufferedReader(new InputStreamReader(input))
    var line = in.readLine()
    while ( line != null ) {
      if ( debug ) {
        if ( colorize ) println(labelPre + line + labelPost)
        else println(line)
      }
      line = in.readLine()
    }
    in.close()
  }

  private var cmdOut : PrintWriter = null
  private val processBuilder = Process(command)
  private val processIO = new ProcessIO(
    o => {cmdOut = new PrintWriter(o)},
    reader(RInScala.COLOR_GREENF,RInScala.COLOR_RESET),
    reader(RInScala.COLOR_REDF,RInScala.COLOR_RESET),
    true
  )
  private val processInstance = processBuilder.run(processIO)

  private val serverDataSocket : ServerSocket = new ServerSocket(0, 0, InetAddress.getByName("localhost"))
  private val serverTextSocket : ServerSocket = new ServerSocket(0, 0, InetAddress.getByName("localhost"))

  while ( cmdOut == null ) Thread.sleep(100)
  
  cmdOut.println("""
.RInScala.dataSocket <- socketConnection(host="localhost", port=%d, open="r+b", blocking=TRUE)
.RInScala.textSocket <- socketConnection(host="localhost", port=%d, open="r+", blocking=TRUE)
.RInScala.CODE_OK <- as.integer(%d)
.RInScala.CODE_ERROR <- as.integer(%d)
.RInScala.CODE_STRING_VECTOR <- as.integer(%d)
.RInScala.CODE_STRING_MATRIX <- as.integer(%d)
.RInScala.CODE_DOUBLE_VECTOR <- as.integer(%d)
.RInScala.CODE_DOUBLE_MATRIX <- as.integer(%d)
.RInScala.CODE_INT_VECTOR <- as.integer(%d)
.RInScala.CODE_INT_MATRIX <- as.integer(%d)
.RInScala.CODE_BOOLEAN_VECTOR <- as.integer(%d)
.RInScala.CODE_BOOLEAN_MATRIX <- as.integer(%d)
""".format(serverDataSocket.getLocalPort,serverTextSocket.getLocalPort,
           RInScala.CODE_OK,RInScala.CODE_ERROR,
           RInScala.CODE_STRING_VECTOR,RInScala.CODE_STRING_MATRIX,
           RInScala.CODE_DOUBLE_VECTOR,RInScala.CODE_DOUBLE_MATRIX,
           RInScala.CODE_INT_VECTOR,RInScala.CODE_INT_MATRIX,
           RInScala.CODE_BOOLEAN_VECTOR,RInScala.CODE_BOOLEAN_MATRIX))

  cmdOut.println("""
.RInScala.getCleanErrorMessage <- function() {
  sub("^Error[^:]*:\\s+(.*)\\n$","\\1",geterrmessage())
}

.RInScala.sendError <- function() {
  writeBin(.RInScala.CODE_ERROR,con=.RInScala.dataSocket,endian="big")
  .RInScala.sendCharacterVector(.RInScala.getCleanErrorMessage(),FALSE)
}

.RInScala.eval <- function() {
  cmds <- readLines(n=.RInScala.nLinesToRead)
  x <- try(eval(parse(text=cmds),env=.GlobalEnv))
  if ( inherits(x,"try-error") ) .RInScala.sendError()
  else writeBin(.RInScala.CODE_OK,con=.RInScala.dataSocket,endian="big")
  flush(.RInScala.dataSocket)
  flush(.RInScala.textSocket)
}

.RInScala.sendCharacterVector <- function(x,include.code=TRUE) {
  if ( include.code ) writeBin(.RInScala.CODE_STRING_VECTOR,con=.RInScala.dataSocket,endian="big")
  writeBin(length(x),con=.RInScala.dataSocket,endian="big")
  x <- gsub("\r\n","\n",x)
  nNewLinesPerItem <- as.integer(nchar(x)-nchar(gsub("\n","",x)))
  writeBin(nNewLinesPerItem,con=.RInScala.dataSocket,endian="big")
  writeLines(x,con =.RInScala.textSocket,sep="\n",useBytes=TRUE)
}

.RInScala.sendCharacterMatrix <- function(x) {
  writeBin(.RInScala.CODE_STRING_MATRIX,con=.RInScala.dataSocket,endian="big")
  writeBin(nrow(x),con=.RInScala.dataSocket,endian="big")
  if ( nrow(x) > 0 ) {
    for ( i in 1:nrow(x) ) {
      .RInScala.sendCharacterVector(x[i,])
    }
  }
}

.RInScala.sendVector <- function(x,code,size) {
  if ( code == .RInScala.CODE_STRING_VECTOR ) .RInScala.sendCharacterVector(x)
  else {
    writeBin(code,con=.RInScala.dataSocket,endian="big")
    writeBin(length(x),con=.RInScala.dataSocket,endian="big")
    writeBin(x,con=.RInScala.dataSocket,endian="big",size=size)
  }
}

.RInScala.sendMatrix <- function(x,code,size) {
  if ( code == .RInScala.CODE_STRING_MATRIX ) .RInScala.sendCharacterMatrix(x)
  else {
    writeBin(code,con=.RInScala.dataSocket,endian="big")
    writeBin(dim(x),con=.RInScala.dataSocket,endian="big")
    if ( nrow(x) > 0 ) {
      for ( i in 1:nrow(x) ) {
        writeBin(x[i,],con=.RInScala.dataSocket,endian="big",size=size)
      }
    }
  }
}

.RInScala.send <- function(x,code,size=NA_integer_) {
  if ( is.vector(x) ) .RInScala.sendVector(x,code,size)
  else if ( is.matrix(x) ) .RInScala.sendMatrix(x,code+as.integer(1),size)
  else .RInScala.sendError()
}

.RInScala.pull <- function() {
  cmds <- readLines(n=.RInScala.nLinesToRead)
  x <- try(eval(parse(text=cmds),env=.GlobalEnv))
  if ( inherits(x,"try-error") ) .RInScala.sendError()
  else {
    if ( is.character(x) ) .RInScala.send(x,.RInScala.CODE_STRING_VECTOR)
    else if ( is.double(x) ) .RInScala.send(x,.RInScala.CODE_DOUBLE_VECTOR)
    else if ( is.integer(x) ) .RInScala.send(x,.RInScala.CODE_INT_VECTOR)
    else if ( is.logical(x) ) .RInScala.send(x,.RInScala.CODE_BOOLEAN_VECTOR,1)
    else .RInScala.sendError()
  }
  flush(.RInScala.dataSocket)
  flush(.RInScala.textSocket)
}

.RInScala.capture <- function() {
  cmds <- readLines(n=.RInScala.nLinesToRead)
  x <- try(unlist(lapply(parse(text=cmds),function(x) {capture.output(eval(x,env=.GlobalEnv))})))
  if ( inherits(x,"try-error") ) .RInScala.sendError()
  else .RInScala.sendCharacterVector(x)
  flush(.RInScala.dataSocket)
  flush(.RInScala.textSocket)
}

.RInScala.isValidVariableName <- function(x) {
  if ( length(x) != 1 ) return(FALSE)
  # Don't allow reserved words
      if ( x == "..." ) return(FALSE)
      if ( grepl("^\\.{2}[[:digit:]]+$", x) ) return(FALSE)
  if ( x != make.names(x) ) return(FALSE)
  TRUE
}
""")
  cmdOut.flush()

  private val clientDataSocket = serverDataSocket.accept()
  private val clientTextSocket = serverTextSocket.accept()
  private val dataIn  = new DataInputStream( new BufferedInputStream( clientDataSocket.getInputStream ))
  private val dataOut = new DataOutputStream(new BufferedOutputStream(clientDataSocket.getOutputStream))
  private val textIn  = new BufferedReader(new InputStreamReader(clientTextSocket.getInputStream))
  private val textOut = new PrintWriter(clientTextSocket.getOutputStream)




  // Shut everything down

  /** Closes the R interpreter for this `RInScala` object. The `RInScala` object will still exist, 
    * but it will no longer interact with R. 
    */
  def quit() = {
    dataOut.close
    textOut.close
    dataIn.close
    textIn.close
    clientDataSocket.close
    clientTextSocket.close
    serverDataSocket.close
    serverTextSocket.close
    cmdOut.println("close(.RInScala.dataSocket)")
    cmdOut.println("close(.RInScala.textSocket)")
    cmdOut.flush()
    cmdOut.close()
  }

  class REvaluationException(message: String = null, cause: Throwable = null) extends RuntimeException(message, cause)

  private def throwRException(message : String = null) = {
    if ( message == null ) throw new REvaluationException(pullStringVector().mkString("\n"))
    else throw new REvaluationException(message)
  }



  // Evaluation without text capture

  private def send(precursor : String, expression : String) = {
    val result = expression.replaceAll("\r\n","\n").split("\n")
    cmdOut.println(""".RInScala.nLinesToRead <- %d""".format(result.length))
    cmdOut.println(precursor)
    cmdOut.println(result.mkString(RInScala.NEWLINE))
    cmdOut.flush()
  }


  /** Evaluates R code. Can process multiple lines. 
    * @param expression an R expression
    * ===Example:===
    * {{{
    * scala> val R = RInScala()
    *
    * // Primitive line 
    * scala> R.eval("set.seed(24)")
    * 
    * // Multi-line
    * scala> R.eval("""
    *      |   draws <- rgamma(100,1,2.3)
    *      |   stdev <- sd(draws)
    *      | """)
    * 
    * scala> print(R.capture("stdev"))
    * [1] 0.4903072
    * }}}
    */
  def eval(expression : String) : Unit = {
    send(".RInScala.eval()",expression)
    if ( dataIn.readInt() == RInScala.CODE_ERROR ) throwRException()
  }


  // Evaluation with text capture

  // Sys.getlocale("LC_CTYPE")  --- Make sure encoding match!

  /** Evaluates one statement in R and returns a `String` of the R output to Scala.
    * @param expression an R expression
    * ===Example:===
    * {{{
    * scala> val R = RInScala()
    * 
    * scala> R>"""
    *      |   set.seed(24) 
    *      |   samp <- rnorm(5) 
    *      | """)
    * 
    * scala> print(R.capture("samp"))
    * [1] -0.5458808  0.5365853  0.4196231 -0.5836272  0.8474600
    * }}}
    */
  def capture(expression : String) : String = {
    send(".RInScala.capture()",expression)
    dataIn.readInt() match {
      case RInScala.CODE_ERROR => throwRException()
      case _ => pullStringVector().mkString("\n")
    }
  }

  /** Returns the version of R associated with this 'RInScala' instance. */
  lazy val version : String = toPrimitive[String]("R.version.string")

  // Pull values from R into Scala

  private def pullStringVector() : Array[String] = {
    val n = dataIn.readInt()
    val nNewLinesPerItem = new Array[Int](n)
    for ( i <- 0 until n ) nNewLinesPerItem(i) = dataIn.readInt()
    val result = new Array[String](n)
    for ( i <- 0 until n ) {
      val builder = new StringBuilder()
      for ( j <- 0 until nNewLinesPerItem(i) ) {
        builder.append(textIn.readLine())
        builder.append(RInScala.NEWLINE)
      }
      builder.append(textIn.readLine())
      result(i) = builder.toString
    }
    result
  }

  private def pullStringMatrix() : Array[Array[String]] = {
    val nRows = dataIn.readInt()
    val result = new Array[Array[String]](nRows)
    for ( i <- 0 until nRows ) {
      dataIn.readInt()  // Skip code
      result(i) = pullStringVector()
    }
    result
  }

  private def pullVector[A : ClassTag](read : () => A) : Array[A] = {
    val n = dataIn.readInt()
    val result = new Array[A](n)
    for ( i <- 0 until n ) {
      result(i) = read()
    }
    result
  }

  private def pullMatrix[A : ClassTag](read : () => A) : Array[Array[A]] = {
    val nRows = dataIn.readInt()
    val nCols = dataIn.readInt()
    val result = new Array[Array[A]](nRows)
    for ( i <- 0 until nRows ) {
      val x = new Array[A](nCols)
      for ( j <- 0 until nCols ) {
        x(j) = read()
      }
      result(i) = x
    }
    result
  }


  /** Pulls a single R value to Scala. Returns a Scala representation of a single R value.
    * @param expression an R expression
    * ===Example:===
    * {{{
    * scala> val R = RInScala()
    * 
    * scala> print(R.toPrimitive[Double]("pnorm(1.75,0,1)"))
    * 0.9599408431361829 
    * }}}
    */
  def toPrimitive[A](expression : String) : A = {
    val result = toVector[A](expression)
    if ( result.length != 1 ) throwRException("Length is not 1.")
    result(0)
  }

  /** Pulls a vector from R into Scala.  Returns a Scala array representation of an R vector.
    * @param expression an R expression 
    * ===Example:===
    * {{{
    * scala> val R = RInScala()
    * 
    * scala> R.toVector[Double]("dnorm(seq(-1,1,1))")
    * res1: Array[Double] = Array(0.24197072451914337, 0.3989422804014327, 0.24197072451914337)
    * }}}
    */
  def toVector[A](expression : String) : Array[A] = {
    apply(expression).asInstanceOf[Array[A]]
  }

  /** Pulls a matrix from R into Scala. Returns a Scala two-dimensional array representation of an R matrix. 
    * @param expression an R expression
    * ===Example:===
    * {{{
    * scala> val R = RInScala()
    * 
    * scala> R.eval("""
    *      |   n <- 3
    *      |   set.seed(24)
    *      |   alpha <- rnorm(n,10,1)
    *      | 
    *      |   set.seed(24)
    *      |   beta <- rbeta(n,2,1)
    *      | 
    *      |   set.seed(24)
    *      |   draws <- rgamma(n,alpha,beta)
    *      | 
    *      |   mtx <- matrix(draws,nrow=n,ncol=3)
    *      |   mtx[,2] <- alpha
    *      |   mtx[,3] <- beta
    *      | """)
    * 
    * scala> val scalaMTX = R.toMatrix[Double]("mtx")
    * scalaMTX: Array[Array[Double]] = Array(Array(16.33566814089132, 9.454119241633974, 0.4526997563896996), 
    *                                        Array(12.326618627315444, 10.536585304107613, 0.8264446296916081), 
    *                                        Array(18.630572622283808, 10.419623148618683, 0.7970790261165976)) 
    * }}}
    */
  def toMatrix[A](expression : String) : Array[Array[A]] = {
    apply(expression).asInstanceOf[Array[Array[A]]]
  }

  /** Not intended for use by user; see the [[RInScala.apply(expression:String)* apply(expression:String)]] and 
    * [[RInScala.apply(symbol:Symbol)* apply(symbol:Symbol)]] methods.
    */
  def selectDynamic(expression : String) : Any = {
    send(".RInScala.pull()",expression)
    dataIn.readInt() match {
      case RInScala.CODE_ERROR          => throwRException()
      case RInScala.CODE_STRING_VECTOR  => pullStringVector()
      case RInScala.CODE_STRING_MATRIX  => pullStringMatrix()
      case RInScala.CODE_DOUBLE_VECTOR  => pullVector(dataIn.readDouble)
      case RInScala.CODE_DOUBLE_MATRIX  => pullMatrix(dataIn.readDouble)
      case RInScala.CODE_INT_VECTOR     => pullVector(dataIn.readInt)
      case RInScala.CODE_INT_MATRIX     => pullMatrix(dataIn.readInt)
      case RInScala.CODE_BOOLEAN_VECTOR => pullVector(dataIn.readBoolean)
      case RInScala.CODE_BOOLEAN_MATRIX => pullMatrix(dataIn.readBoolean)
      case e => throwRException("Unrecognized code: "+e)
    }
  }


  /** Evaluates the expression in R and returns an object of type `Any` to Scala. 
    * @param expression an R expression
    * ==Example:==
    * {{{
    * scala> val R = RInScala()
    *
    * scala> R.apply("a <- 3")       // equivalent to R("a <- 3")
    * res0: Any = Array(3.0)
    * 
    * scala> R.apply("a")            // equivalent to R.a
    * res1: Any = Array(3.0)
    * 
    * print(R.capture("a"))
    * [1] 3
    * 
    * scala> R.apply("matrix(c(1,2,3,4),nrow=2,ncol=2)") // Equivalent to: R("matrix(c(1,2,3,4),nrow=2,ncol=2)")
    * res2: Any = Array(Array(1.0, 3.0), Array(2.0, 4.0))
    * }}}
    */
  def apply(expression : String) : Any = selectDynamic(expression)

  /** Finds the name of the symbol and accesses the apply function on the symbol name.
    * @param symbol a Scala `Symbol`
    * ===Example:===
    * {{{
    * scala> val R = RInScala()
    * 
    * scala> R.eval("a <- 3")
    * 
    * scala> R.apply('a)
    * res1: Any = Array(3.0)
    * }}} 
    */ 
  def apply(symbol : Symbol) : Any = apply(symbol.name)


  // Push values from Scala into R

  private def updatePrimitivePrimitive[A](name : String, value : A, emptyValue : A, valueType : String, valueSize : String, write : (A) => Unit) : Unit = {
    cmdOut.println("""%s <- readBin(con=.RInScala.dataSocket,what="%s",n=%d,endian="big",size=%s)""".format(name,valueType,1,valueSize))
    cmdOut.flush()
    write(value)
  }

  private def updatePrimitiveVector[A](name : String, value : Array[A], emptyValue : A, valueType : String, valueSize : String, write : (A) => Unit) : Unit = {
    cmdOut.println("""%s <- readBin(con=.RInScala.dataSocket,what="%s",n=%d,endian="big",size=%s)""".format(name,valueType,value.length,valueSize))
    cmdOut.flush()
    value.foreach(write)
  }

  private def updatePrimitiveMatrix[A](name : String, value : Array[Array[A]], emptyValue : A, valueType : String, valueSize : String, write : (A) => Unit) : Unit = {
    val nRows = value.length
    if ( nRows == 0 ) {
      cmdOut.println("""%s <- matrix(%s,nrow=0,ncol=1)""".format(name,emptyValue))
      cmdOut.flush()
      return
    }
    val nCols = value(0).length
    value.foreach(v => if ( v.length != nCols ) throwRException("Array lengths must be equal."))
    val nItems = nRows*nCols
    cmdOut.println("""%s <- matrix(readBin(con=.RInScala.dataSocket,what="%s",n=%d,endian="big",size=%s),nrow=%d,byrow=TRUE)""".format(name,valueType,nItems,valueSize,nRows))
    cmdOut.flush()
    for ( i <- 0 until nRows ) {
      for ( j <- 0 until nCols ) {
        write(value(i)(j))
      }
    }
  }

  private val countingRegex = """\[\]""".r


  /** Not intended for use by the user.  See the [[RInScala.update(name:String,value:Any)* update(name:String,value:Any)]] and [[RInScala.update(symbol:Symbol,value:Any)* update(symbol:Symbol,value:Any)]]
    * functions. 
    */
  def updateDynamic(name : String)(value : Any) : Unit = {
    val clazz = value.getClass
    val typeName = clazz.getCanonicalName.
          replace("java.lang.Double","double").
          replace("java.lang.Integer","int").
          replace("java.lang.Boolean","boolean")
    val valueType = countingRegex.replaceAllIn(typeName,"")
    val valueShape = countingRegex.findAllIn(typeName).toArray.length
    if ( valueShape > 2 ) throwRException("Value's type is not supported.")
    if ( name.split("\r\n")(0) != name ) throwRException("Invalid variable name.")
    if ( name.split("\n")(0) != name ) throwRException("Invalid variable name.")
    cmdOut.println(""".RInScala.variableName <- readLines(con=.RInScala.textSocket,n=1)""")
    cmdOut.flush()
    textOut.println(name)
    textOut.flush()
    if ( validateNamesForAssignment ) {
      if ( ! toPrimitive[Boolean](""".RInScala.isValidVariableName(.RInScala.variableName)""") ) {
        throwRException("Invalid variable name.  Set 'validateNamesForAssignment=false' to suppress this check but know that any error will hang the interpreter.")
      }
    }
    valueType match {
      case "double" =>
        valueShape match {
          case 0 =>
            updatePrimitivePrimitive(name,value.asInstanceOf[Double],0.0,"double","NA_integer_",dataOut.writeDouble _)
          case 1 =>
            updatePrimitiveVector(name,value.asInstanceOf[Array[Double]],0.0,"double","NA_integer_",dataOut.writeDouble _)
          case 2 =>
            updatePrimitiveMatrix(name,value.asInstanceOf[Array[Array[Double]]],0.0,"double","NA_integer_",dataOut.writeDouble _)
       }
      case "int" =>
        valueShape match {
          case 0 =>
            updatePrimitivePrimitive(name,value.asInstanceOf[Int],0,"integer","NA_integer_",dataOut.writeInt _)
          case 1 =>
            updatePrimitiveVector(name,value.asInstanceOf[Array[Int]],0,"integer","NA_integer_",dataOut.writeInt _)
          case 2 =>
            updatePrimitiveMatrix(name,value.asInstanceOf[Array[Array[Int]]],0,"integer","NA_integer_",dataOut.writeInt _)
        }
      case "boolean" =>
        valueShape match {
          case 0 =>
            updatePrimitivePrimitive(name,value.asInstanceOf[Boolean],false,"logical","1",dataOut.writeBoolean _)
          case 1 =>
            updatePrimitiveVector(name,value.asInstanceOf[Array[Boolean]],false,"logical","1",dataOut.writeBoolean _)
          case 2 =>
            updatePrimitiveMatrix(name,value.asInstanceOf[Array[Array[Boolean]]],false,"logical","1",dataOut.writeBoolean _)
        }
      case "java.lang.String" =>
        valueShape match {
          case 0 =>
            val v = value.asInstanceOf[String].replaceAll("\r\n","\n").split("\n")
            cmdOut.println("""%s <- paste(readLines(con=.RInScala.textSocket,n=%d),collapse="\n")""".format(name,v.length))
            cmdOut.flush()
            v.foreach(textOut.println(_))
          case 1 =>
            val v = value.asInstanceOf[Array[String]]
            cmdOut.println("""%s <- character(%d)""".format(name,v.length))
            cmdOut.flush()
            for ( index <- 0 until v.length ) {
              val result = v(index).replaceAll("\r\n","\n").split("\n")
              cmdOut.println("""%s[%d] <- paste(readLines(con=.RInScala.textSocket,n=%d),collapse="\n")""".format(name,index+1,result.length))
              cmdOut.flush()
              result.foreach(textOut.println)
            }
          case 2 =>
            val v = value.asInstanceOf[Array[Array[String]]]
            val nRows = v.length
            if ( nRows == 0 ) {
              cmdOut.println("""%s <- matrix(%s,nrow=0,ncol=1)""".format(name,""))
              cmdOut.flush()
            } else {
              val nCols = v(0).length
              v.foreach(vv => if ( vv.length != nCols ) throwRException("Array lengths must be equal."))
              cmdOut.println("""%s <- matrix("",nrow=%d,ncol=%d)""".format(name,nRows,nCols))
              cmdOut.flush()
              for ( i <- 0 until nRows ) {
                for ( j <- 0 until nCols ) {
                  val result = v(i)(j).replaceAll("\r\n","\n").split("\n")
                  cmdOut.println("""%s[%d,%d] <- paste(readLines(con=.RInScala.textSocket,n=%d),collapse="\n")""".format(name,i+1,j+1,result.length))
                  cmdOut.flush()
                  result.foreach(textOut.println)
                }
              }
            }
        }
      case _ => throwRException("Value's type is not supported: "+valueType)
    }
    dataOut.flush()
    textOut.flush()
  }


  /** Creates or reassigns an object in R with objects or values from Scala.
    * @param name the name of an R object to be assigned a Scala object or value
    * @param value the Scala value or object to be assigned to `name`
    * ==Example:==
    * {{{
    * scala> val R = RInScala()
    * 
    * scala> R.update("myRstring","This String was created in Scala and pulled into R")
    * scala> print(R.capture("myRstring"))
    * [1] "This String was created in Scala and pulled into R"
    *
    *  
    * // Alternate way of invoking R.update
    * scala> R.rVector = Array[Int](1,5,7)
    * R.rVector: Any = [I@457a0993
    * 
    * scala> print(R.capture("rVector"))
    * [1] 1 5 7 
    * }}}
    */ 
  def update(name : String, value : Any) : Unit = updateDynamic(name)(value)

  /** Calls the [[RInScala!.update(name:String,value:Any)* update(name:String,value:Any)]] function with the symbol name as its first argument.
    * @param symbol a Scala `Symbol` 
    * ===Example:===
    * {{{ 
    * scala> val R = RInScala()
    *
    * scala> R.update('a,3)
    * 
    * scala> print(R.capture("a"))
    * [1] 3
    * }}}
    */
  def update(symbol : Symbol, value : Any) : Unit = update(symbol.name,value)

 
  // Misc. functions

  /** Starts a read-eval-print loop (REPL) session for R within Scala.
    * Exit by entering ^D (ctrl+D).  Values defined in this session will be accessible by Scala.
    * Equivalent to the [[RInScala.>()* >()]] method.
    * ===Example:===
    * {{{
    * scala> val R = RInScala()
    * 
    * scala> R.prompt()    // Equivalent to R>
    * Welcome to the R prompt!  Type R commands.  No prompt is provided.  Exit by pressing ^D.
    * 
    * f <- function(x) pnorm(x)
    * f <- function(x) pnorm(x)  // prompt output
    * 
    * f(0)
    * f(0)                 // prompt output
    * [1] 0.5              // prompt output
    * 
    * f(2.5)
    * f(2.5)               // prompt output             
    * [1] 0.9937903        // prompt output
    * 
    * // enter "^D"
    * Exiting R prompt.    // prompt output
    *
    * // Objects and functions defined in the R prompt are accessible by Scala 
    * scala> R.toPrimitive[Double]("f(-1.5)")
    * res1: Double = 0.06680720126885807
    * }}}
    */
  def prompt() = {
    println("Welcome to the R prompt!  Type R commands.  No prompt is provided.  Exit by pressing ^D.")
    val reader = new scala.tools.jline.console.ConsoleReader()
    val debugFlag = debug
    var line = reader.readLine()
    debug = true
    while ( line != null ) {
      cmdOut.println(line)
      cmdOut.flush()
      line = reader.readLine()
    }
    debug = debugFlag
    println("Exiting R prompt.")
  }

  /** Starts a read-eval-print loop (REPL) session for R within Scala.
    * Exit by entering ^D (ctrl+D).  Values defined in this session will be accessible by Scala.
    * Equivalent to the [[RInScala.prompt prompt()]] method.
    * ===Example:===
    * {{{
    * scala> val R = RInScala()
    * 
    * scala> R>   // Equivalent to R.prompt()
    * Welcome to the R prompt!  Type R commands.  No prompt is provided.  Exit by pressing ^D.
    * 
    * f <- function(x) pnorm(x)
    * f <- function(x) pnorm(x)  // prompt output
    * 
    * f(0)
    * f(0)                 // prompt output
    * [1] 0.5              // prompt output
    * 
    * f(2.5)
    * f(2.5)               // prompt output             
    * [1] 0.9937903        // prompt output
    * 
    * // enter "^D"
    * Exiting R prompt.    // prompt output
    *
    * // Objects and functions defined in the R prompt are accessible by Scala 
    * scala> R.toPrimitive[Double]("f(-1.5)")
    * res1: Double = 0.06680720126885807
    * }}}
    */
  def >() : Unit = prompt()

  /** Evaluates an expression as if at the R prompt.  Multi-line expressions are possible using triple quotes.
    * @param expression an R expression
    * ===Example:===
    * {{{
    * scala> val R = RInScala()
    * 
    * scala> R> "sentence <- c('I','love','pi!')"
    * scala> R> "sentence"
    * [1] "I"    "love" "pi!" 
    * 
    * // Multi-line example:
    *
    * scala> R> """
    *      | for(i in 1:length(sentence)){
    *      |   cat(sentence[i],"\n")
    *      | }
    *      | """
    * I 
    * love 
    * pi!
    * }}}
    */ 
  def >(expression : String) : Unit = println(capture(expression))

}


/** To be primarily used as a factory to create an instance of its companion class.
  * 
  * A paper describing the software in detail is available here: [[http://dahl.byu.edu/software/jvmr/ http://dahl.byu.edu/software/jvmr/]]
  *
  * ===Example:===
  * {{{
  * scala> import org.ddahl.jvmr.RInScala
  * import org.ddahl.jvmr.RInScala
  *
  * scala> val R = RInScala()
  * }}}
  */
object RInScala {

  private val NEWLINE = System.getProperty("line.separator")
  private val CODE_OK = 0
  private val CODE_ERROR = 1
  private val CODE_STRING_VECTOR = 2
  private val CODE_STRING_MATRIX = 3
  private val CODE_DOUBLE_VECTOR = 4
  private val CODE_DOUBLE_MATRIX = 5
  private val CODE_INT_VECTOR = 6
  private val CODE_INT_MATRIX = 7
  private val CODE_BOOLEAN_VECTOR = 8
  private val CODE_BOOLEAN_MATRIX = 9
  private val COLOR_REDB="\033[1;41m"
  private val COLOR_REDF="\033[31m"
  private val COLOR_GREENB="\033[1;42m"
  private val COLOR_GREENF="\033[1;32m"
  private val COLOR_YELLOWB="\033[1;43m"
  private val COLOR_YELLOWF="\033[1;33m"
  private val COLOR_BLUEB="\033[1;44m"
  private val COLOR_BLUEF="\033[1;34m"
  private val COLOR_MAGENTAB="\033[1;45m"
  private val COLOR_MAGENTAF="\033[1;35m"
  private val COLOR_CYANB="\033[1;46m"
  private val COLOR_CYANF="\033[1;36m"
  private val COLOR_WHITEB="\033[1;47m"
  private val COLOR_WHITEF="\033[1;37m"
  private val COLOR_RESET="\033[0m"

  /** A `String` stating the type of operating system in use. */
  lazy val OS = System.getProperty("os.name").toLowerCase match {
    case s if s.startsWith("""windows""") => "windows"
    case s if s.startsWith("""linux""") => "linux"
    case s if s.startsWith("""unix""") => "linux"
    case s if s.startsWith("""mac""") => "macintosh"
    case _ => throw new RuntimeException("Unrecognized OS")
  }

  /** A `String` listing the default options when opening R for the interpreter. */
  lazy val defaultArguments = OS match {
    case "windows" =>    Array[String]("--vanilla","--silent","--slave","--ess") 
    case "linux" =>      Array[String]("--vanilla","--silent","--slave","--interactive")
    case "unix" =>       Array[String]("--vanilla","--silent","--slave","--interactive")
    case "macintosh" =>  Array[String]("--vanilla","--silent","--slave","--interactive")
  }

  private def findROnWindows : String = {
    var result : String = null
    for ( root <- List("HKEY_LOCAL_MACHINE","HKEY_CURRENT_USER") ) {
      val out = new StringBuilder()
      val logger = ProcessLogger((o: String) => { out.append(o); out.append(NEWLINE) },(e: String) => {})
      try {
        ("reg query \"" + root + "\\Software\\R-core\\R\" /v \"InstallPath\"") ! logger
        val a = out.toString.split(NEWLINE).filter(_.matches("""^\s*InstallPath\s*.*"""))(0)
        result = a.split("REG_SZ")(1).trim() + """\bin\R.exe"""
      } catch {
        case _ : Throwable =>
      }
    }
    if ( result == null ) throw new RuntimeException("Cannot locate R using Windows registry.")
    else return result
  }

  /** The code which is used to access R. */
  lazy val defaultExecutable = OS match {
    case "windows" =>   findROnWindows
    case "linux" =>     """R"""
    case "unix" =>      """R"""
    case "macintosh" => """R"""
  }

  /** Creates an instance of the R interpreter of private class [[RInScala]].  R will be started
    * using the path from [[RInScala.defaultExecutable]] and use the options specified by 
    * [[RInScala.defaultArguments]]. 
    * ===Example:===
    * {{{
    * scala> import org.ddahl.jvmr.RInScala
    *
    * scala> val R = RInScala()
    * }}}
    */
  def apply() = new RInScala()

  /** Creates an instance of the R interpreter of private class [[RInScala]] using a specified path to access R.
    * R will use the default options specified by [[RInScala.defaultArguments]].  This method only needs
    * to be used if R is not contained in the operating system's PATH environment variable.
    * @param command the path to a working version of R
    * ===Example:===
    * {{{
    * scala> import org.ddahl.jvmr.RInScala
    * 
    * scala> val R = RInScala("/opt/R/3.0.1/bin/R")
    * }}}  
    */ 
  def apply(command : String) = new RInScala(command)

  /** For developer use only.  Creates an instance of the R Interpreter of private class [[RInScala]] 
    * using a specified path to access R and R options. 
    * @param command a Scala {@code Array[String]} specifying the R path and R options
    * ===Example:===
    * {{{
    * scala> import org.ddahl.jvmr.RInScala
    * 
    * scala> val R = RInScala(Array("/usr/bin/R","--vanilla","--silent","--slave","--interactive"))
    * }}}
    */
  def apply(command : Array[String]) = new RInScala(command) 
}

