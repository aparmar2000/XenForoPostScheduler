package aparmar2000.xenforoposter.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.formdev.flatlaf.FlatLaf;

import aparmar2000.xenforoposter.extension.ExtensionManager;
import aparmar2000.xenforoposter.extension.toolbar.BbCodeToolbarItem;
import aparmar2000.xenforoposter.extension.toolbar.EditorContext;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodePreviewRenderer;

public class BbCodeEditorPanel extends JPanel {
	private static final long serialVersionUID = 9000475208358345971L;

	public static final String CARD_EDITOR = "EDITOR";
	public static final String CARD_PREVIEW = "PREVIEW";

	private final CardLayout cardLayout;
	private final JPanel cardsPanel;

	private final RSyntaxTextArea textArea;
	private final JEditorPane previewPane;
	private final JPanel toolbarPanel;
	private final JLabel statsLabel;
	private final JLabel previewStatsLabel;

	private final BbCodePreviewRenderer previewRenderer = new BbCodePreviewRenderer();
	private final ExtensionManager extensionManager;

	private boolean showingPreview = false;

	/**
	 * Design-time only constructor for Eclipse WindowBuilder preview.
	 * @wbp.parser.constructor
	 * @deprecated For WindowBuilder GUI designer and preview use only.
	 */
	@Deprecated
	@ApiStatus.Internal
	public BbCodeEditorPanel() {
		this(UiPreviewHelper.createPreviewExtensionManager());
		if (textArea.getText().isEmpty()) {
			setContent("[B]Sample Headline[/B]\n\nThis is a preview of the [COLOR=#007bff]BBCode editor[/COLOR] and live HTML renderer.\n\n[QUOTE=Preview]Formatted quote block[/QUOTE]");
		}
	}

