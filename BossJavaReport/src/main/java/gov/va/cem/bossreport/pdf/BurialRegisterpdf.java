package gov.va.cem.bossreport.pdf;
/* to test
 * encrypted n911frm,develop for PC use
 * http://localhost:8888/bossreport/cemeterylisting?u=c46a1ca72d8c332e8196a736fbb3d7164277a2b9a1525386
   to make it fail (wrong password for n911frm), try this:
      http://localhost:8888/bossreport/cemeterylisting?u=fb9fb36d58e31f91d5a3c92d2c4b9e1914dc1363e3aa5925
      SQL:
        SQL> select garencrypt('n911frm,test') from dual;
             GARENCRYPT('N911FRM,TEST')
             -------------------------------------------------
             fb9fb36d58e31f91d5a3c92d2c4b9e1914dc1363e3aa5925
 * 
 * */
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

* to test
 * encrypted n911frm,develop for PC use
 * http://localhost:8888/bossreport/burregpdf?u=c46a1ca72d8c332e8196a736fbb3d7164277a2b9a1525386
   to make it fail (wrong password for n911frm), try this:
      http://localhost:8888/bossreport/cemeterylisting?u=fb9fb36d58e31f91d5a3c92d2c4b9e1914dc1363e3aa5925
      SQL:
        SQL> select garencrypt('n911frm,test') from dual;
             GARENCRYPT('N911FRM,TEST')
             -------------------------------------------------
             fb9fb36d58e31f91d5a3c92d2c4b9e1914dc1363e3aa5925
 * 
 * */
// you would need to add under Dependencies itext-2.1.3.jar
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
import java.util.Iterator;
import java.util.Vector;

//import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
//import javax.naming.InitialContext;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.IOException;

public class BurialRegisterpdf extends HttpServlet 
{
  private static final String CONTENT_TYPE = "text/html; charset=windows-1252";
  private static final int STRING_ARRAY_SIZE = 7;
  //private DataSource ods = null;

  public void init(ServletConfig config) throws ServletException
  {
    super.init(config);

    /* using Data Source Name
    String DATASOURCE = "jdbc/OracleDS";

    try {
        InitialContext ic = new InitialContext();
        ods = (DataSource)ic.lookup(DATASOURCE);
    } catch (javax.naming.NamingException e) {
        System.out.println("Production Datasource error - javax.naming.NamingException " + 
                           e.toString());
    }*/
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
            doc.addTitle("Burial Register");  //change this to your report name
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
              /*
              Utility.displayErrMsg(res, conn, pstmt,
                              " Exception " + 
                              "\n" +  qry + " cemNum = " + cemNum + " cemNme = " + cemNme); */
                              
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
              
              rs = null;
              pstmt = null; 
            }catch (SQLException e) {
                System.err.println(new java.util.Date() + 
                                   " Error retrieving data " + "\n" + qry );
               }
            
