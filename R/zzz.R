get.urls <- function(partial.url) {
  con <- file(paste(partial.url,"list.txt",sep="/"),open="r")
  lines <- readLines(con)
  close(con)
  if ( lines[1] != "#jvmr.jar.list" ) stop("unexpected header.")
  lines <- lines[-1]
  paste(partial.url,lines,sep="/")
}

download.jars <- function(path) {
  if ( ! file.exists(path) ) {
    tmpDir <- paste(path,"-tmp",sep="")
    dir.create(tmpDir,recursive=TRUE,showWarnings=FALSE)
    cwd <- getwd()
    setwd(tmpDir)
    download.sites <- c("http://dahl.byu.edu/software/jvmr/alljars/","http://ddahl.org/alljars/","file:///home/dahl/docs/devel/website-professional/html/software/jvmr/alljars/")
    jarURLs <- tryCatch(get.urls(paste(download.sites[1],jvmr.version,"/bin",sep="")),
      error = function(e) {
        tryCatch(get.urls(paste(download.sites[2],jvmr.version,"/bin",sep="")),
          error = function(e) {
            get.urls(paste(download.sites[3],jvmr.version,"/bin",sep=""))
          }
        )
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

