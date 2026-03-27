import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class BugTrackerApp extends JFrame {

    // 1) Set your MySQL credentials here
    private static final String DB_USER = "root";
    private static final String DB_PASS = "swami@06";

    // MySQL JDBC URLs
    private static final String DB_ROOT_URL =
            "jdbc:mysql://localhost:3306/bug_tracker_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_URL ="jdbc:mysql://localhost:3306/bug_tracker_db?useSSL=false&serverTimezone=UTC";
    public static String getDB_URL() {
        return DB_URL;
    }

    private final JTextField titleField = new JTextField(24);
    private final JTextArea descriptionArea = new JTextArea(5, 24);
    private final JTextField reporterField = new JTextField(24);
    private final JTextField assigneeField = new JTextField(24);

    private final JComboBox<String> priorityBox =
            new JComboBox<>(new String[]{"LOW", "MEDIUM", "HIGH", "CRITICAL"});
    private final JComboBox<String> statusBox =
            new JComboBox<>(new String[]{"OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"});

    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> filterStatusBox =
            new JComboBox<>(new String[]{"ALL", "OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"});

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Title", "Priority", "Status", "Reporter", "Assignee", "Updated At"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable table = new JTable(model);
    private int selectedId = -1;

    public BugTrackerApp() {
        super("Issue Tracking and Bug Management (Java + Swing + JDBC + MySQL)");
        setupLookAndFeel();
        buildUI();
        initDatabase();
        loadBugs("", "ALL");
    }

    private void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException ignored) {
        }
    }

    private void buildUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 760);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("Issue Tracking and Bug Management", SwingConstants.CENTER);
        title.setFont(new Font("Segos UI", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(title, BorderLayout.NORTH);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        root.add(buildFormPanel(), BorderLayout.WEST);
        root.add(buildTablePanel(), BorderLayout.CENTER);
        add(root, BorderLayout.CENTER);
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(420, 620));
        panel.setBorder(BorderFactory.createTitledBorder("Bug Details"));

        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int row = 0;
        addField(panel, gbc, row++, "Title*", titleField);
        addField(panel, gbc, row++, "Reporter*", reporterField);
        addField(panel, gbc, row++, "Assignee", assigneeField);
        addField(panel, gbc, row++, "Priority", priorityBox);
        addField(panel, gbc, row++, "Status", statusBox);

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Description*"), gbc);

        gbc.gridy = row++;
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setPreferredSize(new Dimension(360, 130));
        panel.add(descScroll, gbc);

        gbc.gridy = row++;
        panel.add(buildActionButtons(), gbc);

        gbc.gridy = row++;
        panel.add(buildStatusButtons(), gbc);

        gbc.gridy = row;
        gbc.weighty = 1;
        panel.add(new JPanel(), gbc);

        return panel;
    }

    private JPanel buildActionButtons() {
        JPanel p = new JPanel();

        JButton addBtn = new JButton("Add");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");
        JButton clearBtn = new JButton("Clear");

        // Event handling
        addBtn.addActionListener(e -> addBug());
        updateBtn.addActionListener(e -> updateBug());
        deleteBtn.addActionListener(e -> deleteBug());
        clearBtn.addActionListener(e -> clearForm());

        p.add(addBtn);
        p.add(updateBtn);
        p.add(deleteBtn);
        p.add(clearBtn);
        return p;
    }

    private JPanel buildStatusButtons() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder("Quick Status"));

        JButton inProgress = new JButton("IN_PROGRESS");
        JButton resolved = new JButton("RESOLVED");
        JButton closed = new JButton("CLOSED");

        // Event handling
        inProgress.addActionListener(e -> updateSelectedStatus("IN_PROGRESS"));
        resolved.addActionListener(e -> updateSelectedStatus("RESOLVED"));
        closed.addActionListener(e -> updateSelectedStatus("CLOSED"));

        p.add(inProgress);
        p.add(resolved);
        p.add(closed);
        return p;
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Bug List"));

        JPanel searchPanel = new JPanel();
        JButton searchBtn = new JButton("Search");
        JButton refreshBtn = new JButton("Refresh");

        // Event handling
        searchBtn.addActionListener(e ->
                loadBugs(searchField.getText().trim(), (String) filterStatusBox.getSelectedItem()));
        refreshBtn.addActionListener(e -> loadBugs("", "ALL"));

        searchPanel.add(new JLabel("Keyword:"));
        searchPanel.add(searchField);
        searchPanel.add(new JLabel("Status:"));
        searchPanel.add(filterStatusBox);
        searchPanel.add(searchBtn);
        searchPanel.add(refreshBtn);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelectedRowIntoForm();
        });

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void addField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.25;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.75;
        panel.add(comp, gbc);
    }

    // ---------------- DB ----------------

    private void initDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(DB_ROOT_URL, DB_USER, DB_PASS);
                 Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE DATABASE IF NOT EXISTS bug_tracker_db");
            }

            String createTable = "CREATE TABLE IF NOT EXISTS bugs (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "title VARCHAR(200) NOT NULL," +
                    "description TEXT NOT NULL," +
                    "reporter VARCHAR(100) NOT NULL," +
                    "assignee VARCHAR(100) DEFAULT ''," +
                    "priority ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM'," +
                    "status ENUM('OPEN','IN_PROGRESS','RESOLVED','CLOSED') NOT NULL DEFAULT 'OPEN'," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ")";

            try (Connection conn = getConnection();
                 Statement st = conn.createStatement()) {
                st.executeUpdate(createTable);
            }

        } catch (ClassNotFoundException | SQLException ex) {
            showError("Database initialization failed", ex);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    // ---------------- CRUD ----------------

    private void addBug() {
        if (!isValidForm()) return;

        String sql = "INSERT INTO bugs(title, description, reporter, assignee, priority, status) VALUES(?,?,?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, titleField.getText().trim());
            ps.setString(2, descriptionArea.getText().trim());
            ps.setString(3, reporterField.getText().trim());
            ps.setString(4, assigneeField.getText().trim());
            ps.setString(5, (String) priorityBox.getSelectedItem());
            ps.setString(6, (String) statusBox.getSelectedItem());

            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Bug added.");
            clearForm();
            loadBugs("", "ALL");

        } catch (Exception ex) {
            showError("Add failed", ex);
        }
    }

    private void updateBug() {
        if (selectedId <= 0) {
            JOptionPane.showMessageDialog(this, "Select a bug first.");
            return;
        }
        if (!isValidForm()) return;

        String sql = "UPDATE bugs SET title=?, description=?, reporter=?, assignee=?, priority=?, status=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, titleField.getText().trim());
            ps.setString(2, descriptionArea.getText().trim());
            ps.setString(3, reporterField.getText().trim());
            ps.setString(4, assigneeField.getText().trim());
            ps.setString(5, (String) priorityBox.getSelectedItem());
            ps.setString(6, (String) statusBox.getSelectedItem());
            ps.setInt(7, selectedId);

            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Bug updated.");
            loadBugs("", "ALL");

        } catch (Exception ex) {
            showError("Update failed", ex);
        }
    }

    private void deleteBug() {
        if (selectedId <= 0) {
            JOptionPane.showMessageDialog(this, "Select a bug first.");
            return;
        }

        int ans = JOptionPane.showConfirmDialog(this,
                "Delete selected bug ID: " + selectedId + " ?",
                "Confirm", JOptionPane.YES_NO_OPTION);

        if (ans != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM bugs WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, selectedId);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Bug deleted.");
            clearForm();
            loadBugs("", "ALL");

        } catch (Exception ex) {
            showError("Delete failed", ex);
        }
    }

    private void updateSelectedStatus(String newStatus) {
        if (selectedId <= 0) {
            JOptionPane.showMessageDialog(this, "Select a bug first.");
            return;
        }

        String sql = "UPDATE bugs SET status=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setInt(2, selectedId);
            ps.executeUpdate();

            statusBox.setSelectedItem(newStatus);
            loadBugs("", "ALL");
            JOptionPane.showMessageDialog(this, "Status updated to " + newStatus);

        } catch (Exception ex) {
            showError("Status update failed", ex);
        }
    }

    private void loadBugs(String keyword, String statusFilter) {
        model.setRowCount(0);

        StringBuilder sql = new StringBuilder(
                "SELECT id,title,priority,status,reporter,assignee,updated_at FROM bugs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (title LIKE ? OR description LIKE ? OR reporter LIKE ? OR assignee LIKE ?)");
            String k = "%" + keyword + "%";
            params.add(k);
            params.add(k);
            params.add(k);
            params.add(k);
        }

        if (statusFilter != null && !"ALL".equalsIgnoreCase(statusFilter)) {
            sql.append(" AND status=?");
            params.add(statusFilter);
        }

        sql.append(" ORDER BY updated_at DESC");

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("updated_at");
                    model.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("priority"),
                            rs.getString("status"),
                            rs.getString("reporter"),
                            rs.getString("assignee"),
                            ts == null ? "" : ts.toString()
                    });
                }
            }

        } catch (Exception ex) {
            showError("Could not load bugs", ex);
        }
    }

    private void loadSelectedRowIntoForm() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        selectedId = Integer.parseInt(model.getValueAt(row, 0).toString());
        titleField.setText(value(row, 1));
        priorityBox.setSelectedItem(value(row, 2));
        statusBox.setSelectedItem(value(row, 3));
        reporterField.setText(value(row, 4));
        assigneeField.setText(value(row, 5));

        String sql = "SELECT description FROM bugs WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, selectedId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) descriptionArea.setText(rs.getString("description"));
                else descriptionArea.setText("");
            }
        } catch (Exception ex) {
            descriptionArea.setText("");
        }
    }

    private String value(int row, int col) {
        Object v = model.getValueAt(row, col);
        return v == null ? "" : v.toString();
    }

    private boolean isValidForm() {
        if (titleField.getText().trim().isEmpty() ||
                descriptionArea.getText().trim().isEmpty() ||
                reporterField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title, Description and Reporter are required.");
            return false;
        }
        return true;
    }

    private void clearForm() {
        selectedId = -1;
        titleField.setText("");
        descriptionArea.setText("");
        reporterField.setText("");
        assigneeField.setText("");
        priorityBox.setSelectedItem("MEDIUM");
        statusBox.setSelectedItem("OPEN");
        table.clearSelection();
    }

    private void showError(String msg, Exception ex) {
        JOptionPane.showMessageDialog(this, msg + "\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BugTrackerApp().setVisible(true));
    }
}
