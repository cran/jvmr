package org.ddahl.jvmr;
/** Embeds an R interpreter within Java to allow the use of R's statistical methods.
 * The following examples are shown as if at the BeanShell prompt where ''bsh %'' indicates
 * a line of Java code.  This code can also be run in a Java program.
 *
 * A paper describing the software in detail is available here: <a href="http://dahl.byu.edu/software/jvmr/">http://dahl.byu.edu/software/jvmr/</a>
 *
 * @author David B. Dahl
 */
public class RInJava {
  private RInScala R;

  /** Creates an instance of the {@code RInJava} interpreter.  
   * The R interpreter constructs a connection 
   * between Java and R which can be used to assign variables, perform statistical methods, 
   * produce graphs, etc. in R.  Multiple R interpreters ({@code RInJava} objects)
   * are permitted and each R interpreter maintains its own workspace and memory.
   * 
   * <section style="background-color:#E8E8E8;">
   * <h1> Example: </h1>
   * <PRE>
   * bsh % import org.ddahl.jvmr.RInJava;
   * bsh % RInJava R = new RInJava();
   * 
   * <font color="green">// Variables and objects can now be created and accessed in R
   * 
   * // Assign variable "a" in R </font>
   * bsh % R.eval("a &lt;- 'This String was made in R'");
   * 
   * <font color="green">// Call the variable "a" in R and print the result in Java</font>
   * bsh % System.out.print(R.capture("a"));
   * [1] "This String was made in R"
   * </PRE>
   * </section>
   */
  public RInJava() {
    R = new RInScala();
  }

  /** Creates an instance of the {@code RInJava} interpreter using a specified path to access R.
   * This method only needs to be used if R is not contained in the current PATH.
   * @param command the path to a working version of R
   * 
   * <section style="background-color:#E8E8E8;">
   * <h1> Example: </h1>
   * <PRE>
   * bsh % import org.ddahl.jvmr.RInJava;
   * bsh % RInJava R = new RInJava("/opt/R/3.0.1/bin/R");
   * 
   * <font color="green">// Variables and objects can now be created and accessed in R
   * 
   * // Assign variable "a" in R </font>
   * bsh % R.eval("a &lt;- 'This String was made in R'");
   * 
   * <font color="green">// Call the variable "a" in R and print the result in Java</font>
   * bsh % System.out.print(R.capture("a"));
   * [1] "This String was made in R"
   * </PRE></section>
   */
  public RInJava(String command) {
    R = new RInScala(command);
  }


  /** For developer use only.  Creates an instance of the R interpreter using a specified path to 
   * access R and R options.
   * @param command a {@code String[]} specifying the R path and R options
   * 
   * <section style="background-color:#E8E8E8;">
   * <h1> Example: </h1>
   * <PRE>
   * bsh % import org.ddahl.jvmr.RInJava;
   *
   * bsh % RInJava R = new RInJava(new String[]{"/opt/R/3.0.1/bin/R","--vanilla","--silent","--slave","--interactive"});
   * </PRE></section>
   */ 
  public RInJava(String[] command) {
    R = new RInScala(command);
  }

  /** Returns the current working version of R.
   * @return Returns the current working version of R in a {@code String}
   */
  public String version() { return(R.version()); }

  /** Closes the R interpreter for this {@code RInJava} object. The {@code RInJava} object will still exist, 
   * but it will no longer interact with R.
   */
  public void quit() { R.quit(); }

  /** Starts a read-eval-print loop (REPL) session for R within Java.  Exit by entering ^D (ctrl+D).
   * Values defined in this session will be accessible by Java.
   * <section style="background-color:#E8E8E8;">
   * <h1>Example:</h1>
   * <PRE>
   * bsh % RInJava R = new RInJava();
   * bsh % R.prompt();
   * Welcome to the R prompt!  Type R commands.  No prompt is provided.  Exit by pressing ^D.
   * 
   * cat("You are in the R prompt!")
   * <font color=#00CC00>cat("You are in the R prompt!") 
   * You are in the R prompt! </font> 
   * 
   * f &lt;- function(x) x^2
   * <font color=#00CC00>f &lt;- function(x) x^2 </font>
   * 
   * f(3) 
   * <font color=#00CC00>f(3) 
   * [1] 9 </font>
   * 
   * Exiting R prompt. <font color="green">// Exit with ^D (ctrl+D)</font>
   *
   * bsh % System.out.print(R.capture("f(5)"));
   * [1] 25
   * </PRE>
   * </section>
   */
  public void prompt() { R.prompt(); }

