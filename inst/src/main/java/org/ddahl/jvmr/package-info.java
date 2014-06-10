/** Provides the classes necessary to use R within Java via a high-level interface.
 * This interface has simple syntax which accesses R's abilities within Java.  It allows the user 
 * to seamlessly incorporate both R and Java code into one program via an interpreter.
 * 
 * A paper describing the software in detail is available here: <a href="http://dahl.byu.edu/software/jvmr/">http://dahl.byu.edu/software/jvmr/</a>
 *
 * <section style="background-color:#E8E8E8;">
 * <h1>Example:</h1>
 * <PRE>
 * bsh % import org.ddahl.jvmr.RInJava;
 * bsh % RInJava R = new RInJava();   
 * bsh % double zScore = -3.5;
 * bsh % R.update("rzscore",zScore);<font color="green">                          //Pull a Java value into R</font>
 * bsh % double pVal = R.toPrimitiveDouble("pnorm(rzscore)");<font color="green"> //Pull an R value into Java</font>
 * bsh % print("The p-value is: " +  pVal);
 * The p-value is: 2.3262907903552494E-4
 * </PRE><section>
 * @author David B. Dahl
 */
package org.ddahl.jvmr;
