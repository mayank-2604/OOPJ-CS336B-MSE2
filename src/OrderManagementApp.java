import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.Scanner;

public class OrderManagementApp {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/ecommerce_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "1234"; 
    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC Driver not found: " + e.getMessage());
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to ecommerce_db successfully.");
            
            while (true) {
                printMenu();
                System.out.print("Enter your choice: ");
                String input = scanner.nextLine().trim();

                int choice;
                try {
                    choice = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    System.out.println("Please enter a valid number (1-6).");
                    continue;
                }

                switch (choice) {
                    case 1:
                        insertOrders(conn, scanner);
                        break;
                    case 2:
                        displayOrdersByStatusAndQty(conn, scanner);
                        break;
                    case 3:
                        increaseOrderQuantity(conn, scanner);
                        break;
                    case 4:
                        updateOrderStatus(conn, scanner);
                        break;
                    case 5:
                        showTotalValueAndDeleteCancelledOrder(conn, scanner);
                        break;
                    case 6:
                        System.out.println("Exiting Order Management System. Goodbye!");
                        return;
                    default:
                        System.out.println("Invalid choice. Please select between 1 and 6.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private static void printMenu() {
        System.out.println("\n=== Order Management System (JDBC) ===");
        System.out.println("1. Insert Order Records (PreparedStatement)");
        System.out.println("2. View Orders by Status (Quantity > 2, Sorted by Unit Price DESC)");
        System.out.println("3. Increase Order Quantity by Order ID");
        System.out.println("4. Update Order Status (Pending / Shipped / Delivered)");
        System.out.println("5. Total Order Value & Delete Cancelled Order by ID");
        System.out.println("6. Exit");
    }

    // (a) Insert order records using PreparedStatement and handle duplicate order_id
    private static void insertOrders(Connection conn, Scanner scanner) {
        System.out.print("How many orders would you like to insert? (Enter at least 3 for full test): ");
        int count;
        try {
            count = Integer.parseInt(scanner.nextLine().trim());
            if (count <= 0) {
                System.out.println("Count must be greater than 0.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid count.");
            return;
        }

        String sql = "INSERT INTO Orders (order_id, customer_name, product_name, quantity, unit_price, status) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 1; i <= count; i++) {
                System.out.println("\n--- Entering Order #" + i + " ---");
                try {
                    System.out.print("Order ID (int): ");
                    int orderId = Integer.parseInt(scanner.nextLine().trim());

                    System.out.print("Customer Name: ");
                    String customerName = scanner.nextLine().trim();

                    System.out.print("Product Name: ");
                    String productName = scanner.nextLine().trim();

                    System.out.print("Quantity (int): ");
                    int quantity = Integer.parseInt(scanner.nextLine().trim());

                    System.out.print("Unit Price (double): ");
                    double unitPrice = Double.parseDouble(scanner.nextLine().trim());

                    System.out.print("Status (e.g., Pending, Shipped, Delivered, Cancelled): ");
                    String status = scanner.nextLine().trim();

                    pstmt.setInt(1, orderId);
                    pstmt.setString(2, customerName);
                    pstmt.setString(3, productName);
                    pstmt.setInt(4, quantity);
                    pstmt.setDouble(5, unitPrice);
                    pstmt.setString(6, status);

                    int rows = pstmt.executeUpdate();
                    if (rows > 0) {
                        System.out.println("Order #" + orderId + " inserted successfully.");
                    }
                } catch (SQLIntegrityConstraintViolationException dupEx) {
                    System.out.println("Error: Duplicate Order ID! That order already exists in Orders.");
                } catch (NumberFormatException numEx) {
                    System.out.println("Invalid numeric input for this order. Skipping entry.");
                } catch (SQLException sqlEx) {
                    if (sqlEx.getErrorCode() == 1062) {
                        System.out.println("Error: Duplicate Order ID! That order already exists in Orders.");
                    } else {
                        System.out.println("Failed to insert order: " + sqlEx.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error preparing insert statement: " + e.getMessage());
        }
    }

    // (b) Display orders matching status with quantity > 2, ordered by unit_price DESC
    private static void displayOrdersByStatusAndQty(Connection conn, Scanner scanner) {
        System.out.print("Enter order status to filter: ");
        String status = scanner.nextLine().trim();

        String sql = "SELECT order_id, customer_name, product_name, quantity, unit_price, status " +
                     "FROM Orders WHERE LOWER(status) = LOWER(?) AND quantity > 2 " +
                     "ORDER BY unit_price DESC";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);

            try (ResultSet rs = pstmt.executeQuery()) {
                boolean found = false;
                System.out.println("\nOrder ID | Customer | Product | Qty | Unit Price | Status");
                System.out.println("------------------------------------------------------------------");
                while (rs.next()) {
                    found = true;
                    System.out.printf("%d | %s | %s | %d | %.2f | %s%n",
                            rs.getInt("order_id"),
                            rs.getString("customer_name"),
                            rs.getString("product_name"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_price"),
                            rs.getString("status"));
                }
                if (!found) {
                    System.out.println("No orders found with status '" + status + "' and quantity > 2.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving orders: " + e.getMessage());
        }
    }

    // (c) Increase existing quantity of specified order by additional quantity
    private static void increaseOrderQuantity(Connection conn, Scanner scanner) {
        try {
            System.out.print("Enter Order ID: ");
            int orderId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter additional quantity to add: ");
            int addQty = Integer.parseInt(scanner.nextLine().trim());

            if (addQty <= 0) {
                System.out.println("Additional quantity must be greater than 0.");
                return;
            }

            String updateSql = "UPDATE Orders SET quantity = quantity + ? WHERE order_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setInt(1, addQty);
                pstmt.setInt(2, orderId);

                int rowsUpdated = pstmt.executeUpdate();
                if (rowsUpdated == 0) {
                    System.out.println("Order with ID " + orderId + " does not exist.");
                } else {
                    displaySingleOrder(conn, orderId);
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter valid integers for Order ID and quantity.");
        } catch (SQLException e) {
            System.out.println("Error updating quantity: " + e.getMessage());
        }
    }

    // (d) Retrieve order and update status only if order exists and status is Pending, Shipped, or Delivered
    private static void updateOrderStatus(Connection conn, Scanner scanner) {
        try {
            System.out.print("Enter Order ID: ");
            int orderId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter new status (Pending, Shipped, Delivered): ");
            String newStatusInput = scanner.nextLine().trim();

            String normalizedStatus = normalizeValidStatus(newStatusInput);
            if (normalizedStatus == null) {
                System.out.println("Invalid status! Allowed statuses are: Pending, Shipped, Delivered.");
                return;
            }

            String selectSql = "SELECT order_id, customer_name, product_name, quantity, unit_price, status " +
                               "FROM Orders WHERE order_id = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, orderId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Order with ID " + orderId + " does not exist.");
                        return;
                    }
                    System.out.println("Current order found: " + formatOrderRow(rs));
                }
            }

            String updateSql = "UPDATE Orders SET status = ? WHERE order_id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, normalizedStatus);
                updateStmt.setInt(2, orderId);
                updateStmt.executeUpdate();
            }

            System.out.println("Status updated successfully. Updated order details:");
            displaySingleOrder(conn, orderId);

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid numeric Order ID.");
        } catch (SQLException e) {
            System.out.println("Error updating status: " + e.getMessage());
        }
    }

    // (e) Display SUM(quantity * unit_price) and delete order by ID ONLY if status is Cancelled
    private static void showTotalValueAndDeleteCancelledOrder(Connection conn, Scanner scanner) {
        String sumSql = "SELECT COALESCE(SUM(quantity * unit_price), 0) AS total_value FROM Orders";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sumSql)) {
            if (rs.next()) {
                System.out.printf("Total Order Value across all orders: %.2f%n", rs.getDouble("total_value"));
            }
        } catch (SQLException e) {
            System.out.println("Error calculating total order value: " + e.getMessage());
            return;
        }

        try {
            System.out.print("Enter Order ID to delete (only allowed if status is Cancelled): ");
            int orderId = Integer.parseInt(scanner.nextLine().trim());

            String checkSql = "SELECT status FROM Orders WHERE order_id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, orderId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Order with ID " + orderId + " does not exist.");
                        return;
                    }

