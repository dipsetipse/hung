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
import javax.naming.InitialContext;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import javax.sql.DataSource;

import java.awt.Color;

/*
 * 
June 2011, fixed the problem with data not displaying "to" date (CCR837) and managed sites not displaying (CCR834)
added "+ 0.9999884" in to-from date

Aug 2011, fixed the problem in computing the date differences. On June 2011, changed the logic to "round"
e.g., "round(m.order_release_dt - g.interment_dt) int2rel_dd,"
however the above resulted to plus one day, so the cemetery appears to be late.
Changed back to "(to_date(m.order_release_dt, 'DD-MON-YY') - to_date(g.interment_dt, 'DD-MON-YY'))"

Notes:
interment, received, set dates have no time component
amas_order_dt, order_release date have time component


converted from mon_time_rpt (C program). Below is the C program SQL
  Arguments : id pw@db CEM_NUM  start_int_dt end_int_dt output_file

select decedent_id, to_char (interment_dt, 'MM/DD/YYYY'),
  cemetery_num, section_id, row_num, site_num
  from gravesite_assignment
  where cemetery_num = :num
  and interment_dt >= to_date (:start_interment_dt, 'MM/DD/YYYY')
  and interment_dt <= to_date (:end_interment_dt, 'MM/DD/YYYY')
  and disinterment_dt is null 
  order by cemetery_num, interment_dt;


while not EOF
  if (chk_dec () == YES)
     process_sub_interment ();
     process_mon (); 
     process_dates (); 
  end if
end


chk_dec ()
select first_nme, substr (last_nme, 1, 24)
  into :first_nme, last_nme 
  from decedent_information
  where id = :dec_id and 
  case_status_cd in ('CE','EL','AU');


process_sub_interment ()  

  select decedent_id
  from gravesite_assignment
  where cemetery_num = :num and
  section_id = :section_id and
  row_num = :row_num and
  site_num = :site_num and
  decedent_id != :dec_id and
  interment_dt is not NULL and
  to_date (:interment_dt, 'MM/DD/YYYY') < interment_dt and
  disinterment_dt is NULL;

If there is sunbinterment:
   select '*' into :sub_ind
    from decedent_information
    where id = :sub_dec_id
    and case_status_cd in ('CE','EL','AU');

process_mon ()
select monument_id into :mon_id
  from decedent_monument where decedent_id = :dec_id;

select to_char (order_release_dt, 'MM/DD/YYYY'),
  to_char (amas_order_dt, 'MM/DD/YYYY'),
  to_char (received_dt, 'MM/DD/YYYY'),
  to_char (set_dt, 'MM/DD/YYYY'),
  monument_type_cd, repl_reason_cd,
  shipment_fy, ship_prefix, cbl_num
  into :order_release_dt, :amas_order_dt, :received_dt, 
  :set_dt, :monument_type_cd, :repl_reason_cd,
  :shipment_fy, :ship_prefix, :cbl_num
  from monument
  where id = :mon_id;mon_time_rpt (c program)
  Arguments : id pw@db CEM_NUM  start_int_dt end_int_dt output_file

select decedent_id, to_char (interment_dt, 'MM/DD/YYYY'),
  cemetery_num, section_id, row_num, site_num
  from gravesite_assignment
  where cemetery_num = :num
  and interment_dt >= to_date (:start_interment_dt, 'MM/DD/YYYY')
  and interment_dt <= to_date (:end_interment_dt, 'MM/DD/YYYY')
  and disinterment_dt is null 
  order by cemetery_num, interment_dt;


while not EOF
  if (chk_dec () == YES)
     process_sub_interment ();
     process_mon (); 
     process_dates (); 
  end if
end


chk_dec ()
select first_nme, substr (last_nme, 1, 24)
  into :first_nme, last_nme 
  from decedent_information
  where id = :dec_id and 
  case_status_cd in ('CE','EL','AU');


process_sub_interment ()  

  select decedent_id
  from gravesite_assignment
  where cemetery_num = :num and
  section_id = :section_id and
  row_num = :row_num and
  site_num = :site_num and
  decedent_id != :dec_id and
  interment_dt is not NULL and
  to_date (:interment_dt, 'MM/DD/YYYY') < interment_dt and
  disinterment_dt is NULL;

If there is sunbinterment:
   select '*' into :sub_ind
    from decedent_information
    where id = :sub_dec_id
    and case_status_cd in ('CE','EL','AU');

process_mon ()
select monument_id into :mon_id
  from decedent_monument where decedent_id = :dec_id;

select to_char (order_release_dt, 'MM/DD/YYYY'),
  to_char (amas_order_dt, 'MM/DD/YYYY'),
  to_char (received_dt, 'MM/DD/YYYY'),
  to_char (set_dt, 'MM/DD/YYYY'),
  monument_type_cd, repl_reason_cd,
  shipment_fy, ship_prefix, cbl_num
  into :order_release_dt, :amas_order_dt, :received_dt, 
  :set_dt, :monument_type_cd, :repl_reason_cd,
  :shipment_fy, :ship_prefix, :cbl_num
  from monument
  where id = :mon_id; 

process_dates ()
    select to_date (:order_release_dt, 'MM/DD/YYYY') -
     to_date (:interment_dt, 'MM/DD/YYYY') into :int2rel_dd from dual;

    select to_date (:amas_order_dt, 'MM/DD/YYYY') -
     to_date (:order_release_dt, 'MM/DD/YYYY') into :rel2ord_dd from dual;

    select to_date (:received_dt, 'MM/DD/YYYY') -
     to_date (:amas_order_dt, 'MM/DD/YYYY') into :ord2rec_dd from dual;

    select to_date (:set_dt, 'MM/DD/YYYY') -
     to_date (:received_dt, 'MM/DD/YYYY') into :rec2set_dd from dual;

    select to_date (:set_dt, 'MM/DD/YYYY') -
     to_date (:interment_dt, 'MM/DD/YYYY') into :int2set_dd from dual;



/* To test:
 * encrypted d901frm for PC use
 * http://localhost:8888/bossreport/monordertimelinessxls?u=62574dd8f22ae9c0eb4330229da7bf6459837255183e214f&cem_num=901&start_dt=01/01/2006&end_dt=01/01/2007
 * 
 */

