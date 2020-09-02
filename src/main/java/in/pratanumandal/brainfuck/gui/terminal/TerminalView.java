package in.pratanumandal.brainfuck.gui.terminal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.javafx.scene.control.ContextMenuContent;
import in.pratanumandal.brainfuck.gui.terminal.annotation.WebkitCall;
import in.pratanumandal.brainfuck.gui.terminal.config.TerminalConfig;
import in.pratanumandal.brainfuck.gui.terminal.helper.ThreadHelper;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.PopupWindow;
import javafx.stage.Window;
import netscape.javascript.JSObject;

import java.io.Reader;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class TerminalView extends Pane {

	private final WebView webView;
	private final ReadOnlyIntegerWrapper columnsProperty;
	private final ReadOnlyIntegerWrapper rowsProperty;
	private final ObjectProperty<Reader> inputReaderProperty;
	private final ObjectProperty<Reader> errorReaderProperty;
	private TerminalConfig terminalConfig = new TerminalConfig();
	protected final CountDownLatch countDownLatch = new CountDownLatch(1);
	protected final Object printLock = new Object();

	public static final String SELECT_TEXT =
			"(function getSelectionText() {\n" +
					"    var text = \"\";\n" +
					"    if (window.getSelection) {\n" +
					"        text = window.getSelection().toString();\n" +
					"    } else if (document.selection && document.selection.type != \"Control\") {\n" +
					"        text = document.selection.createRange().text;\n" +
					"    }\n" +
					"    if (window.getSelection) {\n" +
					"      if (window.getSelection().empty) {  // Chrome\n" +
					"        window.getSelection().empty();\n" +
					"      } else if (window.getSelection().removeAllRanges) {  // Firefox\n" +
					"        window.getSelection().removeAllRanges();\n" +
					"      }\n" +
					"    } else if (document.selection) {  // IE?\n" +
					"      document.selection.empty();\n" +
					"    }" +
					"    return text;\n" +
					"})()";

	public TerminalView() {
		webView = new WebView();
		columnsProperty = new ReadOnlyIntegerWrapper(150);
		rowsProperty = new ReadOnlyIntegerWrapper(10);
		inputReaderProperty = new SimpleObjectProperty<>();
		errorReaderProperty = new SimpleObjectProperty<>();

		inputReaderProperty.addListener((observable, oldValue, newValue) -> {
			ThreadHelper.start(() -> {
				printReader(newValue);
			});
		});

		errorReaderProperty.addListener((observable, oldValue, newValue) -> {
			ThreadHelper.start(() -> {
				printReader(newValue);
			});
		});

		webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
			getWindow().setMember("app", this);
		});
		webView.prefHeightProperty().bind(heightProperty());
		webView.prefWidthProperty().bind(widthProperty());

		webEngine().load(getClass().getClassLoader().getResource("html/hterm.html").toExternalForm());
		
		webView.setOnContextMenuRequested(e -> getPopupWindow());
	}

	private PopupWindow getPopupWindow() {
		final ObservableList<Window> windows = Window.getWindows();

		for (Window window : windows) {
			if (window instanceof ContextMenu) {
				if(window.getScene()!=null && window.getScene().getRoot()!=null){
					Parent root = window.getScene().getRoot();

					if (root.getChildrenUnmodifiable().size() > 0) {
						Node popup = root.getChildrenUnmodifiable().get(0);
						if (popup.lookup(".context-menu") != null) {
							Node bridge = popup.lookup(".context-menu");
							ContextMenuContent cmc= (ContextMenuContent)((Parent)bridge).getChildrenUnmodifiable().get(0);

							VBox itemsContainer = cmc.getItemsContainer();
							ObservableList<Node> children = itemsContainer.getChildren();
							Iterator<Node> iterator = children.iterator();

							while (iterator.hasNext()) {
								Node node = iterator.next();
								ContextMenuContent.MenuItemContainer item = (ContextMenuContent.MenuItemContainer) node;
								MenuItem menuItem = item.getItem();
								if (menuItem.getText().equals("Cut")) {
									iterator.remove();
									break;
								}
							}

							return (PopupWindow)window;
						}
					}
				}
				return null;
			}
		}
		return null;
	}

	@WebkitCall(from = "hterm")
	public String getPrefs() {
		try {
			return new ObjectMapper().writeValueAsString(getTerminalConfig());
		} catch(final Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void updatePrefs(TerminalConfig terminalConfig) {
		if(getTerminalConfig().equals(terminalConfig)) {
			return;
		}

		setTerminalConfig(terminalConfig);
		final String prefs = getPrefs();

		ThreadHelper.runActionLater(() -> {
			try {
				getWindow().call("updatePrefs", prefs);
			} catch(final Exception e) {
				e.printStackTrace();
			}
		}, true);
	}

	@WebkitCall(from = "hterm")
	public void resizeTerminal(int columns, int rows) {
		columnsProperty.set(columns);
		rowsProperty.set(rows);
	}

	@WebkitCall
	public void onTerminalInit() {
		ThreadHelper.runActionLater(() -> {
			getChildren().add(webView);
		}, true);
	}

	@WebkitCall
	/**
	 * Internal use only
	 */
	public void onTerminalReady() {
		ThreadHelper.start(() -> {
			try {
				focusCursor();
				countDownLatch.countDown();
			} catch(final Exception e) {
			}
		});
	}

	private void printReader(Reader bufferedReader) {
		try {
			int nRead;
			final char[] data = new char[1 * 1024];

			synchronized (printLock) {
				while ((nRead = bufferedReader.read(data, 0, data.length)) != -1) {
					final StringBuilder builder = new StringBuilder(nRead);
					builder.append(data, 0, nRead);
					print(builder.toString());
				}
			}

		} catch(final Exception e) {
			e.printStackTrace();
		}
	}

	@WebkitCall(from = "hterm")
	public void copy(String text) {
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		final ClipboardContent clipboardContent = new ClipboardContent();
		clipboardContent.putString(text);
		clipboard.setContent(clipboardContent);
	}

	public void onTerminalFxReady(Runnable onReadyAction) {
		ThreadHelper.start(() -> {
			ThreadHelper.awaitLatch(countDownLatch);

			if(Objects.nonNull(onReadyAction)) {
				ThreadHelper.start(onReadyAction);
			}
		});
	}

	protected void print(String text) {
		ThreadHelper.awaitLatch(countDownLatch);
		ThreadHelper.runActionLater(() -> {
			getTerminalIO().call("print", text);
		});
	}

	public void printText(String text) {
		synchronized (printLock) {
			print(text);
		}
	}

	public void focusCursor() {
		ThreadHelper.runActionLater(() -> {
			webView.requestFocus();
			getTerminal().call("focus");
		}, true);
	}

	private JSObject getTerminal() {
		return (JSObject) webEngine().executeScript("t");
	}

	private JSObject getTerminalIO() {
		return (JSObject) webEngine().executeScript("t.io");
	}

	public JSObject getWindow() {
		return (JSObject) webEngine().executeScript("window");
	}

	private WebEngine webEngine() {
		return webView.getEngine();
	}

	public TerminalConfig getTerminalConfig() {
		if(Objects.isNull(terminalConfig)) {
			terminalConfig = new TerminalConfig();
		}
		return terminalConfig;
	}

	public void setTerminalConfig(TerminalConfig terminalConfig) {
		this.terminalConfig = terminalConfig;
	}

	public ReadOnlyIntegerProperty columnsProperty() {
		return columnsProperty.getReadOnlyProperty();
	}

	public int getColumns() {
		return columnsProperty.get();
	}

	public ReadOnlyIntegerProperty rowsProperty() {
		return rowsProperty.getReadOnlyProperty();
	}

	public int getRows() {
		return rowsProperty.get();
	}

	public ObjectProperty<Reader> inputReaderProperty() {
		return inputReaderProperty;
	}

	public Reader getInputReader() {
		return inputReaderProperty.get();
	}

	public void setInputReader(Reader reader) {
		inputReaderProperty.set(reader);
	}

	public ObjectProperty<Reader> errorReaderProperty() {
		return errorReaderProperty;
	}

	public Reader getErrorReader() {
		return errorReaderProperty.get();
	}

	public void setErrorReader(Reader reader) {
		errorReaderProperty.set(reader);
	}

}