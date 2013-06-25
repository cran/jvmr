\name{jvmr}
\alias{jvmr-package}
\docType{package}
\title{
Integration of R, Java, and Scala
}
\description{
Integrates the Java and Scala interpreters in R. This provides access to
existing Java and Scala libraries.  This package also contains
the JAR files which allow R to be embedded in Java and Scala programs.
}
\usage{
.jvmr.jar
.jvmr.alljars
.jvmr.alljars.vector
.bsh.jar
}
\value{
.jvmr.jar is a character vector giving the location of the jvmr JAR file.  This must be in the classpath when embedding R in Scala.

.jvmr.alljars is a character vector of length 1 containing the paths to all the JARs from the jvmr package, seperated according to \code{.Platform$path.sep}.  All these JARs must be in the classpath when embedding R in Java.

.jvmr.alljars.vector is a character vector containing the paths to all the JARs from the jvmr package.  All these JARs must be in the classpath when embedding R in Java.

.bsh.jar is a character vector on length 1 containing the path of the BeanShell JAR.
}
\author{
David B. Dahl
}
\seealso{
\code{\link{scalaInterpreter}},
\code{\link{javaInterpreter}},
\code{\link{interpret}},
\code{\link{interpret<-}}.
}
\keyword{package}
\keyword{interface}
