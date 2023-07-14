package cu.desoft.odoo.agendaexpress;

import java.util.Calendar;

/**
 * Created by Fidel on 20/07/2020.
 */

public class Event {

    private String name;
    private Calendar startTime;
    private Calendar endTime;
    private Boolean allday;

    public Event(String pname,Calendar pstartTime,Calendar pendTime, Boolean pallday) {
        name = pname;
        startTime = pstartTime;
        endTime = pendTime;
        allday = pallday;
    }

    public String getName() {
        return name;
    }

    public Calendar getStartTime() {
        return startTime;
    }

    public Calendar getEndTime() {
        return endTime;
    }

    public Boolean getAllDay() {
        return allday;
    }
}
