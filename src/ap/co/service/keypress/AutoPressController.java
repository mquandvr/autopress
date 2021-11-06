package ap.co.service.keypress;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ap.co.service.keypress.util.AutoPressJNA;
import ap.co.service.keypress.util.WinRegistry;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.dispatcher.SwingDispatchService;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import org.apache.commons.lang3.StringUtils;

public class AutoPressController implements NativeKeyListener {

    @FXML // fx:id="txtMinutes"
    private TextField txtMinutes; // Value injected by FXMLLoader

    @FXML // fx:id="txtSeconds"
    private TextField txtSeconds; // Value injected by FXMLLoader

    @FXML // fx:id="txtNumberOfKeys"
    private TextField txtNumberOfKeys; // Value injected by FXMLLoader

    @FXML // fx:id="btnStart"
    private Button btnStart; // Value injected by FXMLLoader

    @FXML // fx:id="btnStop"
    private Button btnStop; // Value injected by FXMLLoader

    @FXML // fx:id="cbxKey1"
    private ComboBox<String> cbxKey1; // Value injected by FXMLLoader

    @FXML // fx:id="cbxKey1"
    private ComboBox<String> cbxShortcut; // Value injected by FXMLLoader

    @FXML // fx:id="txtTarget"
    private TextField txtTarget; // Value injected by FXMLLoader

    @FXML // fx:id="lblMsg"
    private Label lblMsg; // Value injected by FXMLLoader

    @FXML // fx:id="lblCount"
    private Label lblCount;

    @FXML
    private Label lblCurrentStatus;

    @FXML
    private Button btnSave;

    private ScheduledExecutorService schedule = null;

    private ScheduledFuture<?> scheduledFuture = null;

    private static final String currentStatusName = "Current Status: %s";

    private static final String msgDefault = "Please input %s!";

    private boolean isRunning = false;

    private int totalRun = 0;

    private final AutoPressJNA autoPressJNA = AutoPressJNA.getInstance();

    private final WinRegistry winRegistry = WinRegistry.getInstance();

    @FXML
        // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
        List<String> listKeyboard = new ArrayList<>();
        listKeyboard.addAll(Arrays.asList(AutoPressConstants.keyPressFArr));
        listKeyboard.addAll(Arrays.asList(AutoPressConstants.keyPressAlphaBetArr));
        listKeyboard.addAll(Arrays.asList(AutoPressConstants.keyPressNumberFArr));

        cbxKey1.getItems().addAll(listKeyboard);

        cbxShortcut.getItems().addAll(listKeyboard);

        readRegistryData();

        txtMinutes.textProperty().addListener((observableValue, oldValue, newValue) -> numberField(txtMinutes, newValue));

        txtSeconds.textProperty().addListener((observableValue, oldValue, newValue) -> numberField(txtSeconds, newValue));

        lblCurrentStatus.setText(String.format(currentStatusName, AutoPressConstants.STATUS_OFF));

        lblMsg.setText(StringUtils.EMPTY);

        lblCount.setText(String.format("Count press %s: %d", cbxKey1.getValue(), totalRun));

        btnStop.setDisable(true);

