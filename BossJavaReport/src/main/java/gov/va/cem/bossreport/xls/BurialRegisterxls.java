/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.va.cem.bossreport.xls;

/*
 *
 * Aug 2011, CCR536 HD48187  Grave Registry Not Showing Cases Exist without Interment Dates -- Possible Flaw
what changed from C program burial_reg.pc
EXEC SQL declare ga_ptr cursor for
    select decedent_id, to_char (interment_dt, 'MM/DD/YYYY'),
    depth_num, burial_position_cd, container_type_cd from
    gravesite_assignment, decedent_information  <---
    where decedent_id = id and
    cemetery_num = :search_cem and
    section_id = :search_section and row_num = :row_num and
    site_num = :site_num and
    (interment_dt is not NULL or
     to_number (decedent_id) < 5000000 or
     case_status_cd = 'AU') and  //added
    disinterment_dt is NULL and cancel_postpone_ind is NULL;
   user_msg (NO, "declare ga_ptr");

 * 
 * to test
 * encrypted n911frm,develop for PC use
 * http://localhost:8888/bossreport/burregxls?u=c46a1ca72d8c332e8196a736fbb3d7164277a2b9a1525386
   to make it fail (wrong password for n911frm), try this:
      http://localhost:8888/bossreport/cemeterylisting?u=fb9fb36d58e31f91d5a3c92d2c4b9e1914dc1363e3aa5925
      SQL:
        SQL> select garencrypt('n911frm,test') from dual;
             GARENCRYPT('N911FRM,TEST')
             -------------------------------------------------
             fb9fb36d58e31f91d5a3c92d2c4b9e1914dc1363e3aa5925
 * 
 * */
import gov.va.cem.bossreport.util.Encryption;
import gov.va.cem.bossreport.util.Utility;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.PrintWriter;
import java.io.IOException;

import java.awt.Color;
import java.util.Vector;
import java.util.Iterator;

public class BurialRegisterxls extends HttpServlet 
{
  private static final String CONTENT_TYPE = "application/vnd.ms-excel"; // for MSWord: application/msword, "text/html; charset=windows-1252";
  
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
        String secId = req.getParameter("section_id");
                
        String cemNme = "";
        String DescrSecType = "";
        String SecTypeCd = "";
        String resvInd = "";
        String DescrGrvSizeCd = "";
        String siteNum = "";
        String rowNum = "";
        String posCd = "";
        String typeCd = "";
        String GrvSizeCd = "";
        String decNme = "";
        String interDt = "";
        String depthNum = "";
        String burPosCd = "";
        String ConTypeCd = "";
        String decId = "";
        String depPos = "";
        String[] gravesiteInfo = new String[7]; //new change 5 to 7
        Vector values = new Vector();
                              
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

      /*  ByteArrayOutputStream baosPDF = new ByteArrayOutputStream();

        Document doc = 
            new Document(PageSize.A4.rotate()); */ //rotate makes it landscape
            //new Document(PageSize.A4); makes it Letter standard size 8.5 x 11
            //new Document(PageSize.A4, 72, 72, 36, 36); 
            // 72 is 1 inch and 36 is the 1/2 inch
            //LR margin of 1/4 inch, Top=1/2, Bottom 1/4inch
            //new Document(PageSize.A4.rotate(), 72, 72, 36, 36); // or (PageSize.LETTER.rotate()); //rotate makes it landscape

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
              qry = "select section_type_cd " +
                    "from gravesite_information " +
                    "where cemetery_num = ? " +
                    "and section_id = ? " +
                    "and rownum = 1 " +
                    "order by to_number (translate (row_num, " +
                    "'0123456789 ABCDEFGHIJKLMNOPQRSTUVWXYZ,./<>?`~!@#$%^&*()-=_+[]\\{}|;:\"', "  +
                    "'0123456789')), " +
                    "nvl(translate (row_num, " +
                    "' ABCDEFGHIJKLMNOPQRSTUVWXYZ,./<>?`~!@#$%^&*()-=_+[]\\{}|;:\"0123456789', " +
                    "' ABCDEFGHIJKLMNOPQRSTUVWXYZ,./<>?`~!@#$%^&*()-=_+[]\\{}|;:\"'), ' '), " +
                    "to_number (translate (site_num, " +
                    "'0123456789 ABCDEFGHIJKLMNOPQRSTUVWXYZ,./<>?`~!@#$%^&*()-=_+[]\\{}|;:\"', " +
                    "'0123456789')), " +
                    "nvl (translate (site_num, " +
                    "' ABCDEFGHIJKLMNOPQRSTUVWXYZ,./<>?`~!@#$%^&*()-=_+[]\\{}|;:\"0123456789', " +
                    "' ABCDEFGHIJKLMNOPQRSTUVWXYZ,./<>?`~!@#$%^&*()-=_+[]\\{}|;:\"'), ' ') ";
                    
