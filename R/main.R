interpret <- function(interpreter,code,...,simplify=TRUE,echo.output=FALSE) UseMethod("interpret")

"interpret<-" <- function(interpreter,varname,type=NULL,echo.output=FALSE,value) UseMethod("interpret<-")





clean <- function(obj,simplify=TRUE) {
  if ( simplify ) {
    if ( .jcall(.jcall(obj,"Ljava/lang/Class;","getClass"),"Z","isArray") ) {
      return(.jevalArray(obj,simplify=TRUE))
    } else {
      return(.jsimplify(obj))
    }
  } else return(obj)
}

substitute <- function(code,...) {
  subs <- c(...)
  if ( length(subs) > 9 ) stop("At most 9 substitutes are permitted.")
  if ( length(subs) > 0 ) {
    index <- 0
    while ( index < length(subs) ) {
      index <- index + 1
      code <- gsub(paste("\\$\\{",index,"\\}",sep=""),subs[index],code)
    }
  }
  code
}





javaInterpreter <- function(...,use.jvmr.class.path=TRUE,include.cwd=TRUE) {
  repl <- .jnew("bsh.Interpreter")
  cp = c()
  if ( use.jvmr.class.path ) cp <- c(getOption("jvmr.class.path"),cp)	
  classpath <- c(...)
  if ( ( ! is.null(classpath) ) && ( classpath != "" ) ) cp <- c(classpath,cp)
  if ( include.cwd ) cp <- c(".",cp)
  if ( length(cp[!file.exists(cp)]) != 0 )
    warning(paste("There are items in the classpath that do not exist: ",paste(cp[!file.exists(cp)],collapse=", ",sep=""),sep=""))
  for ( i in cp ) repl$eval(paste('addClassPath("',i,'")',sep=""))
  a <- list(repl=repl)
  class(a) <- "RJavaInterpreter"
  return(a)
}

interpret.RJavaInterpreter <- function(interpreter,code,...,simplify=TRUE) {
  code <- substitute(code,...)
  z <- .jcall(interpreter$repl,"Ljava/lang/Object;","eval",code)
  return(tryCatch({
      clean(z,simplify)
    }, error = function(e) return(invisible())))
}

"[.RJavaInterpreter" <- function(interpreter,code,...,simplify=TRUE) {
  return(interpret(interpreter,code,...,simplify=simplify))
}

"interpret<-.RJavaInterpreter" <- function(interpreter,varname,type=NULL,echo.output=FALSE,value) {
  if ( is.character(value) || is.numeric(value) || is.logical(value) ) {
    if ( is.character(value) && ( length(value) == 1 ) ) value <- .jcast(.jnew("java/lang/String",value),"java/lang/Object")
    else if ( length(value) > 1 ) value <- .jcast(.jarray(value),"java/lang/Object")
  } else if ( inherits(value,"jobjRef") ) value <- .jcast(value,"java/lang/Object")
  .jcall(interpreter$repl,"V","set",as.character(varname[1]),value)
  return(interpreter)
}

"[<-.RJavaInterpreter" <- function(interpreter,varname,type=NULL,echo.output=FALSE,value) {
  interpret(interpreter,varname,type=type) <- value
  return(interpreter)
}





scalaInterpreter <- function(...,use.jvmr.class.path=TRUE,include.cwd=TRUE,use.java.class.path=FALSE) {
  if ( use.java.class.path == TRUE ) {
    repl <- .jcall(.jnew("org.ddahl.jvmr.impl.RScalaInterpreter$"),"Lorg/ddahl/jvmr/impl/RScalaInterpreter;","apply")
  } else {
    cp = c()
    if ( use.jvmr.class.path ) cp <- c(getOption("jvmr.class.path"),cp)	
    classpath <- c(...)
    if ( ( ! is.null(classpath) ) && ( classpath != "" ) ) cp <- c(classpath,cp)
    if ( include.cwd ) cp <- c(".",cp)
    if ( length(cp[!file.exists(cp)]) != 0 )
      warning(paste("There are items in the classpath that do not exist: ",paste(cp[!file.exists(cp)],collapse=", ",sep=""),sep=""))
    cp <- paste(cp,collapse=.Platform$path.sep)
    repl <- .jcall(.jnew("org.ddahl.jvmr.impl.RScalaInterpreter$"),"Lorg/ddahl/jvmr/impl/RScalaInterpreter;","apply",cp)
  }
  a <- list(repl=repl)
  class(a) <- "RScalaInterpreter"
  return(a)
}

print.scala.output <- function(x,echo.output) {
  # Get results from the PrintWriter buffer and use R's 'cat' method to display to the console.
  output <- .jcall(x$repl,"Ljava/lang/String;","getOutput")
  if ( echo.output ) cat(output)
}

interpret.RScalaInterpreter <- function(interpreter,code,...,simplify=TRUE,echo.output=FALSE) {
  code <- substitute(code,...)
  .jcall(interpreter$repl,"Lscala/tools/nsc/interpreter/Results$Result;","eval",code)
  print.scala.output(interpreter,echo.output)
  return(tryCatch({
      y <- .jcall(interpreter$repl,"Lscala/Option;","lastResult")
      if ( (!is.null(y)) && .jcall(y,"Z","isDefined") ) {
        z <- .jcall(y,"Ljava/lang/Object;","get")
        clean(z,simplify)
      }
    }, error = function(e) return(invisible())))
}

"[.RScalaInterpreter" <- function(interpreter,code,...,simplify=TRUE) {
  return(interpret(interpreter,code,...,simplify=simplify))
}

"interpret<-.RScalaInterpreter" <- function(interpreter,varname,type=NULL,echo.output=FALSE,value) {
  if ( is.null(type) ) {
    result <- try(.jcall(interpreter$repl,"Lscala/tools/nsc/interpreter/Results$Result;","update",varname,value),silent=TRUE)
    if ( inherits(result,"try-error") ) {
      type <- .jcall(.jcall(value,"Ljava/lang/Class;","getClass"),"Ljava/lang/String;","getName")
      result <- try(.jcall(interpreter$repl,"Lscala/tools/nsc/interpreter/Results$Result;","update",varname,.jcast(value,"java/lang/Object"),type))
    }
  } else {
    .jcall(interpreter$repl,"Lscala/tools/nsc/interpreter/Results$Result;","update",varname,.jcast(value,"java/lang/Object"),type)
  }
  print.scala.output(interpreter,echo.output)
  return(interpreter)
}

"[<-.RScalaInterpreter" <- function(interpreter,varname,type=NULL,echo.output=FALSE,value) {
  interpret(interpreter,varname,type=type) <- value
  return(interpreter)
}

