/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.va.cem.bossreport.pdf;

/* to test
 * encrypted n911frm,develop for PC use
 * http://localhost:8888/bossreport/iabydatepdf?u=c46a1ca72d8c332e8196a736fbb3d7164277a2b9a1525386
   to make it fail (wrong password for n911frm), try this:
      http://localhost:8888/bossreport/cemeterylisting?u=fb9fb36d58e31f91d5a3c92d2c4b9e1914dc1363e3aa5925
      SQL:
        SQL> select garencrypt('n911frm,test') from dual;
             GARENCRYPT('N911FRM,TEST')
             -------------------------------------------------
             fb9fb36d58e31f91d5a3c92d2c4b9e1914dc1363e3aa5925
 * 
 * Report: Current Inscription Approved Report (Print_Inscription program unit in REPORT.fmb) - last 2 days is "current", based on daily cron
 *       : Inscription Approved by Date Report (Print_Montrans    program unit in REPORT.fmb) - C prog is montrans_rpt.pc
 * 
 *    daily/montrans cron: 
 *       auto_montrans.pc runs a loop against the cemetery tables and builds a unix command to run the montrans_rpt in the NCSAPPS dir.
 *       After that it checks for a Milan indicator and either does or does not use the direct_print script.
 *       Finding the start date starts at yesterday and keeps subtracting 1 as long as the date in question is a Saturday, Sunday or in the Holiday table.
 * 
 * 
 * */

import gov.va.cem.bossreport.util.Encryption;
import gov.va.cem.bossreport.util.Utility;
 
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.IOException;

public class InscriptionApprovedByDatepdf extends HttpServlet 
{
  private static final String CONTENT_TYPE = "text/html; charset=windows-1252";

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

        ByteArrayOutputStream baosPDF = new ByteArrayOutputStream();

        Document doc = 
            new Document(PageSize.LETTER.rotate()); //rotate makes it landscape
            //new Document(PageSize.A4); makes it Letter standard size 8.5 x 11
            //new Document(PageSize.A4, 72, 72, 36, 36); 
            // 72 is 1 inch and 36 is the 1/2 inch
            //LR margin of 1/4 inch, Top=1/2, Bottom 1/4inch
            //new Document(PageSize.A4.rotate(), 72, 72, 36, 36); // or (PageSize.LETTER.rotate()); //rotate makes it landscape

