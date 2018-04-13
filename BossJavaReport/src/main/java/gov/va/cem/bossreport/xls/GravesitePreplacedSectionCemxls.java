/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.va.cem.bossreport.xls;

import gov.va.cem.bossreport.util.Encryption;
import gov.va.cem.bossreport.util.Utility;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import javax.sql.DataSource;

import java.awt.Color;


/**
 *
 * @author cemNguyeH
 */
public class GravesitePreplacedSectionCemxls extends HttpServlet 
{
  private static final String CONTENT_TYPE = "application/vnd.ms-excel"; // for MSWord: application/msword, "text/html; charset=windows-1252";

  private DataSource ods = null;

  public void init(ServletConfig config) throws ServletException
  {
    super.init(config);

    // using Data Source Name
    String DATASOURCE = "jdbc/OracleDS";

    try {
        InitialContext ic = new InitialContext();
        ods = (DataSource)ic.lookup(DATASOURCE);
    } catch (javax.naming.NamingException e) {
        System.out.println("Production Datasource error - javax.naming.NamingException " + 
                           e.toString());
    }
  }
    

  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
  {

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        //get parameters
        //u & p are in hex and combined separated by comma
        String userID = req.getParameter("u");
        userID = (Encryption.decrypt(userID));

        String cemNum = req.getParameter("cem_num");
        String startDT = req.getParameter("start_dt");
        String cemNme = "";
           
        conn = Utility.logon (userID, req, res);

        if (conn == null) {
            //displayErrMsg(res, conn, pstmt, 
            //              "Error Connecting to the database "); // + userID);
            return;
        }

        if (!(Utility.setRole(conn, "nca_sic"))) {
            Utility.displayErrMsg(res, conn, pstmt, 
                          "Error accessing data on the database.");
            return;
        }

           String qry = "select nme from cemetery where num = ?";
            
            try
            {
              pstmt = conn.prepareStatement(qry);
              pstmt.setString(1, cemNum);
              rs = pstmt.executeQuery();
                                                         
              if(rs.next()) // for single return
              {
                cemNme = rs.getString(1);
              }
              
              rs.close();
              pstmt.close();

              rs = null;
              pstmt = null; 
            }catch (SQLException e) {
                System.err.println(new java.util.Date() + 
                                   " Error retrieving data " + "\n" + qry );
            }
            
            qry = "select g.cemetery_num, " +
                  "c.nme, " + 
                  "g.cemetery_type, " + 
                  "g.msn_id, " +
                  "g.section_id, " + 
                  "g.district_id, " +
                  "d.nme, " +
                  "decode(g.del_ind,'Y','Delete','ADD New'), " +
                  "g.del_reason, " +
                  "to_char(g.insert_dt,'MM/DD/YYYY HH24:MI:SS'), " + 
                  "g.last_update_user_id, " + 
                  "to_char(g.last_update_dt,'MM/DD/YYYY HH24:MI:SS') " +
                  "from gravesite_preplaced_sections g, " +
                  "  cemetery c, " +
                  "  district d " + 
                  "where g.cemetery_num = ? and " +
                  "  g.last_update_dt >= to_date( ?, 'MM/DD/YYYY') and " + 
                  "  g.cemetery_num = c.num and " +
                  "  g.district_id = d.id " +
                  "order by g.cemetery_num, g.last_update_dt";

        try {

                pstmt = conn.prepareStatement(qry);
                pstmt.setString(1, cemNum); //if you want to add parameter in the query
                pstmt.setString(2, startDT); 
                rs = pstmt.executeQuery();

                //instruct the browser to send output as Excel
                res.setContentType(CONTENT_TYPE);
                res.setHeader("Content-Disposition","attachment; filename=GPS_cem" + cemNum + " " + startDT + ".xls");   // filename
                ServletOutputStream out= res.getOutputStream();

                //header in Excel
                out.println ("History-Status Report \n" + 
                             cemNum + " " + cemNme + ", " + startDT + "\n" );
                            
                out.println("CEMETERY NUM\t" + "CEMETERY NAME\t" + 
                            "CEMETERY TYPE\t" + "MSN_ID\t" +
                            "SECTION_ID\t" + "DISTRICT ID\t" + 
                            "DISTRICT NAME\t" + "ACTION REQUESTED\t" + 
                            "REASON FOR DELETE\t" + "INSERT DATE\t" +
                            "USER ID\t" + "UPDATE DATE"
                           );

                //send data to Excel
                while (rs.next()) {
                    out.println(Utility.blankIfNull(rs.getString(1)) + "\t" + 
                                Utility.blankIfNull(rs.getString(2)) + "\t" + 
                                Utility.blankIfNull(rs.getString(3)) + "\t" + 
                                Utility.blankIfNull(rs.getString(4)) + "\t" +                                                
                                Utility.blankIfNull(rs.getString(5)) + "\t" +
                                Utility.blankIfNull(rs.getString(6)) + "\t" +
                                Utility.blankIfNull(rs.getString(7)) + "\t" +
                                Utility.blankIfNull(rs.getString(8)) + "\t" +
                                Utility.blankIfNull(rs.getString(9)) + "\t" +
                                Utility.blankIfNull(rs.getString(10)) + "\t" +
                                Utility.blankIfNull(rs.getString(11)) + "\t" +
                                Utility.blankIfNull(rs.getString(12))
                               );
                }

                out.close(); //close the Excel file

                rs.close();
                pstmt.close();
                conn.close();
                rs = null;
                pstmt = null;
                conn = null;

        } catch (SQLException e) {
                Utility.displayErrMsg(res, conn, pstmt, 
                              "Error retrieving data" + 
                              "\n" + e.toString() + "\n" + qry);
                return;
        }
  }

   public void destroy(Connection conn) { 
 
      try {  
         conn.close(); 
      } 
      catch (Exception ignored) {System.err.println("GPS Report - Error in method destroy"); }  
   } 

  

}