                    String currentStatus = rs.getString("status");
                    if (!"Cancelled".equalsIgnoreCase(currentStatus.trim())) {
                        System.out.println("Cannot delete Order #" + orderId + " because its status is '" +
                                currentStatus + "' (must be 'Cancelled').");
                        return;
                    }
                }
            }

            String deleteSql = "DELETE FROM Orders WHERE order_id = ? AND LOWER(status) = 'cancelled'";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setInt(1, orderId);
                int deleted = deleteStmt.executeUpdate();
                if (deleted > 0) {
                    System.out.println("Cancelled Order #" + orderId + " deleted successfully.");
                } else {
                    System.out.println("Order #" + orderId + " could not be deleted.");
                }
            }

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid numeric Order ID.");
        } catch (SQLException e) {
            System.out.println("Error deleting order: " + e.getMessage());
        }
    }

    private static String normalizeValidStatus(String status) {
        if ("Pending".equalsIgnoreCase(status)) return "Pending";
        if ("Shipped".equalsIgnoreCase(status)) return "Shipped";
        if ("Delivered".equalsIgnoreCase(status)) return "Delivered";
        return null;
    }

    private static void displaySingleOrder(Connection conn, int orderId) throws SQLException {
        String sql = "SELECT order_id, customer_name, product_name, quantity, unit_price, status " +
                     "FROM Orders WHERE order_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println(formatOrderRow(rs));
                }
            }
        }
    }

    private static String formatOrderRow(ResultSet rs) throws SQLException {
        return String.format("Order #%d | Customer: %s | Product: %s | Qty: %d | Unit Price: %.2f | Status: %s",
                rs.getInt("order_id"),
                rs.getString("customer_name"),
                rs.getString("product_name"),
                rs.getInt("quantity"),
                rs.getDouble("unit_price"),
                rs.getString("status"));
    }
}