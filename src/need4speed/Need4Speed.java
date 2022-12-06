
package need4speed;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.TickLabelOrientation;
import eu.hansolo.medusa.skins.ModernSkin;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Need4Speed extends Application {  
    
        // Widgets
    
    public Scene scene;
    public Gauge gauge;
    public Slider slider;
    public Button enabler;
    public Label direction;
    
        // Constants
    
    public String portName = "COM5";
    
    public ImageView imgLocked = new ImageView(new Image("locked.png"));
    public ImageView imgUnlocked = new ImageView(new Image("unlocked.png"));
    
    public int width = 450;
    public int height = 600;
    
    public String title = "Need 4' Speed";
    public String unit = "% PWM";
    
    public String labelOff = "Lock";
    public String labelOn = "Unlock";
    
    public String labelF = "Forward";
    public String labelB = "Reverse";
    
    public String labelRL = "Rotate (L)";
    public String labelRR = "Rotate (R)";
    
    public String labelFR = "Forward (R)";
    public String labelFL = "Forward (L)";
    
    public String labelBR = "Reverse (R)";
    public String labelBL = "Reverse (L)";
    
    public String labelP = "Parking";
    
    public int gaugeAnimationDuration = 2000;
    
    public int startSpeed = 0;
    
    public int delaySpeedUpdate = 50;
    
    public int scaleLower = 65;
    
        // Variables
    
    public int targetSpeed = startSpeed;
    public int currSpeed = startSpeed;
    public boolean globalLock = false;
    
    public boolean isExit = false;
    
    public boolean keyUp = false;
    public boolean keyDown = false;
    public boolean keyLeft = false;
    public boolean keyRight = false;
    
    public enum Direction { F, B, FR, FL, BR, BL, RR, RL, P }
    Direction currDir = Direction.P;
    
        // Temporary
    
    String tmp = "";
    
        // Connection
    
    public OutputStream out;
    public SerialPort serialPort;
    
    void connect() {
        try {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier( portName );
            if (portIdentifier.isCurrentlyOwned()) {
                System.out.println( "[Error] Port is currently in-use." );
            } else {
                int timeout = 5000;
                CommPort commPort = portIdentifier.open(portIdentifier.getName(), timeout );
                if (commPort instanceof SerialPort) {
                    serialPort = (SerialPort) commPort;
                    serialPort.setSerialPortParams( 9600, SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE );
                    out = serialPort.getOutputStream();
                    System.out.println("Connected.");
                } else {
                    System.out.println( "[Error] Only serial ports are handled." );
                }
            }   
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void start(Stage stage) {
        setEvents();
        
        new Thread(new Runnable() {
            @Override
            public void run() { 
                while(!isExit) {
                    try {
                        Thread.sleep(delaySpeedUpdate);
                        if (globalLock || currDir == Direction.P) {
                            if (currSpeed > 0) {
                                currSpeed--;   
                            }
                        } else {
                            if (currSpeed > targetSpeed) {
                                currSpeed--;
                            } else if (currSpeed < targetSpeed) {
                                currSpeed++;
                            }   
                        }
                        
                        // System.out.println("targetSpeed:   " + Integer.toString(targetSpeed));
                        // System.out.println("currSpeed:     " + Integer.toString(currSpeed));

                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                gauge.setValue(currSpeed);
                            }
                        });
                    } catch (InterruptedException e) {
                       e.printStackTrace();
                    }
                }
            }
        }).start();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(!isExit) {
                    try {
                        Thread.sleep(delaySpeedUpdate);
                        
                        if (keyUp) {
                           if (keyRight) {
                               currDir = Direction.FR;
                           } else if (keyLeft) {
                               currDir = Direction.FL;
                           } else {
                               currDir = Direction.F;
                           }
                        } else if (keyDown) {
                           if (keyRight) {
                               currDir = Direction.BR;
                           } else if (keyLeft) {
                               currDir = Direction.BL;
                           } else {
                               currDir = Direction.B;
                           }
                        } else {
                            if (keyRight) {
                                currDir = Direction.RR;
                            } else if (keyLeft) {
                                currDir = Direction.RL;
                            } else {
                                currDir = Direction.P;
                            }
                        }
                        
                        switch (currDir) {
                            case F:
                                tmp = labelF;
                                cmd("forward");
                                cmd(makeSpeed(currSpeed, currSpeed));
                                break;
                            case FR:
                                tmp = labelFR;
                                cmd("forward");
                                cmd(makeSpeed(currSpeed/2, currSpeed));
                                break;
                            case FL:
                                tmp = labelFL;
                                cmd("forward");
                                cmd(makeSpeed(currSpeed, currSpeed/2));
                                break;
                            case B:
                                tmp = labelB;
                                cmd("backward");
                                cmd(makeSpeed(currSpeed, currSpeed));
                                break;
                            case BR:
                                tmp = labelBR;
                                cmd("backward");
                                cmd(makeSpeed(currSpeed, currSpeed/2));
                                break;
                            case BL:
                                tmp = labelBL;
                                cmd("backward");
                                cmd(makeSpeed(currSpeed/2, currSpeed));
                                break;
                            case RR:
                                tmp = labelRR;
                                cmd("rotate right");
                                cmd(makeSpeed(currSpeed, currSpeed));
                                break;
                            case RL:
                                tmp = labelRL;
                                cmd("rotate left");
                                cmd(makeSpeed(currSpeed, currSpeed));
                                break;
                            case P:
                                tmp = labelP;
                                cmd(makeSpeed(currSpeed/2, currSpeed/2));
                                break;
                        }
                        
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                direction.setText(tmp);
                            }
                        });
                    } catch (InterruptedException e) {
                       e.printStackTrace();
                    }
                }
            }
        }).start();
        
        stage.setResizable(false);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.getIcons().add(new Image("icon.png"));
        stage.show();
    }
    
    public void setEvents() {
        slider.setOnMouseReleased(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                targetSpeed = (int) slider.getValue();
                
                //System.out.println("Slider value changed: " + Double.toString(slider.getValue()));
            }
        });
        
        enabler.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                globalLock = !globalLock;
                if (globalLock) {
                    enabler.setGraphic(imgLocked);
                } else {
                    enabler.setGraphic(imgUnlocked);
                }
                        
                //System.out.println("Enabler button clicked.");
            }
        });
        
        scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                switch (event.getCode()) {
                    case UP:
                        keyUp = true;
                        break;
                    case DOWN:
                        keyDown = true;
                        break;
                    case LEFT:
                        keyLeft = true;
                        break;
                    case RIGHT:
                        keyRight = true;
                        break;
                }
            }
        });

        scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                switch (event.getCode()) {
                    case UP:
                        keyUp = false;
                        break;
                    case DOWN:
                        keyDown = false;
                        break;
                    case LEFT:
                        keyLeft = false;
                        break;
                    case RIGHT:
                        keyRight = false;
                        break;
                }
            }
        });
    }
    
    void cmd(String str) {
        write(str);
        
        System.out.println(str);
    }
    
    public void write(String s) {
        s += "\r\n";
        try {
            for (int i = 0; i < s.length(); i++) {
                out.write(s.charAt(i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void init() throws Exception {
        super.init();
        
        connect();
        write("mode a");
        //write("slow");
        
        /************************************/
        
        setupGauge();
        
            // Controller
            
                // Slider
        
        slider = new Slider() {
            public void requestFocus() {}
        };
        slider.setId("slider");
        slider.setMax(100);
        slider.setMin(0);
        slider.setFocusTraversable(false);
        
                // Start-Stop & Direction-Feedback
                
        enabler = new Button() {
            public void requestFocus() {}
        };
        enabler.setId("enabler");
        enabler.setGraphic(imgUnlocked);
               
        direction = new Label(labelP);
        direction.setId("direction");
        
        BorderPane nonSlider = new BorderPane();
        nonSlider.setLeft(enabler);
        nonSlider.setCenter(direction);
                
        BorderPane controller = new BorderPane();
        controller.setBottom(getPadded(slider, 10));
        controller.setTop(getPadded(nonSlider, 10));
        
            // Main layout
        
        BorderPane root = new BorderPane();
        root.setCenter(getPadded(gauge, 10));
        root.setBottom(controller);
        
        scene = new Scene(root, width, height);
        scene.getStylesheets().add("stylesheet.css");
    }
    
    @Override
    public void stop(){
        isExit = true;
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        serialPort.close();
    }
    
    int scaleSpeed(int value) {
        return (int) ((((double) value) / 100.0) * ((double) (100 - scaleLower)) + scaleLower);
    }
    
    String makeSpeed(int speedL, int speedR) {
        return "speed " + Integer.toString(scaleSpeed(speedL)) + " " + Integer.toString(scaleSpeed(speedR));
    }
    
    VBox getSpacing(int value) {
        VBox box = new VBox();
        box.setPadding(new Insets(value, value, value, value));
        return box;
    }
    
    BorderPane getPadded(Node node, int value) {  
        BorderPane pane = new BorderPane();
        pane.setCenter(node);
        pane.setTop(getSpacing(value));
        pane.setBottom(getSpacing(value));
        pane.setRight(getSpacing(value));
        pane.setLeft(getSpacing(value));
        return pane;
    }
    
    void setupGauge() {
        gauge = new Gauge();
        gauge.setAnimated(false);
        //gauge.setAnimationDuration(gaugeAnimationDuration);
        gauge.setUnit(unit);
        gauge.setSkin(new ModernSkin(gauge));
        gauge.setValueColor(Color.WHITE); 
        gauge.setTitleColor(Color.WHITE); 
        gauge.setSubTitleColor(Color.WHITE); 
        gauge.setBarColor(Color.rgb(0, 214, 215)); 
        gauge.setNeedleColor(Color.RED); 
        gauge.setThresholdColor(Color.RED);  //color will become red if it crosses threshold value
        gauge.setThreshold(85);
        gauge.setThresholdVisible(true);
        gauge.setTickLabelColor(Color.rgb(151, 151, 151)); 
        gauge.setTickMarkColor(Color.WHITE); 
        gauge.setTickLabelOrientation(TickLabelOrientation.ORTHOGONAL);
        gauge.setUnitColor(Color.WHITE);
        // gauge.setDecimals(0);
        gauge.setValue((double) startSpeed);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
