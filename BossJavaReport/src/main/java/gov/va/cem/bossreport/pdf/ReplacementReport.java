/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.va.cem.bossreport.pdf;
//: ReplacementReport
/* ReplacementReport.java
   This online program generates a PDF report for the cemetery

   Library needed for this to run "itext-1.2.jar". The library should be in the common library folder.
   Currently it resides in dev: /data11/oraxmid/j2ee/home/default-web-app/WEB-INF/lib
   Currently it resides in boss: $ORACLE_HOME/j2ee/home/default-web-app/WEB-INF/lib

   Location of class:
   in dev: /data11/oraxmid/j2ee/home/default-web-app/WEB-INF/classes
   in prod: /usr/product/oraxias/j2ee/home/default-web-app/WEB-INF/classes

   Changes:
   6/2/05 - Added decedent_monument_history in SQL (only the 1st SQL with the union)
   7/20/05 - Added a field as shown below for sorting the UNION sql, otherwise sorting is not correct if to_char(interment_dt)
        "    nvl(g.interment_dt, to_date('01/01/1100', 'mm/dd/yyyy')) " + //for sorting (has to be date format)
   7/11/06 - change to data source / connection pool and uses actual username and pw.
   8/31/06 - changed where clause to include all cemetery types
*/

//Format 1
import gov.va.cem.bossreport.util.Encryption;
import gov.va.cem.bossreport.util.Utility;

import java.io.*; 
import java.sql.*; 

import javax.servlet.*; 
import javax.servlet.http.*; 

// import the iText packages
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;


//import java.awt.Color;

//for Oracle connection pool
//import oracle.jdbc.driver.*; 
//import oracle.jdbc.pool.*; 

//for DataSource
import javax.sql.DataSource;
import javax.naming.InitialContext;

 
public class ReplacementReport extends HttpServlet{ 
 
  // below is for connection pooling
  //private OracleConnectionPoolDataSource ocpds; 
  //private OracleConnectionCacheImpl ods; 

  //private DataSource ods = null;

  public void init(ServletConfig config) throws ServletException {

    super.init(config);

    /* using Data Source Name
    String DATASOURCE = "jdbc/OracleDS";
 
    try {
        InitialContext ic = new InitialContext();
        ods = (DataSource)ic.lookup(DATASOURCE);
    } 
      catch (javax.naming.NamingException e) { 
        System.out.println("Production Datasource error - javax.naming.NamingException " + e.toString());
    }     

    /* Won't use connection pooling
    try {	
       Class.forName( "oracle.jdbc.driver.OracleDriver" );
    }
    catch (ClassNotFoundException e){
       System.err.println ("ClassNotFoundException");
       return;
    }

    try {
    // Open connection (form of @host:port:SID)
        ocpds =new OracleConnectionPoolDataSource(); 
        //ocpds.setURL("jdbc:oracle:thin:@10.228.1.131:1521:dm9i");
        ocpds.setURL("jdbc:oracle:thin:@dev.cem.va.gov:1521:dev");

        //ocpds.setUser("c101mgr"); 
        //ocpds.setPassword("develop"); 

        // Associate it with the Cache 
        ods = new OracleConnectionCacheImpl(ocpds); 

        // Set the Max Limit 
        ods.setMaxLimit (1); 
 
        // Set the Scheme 
        ods.setCacheScheme (OracleConnectionCacheImpl.FIXED_RETURN_NULL_SCHEME); 

    }
    catch (SQLException e){
       System.err.println ("err creating pool, java.sql.SQLException");
       //return;
    }
    */
  }
 

  // this gets called first on initial load
  public void doGet(HttpServletRequest  request, 
                    HttpServletResponse response)
                      throws javax.servlet.ServletException, java.io.IOException {

      doPost (request, response);
  }


