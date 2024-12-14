package Project;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;

public class EdgeDetection {

    public static void main(String[] args) {

        ImagePlus imp = IJ.openImage("src/main/resources/metaqse/TestDress17.jpg");
        imp.show("Original Image");

        final List<HSV> firstLeftHSVColors = hsvColors((ColorProcessor) imp.getProcessor(), 2, 10);
        final List<HSV> lastRightHSVColors = hsvColors((ColorProcessor) imp.getProcessor(), imp.getProcessor().getHeight() - 6, 10);


        Canny_Edge_Detector canny = new Canny_Edge_Detector();
        // Set parameters
        canny.setLowThreshold(1f);
        canny.setHighThreshold(3f);
        canny.setGaussianKernelRadius(3f);
        canny.setGaussianKernelWidth(16);
        canny.setContrastNormalized(false);

        // Process the image
        ImagePlus edges = canny.process(imp);
        ImagePlus dup = edges.duplicate();
        dup.setTitle("Edges processed");
        dup.show("Edges processed");

        for (int i = 0; i < 20; i++) {
            edges.getProcessor().erode();
        }

        ImagePlus isolatedObject = new ImagePlus("Errosion Object", edges.getProcessor().duplicate());
        isolatedObject.show();

        FloodFiller floodFiller = new FloodFiller(edges.getProcessor());
        int seedX = edges.getWidth() / 2;
        int seedY = edges.getHeight() / 2;
        edges.getProcessor().setColor(255);
        floodFiller.fill(seedX, seedY);
        ImagePlus imagePlus = new ImagePlus("FloodFill", edges.getProcessor().duplicate());
        imagePlus.show();

        ImageProcessor originalProcessor = imp.getProcessor();
        ImageProcessor resultProcessor = originalProcessor.duplicate();
        // Apply the mask
        for (int y = 0; y < originalProcessor.getHeight(); y++) {
            for (int x = 0; x < originalProcessor.getWidth(); x++) {
                if (edges.getProcessor().getPixel(x, y) == 0) {
                    resultProcessor.putPixel(x, y, 0xffffff); // Set the pixel to white (or any background color)
                }
            }
        }



        // Create a new ImagePlus to hold the result
        ImagePlus resultImage = new ImagePlus("Result Image", resultProcessor);
        // Process the image to detect skin
        int[] pixels = (int[]) resultImage.getProcessor().getPixels();
        float[] hsb = new float[3];

        //
        Map<Integer, List<HSV>> hueStatics = new TreeMap<>();
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int red = (pixel & 0xff0000) >> 16;
            int green = (pixel & 0x00ff00) >> 8;
            int blue = (pixel & 0x0000ff);
            Color.RGBtoHSB(red, green, blue, hsb);

            // Use hsb[0], hsb[1], and hsb[2] for hue, saturation, and brightness respectively
            // to detect skin pixels and perform further processing...
            float hue = hsb[0] * 360; // Convert hue to degrees
            float saturation = hsb[1];
            float brightness = hsb[2];

            // Define skin color thresholds
            // These will need fine-tuning for your specific application
            float hueLowerThreshold = 0; // lower hue in degrees
            float hueUpperThreshold = 50; // upper hue in degrees
            float saturationLowerThreshold = 0.2f;
            float saturationUpperThreshold = 0.7f;

            float averageAboveBackgroundHue = calculateAverageFeature(firstLeftHSVColors, HSV::getHue);
            float averageLowerBackgroundHue = calculateAverageFeature(lastRightHSVColors, HSV::getHue);

            float averageAboveBackgroundSat = calculateAverageFeature(firstLeftHSVColors, HSV::getSaturation);
            float averageLowerBackgroundSat = calculateAverageFeature(lastRightHSVColors, HSV::getSaturation);

            float averageAboveBackgroundVal = calculateAverageFeature(firstLeftHSVColors, HSV::getValue);
            float averageLowerBackgroundVal = calculateAverageFeature(lastRightHSVColors, HSV::getValue);


            // Check if the pixel falls within the skin color HSV range
            if (hue >= hueLowerThreshold && hue <= hueUpperThreshold &&
                    saturation >= saturationLowerThreshold && saturation <= saturationUpperThreshold) {
                pixels[i] = 0xffffff; // White color to mask the skin
            }

            double distance1  = calculateDistance(hsb[0], saturation, brightness, averageAboveBackgroundHue/360f, averageAboveBackgroundSat, averageAboveBackgroundVal);
            double distance2  = calculateDistance(hsb[0], saturation, brightness, averageLowerBackgroundHue/360f, averageLowerBackgroundSat, averageLowerBackgroundVal);

            if(distance1 < 0.1f  || distance2 < 0.1f) {
                pixels[i] = 0xffffff;
            }

            pixel = pixels[i];
            red = (pixel & 0xff0000) >> 16;
            green = (pixel & 0x00ff00) >> 8;
            blue = (pixel & 0x0000ff);
            Color.RGBtoHSB(red, green, blue, hsb);

            // Use hsb[0], hsb[1], and hsb[2] for hue, saturation, and brightness respectively
            // to detect skin pixels and perform further processing...
            hue = hsb[0] * 360; // Convert hue to degrees
            saturation = hsb[1];
            brightness = hsb[2];
            int bucketNumber  = (int)hue/15;
            if(hue!=0 || saturation!=0 || brightness!=1) {
                if (hueStatics.get(bucketNumber) == null) {
                    List<HSV> hsvs = new ArrayList<>();
                    hsvs.add(new HSV(hue, saturation, brightness));
                    hueStatics.put(bucketNumber, hsvs);
                } else {
                    List<HSV> hsvs = hueStatics.get(bucketNumber);
                    hsvs.add(new HSV(hue, saturation, brightness));
                    hueStatics.put(bucketNumber, hsvs);
                }
            }
        }
        int maxCount = 0;
        int maxSection = 0;
        for(int i=0; i< 24; i++) {
            if(hueStatics.get(i) != null) {
                List<HSV> hsvs = hueStatics.get(i);
                if(hsvs.size() >maxCount) {
                    maxCount = hsvs.size();
                    maxSection = i;
                }
            }
        }
        float averageHue = calculateAverageFeature(hueStatics.get(maxSection), HSV::getHue);
        float averageSat = calculateAverageFeature(hueStatics.get(maxSection), HSV::getSaturation);
        float averageVal = calculateAverageFeature(hueStatics.get(maxSection), HSV::getValue);