  /** Evaluates R code. 
   * @param expression an R expression
   * 
   * <section style="background-color:#E8E8E8;">
   * <h1>Example:</h1>
   * <PRE>
   * bsh % RInJava R = new RInJava();
   * 
   * bsh % R.eval("pval &lt;- 1 - pf(3,3,2)");
   * bsh % System.out.print(R.capture("pval"));
   * [1] 0.2599267
   * 
   * <font color="green">// Multi-line example </font>
   * bsh % R.eval("set.seed(24)\n" +
   *              "draws &lt;- rgamma(100,1,2.3)\n" +
   *              "stdev &lt;- sd(draws)");
   * bsh % System.out.print(R.capture("stdev"));
   * [1] 0.4903072
   * 
   * </PRE>
   * </section>
   */ 
  public void eval(String expression) { R.eval(expression); }

  /** Evaluates R code and returns a Java {@code String} of the R output to Java.
   * @param expression an R expression
   * @return Returns a Java {@code String} of R output.
   * <section style="background-color:#E8E8E8;">
   * <h1>Example:</h1>
   * <PRE>
   * bsh % RInJava R = new RInJava();
   * 
   * bsh % System.out.print(R.capture("pnorm(-1.75)"));
   * [1] 0.04005916
   * <PRE></section>
   */
  public String capture(String expression) { return(R.capture(expression)); }
  
  /** Creates a generic Java object from an R object.
   * @param expression an R expression
   * @return Returns a Java representation of an R object.
   * 
   * <section style="background-color:#E8E8E8;">
   * <h1>Example:</h1>
   * <PRE>
   * RInJava R = new RInJava();
   * 
   * <font color="green">// Primitives</font>
   * bsh % System.out.print((String) R.apply("'A string made in R'")[0]);
   * A string made in R
   * 
   * <font color="green">// Vectors</font> 
   * bsh % System.out.print((double[])  R.apply("rgamma(2,1.1,2.5)"));
   * double []: {
   * 0.26015648962741816,
   * 0.2268048992444917,
   * }
   * 
   * <font color="green">// Matrices</font>
   * bsh % System.out.print((int[][]) R.apply("matrix(as.integer(1:4),nrow=2,ncol=2)"));
   * [I [][]: {
   * [I@282b3223,
   * [I@2be8ac6f,
   * }
   * </PRE></section>
   */  
  public Object apply(String expression) { return(R.apply(expression)); }


  /** Pulls a character object from R into Java.
   * @param expression an R expression
   * @return Returns a Java {@code String} representation of an R character vector of length one
   *
   * <section style="background-color:#E8E8E8;">
   * <h1>Example:</h1>
   * <PRE>
   * RInJava R = new RInJava();
   * 
   * bsh % System.out.print(R.toPrimitiveString("'Hello World!'"));
   * Hello World!
   * 
   * bsh % R.eval("a &lt;- 'Words from an R object.'");
   * bsh % System.out.print(R.toPrimitiveString("a"));
   * Words from an R object.
   * </PRE><section>
   */ 
  public String  toPrimitiveString(String expression) { return((String)R.toPrimitive(expression)); }

  /** Pulls a double from R into Java.
   * @param expression an R expression
   * @return Returns a Java {@code double} representation of an R double.
   * 
   * <section style="background-color:#E8E8E8;">
   * <h1>Example:</h1>
   * <PRE>
   * RInJava R = new RInJava();
   * 
   * bsh % System.out.print(R.toPrimitiveDouble("qgamma(.01,1,1)"));
   * 0.010050335853501442
   * 
   * bsh % R.eval("quant &lt;- qnorm(.05,1,3)");
   * bsh % System.out.print(R.toPrimitiveDouble("quant"));
   * -3.934560880854418
   * </PRE></section>
   */ 
  public double  toPrimitiveDouble(String expression) { return(((Double)R.toPrimitive(expression)).doubleValue()); }

  /** Pulls an integer from R into Java. 
   * @param expression an R expression
   * @return Returns a Java {@code int} representation of an R integer.
   * 
   * <section style="background-color:#E8E8E8;">
   * <h1>Example:</h1>
   * <PRE>
   * RInJava R = new RInJava();
   *
   * bsh % R.eval("samp &lt;- sample(1:5,5)");
   * bsh % System.out.print(R.capture("samp"));
   * [1] 5 1 4 2 3
   * 
   * bsh % System.out.print(R.toPrimitiveInt("which(samp == 3)"));
   * 5
   * </PRE></section>
   */
  public int     toPrimitiveInt(String expression) { return(((Integer)R.toPrimitive(expression)).intValue()); }

