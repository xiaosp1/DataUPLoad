import com.hikrobotics.solution.module.alarm.entity.IgnoreAlarm;
import java.time.LocalDateTime;

/**
 * W-X15b entity setter 行为测试：
 *  - setEndTimeByString("2099-12-31 23:59:59") → endTime = LocalDateTime
 *  - getEndTime() 返回 LocalDateTime
 */
public class W_X15b_EntityParseTest {
   public static void main(String[] args) {
      IgnoreAlarm entity = new IgnoreAlarm()
         .setEndTimeByString("2099-12-31 23:59:59")
         .setStartTimeByString("2026-01-01 00:00:00");
      LocalDateTime et = entity.getEndTime();
      LocalDateTime st = entity.getStartTime();
      System.out.println("[ENTITY] endTime class   = " + (et == null ? "null" : et.getClass().getName()));
      System.out.println("[ENTITY] endTime value   = " + et);
      System.out.println("[ENTITY] startTime class = " + (st == null ? "null" : st.getClass().getName()));
      System.out.println("[ENTITY] startTime value = " + st);

      // null/empty 不抛
      IgnoreAlarm e2 = new IgnoreAlarm().setEndTimeByString(null).setEndTimeByString("");
      System.out.println("[ENTITY] null/empty endTime = " + e2.getEndTime());

      boolean ok = et != null
                 && et.getYear() == 2099
                 && et.getMonthValue() == 12
                 && et.getDayOfMonth() == 31
                 && et.getHour() == 23
                 && et.getMinute() == 59
                 && et.getSecond() == 59
                 && st != null
                 && e2.getEndTime() == null;
      if (ok) {
         System.out.println("[VERDICT] PASS — entity LocalDateTime fields + byString setters correct.");
         System.exit(0);
      } else {
         System.err.println("[VERDICT] FAIL");
         System.exit(2);
      }
   }
}
