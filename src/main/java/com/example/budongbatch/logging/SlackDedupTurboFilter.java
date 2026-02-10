package com.example.budongbatch.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 동일한 ERROR 로그가 짧은 시간 안에 반복 발생할 경우 Slack 알림이 과도하게 전송되는 것을 방지하기 위한 필터
 * - 실제 Appender(SlackAppender)로 전달되기 이전에 실행
 * - ERROR 로그에 대해 중복 여부를 판단하는 key를 생성
 * - 동일 key가 suppressSeconds 이내에 다시 발생하면 해당 로그를 Slack으로 보내지 않고 차단
 * - 이 필터는 Slack 알림만 제어한다.
 * - FILE / CONSOLE 로그에는 영향을 주지 않는다.
 */
public class SlackDedupTurboFilter extends TurboFilter {

    //동일 에러로 판단되었을 때 슬랙 알림 차단할 시간 ( 30초)
    private long suppressMillis = 30_000;

    private final Map<String, Long> lastSentAt = new ConcurrentHashMap<>();

    /**
     * 로그 이벤트가 발생할 때 마다 호출되는 메서드
     * @param marker 로그 마커 ( 없으면 null)
     * @param logger 로그를 발생 시킨 Logger
     * @param level 로그 레벨
     * @param format 로그 메세지 포맷 문자열
     * @param params 메세지 파라미터
     * @param t 같이 전달된 예외 ( 없으면 null)
     * @return FilterReply
     * - DENY : 슬랙으로 전송 차단
     * - NEUTRAL: 다른 Appender에게 로그 전달 가능
     */
    @Override
    public FilterReply decide(
            Marker marker,
            Logger logger,
            Level level,
            String format,
            Object[] params,
            Throwable t
    ) {

        // Slack은 ERROR만 대상
        if (!Level.ERROR.equals(level)) {
            return FilterReply.NEUTRAL;
        }

        String key = buildKey(logger, format, t);

        long now = System.currentTimeMillis();
        Long last = lastSentAt.get(key);

        if (last != null && now - last < suppressMillis) {
            //같은 에러, N초 이내 → 차단
            return FilterReply.DENY;
        }

        //첫 발생 or 충분히 시간이 지남
        lastSentAt.put(key, now);
        return FilterReply.NEUTRAL;
    }

    //중복 판단을 위한 key 생성
    private String buildKey(Logger logger, String format, Throwable t) {

        String base = logger.getName();
        if( t != null ) {
            // 예외 타입 기준
            return base + "::" + t.getClass().getName();
        }
        return base + "::" + normalize(format);
    }

    /**
     * 로그 메시지 정규화를 위한 메서드
     * - SockJS 요청 URI, JWT token등으로 요청마다 달라지는 값으로 인해 동일 에러가 다른 에러로 인식되는 것을
     * 방지하기 위한 메서드
     */
    private String normalize(String message) {
        if(message == null) return "";

        // SockJS uri 뒤 파라미터 제거
        return message
                .replaceAll("uri=http[^\\s]+", "uri=<normalized>")
                .replaceAll("token=[^&\\s]+", "token=<masked>"); //JWT toke 마스킹
    }

    // logback.xml에서 값 주입 가능하게
    public void setSuppressSeconds(long seconds) {
        this.suppressMillis = seconds * 1000;
    }
}
