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
 * http://localhost:8888/bossreport/iabydatexls?u=62574dd8f22ae9c0eb4330229da7bf6459837255183e214f&cem_num=901&start_dt=01/01/2006&end_dt=01/01/2007
 * 
 */
 
public class InscriptionApprovedByDatexls extends HttpServlet 
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
        
        String cemNum = req.getParameter("cem_num");
        String startDt = req.getParameter("start_dt");
        String endDt = req.getParameter("end_dt");
        String cemNme = "";
        String magCem = "";
        String monId = "";
        String monTypeCd = "";
        String replaceReaCd = "";
        String replReaCd = "";
        String repReaCd = "";
        String decId = "";
        String schDate = "";
        String[] decMon = new String[2];
        String[] graveInfo = new String[2];
        String[] decInfo = new String[2];
                
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
       
            String qry = "select nme from cemetery where num = ? ";
            
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
              
              qry = "";
              qry = "select managing_station_num " +
                    "from cemetery where num = ?"; //better to set num as setString(cemNUM)
              
              pstmt = conn.prepareStatement(qry);
              pstmt.setString(1, cemNum);
              rs = pstmt.executeQuery();  
              
              if(rs.next()) // for single return
              {
               magCem = rs.getString(1);
              }
              
              rs.close();
              pstmt.close();
              
              rs = null;
              pstmt = null; 
            }catch (SQLException e) {
                System.err.println(new java.util.Date() + 
                                   " Error retrieving data " + "\n" + qry );
            }
                                        
              res.setContentType(CONTENT_TYPE);
              res.setHeader("Content-Disposition","attachment; filename=iabydate_cem" + cemNum + ".xls");   // filename
              ServletOutputStream out= res.getOutputStream();

              //header in Excel
              out.println ("Inscription Approved Report \n " +
                           cemNum + " " + cemNme + "\n" +
                           startDt + " - " + endDt + "\n");
              out.println("MICRO ID\t" + "DEC ID\t" + 
                            "LAST NAME\t" + "FIRST NAME\t" +
                            "SCHED DATE\t" + "T/S\t" + "R/C\t" + "CON\t" + "CEM\t");
                         
                   qry = "";
                   qry = "select id, " +
                         "monument_type_cd, " +
                         "replacement_reason_cd, " +
                         "repl_reason_cd " +
                         "from monument " +
                         "where managing_cemetery_num = ? " + 
                         "and order_release_dt between (to_date(?, 'MM/DD/YYYY')) and (to_date(?, 'MM/DD/YYYY') + 1) " +
                         "order by monument_type_cd";
                          
            try {
                    //to test qry:*/ Utility.displayErrMsg ( res, conn, pstmt, qry);
                pstmt = conn.prepareStatement(qry);
                pstmt.setString(1, magCem); 
                pstmt.setString(2, startDt); //if you want to add parameter in the query
                pstmt.setString(3, endDt);
                rs = pstmt.executeQuery();
               
                while (rs.next()) 
                {
                   monId = rs.getString(1);
                   monTypeCd = rs.getString(2);
                   replaceReaCd = rs.getString(3);
                   replReaCd = rs.getString(4);
                   if (replReaCd == null) 
                   {
                     repReaCd = replaceReaCd;
                   }
                   else 
                   {
                     repReaCd = replReaCd;
                   }
                   decMon = getDecMon(monId, conn, res);
                   decId = decMon[0];
                   graveInfo = getGrave(decId, conn, res);
                   decInfo = getDecName(decId, conn, res);
                   schDate = getSchDate(decId, conn, res);
                                  
                   out.println(Utility.blankIfNull(decMon[1]) + "\t" + //microfilm_id
                               Utility.blankIfNull(decMon[0]) + "\t" + //dec_id
                               Utility.blankIfNull(decInfo[1]) + "\t" + //last_nme
                               Utility.blankIfNull(decInfo[0]) + "\t" + //first_nme
                               Utility.blankIfNull(schDate) + "\t" + //scheduled_dt
                               Utility.blankIfNull(rs.getString(2)) + "\t" + //monument_type_cd
                               Utility.blankIfNull(repReaCd) + "\t" + //replacement_reason_cd
                               Utility.blankIfNull(graveInfo[1]) + "\t" + //container_type_cd
                               Utility.blankIfNull(graveInfo[0]) + "\t" //cem_num
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
   
   public static String[] getDecMon(String monId,
                                   Connection conn, HttpServletResponse res) throws IOException
                                   // add res as in catch exception has displayErrMsg(res,conn,psmt, ..) 
   { 
        String decId = "";
        String microId = "";
        String[] arrayString = new String[2];
        
        String qry = "";
        PreparedStatement pstmt = null;

            try {
                      qry = "select decedent_id,  " +
                            "microfilm_id " +
                            "from decedent_monument " +
                            "where monument_id = ? " +
                            "and inscr_death_dt = (select unique(max(inscr_death_dt)) " +
                                                    "from decedent_monument " +
                                                    "where monument_id = ? )";
                                    
                      pstmt = conn.prepareStatement(qry);
                      pstmt.setString(1, monId); 
                      pstmt.setString(2, monId);
                      ResultSet rs = pstmt.executeQuery();
     
                      if (rs.next()) 
                      {
                        decId = rs.getString(1);
                        microId = rs.getString(2);
                        arrayString[0] = decId;
                        arrayString[1] = microId;
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
                              "\n" +  qry );                //return;
               }

               return arrayString;
   }

public static String[] getGrave(String decId,
                                   Connection conn, HttpServletResponse res) throws IOException
                                   // add res as in catch exception has displayErrMsg(res,conn,psmt, ..) 
   { 
        String cemNum = "";
        String conTypeCd = "";
        String[] arrayString = new String[2];
        
        String qry = "";
        PreparedStatement pstmt = null;

            try {
                      qry = "select cemetery_num,  " +
                            "container_type_cd " +
                            "from gravesite_assignment " +
                            "where decedent_id = ? " +
                            "and insert_dt = (select max(insert_dt) " +
                                                    "from gravesite_assignment " +
                                                    "where decedent_id = ? )";
                                    
                      pstmt = conn.prepareStatement(qry);
                      pstmt.setString(1, decId); 
                      pstmt.setString(2, decId);
                      ResultSet rs = pstmt.executeQuery();
     
                      if (rs.next()) 
                      {
                        cemNum = rs.getString(1);
                        conTypeCd = rs.getString(2);
                        arrayString[0] = cemNum;
                        arrayString[1] = conTypeCd;
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
                              "\n" +  qry );                //return;
               }

               return arrayString;
   }

public static String[] getDecName(String decId,
                                   Connection conn, HttpServletResponse res) throws IOException
                                   // add res as in catch exception has displayErrMsg(res,conn,psmt, ..) 
   { 
        String firstNme = "";
        String lastNme = "";
        String[] arrayString = new String[2];
        
        String qry = "";
        PreparedStatement pstmt = null;

            try {
                      qry = "select first_nme,  " +
                            "last_nme " +
                            "from decedent_information " +
                            "where id = ? " ;
                                    
                      pstmt = conn.prepareStatement(qry);
                      pstmt.setString(1, decId); 
                      ResultSet rs = pstmt.executeQuery();
     
                      if (rs.next()) 
                      {
                        firstNme = rs.getString(1);
                        lastNme = rs.getString(2);
                        arrayString[0] = firstNme;
                        arrayString[1] = lastNme;
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
                              "\n" +  qry );                //return;
               }

               return arrayString;
   }

public static String getSchDate(String decId,
                                   Connection conn, HttpServletResponse res) throws IOException
                                   // add res as in catch exception has displayErrMsg(res,conn,psmt, ..) 
   { 
        String schDate = "";
                
        String qry = "";
        PreparedStatement pstmt = null;

            try {
                      qry = "select to_char(scheduled_dt, 'MM/DD/YYYY')  " +
                            "from intr_schedule " +
                            "where decedent_id = ? " +
                            "and insert_dt = (select min(insert_dt) " +
                                             "from intr_schedule " +
                                             "where decedent_id = ? ) ";
                                    
                      pstmt = conn.prepareStatement(qry);
                      pstmt.setString(1, decId); 
                      pstmt.setString(2, decId);
                      ResultSet rs = pstmt.executeQuery();
     
                      if (rs.next()) 
                      {
                        schDate = rs.getString(1);
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
                              "\n" +  qry );                //return;
               }

               return schDate;
   }


}