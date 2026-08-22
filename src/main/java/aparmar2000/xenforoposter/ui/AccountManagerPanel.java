package aparmar2000.xenforoposter.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import aparmar2000.xenforoposter.model.ForumProfile;
import aparmar2000.xenforoposter.scheduler.SchedulerEngine;
import aparmar2000.xenforoposter.security.SecureString;
import aparmar2000.xenforoposter.web.XenForoWebClient;

public class AccountManagerPanel extends JPanel {
	private static final long serialVersionUID = 9068706118347396970L;

	private final SchedulerEngine schedulerEngine;
	private final XenForoWebClient webClient;
	private final JTable profileTable;
	private final DefaultTableModel tableModel;
	private final List<ForumProfile> currentProfiles = new ArrayList<>();

	/**
	 * Design-time only constructor for Eclipse WindowBuilder preview.
	 * @wbp.parser.constructor
	 * @deprecated For WindowBuilder GUI designer and preview use only.
	 */
	@Deprecated
	@ApiStatus.Internal
	public AccountManagerPanel() {
		this(UiPreviewHelper.createPreviewSchedulerEngine(), UiPreviewHelper.createPreviewWebClient());
	}

	public AccountManagerPanel(@NotNull SchedulerEngine schedulerEngine, @NotNull XenForoWebClient webClient) {
		this.schedulerEngine = schedulerEngine;
		this.webClient = webClient;
		setLayout(new BorderLayout(5, 5));

		// Profiles Table
		String[] cols = {"Name", "Base URL", "Username", "Cookies Saved", "ID"};
		tableModel = new DefaultTableModel(cols, 0) {
			@Override
			public boolean isCellEditable(int row, int col) {
				return false;
			}
		};

		profileTable = new JTable(tableModel);
		profileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		profileTable.setRowHeight(24);

		// Top Toolbar
		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton addBtn = new JButton("+ Add Forum Account");
		addBtn.addActionListener(e -> showProfileDialog(null));
		toolbar.add(addBtn);

		JButton editBtn = new JButton("Edit");
		editBtn.addActionListener(e -> {
			int row = profileTable.getSelectedRow();
			if (row >= 0 && row < currentProfiles.size()) {
				showProfileDialog(currentProfiles.get(row));
			}
		});
		toolbar.add(editBtn);

		JButton deleteBtn = new JButton("Delete");
		deleteBtn.addActionListener(e -> {
			int row = profileTable.getSelectedRow();
			if (row >= 0 && row < currentProfiles.size()) {
				ForumProfile p = currentProfiles.get(row);
				int confirm = JOptionPane.showConfirmDialog(this,
						"Delete account profile: " + p.getName() + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
				if (confirm == JOptionPane.YES_OPTION) {
					schedulerEngine.deleteProfile(p.getId());
					refreshTable();
				}
			}
		});
		toolbar.add(deleteBtn);

		JButton testLoginBtn = new JButton("Test Login / Refresh Cookies");
		testLoginBtn.addActionListener(e -> testLoginSelectedProfile());
		toolbar.add(testLoginBtn);

		add(toolbar, BorderLayout.NORTH);
		add(new JScrollPane(profileTable), BorderLayout.CENTER);

		schedulerEngine.addListener(new SchedulerEngine.JobUpdateListener() {
			@Override
			public void onJobsChanged() {
				refreshTable();
			}

			@Override
			public void onJobUpdated(@NotNull aparmar2000.xenforoposter.model.ScheduledJob job) {}
		});

		refreshTable();
	}

	public void refreshTable() {
		currentProfiles.clear();
		currentProfiles.addAll(schedulerEngine.getProfiles());
		tableModel.setRowCount(0);

		for (ForumProfile p : currentProfiles) {
			boolean hasCookies = p.getSessionCookies() != null && !p.getSessionCookies().isEmpty();
			tableModel.addRow(new Object[]{
					p.getName(),
					p.getBaseUrl(),
					p.getUsername() != null ? p.getUsername() : "(None)",
							hasCookies ? "Yes (" + p.getSessionCookies().size() + ")" : "No",
									p.getId()
			});
		}
	}

	private void showProfileDialog(ForumProfile existing) {
		JTextField nameField = new JTextField(existing != null ? existing.getName() : "My XenForo Forum", 20);
		JTextField urlField = new JTextField(existing != null ? existing.getBaseUrl() : "https://forum.example.com/", 25);
		JTextField userField = new JTextField(existing != null && existing.getUsername() != null ? existing.getUsername() : "", 20);
		JPasswordField passField = new JPasswordField(existing != null && existing.getPassword() != null && existing.getPassword().getClearText() != null ? existing.getPassword().getClearText() : "", 20);
		JTextField userAgentField = new JTextField(existing != null && existing.getCustomUserAgent() != null ? existing.getCustomUserAgent() : "", 25);

		JPanel panel = new JPanel(new GridLayout(5, 2, 6, 6));
		panel.add(new JLabel("Profile Name:"));
		panel.add(nameField);
		panel.add(new JLabel("Forum Base URL:"));
		panel.add(urlField);
		panel.add(new JLabel("Username:"));
		panel.add(userField);
		panel.add(new JLabel("Password:"));
		panel.add(passField);
		panel.add(new JLabel("Custom User Agent (optional):"));
		panel.add(userAgentField);

		int res = JOptionPane.showConfirmDialog(this, panel,
				existing == null ? "Add Forum Profile" : "Edit Forum Profile",
						JOptionPane.OK_CANCEL_OPTION);

		if (res == JOptionPane.OK_OPTION) {
			String name = nameField.getText().trim();
			String url = urlField.getText().trim();
			if (name.isEmpty() || url.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Profile Name and Forum URL are required!", "Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			String passwordInput = new String(passField.getPassword()).trim();
			ForumProfile.ForumProfileBuilder b = existing != null ? existing.toBuilder() : ForumProfile.builder();
			b.name(name)
			.baseUrl(url)
			.username(userField.getText().trim().isEmpty() ? null : userField.getText().trim())
			.password(passwordInput.isEmpty() ? null : SecureString.of(passwordInput))
			.customUserAgent(userAgentField.getText().trim().isEmpty() ? null : userAgentField.getText().trim());

			schedulerEngine.addOrUpdateProfile(b.build());
			refreshTable();
		}
	}

	private void testLoginSelectedProfile() {
		int row = profileTable.getSelectedRow();
		if (row < 0 || row >= currentProfiles.size()) {
			JOptionPane.showMessageDialog(this, "Please select an account from the table first.", "Info", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		ForumProfile profile = currentProfiles.get(row);
		XenForoWebClient.LoginResult result = webClient.login(profile);
		if (result.isSuccessful()) {
			ForumProfile updated = profile.toBuilder()
					.sessionCookies(result.getCookies())
					.build();
			schedulerEngine.addOrUpdateProfile(updated);
			JOptionPane.showMessageDialog(this, "Login successful! Session cookies stored.", "Success", JOptionPane.INFORMATION_MESSAGE);
			refreshTable();
		} else {
			JOptionPane.showMessageDialog(this, "Login failed:\n" + result.getErrorMessage(), "Login Failed", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Standalone launcher for WindowBuilder GUI design preview.
	 */
	public static void main(String[] args) {
		UiPreviewHelper.showPreviewFrame(new AccountManagerPanel(), "Forum Account Manager", 800, 500);
	}
}
