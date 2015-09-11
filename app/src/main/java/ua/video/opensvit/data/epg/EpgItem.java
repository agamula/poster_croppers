package ua.video.opensvit.data.epg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EpgItem {
    public static final String DESCRIPTION = "description";
    public static final String DAY = "day";
    public static final String SUCCESS = "success";
    public static final String DAY_OF_WEEK = "dayOfWeek";

    private String description;
    private int day;
    private boolean success;
    private int dayOfWeek;
    private final List<ProgramItem> programs = new ArrayList<>();

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public void addProgram(ProgramItem programItem) {
        programs.add(programItem);
    }

    public List<ProgramItem> getUnmodifiablePrograms() {
        return Collections.unmodifiableList(programs);
    }

    @Override
    public String toString() {
        return "[" + description + "," + day + "," + success + "," + dayOfWeek + "]";
    }
}
