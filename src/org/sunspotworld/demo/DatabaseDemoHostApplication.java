
package org.sunspotworld.demo;

import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.io.j2me.radiostream.*;
import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.peripheral.TimeoutException;
import com.sun.spot.peripheral.radio.RadioPolicy;
import com.sun.spot.util.Utils;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.*;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.microedition.io.*;
import javax.swing.Timer;



public class DatabaseDemoHostApplication {
    // Broadcast port on which we listen for sensor samples
    private static final int DATA_SINK_PORT = 67;
    
    
    // This demo assumes a mySQL database has been installed on the
    // local host at the default port (3306), has a database called
    // Test and can be manipulated using the default administrator 
    // settings (empty password for user 'root'). The JDBC driver 
    // needed to communicate with this database is specified in JDBC_DRIVER. 
    // If your setup is based on a different database and driver, you'll
    // need to modify these settings appropriately.
    // Obviously, an empty password should never be used for anything more
    // serious than a test setup. 
    private static final String DATABASE_URL = "jdbc:mysql://192.168.1.6:3306/";
    private static final String DATABASE_NAME = "wim2";
    private static final String DATABASE_USER = "user";
    private static final String DATABASE_PASSWORD = "";
    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    // Name of the database table where we store sensor readings
    private static final String DATA_TABLE_NAME = "lightreadings";
    
    // Duration (in milliseconds) for which to collect samples.
    private static final int SAMPLING_DURATION = 10000; 
    // This is how long we block waiting to read data on the
    // radio connection. Without a timeout, the host application
    // could run well beyond the duration specified above, e.g. if
    // there are no SPOTs transmitting sensor samples.
    private static final int CONNECTION_TIMEOUT = 10000; 
    private static double value;
    private int s1;
    
    private Statement stmt = null;
    private java.sql.Connection dbCon = null;
    private RadiogramConnection rCon = null;
    
    private void run() throws Exception {
        try {
            setUp();
//            dbCons();
            String query = "SELECT * FROM weight ORDER BY ID Desc Limit 1";
            PreparedStatement pst = dbCon.prepareStatement(query);
            ResultSet rs = pst.executeQuery();
            while(rs.next()){
                value = rs.getDouble("BeratWim1");
                s1 = rs.getInt("id");
                System.out.println(s1);
                
            }
            
            while(value>=6000){
            
            collectData();
//            analyzeData();
            
            tearDown();
            }
                
            
        } catch (Exception e) {
            e.printStackTrace();
        } 
//            tearDown();
            
        
    }
    private void dbCons() throws Exception{
        
    }
    private void setUp() throws Exception {
        System.out.println("Database demo application starting ... ");
        try {            
           
            Class.forName(JDBC_DRIVER);

            
            String url = DATABASE_URL + DATABASE_NAME;
            
            
            dbCon = DriverManager.getConnection(url, DATABASE_USER,
                    DATABASE_PASSWORD);
            
            // Display URL and connection information
            System.out.println("URL: " + url);
            System.out.println("Connection: " + dbCon);
        } catch (Exception e) {
            System.err.println("setUp caught " + e);
            System.err.println("Make sure that mySQL is installed properly \n" +
                    "and has a Test database accessible via the \n" +
                    "default administrator settings on localhost:3306\n");
            throw e;        
        }
        
        try {
            // Get a Statement object
            stmt = dbCon.createStatement();
//            
//           
//            try {
//                stmt.executeUpdate("DROP TABLE " + DATA_TABLE_NAME);
//                System.out.println("Existing table deleted.");
//            } catch (Exception e) {
//                System.out.println(e.getMessage());
//                System.out.println(
//                        "No existing table to delete.");
//            }
            
            // Create a table with three columns:
            // spotId    padded to 20 chars identifier for SPOT
            // timestamp YYYY-MM-DD hh:mm:ss string when reading was taken
            // reading   light sensor reading (4 is simply the display 
            //           width but an unsigned smallint can store any value
            //           from 0 to 65535)
            
//            stmt.executeUpdate(
//                    "CREATE TABLE " + DATA_TABLE_NAME + " (spotId char(20), " +
//                    "timestamp timestamp, reading smallint(4) unsigned)");

            
            rCon = (RadiogramConnection) Connector.open("radiogram://:" +
                    DATA_SINK_PORT);
            // wait a finite time for data to arrive whenever we call receive()
            rCon.setTimeout(CONNECTION_TIMEOUT); 
            System.out.println("setUp completed successfully");
        } catch (Exception e) {
             System.err.println("setUp caught " + e.getMessage());
             throw e;
        }
    }
    