  /** Pulls a boolean value from R into Java.
   * @param expression an R expression
   * @return Returns a Java {@code boolean} representation of an R boolean value. 
   * 
   * <section style="background-color:#E8E8E8;">
   * <h1>Example:</h1>
   * <PRE>
   * RInJava R = new RInJava();
   *
   * bsh % System.out.print(R.toPrimitiveBoolean("is.numeric('yarn')"));
   * false
   * </PRE></section>
   */
  public boolean toPrimitiveBoolean(String expression) { return(((Boolean)R.toPrimitive(expression)).booleanValue()); }



  /** Pulls a character vector from R into Java.
   * @param expression an R expression
   * @return Returns a Java {@code String[]} representation of an R character vector. 
   * 
   * <section style="background-color:#E8E8E8;">
   * <h1>Example:</h1>
   * <PRE>
   * RInJava R = new RInJava();
   *
   * bsh % System.out.print(R.toVectorString("c('Hello','World','!')"));
   * java.lang.String []: {
   * Hello,
   * World,
   * !,
   * }
   * 
   * bsh % R.eval("greeting &lt;- c('Hello','World','Again','!')");
   * bsh % System.out.print(R.toVectorString("greeting"));
   * java.lang.String []: {
   * Hello,
   * World,
   * Again,
   * !,
   * }
   * </PRE><section>
   */
  public String[]  toVectorString(String expression) { return((String[])R.toVector(expression)); }

  /** Pulls a vector of doubles from R into Java.
   * @param expression an R expression
   * @return Returns a Java {@code double[]} representation of an R vector of doubles.
   * 
   * <section style="background-color:#E8E8E8;">
   * <h1>Example:</h1>
   * <PRE>
   * RInJava R = new RInJava();
   *
   * bsh % System.out.print(R.toVectorDouble("rbeta(3,2,2)"));
   * double []: {
   * 0.23974742811526553,
   * 0.662657484046741,
   * 0.13251517763719986,
   * }
   * 
   * bsh % R.eval("stats &lt;- c(mean(1:5),sd(1:5))");
   * bsh % System.out.print(R.toVectorDouble("stats"));
   * double []: {
   * 3.0,
   * 1.5811388300841898,
   * }
   * </PRE></section>
   */
  public double[]  toVectorDouble(String expression) { return(((double[])R.toVector(expression))); }

  /** Pulls a vector of integers from R into Java.
   * @param expression an R expression
   * @return Returns a Java {@code int[]} representation of an R vector of integers.
   * 
   * <section style="background-color:#E8E8E8;">
   * <h1>Example:</h1>
   * <PRE>
   * RInJava R = new RInJava();
   *
   * bsh % System.out.print(R.toVectorInt("which(sample(1:5) &lt; 3)"));
   * int []: {
   * 3,
   * 4,
   * }
   * 
   * bsh % R.eval("samp &lt;- rbeta(5,5,3)");
   * bsh % System.out.print(R.capture("samp"));
   * [1] 0.6824862 0.3900250 0.5871422 0.4112696 0.7930882
   * 
   * bsh % System.out.print(R.toVectorInt("which(samp &lt; .5)"));
   * int []: {
   * 2,
   * 4,
   * }
   * </PRE></section>
   */
  public int[]     toVectorInt(String expression) { return(((int[])R.toVector(expression))); }

  /** Pulls a vector of boolean values from R into Java.
   * @param expression an R expression 
   * @return Returns a Java {@code boolean[]} representation of an R vector of booleans.
   * 
   * <section style="background-color:#E8E8E8;">
   * <h1>Example:</h1>
   * <PRE>
   * RInJava R = new RInJava();
   *
   * bsh % System.out.print(R.toVectorBoolean("c(TRUE,FALSE,FALSE)"));
   * boolean []: {
   * true,
   * false,
   * false,
   * }
   * 
   * bsh % R.eval("myVector &lt;- c(NA,3,NA,5,7,NA)");
   * bsh % System.out.print(R.toVectorBoolean("is.na(myVector)"));
   * boolean []: {
   * true,
   * false,
   * true,
   * false,
   * false,
   * true,
   * }
   * </PRE><section>
   */
  public boolean[] toVectorBoolean(String expression) { return(((boolean[])R.toVector(expression))); }




  /** Pulls a character matrix from R into Java. 
   * @param expression an R expression 
   * @return Returns a Java {@code String[][]} representation of an R character matrix.
   * 
   * <section style="background-color:#E8E8E8;">
   * <h1>Example:</h1>
   * <PRE>
   * RInJava R = new RInJava();
   *
   * bsh % String[][] mtx = R.toMatrixString( "matrix(c('This ','is ','a ','matrix.'),nrow=2,ncol=2)");
   * bsh % System.out.print(mtx[0][0] + mtx[1][0] + mtx[0][1] + mtx[1][1]);
   * This is a matrix.
   * </PRE></section>
   */  
  public String[][]  toMatrixString(String expression) { return((String[][])R.toVector(expression)); }

