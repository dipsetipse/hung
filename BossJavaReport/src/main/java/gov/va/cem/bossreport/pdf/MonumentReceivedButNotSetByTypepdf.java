/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.va.cem.bossreport.pdf;

/* to test
 * encrypted n911frm,develop for PC use
 * http://localhost:8888/bossreport/monrecnotsettype?u=c46a1ca72d8c332e8196a736fbb3d7164277a2b9a1525386
   to make it fail (wrong password for n911frm), try this:
      http://localhost:8888/bossreport/monrecnotsettype?u=fb9fb36d58e31f91d5a3c92d2c4b9e1914dc1363e3aa5925
      SQL:
        SQL> select garencrypt('n911frm,test') from dual;
             GARENCRYPT('N911FRM,TEST')
             -------------------------------------------------
             fb9fb36d58e31f91d5a3c92d2c4b9e1914dc1363e3aa5925
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

public class MonumentReceivedButNotSetByTypepdf extends HttpServlet 
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

        String monuType = req.getParameter("type");
        
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

        ByteArrayOutputStream baosPDF = new ByteArrayOutputStream();

        Document doc = new Document(PageSize.LETTER.rotate()); //rotate makes it landscape
            //new Document(PageSize.A4.rotate()); //rotate makes it landscape
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
            doc.addTitle("Monuments Received But Not Set By Type");  //change this to your report name
            doc.addKeywords("pdf, itext, Java, open source, http, worksheet, cemetery report");

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
                            
              rs = null;
              pstmt = null; 
            }catch (SQLException e) {
                System.err.println(new java.util.Date() + 
                                   " Error retrieving data " + "\n" + qry );
            }
            
            HeaderFooter header = new HeaderFooter(new Phrase("MONUMENTS RECEIVED BUT NOT SET BY TYPE\n" +
                                                   cemNum + " " + cemNme + "\n",
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
            float[] arrayColWid = new float[14];
            arrayColWid[0] = 11; //inter_dt
            arrayColWid[1] = 8;  //dec_id
            arrayColWid[2] = 10; //first_nme
            arrayColWid[3] = 16; //last_nme
            arrayColWid[4] = 3; //monument_type_cd
            arrayColWid[5] = 3; //cem_num
            arrayColWid[6] = 5; //section_id
            arrayColWid[7] = 3; //row_num
            arrayColWid[8] = 8; //site_num
            arrayColWid[9] = 2; //shipment_fy
            arrayColWid[10] = 3; //ship_prefix
            arrayColWid[11] = 6; //cbl_num
            arrayColWid[12] = 11; //order_dt
            arrayColWid[13] = 11; //received_dt

            PdfPTable tbl = new PdfPTable(arrayColWid); //num of cols
            tbl.setWidthPercentage(100);
            tbl.getDefaultCell().setPadding(2); //pad between border and first char in column

            PdfPCell empty = new PdfPCell();
            PdfPCell cell = new PdfPCell(empty);

            cell = new PdfPCell(new Phrase("INTER DT", boldFont));
            cell.setHorizontalAlignment(1);
            cell.setPaddingTop(2);
            cell.setPaddingBottom(6);
            tbl.addCell(cell);

            cell = new PdfPCell(new Phrase("DEC ID", boldFont));
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            tbl.addCell(cell);

            cell = new PdfPCell(new Phrase("FIRST NAME", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);

            cell = new PdfPCell(new Phrase("LAST NAME", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);

            cell = new PdfPCell(new Phrase("TC", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
            
            cell = new PdfPCell(new Phrase("GRAVESITE", boldFont));
            cell.setColspan(4);
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
                                    
            cell = new PdfPCell(new Phrase("SHIPMENT NUM", boldFont));
            cell.setColspan(3);
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
                                    
            cell = new PdfPCell(new Phrase("ORDER DT", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
            
            cell = new PdfPCell(new Phrase("RECEIVED DT", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
            
            tbl.setHeaderRows(1); //end the header that will repeat on every page
           
       
                   qry = "";
                   qry = "select id, " +
                         "monument_type_cd, " +
                         "to_char(amas_order_dt, 'mm/dd/yyyy'), " + 
                         "to_char(received_dt, 'mm/dd/yyyy'), " + 
                         "shipment_fy, " +
                         "ship_prefix, " + 
                         "cbl_num " +
                         "from monument " +
                         "where managing_cemetery_num = ? " + 
                         "and monument_type_cd = ? " +
                         "and amas_order_dt is not null " +
                         "and received_dt is not null " +
                         "and set_dt is null " +
                         "and ship_prefix is not null " +
                         "order by monument_type_cd, received_dt";
                          
            try {
                    //to test qry:*/ Utility.displayErrMsg ( res, conn, pstmt, qry);
                pstmt = conn.prepareStatement(qry);
                pstmt.setString(1, cemNum); 
                pstmt.setString(2, monuType); //if you want to add parameter in the query
                rs = pstmt.executeQuery();

                boolean grey = true;
                                
                int rowCount = 0;
                
                while (rs.next()) 
                {
                   rowCount ++;
                                     
                   micrfId = rs.getString(1);
                   decInfo = getDecInfo(micrfId, conn, res);

                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(decInfo[0]), font));//interment_dt
                   cell.setHorizontalAlignment(1);
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   cell.setPaddingTop(2);
                   cell.setPaddingBottom(6);
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(decInfo[1]), font));//dec_id
                   cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(decInfo[2]), font));//first_nme
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(decInfo[3]), font));//last_nme
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(2)), font)); //monument_type_cd
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(decInfo[4]), font)); //cem_num
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(decInfo[5]), font));//section_id
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(decInfo[6]), font));//row_num
                   cell.setHorizontalAlignment(1);
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(decInfo[7]), font));//site_num
                   cell.setHorizontalAlignment(1);
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   //cell.setPaddingTop(2);
                   //cell.setPaddingBottom(6);
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(5)), font));//shipment_fy
                   cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(6)), font));//ship_prefix
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(7)), font));//cbl_num
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(3)), font));//order_dt
                   cell.setHorizontalAlignment(1);
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(4)), font));//received_dt
                   cell.setHorizontalAlignment(1);
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                                              
                   if (grey) {
                      grey=false;
                   } else {
                      grey=true;
                   }

                } //end while
                              
                
                if ( rowCount == 0 ) {
                       cell = new PdfPCell(new Phrase("No Records Found ", font));//received_dt
                       cell.setColspan(14);
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
                                                    "where monument_id = ? )" +
                            "  and rownum = 1 ";
                                    
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
