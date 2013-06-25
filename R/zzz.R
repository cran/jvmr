download.jars <- function(path) {
  if ( ! file.exists(path) ) {
    tmpDir <- paste(path,"-tmp",sep="")
    dir.create(tmpDir,recursive=TRUE,showWarnings=FALSE)
    cwd <- getwd()
    setwd(tmpDir)
    # jvmr.alljars.server <- "localhost:9000"
    jvmr.alljars.server <- "dahl.byu.edu"
    jvmr.alljars.url <- paste("http://",jvmr.alljars.server,"/software/jvmr/alljars/",jvmr.version,sep="")
    jarURLs <- local({
      con <- file(paste(jvmr.alljars.url,"list.txt",sep="/"),open="r")
      lines <- readLines(con)
      close(con)
      paste(jvmr.alljars.url,lines,sep="/")
    })
    for ( jarURL in jarURLs ) {
      download.file(jarURL,basename(jarURL),mode = "wb",quiet=FALSE)
    }
    setwd(cwd)
    file.rename(tmpDir,path)
  }
}

path <- paste(dirname(system.file(".",package="jvmr")),.Platform$file.sep,"java",sep="")
download.jars(path)

