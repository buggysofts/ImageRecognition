package sample;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class InitWindowController implements Initializable {

    @FXML private Button train_btn;
    @FXML private Button test_btn;
    @FXML private Label status_txt;
    @FXML private ProgressBar progress;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        /*System.out.println("starting conversion...");
        String[] cmds = new String[39];
        for(int i=0;i<39;++i){
            String name = Integer.toString(i).length() == 1 ? ("0"+Integer.toString(i)):Integer.toString(i);
            File target = new File("/home/ragib/Desktop/data/"+"yaleB"+name);
            if(target.exists()){
                cmds[i] = "mogrify -format png"+" "+target.getAbsolutePath()+"/"+"*.pgm";
                try {
                    try {
                        Runtime.getRuntime().exec(cmds[i]).waitFor();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    File[] files = target.listFiles();
                    for(int j=0;files!=null && j<files.length;++j){
                        if(!files[j].getName().endsWith(".png")){
                            files[j].delete();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Could not find target "+target.getName());
            }
        }
        System.out.println("Conversion finished!");*/

        train_btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                DirectoryChooser train_src_loc_chooser = new DirectoryChooser();
                train_src_loc_chooser.setTitle("Choose training data set directory");
                File dir = train_src_loc_chooser.showDialog(MAIN.MainWindow);
                if(dir != null){
                    Thread training_thread = new Thread(new Runnable() {
                        @Override
                        public void run() {

                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    train_btn.setDisable(true);
                                    test_btn.setDisable(true);
                                    status_txt.setText("Preparing...");
                                }
                            });

                            List<File> files = getFileList(dir);
                            for(int i=0;i < files.size();++i){

                                int finalI = i;
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        status_txt.setText("Training - "+(finalI+1)+"/"+files.size());
                                        progress.setProgress(((double)(finalI +1))/((double)(files.size())));
                                    }
                                });

                                SaveHistData(files.get(i));
                            }

                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    train_btn.setDisable(false);
                                    test_btn.setDisable(false);
                                    status_txt.setText("Training Complete.");
                                }
                            });

                        }
                    });
                    training_thread.setDaemon(true);
                    training_thread.start();
                }

            }
        });

        test_btn.setOnAction(new EventHandler<ActionEvent>() {

            private Image image;
            private File last_best_match_candidate;
            private double last_best_confidence;

            @Override
            public void handle(ActionEvent event) {

                image = null;
                last_best_match_candidate = null;
                last_best_confidence = Double.MAX_VALUE;

                FileChooser test_img_chooser = new FileChooser();
                test_img_chooser.setTitle("Choose an image to test");
                test_img_chooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter(
                                "Image Files",
                                "*.png","*.PNG","*.jpg","*.JPG","*.jpeg","*.JPEG"
                        )
                );
                final File input_img_file = test_img_chooser.showOpenDialog(MAIN.MainWindow);
                if(input_img_file != null){

                    try {
                        image = new Image(new FileInputStream(input_img_file.getAbsolutePath()));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    if(image!=null && !image.isError()){

                        DirectoryChooser train_src_loc_chooser = new DirectoryChooser();
                        train_src_loc_chooser.setTitle("Choose data set location");
                        File dir = train_src_loc_chooser.showDialog(MAIN.MainWindow);
                        if(dir != null){

                            Thread training_thread = new Thread(new Runnable() {
                                @Override
                                public void run() {

                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            train_btn.setDisable(true);
                                            test_btn.setDisable(true);
                                            status_txt.setText("Preparing...");
                                        }
                                    });

                                    List<Integer> current_face_hist = getHistList(getLBPOutput(image));

                                    File[] files = dir.listFiles();
                                    for(int i=0;files != null && i < files.length;++i){

                                        int finalI = i;
                                        Platform.runLater(new Runnable() {
                                            @Override
                                            public void run() {
                                                status_txt.setText("Scanning - "+(finalI+1)+"/"+files.length);
                                                progress.setProgress(((double)(finalI +1))/((double)(files.length)));
                                            }
                                        });

                                        String[] current_trained_hist_strs = new String(ReadFile(files[i])).split("\n");
                                        List<Integer> current_trained_hist = new ArrayList<>(0);
                                        for(int j=0;j<current_trained_hist_strs.length;++j){
                                            current_trained_hist.add(Integer.parseInt(current_trained_hist_strs[j]));
                                        }

                                        double X = 0;
                                        for(int j=0;j<current_face_hist.size();++j){
                                            X = X + ((current_face_hist.get(j)-current_trained_hist.get(j))*(current_face_hist.get(j)-current_trained_hist.get(j)));
                                        }

                                        if(X<=last_best_confidence){
                                            last_best_confidence = X;
                                            last_best_match_candidate = new File(
                                                    files[i].getParentFile().getParentFile().getAbsolutePath()+File.separator +"imgs"+File.separator+files[i].getName().substring(0,files[i].getName().lastIndexOf(".hgd"))
                                            );
                                        }
                                    };

                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {

                                            train_btn.setDisable(false);
                                            test_btn.setDisable(false);
                                            status_txt.setText("Scan Complete");

                                            VBox box = new VBox(
                                                    8,
                                                    new Label("Input Image: "+input_img_file.getAbsolutePath()),
                                                    new ImageView(new Image("file://"+input_img_file.getAbsolutePath())),
                                                    new Label(""),
                                                    new Label("Output/Result Image: "+last_best_match_candidate.getAbsolutePath()),
                                                    new ImageView(new Image("file://"+last_best_match_candidate.getAbsolutePath())),
                                                    new Label(""),
                                                    new TextField("Inp image path: "+input_img_file.getAbsolutePath()),
                                                    new TextField("Out image path: "+last_best_match_candidate.getAbsolutePath())
                                            );
                                            box.setPadding(new Insets(8,8,8,8));

                                            Alert alert = new Alert(Alert.AlertType.INFORMATION,null,ButtonType.OK);
                                            alert.getDialogPane().setHeader(box);
                                            alert.getDialogPane().setContent(null);
                                            alert.setTitle("Scan result");
                                            alert.showAndWait();
                                        }
                                    });

                                }
                            });
                            training_thread.setDaemon(true);
                            training_thread.start();
                        }
                    } else {
                        System.out.println("Something went wrong");
                    }
                }

            }
        });

    }

    private List<File> getFileList(File directory){

        List<File> files = new ArrayList<>(0);
        getFileList(directory, files);

        return files;

    }
    private void getFileList(File directory, List<File> retList){
        File[] files = directory.listFiles();
        for(int i=0;files != null && i<files.length;++i){
            if(files[i].isFile()){
                retList.add(files[i]);
            } else {
                getFileList(directory, retList);
            }
        }
    }

    private void SaveHistData(File file) {
        Image image = null;
        try {
            image = new Image(new FileInputStream(file.getAbsolutePath()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(image!=null && !image.isError()){

            Image img = getLBPOutput(image);
            if(img != null){
                List<Integer> hist = getHistList(img);

                StringBuilder data_builder = new StringBuilder("");
                for(int i=0;i<hist.size();++i){
                    data_builder.append(hist.get(i)).append("\n");
                }

                File hist_data_dir = new File(file.getParentFile().getParentFile().getAbsolutePath()+File.separator+"hists");
                File current_hist_data = new File(hist_data_dir.getAbsolutePath()+File.separator+file.getName()+".hgd");

                hist_data_dir.mkdirs();
                if(hist_data_dir.exists()){
                    WriteFile(current_hist_data,data_builder.toString().getBytes());
                }

            } else {
                // something was wrong, skip this image
            }

        } else {
            System.out.println("Invalid image data found in: "+file.getAbsolutePath());
        }
    }

    private boolean WriteFile(File current_hist_data, byte[] data) {
        boolean success = true;
        BufferedOutputStream buff_out = null;
        try{
            buff_out = new BufferedOutputStream(new FileOutputStream(current_hist_data));
            buff_out.write(data,0,data.length);
        }catch (Exception e){
            e.printStackTrace();
            success = false;
        }
        try{
            if(buff_out != null){
                buff_out.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return success;
    }
    private byte[] ReadFile(File current_hist_data) {
        byte[] data = new byte[((int) current_hist_data.length())];
        BufferedInputStream buff_in = null;
        try{
            buff_in = new BufferedInputStream(new FileInputStream(current_hist_data));
            buff_in.read(data,0,data.length);
        }catch (Exception e){
            e.printStackTrace();
            data = null;
        }
        try{
            if(buff_in != null){
                buff_in.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return data;
    }

    private List<Integer> getHistList(Image image){
        List<Integer> hist = new ArrayList<>(0);
        PixelReader reader = image.getPixelReader();
        int width = ((int) image.getWidth());
        int height = ((int) image.getHeight());
        for(int i=0;i<width;i+=8){
            for(int j=0;j<height;j+=8){
                List<Integer> unit_hist = new ArrayList<>(Collections.nCopies(256, 0));
                for(int x=i;x<i+7;++x){
                    for(int y=j;y<j+7;++y){
                        Color color = reader.getColor(x,y);
                        int gray_val = (int)(((color.getRed() + color.getGreen() + color.getBlue())/3.0)*255);
                        unit_hist.set(gray_val,unit_hist.get(gray_val)+1);
                    }
                }
                hist.addAll(unit_hist);
            }
        }
        return hist;
    }

    private Image getLBPOutput(Image image){

        int width = ((int) image.getWidth());
        int height = ((int) image.getHeight());

        // get handles for input and output images
        WritableImage w_img = new WritableImage(width, height);
        PixelWriter writer  =  w_img.getPixelWriter();
        PixelReader reader = image.getPixelReader();

        try{

            // set border pixels
            for(int i=0;i<width;++i){
                writer.setColor(i,0,Color.WHITE);
            }
            for(int i=0;i<width;++i){
                writer.setColor(i,height-1,Color.WHITE);
            }
            for(int i=0;i<height;++i){
                writer.setColor(0,i,Color.WHITE);
            }
            for(int i=0;i<height;++i){
                writer.setColor(width-1,i,Color.WHITE);
            }

            // now set inner pixels
            for(int i=1;i<width-1;++i){
                for(int j=1;j<height-1;++j){

                    // Define the 8 neighbors
                    Point center = new Point(i,j);
                    Point up = new Point(i,j-1);
                    Point down = new Point(i,j+1);
                    Point left = new Point(i-1,j);
                    Point right = new Point(i+1,j);
                    Point dig_left_up = new Point(i-1,j-1);
                    Point dig_right_down = new Point(i+1,j+1);
                    Point dig_left_down = new Point(i-1,j+1);
                    Point dig_right_up = new Point(i+1,j-1);

                    // get neighbor's colors
                    Color gray_center = reader.getColor(center.x,center.y);
                    Color gray_up = reader.getColor(up.x,up.y);
                    Color gray_down = reader.getColor(down.x,down.y);
                    Color gray_left = reader.getColor(left.x,left.y);
                    Color gray_right = reader.getColor(right.x,right.y);
                    Color gray_dig_left_up = reader.getColor(dig_left_up.x,dig_left_up.y);
                    Color gray_dig_right_down = reader.getColor(dig_right_down.x,dig_right_down.y);
                    Color gray_dig_left_down = reader.getColor(dig_left_down.x,dig_left_down.y);
                    Color gray_dig_right_up = reader.getColor(dig_right_up.x,dig_right_up.y);

                    // get threshold (average of 8-neighbors)
                    double threshold =
                            ((gray_up.getRed()+gray_up.getGreen()+gray_up.getBlue())/3.0 +
                                    (gray_down.getRed()+gray_down.getGreen()+gray_down.getBlue())/3.0 +
                                    (gray_left.getRed()+gray_left.getGreen()+gray_left.getBlue())/3.0 +
                                    (gray_right.getRed()+gray_right.getGreen()+gray_right.getBlue())/3.0 +
                                    (gray_dig_left_up.getRed()+gray_dig_left_up.getGreen()+gray_dig_left_up.getBlue())/3.0 +
                                    (gray_dig_right_down.getRed()+gray_dig_right_down.getGreen()+gray_dig_right_down.getBlue())/3.0 +
                                    (gray_dig_left_down.getRed()+gray_dig_left_down.getGreen()+gray_dig_left_down.getBlue())/3.0 +
                                    (gray_dig_right_up.getRed()+gray_dig_right_up.getGreen()+gray_dig_right_up.getBlue())/3.0)/8.0;

                    // get binary string
                    StringBuilder builder = new StringBuilder("");
                    builder.append(
                            (gray_dig_left_up.getRed()+gray_dig_left_up.getGreen()+gray_dig_left_up.getBlue())/3.0 >= threshold ? "1":"0"
                    ).append(
                            (gray_up.getRed()+gray_up.getGreen()+gray_up.getBlue())/3.0 >= threshold ? "1":"0"
                    ).append(
                            (gray_dig_right_up.getRed()+gray_dig_right_up.getGreen()+gray_dig_right_up.getBlue())/3.0 >= threshold ? "1":"0"
                    ).append(
                            (gray_left.getRed()+gray_left.getGreen()+gray_left.getBlue())/3.0 >= threshold ? "1":"0"
                    ).append(
                            (gray_right.getRed()+gray_right.getGreen()+gray_right.getBlue())/3.0 >= threshold ? "1":"0"
                    ).append(
                            (gray_dig_left_down.getRed()+gray_dig_left_down.getGreen()+gray_dig_left_down.getBlue())/3.0 >= threshold ? "1":"0"
                    ).append(
                            (gray_down.getRed()+gray_down.getGreen()+gray_down.getBlue())/3.0 >= threshold ? "1":"0"
                    ).append(
                            (gray_dig_right_down.getRed()+gray_dig_right_down.getGreen()+gray_dig_right_down.getBlue())/3.0 >= threshold ? "1":"0"
                    );

                    // set output pixel
                    writer.setColor(
                            center.x,
                            center.y,
                            Color.grayRgb(Integer.parseInt(builder.toString(),2))
                    );
                }
            }

        }catch (Exception e){
            e.printStackTrace();

            // if something is wrong, set return image as null
            w_img = null;
        }

        // if everything was okay, it will return the LBP output image, otherwise null
        return w_img;

    }

}
