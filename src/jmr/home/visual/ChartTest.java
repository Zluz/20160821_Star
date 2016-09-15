package jmr.home.visual;

import java.io.File;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import jmr.home.database.ConnectionProvider;

public class ChartTest
{
   public static void main( String[ ] args )throws Exception {
      
      /* Create MySQL Database Connection */
      Class.forName( "com.mysql.jdbc.Driver" );
//      Connection connect = DriverManager.getConnection( 
//      "jdbc:mysql://localhost:3306/jf_testdb" ,     
//      "root",     
//      "root123");
      
//      Statement statement = connect.createStatement( );
      Statement statement = ConnectionProvider.createStatement();

//      ResultSet resultSet = statement.executeQuery("select * from dataset_tb" );
//      final String strSQL = "select * from galaxy.atom";
//      final String strSQL = "SELECT * FROM galaxy.line where ( name like 'A%' ) AND NOT ( name like 'Atom%' )";
//      final String strSQL = "SELECT * FROM galaxy.line al, galaxy.atom a where al.seq_Atom = a.seq AND ( al.name like 'A1' ) AND NOT ( al.name like 'Atom%' ) AND Created > date_sub( now(), interval 2 day )";

      final String strSQLTH = "SELECT * FROM galaxy.line al, galaxy.atom a where al.seq_Atom = a.seq AND ( ( al.name like 'Temp' ) OR ( al.name like 'Humid' ) ) AND ( Created > date_sub( now(), interval 240 minute ) ) order by created desc";

      ResultSet resultSetTH = statement.executeQuery( strSQLTH );
      final XYSeries seriesTemp = new XYSeries( "Temperature" );
      final XYSeries seriesHumid = new XYSeries( "Humidity" );

      while( resultSetTH.next( ) ) {
    	  final float fValue = resultSetTH.getFloat( "Value" );
    	  final Date dateCreated = resultSetTH.getTime( "Created" );
    	  final String strName = resultSetTH.getString( "Name" );
    	  final long lTime = dateCreated.getTime();

    	  if ( "Temp".equals( strName ) ) {
    		  seriesTemp.add( lTime, fValue );
    	  } else if ( "Humid".equals( strName ) ) {
    		  seriesHumid.add( lTime, fValue );
    	  }
      }

      XYSeriesCollection dataset = new XYSeriesCollection();
      dataset.addSeries( seriesTemp );
      dataset.addSeries( seriesHumid );


      final String strSQL = "SELECT * FROM galaxy.line al, galaxy.atom a where al.seq_Atom = a.seq AND ( al.name like 'A%' ) AND NOT ( al.name like 'Atom%' ) AND Created > date_sub( now(), interval 10 minute )";

      ResultSet resultSet = statement.executeQuery( strSQL );
      
      final XYSeries series[] = new XYSeries[8];
      for ( int i=1; i<8; i++ ) {
          series[i] = new XYSeries( "Series_" + i );
      }
      
      long lLast = 0;

      final boolean bAddAnalogPins = false;
  		if ( bAddAnalogPins  ) {

  		while( resultSet.next( ) ) {
	    	  final int iValue = resultSet.getInt( "ValueInt" );
	    	  final Date dateCreated = resultSet.getTime( "Created" );
	    	  final String strName = resultSet.getString( "Name" );
	    	  final int iPin = strName.charAt(1) - '0';
	    	  final long lTime = dateCreated.getTime();
	    	  lLast = lTime;
	    	  
	    	  int iValueNorm = Math.min( iValue, 300 );
	    	  double fValueNorm = iValueNorm + iPin;
	    	  fValueNorm = 10*Math.random() - 5 + iValueNorm;
	    	  
	//    	  series[iPin].add( lTime, (float)iValue/1024 );
	    	  series[iPin].add( lTime, fValueNorm );
	    	  series[iPin+1].add( lTime+10000, fValueNorm + 10 );
	//    	  series01.add(x, y);
	//         dataset.setValue( 
	//        		 resultSet.getString( "name" ) ,
	//        		 Double.parseDouble( resultSet.getString( "value" )) );
	      }

	      for ( int i=1; i<7; i++ ) {
	          dataset.addSeries( series[i] );
	      }
	
	      final XYSeries series_A = new XYSeries( "Series_A" );
	      final XYSeries series_B = new XYSeries( "Series_B" );
	      for ( int i=1; i<100; i++ ) {
	    	  long lTime = (long) (lLast - 100000*Math.random());
	    	  double fValueA = 300*Math.random();
	          series_A.add( lTime, fValueA );
	    	  double fValueB = 300*Math.random();
	          series_B.add( lTime, fValueB );
	      }
	      
	      dataset.addSeries( series_A );
	      dataset.addSeries( series_B );
      }
      
      JFreeChart chart = ChartFactory.createScatterPlot( 
    		  "Node Send History", "Time", "Values", dataset );

//      JFreeChart chart = ChartFactory.createPieChart(
//         "Mobile Sales",  // chart title           
//         dataset,         // data           
//         true,            // include legend          
//         true,           
//         false );

      int width = 1600; /* Width of the image */
      int height = 700; /* Height of the image */ 
      File file = new File( "Chart.jpeg" );
      ChartUtilities.saveChartAsJPEG( file , chart , width , height );
      System.out.println( "File generated: " + file.getAbsolutePath() );
   }
}