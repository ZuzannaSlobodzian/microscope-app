import java.nio.ByteBuffer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.util.Duration;
import javafx.scene.layout.*;
import java.util.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import java.util.List;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.stage.Screen;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import java.io.*;
import javax.imageio.*;
import java.awt.image.BufferedImage;



public class Cam extends Application
{
    private static final int FRAME_WIDTH  = 640;
    private static final int FRAME_HEIGHT = 480;


    GraphicsContext gc, gcSecondary;
    Canvas canvas, canvasSecondary;
    byte bufferSnap[];
    BufferedImage image = null;
    BufferedImage binaryImageSave = null;
    final Stage secondaryStage = new Stage();
    Scene sceneSecondary;
    Screen secondaryScreen;

    Button btn = new Button();
    Button btnSave = new Button();
    Button btnLoopa = new Button();
    Button btnLoad = new Button();
    Button btnBinary = new Button();
    Button btnDilate = new Button();
    Button btnErode = new Button();
    Button btnCanny = new Button();
    Button btnMedian = new Button();

    int level;

    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage)
    {
        Timeline timeline;

        final Screen primaryScreen = Screen.getPrimary();
        final List<Screen> allScreens = Screen.getScreens();

        if (allScreens.size() <= 1) {
            System.out.println("Only one screen");
            secondaryScreen = primaryScreen;
        } else {
            if (allScreens.get(0).equals(primaryScreen)) {
                secondaryScreen = allScreens.get(1);
            } else {
                secondaryScreen = allScreens.get(0);
            }
        }
        configureAndShowStage("Primary", primaryStage, primaryScreen);  //wyswietlanie ekranu jeden

        primaryStage.setTitle("Camera");
        primaryStage.setWidth(FRAME_WIDTH + 120);
        primaryStage.setHeight(FRAME_HEIGHT + 120);
        secondaryStage.setTitle("Snap");
        secondaryStage.setWidth(FRAME_WIDTH + 120);
        secondaryStage.setHeight(FRAME_HEIGHT + 120);
        Scene scene;
        Slider alpha;

        alpha = new Slider(0, 765, 5);
        alpha.setShowTickMarks(true);
        alpha.setShowTickLabels(true);
        alpha.valueProperty().addListener(new ChangeListener<Number>()
        {
            public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val)
            {
                level = new_val.intValue();
                System.out.println("alpha=" + new_val);
            }
        });

        AnchorPane root = new AnchorPane();
        AnchorPane rootSecondary = new AnchorPane();

        canvas     = new Canvas(650, 490);
        gc         = canvas.getGraphicsContext2D();

        canvasSecondary     = new Canvas(650, 490);
        gcSecondary         = canvasSecondary.getGraphicsContext2D();

        btn.setText("Snap");
        btn.setOnAction(this::disp_frame2);
        btnLoopa.setText("+");

        btnLoad.setText("Load file");
        btnBinary.setText("Binary");
        btnDilate.setText("Dilate");
        btnErode.setText("Erode");
        btnCanny.setText("Canny");
        btnMedian.setText("Median");


        btnSave.setText("Save");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save");
        fileChooser.getExtensionFilters().addAll(new ExtensionFilter("All Files", "*.*"));

        btnSave.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {

                File outputFile = fileChooser.showSaveDialog(secondaryStage);
                int RGB_pixels[];
                BufferedImage bi;
                RGB_pixels = new int[FRAME_WIDTH*FRAME_HEIGHT];
                if(bufferSnap!=null){
                    ByteArrayInputStream bais = new ByteArrayInputStream(bufferSnap); // tu jest zapisanie z buffered snap
                    try{
                        int i, j;

                        j = 0;
                        for(i = 0; i < RGB_pixels.length; i++)
                        {
                            RGB_pixels[i] = (int) (bufferSnap[j] << 16) + (bufferSnap[j+1]<< 8) + bufferSnap[j+2];
                            j+=3;
                        }

                        bi = new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_INT_RGB);

                        bi.setRGB(0, 0, FRAME_WIDTH, FRAME_HEIGHT, RGB_pixels, 0, FRAME_WIDTH);

                        System.out.println("try");
                        ImageIO.write(bi, "png", outputFile);
                    }catch (IOException e){ System.out.println("blad !!");
                        throw new RuntimeException(e);
                    }
                }else{
                    try {
                        ImageIO.write(binaryImageSave, "png", outputFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
        });



        btnLoad.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {

                File result= fileChooser.showOpenDialog(secondaryStage);

                try{
                    image = ImageIO.read(result);
                    binaryImageSave = image;

                }catch (IOException e){ System.out.println("blad !!");
                    throw new RuntimeException(e);
                }
                WritableImage wr = null;
                if(image != null) {
                    PixelWriter pw = gcSecondary.getPixelWriter();

                    for (int x = 0; x < image.getWidth(); x++) {
                        for (int y = 0; y < image.getHeight(); y++) {
                            pw.setArgb(x + 100, y + 100, image.getRGB(x, y));
                        }
                    }
                }
                bufferSnap = null;

            }
        });

        btnBinary.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                int w = image.getWidth();
                int h = image.getHeight();
                BufferedImage binaryImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

                if(image == null){
                }
                else{
                    for(int i = 0; i < w; i++){
                        for(int j = 0; j < h; j++){
                            int val = image.getRGB(i, j);
                            int r = (0x00ff0000 & val) >> 16;
                            int g = (0x0000ff00 & val) >> 8;
                            int b = (0x000000ff & val);

                            int m=(r+g+b);

                            if(m>=level){
                                binaryImage.setRGB(i, j, 0xFFFFFFFF);
                            }
                            else{
                                binaryImage.setRGB(i, j, 0);
                            }
                        }
                    }
                    PixelWriter pw = gcSecondary.getPixelWriter();

                    for(int x = 0; x < binaryImage.getWidth(); x++){
                        for(int y = 0; y < binaryImage.getHeight(); y++){
                            pw.setArgb(x+100, y+100, binaryImage.getRGB(x, y));
                        }
                    }
                }
                binaryImageSave = binaryImage;


            }
        });

        btnDilate.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                int width = binaryImageSave.getWidth();
                int height = binaryImageSave.getHeight();
                BufferedImage dilatedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {

                        boolean isWhite = false;
                        for (int i = x - 1; i <= x + 1; i++) {
                            for (int j = y - 1; j <= y + 1; j++) {

                                if (i >= 0 && i < width && j >= 0 && j < height && binaryImageSave.getRGB(i, j) == 0xFFFFFFFF) {
                                    isWhite = true;
                                    break;
                                }
                            }
                            if (isWhite) {
                                break;
                            }
                        }
                        if (isWhite) {
                            dilatedImage.setRGB(x, y, 0xFFFFFFFF);
                        } else {
                            dilatedImage.setRGB(x, y, 0xFF000000);
                        }
                    }
                }
                PixelWriter pw = gcSecondary.getPixelWriter();

                for(int x = 0; x < dilatedImage.getWidth(); x++){
                    for(int y = 0; y < dilatedImage.getHeight(); y++){
                        pw.setArgb(x+100, y+100, dilatedImage.getRGB(x, y));
                    }
                }

                binaryImageSave = dilatedImage;


            }
        });

        btnErode.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                int width = binaryImageSave.getWidth();
                int height = binaryImageSave.getHeight();
                BufferedImage erodedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        boolean isBlack = false;
                        for (int i = x - 1; i <= x + 1; i++) {
                            for (int j = y - 1; j <= y + 1; j++) {
                                if (i >= 0 && i < width && j >= 0 && j < height && binaryImageSave.getRGB(i, j) == 0xFF000000) {
                                    isBlack = true;
                                    break;
                                }
                            }
                            if (isBlack) {
                                break;
                            }
                        }
                        if (isBlack) {
                            erodedImage.setRGB(x, y, 0xFF000000);
                        } else {
                            erodedImage.setRGB(x, y, 0xFFFFFFFF);
                        }
                    }
                }
                PixelWriter pw = gcSecondary.getPixelWriter();

                for(int x = 0; x < erodedImage.getWidth(); x++){
                    for(int y = 0; y < erodedImage.getHeight(); y++){
                        pw.setArgb(x+100, y+100, erodedImage.getRGB(x, y));
                    }
                }

                binaryImageSave = erodedImage;


            }
        });

        btnCanny.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                int width = binaryImageSave.getWidth();
                int height = binaryImageSave.getHeight();
                BufferedImage enhancedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

                for (int x = 1; x < width - 1; x++) {
                    for (int y = 1; y < height - 1; y++) {
                        int top = binaryImageSave.getRGB(x, y - 1) & 0xFF;
                        int left = binaryImageSave.getRGB(x - 1, y) & 0xFF;
                        int center = binaryImageSave.getRGB(x, y) & 0xFF;
                        int right = binaryImageSave.getRGB(x + 1, y) & 0xFF;
                        int bottom = binaryImageSave.getRGB(x, y + 1) & 0xFF;

                        if (center != left || center != right || center != top || center != bottom) {
                            enhancedImage.setRGB(x, y, 0xFFFFFFFF);
                        } else {
                            enhancedImage.setRGB(x, y, 0xFF000000);
                        }
                    }
                }

                PixelWriter pw = gcSecondary.getPixelWriter();

                for(int x = 0; x < enhancedImage.getWidth(); x++){
                    for(int y = 0; y < enhancedImage.getHeight(); y++){
                        pw.setArgb(x+100, y+100, enhancedImage.getRGB(x, y));
                    }
                }

                binaryImageSave = enhancedImage;


            }
        });

        btnMedian.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                int kernelSize = 3;
                int width = binaryImageSave.getWidth();
                int height = binaryImageSave.getHeight();
                BufferedImage enhancedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

                int halfKernel = kernelSize / 2;

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int[] pixelValues = new int[kernelSize * kernelSize];

                        for (int dy = -halfKernel; dy <= halfKernel; dy++) {
                            for (int dx = -halfKernel; dx <= halfKernel; dx++) {
                                int neighborX = Math.min(Math.max(x + dx, 0), width - 1);
                                int neighborY = Math.min(Math.max(y + dy, 0), height - 1);

                                pixelValues[(dy + halfKernel) * kernelSize + (dx + halfKernel)] = binaryImageSave.getRGB(neighborX, neighborY);
                            }
                        }

                        Arrays.sort(pixelValues);

                        int medianPixel = pixelValues[(kernelSize * kernelSize) / 2];

                        enhancedImage.setRGB(x, y, medianPixel);


                    }
                }

                PixelWriter pw = gcSecondary.getPixelWriter();

                for(int a = 0; a < enhancedImage.getWidth(); a++){
                    for(int b = 0; b < enhancedImage.getHeight(); b++){
                        pw.setArgb(a+100, b+100, enhancedImage.getRGB(a, b));
                    }
                }

                binaryImageSave = enhancedImage;
            }
        });

        timeline = new Timeline(new KeyFrame(Duration.millis(130), e->disp_frame()));

        timeline.setCycleCount(Timeline.INDEFINITE);

        timeline.play();

        root.getChildren().add(canvas);

        root.getChildren().add(btn);
        root.getChildren().add(btnLoopa);
        root.setBottomAnchor(btnLoopa, 5.0d);


        scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();

        configureAndShowStage("Secondary", secondaryStage, secondaryScreen); //wyswietlanie ekranu dwa

        rootSecondary.getChildren().add(canvasSecondary);
        rootSecondary.getChildren().add(btnSave);
        rootSecondary.getChildren().add(btnLoad);
        rootSecondary.getChildren().add(btnBinary);
        rootSecondary.getChildren().add(btnDilate);
        rootSecondary.getChildren().add(btnErode);
        rootSecondary.getChildren().add(btnCanny);
        rootSecondary.getChildren().add(btnMedian);
        rootSecondary.setTopAnchor(btnLoad, 30.0d);
        rootSecondary.setTopAnchor(btnBinary, 60.0d);
        rootSecondary.setTopAnchor(btnDilate, 90.0d);
        rootSecondary.setTopAnchor(btnErode, 120.0d);
        rootSecondary.setTopAnchor(btnCanny, 150.0d);
        rootSecondary.setTopAnchor(btnMedian, 180.0d);

        rootSecondary.getChildren().add(alpha);
        AnchorPane.setTopAnchor(alpha, 490.0d);

        sceneSecondary = new Scene(rootSecondary);
        secondaryStage.setScene(sceneSecondary);
        secondaryStage.show();
    }

    private void disp_frame()//obraz z mikroskopu
    {

        //pixelWriter = gc.getPixelWriter();
        //pixelFormat = PixelFormat.getByteRgbInstance();


        //buffer = frames.get_frame();
        //pixelWriter.setPixels(100, 100, FRAME_WIDTH, FRAME_HEIGHT, pixelFormat, buffer, 0, FRAME_WIDTH*3);

    }

    private void disp_frame2(ActionEvent e)//obraz z mikroskopu
    {

//            configureAndShowStage("Secondary", secondaryStage, secondaryScreen); //wyswietlanie ekranu dwa
//
//      rootSecondary.getChildren().add(canvasSecondary);
//      rootSecondary.getChildren().add(btnSave);
//
//      sceneSecondary = new Scene(rootSecondary);
//       secondaryStage.setScene(sceneSecondary);
//         secondaryStage.show();


        // pixelWriter = gcSecondary.getPixelWriter();
        // PixelFormat.getByteRgbInstance();



        //bufferSnap = frames.get_frame(); //to trzeba zapisac
        //System.out.println(Arrays.toString(bufferSnap));
        //pixelWriter.setPixels(100, 100, FRAME_WIDTH, FRAME_HEIGHT, pixelFormat, bufferSnap, 0, FRAME_WIDTH*3);
        image = null;

    }


    private void configureAndShowStage(final String name, final Stage stage, final Screen screen) {
        StackPane root = new StackPane();
        root.getChildren().add(new Label(name + " stage"));
        Scene scene = new Scene(root, 300, 200);
        stage.setScene(scene);
        stage.setTitle(name);

        Rectangle2D bounds = screen.getBounds();
        System.out.println(bounds);
        stage.setX(bounds.getMinX() + (bounds.getWidth() - 300) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - 200) / 2);
        stage.show();

    }

}