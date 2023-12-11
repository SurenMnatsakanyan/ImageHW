package Project;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.Binary;
import ij.process.ColorProcessor;

import java.awt.*;
// This class contains printed text removal on sample image, considering the tolerance level.
public class Stage1 {

    private static final int COLOR_DIFFERENCE_TOLERANCE = 10;
    private static final int MARK_THRESHOLD = COLOR_DIFFERENCE_TOLERANCE;
    private static final int RED_MULTIPLIER_THRESHOLD = 3 * COLOR_DIFFERENCE_TOLERANCE;
    private static final int TOP_MARGIN_TO_IGNORE = 5;

    public static void main(String[] args) {
        ImagePlus originalImage = IJ.openImage("src/main/resources/CSHandwriting/M105/OOP.MT2.240315.M105_p2.jpg");

        ColorProcessor processor = (ColorProcessor) originalImage.getProcessor();

        applyBinaryOperation(originalImage);

        processor.invert();
        processor.erode();

        removePrintedTextAndMarks(processor);

        processor.invert();
        processor.erode();

        originalImage.updateAndDraw(); // To refresh the image window if it's open
        originalImage.show(); // To display the updated image
        IJ.save(originalImage, "src/main/resources/CSHandwriting/M105/");
    }

    private static void applyBinaryOperation(ImagePlus image) {
        Binary binaryOperation = new Binary();
        binaryOperation.setup("", image);
        binaryOperation.run(image.getProcessor());
    }

    private static void removePrintedTextAndMarks(ColorProcessor processor) {
        for (int x = 0; x < processor.getWidth(); x++) {
            for (int y = 0; y < processor.getHeight(); y++) {
                if (shouldBeCleared(processor, x, y)) {
                    processor.putPixel(x, y, Color.WHITE.getRGB()); // Clearing the pixel by setting it to white
                }
            }
        }
    }

    private static boolean shouldBeCleared(ColorProcessor processor, int x, int y) {
        Color pixelColor = processor.getColor(x, y);

        int red = pixelColor.getRed();
        int green = pixelColor.getGreen();
        int blue = pixelColor.getBlue();

        boolean colorCloseToGray = Math.abs(red - green) <= COLOR_DIFFERENCE_TOLERANCE
                && Math.abs(red - blue) <= COLOR_DIFFERENCE_TOLERANCE
                && Math.abs(green - blue) <= COLOR_DIFFERENCE_TOLERANCE;

        boolean likelyInstructorMark = green > MARK_THRESHOLD && blue > MARK_THRESHOLD && red < RED_MULTIPLIER_THRESHOLD;

        boolean inTopMargin = y < TOP_MARGIN_TO_IGNORE;

        return colorCloseToGray || likelyInstructorMark || inTopMargin;
    }
}