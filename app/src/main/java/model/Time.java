package model;

import android.util.Log;

import com.google.firebase.Timestamp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Time {
    private int _nanoseconds, _seconds;
    private Timestamp timestamp;
    private static final DateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
    private static final DateFormat timeFormatter = new SimpleDateFormat("HH:mm");

    public Time() {
    }

    public Time(int _nanoseconds, int _seconds) {
        this._nanoseconds = _nanoseconds;
        this._seconds = _seconds;
        this.timestamp = new Timestamp(_seconds, _nanoseconds);
    }

    public int get_nanoseconds() {
        return _nanoseconds;
    }

    public void set_nanoseconds(int _nanoseconds) {
        this._nanoseconds = _nanoseconds;
    }

    public int get_seconds() {
        return _seconds;
    }

    public void set_seconds(int _seconds) {
        this._seconds = _seconds;
    }

    public String getDateString() {
        if (timestamp == null) {
            timestamp = new Timestamp(_seconds, _nanoseconds);
        }
        return dateFormatter.format(timestamp.toDate());
    }

    public String getTimeString() {
        if (timestamp == null) {
            timestamp = new Timestamp(_seconds, _nanoseconds);
        }
        return timeFormatter.format(timestamp.toDate());
    }
}