              pstmt = conn.prepareStatement(qry);
              pstmt.setString(1, cemNum);
              pstmt.setString(2, secId);
              rs = pstmt.executeQuery();  
              
              if(rs.next()) // for single return
              {
                  SecTypeCd = rs.getString(1);  
              }     
                                            
              rs.close();
              pstmt.close();      
              
              qry = "";
              qry = "select descr from grave_section_type where cd = ? ";
              
              pstmt = conn.prepareStatement(qry);
              pstmt.setString(1, SecTypeCd);
              rs = pstmt.executeQuery();
                                                         
              if(rs.next()) // for single return
              {
                DescrSecType = rs.getString(1);
              }
                                            
              rs.close();
              pstmt.close();
                                
              qry = "";
              qry = "select row_num, " +
                    "site_num, " +
                    "position_cd, " + 
                    "section_type_cd, " + 
                    "type_cd, " +
                    "grave_size_cd, " + 
                    "nvl(reservation_ind, 'N') " +
                    "from gravesite_information " +
                    "where cemetery_num = ? " + 
                    "and section_id = ? " +
                    "order by to_number (translate (row_num, " +
                    "'0123456789 ABCDEFGHIJKLMNOPQRSTUVWXYZ,./<>?`~!@#$%^&*()-=_+[]\\{}|;:\"', "  +
                    "'0123456789')), " +
                    "nvl(translate (row_num, " +
                    "' ABCDEFGHIJKLMNOPQRSTUVWXYZ,./<>?`~!@#$%^&*()-=_+[]\\{}|;:\"0123456789', " +
                    "' ABCDEFGHIJKLMNOPQRSTUVWXYZ,./<>?`~!@#$%^&*()-=_+[]\\{}|;:\"'), ' '), " +
                    "to_number (translate (site_num, " +
                    "'0123456789 ABCDEFGHIJKLMNOPQRSTUVWXYZ,./<>?`~!@#$%^&*()-=_+[]\\{}|;:\"', " +
                    "'0123456789')), " +
                    "nvl (translate (site_num, " +
                    "' ABCDEFGHIJKLMNOPQRSTUVWXYZ,./<>?`~!@#$%^&*()-=_+[]\\{}|;:\"0123456789', " +
                    "' ABCDEFGHIJKLMNOPQRSTUVWXYZ,./<>?`~!@#$%^&*()-=_+[]\\{}|;:\"'), ' ') ";
                          
            
                    //to test qry:*/ Utility.displayErrMsg ( res, conn, pstmt, qry);
                pstmt = conn.prepareStatement(qry);
                pstmt.setString(1, cemNum); 
                pstmt.setString(2, secId); //if you want to add parameter in the query
                rs = pstmt.executeQuery();

                //instruct the browser to send output as Excel
                res.setContentType(CONTENT_TYPE);
                res.setHeader("Content-Disposition","attachment; filename=BurialRegister_cem" + cemNum  + ".xls");   // filename
                ServletOutputStream out= res.getOutputStream();