    public void collectData() throws Exception {
        String id = null;
        String ts = null;
        int val = 0;
        double xval = 0;
        double yval = 0;
        double zval = 0;
        int sampleCnt = 0;
        Datagram dg = null;
        long start = 0L;

        System.out.println("Sampling for " + (SAMPLING_DURATION/1000) +
                " seconds ...\n[Each * indicates one sample, ! implies" + 
                " radio timed out waiting for a sample]");
        start = System.currentTimeMillis();
        dg = rCon.newDatagram(rCon.getMaximumLength());
        
        // Main data collection loop
        while ((System.currentTimeMillis() - start) < SAMPLING_DURATION) {
            try {
                // Read sensor sample received over the radio
                rCon.receive(dg);
                id = dg.readUTF();  // read sender's Id
                ts = dg.readUTF();  // read time stamp for the reading
//                val = dg.readInt(); // read the sensor value
                xval = dg.readDouble();
                yval = dg.readDouble();
                zval = dg.readDouble();
                System.out.println("*");
                stmt.executeUpdate("INSERT INTO " + DATA_TABLE_NAME +
                        "(spotId,x_accel,y_accel,z_accel)" +
                        "VALUES('" + id + "'  , '" + xval + "' , '"+yval+"' , '"+zval+"' )");
                sampleCnt++;
            } catch (TimeoutException e) {
                System.err.println("!");
            } catch (SQLException e) {
                System.err.println("Caught " + e + 
                        " while storing sensor sample <" +
                        "'" + id + "','" + xval + "','"+yval+"','"+zval+"' >");
                throw e;
            } catch (Exception e) {
                System.err.println("Caught " + e + 
                        " while reading sensor samples.");
                throw e;
            }
        }

        System.out.println("\nDone! Collected " + sampleCnt + " samples.");
    }
    
    public void analyzeData() throws Exception {
        ResultSet rs = null;
        String id = null;
        String ts = null;
        int val = 0;
        
        try {
            // Display all results
            rs = stmt.executeQuery("SELECT * " +
                    "from " + DATA_TABLE_NAME + 
                    " ORDER BY timestamp");
            System.out.println("Collected sensor readings:");
            System.out.println("\t+--------------------------------------" +
                    "-----------------+");
            System.out.println("\t|      spotId       \t      timestamp  " +
                    " \treading |");
            System.out.println("\t+--------------------------------------" +
                    "-----------------+");
            while (rs.next()) {
                id = rs.getString("spotId");
                ts = rs.getString("timestamp");
                val = rs.getInt("reading");
                System.out.println("\t " + id + "\t" + ts + "\t   " + val);
            }    
            System.out.println("\t+--------------------------------------" +
                    "-----------------+");
            
            // Print the spotId and timestamp corresponding to the highest val
            rs = stmt.executeQuery("SELECT * " +
                    "from " + DATA_TABLE_NAME + 
                    " ORDER BY reading DESC LIMIT 1");
            while (rs.next()) {
                id = rs.getString("spotId");
                ts = rs.getString("timestamp");
                val = rs.getInt("reading");
                System.out.println("\tMax: " + val +
                        " (recorded by SPOT " + id + " at " + ts + ")");
            }
            
            // Print the spotId and timestamp corresponding to the lowest val
            rs = stmt.executeQuery("SELECT * " +
                    "from " + DATA_TABLE_NAME + 
                    " ORDER BY reading ASC LIMIT 1");
            while (rs.next()) {
                id = rs.getString("spotId");
                ts = rs.getString("timestamp");
                val = rs.getInt("reading");
                System.out.println("\tMin: " + val +
                        " (recorded by SPOT " + id + " at " + ts + ")");
            }
            
            // Print the average sensor reading across all samples
            rs = stmt.executeQuery("SELECT AVG(reading) AS avg FROM " +
                    DATA_TABLE_NAME);
            while (rs.next()) {
                val = rs.getInt("avg");
                System.out.println("\tAvg: " + val);
            }            
        } catch (Exception e) {
            System.err.println("Caught " + e + " in analyzeData()");
            throw e;
        }
    }
    
    public void tearDown() throws Exception {
        
        if (dbCon != null) dbCon.close();
        if (rCon != null) rCon.close();
        
//        System.exit(0);
    }
    
    /**
     * Start up the host application.
     *
     * @param args any command line arguments
     */
    public static void main(String[] args) throws Exception {
        DatabaseDemoHostApplication app = new DatabaseDemoHostApplication();
        app.run();
    }
}
