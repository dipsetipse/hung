/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.va.cem.bossreport.pdf;

/* to test
 * encrypted n911frm,develop for PC use
 * http://localhost:8888/bossreport/funeralhomecontactpdf?u=c46a1ca72d8c332e8196a736fbb3d7164277a2b9a1525386
   to make it fail (wrong password for n911frm), try this:
      http://localhost:8888/bossreport/funeralhomecontactpdf?u=fb9fb36d58e31f91d5a3c92d2c4b9e1914dc1363e3aa5925
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

public class FuneralHomeContactpdf extends HttpServlet 
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
        
        String cemNUM = "";
        String cemNME = "";
        String micrfID = "";
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
            doc.addTitle("Monuments Ordered But Not Received");  //change this to your report name
            doc.addKeywords("pdf, itext, Java, open source, http, worksheet, cemetery report");

            String qry = "select num, nme from cemetery where num = substr(?, 2, 3)";
            
            try
            {
              pstmt = conn.prepareStatement(qry);
              pstmt.setString(1, userID);
              rs = pstmt.executeQuery();
                                                         
              if(rs.next()) // for single return
              {
                cemNUM = rs.getString(1);
                cemNME = rs.getString(2);
              }
            
              rs.close();
              pstmt.close();
              
              rs = null;
              pstmt = null; 
            }catch (SQLException e) {
                System.err.println(new java.util.Date() + 
                                   " Error retrieving data " + "\n" + qry );
            }
            
            HeaderFooter header = new HeaderFooter(new Phrase("FUNERAL HOME CONTACTS\n" +
                                                   cemNUM + " " + cemNME + "\n",
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
        
            float[] arrayColWid = new float[11];
            arrayColWid[0] = 5; //fh id
            arrayColWid[1] = 20;  //fh name
            arrayColWid[2] = 20; //address
            arrayColWid[3] = 8; //city
            arrayColWid[4] = 3; //state
            arrayColWid[5] = 5; //zip code
            arrayColWid[6] = 9; //phone
            arrayColWid[7] = 8; //fax
            arrayColWid[8] = 8; //first name
            arrayColWid[9] = 10; //last name
            arrayColWid[10] = 16; //email

            PdfPTable tbl = new PdfPTable(arrayColWid); //num of cols
            tbl.setWidthPercentage(100);
            tbl.getDefaultCell().setPadding(2); //pad between border and first char in column

            PdfPCell empty = new PdfPCell();
            PdfPCell cell = new PdfPCell(empty);

            cell = new PdfPCell(new Phrase("FH ID", boldFont));
            cell.setHorizontalAlignment(1);
            cell.setPaddingTop(2);
            cell.setPaddingBottom(6);
            tbl.addCell(cell);

            cell = new PdfPCell(new Phrase("FH NAME", boldFont));
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            tbl.addCell(cell);

            cell = new PdfPCell(new Phrase("ADDRESS", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);

            cell = new PdfPCell(new Phrase("CITY", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);

            cell = new PdfPCell(new Phrase("STATE", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
            
            cell = new PdfPCell(new Phrase("ZIP CODE", boldFont));
            ///cell.setColspan(4);
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
                                    
            cell = new PdfPCell(new Phrase("PHONE", boldFont));
            ///cell.setColspan(3);
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
                                    
            cell = new PdfPCell(new Phrase("FAX", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
            
            cell = new PdfPCell(new Phrase("FIRST NAME", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
            
            cell = new PdfPCell(new Phrase("LAST NAME", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
            
            cell = new PdfPCell(new Phrase("EMAIL", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
            
            tbl.setHeaderRows(1); //end the header that will repeat on every page
            
                   qry = "";
                   qry =  "select fh.id, " +
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
                          
            try {
                    //to test qry:*/ Utility.displayErrMsg ( res, conn, pstmt, qry);
                pstmt = conn.prepareStatement(qry);
                pstmt.setString(1, cemNUM); 
                rs = pstmt.executeQuery();

                boolean grey = true;
                int rowCount = 0;
                
                while (rs.next()) 
                {
                   rowCount ++;
                   
                   micrfID = rs.getString(1);
                   ///decInfo = getDecInfo(micrfID, conn, res);

                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(1)), font)); //monument_type_cd
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   cell.setPaddingTop(2);
                   cell.setPaddingBottom(6);
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(2)), font)); //monument_type_cd
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(3)), font));//shipment_fy
                   cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(4)), font));//ship_prefix
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(5)), font));//cbl_num
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(6)), font));//release_dt
                   cell.setHorizontalAlignment(1);
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(7)), font));//order_dt
                   cell.setHorizontalAlignment(1);
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(8)), font));//order_dt
                   cell.setHorizontalAlignment(1);
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(9)), font));//order_dt
                   cell.setHorizontalAlignment(1);
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(10)), font));//order_dt
                   cell.setHorizontalAlignment(1);
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                   
                   cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(11)), font));//order_dt
                   cell.setHorizontalAlignment(1);
                   if (grey) {
                      cell.setBackgroundColor(Color.LIGHT_GRAY);
                   }
                   tbl.addCell(cell);
                                              
                   if (grey) {grey=false;} else {grey=true;}

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

}