                //header in Excel
                out.println ("Burial Register Report");
                out.println ("Grave Section Type: " + DescrSecType + "    Section : " + secId);
                out.println (cemNum + " " + cemNme + "\n");
                out.println("GRAVE\t" + "ROW\t" + 
                            "NAME\t" + "INTER-DATE\t" +
                            "REL\t" + "DEP POS\t" + "CON\t" + 
                            "OB\t" + "SEC TYP\t" + "GR TYP\t" +
                            "SIZE"
                           );
                           
                while (rs.next()) 
                {
                  rowNum = rs.getString(1);  
                  siteNum = rs.getString(2);
                  posCd = rs.getString(3);
                  SecTypeCd = rs.getString(4);
                  typeCd = rs.getString(5);
                  GrvSizeCd = rs.getString(6);
                  resvInd = rs.getString(7);
                  String[] stringArray;
                                    
                  DescrGrvSizeCd = getDescrGraveSize(GrvSizeCd, conn, res);
                                    
                  if (resvInd.equals("Y"))
                  {                    
                    decNme = getReservInfo(cemNum, secId, rowNum, siteNum, conn, res);
                    interDt = "RESERVED";
                    
                    out.println(Utility.blankIfNull(rs.getString(2)) + "\t" + //site_num
                                Utility.blankIfNull(rs.getString(1)) + "\t" + //row_num
                                Utility.blankIfNull(decNme) + "\t" + //last_nme,first_nme, middle_nme
                                Utility.blankIfNull("RESERVED") + "\t" + //interment_dt
                                "\t" + "\t" + "\t" + "\t" + //Rel,Dep Pos,Con,Ob
                                Utility.blankIfNull(rs.getString(4)) + "\t" + //section_type_cd
                                Utility.blankIfNull(rs.getString(5)) + "\t" + //type_cd
                                Utility.blankIfNull(DescrGrvSizeCd)  //descr_grave_size_cd
                               );
                  }
                  else
                  {                    
                    values = getGravesiteInfo(cemNum, secId, rowNum, siteNum, conn, res);
                    if (values.isEmpty()) //new
                    {
                      String[] arrayString = new String[7];
                      for (int i = 0; i < arrayString.length; i++)
                          arrayString[i] = "";
                      values.add(arrayString);
                    } //end new
                    Iterator iter = values.iterator();
                    while(iter.hasNext()) 
                    {   
                      stringArray = (String[])iter.next();
                      depPos = Utility.blankIfNull(stringArray[2]) + Utility.blankIfNull(stringArray[3]);
                      out.println(Utility.blankIfNull(rs.getString(2)) + "\t" + //site_num
                                  Utility.blankIfNull(rs.getString(1)) + "\t" + //row_num
                                  Utility.blankIfNull(stringArray[5]) + "\t" + //decedent_name
                                  Utility.blankIfNull(stringArray[1]) + "\t" + //interment_dt
                                  Utility.blankIfNull(stringArray[6]) + "\t" + //relationship_cd
                                  Utility.blankIfNull(depPos) + "\t" + //depth_num, burial_position_cd 
                                  Utility.blankIfNull(stringArray[4]) + "\t" + //ontainer_type_cd
                                  Utility.blankIfNull(rs.getString(3)) + "\t" + //position_cd
                                  Utility.blankIfNull(rs.getString(4)) + "\t" + //section_type_cd
                                  Utility.blankIfNull(rs.getString(5)) + "\t" + //type_cd
                                  Utility.blankIfNull(DescrGrvSizeCd) //descr_grave_size_cd
                                 );
                                   
                    } //end of wile loop for iter.hasNext 
                  } //end of else loop */
                  
                } //end while
           
