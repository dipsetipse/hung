/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.va.cem.bossreport.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author cemMenorD
 */
public class Utility {
   /** 
   * Method to login
   */
   public static Connection logon( String user_pw, HttpServletRequest req, HttpServletResponse res) throws IOException { 

      //PreparedStatement pstmt = null;
      Connection conn = null;
      String user = "";
      String pw = "";
      String database = "";
      String hostname = "";
      String port = "";

      String dbConnectFile =  req.getSession().getServletContext().getInitParameter("dbPropertiesPath");       
      String appPath = req.getSession().getServletContext().getInitParameter("application_file_path");
   
      if (user_pw != ""  && user_pw != null) {
          if (user_pw.indexOf(",") > 0) { //if there is comma
                     //System.out.println(", pos=" + uid.indexOf(","));
                     //System.out.println("user=" + uid.substring(0, uid.indexOf(",")));
                     //System.out.println("pw=" + uid.substring(uid.indexOf(",")+1));
            user= user_pw.substring(0,user_pw.indexOf(","));
            pw= user_pw.substring(user_pw.indexOf(",")+1);
          } else {
                  pw="unknown?";
          }
       //} else {
       //     uid = "boss"; 
       //     pw = "develop";
       }
      
      Properties props = new Properties();
        try {
            
            FileInputStream dbFile = new FileInputStream(appPath+dbConnectFile);
            props.load(dbFile);
            database = props.getProperty("jdbc.service");
            hostname = props.getProperty("jdbc.host");
            port =  props.getProperty("jdbc.port");
            dbFile.close();
        } 
        catch (IOException e) 
        {
           System.err.println("db prop read Error " );
           ServletOutputStream out = res.getOutputStream();
           StringWriter errors = new StringWriter();
           e.printStackTrace(new PrintWriter(errors));
           res.setContentType("text/html");
           out.println("<html><head><title>Boss Reports</title></head>");
           //out.println("<body><h4><font color='red'>Image Display Error=" + e.getMessage() + "</font></h4></body></html>");
           out.println("<body>Utility.logon db.properties file read Error " + e.getMessage() + "</br></br>" + errors.toString() + "</br></br>" +
              "</body></html>");
             
            return conn;
        
        }
        catch (Exception e) 
        {
           System.err.println("db prop read Error " );
           ServletOutputStream out = res.getOutputStream();
           StringWriter errors = new StringWriter();
           e.printStackTrace(new PrintWriter(errors));
           res.setContentType("text/html");
           out.println("<html><head><title>Boss Reports</title></head>");
           //out.println("<body><h4><font color='red'>Image Display Error=" + e.getMessage() + "</font></h4></body></html>");
           out.println("<body>Utility.logon db prop read Error #2 " + e.getMessage() + "</br></br>" + errors.toString() + "</br></br>" +
              "</body></html>");

            return conn;
        
        }
       try {
           Class.forName("oracle.jdbc.OracleDriver");        
           String url = "jdbc:oracle:thin:@//" + hostname + ":" + port + "/" + database;
           conn = DriverManager.getConnection(url, user, pw);
           return conn;
       }       
        catch (ClassNotFoundException e) {
           //Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
           System.err.println("OracleDriver Error " );
           ServletOutputStream out = res.getOutputStream();
           StringWriter errors = new StringWriter();
           e.printStackTrace(new PrintWriter(errors));
           res.setContentType("text/html");
           out.println("<html><head><title>Boss Reports</title></head>");
           //out.println("<body><h4><font color='red'>Image Display Error=" + e.getMessage() + "</font></h4></body></html>");
           out.println("<body>Utility.logon OracleDriver Error " + e.getMessage() + "</br></br>" + errors.toString() + "</br></br>" +
              "</body></html>");

            return conn;
           
       }
         catch (SQLException e) {
           //Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
           System.err.println("Connection Error userID=" + user );
           ServletOutputStream out = res.getOutputStream();
           StringWriter errors = new StringWriter();
           e.printStackTrace(new PrintWriter(errors));
           res.setContentType("text/html");
           out.println("<html><head><title>Boss Reports</title></head>");
           //out.println("<body><h4><font color='red'>Image Display Error=" + e.getMessage() + "</font></h4></body></html>");
           out.println("<body>Utility.logon Connection Error userID=" + user + " " + e.getMessage() + "</br></br>" + errors.toString() + "</br></br>" +
              "</body></html>");
           return conn;
       }
   }
   /** 
   * Method to enable pw protected roles
   */
   //private static void setRole(Connection conn, String pw) { 
   // changed to return a value.
   public static boolean setRole(Connection conn, String pw) { 
      boolean continueFlag=true;
      Statement stmt=null;
      ResultSet rs=null;
      String qry="select granted_role from user_role_privs";
      try {
          stmt=conn.createStatement();
          rs=stmt.executeQuery(qry);

          qry="set role";
          while (rs.next()) {
                 qry=qry + " " + rs.getString(1) + " identified by " + pw + ",";
          }
          qry = qry.substring(0,qry.length()-1); //remove comma
            rs.close();

          stmt.executeUpdate(qry);
            stmt.close();
          return continueFlag;
      } catch (SQLException e){
          System.err.println ("Utility.setRole Error " + qry + " " + e.toString());
          return false;
     }
   } // end setRole
   
