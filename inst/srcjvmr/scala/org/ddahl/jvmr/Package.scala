package org.ddahl
/** Allows Scala to access R's statistical methods via a high-level interface between R and Scala.
  * This package provides intuitive syntax which seamlessly integrates R within Scala.
  * Use of this package allows the user to integrate Scala and R code into a single program.
  * 
  * A paper describing the software in detail is available here: [[http://dahl.byu.edu/software/jvmr/ http://dahl.byu.edu/software/jvmr/]]
  *
  * To use the [[org.ddahl.jvmr.RInScala]] package:
  *
  * __Step 1__: At the R prompt, install and load the '''"jvmr"''' package as follows:
  * {{{
  * install.packages("jvmr")
  * library(jvmr)
  * }}}
  *
  * __Step 2__: At the R prompt, query to locate the path of the jvmr JAR file on the local machine as follows:
  * {{{
  * .jvmr.jar
  * }}}
  *
  * __Step 3__	: Include the jvmr JAR file in the classpath.
  *
  * Now the package is ready for use.
  * 
  * ===Example:===
  * With the appropriate JARs included in the Scala program by specifying the classpath, one can use R's statistical routines and graphics within Scala.
  * For example, consider the following code executed at the Scala interpreter:
  * {{{
  * scala> import org.ddahl.jvmr.RInScala
  *
  * scala> val R = RInScala()
  * 
  * scala> val sample_size = 5
  * sample_size: Int = 5
  * 
  * scala> R("x <- rnorm(%s)".format(sample_size))
  * res1: Any = Array(0.7501045632267074, 0.9518238448808821, 0.6149436855035977, 0.46385023744725495, 0.19677067043728344)
  *
  * scala> println(R.toVector("x").mkString(","))
  * 0.7501045632267074,0.9518238448808821,0.6149436855035977,0.46385023744725495,0.19677067043728344
  *
  * scala> println(R.toPrimitive[Double]("sd(x)"))
  * 0.28616605691936564 
  * }}}
  */

package object jvmr {

  /** Prints a String of the Major Version of [[org.ddahl.jvmr]] */
  val MajorVersion = "1.0.4"

  /** Prints a String of the Full Version of [[org.ddahl.jvmr]] */
  val FullVersion = MajorVersion

}
