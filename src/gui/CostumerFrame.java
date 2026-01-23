import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class CostumerFrame extends JFrame {

    private DefaultTableModel model;
    private JTable table;

    public CostumerFrame() {
        setTitle("Customer Management");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Ορισμός στηλών
        String[] columns = { "ID", "Name", "Last Name", "Email", "Phone", "Address", "Birth Date"};

        model = new DefaultTableModel(columns, 0);
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();

        JButton btnRefresh = new JButton("Load Data");
        JButton btnAdd = new JButton("Add Customer");
        JButton btnDelete = new JButton("Delete Customer (Dropdown)"); // Εδώ θα είναι το dropdown

        buttonPanel.add(btnRefresh);
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnDelete);

        add(buttonPanel, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadData());
        btnAdd.addActionListener(e -> addCustomer());
        btnDelete.addActionListener(e -> deleteCustomerWithDropdown());

        loadData();
    }

    // method to gather all the customers and create a dropdown list
    private Map<String, String> getExistingCustomers() {
        Map<String, String> customers = new HashMap<>();
        String sql = "SELECT cust_id, cust_name, cust_lname FROM customer ORDER BY cust_id";

        try (Connection conn = DBConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while(rs.next()) {
                String id = String.valueOf(rs.getInt("cust_id"));
                String name = rs.getString("cust_name");
                String lname = rs.getString("cust_lname");

                // Αυτό θα βλέπει ο χρήστης στη λίστα: "1 - Giannis Ioannou"
                String display = id + " - " + name + " " + lname;

                customers.put(display, id); // Αποθηκεύουμε το Display ως κλειδί και το ID ως τιμή
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading customers: " + e.getMessage());
        }

        return customers;
    }

    // load data
    private void loadData() {
        model.setRowCount(0);
        String sql = "SELECT * FROM customer";

        try (Connection conn = DBConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while(rs.next()) {
                int id = rs.getInt("cust_id");
                String name = rs.getString("cust_name");
                String lname = rs.getString("cust_lname");
                String email = rs.getString("cust_email");
                String phone = rs.getString("cust_phone");
                String address = rs.getString("cust_address");
                Date birthDate = rs.getDate("cust_birth_date");

                model.addRow(new Object[]{id, name, lname, email, phone, address, birthDate});
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "SQL Error: " + e.getMessage());
        }
    }


    private void addCustomer() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));

        JTextField txtId = new JTextField();
        JTextField txtName = new JTextField();
        JTextField txtLname = new JTextField();
        JTextField txtEmail = new JTextField();
        JTextField txtPhone = new JTextField();
        JTextField txtAddress = new JTextField();
        JTextField txtBirthDate = new JTextField();
        txtBirthDate.setToolTipText("YYYY-MM-DD");

        panel.add(new JLabel("Customer ID:")); panel.add(txtId);
        panel.add(new JLabel("First Name:")); panel.add(txtName);
        panel.add(new JLabel("Last Name:")); panel.add(txtLname);
        panel.add(new JLabel("Email:")); panel.add(txtEmail);
        panel.add(new JLabel("Phone:")); panel.add(txtPhone);
        panel.add(new JLabel("Address:")); panel.add(txtAddress);
        panel.add(new JLabel("Birth Date (YYYY-MM-DD):")); panel.add(txtBirthDate);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Add New Customer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            if (txtId.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "ID is required!");
                return;
            }

            String sql = "INSERT INTO customer (cust_id, cust_name, cust_lname, cust_email, cust_phone, cust_address, cust_birth_date) VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = DBConnection.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, Integer.parseInt(txtId.getText().trim()));
                pstmt.setString(2, txtName.getText().trim());
                pstmt.setString(3, txtLname.getText().trim());
                pstmt.setString(4, txtEmail.getText().trim());
                pstmt.setString(5, txtPhone.getText().trim());
                pstmt.setString(6, txtAddress.getText().trim());

                String dateStr = txtBirthDate.getText().trim();
                if (!dateStr.isEmpty()) {
                    pstmt.setDate(7, Date.valueOf(dateStr));
                } else {
                    pstmt.setNull(7, Types.DATE);
                }

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(this, "Success! Customer added.");
                    loadData();
                }

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Error: ID must be a number!");
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, "Error: Date must be YYYY-MM-DD!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage());
            }
        }
    }



    // delete customers using dropdown list so it is easier to use
    private void deleteCustomerWithDropdown() {

        Map<String, String> customers = getExistingCustomers();

        if (customers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No customers found to delete.");
            return;
        }

        // getting the customers in an array to performm dropdown
        String[] customerOptions = customers.keySet().toArray(new String[0]);

        // display dropdown
        String selectedDisplay = (String) JOptionPane.showInputDialog(
                this,
                "Select Customer to Delete:",
                "Delete Customer",
                JOptionPane.QUESTION_MESSAGE,
                null,
                customerOptions,
                customerOptions[0]
        );

        // cancel
        if (selectedDisplay == null) return;

        // id and its nAME
        String custIdStr = customers.get(selectedDisplay);
        int custId = Integer.parseInt(custIdStr);

        // confirming
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete customer: " + selectedDisplay + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM customer WHERE cust_id = ?";

            try (Connection conn = DBConnection.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, custId);
                int rows = pstmt.executeUpdate();

                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "Customer deleted successfully.");
                    loadData();
                }

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting: " + e.getMessage());
            }
        }
    }


}
