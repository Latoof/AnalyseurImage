

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;

import boofcv.io.image.UtilImageIO;


public class AnalyseurImage {

	
	public List<Double> analyse( InputStream input ) {
		
		BufferedImage imageBuf = null;
		
		try {
			imageBuf = ImageIO.read(input);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			
			Methodes.convertBufferedToRGB( imageBuf );

			List<Integer> listd = Methodes.getDifferencialRGB(imageBuf);
			for ( int i=0; i<listd.size(); i++) {
				System.out.println(i+" --> "+listd.get(i));
			}
			
			List<Double> listdp = Methodes.totalToPercentage( listd );
			for ( int i=0; i<listdp.size(); i++) {
				System.out.println(i+" --> "+listdp.get(i)+" %");
			}
			
			return listdp;
			
		}	
		
		return null;
	}
	
	public List<Double> analyse( BufferedImage imageBuf ) {
		
		Methodes.convertBufferedToRGB( imageBuf );

		List<Integer> listd = Methodes.getDifferencialRGB(imageBuf);
		for ( int i=0; i<listd.size(); i++) {
			System.out.println(i+" --> "+listd.get(i));
		}
		
		List<Double> listdp = Methodes.totalToPercentage( listd );
		for ( int i=0; i<listdp.size(); i++) {
			System.out.println(i+" --> "+listdp.get(i)+" %");
		}
		
		return listdp;
			
	}
	
	public static void main( String args[] ) {
		
		BufferedImage input = UtilImageIO.loadImage("res/img.jpg");
		 
		// Uncomment lines below to run each example
 
		List<Integer> list = Methodes.getTotalRGB(input);
		for ( int i=0; i<list.size(); i++) {
			System.out.println(i+" --> "+list.get(i));
		}
		
		List<Double> listp = Methodes.totalToPercentage( list );
		for ( int i=0; i<listp.size(); i++) {
			System.out.println(i+" --> "+listp.get(i)+" %");
		}
		
		Methodes.convertBufferedToRGB( input );

		
		/*
		File f = new File("res/img.jpg");
		
		
		this.analyse(   );
		*/
	}
	
}
	

