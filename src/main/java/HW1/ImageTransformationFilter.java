package HW1;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class ImageTransformationFilter implements PlugInFilter {
  @Override
  public int setup(String args, ImagePlus imagePlus) {
    return DOES_ALL;
  }

  @Override
  public void run(ImageProcessor imageProcessor) {

    int width = imageProcessor.getWidth();
    int height = imageProcessor.getHeight();

    int leftWidth = width / 2;
    int rightWidth = width - leftWidth;
    int topHeight = height / 2;
    int bottomHeight = height - topHeight;

    int y = 0;
    while(y != height){
      for (int x = 0; x < leftWidth; x++) {
        int leftPixel = imageProcessor.getPixel(x, y);
        int rightPixel = imageProcessor.getPixel(x + rightWidth, y);
        imageProcessor.putPixel(x, y, rightPixel);
        imageProcessor.putPixel(x + rightWidth, y, leftPixel);
      }
      y++;
    }
    y = 0;
    while (y != topHeight){
      for (int x = 0; x < width; x++) {
        int topPixel = imageProcessor.getPixel(x, y);
        int bottomPixel = imageProcessor.getPixel(x, y + bottomHeight);
        imageProcessor.putPixel(x, y, bottomPixel);
        imageProcessor.putPixel(x, y + bottomHeight, topPixel);
      }
      y++;
    }
    ImagePlus imagePlus = new ImagePlus("Transofrmer", imageProcessor);
    imagePlus.show();
  }

  public static void main(String[] args) {
    ImagePlus ip = IJ.openImage("src/main/resources/result.png");
    ImageTransformationFilter imageTransformationFilter = new ImageTransformationFilter();
    imageTransformationFilter.setup("", ip);
    imageTransformationFilter.run(ip.getProcessor());
    FileSaver fileSaver = new FileSaver(ip);
    fileSaver.saveAsPng("src/main/resources/result-edit.png");
  }
}