      /** 
   * Method to insert two ticks if there is one tick
   */
   private static String insertExtraTick(String s) { 
      /* Note: Two String variable types cannot be compared with "==" operator,
         you have to use object.equals(object2)
            System.out.println("Example"); 
            String srchString = "Y";
            String x          = "Y";
            System.out.println(x + "==" + srchString + " " + (x == srchString)); //returns false, always
            System.out.println(x + " equals " + srchString + " " + (x.equals(srchString))); //returns true if values are equal
         "equals" implements "value comparison", while "==" implements "handle comparison" 
      
      "substring" notes
         System.out.println(" i=0 " + s.substring(0,1));  (returns 1st letter)
         System.out.println(" i=1 " + s.substring(1,2));  (returns 2nd letter) 
         System.out.println(" i=2 " + s.substring(2,3));  (returns 3rd letter, etc)
      */

      String newString = new String();
      for (int i=0; i<s.length(); i++ )  {
           if (s.substring(i,i+1).equals("'"))     //if it is tick, insert extra tick
              newString = newString + "''";  
           else
           if (s.substring(i,i+1).equals("´"))  //it is a tick look-alike, insert 2 ticks
              newString = newString + "''"; 
           else
              newString = newString + s.substring(i,i+1);
      }

      return (newString); 
   } 

   /** 
   * Method to return a blank string if input is null
   */
   public static String blankIfNull(String inputString) { 

      return inputString == null ? " " : inputString;

/* using the conditional operator ?
if (a > b) {
  max = a;
}
else {
  max = b;
}

Setting a single variable to one of two states based on a single condition is such a common use of if-else that a shortcut has been devised for it, the conditional operator, ?:. Using the conditional operator you can rewrite the above example in a single line like this: 

max = (a > b) ? a : b;

(a > b) ? a : b; is an expression which returns one of two values, a or b. The condition, (a > b), is tested. If it is true the first value, a, is returned. If it is false, the second value, b, is returned. Whichever value is returned is dependent on the conditional test, a > b. The condition can be any expression which returns a boolean value.

*/
   } //blankIfNull

   /** 
   * Method to return blank if input is null, note int can't be null
   */
   public static String blankIfNull(int inputInt) { 

      return Integer.toString(inputInt); // //(inputInt == null) ? " " : Integer.toString(inputInt);

   } //blankIfNull

  public static void displayErrMsg ( HttpServletResponse p_response, Connection myConn, PreparedStatement myStmt, String errMsg) 
    throws IOException {

     ServletOutputStream l_out=p_response.getOutputStream();

     l_out.println(" <HTML> ");
     l_out.println(" <HEAD> ");
     l_out.println(" <TITLE> ");
     l_out.println(" Unexpected Error ");
     l_out.println(" </TITLE> ");
     l_out.println(" <BODY> ");
     l_out.println(errMsg);
     l_out.println(" </BODY> ");
     l_out.println(" </HTML> ");
     l_out.println(" </HEAD> ");
     l_out.close();

     try {
        if (myStmt != null) {
            myStmt.close();
            myStmt = null;
        }
        if (myConn != null) {
            myConn.close();
            myConn = null;
        }
     } catch (SQLException e) {
         System.err.println ("Error closing connection or statement " + e.toString());
     }

  } //end displayErrMsg

}