	public BbCodeEditorPanel(@NotNull ExtensionManager extensionManager) {
		this.extensionManager = extensionManager;
		setLayout(new BorderLayout());

		cardLayout = new CardLayout();
		cardsPanel = new JPanel(cardLayout);

		// === Editor card
		JPanel editorCard = new JPanel(new BorderLayout());

		toolbarPanel = new JPanel(new BorderLayout());
		rebuildToolbar();

		textArea = new RSyntaxTextArea();
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
		textArea.setCodeFoldingEnabled(true);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setFont(new Font("Consolas", Font.PLAIN, 13));
		RTextScrollPane editorScrollPane = new RTextScrollPane(textArea);
		editorScrollPane.setBorder(BorderFactory.createTitledBorder("XenForo BBCode Editor"));

		statsLabel = new JLabel("Characters: 0 | Lines: 0 | BBCode Length: 0 chars");
		statsLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

		editorCard.add(toolbarPanel, BorderLayout.NORTH);
		editorCard.add(editorScrollPane, BorderLayout.CENTER);
		editorCard.add(statsLabel, BorderLayout.SOUTH);

		// === Preview card
		JPanel previewCard = new JPanel(new BorderLayout());

		JPanel previewToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
		JButton backToEditorBtn = new JButton("← Back to Editor");
		backToEditorBtn.setFont(backToEditorBtn.getFont().deriveFont(Font.BOLD));
		backToEditorBtn.addActionListener(e -> showEditorCard());
		previewToolbar.add(backToEditorBtn);

		previewToolbar.add(createVerticalSeparator());

		previewPane = new JEditorPane();
		previewPane.setContentType("text/html");
		previewPane.setEditable(false);
		JScrollPane previewScrollPane = new JScrollPane(previewPane);
		previewScrollPane.setBorder(BorderFactory.createTitledBorder("XenForo HTML Preview"));

		previewStatsLabel = new JLabel("Characters: 0 | Lines: 0 | BBCode Length: 0 chars");
		previewStatsLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

		previewCard.add(previewToolbar, BorderLayout.NORTH);
		previewCard.add(previewScrollPane, BorderLayout.CENTER);
		previewCard.add(previewStatsLabel, BorderLayout.SOUTH);

		// Add cards to container
		cardsPanel.add(editorCard, CARD_EDITOR);
		cardsPanel.add(previewCard, CARD_PREVIEW);

		add(cardsPanel, BorderLayout.CENTER);

		textArea.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateStats();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateStats();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateStats();
			}
		});

		extensionManager.addChangeListener(this::rebuildToolbar);

		updateStats();
	}

	public void rebuildToolbar() {
		toolbarPanel.removeAll();

		JPanel leftContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));

		// === Group 1: Basic Formatting
		JButton btnBold = new JButton("B");
		btnBold.setFont(btnBold.getFont().deriveFont(Font.BOLD));
		btnBold.setToolTipText("Bold ([B]text[/B])");
		btnBold.addActionListener(e -> wrapSelection("[B]", "[/B]"));

		JButton btnItalic = new JButton("I");
		btnItalic.setFont(btnItalic.getFont().deriveFont(Font.ITALIC));
		btnItalic.setToolTipText("Italic ([I]text[/I])");
		btnItalic.addActionListener(e -> wrapSelection("[I]", "[/I]"));

		JButton btnUnderline = new JButton("U");
		btnUnderline.setToolTipText("Underline ([U]text[/U])");
		btnUnderline.addActionListener(e -> wrapSelection("[U]", "[/U]"));

		JButton btnStrikethrough = new JButton("S");
		btnStrikethrough.setToolTipText("Strikethrough ([S]text[/S])");
		btnStrikethrough.addActionListener(e -> wrapSelection("[S]", "[/S]"));

		JButton btnColor = new JButton("Color");
		btnColor.setToolTipText("Text Color ([COLOR=#hex]text[/COLOR])");
		btnColor.addActionListener(e -> showColorChooser());

		// Font Size dropdown
		JPopupMenu sizeMenu = new JPopupMenu();
		String[][] fontSizes = {
				{"Size 1 (9px)", "1"},
				{"Size 2 (10px)", "2"},
				{"Size 3 (12px)", "3"},
				{"Size 4 (15px)", "4"},
				{"Size 5 (18px)", "5"},
				{"Size 6 (22px)", "6"},
				{"Size 7 (26px)", "7"}
		};
		for (String[] sizePair : fontSizes) {
			JMenuItem mi = new JMenuItem(sizePair[0]);
			String sizeTag = sizePair[1];
			mi.addActionListener(e -> wrapSelection("[SIZE=" + sizeTag + "]", "[/SIZE]"));
			sizeMenu.add(mi);
		}
		JButton btnFontSize = createMenuButton("Size ▾", "Font Size ([SIZE=x])", sizeMenu);

		// Font Family dropdown
		JPopupMenu fontMenu = new JPopupMenu();
		String[] fontFamilies = {
				"Arial", "Book Antiqua", "Courier New", "Georgia",
				"Helvetica", "Impact", "Tahoma", "Times New Roman", "Trebuchet MS", "Verdana"
		};
		for (String fontName : fontFamilies) {
			JMenuItem mi = new JMenuItem(fontName);
			mi.setFont(new Font(fontName, Font.PLAIN, 12));
			mi.addActionListener(e -> wrapSelection("[FONT='" + fontName + "']", "[/FONT]"));
			fontMenu.add(mi);
		}
		JButton btnFontFamily = createMenuButton("Font ▾", "Font Family ([FONT='name'])", fontMenu);

		// Heading dropdown
		JPopupMenu headingMenu = new JPopupMenu();
		for (int h = 1; h <= 6; h++) {
			final int level = h;
			JMenuItem mi = new JMenuItem("Heading " + level);
			mi.addActionListener(e -> wrapSelection("[HEADING=" + level + "]", "[/HEADING=" + level + "]"));
			headingMenu.add(mi);
		}
		JButton btnHeading = createMenuButton("Heading ▾", "Heading Format ([HEADING=x])", headingMenu);

		leftContainer.add(btnBold);
		leftContainer.add(btnItalic);
		leftContainer.add(btnUnderline);
		leftContainer.add(btnStrikethrough);
		leftContainer.add(btnColor);
		leftContainer.add(btnFontSize);
		leftContainer.add(btnFontFamily);
		leftContainer.add(btnHeading);

		leftContainer.add(createVerticalSeparator());
		// ===

		// === Group 2
		JButton btnLink = new JButton("Link");
		btnLink.setToolTipText("Insert Link ([URL='...']text[/URL])");
		btnLink.addActionListener(e -> showInsertLinkDialog());

		JButton btnImage = new JButton("Image");
		btnImage.setToolTipText("Insert Image ([IMG]...[/IMG])");
		btnImage.addActionListener(e -> showInsertImageDialog());

		// List dropdown
		JPopupMenu listMenu = new JPopupMenu();
		JMenuItem miOrdered = new JMenuItem("Ordered List (1, 2, 3)");
		miOrdered.addActionListener(e -> insertOrderedList());
		listMenu.add(miOrdered);

		JMenuItem miUnordered = new JMenuItem("Unordered List (Bullets)");
		miUnordered.addActionListener(e -> insertUnorderedList());
		listMenu.add(miUnordered);

		listMenu.addSeparator();

		JMenuItem miIndent = new JMenuItem("Indent");
		miIndent.addActionListener(e -> applyIndent());
		listMenu.add(miIndent);

		JMenuItem miOutdent = new JMenuItem("Outdent");
		miOutdent.addActionListener(e -> applyOutdent());
		listMenu.add(miOutdent);

		JButton btnList = createMenuButton("List ▾", "List & Indentation", listMenu);

		// Text Alignment dropdown
		JPopupMenu alignMenu = new JPopupMenu();
		JMenuItem miLeft = new JMenuItem("Align Left");
		miLeft.addActionListener(e -> wrapSelection("[LEFT]", "[/LEFT]"));
		alignMenu.add(miLeft);

		JMenuItem miCenter = new JMenuItem("Align Center");
		miCenter.addActionListener(e -> wrapSelection("[CENTER]", "[/CENTER]"));
		alignMenu.add(miCenter);

		JMenuItem miRight = new JMenuItem("Align Right");
		miRight.addActionListener(e -> wrapSelection("[RIGHT]", "[/RIGHT]"));
		alignMenu.add(miRight);

		JMenuItem miJustify = new JMenuItem("Justify");
		miJustify.addActionListener(e -> wrapSelection("[ALIGN=justify]", "[/ALIGN]"));
		alignMenu.add(miJustify);

		JButton btnAlign = createMenuButton("Align ▾", "Text Alignment", alignMenu);

		leftContainer.add(btnLink);
		leftContainer.add(btnImage);
		leftContainer.add(btnList);
		leftContainer.add(btnAlign);

		leftContainer.add(createVerticalSeparator());
		// ===

		// === Group 3: Insert Dropdown & Extensions
		JPopupMenu insertMenu = new JPopupMenu();
		JMenuItem miQuote = new JMenuItem("Quote");
		miQuote.addActionListener(e -> wrapSelection("[QUOTE]\n", "\n[/QUOTE]"));
		insertMenu.add(miQuote);

		JMenuItem miArticle = new JMenuItem("Article");
		miArticle.addActionListener(e -> wrapSelection("[ARTICLE]\n", "\n[/ARTICLE]"));
		insertMenu.add(miArticle);

		JMenuItem miTable = new JMenuItem("Insert Table");
		miTable.addActionListener(e -> insertAtCaret("\n[TABLE]\n[TR][TH]Header 1[/TH][TH]Header 2[/TH][TH]Header 3[/TH][/TR]\n[TR][TD]Value A[/TD][TD]Value B[/TD][TD]Value C[/TD][/TR]\n[TR][TD]Value D[/TD][TD]Value E[/TD][TD]Value F[/TD][/TR]\n[/TABLE]\n\n"));
		insertMenu.add(miTable);

		JMenuItem miHr = new JMenuItem("Horizontal Line");
		miHr.addActionListener(e -> insertAtCaret("\n[HR][/HR]\n"));
		insertMenu.add(miHr);

		JMenuItem miSpoiler = new JMenuItem("Spoiler");
		miSpoiler.addActionListener(e -> showInsertSpoilerDialog());
		insertMenu.add(miSpoiler);

		JMenuItem miISpoiler = new JMenuItem("Inline Spoiler");
		miISpoiler.addActionListener(e -> wrapSelection("[ISPOILER]", "[/ISPOILER]"));
		insertMenu.add(miISpoiler);

		JMenuItem miCode = new JMenuItem("Code Block");
		miCode.addActionListener(e -> wrapSelection("\n[CODE]\n", "\n[/CODE]\n"));
		insertMenu.add(miCode);

		JMenuItem miICode = new JMenuItem("Inline Code");
		miICode.addActionListener(e -> wrapSelection("[ICODE]", "[/ICODE]"));
		insertMenu.add(miICode);

		JButton btnInsert = createMenuButton("Insert ▾", "Insert BBCode Elements", insertMenu);

		// Extensions dropdown
		JPopupMenu extMenu = new JPopupMenu();
		List<BbCodeToolbarItem> extItems = extensionManager.getActiveToolbarItems();
		EditorContext context = createEditorContext();
		if (extItems.isEmpty()) {
			JMenuItem miNone = new JMenuItem("(No extension tools available)");
			miNone.setEnabled(false);
			extMenu.add(miNone);
		} else {
			for (BbCodeToolbarItem item : extItems) {
				JMenuItem mi = new JMenuItem(item.getLabel());
				if (item.getTooltip() != null) {
					mi.setToolTipText(item.getTooltip());
				}
				mi.addActionListener(e -> {
					item.execute(context);
					updateStats();
				});
				extMenu.add(mi);
			}
		}
		JButton btnExtensions = createMenuButton("Extensions ▾", "Extension-Added Toolbar Items", extMenu);

		leftContainer.add(btnInsert);
		leftContainer.add(btnExtensions);

		toolbarPanel.add(leftContainer, BorderLayout.CENTER);
		// ===

		// === Preview Button
		JPanel rightContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
		rightContainer.add(createVerticalSeparator());

		JButton previewBtn = new JButton("Preview Post →");
		previewBtn.setFont(previewBtn.getFont().deriveFont(Font.BOLD));
		previewBtn.setToolTipText("Switch to rendered HTML preview");
		previewBtn.addActionListener(e -> showPreviewCard());
		rightContainer.add(previewBtn);

		toolbarPanel.add(rightContainer, BorderLayout.EAST);
		// ===

		toolbarPanel.revalidate();
		toolbarPanel.repaint();
	}

	public void showPreviewCard() {
		updatePreview();
		cardLayout.show(cardsPanel, CARD_PREVIEW);
		showingPreview = true;
	}

	public void showEditorCard() {
		cardLayout.show(cardsPanel, CARD_EDITOR);
		showingPreview = false;
		textArea.requestFocusInWindow();
	}

	public boolean isShowingPreview() {
		return showingPreview;
	}

	public CardLayout getCardLayout() {
		return cardLayout;
	}

	public JPanel getCardsPanel() {
		return cardsPanel;
	}

	public RSyntaxTextArea getTextArea() {
		return textArea;
	}

	public JEditorPane getPreviewPane() {
		return previewPane;
	}

	public void updateStats() {
		String bbCode = textArea.getText();
		String stats = String.format("Characters: %d | Lines: %d | BBCode Length: %d chars",
				bbCode.length(), textArea.getLineCount(), bbCode.length());
		statsLabel.setText(stats);
		if (previewStatsLabel != null) {
			previewStatsLabel.setText(stats);
		}
	}

	public void updatePreview() {
		String bbCode = textArea.getText();
		boolean isDark = FlatLaf.isLafDark();
		String html = previewRenderer.renderToHtml(bbCode, isDark);
		previewPane.setText(html);
		previewPane.setCaretPosition(0);
		updateStats();
	}

	public String getContent() {
		return textArea.getText();
	}

	public void setContent(String content) {
		textArea.setText(content != null ? content : "");
		textArea.setCaretPosition(0);
		updateStats();
		if (showingPreview) {
			updatePreview();
		}
	}

	private JSeparator createVerticalSeparator() {
		JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
		sep.setPreferredSize(new Dimension(2, 22));
		return sep;
	}

	private JButton createMenuButton(@NotNull String label, @Nullable String tooltip, @NotNull JPopupMenu popupMenu) {
		JButton button = new JButton(label);
		if (tooltip != null) {
			button.setToolTipText(tooltip);
		}
		button.addActionListener(e -> popupMenu.show(button, 0, button.getHeight()));
		return button;
	}

	private void showColorChooser() {
		Color selectedColor = JColorChooser.showDialog(this, "Select Text Color", Color.BLACK);
		if (selectedColor != null) {
			String hex = String.format("#%02X%02X%02X", selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue());
			wrapSelection("[COLOR=" + hex + "]", "[/COLOR]");
		}
	}

	private void showInsertLinkDialog() {
		String selectedText = textArea.getSelectedText();
		if (selectedText == null) {
			selectedText = "";
		}
		String initialUrl = (selectedText.startsWith("http://") || selectedText.startsWith("https://")) ? selectedText : "https://";
		String initialText = (selectedText.startsWith("http://") || selectedText.startsWith("https://")) ? "" : selectedText;

		JTextField urlField = new JTextField(initialUrl, 25);
		JTextField textField = new JTextField(initialText, 25);

		JPanel panel = new JPanel(new GridLayout(2, 2, 6, 6));
		panel.add(new JLabel("URL:"));
		panel.add(urlField);
		panel.add(new JLabel("Text:"));
		panel.add(textField);

		int result = JOptionPane.showConfirmDialog(this, panel, "Insert Link", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {
			String url = urlField.getText().trim();
			String text = textField.getText().trim();
			if (!url.isEmpty()) {
				if (!text.isEmpty()) {
					textArea.replaceSelection("[URL='" + url + "']" + text + "[/URL]");
				} else {
					textArea.replaceSelection("[URL]" + url + "[/URL]");
				}
				updateStats();
				textArea.requestFocusInWindow();
			}
		}
	}

	private void showInsertImageDialog() {
		String selectedText = textArea.getSelectedText();
		String initialUrl = (selectedText != null && (selectedText.startsWith("http://") || selectedText.startsWith("https://"))) ? selectedText : "https://";

		JTextField urlField = new JTextField(initialUrl, 25);
		JPanel panel = new JPanel(new GridLayout(1, 2, 6, 6));
		panel.add(new JLabel("Image URL:"));
		panel.add(urlField);

		int result = JOptionPane.showConfirmDialog(this, panel, "Insert Image", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {
			String url = urlField.getText().trim();
			if (!url.isEmpty()) {
				textArea.replaceSelection("[IMG]" + url + "[/IMG]");
				updateStats();
				textArea.requestFocusInWindow();
			}
		}
	}

	private void showInsertSpoilerDialog() {
		String title = JOptionPane.showInputDialog(this, "Spoiler Title (optional):", "Insert Spoiler", JOptionPane.PLAIN_MESSAGE);
		if (title != null) {
			String trimmed = title.trim();
			String prefix = trimmed.isEmpty() ? "[SPOILER]\n" : "[SPOILER='" + trimmed + "']\n";
			String suffix = "\n[/SPOILER]";
			wrapSelection(prefix, suffix);
		}
	}

	private void insertOrderedList() {
		String sel = textArea.getSelectedText();
		if (sel == null || sel.trim().isEmpty()) {
			insertAtCaret("\n[LIST=1]\n[*]Item 1\n[*]Item 2\n[*]Item 3\n[/LIST]\n");
		} else {
			String[] lines = sel.split("\n");
			StringBuilder sb = new StringBuilder("\n[LIST=1]\n");
			for (String line : lines) {
				if (!line.trim().isEmpty()) {
					sb.append("[*]").append(line.trim()).append("\n");
				}
			}
			sb.append("[/LIST]\n");
			textArea.replaceSelection(sb.toString());
			updateStats();
			textArea.requestFocusInWindow();
		}
	}

	private void insertUnorderedList() {
		String sel = textArea.getSelectedText();
		if (sel == null || sel.trim().isEmpty()) {
			insertAtCaret("\n[LIST]\n[*]Item 1\n[*]Item 2\n[*]Item 3\n[/LIST]\n");
		} else {
			String[] lines = sel.split("\n");
			StringBuilder sb = new StringBuilder("\n[LIST]\n");
			for (String line : lines) {
				if (!line.trim().isEmpty()) {
					sb.append("[*]").append(line.trim()).append("\n");
				}
			}
			sb.append("[/LIST]\n");
			textArea.replaceSelection(sb.toString());
			updateStats();
			textArea.requestFocusInWindow();
		}
	}

	private void applyIndent() {
		wrapSelection("[INDENT]", "[/INDENT]");
	}

	private void applyOutdent() {
		String sel = textArea.getSelectedText();
		if (sel != null && !sel.isEmpty()) {
			String unindented = sel;
			if (unindented.startsWith("[INDENT]") && unindented.endsWith("[/INDENT]")) {
				unindented = unindented.substring(8, unindented.length() - 9);
			} else {
				unindented = unindented.replace("[INDENT]", "").replace("[/INDENT]", "");
			}
			textArea.replaceSelection(unindented);
		} else {
			int pos = textArea.getCaretPosition();
			String text = textArea.getText();
			if (pos >= 8 && text.startsWith("[INDENT]", pos - 8)) {
				textArea.replaceRange("", pos - 8, pos);
			}
		}
		updateStats();
		textArea.requestFocusInWindow();
	}

	private void wrapSelection(@NotNull String prefix, @NotNull String suffix) {
		String sel = textArea.getSelectedText();
		if (sel != null && !sel.isEmpty()) {
			textArea.replaceSelection(prefix + sel + suffix);
		} else {
			int pos = textArea.getCaretPosition();
			textArea.insert(prefix + suffix, pos);
			textArea.setCaretPosition(pos + prefix.length());
		}
		updateStats();
		textArea.requestFocusInWindow();
	}

	private void insertAtCaret(@NotNull String text) {
		int pos = textArea.getCaretPosition();
		textArea.insert(text, pos);
		textArea.setCaretPosition(pos + text.length());
		updateStats();
		textArea.requestFocusInWindow();
	}

	private EditorContext createEditorContext() {
		return new EditorContext() {
			@Override
			public @NotNull String getText() {
				return textArea.getText();
			}

			@Override
			public @NotNull String getSelectedText() {
				String sel = textArea.getSelectedText();
				return sel != null ? sel : "";
			}

			@Override
			public void replaceSelection(@NotNull String newText) {
				textArea.replaceSelection(newText);
			}

			@Override
			public void insertAtCaret(@NotNull String text) {
				textArea.insert(text, textArea.getCaretPosition());
			}

			@Override
			public int getCaretPosition() {
				return textArea.getCaretPosition();
			}

			@Override
			public void setCaretPosition(int position) {
				textArea.setCaretPosition(Math.max(0, Math.min(position, textArea.getText().length())));
			}

			@Override
			public void wrapSelection(@NotNull String prefix, @NotNull String suffix) {
				String sel = textArea.getSelectedText();
				if (sel != null && !sel.isEmpty()) {
					textArea.replaceSelection(prefix + sel + suffix);
				} else {
					int pos = textArea.getCaretPosition();
					textArea.insert(prefix + suffix, pos);
					textArea.setCaretPosition(pos + prefix.length());
				}
			}
		};
	}

	/**
	 * Standalone launcher for WindowBuilder GUI design preview.
	 */
	public static void main(String[] args) {
		UiPreviewHelper.showPreviewFrame(new BbCodeEditorPanel(), "BBCode Editor & Preview", 900, 600);
	}
}
