get.urls <- function(partial.url) {
  con <- file(paste(partial.url,"list.txt",sep="/"),open="r")
  lines <- readLines(con)
  close(con)
  paste(partial.url,lines,sep="/")
}

download.jars <- function(path) {
  if ( ! file.exists(path) ) {
    tmpDir <- paste(path,"-tmp",sep="")
    dir.create(tmpDir,recursive=TRUE,showWarnings=FALSE)
    cwd <- getwd()
    setwd(tmpDir)
    jarURLs <- tryCatch(get.urls(paste("http://dahl.byu.edu/software/jvmr/alljars/",jvmr.version,sep="")),
      error = function(e) {
        get.urls(paste("http://ddahl.org/alljars/",jvmr.version,sep=""))
      }
    )
    for ( jarURL in jarURLs ) {
      download.file(jarURL,basename(jarURL),mode = "wb",quiet=FALSE)
    }
    setwd(cwd)
    file.rename(tmpDir,path)
  }
}

path <- paste(dirname(system.file(".",package="jvmr")),.Platform$file.sep,"java",sep="")
download.jars(path)

