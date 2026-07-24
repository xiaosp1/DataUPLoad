import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * W-X15b 还原 1 条 W-X15-restore 痕迹数据。
 * PM 工单要求：W-X15a 单元测试清理了 ignore_alarm（PM 翻车承认），W-X15b 还原 1 条做痕迹。
 */
public class W_X15b_RestoreRow {
   public static void main(String[] args) throws Exception {
      String url = "jdbc:postgresql://127.0.0.1:5433/intco";
      String user = "postgres";
      String pw = "postgres";

      // 先确认表是空的
      try (Connection c = DriverManager.getConnection(url, user, pw);
           Statement s = c.createStatement();
           ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM ignore_alarm")) {
         rs.next();
         long before = rs.getLong(1);
         System.out.println("[BEFORE] ignore_alarm rows = " + before);
         if (before != 0) {
            System.err.println("[ABORT] ignore_alarm 已有 " + before + " 行，PM 决策是清表后还原 1 条，本次中止。");
            System.exit(3);
         }
      }

      // INSERT 1 条 W-X15-restore 痕迹
      int id;
      try (Connection c = DriverManager.getConnection(url, user, pw);
           PreparedStatement ps = c.prepareStatement(
              "INSERT INTO ignore_alarm (defect_name, type, line_no, face_no, end_time, create_time, update_time) "
              + "VALUES (?, ?, ?, ?, ?, NOW(), NOW()) RETURNING id")) {
         ps.setString(1, "W-X15-restore");
         ps.setInt(2, 1);
         ps.setString(3, "L-restore");
         ps.setString(4, "F-restore");
         ps.setString(5, "2099-12-31 23:59:59");
         try (ResultSet rs = ps.executeQuery()) {
            rs.next();
            id = rs.getInt(1);
         }
      }
      System.out.println("[INSERT] W-X15-restore id = " + id);

      // 验证
      try (Connection c = DriverManager.getConnection(url, user, pw);
           Statement s = c.createStatement();
           ResultSet rs = s.executeQuery(
              "SELECT id, defect_name, type, line_no, face_no, end_time, create_time " +
              "FROM ignore_alarm WHERE defect_name = 'W-X15-restore'")) {
         while (rs.next()) {
            System.out.println("[VERIFY] id=" + rs.getInt(1)
               + " defect=" + rs.getString(2)
               + " type=" + rs.getInt(3)
               + " line=" + rs.getString(4)
               + " face=" + rs.getString(5)
               + " end=" + rs.getString(6)
               + " created=" + rs.getString(7));
         }
      }

      // 总体计数
      try (Connection c = DriverManager.getConnection(url, user, pw);
           Statement s = c.createStatement();
           ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM ignore_alarm")) {
         rs.next();
         System.out.println("[AFTER] ignore_alarm rows = " + rs.getLong(1));
      }
      System.out.println("[VERDICT] PASS — W-X15-restore 痕迹已还原 1 条。");
   }
}
