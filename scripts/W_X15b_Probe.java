import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class W_X15b_Probe {
   public static void main(String[] args) throws Exception {
      String url = "jdbc:postgresql://127.0.0.1:5433/intco";
      String user = "postgres";
      String pw = "postgres";
      try (Connection c = DriverManager.getConnection(url, user, pw);
           Statement s = c.createStatement()) {
         // Show schema (column types)
         System.out.println("=== schema: ignore_alarm ===");
         try (ResultSet rs = s.executeQuery(
               "SELECT column_name, data_type, character_maximum_length " +
               "FROM information_schema.columns WHERE table_name='ignore_alarm' ORDER BY ordinal_position")) {
            while (rs.next()) {
               System.out.println("  " + rs.getString(1) + " | " + rs.getString(2) +
                  (rs.getString(3) != null ? "(" + rs.getString(3) + ")" : ""));
            }
         }
         System.out.println("=== existing rows ===");
         try (ResultSet rs = s.executeQuery(
               "SELECT id, defect_name, type, line_no, face_no, ignore_all, " +
               "start_time, end_time, create_time FROM ignore_alarm ORDER BY id")) {
            int n = 0;
            while (rs.next()) {
               n++;
               System.out.println("  id=" + rs.getInt(1)
                  + " defect=" + rs.getString(2)
                  + " type=" + rs.getInt(3)
                  + " line=" + rs.getString(4)
                  + " face=" + rs.getString(5)
                  + " ignore_all=" + rs.getObject(6)
                  + " start=" + rs.getString(7)
                  + " end=" + rs.getString(8)
                  + " created=" + rs.getString(9));
            }
            System.out.println("  total rows: " + n);
         }
      }
   }
}