        listener();
    }

    public void listener() {
        try {
            GlobalScreen.setEventDispatcher(new SwingDispatchService());
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            e.printStackTrace();

            System.exit(1);
        }

        GlobalScreen.addNativeKeyListener(this);
    }

    @FXML
    void onStartClick() {
        lblMsg.setText("");
        if (!validate() && !isRunning) {
            return;
        }

        if (StringUtils.isNotBlank(txtTarget.getText()) && !autoPressJNA.checkWindowExist(txtTarget.getText())) {
            lblMsg.setText(String.format("App %s not found!", txtTarget.getText()));

            Platform.runLater(() -> btnStop.fire());
            return;
        }

        schedule();
    }

    private void schedule() {
        int minutes = Integer.parseInt(txtMinutes.getText());
        int seconds = Integer.parseInt(txtSeconds.getText());

        LocalTime time = LocalTime.of(0, minutes, seconds);

        schedule = Executors.newSingleThreadScheduledExecutor();

        scheduledFuture = schedule.scheduleAtFixedRate(this::task, 0, time.getSecond(), TimeUnit.SECONDS);

        btnStart.setDisable(true);
        btnStop.setDisable(false);

        lblCurrentStatus.setText(String.format(currentStatusName, AutoPressConstants.STATUS_ON));

        isRunning = true;
        totalRun = 0;
    }

    private void task() {
        int numberOfKeys = Integer.parseInt(txtNumberOfKeys.getText());
        if (numberOfKeys == 0 || totalRun < numberOfKeys) {

            autoPressJNA.postMessage(cbxKey1.getValue());
            totalRun++;
            Platform.runLater(() -> lblCount.setText(String.format("Count press %s: %d", cbxKey1.getValue(), totalRun)));
        } else {
            Platform.runLater(() -> btnStop.fire());
        }
    }

    @FXML
    void onStopClick() {
        if (isRunning) {
            scheduledFuture.cancel(true);
            schedule.shutdown();

            btnStart.setDisable(false);
            btnStop.setDisable(true);

            lblCurrentStatus.setText(String.format(currentStatusName, AutoPressConstants.STATUS_OFF));

            isRunning = false;
            totalRun = 0;
        }
    }

    @FXML
    void onSaveClick() throws InterruptedException {
        btnSave.setDisable(true);
        Task<Void> task = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                writeRegistryData();
                return null;
            }
        };
        task.setOnSucceeded(workerStateEvent -> {
            btnSave.setDisable(false);
        });
        new Thread(task).start();
    }

    @FXML
    void onClearClick() {
        cbxKey1.setValue(StringUtils.EMPTY);
        cbxShortcut.setValue(StringUtils.EMPTY);
        txtMinutes.setText(AutoPressConstants.ZERO);
        txtSeconds.setText(AutoPressConstants.ZERO);
        txtNumberOfKeys.setText(AutoPressConstants.ZERO);
        txtTarget.setText(StringUtils.EMPTY);

        writeRegistryData();
    }

    private void writeRegistryData() {
        winRegistry.writeRegistry(AutoPressConstants.REGISTRY_KEY_PRESS_KEY, cbxKey1.getValue());
        winRegistry.writeRegistry(AutoPressConstants.REGISTRY_SHORTCUT_KEY, cbxShortcut.getValue());
        winRegistry.writeRegistry(AutoPressConstants.REGISTRY_MINUTES_KEY, txtMinutes.getText());
        winRegistry.writeRegistry(AutoPressConstants.REGISTRY_SECONDS_KEY, txtSeconds.getText());
        winRegistry.writeRegistry(AutoPressConstants.REGISTRY_NUMBER_OF_KEYS_KEY, txtNumberOfKeys.getText());
        winRegistry.writeRegistry(AutoPressConstants.REGISTRY_TARGET_KEY, txtTarget.getText());
    }

    private void readRegistryData() {
        cbxKey1.setValue(winRegistry.readRegistry(AutoPressConstants.REGISTRY_KEY_PRESS_KEY));
        cbxShortcut.setValue(winRegistry.readRegistry(AutoPressConstants.REGISTRY_SHORTCUT_KEY));
        txtMinutes.setText(winRegistry.readRegistry(AutoPressConstants.REGISTRY_MINUTES_KEY));
        txtSeconds.setText(winRegistry.readRegistry(AutoPressConstants.REGISTRY_SECONDS_KEY));
        txtNumberOfKeys.setText(winRegistry.readRegistry(AutoPressConstants.REGISTRY_NUMBER_OF_KEYS_KEY));
        txtTarget.setText(winRegistry.readRegistry(AutoPressConstants.REGISTRY_TARGET_KEY));
    }

    private boolean validate() {
        if (StringUtils.isBlank(cbxKey1.getValue())) {
            lblMsg.setText(String.format(msgDefault, "Key of Automates"));
            return false;
        }

        if (!StringUtils.isNumeric(txtMinutes.getText())) {
            lblMsg.setText(String.format(msgDefault, "Minutes"));
            return false;
        }

        if (!StringUtils.isNumeric(txtSeconds.getText())) {
            lblMsg.setText(String.format(msgDefault, "Seconds"));
            return false;
        }

        if (txtMinutes.getText().equals("0") && txtSeconds.getText().equals("0")) {
            lblMsg.setText("Cant run with 0 minute 0 second!");
            return false;
        }

        if (!StringUtils.isNumeric(txtNumberOfKeys.getText())) {
            lblMsg.setText(String.format(msgDefault, "Number of Keys"));
            return false;
        }

        return true;
    }

    private void numberField(TextField txtField, String newValue) {
        if (!newValue.matches("\\d*")) {
            txtField.setText(newValue.replaceAll("[^\\d]", ""));
        }
        if (txtField.getText().length() > AutoPressConstants.TIME_MAXLENGTH) {
            String value = txtField.getText().substring(0, AutoPressConstants.TIME_MAXLENGTH);
            txtField.setText(value);
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeEvent) {
        if (cbxShortcut != null && StringUtils.isNotBlank(cbxShortcut.getValue())) {
            if (KeyCode.getKeyCode(NativeKeyEvent.getKeyText(nativeEvent.getKeyCode())).getCode()
                    == KeyCode.getKeyCode(cbxShortcut.getValue()).getCode()) {
                if (isRunning) {
                    Platform.runLater(() -> btnStop.fire());
                } else {
                    Platform.runLater(() -> btnStart.fire());
                }
            }
        }
    }
}