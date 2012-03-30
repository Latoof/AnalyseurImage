
import boofcv.core.image.ConvertBufferedImage;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

public class Methodes {

 
	/**
	 * --> Depuis la doc officielle de BoofCV :
	 * BufferedImage support many different image formats internally.  More often than not the order
	 * of its bands are not RGB, which can cause problems when you expect it to be RGB.  A function
	 * is provided that will swap the bands of a MultiSpectral image created from a BufferedImage
	 * to ensure that it is in RGB ordering.
	 */
	public static void convertBufferedToRGB( BufferedImage input ) {
 
		// convert the BufferedImage into a MultiSpectral
		MultiSpectral<ImageUInt8> image = ConvertBufferedImage.convertFromMulti(input,null,ImageUInt8.class);
 
		/* Arbitrary pixel */
		int x=15,y=15;
 
		// print the value of "red" at a pixel to make it easy to see the change
		System.out.print("before: ");
		for( int i = 0; i < image.getNumBands(); i++ )
			System.out.print(image.getBand(i).get(x,y)+" ");
		System.out.println();
 
		// change the bands
		ConvertBufferedImage.orderBandsIntoRGB(image,input);
 
		// THe value of red should be different of the BufferedImage was not in RGB format.
		System.out.print("After:  ");
		for( int i = 0; i < image.getNumBands(); i++ )
			System.out.print(image.getBand(i).get(x,y)+" ");
		System.out.println();
	}
	/*** FIN METHODES OFFICIELLES */
 

	/* Depuis une liste de valeurs, on renvoie une liste avec les valeurs donnees en pourcentage */
	public static List<Double> totalToPercentage( List<Integer> rgb ) {
		
		int total = 0;
		for (int i=0; i<rgb.size(); i++) {
			total += rgb.get(i);
		}
		
		Double[] rgbp = { 0.0, 0.0, 0.0 };
		
		for (int i=0; i<rgb.size(); i++) {
			rgbp[i] = (double) (rgb.get(i)/(double)total) * 100;
		}
		
		return Arrays.asList(rgbp);
		
	}
	
	/* Recupere le pourcentage total de chaque couleur RGB dans l'image */
	public static List<Integer> getTotalRGB( BufferedImage input ) {
				
		Integer[] rgb = {0,0,0};
			
		MultiSpectral<ImageUInt8> image = ConvertBufferedImage.convertFromMulti(input,null,ImageUInt8.class);
		ConvertBufferedImage.orderBandsIntoRGB(image,input);

		/* Pour chaque pixel et chaque couleur, on ajoute au tableau la quantite de couleur presente dans le pixel */
		for( int i = 0; i < image.getNumBands(); i++ ) {
			
			for ( int h = 0; h < image.getHeight(); h++ ) {
				for ( int w = 0; w < image.getWidth(); w++ ) {
					
					rgb[i] += image.getBand(i).get(w, h);
					
				}
			}
			
		}
		
		return Arrays.asList(rgb);
		
	}
	
	/* Amelioration de getTotalRGB : Ne compte une couleur que lorsqu'elle predomine sur les autres, sur un pixel donne */
	public static List<Integer> getDifferencialRGB( BufferedImage input ) {
				
		Integer[] rgb = {0,0,0};
			
		MultiSpectral<ImageUInt8> image = ConvertBufferedImage.convertFromMulti(input,null,ImageUInt8.class);
		
		/* Modifie l'ordre des bandes de couleurs de facon a obtenir un ordre RGB */
		ConvertBufferedImage.orderBandsIntoRGB(image,input);

		/* Pour chaque pixel de l'image */
		for ( int h = 0; h < image.getHeight(); h++ ) {
			for ( int w = 0; w < image.getWidth(); w++ ) {
				
				/* Somme des couleurs du pixel */
				int total = 0;
				for( int i = 0; i < image.getNumBands(); i++ ) {

					total += image.getBand(i).get(w, h);
				
				}
				
				/* Moyenne des valeurs des trois bandes de couleur du pixel */
				double moy = total /  image.getNumBands();
								
				if ( total != 0 ) {
					for( int i = 0; i < image.getNumBands(); i++ ) {

						/* Si, pour ce pixel, la valeur d'une bande est superieure de plus de 20% 
						 * par rapport a la moyenne, on lui attributs un "point", 
						 * et on estime que la couleur domine sur ce pixel
						 */
						if ( image.getBand(i).get(w, h) > moy+moy*0.20 ) {
							rgb[i]++;	
						}
					
					}
				}
				
				
			}
		}
		
		
		return Arrays.asList(rgb);
		
	}
	
	
	
	
}