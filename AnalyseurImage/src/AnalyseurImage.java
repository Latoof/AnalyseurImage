

	/*
	* Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
	*
	* This file is part of BoofCV (http://boofcv.org).
	*
	* Licensed under the Apache License, Version 2.0 (the "License");
	* you may not use this file except in compliance with the License.
	* You may obtain a copy of the License at
	*
	* http://www.apache.org/licenses/LICENSE-2.0
	*
	* Unless required by applicable law or agreed to in writing, software
	* distributed under the License is distributed on an "AS IS" BASIS,
	* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	* See the License for the specific language governing permissions and
	* limitations under the License.
	*/

	import boofcv.abst.feature.associate.GeneralAssociation;
	import boofcv.abst.feature.describe.DescribeRegionPoint;
	import boofcv.abst.feature.detect.interest.InterestPointDetector;
	import boofcv.alg.feature.associate.ScoreAssociateEuclideanSq;
import boofcv.alg.filter.blur.BlurImageOps;
	import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.misc.GPixelMath;
	import boofcv.alg.sfm.robust.DistanceHomographySq;
	import boofcv.alg.sfm.robust.GenerateHomographyLinear;
	import boofcv.core.image.ConvertBufferedImage;
	import boofcv.factory.feature.associate.FactoryAssociation;
	import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
	import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
	import boofcv.gui.image.HomographyStitchPanel;
	import boofcv.gui.image.ShowImages;
	import boofcv.io.image.UtilImageIO;
	import boofcv.numerics.fitting.modelset.ModelMatcher;
	import boofcv.numerics.fitting.modelset.ransac.SimpleInlierRansac;
	import boofcv.struct.FastQueue;
	import boofcv.struct.feature.AssociatedIndex;
	import boofcv.struct.feature.TupleDescQueue;
	import boofcv.struct.feature.TupleDesc_F64;
	import boofcv.struct.image.ImageFloat32;
	import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
	import georegression.struct.homo.Homography2D_F64;
	import georegression.struct.point.Point2D_F64;

import java.awt.Image;
	import java.awt.image.BufferedImage;
	import java.util.ArrayList;
