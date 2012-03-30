

import java.awt.image.BufferedImage;
import java.util.List;
import boofcv.io.image.UtilImageIO;

public class AnalyseurImage {

	public final String[] colors = { "Red", "Green", "Blue" };
	
	int higher_index( List<Double> list ) {
		
		/* Detection d'une couleur dominante */
		int max_index = -1;
		double max_value = -1;
		for (int i=0; i<list.size(); i++) {
			if ( list.get(i) > max_value ) {
				max_value = list.get(i);
				max_index = i;
				
			}
		}
		
		return max_index;
		
	}
	
	public List<Double> analyse_stat( BufferedImage imageBuf ) {
		
		Methodes.convertBufferedToRGB( imageBuf );

		List<Integer> listd = Methodes.getDifferencialRGB( imageBuf );
		List<Double> listdp = Methodes.totalToPercentage( listd );

		
		return listdp;
			
	}
	
	public String analyse_string( BufferedImage imageBuf ) {
		
		List<Double> list = this.analyse_stat(imageBuf);
		
		int max_index = this.higher_index( list );
		
		if ( max_index != -1 ) {
			return colors[max_index]+" is the major color";
		}
		else {
			return "No major color";
		}
		
	}
	
	public static void main( String args[] ) {
		
		BufferedImage input = UtilImageIO.loadImage("res/img.jpg");
		  
		List<Integer> list = Methodes.getTotalRGB(input);
		for ( int i=0; i<list.size(); i++) {
			System.out.println(i+" --> "+list.get(i));
		}
		
		List<Double> listp = Methodes.totalToPercentage( list );
		for ( int i=0; i<listp.size(); i++) {
			System.out.println(i+" --> "+listp.get(i)+" %");
		}
		
		AnalyseurImage ia = new AnalyseurImage();
		System.out.println("AnaS : "+ia.analyse_string( input ));

	}
	
}
	

