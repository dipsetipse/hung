/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.va.cem.bossreport.pdf;

/*
 * Aug 2011, increased font size from 8 to 10, CCR846
 * 
 * 
 * to test
 * encrypted n911frm,develop for PC use
 * http://localhost:8888/bossreport4/moarpdf?u=7989ff4b9b111f6f7aa167574f26d629d14d5dfdf82af533&cem_num=901&start_order_dt=&end_order_dt=&ship_prefix=MAB&shipment_fy=97&cbl_num=167
   to make it fail (wrong password for n911frm), try this:
      http://localhost:8888/bossreport/moarpdf?u=fb9fb36d58e31f91d5a3c92d2c4b9e1914dc1363e3aa5925
      SQL:
        SQL> select garencrypt('n911frm,test') from dual;
             GARENCRYPT('N911FRM,TEST')
             -------------------------------------------------
             fb9fb36d58e31f91d5a3c92d2c4b9e1914dc1363e3aa5925
 * 
 * */
import gov.va.cem.bossreport.util.Encryption;
import gov.va.cem.bossreport.util.Utility;
 
import com.lowagie.text.Chunk;
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

public class MonumentOrderAcknowledgementpdf extends HttpServlet 
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

        String cemNUM = req.getParameter("cem_num");
        String startDT = req.getParameter("start_order_dt");
        String endDT = req.getParameter("end_order_dt");
        
        String shipPrefix = req.getParameter("ship_prefix");
        String shipFY = req.getParameter("shipment_fy");
        String cblNUM = req.getParameter("cbl_num");
        
        int rowCount = 0; //new
        
        String cemTYPE = "";
        String magCEM = "";
        String micrfID = ""; 
        String orderDATE = "";
        String decID = "";
        String vetID = "";
        String insFNME = ""; 
        String insLNME = "";
        String remarksTxt = "";
        String endLINE = "";
        
        String qry = "";
        if (endDT.length() == 0)
        {
          endDT = "X";
        }
       

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

        Document doc = new Document(PageSize.LETTER, 4, 4, 18, 9);
            //new Document(PageSize.A4, 18, 18, 36, 18); <--L&R margin of 1/4 inch, Top=1/2, Bottom 1/4inch
            //new Document(PageSize.A4.rotate(), 72, 72, 36, 36); // or (PageSize.LETTER.rotate()); //rotate makes it landscape

        try { //1 PDF

            PdfWriter pdfw = PdfWriter.getInstance(doc, baosPDF);
            Font font = new Font(Font.HELVETICA, 10); //changed from 8 to 10
            //or you can declare: Font boldFont = new Font(Font.HELVETICA, 8, Font.BOLD); or
            Font boldFont = 
                new Font(FontFactory.getFont(FontFactory.HELVETICA, 10,  //changed from 8 to 10
                                             Font.BOLD)); //new Color(255, 0, 0)));

            doc.addAuthor(this.getClass().getName());
            doc.addCreationDate();
            doc.addProducer();
            doc.addCreator(this.getClass().getName());
            doc.addTitle("Monument Order Acknowledgement Report");  //change this to your report name
            doc.addKeywords("pdf, itext, Java, open source, http, worksheet, cemetery report");

            //get managing cemetery
            try {
              qry = "select managing_station_num, cemetery_type " +
                  "from cemetery where num = ? ";
              pstmt = conn.prepareStatement(qry);
              pstmt.setString(1, cemNUM);
              rs = pstmt.executeQuery();
              if(rs.next()) {
                magCEM  = rs.getString(1);
                cemTYPE = rs.getString(2);
              }
              pstmt.close();
              rs.close();
              
              //System.out.println("mngCem=" + magCEM + "  cemnum=" + cemNUM + " type=" + cemTYPE);
 
            }catch (SQLException e) {
                   System.err.println(new java.util.Date() + 
                                   " Error retrieving managing cemetery " );
            }

            Phrase phrase = new Phrase();
            phrase.setFont(new Font(Font.HELVETICA, 11, Font.BOLD));
            //headerPhrase.setLeading(1.5f); //this is supposed to be line spacing but does not work
            //phrase.add("DEPARTMENT OF VETERANS AFFAIRS\n");
            //phrase.add("NATIONAL CEMETERY SYSTEM\n");
            phrase.add("MONUMENT ORDER ACKNOWLEDGEMENT REPORT\n");
            phrase.add("CONSIGNEE " + cemTYPE +  magCEM);

/*
            Paragraph headerPhrase = new Paragraph();
            headerPhrase.setFont(new Font(Font.HELVETICA, 11, Font.BOLD));
            //headerPhrase.setLeading(1.2f, 1.2f); //does not work either
            headerPhrase.add("DEPARTMENT OF VETERANS AFFAIRS\n");
            headerPhrase.add("NATIONAL CEMETERY SYSTEM\n");
            headerPhrase.add("MONUMENT ORDER ACKNOWLEDGEMENT\n");
            headerPhrase.add("CONSIGNEE " + cemTYPE +  magCEM);
*/

            HeaderFooter header = new HeaderFooter(phrase, false);
            header.setAlignment(Element.ALIGN_CENTER); //.ALIGN_LEFT);
            header.setBorder(Rectangle.NO_BORDER);
            doc.setHeader(header);

            HeaderFooter footer = 
                new HeaderFooter(new Phrase(new java.util.Date() + 
                                                ",  Page ", font), true);

            footer.setBorder(Rectangle.NO_BORDER);
            doc.setFooter(footer);
//FOOTER ends
            doc.open();

//Display second header
            //determine number of columns in your report and set it here
            float[] arrayColWid = new float[2];
            arrayColWid[0] = 50;
            arrayColWid[1] = 50;
            PdfPTable tbl = new PdfPTable(arrayColWid); //num of cols
            tbl.setWidthPercentage(100);
            //tbl.getDefaultCell().setBorderWidth(1); //test to see table/column structure
            tbl.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            //declare cells
            PdfPCell empty = new PdfPCell();
            PdfPCell cell = new PdfPCell(empty);

            if (endDT.equals("X")) {
            try {
                    //to test qry: Utility.displayErrMsg ( res, conn, pstmt, qry);
                   qry = "select to_char(assembly_dt,'mm/dd/yyyy') from monument_apl " + 
                   "where shipment_fy= ? and ship_prefix= ? and cbl_num= ? " +
                   "and origin_cd = 'B' " +
                   "and status_cd = 'MO' and rownum < 2";

                    pstmt = conn.prepareStatement(qry);
                    pstmt.setInt   (1, Integer.parseInt(shipFY));
                    pstmt.setString(2, shipPrefix);
                    pstmt.setInt   (3, Integer.parseInt(cblNUM));

                    rs = pstmt.executeQuery();
                    
                    if(rs.next()) {// for single return

                       orderDATE = rs.getString(1);
                    }
                    rs.close();
                    pstmt.close();
              
                    cell = new PdfPCell(new Phrase("SHIPMENT NUM: " + shipPrefix  + "-" + shipFY + "-" + cblNUM, boldFont));
                    cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                    cell.setPaddingTop(2);
                    cell.setPaddingBottom(18);
                    cell.setBorder(Rectangle.NO_BORDER);
                    tbl.addCell(cell);
 
                    cell = new PdfPCell(new Phrase("ORDER DATE: " + orderDATE, boldFont));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER); //ALIGN_RIGHT);
                    cell.setPaddingTop(2);
                    cell.setPaddingBottom(18);
                    cell.setBorder(Rectangle.NO_BORDER);
                    tbl.addCell(cell);
 
            }catch (SQLException e) {
                    System.err.println(new java.util.Date() + 
                                   " Error retrieving data " );
                  
                    cell = new PdfPCell(new Phrase("Error displaying Title", boldFont));
                    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    cell.setPaddingTop(2);
                    cell.setPaddingBottom(6);
                    tbl.addCell(cell);
                  //return "Error displaying Title";
            }
          } else {

                    cell = new PdfPCell(new Phrase("DATES: " + startDT + " - " + endDT, boldFont));
                    cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                    cell.setPaddingTop(2);
                    cell.setPaddingBottom(18);
                    cell.setColspan(2);
                    cell.setBorder(Rectangle.NO_BORDER);
                    tbl.addCell(cell);
 
          }

          tbl.setHeaderRows(1); //end the header that will repeat on every page
          //doc.add(tbl);