   // this gets called after clicking the "OK" button or when doGet calls it
   public void doPost(HttpServletRequest req, HttpServletResponse res) 
                                throws javax.servlet.ServletException, java.io.IOException { 
      Connection conn = null; 
      Statement stmt = null;
      ResultSet rs = null;

                //get parameters

                String userID = req.getParameter("u");
                userID = (Encryption.decrypt(userID));
                String cem  = req.getParameter("c");
                String date1 = req.getParameter("d1");
                String date2 = req.getParameter("d2");
                String expCd = req.getParameter("exp");
                String errCd = req.getParameter("err");
                String replCd = req.getParameter("rep");
                String expCdWhere = "";
                String errCdWhere = "";
                String replCdWhere = " m.repl_reason_cd is not null and ";
                String cemName = "";
 
                /*
                if (pwdb != ""  && pwdb != null) {
	             //System.out.println("@ pos=" + pwdb.indexOf("@"));
	             //System.out.println("pw=" + pwdb.substring(0,pwdb.indexOf("@")));
	             //System.out.println("db=" + pwdb.substring(pwdb.indexOf("@")+1));
	             pw= pwdb.substring(0,pwdb.indexOf("@"));
	             db= pwdb.substring(pwdb.indexOf("@")+1);
                }
                */

                if (expCd != ""  && expCd != null) {
                   expCd = expCd.toUpperCase();
                   expCdWhere = " m.industry_expense_cd=upper('" + expCd + "') and ";
                }
                if (errCd != ""  && errCd != null) {
                   errCd = errCd.toUpperCase();
                   errCdWhere = " m.industry_grp_err_cd=upper('" + errCd + "') and ";
                }
                if (replCd != ""  && replCd != null) {
                   replCd = replCd.toUpperCase();
                   replCdWhere = " m.repl_reason_cd=upper('" + replCd + "') and ";
                }

      //System.out.println("Creates BAOS");
      //OutputStream os = new ByteArrayOutputStream();

      ByteArrayOutputStream baosPDF = new ByteArrayOutputStream();

      //Rectangle pageSize = new Rectangle(144, 720);
      //Rectangle pageSize = new Rectangle(600,800);
      //pageSize.setBackgroundColor(new java.awt.Color(0xFF, 0xFF, 0xDE));

      //Document doc = new Document(pageSize);
      Document doc = new Document(PageSize.LETTER.rotate()); //rotate makes it landscape

      try {

                   /* will use instead actual username and pw
                   //will hardcode userID
                   if (db.equals("BOSS")) {
                      userID = "N901XXX";
                      pw = "DBASEs";
                      //userID = "menor";
                      //pw = "coops";
                      userID = userID;
                      pw = pwdb;
                   } else if (db.equals("DEV")) {
                      userID = "D901FRM";
                      pw = "DEVELOP";
                   } else if (db.equals("BETA")) {
                      userID = "T901RNG";
                      pw = "TRAINING";
                   } else {
                      //userID = "N901XXX";
                      //pw = "DBASE";
                      userID = userID;
                      pw = pwdb;
                   }
                   */

                   //userID = userID;
                   //pw = pwdb;
               
                   conn = Utility.logon (userID, req, res);

                   if (conn == null) {
                      displayErrMsg ( res, conn, stmt, "Error Connecting to the database"); // as " + userID);
                      return;
                   }

                   stmt = conn.createStatement ();

                   if (!Utility.setRole(conn, "nca_sic")) {
                      displayErrMsg ( res, conn, stmt, "Error accessing data on the database.");
                      return;
                   }

/*
                  //(for some reason, does not clear table righrt away after logging out)
                  //cannot delete temp tanle because of insufficient privs
                  //Did not work also
                  System.out.println ("temp table check");
                  try {
                       rs = stmt.executeQuery (" select CEMETERY_NUM "  +
                                                " FROM replacement_wrktbl " +
                                                " where rownum < 2" );
                       if (rs.next()) {
                         System.out.println ("Will delete");                       
                         //stmt.executeUpdate("delete from replacement_wrktbl");
                         rs.close();
                         stmt.close();
                         conn.close();
                         rs = null;
                         //login again
                         conn = Utility.logon (ods, userID, res);
                         if (conn == null) {
                            displayErrMsg ( res, conn, stmt, "Error Connecting to the database as " + userID);
                            return;
                         }

                         stmt = conn.createStatement ();

                         if (!setRole(conn, "nca_sic")) {
                            displayErrMsg ( res, conn, stmt, "Error accessing data on the database.");
                            return;
                         }
                       }
                       if (rs != null) {
                          rs.close();
                       }
                         
                  } catch (SQLException e){
                         System.err.println(new java.util.Date() + " Replacement Report - Error deleting temp table " + e.toString());
                         displayErrMsg ( res, conn, stmt, "Error deleting Replacement report table.");
                         //return;
                  }
*/

      } catch (SQLException e){
         System.err.println(new java.util.Date() + " Replacement Report - Error establishing connection, stmt or role");
         displayErrMsg ( res, conn, stmt, "Error Connecting to the database.");
         return; 
      }


      try {

                PdfWriter pdfw = PdfWriter.getInstance(doc, baosPDF);
                Font font = new Font(Font.HELVETICA,8);
                //or you can declare: Font boldFont = new Font(Font.HELVETICA, 8, Font.BOLD); or
                Font boldFont = new Font(FontFactory.getFont(FontFactory.HELVETICA, 8, Font.BOLD)); //new Color(255, 0, 0)));

		doc.addAuthor(this.getClass().getName());
		doc.addCreationDate();
		doc.addProducer();
		doc.addCreator(this.getClass().getName());
		doc.addTitle("This is a title.");
		doc.addKeywords("pdf, itext, Java, open source, http");
			
		//doc.setPageSize(PageSize.LETTER);
			
                //get cemName
                try {

                        rs = stmt.executeQuery (" select nme " +
                                                " FROM cemetery " +
                                                " where num= '" + cem + "'");

                        if (rs.next()) {
                           cemName = rs.getString(1);
                        } else {
                           cemName = "Invalid Cemetery " + cem;
                           System.err.println (new java.util.Date() + " Replacement Report - Invalid Cem " + cem);
                           displayErrMsg ( res, conn, stmt, "Invalid cemetery number " + cem);
                        }
                        rs.close();

                } catch (SQLException e){
                        System.err.println(new java.util.Date() + " Replacement Report - Error retrieving cem name");
                        displayErrMsg ( res, conn, stmt, "Error retrieving cemetery name for " + cem);
                        return;
                }

		HeaderFooter header = new HeaderFooter(
                                                        //new Phrase(cemName + " (" + cem + ") REPLACEMENT REPORT", new Font(Font.HELVETICA, 11, Font.BOLD)), 
                                                        new Phrase(cemName + " (" + cem + ") REPLACEMENT REPORT, " + date1 + " - " + date2, new Font(Font.HELVETICA, 11, Font.BOLD)), 
                                                        //new Phrase("REPLACEMENT REPORT"), 
                                                        false);
                header.setAlignment(1);
		header.setBorder(Rectangle.NO_BORDER);
		doc.setHeader(header);

		HeaderFooter footer = new HeaderFooter(
							new Phrase(new java.util.Date() + ",  Page ", font),
							true);

		footer.setBorder(Rectangle.NO_BORDER);
		doc.setFooter(footer);
		
                doc.open();

                /*
                test, add image and some paragraphs to the document
                Image img = Image.getInstance("blubetta.jpg"); //(filename);
                doc.add(img);
 
                for (int i = 0; i < 1; i++) {
                   doc.add(new Paragraph("It's " + new java.util.Date(), font));
                }
                
                //String strServerInfo = this.getServletContext.getServerInfo();
                ServletContext ctx = this.getServletContext();
                String strServerInfo = ctx.getServerInfo();

                if (strServerInfo != null) {
                   doc.add(new Paragraph("Servlet engine: " + strServerInfo));
                }
                */


                Table tbl = null;

                try {
                
                  /*delete temp table (for some reason, does not clear table righrt away after logging out)
                  Did not work because of insufficient privs but we will add it anyway in case there is a problem
                  in the future. What needs to do is grant on the DBA side
                  System.out.println ("temp table check");
                  */
                  try {
                       rs = stmt.executeQuery (" select CEMETERY_NUM "  +
                                                " FROM replacement_wrktbl " +
                                                " where rownum < 2" );
                       if (rs.next()) {
                         //System.out.println ("Will delete");                       
                         stmt.executeUpdate("delete from replacement_wrktbl");
                       }
                       rs.close();
                         
                  } catch (SQLException e){
                         System.err.println(new java.util.Date() + " Replacement Report - Error deleting temp table. \n " + //e.toString());
                         " Please grant Delete on Replacement_Wrktbl to all BOSS users");
                         //displayErrMsg ( res, conn, stmt, "Error deleting Replacement report table.");
                         //return;
                  }

                   //if (replaOrder(conn, "901", "09/01/2000", "07/31/2003")) {}
                   if (!replaOrder(conn, cem, date1, date2, expCdWhere, errCdWhere, replCdWhere)) {
                      displayErrMsg ( res, conn, stmt, "Error accessing data on the database.");
                      return;
                   }

                   tbl = new Table(9); //num of cols
                   tbl.setWidth(100);
                   int[] arrayColWid = new int[9];
                   arrayColWid[0] = 7;
                   arrayColWid[1] = 7;
                   arrayColWid[2] = 7;
                   arrayColWid[3] = 20;
                   arrayColWid[4] = 12;
                   arrayColWid[5] = 6;
                   arrayColWid[6] = 5;
                   arrayColWid[7] = 5;
                   arrayColWid[8] = 31;
                   tbl.setWidths(arrayColWid);
                   tbl.setBorderWidth(0);
                   tbl.setPadding(2); //pad between border and first char in column
                   tbl.setSpacing(0); // spacing between cols

/* moved up to the header
                   //get cemName
                   try {
                        rs = stmt.executeQuery (" select num || ' ' || nme "  +
                                                " FROM cemetery " +
                                                " where num= '" + cem + "'");


                        Cell cell = null;

                        if (rs.next()) {
                           cell = new Cell ( new Phrase(rs.getString(1), boldFont) );
                        } else {
                           cell = new Cell ( new Phrase("Error on Report Criteria, Invalid Cemetery " + cem, boldFont) );
                           System.out.println (new java.util.Date() + " Replacement Report - Invalid Cem " + cem);
                        }

                        cell.setHeader(true);
                        cell.setColspan(9);
                        cell.setBorder(Rectangle.NO_BORDER);
                        tbl.addCell(cell);

                   } catch (SQLException e){
                        System.err.println(new java.util.Date() + " Replacement Report - Error retrieving cem name");
                        Cell cell = new Cell ( new Phrase("Error on Report Criteria", boldFont) );
                        cell.setHeader(true);
                        cell.setColspan(9);
                        cell.setBorder(Rectangle.NO_BORDER);
                        tbl.addCell(cell);
                   }


                      Cell cell = new Cell ( new Phrase("Time Period = " + date1 + " - " + date2, font) );
                      cell.setColspan(9);
                      cell.setBorder(Rectangle.NO_BORDER);
                      tbl.addCell(cell);
*/
                   Cell cell = null; //new Cell(" ");
/*                   cell.setHeader(true);
                   cell.setBorder(Rectangle.NO_BORDER);
                   cell.setColspan(9);
                   tbl.addCell(cell);
/*
                   cell = new Cell( new Phrase(cem + " " + cemName, boldFont) );
                   cell.setHeader(true);
                   cell.setBorder(Rectangle.NO_BORDER);
                   cell.setColspan(9);
                   tbl.addCell(cell);

                   cell = new Cell( new Phrase("Date: " + date1 + " - " + date2, boldFont) );
                   cell.setHeader(true);
                   cell.setBorder(Rectangle.NO_BORDER);
                   cell.setColspan(9);
                   tbl.addCell(cell);
*/
                   //get additional criteria, if any
                   try {
                      String strCriteria = ""; //date1 + " - " + date2; (remove rem if you want to include dates on 2nd lne)
                      //boolean firstTime = true;
                      if ((expCd != ""  && expCd != null) || (errCd != ""  && errCd != null) || (replCd != ""  && replCd != null)) {

                         //cell = new Cell( new Phrase("LIMITED TO ", boldFont) );
                         //cell.setBorder(Rectangle.NO_BORDER);
                         //tbl.addCell(cell);

                         if (expCd != ""  && expCd != null) {
                            //firstTime = false;
                            rs = stmt.executeQuery (" select descr "  +
                                                " FROM Repl_Industry " +
                                                " where cd= '" + expCd + "'");

                            if (rs.next()) {
                               //cell = new Cell( new Phrase("EXPENSE: " + rs.getString(1), boldFont) );
                               strCriteria = strCriteria + "   EXPENSE: " + rs.getString(1);
                            } else {
                               //cell = new Cell( new Phrase("EXPENSE CODE: " + expCd, boldFont) );
                               strCriteria = strCriteria + "   EXPENSE CODE: " + expCd;
                            }
                            rs.close();
                            //cell.setColspan(9);
                            //cell.setBorder(Rectangle.NO_BORDER);
                            //tbl.addCell(cell);

                         }

                         if (errCd != ""  && errCd != null) {
                            rs = stmt.executeQuery (" select descr "  +
                                                " FROM Repl_Industry " +
                                                " where cd= '" + errCd + "'");

                            /*
                            if (firstTime) {
                               cell = new Cell( new Phrase("LIMITED TO ", boldFont) );
                            } else {
                               firstTime = false;
                               cell = new Cell( " " );
                            }
                            cell.setBorder(Rectangle.NO_BORDER);
                            tbl.addCell(cell);
                            */

                            if (rs.next()) {
                               //cell = new Cell( new Phrase("ERROR: " + rs.getString(1), boldFont) );
                               strCriteria = strCriteria + "   ERROR: " + rs.getString(1);
                            } else {
                               //cell = new Cell( new Phrase("ERROR CODE: " + errCd, boldFont) );
                               strCriteria = strCriteria + "   ERROR CODE: " + errCd;
                            }
                            
                            rs.close();
                            //cell.setColspan(9); 
                            //cell.setBorder(Rectangle.NO_BORDER);
                            //tbl.addCell(cell);

                         }

                         if (replCd != ""  && replCd != null) {
                            rs = stmt.executeQuery (" select descr "  +
                                                " FROM Repl_Reason " +
                                                " where cd= '" + replCd + "'");

                            /*
                            if (firstTime) {
                               cell = new Cell( new Phrase("LIMITED TO ", boldFont) );
                            } else {
                               cell = new Cell( " " );
                            }
                            cell.setBorder(Rectangle.NO_BORDER);
                            tbl.addCell(cell);
                            */

                            if (rs.next()) {
                               //cell = new Cell( new Phrase("REPLACEMENT REASON:  " + rs.getString(1), boldFont) );
                               strCriteria = strCriteria + "   REPLACEMENT REASON: " + rs.getString(1);
                            } else {
                               cell = new Cell( new Phrase("REPLACEMENT REASON: " + replCd, boldFont) );
                               strCriteria = strCriteria + "   REPLACEMENT REASON CODE: " + replCd;
                            }
                            rs.close();
                            //cell.setColspan(9); 
                            //cell.setBorder(Rectangle.NO_BORDER);
                            //tbl.addCell(cell);

                         }

                         cell = new Cell( new Phrase(strCriteria, new Font(Font.HELVETICA, 10, Font.BOLD)) );
                         cell.setHorizontalAlignment(1);
                         cell.setHeader(true);
                         cell.setBorder(Rectangle.NO_BORDER);
                         cell.setColspan(9);
                         tbl.addCell(cell);

                         cell = new Cell( " " );
                         cell.setColspan(9); 
                         //cell.setRowspan(3); 
                         cell.setBorder(Rectangle.NO_BORDER);
                         tbl.addCell(cell);

                         cell = new Cell( " " );
                         cell.setColspan(9); 
                         //cell.setRowspan(3); 
                         cell.setBorder(Rectangle.NO_BORDER);
                         tbl.addCell(cell);


                      }


                   } catch (SQLException e){
                      System.err.println(new java.util.Date() + " Replacement Report - Error exec qry on criteria ");
                      displayErrMsg ( res, conn, stmt, "Error on Expense, Error, or Replacement Codes");
                      return;
                   }

                   cell = new Cell( new Phrase("Interment Date", boldFont) );
                   tbl.addCell(cell);

                   cell = new Cell( new Phrase("Insc Appr Date", boldFont) );
                   tbl.addCell(cell);

                   cell = new Cell( new Phrase("Decedent ID", boldFont) );
                   tbl.addCell(cell);

                   cell = new Cell( new Phrase("Last Name", boldFont) );
                   tbl.addCell(cell);

                   cell = new Cell( new Phrase("First Name", boldFont) );
                   tbl.addCell(cell);

                   cell = new Cell( new Phrase("Expense Code", boldFont) );
                   tbl.addCell(cell);

                   cell = new Cell( new Phrase("Error Code", boldFont) );
                   tbl.addCell(cell);

                   cell = new Cell( new Phrase("Replace Code", boldFont) );
                   tbl.addCell(cell);

                   cell = new Cell( new Phrase("Replacement Remarks", boldFont) );
                   tbl.addCell(cell);

                   tbl.endHeaders(); //end the header that will repeat on every page


                      try {
                         rs = stmt.executeQuery (" SELECT to_char(interment_dt,'mm/dd/yyyy'), to_char(inscription_approved_dt,'mm/dd/yyyy'), " +
                                                 " decedent_id, last_nme, first_nme, industry_expense_cd, industry_grp_err_cd, repl_reason_cd, repl_remarks_txt, rownum " +
                                                 " FROM replacement_wrktbl");

                      } catch (SQLException e){
                         System.err.println(new java.util.Date() + " Replacement Report - Error exec qry on temp table " + e.toString());
                         displayErrMsg ( res, conn, stmt, "Error reading replacement report table.");
                         return;
                      }

                      while (rs.next()) {

//System.out.println( rs.getString(10) + ". " + rs.getString(1) + " " + rs.getString(2)+ " " + rs.getString(4));

                         //try {
                         //we have to check for nulls, otherwise will get NPE and report is corrupted
                         // will remove catch, better to display error than wrong data, just in case

                         tbl.addCell(new Cell( new Phrase(Utility.blankIfNull(rs.getString(1)),font) ));
                         tbl.addCell(new Cell( new Phrase(Utility.blankIfNull(rs.getString(2)),font) ));
                         tbl.addCell(new Cell( new Phrase(Utility.blankIfNull(rs.getString(3)),font) ));
                         tbl.addCell(new Cell( new Phrase(Utility.blankIfNull(rs.getString(4)),font) ));
                         tbl.addCell(new Cell( new Phrase(Utility.blankIfNull(rs.getString(5)),font) ));
                         tbl.addCell(new Cell( new Phrase(Utility.blankIfNull(rs.getString(6)),font) ));
                         tbl.addCell(new Cell( new Phrase(Utility.blankIfNull(rs.getString(7)),font) ));
                         tbl.addCell(new Cell( new Phrase(Utility.blankIfNull(rs.getString(8)),font) ));
                         tbl.addCell(new Cell( new Phrase(Utility.blankIfNull(rs.getString(9)),font) ));

                         //} catch (NullPointerException npe) {
                         //  System.out.println ("NPE");
                         //}

                      } 

                      doc.add(tbl);

                      rs.close();
                      stmt.close();
                      conn.close();
                      rs=null;
                      stmt=null;
                      conn=null;

                } catch (SQLException e){
                    System.err.println(new java.util.Date() + " Replacement Report - Error reading temp table " + e.toString());
                    displayErrMsg ( res, conn, stmt, "Error reading replacement report table.");
                    return;
                }  finally {
	                  if (rs != null) {
		                   try { rs.close(); } catch (SQLException e) { ; }
		                   rs = null;
	                  }
	                  if (stmt != null) {
		                   try { stmt.close(); } catch (SQLException e) { ; }
		                   stmt = null;
	                  }
	                  if (conn != null) {
		                   try { conn.close(); } catch (SQLException e) { ; }
		                   conn = null;
	               }
	             }


                //tbl.addCell(new Cell("San Diego"));
                //tbl.addCell(new Cell("California"));


//System.err.println("Writes doc " + new java.util.Date());
 //               doc.add(t);

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

		res.setHeader(
			"Content-disposition",
			sbContentDispValue.toString());

		res.setContentLength(baosPDF.size());

		ServletOutputStream sos;

		sos=res.getOutputStream();
		baosPDF.writeTo(sos);
		sos.flush();

//System.err.println("sos Flush... ");

             baosPDF.reset();

                //PdfWriter.getInstance(doc, new FileOutputStream(filename));
/*
 System.out.println("Opens Doc");
                doc.open();
                //Image img = Image.getInstance("blubetta.jpg"); //(filename);
                //doc.add(img);

                // add some paragraphs to the document
                for (int i = 0; i < 1; i++) {
                   doc.add(new Paragraph("It's " + new java.util.Date()));
                }


                Table t = new Table(2); //num of cols
                tbl.addCell(new Cell("Cem"));
                tbl.addCell(new Cell("count"));
/*
                //doc.add(t);
                while (rs.next()) {
                   tbl.addCell(new Cell(rs.getString(1)));
                   tbl.addCell(new Cell(rs.getString(2)));
                } 

                //tbl.addCell(new Cell("San Diego"));
                //tbl.addCell(new Cell("California"));
//
 System.out.println("Writes doc " + new java.util.Date());
                doc.add(t);


 System.out.println("Closes doc");
             doc.close();
             pdfw.close();
*/

      } catch (DocumentException dex){

                        System.err.println ("PDF Error " + dex.toString());
			res.setContentType("text/html");
			PrintWriter writer = res.getWriter();
			writer.println(
					this.getClass().getName() 
					+ " caught an exception: " 
					+ dex.getClass().getName()
					+ "<br>");
			writer.println("<pre>");
			dex.printStackTrace(writer);
			writer.println("</pre>");

      }    


   } 


