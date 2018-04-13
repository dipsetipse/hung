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
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

import java.awt.Color;

/* To test:
 * encrypted d901frm for PC use
 * http://localhost:8888/bossreport/funeralhomecontactxls?u=62574dd8f22ae9c0eb4330229da7bf6459837255183e214f&cem_num=901
 * 
 */
 
public class FuneralHomeContactxls extends HttpServlet 
{
  private static final String CONTENT_TYPE = "application/vnd.ms-excel"; //for MSWord: application/msword

  public void init(ServletConfig config) throws ServletException
  {
    super.init(config);

    // using Data Source Name
    String DATASOURCE = "jdbc/OracleDS";

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
        System.out.println ("user=" + userID);
        
        String cemNum = "";
        String cemNme = "";
        String micrfId = "";
        String[] decInfo = new String[8];
                
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
       
            String qry = "select num, nme from cemetery where num = substr(?, 2, 3)";
            
            try
            {
              pstmt = conn.prepareStatement(qry);
              pstmt.setString(1, userID);
              rs = pstmt.executeQuery();
                                                         
              if(rs.next()) // for single return
              {
                cemNum = rs.getString(1);
                cemNme = rs.getString(2);
              }
              
              rs.close();
              pstmt.close();
              
              res.setContentType(CONTENT_TYPE);
              res.setHeader("Content-Disposition","attachment; filename=FuneralHome_cem" + cemNum + ".xls");   // filename
              ServletOutputStream out= res.getOutputStream();

              //header in Excel
              out.println ("Funeral Home Contacts Report \n " +
                           cemNum + " " + cemNme + "\n");
              out.println("FH ID\t" + "FH NAME\t" +
                            "ADDRESS\t" +  "CITY\t" +
                            "STATE\t" + "ZIP CODE\t" + 
                            "PHONE\t" + "FAX\t" + 
                            "FIRST NAME\t" + "LAST NAME\t" + "EMAIL" 
                         );
                         
              qry = "";
              qry = "select fh.id, " +
                    "fh.nme, " +
                    "fh.line_one_addr || ' ' || fh.line_two_addr, " + 
                    "fh.city_nme, " + 
                    "fh.state_cd, " +
                    "fh.zip_five_cd || '-' || fh.zip_four_cd, " + 
                    "'(' || fh.phone_area_code_one_num || ')' || fh.phone_one_num, " + 
                    "'(' || fh.phone_area_code_two_num || ')' || fh.phone_two_num, " + 
                    "ci.first_nme, " +
                    "ci.last_nme, " +
                    "ci.email_one_addr " +
                    "from funeral_home fh, " +
                    "contact_information ci " +
                    "where fh.managing_cemetery_num = ? " + 
                    "and fh.id = ci.funeral_home_id " +
                    "and ci.status_cd = 'A' " +
                    "and ci.email_one_addr is not null " +
                    "order by fh.nme, ci.last_nme, ci.first_nme ";
                    
              pstmt = conn.prepareStatement(qry);
              pstmt.setString(1, cemNum); 
              rs = pstmt.executeQuery();
                                          
              while (rs.next()) 
              {  
                 micrfId = rs.getString(1);
                 ///decInfo = getDecInfo(micrfId, conn, res);
                 
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
                             Utility.blankIfNull(rs.getString(11)) 
                            );
              }
              
              out.close(); //close the Excel file

              rs.close();
              pstmt.close();
              conn.close();
              rs = null;
              pstmt = null; 
            }catch (SQLException e) {
                   Utility.displayErrMsg(res, conn, pstmt, 
                              "Error retrieving data" + 
                              "\n" + e.toString() + "\n" + qry);
                   return;
            }
            
  }      
  
  
   
}
