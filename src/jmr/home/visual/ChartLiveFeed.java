package jmr.home.visual;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;

import jmr.home.apps.StarApp;
import jmr.home.database.ConnectionProvider;

public class ChartLiveFeed {

	final Composite comp;
	final Thread thread;
	final File file;
	private Image image;
	final private Canvas canvas;
	private Display display;
	private int iHeight;
	private boolean bQueueResizeRefresh;
	
	public ChartLiveFeed( final Composite parent ) {
		this.comp = parent;
		this.image = null;
		
		canvas = new Canvas( parent, SWT.NONE );
		parent.setLayout( new FillLayout() );
//		canvas = null;
		
		File fileTemp = null;
		try {
			fileTemp = File.createTempFile( "LiveChart_", ".jpg" );
		} catch ( final IOException e ) {
			System.err.println( "Failed to allocate temporary file: " + e );
		}
		this.file = fileTemp;
		this.file.deleteOnExit();
		
		this.iHeight = 500;
		
		if ( null!=this.file ) {
			
			this.thread = new Thread( "Chart update thread" ) {
				@Override
				public void run() {
					try {
						do {
							update();
							
//							Thread.sleep( 1000 );
							for ( int i=0; i<100; i++ ) {
								Thread.sleep( 10 );
								if ( bQueueResizeRefresh ) {
									i=100;
								}
							}
							
						} while ( true );
					} catch ( final InterruptedException e ) {
						System.out.println( "Thread for live chart interrupted." );
					}
				}
			};
			
			this.canvas.addControlListener( new ControlAdapter() {
				@Override
				public void controlResized( final ControlEvent e ) {
					bQueueResizeRefresh = true;
				}
			});
			
			this.canvas.addPaintListener( new PaintListener() {
				@Override
				public void paintControl( final PaintEvent event ) {
					if ( null==event ) return;
					if ( null==image ) return;
					if ( null==canvas ) return;
					if ( image.isDisposed() ) return;
					if ( canvas.isDisposed() ) return;
					
					final int iWidthImage = image.getImageData().width;
					final int iWidthCanvas = canvas.getSize().x;
					iHeight = canvas.getSize().y;
					
					event.gc.drawImage( image, iWidthCanvas - iWidthImage, 0 );
					
					bQueueResizeRefresh = false;
				}
			});
			
		} else {
			this.thread = null;
		}
		thread.start();
	}

