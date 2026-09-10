package org.dubhe.data.capture;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class CaptureExecutionStateMachine {

    private static final Map<CaptureExecutionStatus, Map<CaptureExecutionEvent, CaptureExecutionStatus>> TRANSITIONS;

    static {
        Map<CaptureExecutionStatus, Map<CaptureExecutionEvent, CaptureExecutionStatus>> map = new EnumMap<>(CaptureExecutionStatus.class);

        map.put(CaptureExecutionStatus.QUEUED, createTransitions(
                CaptureExecutionEvent.START, CaptureExecutionStatus.RUNNING,
                CaptureExecutionEvent.CANCEL, CaptureExecutionStatus.CANCELLED,
                CaptureExecutionEvent.FAIL, CaptureExecutionStatus.FAILED
        ));
        map.put(CaptureExecutionStatus.RUNNING, createTransitions(
                CaptureExecutionEvent.FINISH, CaptureExecutionStatus.SUCCESS,
                CaptureExecutionEvent.FAIL, CaptureExecutionStatus.FAILED,
                CaptureExecutionEvent.CANCEL, CaptureExecutionStatus.CANCELLED
        ));
        map.put(CaptureExecutionStatus.SUCCESS, Collections.emptyMap());
        map.put(CaptureExecutionStatus.FAILED, Collections.emptyMap());
        map.put(CaptureExecutionStatus.CANCELLED, Collections.emptyMap());
        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    private CaptureExecutionStateMachine() {
    }

    private static Map<CaptureExecutionEvent, CaptureExecutionStatus> createTransitions(Object... kv) {
        Map<CaptureExecutionEvent, CaptureExecutionStatus> map = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((CaptureExecutionEvent) kv[i], (CaptureExecutionStatus) kv[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }

    public static CaptureExecutionStatus transit(CaptureExecutionStatus current, CaptureExecutionEvent event) {
        if (current == null || event == null) {
            return null;
        }
        return TRANSITIONS.getOrDefault(current, Collections.emptyMap()).get(event);
    }
}