//end of second header
        
            //Sample settings for table and cell
            //tbl.getDefaultCell().setPadding(2); //pad between border and first char in column
            //tbl.setSpacing(0); // spacing between cols
            //cell.setBorderWidth(1.25f); 
            //cell.setPaddingBottom(36); //pad or spacing between bottom border and last line in cell
            //cell.setLeading(1.2f, 1.2f); //increase line spacing "absolute leading: 1.2; relative leading: 1.2"


             qry = "select id, " +
                   "to_char(assembly_dt, 'MM/DD/YYYY'), " +
                   "ship_prefix, " + 
                   "shipment_fy, " + 
                   "cbl_num " +
                   "from monument_apl " +
                   "where station_num = ? ";
             if (endDT.equals("X")) {      
                qry = qry + " and ship_prefix = ? " +
                      " and shipment_fy = ? " + 
                      " and cbl_num = ?";
             } else {
                qry = qry +    
                   "and assembly_dt between to_date(?, 'MM/DD/YYYY') " +
                   "and to_date(?, 'MM/DD/YYYY') + 0.9999884 ";
//                   "and assembly_dt >= trunc(to_date(?, 'MM/DD/YYYY')) " +
//                   "and assembly_dt <= trunc(to_date(?, 'MM/DD/YYYY')) ";
             }
             qry = qry +
                   "and origin_cd = 'B' " +
                   "and status_cd = 'MO' " +
                   "order by ship_prefix, shipment_fy, cbl_num, section_id, row_num, site_num ";

             try {
                    //to test qry:*/ Utility.displayErrMsg ( res, conn, pstmt, qry);
                    //test*/ System.err.println(qry+"\n");
                    pstmt = conn.prepareStatement(qry);
                    pstmt.setString(1, magCEM);
                    if (endDT.equals("X")) {      
                       pstmt.setString(2, shipPrefix);
                       pstmt.setInt(3, Integer.parseInt(shipFY));
                       pstmt.setInt(4, Integer.parseInt(cblNUM));
                    } else {
                      //System.err.println(" before startDT..." + startDT + " cem=" + magCEM );
                       pstmt.setString(2, startDT);
                      //System.err.println(" before endDT..." + endDT);
                       pstmt.setString(3, endDT);
                    }

                    rs = pstmt.executeQuery();
                    //System.out.println("after exec qry");

                    int recordCtr = 0; //counts from 0 to 2 (tells how many records were printed on one row)
                    while (rs.next()) {
                      rowCount ++; //new
                      micrfID = rs.getString(1);
                      orderDATE = rs.getString(2);
                      shipPrefix = rs.getString(3);
                      shipFY = rs.getString(4); //(this is number type)
                      cblNUM = rs.getString(5); //(this is number type)

                      //test*/System.err.println("in while loop " + " uID=" + micrfID + " prefix=" + shipPrefix + " FY="+ shipFY + " cbl=" + cblNUM);
                      
                                                                                                          
//Start of nested table
    //NOTE: When I declared the nested table above or outside the while loop, it would print two records on one row.
        //getDecedentName - 1 column
            //This is for the actual inscription
            float[] arrayColWidNestedA = new float[2]; //2 columns

            arrayColWidNestedA[0] = 65;
            arrayColWidNestedA[1] = 35;
            PdfPTable nestedTblA = new PdfPTable(arrayColWidNestedA);
            nestedTblA.setWidthPercentage(100);
            //nestedTblA.getDefaultCell().setBorderWidth(1); //test to see table/column structure
            nestedTblA.getDefaultCell().setBorder(Rectangle.NO_BORDER);

                      Chunk vetName = new Chunk(getDecedentName(micrfID,
                                                                  shipPrefix,
                                                                  shipFY,
                                                                  cblNUM,
                                                                  conn), boldFont);
                      vetName.setUnderline(0.2f, -1f); /*first parameter defines the thickness of the line; 
                                                           the second specifies the Y position above (Y > 0) or under (Y < 0) the baseline of the Chunk.*/

                      phrase = new Phrase();
                      phrase.add(vetName);

                      //cell = new PdfPCell(new Phrase("Testing: record:" + rowCount + " cell ctr:" + recordCtr, font));
                      cell = new PdfPCell(phrase);
                      cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                      cell.setPaddingTop(2);
                      cell.setPaddingBottom(6);
                      cell.setColspan(2);
                      //cell.setFixedHeight(72f);
                      cell.setBorder(Rectangle.NO_BORDER);
                      nestedTblA.addCell(cell);

        //getInscription - front - column 1

                      Chunk inscripFront = new Chunk (getInscriptionLinesFront(micrfID,
                                                                  shipPrefix,
                                                                  shipFY,
                                                                  cblNUM,
                                                                  conn), font);
                      phrase = new Phrase();
                      phrase.add(inscripFront);
                      cell = new PdfPCell(phrase);
                      //cell = new PdfPCell(new Phrase("Front", font));
                      cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                      cell.setPaddingTop(2);
                      cell.setPaddingBottom(6);
                      cell.setLeading(1.2f, 1.2f);
                      cell.setBorder(Rectangle.NO_BORDER);
                      nestedTblA.addCell(cell);

        //getInscription - back - column 2

                      //alternative to above when declaring a cell
                      cell = new PdfPCell(new Phrase(getInscriptionLinesBack(micrfID,
                                                                  shipPrefix,
                                                                  shipFY,
                                                                  cblNUM,
                                                                  conn), font));
                      //cell = new PdfPCell(new Phrase("Back", font));
                      cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                      cell.setPaddingTop(2);
                      cell.setPaddingBottom(6);
                      cell.setLeading(1.2f, 1.2f);
                      cell.setBorder(Rectangle.NO_BORDER);
                      nestedTblA.addCell(cell);

        //getRemarks- 1 column

                      Chunk txtRemark = new Chunk(getRemarksTxt(micrfID, conn), font);

                      phrase = new Phrase();
                      phrase.add(txtRemark);
                      cell = new PdfPCell(phrase);
                      //cell = new PdfPCell(new Phrase("Remarks", font)); //(phrase); 
                      cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                      cell.setPaddingTop(2);
                      cell.setPaddingBottom(6);
                      cell.setColspan(2);
                      cell.setLeading(1.2f, 1.2f);
                      cell.setBorder(Rectangle.NO_BORDER);
                      nestedTblA.addCell(cell);

                      cell = new PdfPCell(new Phrase("RCV DT ________ VER DT ________ SET DT ________", font));
                      cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                      cell.setPaddingTop(2);
                      cell.setPaddingBottom(6);
                      cell.setColspan(2);
                      //cell.setLeading(1.2f, 1.2f);
                      cell.setPaddingBottom(50);
                      cell.setBorder(Rectangle.NO_BORDER);
                      nestedTblA.addCell(cell);

        //add nested table into the main table
                      cell = new PdfPCell(nestedTblA);
                      //cell.setBorder(Rectangle.NO_BORDER);
                      tbl.addCell(cell);
                      //tbl.addCell(new PdfPCell(nestedTblA));

     //System.out.println ("added nested table recordCtr=" + recordCtr);

//end of nested table

                      if (recordCtr < 2) { recordCtr++;}
                      if (recordCtr == 2) {
                        recordCtr = 0;
                      }
                      
                    }//end while
                    
            //System.out.println ("out of while, recordCtr=" + recordCtr);

                      if (recordCtr == 1) {

                        //you need to add a cell if only record is printed
                        cell = new PdfPCell(empty);
                        //cell = new PdfPCell(new Phrase("only one record - test",font));
                        cell.setBorder(Rectangle.NO_BORDER);
                        tbl.addCell(cell);
                      }
            
                    rs.close();
                    pstmt.close();
                
                    conn.close();
                    rs = null;
                    pstmt = null;
                    conn = null;

             } catch (SQLException e) {
                System.err.println(new java.util.Date() + 
                                   " Error retrieving data " + "\n" + qry );
                return;
                      
             }        

             if ( rowCount == 0 ) {
                   doc.add(new Phrase("No Records Found ", font));
             } else {
                   doc.add(tbl);
             } //new
 

            //doc.add(tbl); //new comment out

            //System.out.println ("outside of while loop");
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
  
   /* Method to return Header (does not work properly because Line 2 has to be left justified
   public static String getHeader(String endDate, String startDate,
                                  String shipFY, String shipPrefix, String cblNum, 
                                  String cemTYPE, String mngCEM,
                                  Connection conn)
  { 
            String qry = "select to_char(assembly_dt,'mm/dd/yyyy') from monument_apl " + 
                   "where shipment_fy= ? and ship_prefix= ? and cbl_num= ? " +
                   "and origin_cd = 'B' " +
                   "and status_cd = 'MO' and rownum < 2";
                              
          if (endDate.equals("X")) {
            try {
                    //to test qry: Utility.displayErrMsg ( res, conn, pstmt, qry);
                    String assemblyDate = "";
                    PreparedStatement pstmt = conn.prepareStatement(qry);
                    pstmt.setInt   (1, Integer.parseInt(shipFY));
                    pstmt.setString(2, shipPrefix);
                    pstmt.setInt   (3, Integer.parseInt(cblNum));

                    ResultSet rs = pstmt.executeQuery();
                    
                    if(rs.next()) {// for single return

                       assemblyDate = rs.getString(1);
                    }
                    rs.close();
                    pstmt.close();
              
                    rs = null;
                    pstmt = null;
                    return
                       "                                       MONUMENT ORDER ACKNOWLEDGEMENT REPORT\n" +
                       "SHIPMENT NUM: " + shipPrefix  + "-" + shipFY + "-" + cblNum + 
                       "            CONSIGNEE " + cemTYPE +  mngCEM + 
                       "                  ORDER DATE: " + assemblyDate + "\n";

            }catch (SQLException e) {
                   System.err.println(new java.util.Date() + 
                                   " Error retrieving data " + "\n" + qry );
                  
                  return "Error displaying Title";
            }
          } else {
              return
                       "                                       MONUMENT ORDER ACKNOWLEDGEMENT REPORT\n" +
                       "DATES: " + startDate + " - " + endDate + "               CONSIGNEE " + cemTYPE +  mngCEM + " \n";
          }

   } //getHeader
*/

   // Method to return Shipment Num with Decedent name
   public static String getDecedentName(String micrfID, String shipNUM, String shipFY,
                                        String cblNUM, Connection conn)
  { 
            String decID = "";
            String vetID = "";
            String insFNME = "";
            String insLNME = "";
            String inscripLine = "";
            String qry = "";
          
            qry = "select decedent_id, veteran_id, inscr_first_nme, inscr_last_nme " +
                "from decedent_monument where microfilm_id = ? " +
                "and inscr_death_dt = (select unique(max(inscr_death_dt)) " +
                                      "from decedent_monument " +
                                      "where microfilm_id = ? )";
                              
            try {
                    //to test qry: Utility.displayErrMsg ( res, conn, pstmt, qry);
                    PreparedStatement pstmt = conn.prepareStatement(qry);
                    pstmt.setString(1, micrfID); //if you want to add parameter in the query
                    pstmt.setString(2, micrfID);
                    ResultSet rs = pstmt.executeQuery();
                    
                    if(rs.next()) // for single return
                    {
                      decID = rs.getString(1);
                      vetID = rs.getString(2); 
                      insFNME = rs.getString(3);
                      insLNME = rs.getString(4);
                      inscripLine = shipNUM +"-" + shipFY + "-" + cblNUM + " " + insLNME + ", " + insFNME + "\n"; 
                    } else 
                    {
                      inscripLine = " missing shipment number\n"; 
                    }
                                             
                    rs.close();
                    pstmt.close();   
              
                    rs = null;
                    pstmt = null;
                }catch (SQLException e) {
                   System.err.println(new java.util.Date() + 
                                   " Error retrieving data " + "\n" + qry );
                  
                  //return;
               }
           return inscripLine;  
 
  }


   // Method to return Final Inscription Front
   public static String getInscriptionLinesFront(String micrfID, String shipNUM, String shipFY,
                                           String cblNUM, Connection conn)
  { 
            String decID = "";
            String vetID = "";
            String insFNME = "";
            String insLNME = "";
            String inscripLine = "";
            String qry = "";
            
            qry = "select decedent_id, veteran_id, inscr_first_nme, inscr_last_nme " +
                "from decedent_monument where microfilm_id = ? " +
                "and inscr_death_dt = (select unique(max(inscr_death_dt)) " +
                                      "from decedent_monument " +
                                      "where microfilm_id = ? )";
                              
            try {
                    //to test qry: Utility.displayErrMsg ( res, conn, pstmt, qry);
                    PreparedStatement pstmt = conn.prepareStatement(qry);
                    pstmt.setString(1, micrfID); //if you want to add parameter in the query
                    pstmt.setString(2, micrfID);
                    ResultSet rs = pstmt.executeQuery();
                    
                    if(rs.next()) // for single return
                    {
                      decID = rs.getString(1);
                      vetID = rs.getString(2); 
                      insFNME = rs.getString(3);
                      insLNME = rs.getString(4);
                      //inscripLine = "\n"; //decID + "\n" + vetID + "\n" + insFNME + "\n" + insLNME + "\n"; 
                      rs.close();
                      pstmt.close();
                      qry = "select line, front_back_ind " + 
                            "from final_inscription where " +
                            "monapl_id = ? and front_back_ind = ? " + 
                            "order by front_back_ind, seq_num";
                      pstmt = conn.prepareStatement(qry);
                      pstmt.setString(1, micrfID);
                      pstmt.setString(2, "F");
                      rs = pstmt.executeQuery();
                       while (rs.next()) 
                       {
                         inscripLine = inscripLine + /*rs.getString(2) + " " + */ rs.getString(1) + "\n";
                       }
                      
                    } else 
                    {
                      inscripLine = " missing inscription\n"; 
                    }
                                             
                    rs.close();
                    pstmt.close();   
              
                    rs = null;
                    pstmt = null;
                }catch (SQLException e) {
                   System.err.println(new java.util.Date() + 
                                   " Error retrieving data " + "\n" + qry );
                  
                  //return;
               }
           //inscripLine = inscripLine;
           //last line =  "RCV DT ________ VER DT ________ SET DT ________" ;
 
           return inscripLine; 
 

   }               

   // Method to return Final Inscription Back
   public static String getInscriptionLinesBack (String micrfID, String shipNUM, String shipFY,
                                           String cblNUM, Connection conn)
  { 
            String decID = "";
            String vetID = "";
            String insFNME = "";
            String insLNME = "";
            String inscripLine = "";
            String qry = "";
          
            qry = "select decedent_id, veteran_id, inscr_first_nme, inscr_last_nme " +
                "from decedent_monument where microfilm_id = ? " +
                "and inscr_death_dt = (select unique(max(inscr_death_dt)) " +
                                      "from decedent_monument " +
                                      "where microfilm_id = ? )";
                              
            try {
                    //to test qry: Utility.displayErrMsg ( res, conn, pstmt, qry);
                    PreparedStatement pstmt = conn.prepareStatement(qry);
                    pstmt.setString(1, micrfID); //if you want to add parameter in the query
                    pstmt.setString(2, micrfID);
                    ResultSet rs = pstmt.executeQuery();
                    
                    if(rs.next()) // for single return
                    {
                      decID = rs.getString(1);
                      vetID = rs.getString(2); 
                      insFNME = rs.getString(3);
                      insLNME = rs.getString(4);
                      //inscripLine = decID + "\n" + vetID + "\n" + insFNME + "\n" + insLNME + "\n"; 
                      rs.close();
                      pstmt.close();
                      qry = "select line, front_back_ind " + 
                            "from final_inscription where " +
                            "monapl_id = ? and front_back_ind = ? " + 
                            "order by front_back_ind, seq_num";
                      pstmt = conn.prepareStatement(qry);
                      pstmt.setString(1, micrfID);
                      pstmt.setString(2, "B");
                      rs = pstmt.executeQuery();
                      //inscripLine = "\n";
                       while (rs.next()) 
                       {
                         inscripLine = inscripLine + /*rs.getString(2) + " " +*/ rs.getString(1) + "\n";
                       }
                      
                    } else 
                    {
                      inscripLine = " missing inscription\n"; 
                    }
                                             
                    rs.close();
                    pstmt.close();   
              
                    rs = null;
                    pstmt = null;
                }catch (SQLException e) {
                   System.err.println(new java.util.Date() + 
                                   " Error retrieving data " + "\n" + qry );
                  
                  //return;
               }
           return inscripLine;
 
   }  
   
public static String getRemarksTxt(String micrfID, Connection conn)
  { 
            String remarksTxt = "";
                        
            String qry = "";
          
            qry = "select remarks_txt " +
                  "from decedent_monument where microfilm_id = ? ";
                              
            try {
                    //to test qry: Utility.displayErrMsg ( res, conn, pstmt, qry);
                    PreparedStatement pstmt = conn.prepareStatement(qry);
                    pstmt.setString(1, micrfID); //if you want to add parameter in the query
                    ResultSet rs = pstmt.executeQuery();
                    
                    if(rs.next()) // for single return
                    {
                      remarksTxt = Utility.blankIfNull(rs.getString(1));
                      remarksTxt = "\n" + remarksTxt;
                    } 
                    //System.out.println ("* remarks_txt = " + remarksTxt);                        
                    rs.close();
                    pstmt.close();   
              
                    rs = null;
                    pstmt = null;
                }catch (SQLException e) {
                   System.err.println(new java.util.Date() + 
                                   " Error retrieving data " + "\n" + qry );
                  remarksTxt = " Error retrieving data " + "\n" + qry;
                  
                  //return;
               }
           
           return remarksTxt;  
 
  }


}