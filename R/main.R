stack.trace <- function(expr) {
  tryCatch(expr,Exception = function(e){
    print(e)
    cat(paste(unlist(lapply(.jevalArray(e$jobj$getStackTrace()),function(x) { x$toString() })),collapse="\n"),"\n",sep="")
  })
}

checkForException <- function() {
  b <- .jgetEx(clear=TRUE)
  x <- if ( ! is.null(b) ) {
    cat(b$toString(),"\n")
    cat(paste(unlist(lapply(.jevalArray(b$getStackTrace()),function(x) { x$toString() })),collapse="\n"),"\n",sep="")
    b
  } else NULL
  x
}





interpret <- function(interpreter,code,...,eval.only=FALSE,simplify=TRUE,echo.output=FALSE) UseMethod("interpret")

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

interpret.RJavaInterpreter <- function(interpreter,code,...,eval.only=FALSE,simplify=TRUE) {
  code <- substitute(code,...)
  z <- .jcall(interpreter$repl,"Ljava/lang/Object;","eval",code)
  if ( eval.only ) return()
  return(tryCatch({
      clean(z,simplify)
    }, error = function(e) {}))
}

"[.RJavaInterpreter" <- function(interpreter,code,...,eval.only=FALSE,simplify=TRUE) {
  return(interpret(interpreter,code,...,simplify=simplify))
}

"interpret<-.RJavaInterpreter" <- function(interpreter,varname,type=NULL,echo.output=FALSE,value) {
  if ( is.character(value) || is.numeric(value) || is.logical(value) ) {
    if ( is.character(value) && ( length(value) == 1 ) ) value <- .jcast(.jnew("java/lang/String",value),"java/lang/Object")
    else if ( length(value) > 1 ) value <- .jcast(.jarray(value,dispatch=TRUE),"java/lang/Object")
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

interpret.RScalaInterpreter <- function(interpreter,code,...,eval.only=FALSE,simplify=TRUE,echo.output=FALSE) {
  code <- substitute(code,...)
  z <- try(.jcall(interpreter$repl,"Ljava/lang/Object;","eval",code,check=FALSE))
  zz <- checkForException()
  print.scala.output(interpreter,echo.output)
  if ( ! is.null(zz) ) return(zz)
  if ( eval.only ) return()
  return(tryCatch({
      clean(z,simplify)
    }, error = function(e) {}))
}

"[.RScalaInterpreter" <- function(interpreter,code,...,eval.only=FALSE,simplify=TRUE,echo.output=FALSE) {
  interpret(interpreter,code,...,eval.only=eval.only,simplify=simplify,echo.output=echo.output)
}

"interpret<-.RScalaInterpreter" <- function(interpreter,varname,type=NULL,echo.output=FALSE,value) {
  if ( is.character(value) || is.numeric(value) || is.logical(value) ) {
    if ( length(value) == 1 ) {
      result <- .jcall(interpreter$repl,"Lscala/tools/nsc/interpreter/Results$Result;","update",as.character(varname[1]),value)
    } else {
      result <- .jcall(interpreter$repl,"Lscala/tools/nsc/interpreter/Results$Result;","update",as.character(varname[1]),.jarray(value,dispatch=TRUE))
    }
  } else if ( inherits(value,"jobjRef") ) {
    if ( any(is.null(type)) ) type <- .jclass(value)
    value <- .jcast(value,"java/lang/Object")
    result <- .jcall(interpreter$repl,"Lscala/tools/nsc/interpreter/Results$Result;","update",as.character(varname[1]),value,as.character(type[1]))
  } else stop("Unsupported data type")
  print.scala.output(interpreter,echo.output)
  if ( ! .jinherits(result,"scala.tools.nsc.interpreter.Results$Success$") ) {
    stop("Could not make assignment. Consider specifying the type argument.\n")
  }
  return(interpreter)
}

"[<-.RScalaInterpreter" <- function(interpreter,varname,type=NULL,echo.output=FALSE,value) {
  interpret(interpreter,varname,type=type) <- value
  return(interpreter)
}

