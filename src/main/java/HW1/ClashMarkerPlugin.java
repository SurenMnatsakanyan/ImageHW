package HW1;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.plugin.PlugIn;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ClashMarkerPlugin implements PlugIn {
  private static final String filePathOfCRS = "src/main/resources/tre-s-92.crs";
  private static final  String filePathOfSTU = "src/main/resources/tre-s-92.stu";

  @Override
  public void run(final String s) {
   int N  = getCourseCountFromCRSFile();

    ImageProcessor binaryProcessor= new BinaryProcessor(new ByteProcessor(N,N));
    binaryProcessor.setColor(Color.WHITE);
    binaryProcessor.fill();
    markClashingCourses(binaryProcessor);

    ImagePlus binaryImage = new ImagePlus("Clashing Courses", binaryProcessor);
    binaryImage.show();
    FileSaver fileSaver = new FileSaver(binaryImage);
    fileSaver.saveAsPng("src/main/resources/result.png");
  }

  private void markClashingCourses( ImageProcessor binaryProcessor) {
    try (BufferedReader reader = new BufferedReader(new FileReader(filePathOfSTU))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String[] tokens = line.trim().split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
          for (int j = i + 1; j < tokens.length; j++) {
            int courseIndex1 = Integer.parseInt(tokens[i]);
            int courseIndex2 = Integer.parseInt(tokens[j]);
            binaryProcessor.putPixel(courseIndex1 , courseIndex2 , 0);
            binaryProcessor.putPixel(courseIndex2, courseIndex1 , 0);
          }
        }
      }
    } catch (IOException e) {
      IJ.log("Error reading .stu file: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static int getCourseCountFromCRSFile(){
    try(BufferedReader bufferedReader =  new BufferedReader(new FileReader(filePathOfCRS))){
      int courseCount  = 0;
      String line;
      while ((line = bufferedReader.readLine()) != null)
        courseCount++;
      return courseCount;
    }catch (IOException e) {
      e.printStackTrace();
    }
    return 0;
  }

  public static void main(String[] args) {
   ClashMarkerPlugin clashMarkerPlugin = new ClashMarkerPlugin();
   clashMarkerPlugin.run("");
  }
}