   /** 
   * Method to read/write Replacement Orders
   */
   //private static void replaOrder(Connection conn, String date1, String date2, String filename) { 
   private static boolean replaOrder(Connection conn, String cem, String date1, String date2,
                                     String expCdWhere, String errCdWhere, String replCdWhere) {

      Statement stmt=null;
      Statement stmt2=null;
      Statement stmt3=null;
      ResultSet rs=null;
      ResultSet rs2=null;
      String qry="";
      String mngCem = "";
      
      try {

          // Create Query Statement
          //Statement stmt=conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE);  //(did not work)
          stmt=conn.createStatement();

          try {
             //get the managing cemetery:
             qry = " select MANAGING_STATION_NUM from cemetery where num='" + cem + "'";
             rs=stmt.executeQuery(qry);
             if (rs.next()) {
                mngCem = rs.getString(1);
             }
             rs.close();
          }catch(SQLException ex){  
             System.err.println(new java.util.Date() + " Replacement Report - Got error when retrieving MngCemNum for Cem=" + cem + " - qry: " + qry);
             rs.close();
             stmt.close();
             return false;
          }

          //this is only for Natl cems. we will leave the join with cem table to have the same query as the chron's
          qry=" select nvl(c.msn_id,' '), to_char(m.INSCRIPTION_APPROVED_DT, 'mm/dd/yyyy hh24:mi:ss'), " +
              " m.id, " + 
              " m.veteran_id , m.station_num, section_id, nvl(row_num,'0'), site_num, " +
              " m.INDUSTRY_EXPENSE_CD, m.INDUSTRY_GRP_ERR_CD, m.REPL_REASON_CD, m.REPL_REMARKS_TXT, " + 
              " m.status_cd " +
              "from monument_apl m, " + 
              "  cemetery c " + 
              "where " + 
              "  m.station_num = c.num and " +
              //"  c.cemetery_type = 'N' and " +  // removed 8/31/06
              "  m.station_num = '" + mngCem + "' and " +
              "  m.origin_cd='B' and " +
                 expCdWhere + 
                 errCdWhere + 
                 replCdWhere +
              "  m.status_cd in ('IA','MO','MS') and " + 
              "  INSCRIPTION_APPROVED_DT between to_date('" + date1 + "','mm/dd/yyyy') and to_date('" + date2 + "','mm/dd/yyyy')+1 ";

          rs=stmt.executeQuery(qry);

//          try {

	       String decedentID = "";

/*
             fields of rs:
	         c.msn_id (note: this is null for non-natl)
               INSCRIPTION_APPROVED_DT
               m.id
               vet id
               station_num (mng_cem)
               section
               row
               site
               INDUSTRY_EXPENSE_CD, 
               INDUSTRY_GRP_ERR_CD, 
               REPL_REASON_CD, 
               REPL_REMARKS_TXT
               m.status_cd

             fields of rs2:
               cem_num
               d_id
               interment
               last, first, 
               sec, row, site
*/

             while (rs.next()) {
               //System.out.println("Reading " + rs.getString(1));

               stmt2=conn.createStatement();
               stmt3=conn.createStatement();
               String gCem = "";

               try {

		   //get matching decedents using microfilm ID to get the actual cemetery or station
                  rs2=stmt2.executeQuery("select " + 
                      "    g.cemetery_num, g.decedent_id, " + 
		          "    to_char(nvl(g.interment_dt,to_date('01/01/1101', 'mm/dd/yyyy')), 'mm/dd/yyyy'), " + 
                      "    d.last_nme, d.first_nme, section_id, row_num, site_num, " + 
                      "    nvl(g.interment_dt, to_date('01/01/1100', 'mm/dd/yyyy')) " + //for sorting (has to be date format)
                      "from " +
                      "  decedent_monument dm, " +
                      "  decedent_information d, " +
                      "  gravesite_assignment g " +
                      "where " +
                      "  dm.MICROFILM_ID='" + rs.getString(3) + "' and " +
                      "  dm.decedent_id=d.id and " + 
                      "  d.id = g.decedent_id  " +
                  "UNION  select " + 
                      "    g.cemetery_num, g.decedent_id, " + 
		          "    to_char(nvl(g.interment_dt,to_date('01/01/1101', 'mm/dd/yyyy')), 'mm/dd/yyyy'), " + 
                      "    d.last_nme, d.first_nme, section_id, row_num, site_num, " + 
                      "    nvl(g.interment_dt, to_date('01/01/1100', 'mm/dd/yyyy')) " + //for sorting (has to be date format)
                      "from " +
                      "  decedent_monument_history dm, " +
                      "  decedent_information d, " +
                      "  gravesite_assignment g " +
                      "where " +
                      "  dm.MICROFILM_ID='" + rs.getString(3) + "' and " +
                      "  dm.decedent_id=d.id and " + 
                      "  d.id = g.decedent_id  " +
                      "  order by 9 desc"); //force the null interment to go to the bottom, prior has this problem


                  if (rs2.next()) {
                    //found using: uFilm ID

                    gCem = rs2.getString(1);
                    if (gCem.equals(cem)) {

                       try {

                          qry= buildQry_Repla(null,  //overlaid
                                  "'"+rs.getString(1)+"'",   //MSN (null if non-natl)
                                  "'"+rs2.getString(1)+"'",  //Cem
                                  "to_date('" + rs2.getString(3)+ "','mm/dd/yyyy')", //IntermentDt
                                  "to_date('"+rs.getString(2)+"','mm/dd/yyyy hh24:mi:ss')", //InscripDt
                                  "'"+rs.getString(3)+"'",   //Microfilm/monaplID
                                  "'"+rs2.getString(2)+"'",  //DecedentID
                                  "'"+insertExtraTick(rs2.getString(4))+"'",  //lastname
                                  "'"+insertExtraTick(rs2.getString(5))+"'",  //firstname
                                  "'"+rs.getString(9)+"'",   //ExpCd
                                  "'"+rs.getString(10)+"'",  //ErrCd
                                  "'"+rs.getString(11)+"'",  //ReplCd
                                  "'"+insertExtraTick(rs.getString(12))+"'",  //Repl Remarks
                                  "'"+rs.getString(5)+"'",   //MngCem
                                  "'"+rs.getString(13)+"'",  //StatusCd
                                  "'MicrofilmID'");          //Search Method

                          stmt3.executeUpdate(qry);

                       } catch (SQLException e) {
                         System.err.println(new java.util.Date() + " Replacement Report - Error inserting #1 to temp table. qry: " + qry + " " +e.toString());
                         return false;
                       }
                    } 

                  } else {

                    //We need to match by section, row and site, vetID and also check for interment < inscription dates
                    //we are also forcing null interment to be 01/01/1101 so we'll have values for priors
                    
                    rs2.close();

                    rs2=stmt2.executeQuery("select " + 
                        "    g.cemetery_num, g.decedent_id, " + 
		            "    to_char(nvl(g.interment_dt,to_date('01/01/1101', 'mm/dd/yyyy')), 'mm/dd/yyyy'), " + 
                        "    d.last_nme, d.first_nme, section_id, row_num, site_num " + 
                        "from " +
                        "  decedent_information d, " +
                        "  gravesite_assignment g " +
                        "where " +
                        "  d.id = g.decedent_id and       " +
                        "  d.veteran_id='" + rs.getString(4) + "' and " + 
                        "  g.section_id='" + rs.getString(6) + "' and " +
                        "  g.row_num=   '" + rs.getString(7) + "' and " +
                        "  g.site_num=  '" + rs.getString(8) + "' and " +     //added section, row and site bec a decedent could move to other cem
                        "  nvl(g.interment_dt,to_date('01/01/1101','mm/dd/yyyy')) <=  " + 
                        "      to_date('" + rs.getString(2) + "','mm/dd/yyyy hh24:mi:ss') " + 
                        "  order by nvl(g.interment_dt, to_date('01/01/1100', 'mm/dd/yyyy')) desc"); //force the null interment to go to the bottom, prior has this problem

                    if (rs2.next()) {
                       //found by: SecRowSiteVetIDInterment

                       gCem = rs2.getString(1);
                       if (gCem.equals(cem)) {

                          try {

                             qry= buildQry_Repla("'*'",  //overlaid
                                  "'"+rs.getString(1)+"'",   //MSN
                                  "'"+rs2.getString(1)+"'",  //Cem
                                  "to_date('" + rs2.getString(3)+ "','mm/dd/yyyy')", //IntermentDt
                                  "to_date('"+rs.getString(2)+"','mm/dd/yyyy hh24:mi:ss')", //InscripDt
                                  "'"+rs.getString(3)+"'",   //Microfilm/monaplID
                                  "'"+rs2.getString(2)+"'",  //DecedentID
                                  "'"+insertExtraTick(rs2.getString(4))+"'",  //lastname
                                  "'"+insertExtraTick(rs2.getString(5))+"'",  //firstname
                                  "'"+rs.getString(9)+"'",   //ExpCd
                                  "'"+rs.getString(10)+"'",  //ErrCd
                                  "'"+rs.getString(11)+"'",  //ReplCd
                                  "'"+insertExtraTick(rs.getString(12))+"'",  //Repl Remarks
                                  "'"+rs.getString(5)+"'",   //MngCem
                                  "'"+rs.getString(13)+"'",  //StatusCd
                                  "'SecRowSiteVetIDInterment'");          //Search Method

                             stmt3.executeUpdate(qry);

                          } catch (SQLException e) {
                             System.err.println(new java.util.Date() + " Replacement Report - Error inserting #2 to temp table. qry: " + qry + " " +e.toString());
                             return false;
                          }
                       }

                    } else {

                      //System.out.println ("will use 'like VetID' in decedent_monument MonID=" + rs.getString(3));

                      rs2.close();

                      rs2=stmt2.executeQuery("select " + 
                          "    g.cemetery_num, g.decedent_id, " + 
		              "    to_char(nvl(g.interment_dt,to_date('01/01/1101', 'mm/dd/yyyy')), 'mm/dd/yyyy'), " + 
                          "    d.last_nme, d.first_nme, section_id, row_num, site_num " + 
                          "from decedent_monument dm, " + 
                          "  decedent_information d, " +
                          "  gravesite_assignment g " +
                          "where dm.veteran_id='" + rs.getString(4) + "' and " +
                          "  dm.remarks_txt like '%" +  rs.getString(3) + "%'  and " + //monument id
                          "  d.id = dm.decedent_id and " +
                          "  d.id = g.decedent_id " + 
                          "  order by nvl(g.interment_dt, to_date('01/01/1100', 'mm/dd/yyyy')) desc"); //force the null interment to go to the bottom, prior has this problem

                      if (rs2.next()) {
                         //found by: RemarksLikeMonID-VetID

                         gCem = rs2.getString(1);
                         if (gCem.equals(cem)) {

                            try {

                               qry= buildQry_Repla("'*'",  //overlaid
                                  "'"+rs.getString(1)+"'",   //MSN
                                  "'"+rs2.getString(1)+"'",  //Cem
                                  "to_date('" + rs2.getString(3)+ "','mm/dd/yyyy')", //IntermentDt
                                  "to_date('"+rs.getString(2)+"','mm/dd/yyyy hh24:mi:ss')", //InscripDt
                                  "'"+rs.getString(3)+"'",   //Microfilm/monaplID
                                  "'"+rs2.getString(2)+"'",  //DecedentID
                                  "'"+insertExtraTick(rs2.getString(4))+"'",  //lastname
                                  "'"+insertExtraTick(rs2.getString(5))+"'",  //firstname
                                  "'"+rs.getString(9)+"'",   //ExpCd
                                  "'"+rs.getString(10)+"'",  //ErrCd
                                  "'"+rs.getString(11)+"'",  //ReplCd
                                  "'"+insertExtraTick(rs.getString(12))+"'",  //Repl Remarks
                                  "'"+rs.getString(5)+"'",   //MngCem
                                  "'"+rs.getString(13)+"'",  //StatusCd
                                  "'RemarksLikeMonID-VetID'");          //Search Method

                               stmt3.executeUpdate(qry);

                            } catch (SQLException e) {
                               System.err.println(new java.util.Date() + " Replacement Report - Error inserting #3 to temp table. qry: " + qry + " " +e.toString());
                               return false;
                            }
                         }

                      } else { //didn't find a matching gravesite record

                        //System.out.println ("will use 'like' MngCem in decedent_monument MonID=" + rs.getString(3));

                        rs2.close();

                        rs2=stmt2.executeQuery("select " + 
                            "    g.cemetery_num, g.decedent_id, " + 
		                "    to_char(nvl(g.interment_dt,to_date('01/01/1101', 'mm/dd/yyyy')), 'mm/dd/yyyy'), " + 
                            "    d.last_nme, d.first_nme, section_id, row_num, site_num " + 
                            "from decedent_monument dm, " + 
                            "  decedent_information d, " +
                            "  gravesite_assignment g " +
                            "where dm.MANAGING_CEMETERY_NUM='" + rs.getString(5) + "' and " +
                            "  dm.remarks_txt like '%" +  rs.getString(3) + "%'  and " + //monument id
                            "  d.id = dm.decedent_id and " +
                            "  d.id = g.decedent_id " + 
                            "  order by nvl(g.interment_dt, to_date('01/01/1100', 'mm/dd/yyyy')) desc"); //force the null interment to go to the bottom, prior has this problem

                        if (rs2.next()) {
                          //found by: RemarksLikeMonID-MngCem

                          gCem = rs2.getString(1);
                          if (gCem.equals(cem)) {

                             try {

                                qry= buildQry_Repla("'*'",  //overlaid
                                     "'"+rs.getString(1)+"'",   //MSN
                                     "'"+rs2.getString(1)+"'",  //Cem
                                     "to_date('" + rs2.getString(3)+ "','mm/dd/yyyy')", //IntermentDt
                                     "to_date('"+rs.getString(2)+"','mm/dd/yyyy hh24:mi:ss')", //InscripDt
                                     "'"+rs.getString(3)+"'",   //Microfilm/monaplID
                                     "'"+rs2.getString(2)+"'",  //DecedentID
                                     "'"+insertExtraTick(rs2.getString(4))+"'",  //lastname
                                     "'"+insertExtraTick(rs2.getString(5))+"'",  //firstname
                                     "'"+rs.getString(9)+"'",   //ExpCd
                                     "'"+rs.getString(10)+"'",  //ErrCd
                                     "'"+rs.getString(11)+"'",  //ReplCd
                                     "'"+insertExtraTick(rs.getString(12))+"'",  //Repl Remarks
                                     "'"+rs.getString(5)+"'",   //MngCem
                                     "'"+rs.getString(13)+"'",  //StatusCd
                                     "'RemarksLikeMonID-MngCem'");          //Search Method
  
                                stmt3.executeUpdate(qry);

                             } catch (SQLException e) {
                                  System.err.println(new java.util.Date() + " Replacement Report - Error inserting #4 to temp table. qry: " + qry + " " +e.toString());
                                  return false;
                             }
                          }

                        } else { //didn't find a matching gravesite record

                          //System.out.println ("will use sec,row,site,vetid but no interment date");

                          //remove chk on inscription/interment dates
                          rs2.close();

                          rs2=stmt2.executeQuery("select " + 
                              "    g.cemetery_num, g.decedent_id, " + 
		              "    to_char(nvl(g.interment_dt,to_date('01/01/1101', 'mm/dd/yyyy')), 'mm/dd/yyyy'), " + 
                              "    d.last_nme, d.first_nme, section_id, row_num, site_num " + 
                              "from " +
                              "  decedent_information d, " +
                              "  gravesite_assignment g " +
                              "where " +
                              "  d.id = g.decedent_id and       " +
                              "  d.veteran_id='" + rs.getString(4) + "' and " + 
                              "  g.section_id='" + rs.getString(6) + "' and " +
                              "  g.row_num=   '" + rs.getString(7) + "' and " +
                              "  g.site_num=  '" + rs.getString(8) + "' " +     //added section, row and site bec a decedent could move to other cem
                              "  order by nvl(g.interment_dt, to_date('01/01/1100', 'mm/dd/yyyy')) desc"); //force the null interment to go to the bottom, prior has this problem

                          if (rs2.next()) {
                             //found by: SecRowSiteVetIDNoInter

                             gCem = rs2.getString(1);
                             if (gCem.equals(cem)) {

                             try {

                                   qry= buildQry_Repla("'*'",  //overlaid
                                     "'"+rs.getString(1)+"'",   //MSN
                                     "'"+rs2.getString(1)+"'",  //Cem
                                     "to_date('" + rs2.getString(3)+ "','mm/dd/yyyy')", //IntermentDt
                                     "to_date('"+rs.getString(2)+"','mm/dd/yyyy hh24:mi:ss')", //InscripDt
                                     "'"+rs.getString(3)+"'",   //Microfilm/monaplID
                                     "'"+rs2.getString(2)+"'",  //DecedentID
                                     "'"+insertExtraTick(rs2.getString(4))+"'",  //lastname
                                     "'"+insertExtraTick(rs2.getString(5))+"'",  //firstname
                                     "'"+rs.getString(9)+"'",   //ExpCd
                                     "'"+rs.getString(10)+"'",  //ErrCd
                                     "'"+rs.getString(11)+"'",  //ReplCd
                                     "'"+insertExtraTick(rs.getString(12))+"'",  //Repl Remarks
                                     "'"+rs.getString(5)+"'",   //MngCem
                                     "'"+rs.getString(13)+"'",  //StatusCd
                                     "'SecRowSiteVetIDNoInter'");          //Search Method

                                   stmt3.executeUpdate(qry);

                                } catch (SQLException e) {
                                     System.err.println(new java.util.Date() + " Replacement Report - Error inserting #5 to temp table. qry: " + qry + " " +e.toString());
                                     return false;
                                }
                             }

                          } else { //didn't find a matching gravesite record

                            //System.out.println ("still did not find a match, will use even slower like ");

                            rs2.close();
                            stmt2.close();
                            stmt2=conn.createStatement();
                            rs2=stmt2.executeQuery("select " + 
                                "    g.cemetery_num, g.decedent_id, " + 
		                "    to_char(nvl(g.interment_dt,to_date('01/01/1101', 'mm/dd/yyyy')), 'mm/dd/yyyy'), " + 
                                "    d.last_nme, d.first_nme, section_id, row_num, site_num " + 
                                "from decedent_monument dm, " + 
                                "  decedent_information d, " +
                                "  gravesite_assignment g " +
                                "where " +
                                "  dm.remarks_txt like '%" +  rs.getString(3) + "%'  and " + //monument id
                                "  d.id = dm.decedent_id and " +
                                "  d.id = g.decedent_id " +
                                "  order by nvl(g.interment_dt, to_date('01/01/1100', 'mm/dd/yyyy')) desc"); //force the null interment to go to the bottom, prior has this problem


                            if (rs2.next()) {
                                //found by: RemarksLikeMonID

                                gCem = rs2.getString(1);
                                if (gCem.equals(cem)) {

                                   try {

                                      qry= buildQry_Repla("'*'",  //overlaid
                                  "'"+rs.getString(1)+"'",   //MSN
                                  "'"+rs2.getString(1)+"'",  //Cem
                                  "to_date('" + rs2.getString(3)+ "','mm/dd/yyyy')", //IntermentDt
                                  "to_date('"+rs.getString(2)+"','mm/dd/yyyy hh24:mi:ss')", //InscripDt
                                  "'"+rs.getString(3)+"'",   //Microfilm/monaplID
                                  "'"+rs2.getString(2)+"'",  //DecedentID
                                  "'"+insertExtraTick(rs2.getString(4))+"'",  //lastname
                                  "'"+insertExtraTick(rs2.getString(5))+"'",  //firstname
                                  "'"+rs.getString(9)+"'",   //ExpCd
                                  "'"+rs.getString(10)+"'",  //ErrCd
                                  "'"+rs.getString(11)+"'",  //ReplCd
                                  "'"+insertExtraTick(rs.getString(12))+"'",  //Repl Remarks
                                  "'"+rs.getString(5)+"'",   //MngCem
                                  "'"+rs.getString(13)+"'",  //StatusCd
                                  "'RemarksLikeMonID'");     //Search Method

                                      stmt3.executeUpdate(qry);

                                   } catch (SQLException e) {
                                       System.err.println(new java.util.Date() + " Replacement Report - Error inserting #6 to temp table. qry: " + qry + " " +e.toString());
                                       return false;
                                   }
                                }


                            } else { //didn't find a matching gravesite record

                              //System.out.println ("will remove SecRowSite, will use VetID only ");

		              //remove check on section, row and site

                              rs2.close();

                              rs2=stmt2.executeQuery("select " + 
                                  "    g.cemetery_num, g.decedent_id, " + 
		                  "    to_char(nvl(g.interment_dt,to_date('01/01/1101', 'mm/dd/yyyy')), 'mm/dd/yyyy'), " + 
                                  "    d.last_nme, d.first_nme, section_id, row_num, site_num " + 
                                  "from " +
                                  "  decedent_information d, " +
                                  "  gravesite_assignment g " +
                                  "where " +
                                  "  d.id = g.decedent_id and       " +
                                  "  d.veteran_id='" + rs.getString(4) + "' " + 
                                  "  order by nvl(g.interment_dt, to_date('01/01/1100', 'mm/dd/yyyy')) desc"); //force the null interment to go to the bottom, prior has this problem

                              if (rs2.next()) {
                                //found by: VetID only

                                gCem = rs2.getString(1);
                                if (gCem.equals(cem)) {


                                   try {

                                      qry= buildQry_Repla("'*'",  //overlaid
                                        "'"+rs.getString(1)+"'",   //MSN
                                        "'"+rs2.getString(1)+"'",  //Cem
                                        "to_date('" + rs2.getString(3)+ "','mm/dd/yyyy')", //IntermentDt
                                        "to_date('"+rs.getString(2)+"','mm/dd/yyyy hh24:mi:ss')", //InscripDt
                                        "'"+rs.getString(3)+"'",   //Microfilm/monaplID
                                        "'"+rs2.getString(2)+"'",  //DecedentID
                                        "'"+insertExtraTick(rs2.getString(4))+"'",  //lastname
                                        "'"+insertExtraTick(rs2.getString(5))+"'",  //firstname
                                        "'"+rs.getString(9)+"'",   //ExpCd
                                        "'"+rs.getString(10)+"'",  //ErrCd
                                        "'"+rs.getString(11)+"'",  //ReplCd
                                        "'"+insertExtraTick(rs.getString(12))+"'",  //Repl Remarks
                                        "'"+rs.getString(5)+"'",   //MngCem
                                        "'"+rs.getString(13)+"'",  //StatusCd
                                        "'VetID only'");     //Search Method

                                      stmt3.executeUpdate(qry);

                                   } catch (SQLException e) {
                                       System.err.println(new java.util.Date() + " Replacement Report - Error inserting #7 to temp table. qry: " + qry + " " +e.toString());
                                       return false;
                                   }
                                }

                              } else { //will try a cem, sec, row, site and interment

                                //System.out.println ("last resort cem, sec,row,site, interment");

                                rs2.close();

                                rs2=stmt2.executeQuery("select " + 
                                    "    g.cemetery_num, g.decedent_id, " + 
		                    "    to_char(nvl(g.interment_dt,to_date('01/01/1101', 'mm/dd/yyyy')), 'mm/dd/yyyy'), " + 
                                    "    d.last_nme, d.first_nme, section_id, row_num, site_num " + 
                                    "from " +
                                    "  decedent_information d, " +
                                    "  gravesite_assignment g " +
                                    "where " +
                                    "  d.id = g.decedent_id and " +
                                    "  d.MANAGING_CEMETERY_NUM = '" + rs.getString(5) + "' and " +
                                    "  g.section_id='" + rs.getString(6) + "' and " +
                                    "  g.row_num=   '" + rs.getString(7) + "' and " +
                                    "  g.site_num=  '" + rs.getString(8) + "' and " +     //added section, row and site bec a decedent could move to other cem
                                    "  nvl(g.interment_dt,to_date('01/01/1101','mm/dd/yyyy')) <=  " + 
                                    "      to_date('" + rs.getString(2) + "','mm/dd/yyyy hh24:mi:ss') " + 
                                    "  order by nvl(g.interment_dt, to_date('01/01/1100', 'mm/dd/yyyy')) desc"); //force the null interment to go to the bottom, prior has this problem

                                 if (rs2.next()) {
                                   //found by: SecRowSiteMngCemInterment

                                   gCem = rs2.getString(1);
                                   if (gCem.equals(cem)) {

                                      try {

                                        qry= buildQry_Repla("'*'",  //overlaid
                                           "'"+rs.getString(1)+"'",   //MSN
                                           "'"+rs2.getString(1)+"'",  //Cem
                                           "to_date('" + rs2.getString(3)+ "','mm/dd/yyyy')", //IntermentDt
                                           "to_date('"+rs.getString(2)+"','mm/dd/yyyy hh24:mi:ss')", //InscripDt
                                           "'"+rs.getString(3)+"'",   //Microfilm/monaplID
                                           "'"+rs2.getString(2)+"'",  //DecedentID
                                           "'"+insertExtraTick(rs2.getString(4))+"'",  //lastname
                                           "'"+insertExtraTick(rs2.getString(5))+"'",  //firstname
                                           "'"+rs.getString(9)+"'",   //ExpCd
                                           "'"+rs.getString(10)+"'",  //ErrCd
                                           "'"+rs.getString(11)+"'",  //ReplCd
                                           "'"+insertExtraTick(rs.getString(12))+"'",  //Repl Remarks
                                           "'"+rs.getString(5)+"'",   //MngCem
                                           "'"+rs.getString(13)+"'",  //StatusCd
                                           "'SecRowSiteMngCemInterment'");     //Search Method

                                        stmt3.executeUpdate(qry);

                                      } catch (SQLException e) {
                                       System.err.println(new java.util.Date() + " Replacement Report - Error inserting #8 to temp table. qry: " + qry + " " +e.toString());
                                       return false;
                                      }
                                   }

                                 } else { //can never find a matching gravesite record


                                   //System.out.println ("NEVER found a match " + rs.getString(3));

                                    try {

                                      qry= buildQry_Repla("'*'",  //overlaid
                                           "'"+rs.getString(1)+"'",   //MSN
                                           "null",  //Cem
                                           "null", //IntermentDt
                                           "to_date('"+rs.getString(2)+"','mm/dd/yyyy hh24:mi:ss')", //InscripDt
                                           "'"+rs.getString(3)+"'",   //Microfilm/monaplID
                                           "null",  //DecedentID
                                           "null",  //lastname
                                           "null",  //firstname
                                           "'"+rs.getString(9)+"'",   //ExpCd
                                           "'"+rs.getString(10)+"'",  //ErrCd
                                           "'"+rs.getString(11)+"'",  //ReplCd
                                           "'"+insertExtraTick(rs.getString(12))+"'",  //Repl Remarks
                                           "'"+rs.getString(5)+"'",   //MngCem
                                           "'"+rs.getString(13)+"'",  //StatusCd
                                           "'No Match'");     //Search Method

                                      stmt3.executeUpdate(qry);

                                   } catch (SQLException e) {
                                     System.err.println ("#9 Error inserting record " + e.toString());
                                     System.err.println (qry);
                                     return false;
                                   }


                                 }//end if rs2
                              }//end if rs2
                            }//end if rs2
                         }//end if rs2
                      }//end if rs2
                    }//end if rs2
                  }//end if rs2
                }//end if rs2
                rs2.close();
                stmt2.close();

               } catch (SQLException e) {
                   System.err.println (new java.util.Date() + " Replacement Report - Error decedent id is null " + e.toString());
                   return false;
               }

               stmt3.close();

             } //end while

         rs.close();

         /* test
         rs=stmt.executeQuery("Select count(*) from replacement_wrktbl");
         if (rs.next()) 
            System.err.print ("count=" + rs.getInt(1) + " <");
         rs.close();
         */

         stmt.close();

         return true;

      } catch (SQLException e){
          System.err.println (new java.util.Date() + " Replacement Report - General Error of replaOrder method " + e.toString());
          return false;
      }

   } //replaOrder

