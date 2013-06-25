\name{scalaInterpreter}
\alias{scalaInterpreter}
\alias{javaInterpreter}
\docType{methods}
\title{
Creates an instance of a Scala or Java interpreter embedded in the R session.
}
\description{
Creates an instance of a Scala or Java interpreter embedded in the R session.
}
\usage{
scalaInterpreter(\dots, use.jvmr.class.path = TRUE, include.cwd = TRUE,
                 use.java.class.path = FALSE)
javaInterpreter(\dots, use.jvmr.class.path = TRUE, include.cwd = TRUE)
}
\arguments{
    \item{\dots}{Specifies additional elements for the classpath of the interpreter.
This only has an effect if \code{use.java.class} is FALSE.}
    \item{use.jvmr.class.path}{If TRUE (the default value), the JAR
files specified in the global option \code{jvmr.class.path} will be included in the classpath.
This only has an effect if \code{use.java.class} is FALSE.}
    \item{include.cwd}{If TRUE (the default value), the
current working directory will be included in the classpath.
These class paths will only be included if \code{use.java.class} is FALSE.}
    \item{use.java.class.path}{If TRUE, the default java classpath
will be used and all other arguments are ignored.}
}
\author{
David B. Dahl
}
\seealso{
\code{\link{jvmr-package}},
\code{\link{interpret}},
\code{\link{interpret<-}}.
}
\keyword{interface}
\examples{
library(jvmr)

\dontrun{
# Creating an instance of the Scala interpreter
a <- scalaInterpreter()

# Create an instance of the Java interpreter
b <- javaInterpreter()
}

}

