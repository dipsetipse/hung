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
 * http://localhost:8888/bossreport/monordernotrecvbytypexls?u=62574dd8f22ae9c0eb4330229da7bf6459837255183e214f&cem_num=901&start_dt=01/01/2006&end_dt=01/01/2007
 * 
 */
 
public class MonumentOrderedButNotReceivedByTypexls extends HttpServlet 
{
  private static final String CONTENT_TYPE = "application/vnd.ms-excel"; //for MSWord: application/msword

  public void init(ServletConfig config) throws ServletException
  {
    super.init(config);

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
        
        String monuType = req.getParameter("type");
        
        String cemNum = "";
        String cemNme = "";
        String magCem = "";
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
              
              qry = "";
              qry = "select managing_station_num from cemetery where num = ? ";
              
              pstmt = conn.prepareStatement(qry);
              pstmt.setString(1, cemNum);
              rs = pstmt.executeQuery();  
              
              if(rs.next()) // for single return
              {
               magCem = rs.getString(1);
              }
              
              rs.close();
              pstmt.close();
                            
              res.setContentType(CONTENT_TYPE);
              res.setHeader("Content-Disposition","attachment; filename=MonOrderNotRecType_cem" + cemNum + ".xls");   // filename
              ServletOutputStream out= res.getOutputStream();

              //header in Excel
              out.println ("Monuments Ordered But Not Received By Type Report \n " +
                           cemNum + " " + cemNme + "\n");
              out.println("INTER DT\t" + "DEC ID\t" + 
                            "FIRST NAME\t" + "LAST NAME\t" +
                            "TC\t" + "CEM\t" + "SEC ID\t" + "ROW NUM\t" + "SITE NUM\t" +
                            "SHIP FY\t" + "SHIP PRE\t" + "CBL\t" + "RELEASE DT\t" + "ORDER DT" 
                         );
                         
              qry = "";
              qry = "select id, " +
                    "monument_type_cd, " +
                    "to_char(order_release_dt, 'mm/dd/yyyy'), " + 
                    "to_char(amas_order_dt, 'mm/dd/yyyy'), " + 
                    "shipment_fy, " +
                    "ship_prefix, " + 
                    "cbl_num " +
                    "from monument " +
                    "where managing_cemetery_num = ? " + 
                    "and monument_type_cd = ? " +
                    "and amas_order_dt is not null " +
                    "and received_dt is null " +
                    "and ship_prefix is not null " +
                    "order by amas_order_dt";
                    
              pstmt = conn.prepareStatement(qry);
              pstmt.setString(1, magCem); 
              pstmt.setString(2, monuType);
              rs = pstmt.executeQuery();
                                          
              while (rs.next()) 
              {  
                 micrfId = rs.getString(1);
                 decInfo = getDecInfo(micrfId, conn, res);
                 
                 out.println(Utility.blankIfNull(decInfo[0]) + "\t" + //interment_dt
                             Utility.blankIfNull(decInfo[1]) + "\t" + //dec_ID
                             Utility.blankIfNull(decInfo[2]) + "\t" + //first_nme
                             Utility.blankIfNull(decInfo[3]) + "\t" + //last_nme
                             Utility.blankIfNull(rs.getString(2)) + "\t" + //monument_type_cd
                             Utility.blankIfNull(decInfo[4]) + "\t" + //cem_num
                             Utility.blankIfNull(decInfo[5]) + "\t" + //section_id
                             Utility.blankIfNull(decInfo[6]) + "\t" + //row_num
                             Utility.blankIfNull(decInfo[7]) + "\t" + //site_num
                             Utility.blankIfNull(rs.getString(5)) + "\t" + //shipment_fy
                             Utility.blankIfNull(rs.getString(6)) + "\t" + //ship_prefix
                             Utility.blankIfNull(rs.getString(7)) + "\t" + //cbl_num
                             Utility.blankIfNull(rs.getString(3)) + "\t" + //release_dt
                             Utility.blankIfNull(rs.getString(4)) //order_dt
                            );
              }
              
              out.close(); //close the Excel file

              rs.close();
              pstmt.close();
              conn.close();
              rs = null;
              pstmt = null; 
              conn = null;
              
            }catch (SQLException e) {
                   Utility.displayErrMsg(res, conn, pstmt, 
                              "Error retrieving data" + 
                              "\n" + e.toString() + "\n" + qry);
                   return;
            }
            
  }        
   
   public static String[] getDecInfo(String micrfId,
                                   Connection conn, HttpServletResponse res) throws IOException
                                   // add res as in catch exception has displayErrMsg(res,conn,psmt, ..) 
   { 
        String cemNum = "";
        String intermentDt = "";
        String decId = "";
        String firstNme = "";
        String lastNme = "";
        String sectionId = "";
        String rowNum = "";
        String siteNum = "";   
        String[] arrayString = new String[8];
        
        String qry = "";
        PreparedStatement pstmt = null;

            try {
                      qry = "select di.first_nme,  " +
                            "substr(di.last_nme, 1, 24), " +
                            "dm.decedent_id, " +
                            "to_char(ga.interment_dt, 'mm/dd/yyyy'), " +
                            "ga.cemetery_num, " +
                            "ga.section_id, " +
                            "ga.row_num, " +
                            "ga.site_num " +
                            "from decedent_information di, " +
                            "gravesite_assignment ga, " +
                            "decedent_monument dm " +
                            "where dm.monument_id = ? " +
                            "  and dm.decedent_id = di.id " +
                            "  and dm.decedent_id = ga.decedent_id " +
                            "  and ga.disinterment_dt is null " +
                            "  and inscr_death_dt = (select unique(max(inscr_death_dt)) " +
                                                    "from decedent_monument " +
                                                    "where monument_id = ? )";
                                                               
                      pstmt = conn.prepareStatement(qry);
                      pstmt.setString(1, micrfId); 
                      pstmt.setString(2, micrfId);
                      ResultSet rs = pstmt.executeQuery();
     
                      if (rs.next()) 
                      {
                        firstNme = rs.getString(1); 
                        lastNme = rs.getString(2);
                        decId = rs.getString(3);
                        intermentDt = rs.getString(4);
                        cemNum = rs.getString(5);
                        sectionId = rs.getString(6);
                        rowNum = rs.getString(7);
                        siteNum = rs.getString(8);
                        arrayString[0] = intermentDt;
                        arrayString[1] = decId;
                        arrayString[2] = firstNme;
                        arrayString[3] = lastNme;
                        arrayString[4] = cemNum;
                        arrayString[5] = sectionId;
                        arrayString[6] = rowNum;
                        arrayString[7] = siteNum;
                      } 

                      rs.close();
                      pstmt.close();
                      rs = null;
                      pstmt = null;

                } catch (SQLException e) {
                    System.err.println(new java.util.Date() + 
                                       " Error retrieving subdecedebt " + "\n" + qry );
                    Utility.displayErrMsg(res, conn, pstmt,
                              " Exception " + 
                              "\n" +  qry + " mID= " + micrfId);                //return;
               }

               return arrayString;
   }

}