                rs.close();
                pstmt.close();
                conn.close();
                rs = null;
                pstmt = null;
                conn = null;


            } catch (SQLException e) {
                System.err.println(new java.util.Date() + 
                                   " Error retrieving data " + "\n" + qry );
                Utility.displayErrMsg(res, conn, pstmt, 
                              "Error retrieving data" + 
                              "\n" + e.toString() + "\n" + qry);
                
                return;
            }
       
  }
  
   public static String getDescrGraveSize(String GrvSizeCd,
                                   Connection conn, HttpServletResponse res) throws IOException
   {                               
       String DescrGrvSize = "";
       String qry = "";
       PreparedStatement pstmt = null;

       try {
              qry = "select descr from gravesite_size where cd = ? ";
              pstmt = conn.prepareStatement(qry);
              pstmt.setString(1, GrvSizeCd); 
              ResultSet rs = pstmt.executeQuery();
              
              if (rs.next()) // for single return
              {
                DescrGrvSize = rs.getString(1);
              }
             
              rs.close();
              pstmt.close();   
              
              rs = null;
              pstmt = null;
          }catch (SQLException e) {
                   System.err.println(new java.util.Date() + 
                                   " Error retrieving data " + "\n" + qry );
                   Utility.displayErrMsg(res, conn, pstmt,
                              " Exception " + "\n" + qry );
          }
      return DescrGrvSize;    
   }   
   
                                        
   public static String getReservInfo(String cemNum, String secId, String rowNum, String siteNum,
                                   Connection conn, HttpServletResponse res) throws IOException
                                   // add res as in catch exception has displayErrMsg(res,conn,psmt, ..) 
   { 
        String lastNme = "";
        String firstNme = "";
        String middleNme = "";
        String reservInfo = "";
        
        String qry = "";
        PreparedStatement pstmt = null;

            try {
                      qry = "select first_nme, " + 
                            "substr(middle_nme, 1, 1), " +
                            "last_nme " + 
                            "from gravesite_reservation " +
                            "where cemetery_num = ? " +
                            "and section_id = ? " +
                            "and row_num = ? " +
                            "and site_num = ? " +
                            "and reservation_status_cd <> 'C'";
                                    
                      pstmt = conn.prepareStatement(qry);
                      pstmt.setString(1, cemNum); 
                      pstmt.setString(2, secId);
                      pstmt.setString(3, rowNum);
                      pstmt.setString(4, siteNum);
                      ResultSet rs = pstmt.executeQuery();
     
                      if (rs.next()) 
                      {
                        firstNme = rs.getString(1);
                        middleNme = rs.getString(2);
                        lastNme = rs.getString(3);
                        if (middleNme == null)
                        {
                          reservInfo = lastNme + ", " + firstNme;
                        }
                        else
                        {
                          reservInfo = lastNme + ", " + firstNme + " " + middleNme + ". ";
                        }
                      } 

                      rs.close();
                      pstmt.close();
                      rs = null;
                      pstmt = null;

                } catch (SQLException e) {
                    System.err.println(new java.util.Date() + 
                                       " Error retrieving subdecedebt " + "\n" + qry );
                    Utility.displayErrMsg(res, conn, pstmt,
                              " Exception " +  "\n");
               }

               return reservInfo;
   }

