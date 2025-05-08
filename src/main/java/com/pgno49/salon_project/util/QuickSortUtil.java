package com.pgno49.salon_project.util;

import com.pgno49.salon_project.model.Appointment;
import java.util.List;
import java.time.LocalDateTime;

public class QuickSortUtil {

    public static void sortAppointments(List<Appointment> list) {
        if (list == null || list.size() <= 1) {
            return;
        }
        quickSort(list, 0, list.size() - 1);
    }

    private static void quickSort(List<Appointment> list, int p, int r) {
        if (p < r) {
            int q = partition(list, p, r);
            quickSort(list, p, q - 1);
            quickSort(list, q + 1, r);
        }
    }


    private static int partition(List<Appointment> list, int p, int r) {
        Appointment pivotAppointment = list.get(r);
        LocalDateTime pivotDateTime = pivotAppointment.getAppointmentDateTime();
        int i = p - 1;

        for (int j = p; j < r; j++) {
            Appointment currentAppointment = list.get(j);
            if (currentAppointment.getAppointmentDateTime() != null &&
                    (pivotDateTime == null || currentAppointment.getAppointmentDateTime().isBefore(pivotDateTime) || currentAppointment.getAppointmentDateTime().isEqual(pivotDateTime))) {
                i = i + 1;
                exchange(list, i, j);
            } else if (currentAppointment.getAppointmentDateTime() == null && pivotDateTime != null) {
                i = i + 1;
                exchange(list, i, j);
            }
        }
        exchange(list, i + 1, r);
        return i + 1;
    }

    private static void exchange(List<Appointment> list, int i, int j) {
        Appointment temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }
}