  /** Pulls a matrix of doubles from R into Java.
   * @param expression an R expression 
   * @return Returns a Java {@code double[][]} representation of an R matrix of doubles.
   * 
   * <section style="background-color:#E8E8E8;">
   * <h1>Example:</h1>
   * <PRE>
   * RInJava R = new RInJava();
   * 
   * bsh % R.eval("mtx &lt;- matrix(c(seq(-3,3,.1),pnorm(seq(-3,3,.1))),ncol=2,
   *          dimnames=list(paste('row',1:61,sep=''),c('z-score','P&lt;z')))");
   * bsh % System.out.print(R.capture("head(mtx)"));
   *      z-score         P&lt;z
   * row1    -3.0 0.001349898
   * row2    -2.9 0.001865813
   * row3    -2.8 0.002555130
   * row4    -2.7 0.003466974
   * row5    -2.6 0.004661188
   * row6    -2.5 0.006209665
   * 
   * bsh % double[][] myMatrix = R.toMatrixDouble("mtx");
   * 
   * <font color="green">// Find the proportion of the standard normal curve less than -2.5 </font>
   * 
   * bsh % System.out.print(myMatrix[5][1]);
   * 0.006209665325776135
   * </PRE></section>
   */
  public double[][]  toMatrixDouble(String expression) { return(((double[][])R.toVector(expression))); }

  /** Pulls a matrix of integers from R into Java. 
   * @param expression an R expression
   * @return Returns a Java {@code int[][]} representation of an R matrix of integers.
   * 
   * <section style="background-color:#E8E8E8;">
   * <h1>Example:</h1>
   * <PRE>
   * RInJava R = new RInJava();
   * 
   * bsh % System.out.print(R.toMatrixInt("matrix(as.integer(c(3424,23,3,5)),nrow=2,ncol=2)")[0][0]);
   * 3424
   * 
   * <font color="green">// Alternately:</font>
   * bsh % int[][] myIntMatrix = R.toMatrixInt("matrix(as.integer(c(3424,23,3,5)),nrow=2,ncol=2)");
   * bsh % System.out.print(myIntMatrix[0][0]);
   * 3424
   * </PRE></section>
   */
  public int[][]     toMatrixInt(String expression) { return(((int[][])R.toVector(expression))); }

  /** Pulls a matrix of boolean values from R into Java.
   * @param expression an R expression 
   * @return Returns a Java {@code boolean[][]} representation of an R matrix of booleans.
   * 
   * <section style="background-color:#E8E8E8;">
   * <h1>Example:</h1>
   * <PRE>
   * RInJava R = new RInJava();
   * 
   * bsh % R.eval("mtx &lt;- is.na(matrix(c(NA,2,NA,5),nrow=2,ncol=2))");
   * bsh % boolean[][] myBoolMtx = R.toMatrixBoolean("mtx");
   * bsh % System.out.print(myBoolMtx[0][0]);
   * true
   * </PRE><section>
   */
  public boolean[][] toMatrixBoolean(String expression) { return(((boolean[][])R.toVector(expression))); }




  /** Assigns a Java object to a variable in R.
   * @param expression name of the R variable
   * @param value a Java object
   * 
   * <section style="background-color:#E8E8E8;">
   * <h1>Example:</h1>
   * <PRE>
   * RInJava R = new RInJava();
   *
   * <font color="green">// Primitives </font>
   * bsh % R.update("rvar","Java String assigned to an R variable");
   * bsh % System.out.print(R.capture("rvar"));
   * [1] "Java String assigned to an R variable"
   * 
   * bsh % R.update("rdouble",3.7);
   * bsh % System.out.print(R.capture("rdouble"));
   * [1] 3.7
   * 
   * 
   * <font color="green">// Arrays </font>
   * bsh % double[] myArray = {1.1,2.2,3.3};
   * bsh % R.update("rarray",myArray);
   * bsh % System.out.print(R.capture("rarray"));
   * [1] 1.1 2.2 3.3
   * 
   *
   * <font color="green">// Matrices </font>
   * bsh % double[][] myMatrix = {{1.1,1.2},{2.1,2.2},{3.1,3.2}};
   * bsh % R.update("rmatrix",myMatrix);
   * bsh % System.out.print(R.capture("rmatrix"));
   *      [,1] [,2]
   * [1,]  1.1  1.2
   * [2,]  2.1  2.2
   * [3,]  3.1  3.2
   * </PRE></section>
   */ 
  public void update(String expression, Object value) { R.update(expression,value); }

}
