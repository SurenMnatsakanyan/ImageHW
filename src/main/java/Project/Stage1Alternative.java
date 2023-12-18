package Project;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class Stage1Alternative {

    private static final double PIXELS_PER_CM = 100; // Example value, adjust for your image

    public static void main(String[] args) {
        // Load the image
        ImagePlus originalImage = IJ.openImage("src/main/resources/CSHandwriting/M103/OOP.MT2.240315.M103_p2.jpg");
        IJ.run(originalImage, "8-bit", "");
        // Invert the image
        ByteProcessor processor = (ByteProcessor) originalImage.getProcessor();
        processor.invert();
        processor.erode();
        processor.erode();
        originalImage.show();

        // Convert the image to grayscale

        // Calculate the height in pixels for 2 cm
        int pixelHeightFor2Cm = (int) (2 * PIXELS_PER_CM);

        // Map to store the mean pixel value of each cropped section
        LinkedHashMap<ImagePlus, Double>  croppedSectionsMeanValues = new LinkedHashMap<>();
        double max = 0;
        // Crop the image into sections and calculate the mean pixel value
        for (int y = 0; y < processor.getHeight(); y += pixelHeightFor2Cm) {
            int cropHeight = Math.min(pixelHeightFor2Cm, processor.getHeight() - y); // Ensure we don't go outside the image
            processor.setRoi(0, y, processor.getWidth(), cropHeight);
            ByteProcessor croppedProcessor = (ByteProcessor) (processor.crop());
// Convert croppedProcessor to ByteProcessor if it's not already

            // Calculate the mean pixel value for the cropped section, excluding black pixels
            double sum = 0;
            int count = 0;
            for (int x = 0; x < croppedProcessor.getWidth(); x++) {
                for (int h = 0; h < croppedProcessor.getHeight(); h++) {
                    int pixelValue = croppedProcessor.get(x, h);
                    if (pixelValue > 240) { // Exclude black pixels
                        sum += pixelValue;
                        count++;
                    }
                }
            }

            double mean = count > 0 ? sum / count : 0; // Calculate mean, avoid division by zero
            if(mean > max)
                max = mean;
            // Store the mean value
            String sectionKey = String.format("Section_FullWidth_%d", y / pixelHeightFor2Cm);
            new ImagePlus(sectionKey,croppedProcessor).show();
            ImagePlus croppedImage = new ImagePlus(sectionKey, croppedProcessor);
            croppedSectionsMeanValues.put(croppedImage, mean);

            // Output the mean value
        }

        // Show and save the inverted and grayscale image if necessary
        if(max > 240){
        ImagePlus stitchedHandwrittenImage = stitchImages(croppedSectionsMeanValues, 18, max);
        ImagePlus stitchedPrintedImage = stitchImagesPrinted(croppedSectionsMeanValues, 18, max);
        stitchedHandwrittenImage.show();
        stitchedPrintedImage.show();
        }
        else
            originalImage.show();
        // Save the image if needed: IJ.save(originalImage, "path_to_save_inverted_grayscale_image.jpg");
    }

    private static ImagePlus stitchImages(LinkedHashMap<ImagePlus, Double> images, double threshold, double max) {
        ArrayList<ImageProcessor> selectedImages = new ArrayList<>();
        int totalHeight = 0, width = 0;

        for (Map.Entry<ImagePlus, Double> entry : images.entrySet()) {
            if (Math.pow(max - entry.getValue(),2) > threshold) {
                ImageProcessor ip = entry.getKey().getProcessor();
                selectedImages.add(ip);
                totalHeight += ip.getHeight();
                width = Math.max(width, ip.getWidth());
            }
        }

        // Create a new image with the total height
        ImageProcessor stitchedProcessor = getImageProcessor(selectedImages, totalHeight, width);

        return new ImagePlus("Stitched Image", stitchedProcessor);
    }


    private static ImagePlus stitchImagesPrinted(LinkedHashMap<ImagePlus, Double> images, double threshold, double max) {
        ArrayList<ImageProcessor> selectedImages = new ArrayList<>();
        int totalHeight = 0, width = 0;

        for (Map.Entry<ImagePlus, Double> entry : images.entrySet()) {
            if (Math.pow(max - entry.getValue(),2) < threshold) {
                ImageProcessor ip = entry.getKey().getProcessor();
                selectedImages.add(ip);
                totalHeight += ip.getHeight();
                width = Math.max(width, ip.getWidth());
            }
        }

        // Create a new image with the total height
        ImageProcessor stitchedProcessor = getImageProcessor(selectedImages, totalHeight, width);

        return new ImagePlus("Stitched Image Printed", stitchedProcessor);
    }

    private static ImageProcessor getImageProcessor(ArrayList<ImageProcessor> selectedImages, int totalHeight, int width) {
        ImageProcessor stitchedProcessor = new ByteProcessor(width, totalHeight);
        int y = 0;
        for (ImageProcessor ip : selectedImages) {
            stitchedProcessor.insert(ip, 0, y);
            y += ip.getHeight();
        }
        stitchedProcessor.invert();
        return stitchedProcessor;
    }
}