public class MonumentOrderTimelinessxls extends HttpServlet 
{
  private static final String CONTENT_TYPE = "application/vnd.ms-excel"; // for MSWord: application/msword, "text/html; charset=windows-1252";

  private DataSource ods = null;

  public void init(ServletConfig config) throws ServletException
  {
    super.init(config);

    // using Data Source Name
    String DATASOURCE = "jdbc/OracleDS";

    try {
        InitialContext ic = new InitialContext();
        ods = (DataSource)ic.lookup(DATASOURCE);
    } catch (javax.naming.NamingException e) {
        System.out.println("Production Datasource error - javax.naming.NamingException " + 
                           e.toString());
    }
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
        String startDT = req.getParameter("start_dt");
        String endDT = req.getParameter("end_dt");
        String cemNme = "";
           
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

           String qry = "select nme from cemetery where num = ?";
            
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

              rs = null;
              pstmt = null; 
            }catch (SQLException e) {
                System.err.println(new java.util.Date() + 
                                   " Error retrieving data " + "\n" + qry );
            }
/*            
            qry = "select to_char(g.interment_dt, 'MM/DD/YYYY'), " +
                  "g.decedent_id, " + 
                  "d.first_nme, " + 
                  "d.last_nme, " +
                  "m.monument_type_cd, " + 
                  "m.repl_reason_cd, " +
                  "m.shipment_fy||' '||m.ship_prefix||' '||m.cbl_num, " +

                  "to_date(m.order_release_dt, 'DD-MON-YY') - to_date(g.interment_dt, 'DD-MON-YY') int2rel_dd, " +
                  "to_date(m.amas_order_dt, 'DD-MON-YY') - to_date(m.order_release_dt, 'DD-MON-YY') rel2ord_dd, " +
                  "to_date(m.received_dt, 'DD-MON-YY') - to_date(m.amas_order_dt, 'DD-MON-YY') ord2rec_dd, " +
                  "to_date(m.set_dt, 'DD-MON-YY') - to_date(m.received_dt, 'DD-MON-YY') rec2set_dd, " +
                  "to_date(m.set_dt, 'DD-MON-YY') - to_date(g.interment_dt, 'DD-MON-YY') int2set_dd, " +

                  "g.section_id, " + //13
                  "g.row_num, " + //14
                  "g.site_num " + //15

                  "from gravesite_assignment g, " +
                  "  decedent_information d, " +
                  "  monument m, " + 
                  "  decedent_monument dm " +
                  "where g.cemetery_num = ? and " +
                  "  g.interment_dt between to_date( ?, 'MM/DD/YYYY') and to_date( ?, 'MM/DD/YYYY') + 0.9999884 and " + //cannot use + 1 b/c it goes to next day if time is 00:00:00
                  //"  g.interment_dt >= to_date(?, 'MM/DD/YYYY') and " +
                  //"  g.interment_dt <= to_date(?, 'MM/DD/YYYY') " + 
                  "  g.disinterment_dt is NULL and " +
                  "  g.decedent_id = d.id and " +
                  "  d.id = dm.decedent_id (+) and " +
                  "  dm.monument_id = m.id (+) and " +
                  "  d.case_status_cd in ('CE', 'EL', 'AU') " +
                  "order by g.cemetery_num, g.interment_dt";
*/
        qry = "select to_char(g.interment_dt, 'MM/DD/YYYY'), " +
                  "g.decedent_id, " + 
                  "d.first_nme, " + 
                  "d.last_nme, " +
                  "m.monument_type_cd, " + 
                  "m.repl_reason_cd, " +
                  "m.shipment_fy||' '||m.ship_prefix||' '||m.cbl_num, " +
                  "to_date(m.order_release_dt, 'DD-MON-YYYY') - to_date(g.interment_dt, 'DD-MON-YYYY') int2rel_dd, " +
                  "to_date(m.amas_order_dt, 'DD-MON-YYYY') - to_date(m.order_release_dt, 'DD-MON-YYYY') rel2ord_dd, " +
                  "to_date(m.received_dt, 'DD-MON-YYYY') - to_date(m.amas_order_dt, 'DD-MON-YYYY') ord2rec_dd, " +
                  "to_date(m.set_dt, 'DD-MON-YYYY') - to_date(m.received_dt, 'DD-MON-YYYY') rec2set_dd, " +
                  "to_date(m.set_dt, 'DD-MON-YYYY') - to_date(g.interment_dt, 'DD-MON-YYYY') int2set_dd, " +
                /*
                  "round(m.order_release_dt) - round(g.interment_dt) int2rel_dd, " +
                  "round(m.amas_order_dt) - round(m.order_release_dt) rel2ord_dd, " +
                  "round(m.received_dt) - round(m.amas_order_dt) ord2rec_dd, " +
                  "round(m.set_dt) - round(m.received_dt) rec2set_dd, " +
                  "round(m.set_dt) - round(g.interment_dt) int2set_dd, " +       
                 */
                  "g.section_id, " + //13
                  "g.row_num, " + //14
                  "g.site_num, " + //15  
                  "g.interment_dt " +
                  "from gravesite_assignment g, " +
                  "  decedent_information d, " +
                  "  monument m, " + 
                  "  decedent_monument dm " +
                  " where g.cemetery_num = ? and " +
                  "  g.interment_dt between to_date( ?, 'MM/DD/YYYY') and to_date( ?, 'MM/DD/YYYY') + 0.9999884 and " + 
                  "  g.disinterment_dt is NULL and " +
                  "  g.decedent_id = d.id and " +
                  "  d.id = dm.decedent_id (+) and " +
                  "  dm.monument_id = m.id (+) and " +
                  "  d.case_status_cd in ('CE', 'EL', 'AU') " +
                  "  and m.repl_reason_cd is null " +
                  " and d.id not in " +
                  " (select decedent_id from decedent_monument_history dmh, monument_history mh " +
                  " where dmh.monument_id = mh.id " +
                  " and dmh.insert_dt > to_date( ?, 'MM/DD/YYYY') " + 
                  // " and dmh.insert_dt > g.interment_dt " + 
                  " and mh.amas_order_dt is not null )" +
                  " UNION  " +  // replaced
                  " select to_char(ga.interment_dt, 'MM/DD/YYYY'), " +
                  " ga.decedent_id,  " +
                  " da.first_nme,  " +
                  " da.last_nme,  " +
                  " ma.monument_type_cd,  " +
                  " ma.repl_reason_cd, " +
                  " ma.shipment_fy||' '||ma.ship_prefix||' '||ma.cbl_num, " +
                  "to_date(ma.order_release_dt, 'DD-MON-YYYY') - to_date(ga.interment_dt, 'DD-MON-YYYY') int2rel_dd, " +
                  "to_date(ma.amas_order_dt, 'DD-MON-YYYY') - to_date(ma.order_release_dt, 'DD-MON-YYYY') rel2ord_dd, " +
                  "to_date(ma.received_dt, 'DD-MON-YYYY') - to_date(ma.amas_order_dt, 'DD-MON-YYYY') ord2rec_dd, " +
                  "to_date(ma.set_dt, 'DD-MON-YYYY') - to_date(ma.received_dt, 'DD-MON-YYYY') rec2set_dd, " +
                  "to_date(ma.set_dt, 'DD-MON-YYYY') - to_date(ga.interment_dt, 'DD-MON-YYYY') int2set_dd, " +
                 /*
                  " round(ma.order_release_dt) - round(ga.interment_dt) int2rel_dd, " +
                  " round(ma.amas_order_dt) - round(ma.order_release_dt) rel2ord_dd, " +
                  " round(ma.received_dt) - round(ma.amas_order_dt) ord2rec_dd, " +
                  " round(ma.set_dt) - round(ma.received_dt) rec2set_dd, " +
                  " round(ma.set_dt) - round(ga.interment_dt) int2set_dd, " +
                 */
                  " ga.section_id,  " +
                  " ga.row_num,  " +
                  " ga.site_num,  " +
                  " ga.interment_dt " +
                  " from gravesite_assignment ga, " +
                  " decedent_information da, " +
                  " monument ma,  " +
                  " decedent_monument dma " +
                  " where ga.cemetery_num = ?  and  " +
                  " ga.interment_dt between to_date( ?, 'MM/DD/YYYY') and to_date( ?, 'MM/DD/YYYY') + 0.9999884 and  " +  
                  " ga.disinterment_dt is NULL and  " +
                  " ga.decedent_id = da.id  " +
                  " and da.id = dma.decedent_id (+) " +
                  " and dma.monument_id = ma.id (+)  " +
                  " and da.case_status_cd in ('CE', 'EL', 'AU') and  " +
                  " ma.repl_reason_cd is not null  " +
                  " UNION  " +   //prior interments
                  " select to_char(gb.interment_dt, 'MM/DD/YYYY'), " +
                  " gb.decedent_id, db.first_nme, db.last_nme, mb.monument_type_cd, " +
                  " mb.repl_reason_cd, " +
                  " mb.shipment_fy||' '||mb.ship_prefix||' '||mb.cbl_num, " +
                  "to_date(mb.order_release_dt, 'DD-MON-YYYY') - to_date(gb.interment_dt, 'DD-MON-YYYY') int2rel_dd, " +
                  "to_date(mb.amas_order_dt, 'DD-MON-YYYY') - to_date(mb.order_release_dt, 'DD-MON-YYYY') rel2ord_dd, " +
                  "to_date(mb.received_dt, 'DD-MON-YYYY') - to_date(mb.amas_order_dt, 'DD-MON-YYYY') ord2rec_dd, " +
                  "to_date(mb.set_dt, 'DD-MON-YYYY') - to_date(mb.received_dt, 'DD-MON-YYYY') rec2set_dd, " +
                  "to_date(mb.set_dt, 'DD-MON-YYYY') - to_date(gb.interment_dt, 'DD-MON-YYYY') int2set_dd, " +
                 /*
                  " round(mb.order_release_dt) - round(gb.interment_dt), " +
                  " round(mb.amas_order_dt) - round(mb.order_release_dt), " +
                  " round(mb.received_dt) - round(mb.amas_order_dt), " +
                  " round(mb.set_dt) - round(mb.received_dt), " +
                  " round(mb.set_dt) - round(gb.interment_dt), " +
                 */
                  " gb.section_id, gb.row_num, gb.site_num, gb.interment_dt " +
                  " from gravesite_assignment gb, " +
                  " decedent_information db, " +
                  " monument_history mb, " +
                  " decedent_monument_history dmb " +
                  " where gb.cemetery_num = ? " +
                  " and gb.interment_dt between to_date( ?, 'MM/DD/YYYY') and to_date( ?, 'MM/DD/YYYY') + 0.9999884 " + 
                  " and gb.disinterment_dt is NULL " +
                  " and gb.decedent_id = db.id " +
                  " and db.id = dmb.decedent_id  " +
                  " and dmb.monument_id = mb.id (+) " + 
                  " and db.case_status_cd in ('CE','EL','AU') " +
                  " and dmb.insert_dt > to_date( ?, 'MM/DD/YYYY') " +
                  // " and dmb.insert_dt > gb.interment_dt " +
                  " and db.id not in (select decedent_id from decedent_monument where monument_id in " + 
                  " (select id from monument where repl_reason_cd is not null))" +
                  " and mb.id = (select min(dmbb.monument_id) from decedent_monument_history dmbb " +
                  " where dmbb.decedent_id = db.id and amas_order_dt is not null) " +
                  " order by 16";
        try {

                pstmt = conn.prepareStatement(qry);
                pstmt.setString(1, cemNum); //if you want to add parameter in the query
                pstmt.setString(2, startDT); 
                pstmt.setString(3, endDT);
                pstmt.setString(4, startDT); 
                pstmt.setString(5, cemNum);
                pstmt.setString(6, startDT); 
                pstmt.setString(7, endDT); 
                pstmt.setString(8, cemNum);
                pstmt.setString(9, startDT); 
                pstmt.setString(10, endDT);
                pstmt.setString(11, startDT); 
                
                //pstmt.setString(4, cemNum);
                //pstmt.setString(5, startDT); 
                //pstmt.setString(6, endDT); 
                //pstmt.setString(7, cemNum);
                //pstmt.setString(8, startDT); 
                //pstmt.setString(9, endDT);
                rs = pstmt.executeQuery();

                //instruct the browser to send output as Excel
                res.setContentType(CONTENT_TYPE);
                res.setHeader("Content-Disposition","attachment; filename=MonOrderTime_cem" + cemNum + " " + startDT + "-" + endDT + ".xls");   // filename
                ServletOutputStream out= res.getOutputStream();

                //header in Excel
                out.println ("Monument Order Timeliness Report \n" + 
                             cemNum + " " + cemNme + ", " + startDT + " to " + endDT + "\n" );
                            
                out.println("INTER DT\t" + "DEC ID\t" + 
                            "FIRST NAME\t" + "LAST NAME\t" +
                            "S\t" + "TC\t" + "RC\t" + 
                            "SHIPMENT NUM\t" + "INT-REL\t" + "REL-ORD\t" +
                            "ORD-REC\t" + "REC-SET\t" + "INT-SET"
                           );

                //send data to Excel
                while (rs.next()) {
                    out.println(Utility.blankIfNull(rs.getString(1)) + "\t" + //interment_dt
                                Utility.blankIfNull(rs.getString(2)) + "\t" + //dec_ID
                                Utility.blankIfNull(rs.getString(3)) + "\t" + //first_nme
                                Utility.blankIfNull(rs.getString(4)) + "\t" + //last_nme

                             //sub decedent --> if subdecedent, return "*"
                                getSubdecedent(cemNum,rs.getString(2),rs.getString(1),  //dec_id, interment
                                               rs.getString(13),rs.getString(14),rs.getString(15), //sec, row, site
                                               conn) + "\t" + //sub_decedent
                                               
                                Utility.blankIfNull(rs.getString(5)) + "\t" +
                                Utility.blankIfNull(rs.getString(6)) + "\t" +
                                Utility.blankIfNull(rs.getString(7)) + "\t" +
                                Utility.blankIfNull(rs.getString(8)) + "\t" +
                                Utility.blankIfNull(rs.getString(9)) + "\t" +
                                Utility.blankIfNull(rs.getString(10)) + "\t" +
                                Utility.blankIfNull(rs.getString(11)) + "\t" +
                                Utility.blankIfNull(rs.getString(12))
                               );
                }

                out.close(); //close the Excel file

                rs.close();
                pstmt.close();
                conn.close();
                rs = null;
                pstmt = null;
                conn = null;

        } catch (SQLException e) {
                Utility.displayErrMsg(res, conn, pstmt, 
                              "Error retrieving data" + 
                              "\n" + e.toString() + "\n" + qry);
                return;
        }
  }

   public void destroy(Connection conn) { 
 
      try {  
         conn.close(); 
      } 
      catch (Exception ignored) {System.err.println("GAR Report - Error in method destroy"); }  
   } 

   /** 
   * Method to return an "*" if there is a subdecedent in a grave
   */
   public static String getSubdecedent(String cemetery,
                                       String decedentID,
                                       String intermentDate,
                                       String sectionID,
                                       String rowNum,
                                       String siteNum,
                                       Connection conn)
   { 

        String subdecedent = "";
        String qry = "";

            try {
                    //to test qry:*/ Utility.displayErrMsg ( res, conn, pstmt, qry);
                      qry = " select '*'  " +
                            " from gravesite_assignment " +
                            " where cemetery_num = ? " +
                            "   and decedent_id != ? " +
                            "   and interment_dt > to_date(?, 'MM/DD/YYYY') " +
                            "   and section_id = ? " +
                            "   and row_num = ? " +
                            "   and site_num = ? " +
                            "   and interment_dt is not NULL " +
                            "   and disinterment_dt is NULL " +
                            "   and decedent_id in (select id " +
                            "       from decedent_information " +
                            "       where case_status_cd in ('CE','EL','AU'))";
        
                PreparedStatement pstmt = conn.prepareStatement(qry);
                pstmt.setString(1, cemetery); 
                pstmt.setString(2, decedentID);
                pstmt.setString(3, intermentDate);
                pstmt.setString(4, sectionID);
                pstmt.setString(5, rowNum);
                pstmt.setString(6, siteNum);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                   subdecedent = "*"; //rs.getString(1);
                } 


                rs.close();
                pstmt.close();
                rs = null;
                pstmt = null;

            } catch (SQLException e) {
                System.err.println(new java.util.Date() + 
                                   " Error retrieving subdecedebt " + "\n" + qry );
                //return;
            }

      return subdecedent;
   }

}
