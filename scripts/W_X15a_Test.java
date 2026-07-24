import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Standalone unit test for W-X15a fix.
 * <p>
 * Validates the EXACT same SQL query that IgnoreAlarmServiceImpl.isIgnore()
 * will emit against PG (via MyBatis-Plus's `eq` + `apply`), against a live PG
 * connection. Does NOT touch hik-java — pure JDBC + psql.
 * <p>
 * MyBatis-Plus 3.5.3 renders `LambdaQueryWrapper.eq(...)` as
 * `column = ?` and `apply("end_time > {0}", nowStr)` as
 * `AND (end_time > ?)`. We bind parameters in the same order MyBatis-Plus
 * appends them (column-eq params first, then apply params).
 */
public class W_X15a_Test {

   public static void main(String[] args) throws Exception {
      String url = "jdbc:postgresql://127.0.0.1:5433/intco";
      String user = "postgres";
      String pw = "postgres";

      // ---- Test constants (must mirror production semantics) ----
      String defectName = "TEST";
      String lineNo = "L";
      String faceNo = "F";
      Integer type = 1;

      // ---- STEP 1: INSERT ignore_alarm row with end_time far in future ----
      int insertedId;
      try (Connection c = DriverManager.getConnection(url, user, pw)) {
         try (PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO ignore_alarm (defect_name, type, line_no, face_no, ignore_all, face_id, start_time, end_time) "
                 + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id")) {
            ps.setString(1, defectName);
            ps.setInt(2, type);
            ps.setString(3, lineNo);
            ps.setString(4, faceNo);
            ps.setInt(5, 2);
            ps.setString(6, null);
            ps.setString(7, "2026-01-01 00:00:00");
            ps.setString(8, "2099-12-31 23:59:59");
            try (ResultSet rs = ps.executeQuery()) {
               rs.next();
               insertedId = rs.getInt(1);
            }
         }
      }
      System.out.println("[STEP 1] INSERT ignore_alarm insertedId=" + insertedId);

      // ---- STEP 2: Mimic IgnoreAlarmServiceImpl.isIgnore() ----
      String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      // MP's `eq` produces SQL fragments like "(type = ?)" with binding params.
      // The .apply("end_time > {0}", nowStr) appends " AND (end_time > ?)" with
      // nowStr as the bound param. Param order follows declaration order in the
      // wrapper: type, defectName, lineNo, faceNo, nowStr.
      String sql = "SELECT COUNT(*) FROM ignore_alarm "
                 + " WHERE (type = ?) "
                 + " AND (defect_name = ?) "
                 + " AND (line_no = ?) "
                 + " AND (face_no = ?) "
                 + " AND (end_time > ?)";
      System.out.println("[STEP 2] SQL: " + sql);
      System.out.println("[STEP 2] Params: type=" + type
         + ", defectName=" + defectName
         + ", lineNo=" + lineNo
         + ", faceNo=" + faceNo
         + ", nowStr=" + nowStr);

      // ---- STEP 3: Execute ----
      long count;
      try (Connection c = DriverManager.getConnection(url, user, pw);
           PreparedStatement ps = c.prepareStatement(sql)) {
         ps.setInt(1, type);
         ps.setString(2, defectName);
         ps.setString(3, lineNo);
         ps.setString(4, faceNo);
         ps.setString(5, nowStr);
         try (ResultSet rs = ps.executeQuery()) {
            rs.next();
            count = rs.getLong(1);
         }
      }
      boolean isIgnoreResult = (count != 0L);
      System.out.println("[STEP 3] COUNT(*) = " + count
         + " => isIgnore(1, 'TEST', 'L', 'F') = " + isIgnoreResult);

      // ---- STEP 4: DELETE test row ----
      int deleted;
      try (Connection c = DriverManager.getConnection(url, user, pw);
           PreparedStatement ps = c.prepareStatement("DELETE FROM ignore_alarm WHERE id = ?")) {
         ps.setInt(1, insertedId);
         deleted = ps.executeUpdate();
      }
      System.out.println("[STEP 4] DELETE ignore_alarm id=" + insertedId + " affected=" + deleted);

      // ---- STEP 5: Verify no leftover rows ----
      long remaining;
      try (Connection c = DriverManager.getConnection(url, user, pw);
           PreparedStatement ps = c.prepareStatement(
              "SELECT COUNT(*) FROM ignore_alarm WHERE defect_name = 'TEST' AND line_no = 'L' AND face_no = 'F'");
           ResultSet rs = ps.executeQuery()) {
         rs.next();
         remaining = rs.getLong(1);
      }
      System.out.println("[STEP 5] Remaining TEST/L/F rows: " + remaining);

      // ---- STEP 6: negative case (no matching row → false) ----
      // Just to also confirm the same query against an empty table returns 0.
      long negCount;
      try (Connection c = DriverManager.getConnection(url, user, pw);
           PreparedStatement ps = c.prepareStatement(sql)) {
         ps.setInt(1, 999);
         ps.setString(2, "NOPE");
         ps.setString(3, "X");
         ps.setString(4, "Y");
         ps.setString(5, nowStr);
         try (ResultSet rs = ps.executeQuery()) {
            rs.next();
            negCount = rs.getLong(1);
         }
      }
      boolean negResult = (negCount != 0L);
      System.out.println("[STEP 6] Negative case (type=999) COUNT(*) = " + negCount
         + " => isIgnore = " + negResult + " (expected false)");

      // ---- Verdict ----
      boolean cleanupOk = (remaining == 0) && (deleted == 1);
      boolean posOk = isIgnoreResult;
      boolean negOk = !negResult;
      if (posOk && negOk && cleanupOk) {
         System.out.println("[VERDICT] PASS — positive=true, negative=false, cleanup=clean.");
         System.exit(0);
      } else {
         System.err.println("[VERDICT] FAIL posOk=" + posOk + " negOk=" + negOk + " cleanupOk=" + cleanupOk);
         System.exit(2);
      }
   }
}