public static Vector getGravesiteInfo(String cemNum, String secId, String rowNum, String siteNum,
                                   Connection conn, HttpServletResponse res) throws IOException
                                   // add res as in catch exception has displayErrMsg(res,conn,psmt, ..) 
   { 
        String decId = "";
        String interDt = "";
        String depthNum = "";
        String burPosCd = "";
        String conTypeCd = "";
        
        String[] decInfo = new String[2]; //new
        String[] arrayString = new String[7];
        Vector list = new Vector(); //new 
        
        String qry = "";
        PreparedStatement pstmt = null;
        
            try {
                      qry = "select decedent_id, " +  
                            "to_char(interment_dt, 'MM/DD/YYYY'), " +
                            "depth_num, " + 
                            "burial_position_cd, " +
                            "container_type_cd " + 
//added decedent_information, CCR536
                            "from gravesite_assignment, decedent_information " +
                            "where decedent_id = id and " + 
                            "cemetery_num = ? " +
                            "and section_id = ? " +
                            "and row_num = ? " +
                            "and site_num = ? " +
//change the commented line below to next three lines, CCR536
                            "and (interment_dt is not NULL or " + 
                            " to_number (decedent_id) < 5000000 or " + 
                            " case_status_cd = 'AU') " + 
                            //"and (interment_dt is not NULL or to_number(decedent_id) < 5000000) " + 
                            "and disinterment_dt is NULL " +
                            "and cancel_postpone_ind is NULL";

                                    
                      pstmt = conn.prepareStatement(qry);
                      pstmt.setString(1, cemNum); 
                      pstmt.setString(2, secId);
                      pstmt.setString(3, rowNum);
                      pstmt.setString(4, siteNum);
                      ResultSet rs = pstmt.executeQuery();
                                            
                      while (rs.next()) 
                      {
                        arrayString = new String[7];
                        decId = rs.getString(1);
                        interDt = rs.getString(2);
                        depthNum = rs.getString(3);
                        burPosCd = rs.getString(4);
                        conTypeCd = rs.getString(5);
                        arrayString[0] = decId;
                        arrayString[1] = interDt;
                        arrayString[2] = depthNum;
                        arrayString[3] = burPosCd;
                        arrayString[4] = conTypeCd;
                        decInfo = getDecInfo(decId, conn, res); //new
                        arrayString[5] = decInfo[0]; //new decedent name
                        arrayString[6] = decInfo[1]; //new relationship_cd
                        list.add(arrayString);
                                                
                      }
                      rs.close();
                      pstmt.close();
                      rs = null;
                      pstmt = null;

                } catch (SQLException e) {
                    System.err.println(new java.util.Date() + 
                                       " Error retrieving subdecedebt " + "\n" + qry );
                    Utility.displayErrMsg(res, conn, pstmt,
                              " Exception " +  "\n");
               }

               return list;
   }
public static String[] getDecInfo(String decId, Connection conn, HttpServletResponse res) throws IOException
                                   // add res as in catch exception has displayErrMsg(res,conn,psmt, ..) 
   { 
        String firstNme = "";
        String middleNme = "";
        String lastNme = "";
        String suffixNme = "";
        String relshipCd = "";
        String[] arrayString = new String[2];
        
        String qry = "";
        PreparedStatement pstmt = null;
        
            try {
                      qry = "select first_nme, " + 
                            "decode(middle_nme, null, '', substr(middle_nme, 1, 1)||'. '),  " +
                            "last_nme||', ',  " + 
                            "decode(suffix_nme, null, '', suffix_nme), " +
                            "decode(relationship_cd, 'V', 'V', 'D') " +
                            "from decedent_information " +
                            "where id = to_char(?) " +
                            "and case_status_cd in ('CE', 'EL', 'NV', 'NR', 'AU') " ;
                                    
                      pstmt = conn.prepareStatement(qry);
                      pstmt.setString(1, decId); 
                      ResultSet rs = pstmt.executeQuery();
     
                      if (rs.next()) //new change to if
                      {
                        firstNme = rs.getString(1);
                        middleNme = rs.getString(2);
                        lastNme = rs.getString(3);
                        suffixNme = rs.getString(4);
                        relshipCd = rs.getString(5);
                        arrayString[1] = relshipCd;
                        arrayString[0] = lastNme + firstNme + " " + Utility.blankIfNull(middleNme)  + Utility.blankIfNull(suffixNme);
                        
                        //if (middleNme.equals("")) // this fails if middleNme is null
                        //if (middleNme == null )
                     
                      } 

                      rs.close();
                      pstmt.close();
                      rs = null;
                      pstmt = null;

                } catch (SQLException e) {
                    System.err.println(new java.util.Date() + 
                                       " Error retrieving subdecedebt " + "\n" + qry );
                    Utility.displayErrMsg(res, conn, pstmt,
                              " Exception " +  "\n");
               }

               return arrayString;
   }

}