import java.util.List;

	/**
	* <p> Exampling showing how to combines two images together by finding the best fit image transform with point
	* features.</p>
	* <p>
	* Algorithm Steps:<br>
	* <ol>
	* <li>Detect feature locations</li>
	* <li>Compute feature descriptors</li>
	* <li>Associate features together</li>
	* <li>Use robust fitting to find transform</li>
	* <li>Render combined image</li>
	* </ol>
	* </p>
	*
	* @author Peter Abeles
	*/
	public class AnalyseurImage {

	/**
	* Using abstracted code, find a transform which minimizes the difference between corresponding features
	* in both images. This code is completely model independent and is the core algorithms.
	*/
	public static<T extends ImageSingleBand> Homography2D_F64
	computeTransform( T imageA , T imageB ,
	InterestPointDetector<T> detector ,
	DescribeRegionPoint<T> describe ,
	GeneralAssociation<TupleDesc_F64> associate ,
	ModelMatcher<Homography2D_F64,AssociatedPair> modelMatcher )
	{
	// see if the detector has everything that the describer needs
	if( describe.requiresOrientation() && !detector.hasOrientation() )
	throw new IllegalArgumentException("Requires orientation be provided.");
	if( describe.requiresScale() && !detector.hasScale() )
	throw new IllegalArgumentException("Requires scale be provided.");

	// get the length of the description
	int descriptionDOF = describe.getDescriptionLength();

	List<Point2D_F64> pointsA = new ArrayList<Point2D_F64>();
	FastQueue<TupleDesc_F64> descA = new TupleDescQueue(descriptionDOF,true);
	List<Point2D_F64> pointsB = new ArrayList<Point2D_F64>();
	FastQueue<TupleDesc_F64> descB = new TupleDescQueue(descriptionDOF,true);

	// extract feature locations and descriptions from each image
	describeImage(imageA, detector, describe, pointsA, descA);
	describeImage(imageB, detector, describe, pointsB, descB);

	// Associate features between the two images
	associate.associate(descA,descB);

	// create a list of AssociatedPairs that tell the model matcher how a feature moved
	FastQueue<AssociatedIndex> matches = associate.getMatches();
	List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();

	for( int i = 0; i < matches.size(); i++ ) {
	AssociatedIndex match = matches.get(i);

	Point2D_F64 a = pointsA.get(match.src);
	Point2D_F64 b = pointsB.get(match.dst);

	pairs.add( new AssociatedPair(a,b,false));
	}

	// find the best fit model to describe the change between these images
	if( !modelMatcher.process(pairs) )
	throw new RuntimeException("Model Matcher failed!");

	// return the found image transform
	return modelMatcher.getModel();
	}

	/**
	* Detects features inside the two images and computes descriptions at those points.
	*/
	private static <T extends ImageSingleBand>
	void describeImage(T image,
	InterestPointDetector<T> detector,
	DescribeRegionPoint<T> describe,
	List<Point2D_F64> points,
	FastQueue<TupleDesc_F64> descs) {
	detector.detect(image);
	describe.setImage(image);

	descs.reset();
	TupleDesc_F64 desc = descs.pop();
	for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
	// get the feature location info
	Point2D_F64 p = detector.getLocation(i);
	double yaw = detector.getOrientation(i);
	double scale = detector.getScale(i);

	// extract the description and save the results into the provided description
	if( describe.process(p.x,p.y,yaw,scale,desc) != null ) {
	points.add(p.copy());
	desc = descs.pop();
	}
	}
	// remove the last element from the queue, which has not been used.
	descs.removeTail();
	}

	/**
	* Given two input images create and display an image where the two have been overlayed on top of each other.
	*/
	public static <T extends ImageSingleBand> void stitch( BufferedImage imageA , BufferedImage imageB ,
	Class<T> imageType )
	{
	T inputA = ConvertBufferedImage.convertFromSingle(imageA, null, imageType);
	T inputB = ConvertBufferedImage.convertFromSingle(imageB, null, imageType);

	// Detect using the standard SURF feature descriptor and describer
	InterestPointDetector<T> detector = FactoryInterestPoint.fastHessian(1, 2, 400, 1, 9, 4, 4);
	DescribeRegionPoint<T> describe = FactoryDescribeRegionPoint.surf(true,imageType);
	GeneralAssociation<TupleDesc_F64> associate = FactoryAssociation.greedy(new ScoreAssociateEuclideanSq(),2,-1,true);

	// fit the images using a homography. This works well for rotations and distant objects.
	GenerateHomographyLinear modelFitter = new GenerateHomographyLinear();
	DistanceHomographySq distance = new DistanceHomographySq();
	int minSamples = modelFitter.getMinimumPoints();
	ModelMatcher<Homography2D_F64,AssociatedPair> modelMatcher =
	new SimpleInlierRansac<Homography2D_F64,AssociatedPair>(123,modelFitter,distance,60,minSamples,30,1000,9);

	Homography2D_F64 H = computeTransform(inputA, inputB, detector, describe, associate, modelMatcher);

	// draw the results
	HomographyStitchPanel panel = new HomographyStitchPanel(0.5,inputA.width,inputA.height);
	panel.configure(imageA,imageB,H);
	ShowImages.showWindow(panel,"Stitched Images");
	}
	
	
	
	
	
	
	
	
	/** COLORS
	 * 
	 * 
	 * 
	 * @param args
	 */
	
	/**
	 * Many operations designed to only work on {@link boofcv.struct.image.ImageSingleBand} can be applied
	 * to a MultiSpectral image by feeding in each band one at a time.
	 */
	public static void independent( BufferedImage input ) {
		// convert the BufferedImage into a MultiSpectral
		MultiSpectral<ImageUInt8> image = ConvertBufferedImage.convertFromMulti(input,null,ImageUInt8.class);
 
		// declare the output blurred image
		MultiSpectral<ImageUInt8> blurred =
				new MultiSpectral<ImageUInt8>(ImageUInt8.class,image.width,image.height,image.getNumBands());
 
		// Apply Gaussian blur to each band in the image
		for( int i = 0; i < image.getNumBands(); i++ ) {
			// note that the generalized version of BlurImageOps is not being used, but the type
			// specific version.
			BlurImageOps.gaussian(image.getBand(i),blurred.getBand(i),-1,5,null);
		}
 
		// Declare the BufferedImage manually to ensure that the color bands have the same ordering on input
		// and output
		BufferedImage output = new BufferedImage(image.width,image.height,input.getType());
		ConvertBufferedImage.convertTo(blurred, output);
		ShowImages.showWindow(input,"Input");
		ShowImages.showWindow(output,"Ouput");
	}
 
	/**
	 * BufferedImage support many different image formats internally.  More often than not the order
	 * of its bands are not RGB, which can cause problems when you expect it to be RGB.  A function
	 * is provided that will swap the bands of a MultiSpectral image created from a BufferedImage
	 * to ensure that it is in RGB ordering.
	 */
	public static void convertBufferedToRGB( BufferedImage input ) {
 
		// convert the BufferedImage into a MultiSpectral
		MultiSpectral<ImageUInt8> image = ConvertBufferedImage.convertFromMulti(input,null,ImageUInt8.class);
 
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
 
	/**
	 * Values of pixels can be read and modified by accessing the internal {@link boofcv.struct.image.ImageSingleBand}.
	 */
	public static void pixelAccess(  BufferedImage input ) {
		// convert the BufferedImage into a MultiSpectral
		MultiSpectral<ImageUInt8> image = ConvertBufferedImage.convertFromMulti(input,null,ImageUInt8.class);
 
		int x = 10, y = 10;
 
		// to access a pixel you first access the gray image for the each band
		for( int i = 0; i < image.getNumBands(); i++ )
			System.out.println("Original "+i+" = "+image.getBand(i).get(x,y));
 
		// change the value in each band
		for( int i = 0; i < image.getNumBands(); i++ )
			image.getBand(i).set(x, y, 100 + i);
 
		// to access a pixel you first access the gray image for the each band
		for( int i = 0; i < image.getNumBands(); i++ )
			System.out.println("Result   "+i+" = "+image.getBand(i).get(x,y));
	}
 
	/**
	 * There is no real perfect way that everyone agrees on for converting color images into gray scale
	 * images.  Two examples of how to convert a MultiSpectral image into a gray scale image are shown 
	 * in this example.
	 */
	public static void convertToGray( BufferedImage input ) {
		// convert the BufferedImage into a MultiSpectral
		MultiSpectral<ImageUInt8> image = ConvertBufferedImage.convertFromMulti(input,null,ImageUInt8.class);
 
		ImageUInt8 gray = new ImageUInt8( image.width,image.height);
 
		// creates a gray scale image by averaging intensity value across pixels
		GPixelMath.bandAve(image, gray);
		BufferedImage outputAve = ConvertBufferedImage.convertTo(gray,null);
 
		// create an output image just from the first band
		BufferedImage outputBand0 = ConvertBufferedImage.convertTo(image.getBand(0),null);
 
		ShowImages.showWindow(outputAve,"Average");
		ShowImages.showWindow(outputBand0,"Band 0");
	}
	
	String analyse( BufferedImage input ) {
		
		return "";
		
	}

	public static void main( String args[] ) {
		
		/*
		BufferedImage imageA,imageB;
		imageA = UtilImageIO.loadImage("res/img1.jpg");
		imageB = UtilImageIO.loadImage("res/img2.jpg");
		stitch(imageA,imageB, ImageFloat32.class);
		System.out.println("Done");
		*/
		
		BufferedImage input = UtilImageIO.loadImage("res/sea.jpg");
		 
		// Uncomment lines below to run each example
 
		AnalyseurImage.independent(input);
		//AnalyseurImage.convertBufferedToRGB(input);
		AnalyseurImage.pixelAccess(input);
		//AnalyseurImage.convertToGray(input);
		// Image img =Image ;
//		AnalyseurImage(input);
	}
	
}
	