            HeaderFooter header = new HeaderFooter(new Phrase("BURIAL REGISTER\n" +
                                                   DescrSecType + " " + " SECTION: " + secId + "\n" +
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
           
            float[] arrayColWid = new float[11];
            arrayColWid[0] = 8; //site_num
            arrayColWid[1] = 3;  //row_num
            arrayColWid[2] = 55; //last_nme, first_nme, middle_nme, suffix_nme
            arrayColWid[3] = 10; //interment_dt
            arrayColWid[4] = 3; //relationship_cd
            arrayColWid[5] = 3; //depth_num, burial_position_cd
            arrayColWid[6] = 3; //container_type_cd
            arrayColWid[7] = 3; //position_cd
            arrayColWid[8] = 3; //section_type_cd
            arrayColWid[9] = 3; //type_cd
            arrayColWid[10] = 6; //descr_grave_size_cd

            PdfPTable tbl = new PdfPTable(arrayColWid); //num of cols
            tbl.setWidthPercentage(100);
            tbl.getDefaultCell().setPadding(2); //pad between border and first char in column

            PdfPCell empty = new PdfPCell();
            PdfPCell cell = new PdfPCell(empty);

            cell = new PdfPCell(new Phrase("", boldFont));
            cell.setColspan(5);
            tbl.addCell(cell);
            
            cell = new PdfPCell(new Phrase("Dep", boldFont));
            cell.setBorderWidthBottom(0);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            tbl.addCell(cell);
                       
            cell = new PdfPCell(new Phrase("", boldFont));
            cell.setColspan(2);
            tbl.addCell(cell);
            
            cell = new PdfPCell(new Phrase("Sec", boldFont));
            cell.setBorderWidthBottom(0);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            tbl.addCell(cell);
            
            cell = new PdfPCell(new Phrase("Gr", boldFont));
            cell.setBorderWidthBottom(0);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            tbl.addCell(cell);
            
            cell = new PdfPCell(empty);
            tbl.addCell(cell);
            
            cell = new PdfPCell(new Phrase("Grave", boldFont));
            cell.setHorizontalAlignment(1);
            cell.setPaddingTop(2);
            cell.setPaddingBottom(6);
            tbl.addCell(cell);
           
            cell = new PdfPCell(new Phrase("Row", boldFont));
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            tbl.addCell(cell);

            cell = new PdfPCell(new Phrase("Name", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);

            cell = new PdfPCell(new Phrase("Inter-Date", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);

            cell = new PdfPCell(new Phrase("Rel", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
            
            cell = new PdfPCell(new Phrase("Pos", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
                                    
            cell = new PdfPCell(new Phrase("Con", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
                                    
            cell = new PdfPCell(new Phrase("Ob", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
            
            cell = new PdfPCell(new Phrase("Typ", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
            
            cell = new PdfPCell(new Phrase("Typ", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
            
            cell = new PdfPCell(new Phrase("Size", boldFont));
            cell.setHorizontalAlignment(1);
            tbl.addCell(cell);
            
            tbl.setHeaderRows(2); //end the header that will repeat on every page
        
        
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
                          
            try {
                    //to test qry:*/ Utility.displayErrMsg ( res, conn, pstmt, qry);
                pstmt = conn.prepareStatement(qry);
                pstmt.setString(1, cemNum); 
                pstmt.setString(2, secId); //if you want to add parameter in the query
                rs = pstmt.executeQuery();

                boolean grey = true;
                int rowCount = 0;
                
                while (rs.next()) 
                {
                  rowCount ++;
                  
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
                    
                    cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(2)), font));//site_num
                    cell.setHorizontalAlignment(1);
                    if (grey) {
                       cell.setBackgroundColor(Color.LIGHT_GRAY);
                    }
                    cell.setPaddingTop(2);
                    cell.setPaddingBottom(6);
                    tbl.addCell(cell);
                    
                    cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(1)), font)); //row_num
                    if (grey) {
                       cell.setBackgroundColor(Color.LIGHT_GRAY);
                    }
                    tbl.addCell(cell);
                    
                    cell = new PdfPCell(new Phrase(Utility.blankIfNull(decNme), font)); //last_nme, first_nme, middle_nme
                    if (grey) {
                       cell.setBackgroundColor(Color.LIGHT_GRAY);
                    }
                    tbl.addCell(cell);
                    
                    cell = new PdfPCell(new Phrase(Utility.blankIfNull("RESERVED"), font));//interment_dt
                    if (grey) {
                       cell.setBackgroundColor(Color.LIGHT_GRAY);
                    }
                    tbl.addCell(cell);
                    
                    cell = new PdfPCell(new Phrase(Utility.blankIfNull(" "), font));//Rel,Dep Pos,Con,Ob
                    if (grey) {
                       cell.setBackgroundColor(Color.LIGHT_GRAY);
                    }
                    tbl.addCell(cell);
                    tbl.addCell(cell);
                    tbl.addCell(cell);
                    tbl.addCell(cell);
                    
                    cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(4)), font)); //sectiont_type_cd
                    if (grey) {
                       cell.setBackgroundColor(Color.LIGHT_GRAY);
                    }
                    tbl.addCell(cell);
                    
                    cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(5)), font));//type_cd
                    if (grey) {
                       cell.setBackgroundColor(Color.LIGHT_GRAY);
                    }
                    tbl.addCell(cell);
                    
                    cell = new PdfPCell(new Phrase(Utility.blankIfNull(DescrGrvSizeCd), font));//descr_grave_size_cd
                    if (grey) {
                       cell.setBackgroundColor(Color.LIGHT_GRAY);
                    }
                    tbl.addCell(cell); 
                    
                    if (grey) {grey=false;} else {grey=true;}
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
                      cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(2)), font));//site_num
                      cell.setHorizontalAlignment(1);
                      if (grey) {
                         cell.setBackgroundColor(Color.LIGHT_GRAY);
                      }
                      cell.setPaddingTop(2);
                      cell.setPaddingBottom(6);
                      tbl.addCell(cell);
                      
                      cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(1)), font)); //row_num
                      if (grey) {
                         cell.setBackgroundColor(Color.LIGHT_GRAY);
                      }
                      tbl.addCell(cell);
                      
                      cell = new PdfPCell(new Phrase(Utility.blankIfNull(stringArray[5]), font)); //decedent_name //new
                      if (grey) {
                         cell.setBackgroundColor(Color.LIGHT_GRAY);
                      }
                      tbl.addCell(cell);
                      
                      cell = new PdfPCell(new Phrase(Utility.blankIfNull(stringArray[1]), font)); //interment_dt
                      if (grey) {
                         cell.setBackgroundColor(Color.LIGHT_GRAY);
                      }
                      tbl.addCell(cell);
                      
                      cell = new PdfPCell(new Phrase(Utility.blankIfNull(stringArray[6]), font)); //relationship_cd //new
                      if (grey) {
                         cell.setBackgroundColor(Color.LIGHT_GRAY);
                      }
                      tbl.addCell(cell);
                      
                      cell = new PdfPCell(new Phrase(Utility.blankIfNull(depPos), font)); //depth_num, burial_position_cd
                      if (grey) {
                         cell.setBackgroundColor(Color.LIGHT_GRAY);
                      }
                      tbl.addCell(cell);
                      
                      cell = new PdfPCell(new Phrase(Utility.blankIfNull(stringArray[4]), font)); //container_type_cd
                      if (grey) {
                         cell.setBackgroundColor(Color.LIGHT_GRAY);
                      }
                      tbl.addCell(cell);
                      
                      cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(3)), font)); //position_cd
                      if (grey) {
                         cell.setBackgroundColor(Color.LIGHT_GRAY);
                      }
                      tbl.addCell(cell);
                      
                      cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(4)), font)); //section_type_cd
                      if (grey) {
                         cell.setBackgroundColor(Color.LIGHT_GRAY);
                      }
                      tbl.addCell(cell);
                      
                      cell = new PdfPCell(new Phrase(Utility.blankIfNull(rs.getString(5)), font)); //type_cd
                      if (grey) {
                         cell.setBackgroundColor(Color.LIGHT_GRAY);
                      }
                      tbl.addCell(cell);
                      
                      cell = new PdfPCell(new Phrase(Utility.blankIfNull(DescrGrvSizeCd), font)); //descr_grave_size_cd
                      if (grey) {
                         cell.setBackgroundColor(Color.LIGHT_GRAY);
                      }
                      tbl.addCell(cell);
                                                                        
                      if (grey) {grey=false;} else {grey=true;}
                    } //end of wile loop for iter.hasNext 
                  } //end of else loop */
                  
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