	protected void update() {
		
      try ( final Statement statement = ConnectionProvider.createStatement() ) {

//	      ResultSet resultSet = statement.executeQuery("select * from dataset_tb" );
//	      final String strSQL = "select * from galaxy.atom";
//	      final String strSQL = "SELECT * FROM galaxy.line where ( name like 'A%' ) AND NOT ( name like 'Atom%' )";
//	      final String strSQL = "SELECT * FROM galaxy.line al, galaxy.atom a where al.seq_Atom = a.seq AND ( al.name like 'A1' ) AND NOT ( al.name like 'Atom%' ) AND Created > date_sub( now(), interval 2 day )";

	      final String strSQLTH = 
	    		  "SELECT * "
    				  + "FROM galaxy.line al, galaxy.atom a "
    				  + "WHERE al.seq_Atom = a.seq "
    				  	+ "AND ( ( al.name like 'Temp%' ) OR ( al.name like 'Humid%' ) OR (al.name like 'Gas%' ) ) "
    				  	+ "AND ( Created > date_sub( now(), interval 10 minute ) ) "
    				  + "ORDER BY created desc";

	      ResultSet resultSetTH = statement.executeQuery( strSQLTH );
	      final XYSeries seriesTemp = new XYSeries( "Temperature" );
	      final XYSeries seriesHumid = new XYSeries( "Humidity" );
	      final XYSeries seriesGas = new XYSeries( "Flammable Gas" );
	      
	      boolean bHasData = false;

	      while( resultSetTH.next( ) ) {
	    	  try {
		    	  final Object obj = resultSetTH.getObject( "Value" );
		    	  if ( "NA".equals( obj ) ) {
//		    		  System.out.println( "x" );
		    	  } else {
			    	  final float fValue = resultSetTH.getFloat( "Value" );
//			    	  if ( Float.isInfinite( fValue ) ) {
			    	  if ( Float.isNaN( fValue ) ) {
			    		  System.err.println( "Value intended for live feed chart is infinite." );
			    	  } else {
				    	  final Date dateCreated = resultSetTH.getTime( "Created" );
				    	  final String strName = resultSetTH.getString( "Name" );
				    	  final long lTime = dateCreated.getTime();

				    	  if ( strName.startsWith( "Temp" ) ) {
//				    	  if ( "Temp".equals( strName ) ) {
				    		  seriesTemp.add( lTime, fValue );
					    	  bHasData = true;
//				    	  } else if ( "Humid".equals( strName ) ) {
				    	  } else if ( strName.startsWith( "Humid" ) ) {
				    		  seriesHumid.add( lTime, fValue );
					    	  bHasData = true;
//				    	  } else if ( "Gas".equals( strName ) ) {
				    	  } else if ( strName.startsWith( "Gas" ) ) {
				    		  seriesGas.add( lTime, fValue );
					    	  bHasData = true;
				    	  }
			    	  }
		    	  }
	    	  } catch ( final SQLException e ) {
	    		  // can get: 
	    		  // java.sql.SQLException: Invalid value for getFloat() - 'NA' in column 4
	    	  }
	      }

	      XYSeriesCollection dataset = new XYSeriesCollection();
	      dataset.addSeries( seriesTemp );
	      dataset.addSeries( seriesHumid );
	      dataset.addSeries( seriesGas );


	      final boolean bAddAnalogPins = false;
	  		if ( bAddAnalogPins  ) {


	  	      final String strSQL = "SELECT * FROM galaxy.line al, galaxy.atom a where al.seq_Atom = a.seq AND ( al.name like 'A%' ) AND NOT ( al.name like 'Atom%' ) AND Created > date_sub( now(), interval 10 minute )";

	  	      ResultSet resultSet = statement.executeQuery( strSQL );
	  	      
	  	      final XYSeries series[] = new XYSeries[8];
	  	      for ( int i=1; i<8; i++ ) {
	  	          series[i] = new XYSeries( "Series_" + i );
	  	      }
	  	      
	  	      long lLast = 0;

	  			
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
		    	  
		    	  if ( !Double.isNaN( fValueNorm ) ) {
		    	  
			//    	  series[iPin].add( lTime, (float)iValue/1024 );
			    	  series[iPin].add( lTime, fValueNorm );
			    	  series[iPin+1].add( lTime+10000, fValueNorm + 10 );
			//    	  series01.add(x, y);
			//         dataset.setValue( 
			//        		 resultSet.getString( "name" ) ,
			//        		 Double.parseDouble( resultSet.getString( "value" )) );
			    	  bHasData = true;
		    	  }
		      }

		      for ( int i=1; i<7; i++ ) {
		          dataset.addSeries( series[i] );
		      }
		
		      final XYSeries series_A = new XYSeries( "Series_A" );
		      final XYSeries series_B = new XYSeries( "Series_B" );
		      for ( int i=1; i<100; i++ ) {
		    	  long lTime = (long) (lLast - 100000*Math.random());
		    	  double fValueA = 300*Math.random();
		    	  if ( !Double.isNaN(fValueA) ) {
		    		  series_A.add( lTime, fValueA );
		    	  }
		    	  double fValueB = 300*Math.random();
		    	  if ( !Double.isNaN(fValueB) ) {
		    		  series_B.add( lTime, fValueB );
		    	  }
		      }
		      
		      dataset.addSeries( series_A );
		      dataset.addSeries( series_B );
	      }
	      
	  	  if ( bHasData ) {
		      JFreeChart chart = ChartFactory.createScatterPlot( 
		    		  "Node Send History", "Time", "Values", dataset );
		      LegendTitle legend = chart.getLegend();
		      legend.setPosition( RectangleEdge.BOTTOM );
		      chart.setAntiAlias( true );
	
	//	      JFreeChart chart = ChartFactory.createPieChart(
	//	         "Mobile Sales",  // chart title           
	//	         dataset,         // data           
	//	         true,            // include legend          
	//	         true,           
	//	         false );
	
		      int width = 800; /* Width of the image */
		      
	//	      File file = new File( "Chart.jpeg" );
		      
		      file.delete();

		      ChartUtilities.saveChartAsJPEG( file, chart, width, iHeight );

		      display = this.comp.getDisplay();
		      display.asyncExec( new Runnable() {
		    	  @Override
		    	  public void run() {
		    		  if ( null!=image ) {
		    			  image.dispose();
		    		  }
		    		  image = new Image( display, file.getPath() );
		    	
	//	    		  canvas.setBackgroundImage( image );
		    		  canvas.redraw();
	//	    		  comp.setBackgroundImage( image );
	//	    		  comp.redraw();
		    	  }	    	  
		      });
	  	  }
	      
      } catch ( final Throwable t ) {
    	  if ( !StarApp.bStopping ) {
    		  System.err.println( 
    				  "Error while generating chart: " + t.toString() );
    		  t.printStackTrace();
    	  }
      }
	}
	
	
}
