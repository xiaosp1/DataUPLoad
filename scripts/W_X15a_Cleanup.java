import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class W_X15a_Cleanup {
   public static void main(String[] args) throws Exception {
      String url = "jdbc:postgresql://127.0.0.1:5433/intco";
      // Step A: count via separate connection BEFORE delete
      long before;
      try (Connection c1 = DriverManager.getConnection(url, "postgres", "postgres");
           Statement s1 = c1.createStatement();
           ResultSet rs1 = s1.executeQuery("SELECT COUNT(*) FROM ignore_alarm WHERE defect_name='TEST' AND line_no='L' AND face_no='F'")) {
         rs1.next();
         before = rs1.getLong(1);
      }
      System.out.println("before=" + before);
      // Step B: delete via separate connection
      int deleted;
      try (Connection c2 = DriverManager.getConnection(url, "postgres", "postgres");
           PreparedStatement ps = c2.prepareStatement("DELETE FROM ignore_alarm WHERE defect_name='TEST' AND line_no='L' AND face_no='F'")) {
         deleted = ps.executeUpdate();
      }
      System.out.println("deleted=" + deleted);
      // Step C: count via separate connection AFTER delete
      long after;
      try (Connection c3 = DriverManager.getConnection(url, "postgres", "postgres");
           Statement s3 = c3.createStatement();
           ResultSet rs3 = s3.executeQuery("SELECT COUNT(*) FROM ignore_alarm WHERE defect_name='TEST' AND line_no='L' AND face_no='F'")) {
         rs3.next();
         after = rs3.getLong(1);
      }
      System.out.println("after=" + after);
   }
}