        System.out.println("Dominant HSV is (" + averageHue + ", " + averageSat + ", " + averageVal + ")");
        resultImage.show();
    }

    private static List<HSV> hsvColors(ColorProcessor cp, int startY, int edgeWidth) {
        int[] RGB = new int[3];
        List<HSV> hsvs = new ArrayList<>();

            for (int x = 0; x < edgeWidth; x++) {
                cp.getPixel(x, startY, RGB);
                float[] arrHSV = Color.RGBtoHSB(RGB[0], RGB[1], RGB[2], null);
                HSV hsv = new HSV(arrHSV[0] * 360, arrHSV[1], arrHSV[2]);
                hsvs.add(hsv);
            }

        return hsvs;
    }

    private static double calculateDistance(float h1, float s1, float v1, float h2, float s2, float v2) {
        double distance = Math.sqrt(
                Math.pow((v2 - v1), 2) +
                        Math.pow(s1 * v1, 2) + Math.pow(s2 * v2, 2) -
                        2 * s1 * s2 * v1 * v2 * Math.cos((h2 - h1) * Math.PI) // Corrected to use radians
        );

        return distance;
    }

    private static float calculateAverageFeature(List<HSV> hsvs, Function<HSV, Float> hsvConsumer) {
        float sum = 0;
        for(int i = 0; i<hsvs.size(); i++) {
            sum += hsvConsumer.apply(hsvs.get(i));
        }
        return sum/hsvs.size();
    }

}
