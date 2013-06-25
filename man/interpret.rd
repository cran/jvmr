\name{interpret}
\alias{interpret}
\alias{interpret<-}
\docType{methods}
\title{
Run Java or Scala code in an interpreter embedded in R.
}
\description{
These functions are the main interface to the Java and Scala interpreters embedded in R.  Through these functions, code in Java or Scala can be executed and data can be passed between R and these languages.
There are shortcuts using square brackets for each of these functions.  See the examples below.
}
\usage{
interpret(interpreter, code, \dots, simplify = TRUE, echo.output = FALSE)
interpret(interpreter, varname, type = NULL, echo.output = FALSE) <- value
}
\arguments{
    \item{interpreter}{an instance of a Java or Scala interpreter created by the \code{scalaInterpreter} or \code{javaInterpreter} functions.}
    \item{code}{a character vector of length one to be evaluated by the Java or Scala interpreter.  This vector can contain many statements and span multiple lines (by using triple quotes in Scala).}
    \item{varname}{name of a Java or Scala object to be assigned \code{value}.}
    \item{\dots}{specifies up to nine substitutions to be
made in the Java or Scala code.  These substitutions must take the form \code{$\{d\}}
in the Java or Scala code, where the number \code{d} is the argument
number 1, 2, ..., 9.}
    \item{simplify}{When TRUE (the default value), an R object is returned when possible. If FALSE, the reference of the resulting Java object is returned.}
    \item{echo.output}{When FALSE (the default value), output from the Scala interpreter is not printed to the R session.  If TRUE, such output is printed in the R session.  This is not relevant for the Java interpreter.}
    \item{type}{an optional argument, which can be used to explicitly specify the type of the Java or Scala object.}
    \item{value}{the value assigned to the Scala or Java object whose name is given by the \code{varname} argument.}
}
\author{
David B. Dahl
}
\seealso{
\code{\link{scalaInterpreter}},
\code{\link{javaInterpreter}},
\code{\link{jvmr-package}}.
}
\keyword{interface}
\examples{
library(jvmr)

##############
## ScalaInR ##
##############

\dontrun{
# Creating an instance of the Scala interpreter
a <- scalaInterpreter()

# Defining functions
interpret(a,'
  def fib( n: Int): Int = n match {
    case 0 | 1 => n
    case _ => fib( n -1) + fib( n-2)
  }
')

fib <- function(n){
  if( n > 1 ) fib(n-1) + fib(n-2)
  else n
}

system.time(a['fib(33)'])
system.time(fib(33))

# Defining a variable
interpret(a,"num") <- 3.4
# Printing the value of the variable
interpret(a,"num")

# Defining a variable using shortcut syntax
a["x"] <- "Welcome"
# Printing the value of the variable using shortcut syntax
a["x"]

# Using loops
interpret(a,"message") <- c("Hello","World","!")
interpret(a,"message")
interpret(a,'message.foreach{ x => println("<"+x+">") }',echo.output=TRUE)


# Assigning values to variables using the argument list
interpret(a,'
  val d = "Zero"
  val b = "${1}"
  val d2 = "Two"
',"One")

# Illustrating the use of 'simplify'
c <- a['Range(0,100).toArray']
c

d <- a['Range(0,100).toArray',simplify=FALSE]
d

# Illustrating the use of 'type'
library(rJava)
random <- a["new java.util.Random"]     # Can infer type

my.list <- a['List[Double](1.0, 2.3, 5.6)']
\dontrun{
a["myList"] <- my.list                  # Cannot infer type this time
}
a["myList","List[Double]"] <- my.list   # Must explicity specify type


# Illustrating the cost of the high level bridge
a["val who = 'David'"]

system.time(
  for ( i in 1:100 ) { cat(interpret(a,"who"),"\n") }
)

system.time(
  interpret(a,'for ( i <- 0 until 1000 ) println(who)')
)
}


#############
## JavaInR ##
#############

\dontrun{
# Create an instance of the Java interpreter
b <- javaInterpreter()

# Defining functions
interpret(b,'
  int fib(int n) {
    if ( ( n == 0 ) || ( n == 1 ) ) return(n);
    return( fib(n-1) + fib(n-2) );
  }
')


fib <- function(n) {
  if ( n > 1 ) fib(n-1) + fib(n-2)
  else n
}

system.time(b['fib(33)'])
system.time(fib(33))


# Defining a variable
interpret(b,"number") <- 3.4
# Printing the value of the variable
interpret(b,"number")

# Defining a variable using shortcut syntax
b['int[] c = {0,1,2,3,4,5,6}']
# Printing a variable using shortcut syntax
b['c']

# Pulling values from Java into R
c <- b['c']
c

# Use of simplify
d <- b['c',simplify=FALSE]
d

# Using loops
interpret(b,"message2") <- c("Hello","World","Again","!")
interpret(b,"message2")
interpret(b,'for(int i = 0; i < message2.length; i++) {
  System.out.println("<" + message2[i] + ">");
}',echo.output=TRUE)

# Assigning values to variables using the argument list
b['
  String arg1 = "${1}";
  String arg2 = "${2}";
  String statement = "Skies" + arg1 + arg2;
'," are ","blue"]
b['statement']

# Illustrating the cost of the high level bridge
b['String who = "David"']

system.time(
  for ( i in 1:1000 ) { cat(interpret(b,"who"),"\n") }
)

system.time(
  interpret(b,'for ( i=0; i < 1000; i++) java.lang.System.out.println(who)')
)
}
}