   /** 
   * Method to build the insert query for Replacements
   */
   private static String buildQry_Repla(String v0, String v1, String v2, String v3, 
                                  String v4, String v5, String v6, String v7,
                                  String v8, String v9, String vA, String vB,
                                  String vC, String vD, String vE, String vF) {

      //String qry = "insert into replacement_wrktbl ( " + 
      String qry = "insert into replacement_wrktbl ( " + 
                "overlay_ind,"+
                "msn_id,"+
                "cemetery_num,"+
                "Interment_dt,"+
                "Inscription_Approved_dt,"+
                "Microfilm_id,"+
                "decedent_id,"+
                "last_nme,"+
                "first_nme,"+
                "industry_expense_cd,"+
                "industry_grp_err_cd,"+
                "repl_reason_cd,"+
                "repl_remarks_txt,"+
                "managing_cemetery_num,"+
                "status_cd,"+
                "search_method)"+
            "values ("+
                v0+","+
                v1+","+
                v2+","+
                v3+","+
                v4+","+
                v5+","+
                v6+","+
                v7+","+
                v8+","+
                v9+","+
                vA+","+
                vB+","+
                vC+","+
                vD+","+
                vE+","+
                vF+ ")";

      return (qry); 
   } 

   /** 
   * Method to insert two ticks if there is one tick
   */
   private static String insertExtraTick(String s) { 
      /* Note: Two String variable types cannot be compared with "==" operator,
         you have to use object.equals(object2)
            System.out.println("Example"); 
            String srchString = "Y";
            String x          = "Y";
            System.out.println(x + "==" + srchString + " " + (x == srchString)); //returns false, always
            System.out.println(x + " equals " + srchString + " " + (x.equals(srchString))); //returns true if values are equal
         "equals" implements "value comparison", while "==" implements "handle comparison" 
      
      "substring" notes
         System.out.println(" i=0 " + s.substring(0,1));  (returns 1st letter)
         System.out.println(" i=1 " + s.substring(1,2));  (returns 2nd letter) 
         System.out.println(" i=2 " + s.substring(2,3));  (returns 3rd letter, etc)
      */

      String newString = new String();
      for (int i=0; i<s.length(); i++ )  {
           if (s.substring(i,i+1).equals("'"))     //if it is tick, insert extra tick
              newString = newString + "''";  
           else
           if (s.substring(i,i+1).equals("´"))  //it is a tick look-alike, insert 2 ticks
              newString = newString + "''"; 
           else
              newString = newString + s.substring(i,i+1);
      }

      return (newString); 
   } 

  public static void displayErrMsg ( HttpServletResponse p_response, Connection myConn, Statement myStmt, String errMsg) 
    throws IOException {

     ServletOutputStream l_out=p_response.getOutputStream();

     l_out.println(" <HTML> ");
     l_out.println(" <HEAD> ");
     l_out.println(" <TITLE> ");
     l_out.println(" Replacement Report ");
     l_out.println(" </TITLE> ");
     l_out.println(" <BODY> ");
     l_out.println(" Replacement Report<p><p>");
     l_out.println(errMsg);
     l_out.println(" </BODY> ");
     l_out.println(" </HTML> ");
     l_out.println(" </HEAD> ");


     try {
        if (myStmt != null) {
            myStmt.close();
            myStmt = null;
        }
        if (myConn != null) {
            myConn.close();
            myConn = null;
        }
     } catch (SQLException e) {
         System.err.println ("Error closing connection or statement " + e.toString());
     }

  } //end displayErrMsg


   public void destroy(Connection conn) { 
 
      try {  
         conn.close(); 
      } 
      catch (Exception ignored) {System.err.println("Replacement Report - Error in method destroy"); }  
   } 

}