        try { //1 PDF

            PdfWriter pdfw = PdfWriter.getInstance(doc, baosPDF);
            Font font = new Font(Font.HELVETICA, 8);
            //or you can declare: Font boldFont = new Font(Font.HELVETICA, 8, Font.BOLD); or
            Font boldFont = 
                new Font(FontFactory.getFont(FontFactory.HELVETICA, 8, 
                                             Font.BOLD)); //new Color(255, 0, 0)));
            
            doc.addAuthor(this.getClass().getName());
            doc.addCreationDate();
            doc.addProducer();
            doc.addCreator(this.getClass().getName());
            doc.addTitle("Inscription Approved Report");  //change this to your report name
            doc.addKeywords("pdf, itext, Java, open source, http, worksheet, cemetery report");

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
              
              //qry = "";
              //qry = "select managing_station_num " +
              //      "from cemetery where num = '" + cemNum + "'";
                            
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
            
            HeaderFooter header = new HeaderFooter(new Phrase("INSCRIPTION APPROVED REPORT\n" +
                                                   cemNum + " " + cemNme + "\n" +
                                                   startDt + " - " + endDt + "\n",
                                                   new Font(Font.HELVETICA, 11, Font.BOLD)), false);
                         
            header.setAlignment(1);
            header.setBorder(Rectangle.NO_BORDER);
            doc.setHeader(header); 
                        
            HeaderFooter footer = new HeaderFooter(new Phrase(new java.util.Date() + 
                                                ",  Page ", font), true);

            footer.setBorder(Rectangle.NO_BORDER);
            doc.setFooter(footer);

            doc.open();

            //determine number of columns in your report and set it here
        /*
            float[] arrayColWid = new float[9];
            arrayColWid[0] = 11; //inter_dt
            arrayColWid[1] = 9;  //dec_id
            arrayColWid[2] = 10; //first_nme
            arrayColWid[3] = 10; //last_nme
            arrayColWid[4] = 3;  //monument_type_cd
            arrayColWid[5] = 22;  //gravesite
            arrayColWid[6] = 13;  //shipment_num
            arrayColWid[7] = 11; //release_dt
            arrayColWid[8] = 11;  //order_dt 
            
       */
            float[] arrayColWid = new float[9];
            arrayColWid[0] = 14; //microfilm_id
            arrayColWid[1] = 12;  //dec_id
            arrayColWid[2] = 15; //first_nme
            arrayColWid[3] = 25; //last_nme
            arrayColWid[4] = 14; //scheduled_dt
            arrayColWid[5] = 5; //monument_type_cd
            arrayColWid[6] = 5; //repl_reason_cd
            arrayColWid[7] = 5; //container_type_cd
            arrayColWid[8] = 5; //cem_num
            
            PdfPTable tbl = new PdfPTable(arrayColWid); //num of cols
            tbl.setWidthPercentage(100);
            tbl.getDefaultCell().setPadding(2); //pad between border and first char in column

            PdfPCell empty = new PdfPCell();
            PdfPCell cell = new PdfPCell(empty);

            cell = new PdfPCell(new Phrase("MICRO ID", boldFont));
            cell.setHorizontalAlignment(1);
            cell.setPaddingTop(2);
            cell.setPaddingBottom(6);
            tbl.addCell(cell);

            cell = new PdfPCell(new Phrase("DEC ID", boldFont));
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            tbl.addCell(cell);

            cell = new PdfPCell(new Phrase("LAST NAME", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);

            cell = new PdfPCell(new Phrase("FIRST NAME", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);

            cell = new PdfPCell(new Phrase("SCHED DATE", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
            
            cell = new PdfPCell(new Phrase("T/S", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
                                    
            cell = new PdfPCell(new Phrase("R/C", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
                                    
            cell = new PdfPCell(new Phrase("CON", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
            
            cell = new PdfPCell(new Phrase("CEM", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
            
            tbl.setHeaderRows(1); //end the header that will repeat on every page
            
        /*  String qry = "select num, nme, MANAGING_STATION_NUM, CEMETERY_TYPE, STATE_CD " +
                  " from cemetery order by num";
        */
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

                boolean grey = true;
                
                int rowCount = 0;
                
                while (rs.next()) 
                {
                   rowCount ++;
                   
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

                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(decMon[1]), font));//microfilm_id
                   cell.setHorizontalAlignment(1);
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   cell.setPaddingTop(2);
                   cell.setPaddingBottom(6);
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(decMon[0]), font));//dec_id
                   cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(decInfo[1]), font));//last_nme
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(decInfo[0]), font));//first_nme
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(schDate), font)); //scheduled_dt
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(2)), font)); //monument_type_cd
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(repReaCd), font));//replacement_reason_cd
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(graveInfo[1]), font));//container_type_cd
                   cell.setHorizontalAlignment(1);
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(graveInfo[0]), font));//cem_num
                   cell.setHorizontalAlignment(1);
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   //cell.setPaddingTop(2);
                   //cell.setPaddingBottom(6);
                   tbl.addCell(cell);
                                                               
                   if (grey) {grey=false;} else {grey=true;}

                } //end while
                
                if ( rowCount == 0 ) {
                     cell = new PdfPCell(new Phrase("No Records Found ", font));//received_dt
                     cell.setColspan(9);
                     tbl.addCell(cell);
                } 

                doc.add(tbl);

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

            doc.close();
            pdfw.close();

            StringBuffer sbFilename = new StringBuffer();
            sbFilename.append("filename_");
            sbFilename.append(System.currentTimeMillis());
            sbFilename.append(".pdf");

            res.setHeader("Cache-Control", "max-age=30");
            res.setContentType("application/pdf");

            StringBuffer sbContentDispValue = new StringBuffer();
            sbContentDispValue.append("inline");
            sbContentDispValue.append("; filename=");
            sbContentDispValue.append(sbFilename);

            res.setHeader("Content-disposition", 
                          sbContentDispValue.toString());

            res.setContentLength(baosPDF.size());

            ServletOutputStream sos;

            sos = res.getOutputStream();
            baosPDF.writeTo(sos);
            sos.flush();

            //System.err.println("sos Flush... ");

            baosPDF.reset();



        } catch (DocumentException dex) { //1 PDF

            System.err.println("PDF Error " + dex.toString());
            res.setContentType("text/html");
            PrintWriter writer = res.getWriter();
            writer.println(this.getClass().getName() + 
                           " caught an exception: " + 
                           dex.getClass().getName() + "<br>");
            writer.println("<pre>");
            dex.printStackTrace(writer);
            writer.println("</pre>");

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